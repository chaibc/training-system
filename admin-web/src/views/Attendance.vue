<script setup>
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { attendanceApi, studentApi, lessonApi, classApi } from '../api'
import { ReloadOutlined, EditOutlined } from '@ant-design/icons-vue'

const loading = ref(false)
const records = ref([])
const students = ref([])
const lessons = ref([])
const classes = ref([])
const workflowLessons = ref([])
const lessonLoading = ref(false)
const lessonDate = ref(null)
const lessonStatus = ref(undefined)

const drawerOpen = ref(false)
const rosterLoading = ref(false)
const saving = ref(false)
const rosterLesson = ref(null)
const rosterStudents = ref([])
const rosterSummary = ref({ total: 0, present: 0, leave: 0, absent: 0, unmarked: 0 })

const statusMap = {
  PRESENT: { text: '出勤', cls: 'green' },
  LEAVE: { text: '请假', cls: 'gold' },
  ABSENT: { text: '缺勤', cls: 'red' }
}
const lessonStatusMap = {
  SCHEDULED: { text: '待上课', cls: 'red' },
  DONE: { text: '已完成', cls: 'green' },
  CANCELLED: { text: '已取消', cls: 'gray' }
}
const columns = [
  { title: '学员', key: 'student', width: 110 },
  { title: '课次', key: 'lesson', width: 210 },
  { title: '状态', key: 'status', width: 75 },
  { title: '消课', key: 'hours', width: 75 },
  { title: '记录时间', key: 'recordedAt', width: 130 },
  { title: '备注', dataIndex: 'note', key: 'note' }
]
const lessonColumns = [
  { title: '日期', key: 'date', width: 100 },
  { title: '班级 / 课程', key: 'class', width: 200 },
  { title: '时间', key: 'time', width: 120 },
  { title: '教师 / 教室', key: 'teacher', width: 130 },
  { title: '点名', key: 'roster', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 95 }
]
const studentName = (id) => students.value.find((x) => x.id === id)?.name || `学员 #${id}`
const lessonTitle = (id) => {
  const l = lessons.value.find((x) => x.id === id)
  if (!l) return `课次 #${id}`
  const c = classes.value.find((x) => x.id === l.classId)
  return `${c?.name || '班级'} · 第 ${l.lessonNo} 课`
}
const timeText = (value) => value ? String(value).slice(0, 5) : '—'

async function load() {
  loading.value = true
  try {
    const [a, s, l, c] = await Promise.all([
      attendanceApi.list(), studentApi.options(), lessonApi.list(), classApi.list()
    ])
    records.value = a
    students.value = s
    lessons.value = l
    classes.value = c
  } finally { loading.value = false }
}

async function loadLessons() {
  lessonLoading.value = true
  try {
    workflowLessons.value = await attendanceApi.lessons({
      date: lessonDate.value ? lessonDate.value.format('YYYY-MM-DD') : undefined,
      status: lessonStatus.value || undefined
    })
  } finally { lessonLoading.value = false }
}

async function openRollcall(row) {
  drawerOpen.value = true
  rosterLoading.value = true
  rosterLesson.value = row
  rosterStudents.value = []
  try {
    const data = await attendanceApi.roster(row.id)
    rosterLesson.value = data.lesson
    rosterSummary.value = data.summary
    rosterStudents.value = (data.students || []).map((s) => ({ ...s, mark: s.mark || 'PRESENT' }))
  } finally { rosterLoading.value = false }
}

function recalc() {
  const list = rosterStudents.value
  rosterSummary.value = {
    total: list.length,
    present: list.filter((x) => x.mark === 'PRESENT').length,
    leave: list.filter((x) => x.mark === 'LEAVE').length,
    absent: list.filter((x) => x.mark === 'ABSENT').length,
    unmarked: list.filter((x) => !x.mark).length
  }
}

async function submitRollcall() {
  if (!rosterStudents.value.length) return message.warning('该课次没有可点名学员')
  const unmarked = rosterStudents.value.filter((x) => !x.mark)
  if (unmarked.length) return message.warning('请为每位学员选择出勤状态')
  saving.value = true
  try {
    await attendanceApi.rollcall(rosterLesson.value.id, rosterStudents.value.map((x) => ({
      studentId: x.studentId, status: x.mark, note: x.note
    })))
    message.success('点名完成，消课与课时流水已同步')
    drawerOpen.value = false
    await Promise.all([load(), loadLessons()])
  } finally { saving.value = false }
}

onMounted(() => {
  load()
  loadLessons()
})
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="eyebrow">ROLL CALL & HOUR CONSUMPTION</div>
        <h1>点名与消课</h1>
        <p>按课次点名，出勤与缺勤自动消课；修正点名时按差额调整课时并写入流水</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="load(); loadLessons()"><ReloadOutlined /> 刷新</a-button>
      </div>
    </div>

    <div class="stats">
      <div class="stat-card jade"><div class="lbl">出勤记录</div><div class="v">{{ records.filter(r => r.status === 'PRESENT').length }}<em>条</em></div><div class="foot">出勤自动消课</div></div>
      <div class="stat-card"><div class="lbl">请假记录</div><div class="v">{{ records.filter(r => r.status === 'LEAVE').length }}<em>条</em></div><div class="foot">按审批规则处理</div></div>
      <div class="stat-card red"><div class="lbl">缺勤记录</div><div class="v">{{ records.filter(r => r.status === 'ABSENT').length }}<em>条</em></div><div class="foot">按机构规则消课</div></div>
      <div class="stat-card blue"><div class="lbl">消课课时</div><div class="v">{{ records.reduce((s, r) => s + Number(r.consumedHours || 0), 0) }}<em>课时</em></div><div class="foot">全部记录合计</div></div>
    </div>

    <div class="paper-card" style="padding:16px;margin-bottom:16px">
      <div class="filters" style="margin-bottom:12px">
        <div class="card-title" style="margin:0">按课次点名</div>
        <div class="sp"></div>
        <a-date-picker v-model:value="lessonDate" size="small" placeholder="选择日期" @change="loadLessons" />
        <a-select v-model:value="lessonStatus" size="small" placeholder="全部状态" allow-clear style="width:120px" @change="loadLessons">
          <a-select-option value="SCHEDULED">待上课</a-select-option>
          <a-select-option value="DONE">已完成</a-select-option>
          <a-select-option value="CANCELLED">已取消</a-select-option>
        </a-select>
        <span class="muted small">选择课次后为学员标记出勤状态，支持已点名课次修正</span>
      </div>
      <a-table :columns="lessonColumns" :data-source="workflowLessons" :loading="lessonLoading" row-key="id" size="small"
               :pagination="{ pageSize: 8, showSizeChanger: false }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'date'">
            {{ record.lessonDate }}
            <div class="muted" style="font-size:8px">{{ record.startTime?.slice(0, 5) }}–{{ timeText(record.endTime) }}</div>
          </template>
          <template v-if="column.key === 'class'">
            <b>{{ record.className }}</b>
            <div class="muted" style="font-size:8px">{{ record.courseName }} · 第 {{ record.lessonNo }} 课</div>
          </template>
          <template v-if="column.key === 'time'">{{ timeText(record.startTime) }}–{{ timeText(record.endTime) }}</template>
          <template v-if="column.key === 'teacher'">
            {{ record.teacherName }}
            <div class="muted" style="font-size:8px">{{ record.classroomName || '教室待定' }}</div>
          </template>
          <template v-if="column.key === 'roster'">
            <span class="tag" :class="record.markedCount >= record.rosterCount ? 'green' : 'gold'">{{ record.markedCount }} / {{ record.rosterCount }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <span class="tag" :class="lessonStatusMap[record.status]?.cls">{{ lessonStatusMap[record.status]?.text }}</span>
          </template>
          <template v-if="column.key === 'actions'">
            <a-button type="link" size="small" :disabled="record.status === 'CANCELLED'" @click="openRollcall(record)">
              <EditOutlined /> {{ record.status === 'DONE' ? '修正点名' : '去点名' }}
            </a-button>
          </template>
        </template>
      </a-table>
    </div>

    <div class="paper-card" style="padding:16px">
      <div class="card-title">最近点名记录</div>
      <a-table :columns="columns" :data-source="records" :loading="loading" row-key="id" size="small"
               :pagination="{ pageSize: 8, showSizeChanger: false }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'student'"><b>{{ studentName(record.studentId) }}</b></template>
          <template v-if="column.key === 'lesson'">{{ lessonTitle(record.lessonId) }}</template>
          <template v-if="column.key === 'status'"><span class="tag" :class="statusMap[record.status]?.cls">{{ statusMap[record.status]?.text }}</span></template>
          <template v-if="column.key === 'hours'">{{ record.consumedHours }} 课时</template>
          <template v-if="column.key === 'recordedAt'">{{ record.recordedAt ? dayjs(record.recordedAt).format('MM-DD HH:mm') : '—' }}</template>
        </template>
      </a-table>
      <div class="footnote">点名提交后家长端课表、上课记录与课时账户同步更新；重复提交只按差额修正，不重复扣课。</div>
    </div>

    <a-drawer v-model:open="drawerOpen" :title="`点名 · ${rosterLesson?.className || ''}`" width="640" destroy-on-close>
      <a-spin :spinning="rosterLoading">
        <div v-if="rosterLesson" class="roll-head">
          <div>
            <div class="roll-title">{{ rosterLesson.className }} · 第 {{ rosterLesson.lessonNo }} 课</div>
            <div class="muted small">{{ rosterLesson.lessonDate }} {{ timeText(rosterLesson.startTime) }}–{{ timeText(rosterLesson.endTime) }} · {{ rosterLesson.teacherName }} · {{ rosterLesson.classroomName || '教室待定' }}</div>
          </div>
          <span class="tag" :class="lessonStatusMap[rosterLesson.status]?.cls">{{ lessonStatusMap[rosterLesson.status]?.text }}</span>
        </div>
        <div class="roll-stats">
          <div><b>{{ rosterSummary.total }}</b><span>应到</span></div>
          <div class="jade"><b>{{ rosterSummary.present }}</b><span>出勤</span></div>
          <div class="gold"><b>{{ rosterSummary.leave }}</b><span>请假</span></div>
          <div class="red"><b>{{ rosterSummary.absent }}</b><span>缺勤</span></div>
        </div>
        <div class="roll-list">
          <div v-for="student in rosterStudents" :key="student.studentId" class="roll-row">
            <div class="flex1">
              <b>{{ student.name }}</b>
              <span class="muted" style="font-size:9px;margin-left:8px">{{ student.studentNo }}</span>
              <div v-if="student.pendingLeave" class="leave-hint">有请假申请{{ student.leaveReason ? '：' + student.leaveReason : '' }}</div>
            </div>
            <a-radio-group v-model:value="student.mark" size="small" @change="recalc">
              <a-radio-button value="PRESENT">出勤</a-radio-button>
              <a-radio-button value="LEAVE">请假</a-radio-button>
              <a-radio-button value="ABSENT">缺勤</a-radio-button>
            </a-radio-group>
          </div>
          <a-empty v-if="!rosterStudents.length" description="该班级暂无学员" />
        </div>
        <div class="roll-note">出勤与缺勤自动消 1 课时；请假按已通过的请假规则处理。提交后课次标记为已完成，可再次修正。</div>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:20px">
          <a-button @click="drawerOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="submitRollcall">完成点名并消课</a-button>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.roll-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.roll-title { font-family: "Noto Serif SC", "Songti SC", serif; font-size: 18px; font-weight: 600; margin-bottom: 6px; }
.roll-stats {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin: 18px 0;
  background: #F9F5EC; border-radius: 10px; padding: 12px;
}
.roll-stats > div { text-align: center; }
.roll-stats b { display: block; font-family: Georgia, serif; font-size: 24px; color: var(--ink); }
.roll-stats span { font-size: 10px; color: var(--muted); }
.roll-stats .jade b { color: #45644D; }
.roll-stats .gold b { color: #A9782A; }
.roll-stats .red b { color: #B4473C; }
.roll-list { border: 1px solid #F0E9DC; border-radius: 10px; max-height: 380px; overflow-y: auto; }
.roll-row { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-bottom: 1px solid #F0E9DC; }
.roll-row:last-child { border-bottom: none; }
.leave-hint { font-size: 9px; color: #A9782A; margin-top: 3px; }
.roll-note { margin-top: 14px; padding: 10px 12px; border-radius: 8px; background: rgba(194,160,94,.09); color: #6F5A32; font-size: 10px; line-height: 1.7; }
</style>
