<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { leaveApi, studentApi, lessonApi, classApi } from '../api'
import { ReloadOutlined } from '@ant-design/icons-vue'

const loading = ref(false)
const rows = ref([])
const students = ref([])
const lessons = ref([])
const classes = ref([])
const statusFilter = ref('ALL')
const approvingId = ref(null)
const rejectOpen = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const rejecting = ref(false)
const makeupOpen = ref(false)
const makeupSaving = ref(false)
const makeupTarget = ref(null)
const makeupLessonId = ref(undefined)
const makeupNote = ref('')

const statusMap = {
  PENDING: { text: '待审批', cls: 'gold' },
  APPROVED: { text: '已通过', cls: 'green' },
  REJECTED: { text: '已驳回', cls: 'red' },
  CANCELLED: { text: '已撤销', cls: 'gray' }
}
const columns = [
  { title: '学员', key: 'student', width: 100 },
  { title: '请假课次', key: 'lesson', width: 210 },
  { title: '原因', dataIndex: 'reasonText', key: 'reasonText' },
  { title: '提交时间', key: 'submittedAt', width: 130 },
  { title: '是否扣课', key: 'deduct', width: 85 },
  { title: '状态', key: 'status', width: 75 },
  { title: '补课', key: 'makeup', width: 90 },
  { title: '操作', key: 'actions', width: 220 }
]
const filteredRows = computed(() => statusFilter.value === 'ALL'
  ? rows.value
  : rows.value.filter((r) => r.status === statusFilter.value))
const pendingCount = computed(() => rows.value.filter((r) => r.status === 'PENDING').length)
const studentName = (id) => students.value.find((x) => x.id === id)?.name || `学员 #${id}`
const lessonTitle = (id) => {
  const l = lessons.value.find((x) => x.id === id)
  if (!l) return `课次 #${id}`
  const c = classes.value.find((x) => x.id === l.classId)
  return `${c?.name || '班级'} · 第 ${l.lessonNo} 课 · ${l.lessonDate}`
}

async function load() {
  loading.value = true
  try {
    const [d, s, l, c] = await Promise.all([
      leaveApi.page({ current: 1, size: 200 }), studentApi.options(), lessonApi.list(), classApi.list()
    ])
    rows.value = d.records
    students.value = s
    lessons.value = l
    classes.value = c
  } finally { loading.value = false }
}

async function approve(row) {
  approvingId.value = row.id
  try {
    await leaveApi.approve(row.id)
    message.success('已通过请假，出勤与课时已同步')
    await load()
  } finally { approvingId.value = null }
}

function openReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectOpen.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    message.warning('请填写驳回原因')
    return
  }
  rejecting.value = true
  try {
    await leaveApi.reject(rejectTarget.value.id, rejectReason.value.trim())
    message.success('已驳回请假，家长端记录同步更新')
    rejectOpen.value = false
    await load()
  } finally { rejecting.value = false }
}

function openMakeup(row) {
  makeupTarget.value = row
  makeupLessonId.value = undefined
  makeupNote.value = ''
  makeupOpen.value = true
}

async function submitMakeup() {
  if (!makeupLessonId.value) return message.warning('请选择补课课次')
  makeupSaving.value = true
  try {
    await leaveApi.arrangeMakeup(makeupTarget.value.id, {
      lessonId: makeupLessonId.value,
      note: makeupNote.value
    })
    message.success('补课已安排')
    makeupOpen.value = false
    await load()
  } finally { makeupSaving.value = false }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="eyebrow">LEAVE APPROVAL</div>
        <h1>请假审批</h1>
        <p>按机构规则判断是否扣课时，审批结果写入家长端请假记录并联动出勤与课时流水</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="load"><ReloadOutlined /> 刷新</a-button>
      </div>
    </div>

    <div class="stats">
      <div class="stat-card"><div class="lbl">待审批</div><div class="v">{{ pendingCount }}<em>条</em></div><div class="foot">需要教务或教师处理</div></div>
      <div class="stat-card jade"><div class="lbl">已通过</div><div class="v">{{ rows.filter(r => r.status === 'APPROVED').length }}<em>条</em></div><div class="foot">按规则扣课 / 不扣课</div></div>
      <div class="stat-card red"><div class="lbl">已驳回</div><div class="v">{{ rows.filter(r => r.status === 'REJECTED').length }}<em>条</em></div><div class="foot">驳回原因对家长可见</div></div>
      <div class="stat-card blue"><div class="lbl">待补课</div><div class="v">{{ rows.filter(r => r.makeupStatus === 'PENDING').length }}<em>条</em></div><div class="foot">家长已提交补课申请</div></div>
    </div>

    <div class="paper-card" style="padding:16px">
      <div class="filters">
        <a-radio-group v-model:value="statusFilter" size="small" button-style="solid">
          <a-radio-button value="ALL">全部</a-radio-button>
          <a-radio-button value="PENDING">待审批</a-radio-button>
          <a-radio-button value="APPROVED">已通过</a-radio-button>
          <a-radio-button value="REJECTED">已驳回</a-radio-button>
          <a-radio-button value="CANCELLED">已撤销</a-radio-button>
        </a-radio-group>
        <div class="sp"></div>
        <span class="muted small">审批通过后，出勤与课时账户同步；不发送微信通知</span>
      </div>
      <a-table :columns="columns" :data-source="filteredRows" :loading="loading" row-key="id" size="small"
               :pagination="{ pageSize: 10, showSizeChanger: false }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'student'"><b>{{ studentName(record.studentId) }}</b></template>
          <template v-if="column.key === 'lesson'">{{ lessonTitle(record.lessonId) }}</template>
          <template v-if="column.key === 'submittedAt'">{{ record.submittedAt ? dayjs(record.submittedAt).format('MM-DD HH:mm') : '—' }}</template>
          <template v-if="column.key === 'deduct'"><span class="tag" :class="record.deductHours === 1 ? 'red' : 'green'">{{ record.deductHours === 1 ? '扣课时' : '不扣课时' }}</span></template>
          <template v-if="column.key === 'status'"><span class="tag" :class="statusMap[record.status]?.cls">{{ statusMap[record.status]?.text }}</span></template>
          <template v-if="column.key === 'makeup'">
            <span v-if="record.status === 'APPROVED'" class="tag" :class="record.makeupStatus === 'ARRANGED' ? 'green' : 'gold'">
              {{ record.makeupStatus === 'ARRANGED' ? '已安排' : record.makeupStatus === 'PENDING' ? '待安排' : '可申请' }}
            </span>
            <span v-else class="muted" style="font-size:9px">—</span>
          </template>
          <template v-if="column.key === 'actions'">
            <div v-if="record.status === 'PENDING'" class="ops">
              <a-button type="link" size="small" :loading="approvingId === record.id" @click="approve(record)">同意</a-button>
              <a-button type="link" size="small" danger @click="openReject(record)">驳回</a-button>
            </div>
            <div v-else-if="record.status === 'APPROVED' && record.makeupStatus !== 'ARRANGED'" class="ops">
              <a-button type="link" size="small" @click="openMakeup(record)">安排补课</a-button>
            </div>
            <span v-else class="muted" style="font-size:9px">{{ record.approvedAt ? dayjs(record.approvedAt).format('MM-DD HH:mm') : '已处理' }}</span>
          </template>
        </template>
      </a-table>
      <div class="footnote">审批结果写入家长端请假记录；扣课时按提交时规则判定，调整课时会写入课时流水，可追溯。</div>
    </div>

    <a-modal v-model:open="rejectOpen" title="驳回请假" ok-text="确认驳回" cancel-text="取消"
             :confirm-loading="rejecting" @ok="submitReject">
      <div class="muted small" style="margin-bottom:12px">驳回原因将在家长端请假记录中展示。</div>
      <a-textarea v-model:value="rejectReason" :rows="3" maxlength="200" show-count placeholder="如：课次已开始，按未到课处理" />
    </a-modal>

    <a-modal v-model:open="makeupOpen" title="安排补课" ok-text="确认安排" cancel-text="取消"
             :confirm-loading="makeupSaving" @ok="submitMakeup">
      <div class="muted small" style="margin-bottom:12px">学员：{{ makeupTarget?.studentName }} · 原请假 {{ makeupTarget?.leaveNo }}</div>
      <a-form layout="vertical">
        <a-form-item label="补课课次" required>
          <a-select v-model:value="makeupLessonId" show-search option-filter-prop="label" placeholder="选择课次">
            <a-select-option v-for="l in lessons.filter(x => x.status !== 'CANCELLED')" :key="l.id" :value="l.id" :label="lessonTitle(l.id)">
              {{ lessonTitle(l.id) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="makeupNote" :rows="2" placeholder="如：安排在周六同班补课，不重复计费" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
