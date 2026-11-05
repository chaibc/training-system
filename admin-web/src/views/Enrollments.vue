<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { enrollmentApi, classApi, basicApi, studentApi, courseApi } from '../api'
import { ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'

const activeTab = ref('apply')
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, status: undefined, keyword: '' })
const classes = ref([])
const students = ref([])
const courses = ref([])
const createOpen = ref(false)
const createSaving = ref(false)
const createForm = reactive({ studentId: undefined, courseId: undefined, classId: undefined, amount: 0, remark: '' })
const createClasses = computed(() => classes.value.filter((c) => c.courseId === createForm.courseId))

const trialLoading = ref(false)
const trials = ref([])
const trialTotal = ref(0)
const trialQuery = reactive({ current: 1, size: 10, status: undefined, keyword: '' })

const payLoading = ref(false)
const payments = ref([])
const payTotal = ref(0)
const payQuery = reactive({ current: 1, size: 10, keyword: '' })

const confirmOpen = ref(false)
const confirmSaving = ref(false)
const confirmRow = ref(null)
const confirmForm = reactive({ classId: undefined, amount: 0, note: '' })

const payOpen = ref(false)
const paying = ref(false)
const payRow = ref(null)
const payForm = reactive({ paymentMethodId: undefined, amount: 0, note: '' })
const methods = ref([])

const followOpen = ref(false)
const followRow = ref(null)
const followForm = reactive({ status: 'CONTACTED', note: '', nextFollowUp: null })

const statusMap = {
  PENDING_CONFIRM: { text: '待确认', cls: 'gold' },
  PENDING_PAYMENT: { text: '待缴费', cls: 'red' },
  ENROLLED: { text: '已入班', cls: 'green' },
  CANCELLED: { text: '已取消', cls: 'gray' }
}
const trialStatusMap = {
  NEW: { text: '待联系', cls: 'gold' },
  CONTACTED: { text: '已联系', cls: 'blue' },
  VISITED: { text: '已到访', cls: 'blue' },
  CONVERTED: { text: '已转化', cls: 'green' },
  CANCELLED: { text: '已取消', cls: 'gray' }
}

const columns = [
  { title: '申请编号', dataIndex: 'enrollmentNo', key: 'enrollmentNo', width: 150 },
  { title: '学员', key: 'student', width: 110 },
  { title: '意向课程 / 班级', key: 'course', width: 210 },
  { title: '家长', key: 'parent', width: 130 },
  { title: '提交时间', key: 'submittedAt', width: 130 },
  { title: '应缴金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 170 }
]
const trialColumns = [
  { title: '预约编号', dataIndex: 'bookingNo', key: 'bookingNo', width: 150 },
  { title: '学员 / 年龄', key: 'student', width: 110 },
  { title: '意向课程', dataIndex: 'courseName', key: 'courseName', width: 170 },
  { title: '家长联系方式', key: 'phone', width: 140 },
  { title: '期望时间', dataIndex: 'slotName', key: 'slotName', width: 170 },
  { title: '状态', key: 'status', width: 90 },
  { title: '下次跟进', dataIndex: 'nextFollowUp', key: 'nextFollowUp', width: 100 },
  { title: '操作', key: 'actions', width: 100 }
]
const payColumns = [
  { title: '缴费单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 150 },
  { title: '学员', dataIndex: 'studentName', key: 'studentName', width: 100 },
  { title: '项目', dataIndex: 'item', key: 'item' },
  { title: '金额', key: 'amount', width: 100 },
  { title: '方式', dataIndex: 'methodName', key: 'methodName', width: 100 },
  { title: '登记人', dataIndex: 'registeredBy', key: 'registeredBy', width: 90 },
  { title: '登记时间', key: 'paidAt', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 }
]

async function load() {
  loading.value = true
  try {
    const data = await enrollmentApi.page({
      current: query.current, size: query.size,
      status: query.status || undefined, keyword: query.keyword || undefined
    })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

async function loadTrials() {
  trialLoading.value = true
  try {
    const data = await enrollmentApi.trials({
      current: trialQuery.current, size: trialQuery.size,
      status: trialQuery.status || undefined, keyword: trialQuery.keyword || undefined
    })
    trials.value = data.records
    trialTotal.value = data.total
  } finally { trialLoading.value = false }
}

async function loadPayments() {
  payLoading.value = true
  try {
    const data = await enrollmentApi.payments({
      current: payQuery.current, size: payQuery.size, keyword: payQuery.keyword || undefined
    })
    payments.value = data.records
    payTotal.value = data.total
  } finally { payLoading.value = false }
}

async function loadClasses() {
  try { classes.value = await classApi.list() } catch { classes.value = [] }
}
async function loadMethods() {
  try { methods.value = await basicApi.list(basicApi.paymentMethods) } catch { methods.value = [] }
}
async function loadOptions() {
  const [s, c] = await Promise.all([studentApi.options(), courseApi.list()])
  students.value = s
  courses.value = c
}

function openCreate() {
  Object.assign(createForm, {
    studentId: undefined,
    courseId: courses.value[0]?.id,
    classId: undefined,
    amount: 0,
    remark: ''
  })
  createOpen.value = true
}
async function submitCreate() {
  if (!createForm.studentId) return message.warning('请选择学员')
  if (!createForm.courseId) return message.warning('请选择课程')
  createSaving.value = true
  try {
    await enrollmentApi.create({
      ...createForm,
      classId: createForm.classId || null,
      amount: Number(createForm.amount) || null
    })
    message.success('报名已创建，请继续确认与登记缴费')
    createOpen.value = false
    activeTab.value = 'apply'
    await load()
  } finally { createSaving.value = false }
}

function openConfirm(row) {
  confirmRow.value = row
  Object.assign(confirmForm, { classId: row.classId, amount: Number(row.amount || 0), note: '' })
  confirmOpen.value = true
}
async function submitConfirm() {
  confirmSaving.value = true
  try {
    await enrollmentApi.confirm(confirmRow.value.id, { ...confirmForm, amount: Number(confirmForm.amount) })
    message.success('已确认报名，状态变为待缴费')
    confirmOpen.value = false
    await load()
  } finally { confirmSaving.value = false }
}

function openPay(row) {
  payRow.value = row
  Object.assign(payForm, { paymentMethodId: methods.value[0]?.id, amount: Number(row.amount || 0), note: '' })
  payOpen.value = true
}
async function submitPay() {
  if (!payForm.paymentMethodId) { message.warning('请选择缴费方式'); return }
  paying.value = true
  try {
    await enrollmentApi.pay(payRow.value.id, { ...payForm, amount: Number(payForm.amount) })
    message.success('已登记线下缴费，学员已入班并写入课时账户')
    payOpen.value = false
    await load()
    await loadPayments()
  } finally { paying.value = false }
}

async function cancel(row) {
  await enrollmentApi.cancel(row.id)
  message.success('报名已取消')
  await load()
}

function openFollow(row) {
  followRow.value = row
  Object.assign(followForm, {
    status: row.status === 'NEW' ? 'CONTACTED' : row.status,
    note: row.followUpNote || '',
    nextFollowUp: row.nextFollowUp ? dayjs(row.nextFollowUp) : null
  })
  followOpen.value = true
}
async function submitFollow() {
  await enrollmentApi.followUp(followRow.value.id, {
    status: followForm.status,
    note: followForm.note,
    nextFollowUp: followForm.nextFollowUp ? followForm.nextFollowUp.format('YYYY-MM-DD') : null
  })
  message.success('跟进记录已保存')
  followOpen.value = false
  await loadTrials()
}

function onTab(key) {
  activeTab.value = key
  if (key === 'apply') load()
  if (key === 'trial') loadTrials()
  if (key === 'pay') loadPayments()
}

onMounted(async () => {
  await Promise.all([load(), loadClasses(), loadMethods(), loadOptions()])
})
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="eyebrow">ENROLLMENT · OFFLINE PAYMENT</div>
        <h1>报名与缴费</h1>
        <p>报名申请、试听线索与线下缴费统一登记</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="onTab(activeTab)"><ReloadOutlined /> 刷新</a-button>
        <a-button type="primary" size="small" @click="openCreate"><PlusOutlined /> 新建报名</a-button>
      </div>
    </div>

    <div class="scope-banner" style="background:#FBF5E7;border:1px solid #EBD9B4;border-radius:9px;padding:10px 13px;font-size:10px;color:#6E5727;line-height:1.8;margin-bottom:14px">
      <b>范围说明：</b>本期不包含线上支付能力。家长在小程序提交报名/续费意向后，由教务在本页确认并登记线下收款，系统写入课时账户与缴费记录。
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <a-tabs :active-key="activeTab" @change="onTab">
        <a-tab-pane key="apply" tab="报名申请">
          <div class="filters">
            <a-input v-model:value="query.keyword" placeholder="搜索申请编号 / 学员姓名" allow-clear style="width:220px" @press-enter="() => { query.current = 1; load() }" />
            <a-select v-model:value="query.status" placeholder="全部状态" allow-clear style="width:130px" @change="() => { query.current = 1; load() }">
              <a-select-option value="PENDING_CONFIRM">待确认</a-select-option>
              <a-select-option value="PENDING_PAYMENT">待缴费</a-select-option>
              <a-select-option value="ENROLLED">已入班</a-select-option>
              <a-select-option value="CANCELLED">已取消</a-select-option>
            </a-select>
            <a-button size="small" @click="() => { query.current = 1; load() }">查询</a-button>
          </div>
          <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="small"
                   :pagination="{ current: query.current, pageSize: query.size, total, showSizeChanger: false }"
                   @change="(p) => { query.current = p.current; load() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'student'"><b>{{ record.studentName }}</b></template>
              <template v-if="column.key === 'course'">{{ record.courseName }}<div class="muted" style="font-size:9px">{{ record.className || '未分班' }}</div></template>
              <template v-if="column.key === 'parent'">{{ record.parentName }}<div class="muted" style="font-size:9px">{{ record.parentPhone }}</div></template>
              <template v-if="column.key === 'submittedAt'">{{ record.submittedAt ? dayjs(record.submittedAt).format('MM-DD HH:mm') : '—' }}</template>
              <template v-if="column.key === 'amount'">¥{{ Number(record.amount || 0).toLocaleString() }}</template>
              <template v-if="column.key === 'status'"><span class="tag" :class="statusMap[record.status]?.cls">{{ statusMap[record.status]?.text }}</span></template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button v-if="record.status === 'PENDING_CONFIRM'" type="link" size="small" @click="openConfirm(record)">确认报名</a-button>
                  <a-button v-if="record.status === 'PENDING_PAYMENT'" type="link" size="small" @click="openPay(record)">登记缴费</a-button>
                  <a-button v-if="['PENDING_CONFIRM','PENDING_PAYMENT'].includes(record.status)" type="link" size="small" danger @click="cancel(record)">取消</a-button>
                  <span v-if="record.status === 'ENROLLED'" class="muted" style="font-size:9px">已入班</span>
                </div>
              </template>
            </template>
          </a-table>
          <div class="footnote">确认报名后家长端状态变为「待缴费」；登记线下缴费后自动入班并写入课时账户。</div>
        </a-tab-pane>

        <a-tab-pane key="trial" tab="试听预约">
          <div class="filters">
            <a-input v-model:value="trialQuery.keyword" placeholder="搜索学员 / 手机号 / 预约编号" allow-clear style="width:240px" @press-enter="() => { trialQuery.current = 1; loadTrials() }" />
            <a-select v-model:value="trialQuery.status" placeholder="全部跟进状态" allow-clear style="width:140px" @change="() => { trialQuery.current = 1; loadTrials() }">
              <a-select-option value="NEW">待联系</a-select-option>
              <a-select-option value="CONTACTED">已联系</a-select-option>
              <a-select-option value="VISITED">已到访</a-select-option>
              <a-select-option value="CONVERTED">已转化</a-select-option>
              <a-select-option value="CANCELLED">已取消</a-select-option>
            </a-select>
            <a-button size="small" @click="() => { trialQuery.current = 1; loadTrials() }">查询</a-button>
          </div>
          <a-table :columns="trialColumns" :data-source="trials" :loading="trialLoading" row-key="id" size="small"
                   :pagination="{ current: trialQuery.current, pageSize: trialQuery.size, total: trialTotal, showSizeChanger: false }"
                   @change="(p) => { trialQuery.current = p.current; loadTrials() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'student'"><b>{{ record.studentName }}</b><div class="muted" style="font-size:9px">{{ record.age }}</div></template>
              <template v-if="column.key === 'phone'">{{ record.parentName }} · {{ record.phone }}</template>
              <template v-if="column.key === 'status'"><span class="tag" :class="trialStatusMap[record.status]?.cls">{{ trialStatusMap[record.status]?.text || record.status }}</span></template>
              <template v-if="column.key === 'actions'"><a-button type="link" size="small" @click="openFollow(record)">记录跟进</a-button></template>
            </template>
          </a-table>
          <div class="footnote">试听预约由家长小程序提交；教务电话确认并记录跟进结果。本期不发送微信通知。</div>
        </a-tab-pane>

        <a-tab-pane key="pay" tab="缴费记录">
          <div class="filters">
            <a-input v-model:value="payQuery.keyword" placeholder="搜索缴费单号 / 学员姓名" allow-clear style="width:240px" @press-enter="() => { payQuery.current = 1; loadPayments() }" />
            <a-button size="small" @click="() => { payQuery.current = 1; loadPayments() }">查询</a-button>
            <div class="sp"></div>
            <span class="muted small">线下登记 · 不含线上支付</span>
          </div>
          <a-table :columns="payColumns" :data-source="payments" :loading="payLoading" row-key="id" size="small"
                   :pagination="{ current: payQuery.current, pageSize: payQuery.size, total: payTotal, showSizeChanger: false }"
                   @change="(p) => { payQuery.current = p.current; loadPayments() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'"><b class="num">¥{{ Number(record.amount || 0).toLocaleString() }}</b></template>
              <template v-if="column.key === 'paidAt'">{{ record.paidAt ? dayjs(record.paidAt).format('MM-DD HH:mm') : '—' }}</template>
              <template v-if="column.key === 'status'"><span class="tag green">{{ record.status }}</span></template>
            </template>
          </a-table>
          <div class="footnote">线下缴费须记录登记人与时间；课时账户与缴费流水一一对应。</div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <a-modal v-model:open="createOpen" title="新建报名（前台/线下）" ok-text="创建报名" cancel-text="取消"
             :confirm-loading="createSaving" @ok="submitCreate">
      <a-form layout="vertical">
        <a-form-item label="学员" required>
          <a-select v-model:value="createForm.studentId" show-search option-filter-prop="label" placeholder="选择学员">
            <a-select-option v-for="s in students" :key="s.id" :value="s.id" :label="s.name + ' ' + (s.studentNo || '')">{{ s.name }} · {{ s.studentNo }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="课程" required>
          <a-select v-model:value="createForm.courseId" placeholder="选择课程" @change="createForm.classId = undefined">
            <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="班级">
          <a-select v-model:value="createForm.classId" placeholder="可稍后由教务分配" allow-clear>
            <a-select-option v-for="c in createClasses" :key="c.id" :value="c.id">{{ c.name }}（容量 {{ c.capacity }}）</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="应缴金额（元）"><a-input-number v-model:value="createForm.amount" style="width:100%" :min="0" /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="createForm.remark" :rows="2" placeholder="如：前台到店报名、家长说明" /></a-form-item>
      </a-form>
      <div class="login-note">创建后状态为待确认；确认并登记线下缴费后才会充值课时和入班。</div>
    </a-modal>

    <a-modal v-model:open="confirmOpen" title="确认报名" ok-text="确认报名" cancel-text="取消" :confirm-loading="confirmSaving" @ok="submitConfirm">
      <a-form layout="vertical">
        <a-form-item label="学员"><a-input :value="confirmRow?.studentName" disabled /></a-form-item>
        <a-form-item label="意向课程"><a-input :value="confirmRow?.courseName" disabled /></a-form-item>
        <a-form-item label="分配班级">
          <a-select v-model:value="confirmForm.classId" placeholder="选择班级" allow-clear>
            <a-select-option v-for="c in classes" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="应缴金额（元）"><a-input-number v-model:value="confirmForm.amount" style="width:100%" :min="0" /></a-form-item>
        <a-form-item label="确认备注"><a-textarea v-model:value="confirmForm.note" :rows="2" /></a-form-item>
        <div class="login-note">确认后家长端报名记录显示「待缴费」，本期不发起线上支付。</div>
      </a-form>
    </a-modal>

    <a-modal v-model:open="payOpen" title="登记线下缴费" ok-text="确认登记" cancel-text="取消" :confirm-loading="paying" @ok="submitPay">
      <a-form layout="vertical">
        <a-form-item label="学员"><a-input :value="payRow?.studentName" disabled /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="缴费方式" required><a-select v-model:value="payForm.paymentMethodId"><a-select-option v-for="m in methods" :key="m.id" :value="m.id">{{ m.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="金额（元）"><a-input-number v-model:value="payForm.amount" style="width:100%" :min="0" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="备注 / 对账信息"><a-textarea v-model:value="payForm.note" :rows="2" placeholder="如转账尾号、家长说明（选填）" /></a-form-item>
        <div class="login-note">缴费后自动写入课时账户与缴费流水；本期不包含线上支付。</div>
      </a-form>
    </a-modal>

    <a-modal v-model:open="followOpen" title="试听跟进" ok-text="保存跟进" cancel-text="取消" @ok="submitFollow">
      <a-form layout="vertical">
        <a-form-item label="学员"><a-input :value="followRow?.studentName" disabled /></a-form-item>
        <a-form-item label="跟进结果">
          <a-select v-model:value="followForm.status">
            <a-select-option value="CONTACTED">已联系，待确认时间</a-select-option>
            <a-select-option value="VISITED">已到访</a-select-option>
            <a-select-option value="CONVERTED">已转化报名</a-select-option>
            <a-select-option value="CANCELLED">暂无意向</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="下次跟进时间"><a-date-picker v-model:value="followForm.nextFollowUp" style="width:100%" /></a-form-item>
        <a-form-item label="沟通备注"><a-textarea v-model:value="followForm.note" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
