/**
 * 学员管理页：档案分页查询与筛选、新建/编辑档案、档案详情抽屉（课时流水、上课记录、家长与班级）、课时余额调整。
 * 接口：studentApi.page / detail / create / update / adjustHours（api/index.js）
 * 辅助接口：classApi.list（班级筛选下拉）
 *
 * 列表返回的字段不含生日与备注，编辑时会再取一次详情补全；
 * 手机号等敏感信息按最小必要原则展示，完整信息由后端按数据范围控制。
 */
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { studentApi, classApi } from '../api'
import { PlusOutlined, ReloadOutlined, EditOutlined } from '@ant-design/icons-vue'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
// 列表查询条件：分页 + 关键字 + 状态 + 班级；任一条件变化都会把页码重置为 1
const query = reactive({ current: 1, size: 10, keyword: '', status: undefined, classId: undefined })
const classes = ref([])

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const formOpen = ref(false)
const formSaving = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', gender: '男', birthDate: null, school: '', grade: '', status: 'ACTIVE', remark: '' })

const hoursOpen = ref(false)
const hoursSaving = ref(false)
const hoursStudent = ref(null)
const hoursForm = reactive({ type: 'ADD', hours: 1, reason: '' })

// 学员状态码 → 中文标签 + 标签配色
const statusMap = {
  ACTIVE: { text: '在读', cls: 'green' },
  PAUSED: { text: '停课', cls: 'gold' },
  GRADUATED: { text: '结课', cls: 'gray' },
  LEFT: { text: '离校', cls: 'gray' }
}

// 表格列：只有最近上课时间用 customRender 统一格式（MM-DD HH:mm），空值显示 —
const columns = [
  { title: '学员', key: 'student', width: 190 },
  { title: '家长 / 联系方式', key: 'parent', width: 160 },
  { title: '所属班级', key: 'className', width: 160 },
  { title: '课时账户', key: 'hours', width: 150 },
  { title: '有效期至', dataIndex: 'validUntil', key: 'validUntil', width: 100 },
  { title: '状态', key: 'status', width: 75 },
  { title: '最近上课', dataIndex: 'lastLessonAt', key: 'lastLessonAt', width: 110,
    customRender: ({ text }) => text ? dayjs(text).format('MM-DD HH:mm') : '—' },
  { title: '操作', key: 'actions', width: 170 }
]

// 分页查询学员列表；空白的筛选条件转成 undefined，避免后端把空串当成关键字
async function load() {
  loading.value = true
  try {
    const data = await studentApi.page({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      classId: query.classId || undefined
    })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

// 加载班级下拉数据；失败时置空数组，不影响主列表与档案弹窗的使用
async function loadClasses() {
  try { classes.value = await classApi.list() } catch { classes.value = [] }
}

// 打开新建弹窗：重置全部字段（性别默认男、状态默认在读），并清空 editingId
function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', gender: '男', birthDate: null, school: '', grade: '', status: 'ACTIVE', remark: '' })
  formOpen.value = true
}

// 打开编辑弹窗：先用列表行回填可复用的字段，再异步用详情接口补生日与备注
function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, gender: row.gender || '男', birthDate: null,
    school: row.school || '', grade: row.grade || '', status: row.status, remark: ''
  })
  // 详情里拿完整生日与备注
  studentApi.detail(row.id).then((d) => {
    form.birthDate = d.birthDate ? dayjs(d.birthDate) : null
    form.remark = d.remark || ''
  })
  formOpen.value = true
}

// 提交档案：姓名必填；生日是 dayjs 对象，需转成 YYYY-MM-DD 再提交
async function submitForm() {
  if (!form.name) { message.warning('请填写学员姓名'); return }
  formSaving.value = true
  try {
    const payload = {
      name: form.name, gender: form.gender,
      birthDate: form.birthDate ? form.birthDate.format('YYYY-MM-DD') : null,
      school: form.school, grade: form.grade, status: form.status, remark: form.remark
    }
    if (editingId.value) {
      await studentApi.update(editingId.value, payload)
      message.success('学员档案已保存')
    } else {
      await studentApi.create(payload)
      message.success('学员档案已创建')
    }
    formOpen.value = false
    await load()
  } finally {
    formSaving.value = false
  }
}

// 打开档案抽屉：先清空旧数据再请求，避免加载期间闪出上一个学员的档案
async function openDetail(row) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try { detail.value = await studentApi.detail(row.id) } finally { detailLoading.value = false }
}

// 打开课时调整弹窗：默认「补发 1 课时」，原因留空强制重新填写
function openHours(row) {
  hoursStudent.value = row
  Object.assign(hoursForm, { type: 'ADD', hours: 1, reason: '' })
  hoursOpen.value = true
}

// 提交课时调整：原因必填（会写进课时流水），数量由输入框限制为 0.5 的倍数
async function submitHours() {
  if (!hoursForm.reason) { message.warning('请填写调整原因'); return }
  hoursSaving.value = true
  try {
    await studentApi.adjustHours(hoursStudent.value.id, {
      type: hoursForm.type, hours: Number(hoursForm.hours), reason: hoursForm.reason
    })
    message.success('课时调整已保存，已写入课时流水与操作记录')
    hoursOpen.value = false
    await load()
    // 抽屉正开着同一个学员时，同步刷新抽屉里的课时与流水，避免数字对不上
    if (detail.value?.id === hoursStudent.value.id) openDetail(hoursStudent.value)
  } finally {
    hoursSaving.value = false
  }
}

// 进入页面并发加载学员列表与班级下拉
onMounted(async () => { await Promise.all([load(), loadClasses()]) })
</script>

<template>
  <div>
    <!-- 页头：标题、刷新与新建学员 -->
    <div class="page-head">
      <div>
        <div class="eyebrow">STUDENT RECORDS</div>
        <h1>学员管理</h1>
        <p>档案、班级、课时账户与状态管理 · 敏感信息脱敏展示</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="load"><ReloadOutlined /> 刷新</a-button>
        <a-button type="primary" size="small" @click="openCreate"><PlusOutlined /> 新建学员档案</a-button>
      </div>
    </div>

    <div class="paper-card" style="padding:16px">
      <!-- 顶部筛选区：关键字、班级、状态；回车或点击查询都会重置到第一页 -->
      <div class="filters">
        <a-input v-model:value="query.keyword" placeholder="搜索学员姓名 / 学员编号" allow-clear style="width:220px" @press-enter="() => { query.current = 1; load() }" />
        <a-select v-model:value="query.classId" placeholder="全部班级" allow-clear style="width:160px" @change="() => { query.current = 1; load() }">
          <a-select-option v-for="c in classes" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
        </a-select>
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear style="width:120px" @change="() => { query.current = 1; load() }">
          <a-select-option value="ACTIVE">在读</a-select-option>
          <a-select-option value="PAUSED">停课</a-select-option>
          <a-select-option value="GRADUATED">结课</a-select-option>
          <a-select-option value="LEFT">离校</a-select-option>
        </a-select>
        <a-button size="small" @click="() => { query.current = 1; load() }">查询</a-button>
        <div class="sp"></div>
        <span class="muted small">共 {{ total }} 名学员</span>
      </div>

      <!-- 学员表格：课时账户列剩 3 课时及以下标红，分页由后端返回的 total 驱动 -->
      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="small"
               :pagination="{ current: query.current, pageSize: query.size, total, showSizeChanger: false }"
               @change="(p) => { query.current = p.current; load() }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'student'">
            <div style="display:flex;align-items:center;gap:8px">
              <a-avatar :size="30" style="background:#7A5B3D;font-size:11px">{{ record.name.slice(0,1) }}</a-avatar>
              <div><b>{{ record.name }}</b><div class="muted" style="font-size:9px">{{ record.gender }} · {{ record.age }} 岁 · {{ record.studentNo }}</div></div>
            </div>
          </template>
          <template v-if="column.key === 'parent'">
            {{ record.parentName }}<div class="muted" style="font-size:9px">{{ record.parentPhone || '—' }}</div>
          </template>
          <template v-if="column.key === 'className'">{{ record.className }}</template>
          <template v-if="column.key === 'hours'">
            <span class="tag" :class="Number(record.remainingHours) <= 3 ? 'red' : 'green'">剩 {{ record.remainingHours }} / 总 {{ record.totalHours }}</span>
            <a-progress :percent="record.totalHours ? Math.round(record.remainingHours / record.totalHours * 100) : 0" :show-info="false" size="small" :stroke-color="Number(record.remainingHours) <= 3 ? '#B4473C' : '#5C7A63'" />
          </template>
          <template v-if="column.key === 'status'"><span class="tag" :class="statusMap[record.status]?.cls">{{ statusMap[record.status]?.text || record.status }}</span></template>
          <template v-if="column.key === 'actions'">
            <div class="ops">
              <a-button type="link" size="small" @click="openDetail(record)">档案</a-button>
              <a-button type="link" size="small" @click="openEdit(record)"><EditOutlined /> 编辑</a-button>
              <a-button type="link" size="small" @click="openHours(record)">调整课时</a-button>
            </div>
          </template>
        </template>
      </a-table>
      <div class="footnote">学员状态变更自动记录时间与操作人 · 手机号等敏感信息按最小必要原则展示 · 家长端仅可见本子女档案</div>
    </div>

    <!-- 档案详情抽屉：基本信息 + 课时流水 / 上课记录 / 家长与班级 三个页签 -->
    <a-drawer v-model:open="detailOpen" title="学员档案" width="620" destroy-on-close>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
            <a-avatar :size="48" style="background:#7A5B3D;font-size:18px">{{ detail.name.slice(0,1) }}</a-avatar>
            <div style="flex:1">
              <div style="font-family:var(--serif);font-size:18px">{{ detail.name }}</div>
              <div class="muted" style="font-size:10px;margin-top:3px">{{ detail.studentNo }} · {{ detail.school || '—' }} / {{ detail.grade || '—' }}</div>
            </div>
            <span class="tag" :class="statusMap[detail.status]?.cls">{{ statusMap[detail.status]?.text || detail.status }}</span>
          </div>
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="性别 / 生日">{{ detail.gender || '—' }} / {{ detail.birthDate || '—' }}</a-descriptions-item>
            <a-descriptions-item label="剩余课时">{{ detail.remainingHours }} / {{ detail.totalHours }}</a-descriptions-item>
            <a-descriptions-item label="有效期至">{{ detail.validUntil || '—' }}</a-descriptions-item>
            <a-descriptions-item label="家长">{{ detail.parents.map(p => p.name + '（' + p.relation + '）').join('、') || '未绑定' }}</a-descriptions-item>
            <a-descriptions-item label="班级" :span="2">{{ detail.classes.map(c => c.className).join('、') || '未分班' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</a-descriptions-item>
          </a-descriptions>
          <!-- 详情页签：课时流水是余额变动依据，上课记录对应点名结果 -->
          <a-tabs style="margin-top:16px">
            <a-tab-pane key="ledger" tab="课时流水">
              <a-table :data-source="detail.ledger" :pagination="false" row-key="id" size="small">
                <a-table-column title="类型" data-index="type" key="type" width="100" />
                <a-table-column title="变动" key="change" width="80">
                  <template #default="{ record }"><b :style="{ color: Number(record.changeHours) >= 0 ? 'var(--jade)' : 'var(--red)' }">{{ Number(record.changeHours) > 0 ? '+' : '' }}{{ record.changeHours }}</b></template>
                </a-table-column>
                <a-table-column title="余额" data-index="balanceAfter" key="balanceAfter" width="80" />
                <a-table-column title="原因" data-index="reason" key="reason" />
                <a-table-column title="时间" key="time" width="130"><template #default="{ record }">{{ record.createdAt ? dayjs(record.createdAt).format('MM-DD HH:mm') : '—' }}</template></a-table-column>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="attendance" tab="上课记录">
              <a-table :data-source="detail.attendance" :pagination="false" row-key="id" size="small">
                <a-table-column title="课次" data-index="lessonTitle" key="lessonTitle" />
                <a-table-column title="状态" data-index="status" key="status" width="80" />
                <a-table-column title="消课" data-index="consumedHours" key="consumedHours" width="70" />
                <a-table-column title="时间" key="time" width="130"><template #default="{ record }">{{ record.recordedAt ? dayjs(record.recordedAt).format('MM-DD HH:mm') : '—' }}</template></a-table-column>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="parents" tab="家长与班级">
              <div class="card-title">家长绑定</div>
              <div v-for="p in detail.parents" :key="p.userId" style="display:flex;align-items:center;gap:8px;padding:7px 0;border-bottom:1px dashed var(--line)">
                <b>{{ p.name }}</b><span class="tag">{{ p.relation }}</span><span v-if="p.primary" class="tag gold">主联系人</span><span class="muted" style="font-size:9px">{{ p.phone }}</span>
              </div>
              <div class="card-title" style="margin-top:14px">班级</div>
              <div v-for="c in detail.classes" :key="c.classId" style="font-size:10.5px;line-height:1.9">{{ c.className }} · {{ c.courseName }} · {{ c.teacherName }}</div>
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
    </a-drawer>

    <!-- 新建/编辑档案弹窗：两种状态共用一套表单，靠 editingId 判断 -->
    <a-modal v-model:open="formOpen" :title="editingId ? '编辑学员' : '新建学员档案'" ok-text="保存" cancel-text="取消" :confirm-loading="formSaving" @ok="submitForm">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="学员姓名" required><a-input v-model:value="form.name" placeholder="真实姓名" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="性别"><a-select v-model:value="form.gender"><a-select-option value="男">男</a-select-option><a-select-option value="女">女</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="出生日期"><a-date-picker v-model:value="form.birthDate" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="form.status"><a-select-option value="ACTIVE">在读</a-select-option><a-select-option value="PAUSED">停课</a-select-option><a-select-option value="GRADUATED">结课</a-select-option><a-select-option value="LEFT">离校</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="就读学校"><a-input v-model:value="form.school" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="年级 / 班级"><a-input v-model:value="form.grade" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 课时调整弹窗：补发/扣减/修正三种类型，提交后写流水 -->
    <a-modal v-model:open="hoursOpen" title="调整课时" ok-text="确认调整" cancel-text="取消" :confirm-loading="hoursSaving" @ok="submitHours">
      <a-form layout="vertical">
        <a-form-item label="学员"><a-input :value="hoursStudent?.name" disabled /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="调整类型"><a-select v-model:value="hoursForm.type"><a-select-option value="ADD">补发课时</a-select-option><a-select-option value="DEDUCT">扣减课时</a-select-option><a-select-option value="CORRECT">修正消课</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="数量（课时）"><a-input-number v-model:value="hoursForm.hours" style="width:100%" :min="0.5" :step="0.5" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="调整原因" required><a-textarea v-model:value="hoursForm.reason" :rows="3" placeholder="请填写调整原因，便于对账与追溯" /></a-form-item>
        <div class="login-note">操作将写入课时流水与操作记录；修正消课可填写正负数量。</div>
      </a-form>
    </a-modal>
  </div>
</template>
