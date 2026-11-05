<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { classApi, lessonApi, courseApi, teacherApi, basicApi, studentApi } from '../api'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const activeTab = ref('classes')
const classLoading = ref(false)
const classes = ref([])
const courses = ref([])
const teachers = ref([])
const classrooms = ref([])
const lessonLoading = ref(false)
const lessons = ref([])
const weekDate = ref(dayjs())
const weekDays = computed(() => {
  const base = dayjs(weekDate.value)
  const weekday = base.day() || 7
  const monday = base.subtract(weekday - 1, 'day').startOf('day')
  return Array.from({ length: 7 }).map((_, i) => {
    const date = monday.add(i, 'day')
    const key = date.format('YYYY-MM-DD')
    return {
      key,
      label: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][i],
      day: date.format('MM-DD'),
      today: key === dayjs().format('YYYY-MM-DD'),
      lessons: lessons.value.filter((l) => l.lessonDate === key).sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
    }
  })
})
function changeWeek(delta) {
  weekDate.value = weekDate.value.add(delta, 'week')
}
function timeShort(value) { return value ? String(value).slice(0, 5) : '—' }
const classCounts = ref({})
const rosterOpen = ref(false)
const rosterLoading = ref(false)
const roster = ref(null)
const studentOptions = ref([])
const addStudentId = ref(undefined)
const addStudentSaving = ref(false)

const classOpen = ref(false)
const classSaving = ref(false)
const editingClass = ref(null)
const classForm = reactive({ name: '', courseId: undefined, teacherId: undefined, classroomId: undefined, weekday: 6, startTime: null, endTime: null, capacity: 12, status: 'ACTIVE' })

const lessonOpen = ref(false)
const lessonSaving = ref(false)
const editingLesson = ref(null)
const lessonForm = reactive({ classId: undefined, lessonNo: 1, lessonDate: null, startTime: null, endTime: null, teacherId: undefined, classroomId: undefined, status: 'SCHEDULED', note: '' })

const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const classColumns = [
  { title: '班级', dataIndex: 'name', key: 'name', width: 170 },
  { title: '课程', key: 'course', width: 160 },
  { title: '教师', key: 'teacher', width: 90 },
  { title: '固定时间', key: 'time', width: 130 },
  { title: '教室', key: 'room', width: 80 },
  { title: '学员 / 容量', key: 'count', width: 95 },
  { title: '状态', key: 'status', width: 75 },
  { title: '操作', key: 'actions', width: 190 }
]
const lessonColumns = [
  { title: '课次', key: 'lesson', width: 230 },
  { title: '日期 / 时间', key: 'time', width: 180 },
  { title: '教师', key: 'teacher', width: 100 },
  { title: '教室', key: 'room', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 120 }
]

const courseName = (id) => courses.value.find((x) => x.id === id)?.name || '—'
const teacherName = (id) => teachers.value.find((x) => x.id === id)?.name || '—'
const roomName = (id) => classrooms.value.find((x) => x.id === id)?.name || '—'
const className = (id) => classes.value.find((x) => x.id === id)?.name || '—'

async function loadBasics() {
  const [c, t, r] = await Promise.all([courseApi.list(), teacherApi.list(), basicApi.list(basicApi.classrooms)])
  courses.value = c
  teachers.value = t
  classrooms.value = r
}
async function loadClasses() {
  classLoading.value = true
  try {
    const data = await classApi.page({ current: 1, size: 200 })
    classes.value = data.records
    const counts = {}
    await Promise.all(data.records.map(async (row) => {
      try {
        const info = await classApi.students(row.id)
        counts[row.id] = info.count
      } catch (error) {
        counts[row.id] = 0
      }
    }))
    classCounts.value = counts
  } finally { classLoading.value = false }
}

async function openRoster(row) {
  rosterOpen.value = true
  rosterLoading.value = true
  roster.value = null
  addStudentId.value = undefined
  try {
    roster.value = await classApi.students(row.id)
    if (!studentOptions.value.length) studentOptions.value = await studentApi.options()
  } finally { rosterLoading.value = false }
}

async function addRosterStudent() {
  if (!addStudentId.value) return message.warning('请选择学员')
  addStudentSaving.value = true
  try {
    roster.value = await classApi.addStudent(roster.value.classId, addStudentId.value)
    classCounts.value[roster.value.classId] = roster.value.count
    addStudentId.value = undefined
    message.success('学员已加入班级')
  } finally { addStudentSaving.value = false }
}

async function removeRosterStudent(row) {
  roster.value = await classApi.removeStudent(roster.value.classId, row.id)
  classCounts.value[roster.value.classId] = roster.value.count
  message.success('学员已移出班级')
}

const availableStudents = () => {
  if (!roster.value) return []
  const ids = new Set(roster.value.students.map((x) => x.studentId))
  return studentOptions.value.filter((x) => !ids.has(x.id))
}
async function loadLessons() {
  lessonLoading.value = true
  try {
    const data = await lessonApi.page({ current: 1, size: 200 })
    lessons.value = data.records
  } finally { lessonLoading.value = false }
}

function openCreateClass() {
  editingClass.value = null
  Object.assign(classForm, {
    name: '', courseId: courses.value[0]?.id, teacherId: teachers.value[0]?.id,
    classroomId: classrooms.value[0]?.id, weekday: 6,
    startTime: dayjs('09:00', 'HH:mm'), endTime: dayjs('10:00', 'HH:mm'),
    capacity: 12, status: 'ACTIVE'
  })
  classOpen.value = true
}
function openEditClass(row) {
  editingClass.value = row
  Object.assign(classForm, {
    ...row,
    startTime: row.startTime ? dayjs(row.startTime, 'HH:mm:ss') : null,
    endTime: row.endTime ? dayjs(row.endTime, 'HH:mm:ss') : null
  })
  classOpen.value = true
}
async function submitClass() {
  if (!classForm.name) { message.warning('请填写班级名称'); return }
  classSaving.value = true
  try {
    const payload = {
      ...classForm,
      startTime: classForm.startTime?.format('HH:mm:ss'),
      endTime: classForm.endTime?.format('HH:mm:ss'),
      capacity: Number(classForm.capacity)
    }
    if (editingClass.value) await classApi.update(editingClass.value.id, payload)
    else await classApi.create(payload)
    message.success('班级已保存')
    classOpen.value = false
    await loadClasses()
  } finally { classSaving.value = false }
}
async function removeClass(row) {
  await classApi.remove(row.id)
  message.success('班级已删除')
  await loadClasses()
}

function openCreateLesson() {
  editingLesson.value = null
  Object.assign(lessonForm, {
    classId: classes.value[0]?.id, lessonNo: 1, lessonDate: dayjs(),
    startTime: dayjs('09:00', 'HH:mm'), endTime: dayjs('10:00', 'HH:mm'),
    teacherId: teachers.value[0]?.id, classroomId: classrooms.value[0]?.id,
    status: 'SCHEDULED', note: ''
  })
  lessonOpen.value = true
}
function openEditLesson(row) {
  editingLesson.value = row
  Object.assign(lessonForm, {
    ...row,
    lessonDate: row.lessonDate ? dayjs(row.lessonDate) : null,
    startTime: row.startTime ? dayjs(row.startTime, 'HH:mm:ss') : null,
    endTime: row.endTime ? dayjs(row.endTime, 'HH:mm:ss') : null
  })
  lessonOpen.value = true
}
async function submitLesson() {
  if (!lessonForm.classId) { message.warning('请选择班级'); return }
  lessonSaving.value = true
  try {
    const payload = {
      ...lessonForm,
      lessonNo: Number(lessonForm.lessonNo),
      lessonDate: lessonForm.lessonDate?.format('YYYY-MM-DD'),
      startTime: lessonForm.startTime?.format('HH:mm:ss'),
      endTime: lessonForm.endTime?.format('HH:mm:ss')
    }
    if (editingLesson.value) await lessonApi.update(editingLesson.value.id, payload)
    else await lessonApi.create(payload)
    message.success('课次已保存')
    lessonOpen.value = false
    await loadLessons()
  } finally { lessonSaving.value = false }
}
async function removeLesson(row) {
  await lessonApi.remove(row.id)
  message.success('课次已删除')
  await loadLessons()
}

function onTab(key) {
  activeTab.value = key
  if (key === 'lessons') loadLessons()
}

onMounted(async () => {
  await loadBasics()
  await Promise.all([loadClasses(), loadLessons()])
})
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="eyebrow">CLASSES & SCHEDULING</div>
        <h1>班级与排课</h1>
        <p>班级学员、周课次与排课冲突校验 · 教师/教室/学员时间冲突自动拦截</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="onTab(activeTab)"><ReloadOutlined /> 刷新</a-button>
        <a-button v-if="activeTab === 'classes'" type="primary" size="small" @click="openCreateClass"><PlusOutlined /> 新建班级</a-button>
        <a-button v-else type="primary" size="small" @click="openCreateLesson"><PlusOutlined /> 新建课次</a-button>
      </div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <a-tabs :active-key="activeTab" @change="onTab">
        <a-tab-pane key="classes" tab="班级列表">
          <a-table :columns="classColumns" :data-source="classes" :loading="classLoading" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'course'">{{ courseName(record.courseId) }}</template>
              <template v-if="column.key === 'teacher'">{{ teacherName(record.teacherId) }}</template>
              <template v-if="column.key === 'time'">{{ weekdays[(record.weekday || 1) - 1] }} {{ record.startTime?.slice(0,5) }}–{{ record.endTime?.slice(0,5) }}</template>
              <template v-if="column.key === 'room'">{{ roomName(record.classroomId) }}</template>
              <template v-if="column.key === 'count'">
                <span class="tag" :class="classCounts[record.id] >= record.capacity ? 'red' : 'green'">{{ classCounts[record.id] ?? 0 }} / {{ record.capacity }}</span>
              </template>
              <template v-if="column.key === 'status'"><span class="tag" :class="record.status === 'ACTIVE' ? 'green' : 'gray'">{{ record.status === 'ACTIVE' ? '在读' : record.status }}</span></template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button type="link" size="small" @click="openRoster(record)">学员</a-button>
                  <a-button type="link" size="small" @click="openEditClass(record)">编辑</a-button>
                  <a-popconfirm title="删除班级？" @confirm="removeClass(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
                </div>
              </template>
            </template>
          </a-table>
          <div class="footnote">点击「学员」维护班级名单与容量；新建/编辑课次时自动校验教师、教室与学员时间冲突。</div>
        </a-tab-pane>

        <a-tab-pane key="week" tab="周课表">
          <div class="week-toolbar">
            <a-button size="small" @click="changeWeek(-1)">‹ 上一周</a-button>
            <a-date-picker v-model:value="weekDate" size="small" :allow-clear="false" />
            <a-button size="small" @click="changeWeek(1)">下一周 ›</a-button>
            <span class="muted small">点击课次可编辑；冲突校验在保存时执行</span>
          </div>
          <div class="week-board">
            <div v-for="day in weekDays" :key="day.key" class="week-col" :class="{ today: day.today }">
              <div class="week-col-head">
                <b>{{ day.label }}</b><span>{{ day.day }}</span>
              </div>
              <div class="week-col-body">
                <div v-for="lesson in day.lessons" :key="lesson.id" class="week-lesson" @click="openEditLesson(lesson)">
                  <div class="wl-time">{{ timeShort(lesson.startTime) }}–{{ timeShort(lesson.endTime) }}</div>
                  <div class="wl-name">{{ className(lesson.classId) }}</div>
                  <div class="wl-meta">{{ teacherName(lesson.teacherId) }} · {{ roomName(lesson.classroomId) }}</div>
                  <span class="tag" :class="lesson.status === 'DONE' ? 'green' : lesson.status === 'CANCELLED' ? 'red' : 'gold'">{{ lesson.status === 'DONE' ? '已完成' : lesson.status === 'CANCELLED' ? '已取消' : '待上课' }}</span>
                </div>
                <div v-if="!day.lessons.length" class="week-empty">—</div>
              </div>
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="lessons" tab="课次列表">
          <a-table :columns="lessonColumns" :data-source="lessons" :loading="lessonLoading" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'lesson'"><b>{{ className(record.classId) }}</b><div class="muted" style="font-size:9px">第 {{ record.lessonNo }} 课</div></template>
              <template v-if="column.key === 'time'">{{ record.lessonDate }} · {{ record.startTime?.slice(0,5) }}–{{ record.endTime?.slice(0,5) }}</template>
              <template v-if="column.key === 'teacher'">{{ teacherName(record.teacherId) }}</template>
              <template v-if="column.key === 'room'">{{ roomName(record.classroomId) }}</template>
              <template v-if="column.key === 'status'"><span class="tag" :class="record.status === 'DONE' ? 'green' : record.status === 'CANCELLED' ? 'red' : ''">{{ record.status === 'SCHEDULED' ? '待上课' : record.status === 'DONE' ? '已完成' : record.status }}</span></template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button type="link" size="small" @click="openEditLesson(record)">编辑</a-button>
                  <a-popconfirm title="删除课次？" @confirm="removeLesson(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
                </div>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>

    <a-modal v-model:open="classOpen" :title="editingClass ? '编辑班级' : '新建班级'" ok-text="保存" cancel-text="取消" :confirm-loading="classSaving" @ok="submitClass">
      <a-form layout="vertical">
        <a-form-item label="班级名称" required><a-input v-model:value="classForm.name" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="课程" required><a-select v-model:value="classForm.courseId"><a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="任课教师" required><a-select v-model:value="classForm.teacherId"><a-select-option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="教室"><a-select v-model:value="classForm.classroomId" allow-clear><a-select-option v-for="r in classrooms" :key="r.id" :value="r.id">{{ r.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="班级容量"><a-input-number v-model:value="classForm.capacity" style="width:100%" :min="1" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="固定星期"><a-select v-model:value="classForm.weekday"><a-select-option v-for="(w,i) in weekdays" :key="i" :value="i+1">{{ w }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="开始时间"><a-time-picker v-model:value="classForm.startTime" format="HH:mm" style="width:100%" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="结束时间"><a-time-picker v-model:value="classForm.endTime" format="HH:mm" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="classForm.status"><a-select-option value="ACTIVE">在读</a-select-option><a-select-option value="FULL">满班</a-select-option><a-select-option value="FINISHED">已结课</a-select-option></a-select></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="lessonOpen" :title="editingLesson ? '编辑课次' : '新建课次'" ok-text="保存" cancel-text="取消" :confirm-loading="lessonSaving" @ok="submitLesson">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="班级" required><a-select v-model:value="lessonForm.classId"><a-select-option v-for="c in classes" :key="c.id" :value="c.id">{{ c.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="课次序号"><a-input-number v-model:value="lessonForm.lessonNo" style="width:100%" :min="1" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="上课日期"><a-date-picker v-model:value="lessonForm.lessonDate" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="lessonForm.status"><a-select-option value="SCHEDULED">待上课</a-select-option><a-select-option value="DONE">已完成</a-select-option><a-select-option value="CANCELLED">已取消</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="开始时间"><a-time-picker v-model:value="lessonForm.startTime" format="HH:mm" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="结束时间"><a-time-picker v-model:value="lessonForm.endTime" format="HH:mm" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="任课教师"><a-select v-model:value="lessonForm.teacherId"><a-select-option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="教室"><a-select v-model:value="lessonForm.classroomId" allow-clear><a-select-option v-for="r in classrooms" :key="r.id" :value="r.id">{{ r.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="备注"><a-textarea v-model:value="lessonForm.note" :rows="2" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="rosterOpen" :title="roster ? `${roster.className} · 学员名单` : '班级学员'" width="620">
      <a-spin :spinning="rosterLoading">
        <template v-if="roster">
          <div class="roster-meta">
            <span>容量：<b>{{ roster.count }} / {{ roster.capacity }}</b></span>
            <span>剩余名额：<b>{{ roster.remaining }}</b></span>
          </div>
          <div class="roster-add">
            <a-select v-model:value="addStudentId" show-search option-filter-prop="label" placeholder="选择学员加入班级" style="flex:1">
              <a-select-option v-for="s in availableStudents()" :key="s.id" :value="s.id" :label="s.name + ' ' + (s.studentNo || '')">
                {{ s.name }} <span class="muted">{{ s.studentNo }}</span>
              </a-select-option>
            </a-select>
            <a-button type="primary" :loading="addStudentSaving" @click="addRosterStudent">加入班级</a-button>
          </div>
          <a-table :data-source="roster.students" row-key="id" size="small" :pagination="false">
            <a-table-column title="学员" key="student" width="130">
              <template #default="{ record }"><b>{{ record.name }}</b><div class="muted" style="font-size:8px">{{ record.studentNo }}</div></template>
            </a-table-column>
            <a-table-column title="学校 / 年级" key="school">
              <template #default="{ record }">{{ record.school || '—' }} {{ record.grade ? '/ ' + record.grade : '' }}</template>
            </a-table-column>
            <a-table-column title="剩余课时" key="hours" width="90">
              <template #default="{ record }">{{ record.remainingHours }}</template>
            </a-table-column>
            <a-table-column title="入班时间" data-index="joinedAt" key="joinedAt" width="100" />
            <a-table-column title="操作" key="ops" width="80">
              <template #default="{ record }">
                <a-popconfirm title="将该学员移出班级？" @confirm="removeRosterStudent(record)">
                  <a-button type="link" size="small" danger>移出</a-button>
                </a-popconfirm>
              </template>
            </a-table-column>
          </a-table>
          <div class="footnote">移出班级不影响报名与课时记录；如需退班请同时在报名记录中处理。</div>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.roster-meta { display: flex; gap: 20px; font-size: 11px; color: var(--muted); margin-bottom: 12px; }
.roster-meta b { color: var(--ink); }
.roster-add { display: flex; gap: 8px; margin-bottom: 14px; }
.week-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.week-board { display: grid; grid-template-columns: repeat(7, minmax(120px, 1fr)); gap: 8px; overflow-x: auto; padding-bottom: 8px; }
.week-col { background: #FBF7EE; border: 1px solid #F0E9DC; border-radius: 9px; min-height: 430px; }
.week-col.today { border-color: rgba(180, 71, 60, .35); background: rgba(180, 71, 60, .025); }
.week-col-head { display: flex; align-items: baseline; justify-content: space-between; padding: 10px 10px 8px; border-bottom: 1px solid #F0E9DC; }
.week-col-head b { font-size: 11px; }
.week-col-head span { font-size: 9px; color: #948C7C; }
.week-col-body { padding: 8px; display: flex; flex-direction: column; gap: 6px; }
.week-lesson { background: #FFFDF8; border: 1px solid #F0E9DC; border-radius: 7px; padding: 8px; cursor: pointer; }
.week-lesson:hover { border-color: #C2A05E; }
.wl-time { font-size: 9px; color: #A9782A; font-weight: 600; }
.wl-name { font-size: 10px; font-weight: 600; margin-top: 3px; line-height: 1.35; }
.wl-meta { font-size: 8px; color: #948C7C; margin: 3px 0 5px; }
.week-empty { text-align: center; color: #C9BFAE; font-size: 11px; padding: 18px 0; }
</style>
