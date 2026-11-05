<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { courseApi, priceApi, outlineApi, basicApi } from '../api'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const activeTab = ref('courses')
const loading = ref(false)
const courses = ref([])
const courseTotal = ref(0)
const query = reactive({ current: 1, size: 20, keyword: '' })
const categories = ref([])
const stages = ref([])
const classTypes = ref([])

const prices = ref([])
const priceLoading = ref(false)
const priceFilter = ref(undefined)

const outlines = ref([])
const outlineLoading = ref(false)
const outlineCourseId = ref(undefined)

const courseOpen = ref(false)
const courseSaving = ref(false)
const editingCourse = ref(null)
const courseForm = reactive({
  name: '', categoryId: undefined, stageId: undefined, ageRange: '', lessons: 24,
  frequency: '', durationMinutes: 60, coverChar: '', coverColor: '145deg,#75422F,#A05B40',
  description: '', goal: '', tags: '', featured: false, status: 'ON', sort: 0
})

const priceOpen = ref(false)
const priceSaving = ref(false)
const editingPrice = ref(null)
const priceForm = reactive({ courseId: undefined, classTypeId: undefined, lessons: 24, price: 0, unit: 'TERM', status: 'ON', sort: 0 })

const outlineOpen = ref(false)
const outlineSaving = ref(false)
const editingOutline = ref(null)
const outlineForm = reactive({ courseId: undefined, title: '', lessonRange: '', description: '', visible: true, sort: 0 })

const courseColumns = [
  { title: '课程', key: 'name', width: 260 },
  { title: '分类 / 阶段', key: 'cat', width: 150 },
  { title: '适龄段', dataIndex: 'ageRange', key: 'ageRange', width: 90 },
  { title: '课时 / 频率', key: 'lessons', width: 200 },
  { title: '标签', key: 'tags', width: 200 },
  { title: '首页推荐', key: 'featured', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 160 }
]
const priceColumns = [
  { title: '课程', key: 'course', width: 220 },
  { title: '班型', key: 'classType', width: 120 },
  { title: '课时 / 次数', key: 'lessons', width: 100 },
  { title: '价格', key: 'price', width: 110 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 130 }
]
const outlineColumns = [
  { title: '顺序', dataIndex: 'sort', key: 'sort', width: 60 },
  { title: '阶段标题', dataIndex: 'title', key: 'title', width: 140 },
  { title: '课时范围', dataIndex: 'lessonRange', key: 'lessonRange', width: 130 },
  { title: '阶段说明', dataIndex: 'description', key: 'description' },
  { title: '显示', key: 'visible', width: 80 },
  { title: '操作', key: 'actions', width: 190 }
]

const categoryName = (id) => categories.value.find((x) => x.id === id)?.name || '—'
const stageName = (id) => stages.value.find((x) => x.id === id)?.name || '—'
const courseName = (id) => courses.value.find((x) => x.id === id)?.name || '—'
const classTypeName = (id) => classTypes.value.find((x) => x.id === id)?.name || '—'

const filteredPrices = computed(() => priceFilter.value ? prices.value.filter((p) => p.courseId === priceFilter.value) : prices.value)
const filteredOutlines = computed(() => outlineCourseId.value ? outlines.value.filter((o) => o.courseId === outlineCourseId.value) : [])

async function loadCourses() {
  loading.value = true
  try {
    const data = await courseApi.page({ current: query.current, size: query.size, keyword: query.keyword || undefined })
    courses.value = data.records
    courseTotal.value = data.total
    if (!outlineCourseId.value && courses.value.length) outlineCourseId.value = courses.value[0].id
  } finally { loading.value = false }
}

async function loadBasics() {
  const [c, s, t] = await Promise.all([
    basicApi.list(basicApi.categories),
    basicApi.list(basicApi.stages),
    basicApi.list(basicApi.classTypes)
  ])
  categories.value = c
  stages.value = s
  classTypes.value = t
}

async function loadPrices() {
  priceLoading.value = true
  try {
    const data = await priceApi.page({ current: 1, size: 200 })
    prices.value = data.records
  } finally { priceLoading.value = false }
}

async function loadOutlines() {
  outlineLoading.value = true
  try {
    const data = await outlineApi.page({ current: 1, size: 200 })
    outlines.value = data.records
  } finally { outlineLoading.value = false }
}

function openCreateCourse() {
  editingCourse.value = null
  Object.assign(courseForm, {
    name: '', categoryId: categories.value[0]?.id, stageId: stages.value[0]?.id,
    ageRange: '', lessons: 24, frequency: '每周 1 次 × 60 分钟', durationMinutes: 60,
    coverChar: '', coverColor: '145deg,#75422F,#A05B40', description: '', goal: '', tags: '',
    featured: false, status: 'ON', sort: 0
  })
  courseOpen.value = true
}

function openEditCourse(row) {
  editingCourse.value = row
  Object.assign(courseForm, {
    name: row.name, categoryId: row.categoryId, stageId: row.stageId, ageRange: row.ageRange,
    lessons: row.lessons, frequency: row.frequency, durationMinutes: row.durationMinutes,
    coverChar: row.coverChar, coverColor: row.coverColor, description: row.description,
    goal: row.goal, tags: row.tags, featured: row.featured === 1,
    status: row.status, sort: row.sort || 0
  })
  courseOpen.value = true
}

async function submitCourse() {
  if (!courseForm.name || !courseForm.categoryId) { message.warning('请填写课程名称与分类'); return }
  courseSaving.value = true
  try {
    const payload = {
      ...courseForm,
      featured: courseForm.featured ? 1 : 0,
      status: courseForm.status,
      sort: Number(courseForm.sort || 0)
    }
    if (editingCourse.value) {
      await courseApi.update(editingCourse.value.id, payload)
      message.success('课程已保存')
    } else {
      await courseApi.create(payload)
      message.success('课程已创建并上架')
    }
    courseOpen.value = false
    await loadCourses()
  } finally { courseSaving.value = false }
}

async function removeCourse(row) {
  await courseApi.remove(row.id)
  message.success('课程已删除，关联价格与大纲一并删除')
  await loadCourses()
  await Promise.all([loadPrices(), loadOutlines()])
}

function openCreatePrice() {
  editingPrice.value = null
  Object.assign(priceForm, {
    courseId: courses.value[0]?.id, classTypeId: classTypes.value[0]?.id,
    lessons: 24, price: 0, unit: 'TERM', status: 'ON', sort: 0
  })
  priceOpen.value = true
}
function openEditPrice(row) {
  editingPrice.value = row
  Object.assign(priceForm, row)
  priceOpen.value = true
}
async function submitPrice() {
  if (!priceForm.courseId || !priceForm.classTypeId) { message.warning('请选择课程与班型'); return }
  priceSaving.value = true
  try {
    const payload = { ...priceForm, price: Number(priceForm.price), lessons: Number(priceForm.lessons), sort: Number(priceForm.sort || 0) }
    if (editingPrice.value) {
      await priceApi.update(editingPrice.value.id, payload)
      message.success('价格已保存')
    } else {
      await priceApi.create(payload)
      message.success('班型价格已新增')
    }
    priceOpen.value = false
    await loadPrices()
  } finally { priceSaving.value = false }
}
async function removePrice(row) {
  await priceApi.remove(row.id)
  message.success('价格已删除')
  await loadPrices()
}

function openCreateOutline() {
  editingOutline.value = null
  Object.assign(outlineForm, {
    courseId: outlineCourseId.value, title: '', lessonRange: '',
    description: '', visible: true, sort: outlines.value.length + 1
  })
  outlineOpen.value = true
}
function openEditOutline(row) {
  editingOutline.value = row
  Object.assign(outlineForm, { ...row, visible: row.visible === 1 })
  outlineOpen.value = true
}
async function submitOutline() {
  if (!outlineForm.courseId || !outlineForm.title) { message.warning('请选择课程并填写阶段标题'); return }
  outlineSaving.value = true
  try {
    const payload = { ...outlineForm, visible: outlineForm.visible ? 1 : 0, sort: Number(outlineForm.sort || 0) }
    if (editingOutline.value) {
      await outlineApi.update(editingOutline.value.id, payload)
      message.success('大纲阶段已保存')
    } else {
      await outlineApi.create(payload)
      message.success('大纲阶段已新增')
    }
    outlineOpen.value = false
    await loadOutlines()
  } finally { outlineSaving.value = false }
}
async function toggleOutline(row) {
  await outlineApi.update(row.id, { ...row, visible: row.visible === 1 ? 0 : 1 })
  message.success(row.visible === 1 ? '阶段已隐藏，小程序不再展示' : '阶段已显示')
  await loadOutlines()
}
async function removeOutline(row) {
  await outlineApi.remove(row.id)
  message.success('大纲阶段已删除')
  await loadOutlines()
}

function onTab(key) {
  activeTab.value = key
  if (key === 'prices') loadPrices()
  if (key === 'outline') loadOutlines()
}

onMounted(async () => {
  await loadBasics()
  await Promise.all([loadCourses(), loadPrices(), loadOutlines()])
})
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="eyebrow">CURRICULUM & PRICING</div>
        <h1>课程与定价</h1>
        <p>课程、班型价格与内容大纲统一维护，小程序端只读展示</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="onTab(activeTab)"><ReloadOutlined /> 刷新</a-button>
        <a-button type="primary" size="small" @click="openCreateCourse"><PlusOutlined /> 新建课程</a-button>
      </div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <a-tabs :active-key="activeTab" @change="onTab">
        <a-tab-pane key="courses" tab="课程管理">
          <div class="filters">
            <a-input v-model:value="query.keyword" placeholder="搜索课程名称" allow-clear style="width:220px" @press-enter="() => { query.current = 1; loadCourses() }" />
            <a-button size="small" @click="() => { query.current = 1; loadCourses() }">查询</a-button>
            <div class="sp"></div>
            <span class="muted small">共 {{ courseTotal }} 门课程 · 小程序仅展示上架课程</span>
          </div>
          <a-table :columns="courseColumns" :data-source="courses" :loading="loading" row-key="id" size="small"
                   :pagination="{ current: query.current, pageSize: query.size, total: courseTotal, showSizeChanger: false }"
                   @change="(p) => { query.current = p.current; loadCourses() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div style="display:flex;align-items:center;gap:8px">
                  <div style="width:32px;height:32px;border-radius:7px;background:linear-gradient(145deg,#75422F,#A05B40);color:#F8F0DF;display:flex;align-items:center;justify-content:center;font-family:var(--serif);font-size:14px">{{ record.coverChar || record.name.slice(0,1) }}</div>
                  <div><b>{{ record.name }}</b><div class="muted" style="font-size:9px">{{ record.description }}</div></div>
                </div>
              </template>
              <template v-if="column.key === 'cat'">{{ categoryName(record.categoryId) }} · {{ stageName(record.stageId) }}</template>
              <template v-if="column.key === 'lessons'">{{ record.lessons }} 课时<br><span class="muted" style="font-size:9px">{{ record.frequency }}</span></template>
              <template v-if="column.key === 'tags'"><span class="tag">{{ record.tags }}</span></template>
              <template v-if="column.key === 'featured'"><span class="tag" :class="record.featured === 1 ? 'gold' : 'gray'">{{ record.featured === 1 ? '推荐' : '否' }}</span></template>
              <template v-if="column.key === 'status'"><span class="tag" :class="record.status === 'ON' ? 'green' : 'gray'">{{ record.status === 'ON' ? '上架中' : '已下架' }}</span></template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button type="link" size="small" @click="openEditCourse(record)">编辑</a-button>
                  <a-button type="link" size="small" @click="() => { outlineCourseId = record.id; activeTab = 'outline'; loadOutlines() }">大纲</a-button>
                  <a-popconfirm title="删除课程？关联价格与大纲将一并删除" @confirm="removeCourse(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
                </div>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="prices" tab="班型与价格">
          <div class="filters">
            <a-select v-model:value="priceFilter" placeholder="全部课程" allow-clear style="width:200px">
              <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
            </a-select>
            <a-button size="small" @click="loadPrices"><ReloadOutlined /> 刷新</a-button>
            <div class="sp"></div>
            <a-button type="primary" size="small" @click="openCreatePrice"><PlusOutlined /> 新增班型价格</a-button>
          </div>
          <a-table :columns="priceColumns" :data-source="filteredPrices" :loading="priceLoading" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'course'">{{ courseName(record.courseId) }}</template>
              <template v-if="column.key === 'classType'">{{ classTypeName(record.classTypeId) }}</template>
              <template v-if="column.key === 'lessons'">{{ record.lessons }} {{ record.unit === 'TIME' ? '次' : '课时' }}</template>
              <template v-if="column.key === 'price'"><b class="num">¥{{ Number(record.price).toLocaleString() }}</b></template>
              <template v-if="column.key === 'status'"><span class="tag" :class="record.status === 'ON' ? 'green' : 'gray'">{{ record.status === 'ON' ? '在售' : '停售' }}</span></template>
              <template v-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="openEditPrice(record)">编辑</a-button>
                <a-popconfirm title="删除该价格？" @confirm="removePrice(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
              </template>
            </template>
          </a-table>
          <div class="footnote">班型来自「基础数据 → 班型」；同一课程可维护多个班型价格。不设活动价。</div>
        </a-tab-pane>

        <a-tab-pane key="outline" tab="内容大纲">
          <div class="filters">
            <a-select v-model:value="outlineCourseId" style="width:230px" @change="loadOutlines">
              <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
            </a-select>
            <a-button size="small" @click="loadOutlines"><ReloadOutlined /> 刷新</a-button>
            <div class="sp"></div>
            <a-button type="primary" size="small" @click="openCreateOutline"><PlusOutlined /> 新增阶段</a-button>
          </div>
          <a-table :columns="outlineColumns" :data-source="filteredOutlines" :loading="outlineLoading" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'visible'"><a-switch :checked="record.visible === 1" size="small" @change="toggleOutline(record)" /></template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button type="link" size="small" @click="openEditOutline(record)">编辑</a-button>
                  <a-popconfirm title="删除该大纲阶段？" @confirm="removeOutline(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
                </div>
              </template>
            </template>
          </a-table>
          <div class="footnote">大纲阶段支持新增、编辑、删除与显示 / 隐藏；隐藏后小程序课程详情不展示该阶段。</div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <a-modal v-model:open="courseOpen" :title="editingCourse ? '编辑课程' : '新建课程'" ok-text="保存" cancel-text="取消" :confirm-loading="courseSaving" width="640" @ok="submitCourse">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="16"><a-form-item label="课程名称" required><a-input v-model:value="courseForm.name" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="封面字"><a-input v-model:value="courseForm.coverChar" maxlength="1" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="课程分类" required><a-select v-model:value="courseForm.categoryId"><a-select-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="课程阶段"><a-select v-model:value="courseForm.stageId" allow-clear><a-select-option v-for="s in stages" :key="s.id" :value="s.id">{{ s.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="适龄段"><a-input v-model:value="courseForm.ageRange" placeholder="如 5–7 岁" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="总课时"><a-input-number v-model:value="courseForm.lessons" style="width:100%" :min="1" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="单次时长（分钟）"><a-input-number v-model:value="courseForm.durationMinutes" style="width:100%" :min="1" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="上课频率"><a-input v-model:value="courseForm.frequency" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="课程标签"><a-input v-model:value="courseForm.tags" placeholder="逗号分隔，如：小班 6 人,含教材" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="课程简介"><a-textarea v-model:value="courseForm.description" :rows="2" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="教学目标"><a-textarea v-model:value="courseForm.goal" :rows="3" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="courseForm.status"><a-select-option value="ON">上架中</a-select-option><a-select-option value="OFF">已下架</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="推荐到首页"><a-switch v-model:checked="courseForm.featured" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="priceOpen" :title="editingPrice ? '编辑班型价格' : '新增班型价格'" ok-text="保存" cancel-text="取消" :confirm-loading="priceSaving" @ok="submitPrice">
      <a-form layout="vertical">
        <a-form-item label="课程" required><a-select v-model:value="priceForm.courseId"><a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option></a-select></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="班型" required><a-select v-model:value="priceForm.classTypeId"><a-select-option v-for="t in classTypes" :key="t.id" :value="t.id">{{ t.name }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="计价单位"><a-select v-model:value="priceForm.unit"><a-select-option value="TERM">按期（课时包）</a-select-option><a-select-option value="TIME">按次</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="课时 / 次数"><a-input-number v-model:value="priceForm.lessons" style="width:100%" :min="1" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="价格（元）"><a-input-number v-model:value="priceForm.price" style="width:100%" :min="0" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="priceForm.status"><a-select-option value="ON">在售</a-select-option><a-select-option value="OFF">停售</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="排序"><a-input-number v-model:value="priceForm.sort" style="width:100%" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="outlineOpen" :title="editingOutline ? '编辑大纲阶段' : '新增大纲阶段'" ok-text="保存" cancel-text="取消" :confirm-loading="outlineSaving" @ok="submitOutline">
      <a-form layout="vertical">
        <a-form-item label="课程" required><a-select v-model:value="outlineForm.courseId"><a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option></a-select></a-form-item>
        <a-form-item label="阶段标题" required><a-input v-model:value="outlineForm.title" placeholder="如：基本笔画" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="课时范围"><a-input v-model:value="outlineForm.lessonRange" placeholder="如：第 5–10 课时" /></a-form-item></a-col>
          <a-col :span="6"><a-form-item label="排序"><a-input-number v-model:value="outlineForm.sort" style="width:100%" /></a-form-item></a-col>
          <a-col :span="6"><a-form-item label="显示"><a-switch v-model:checked="outlineForm.visible" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="阶段说明"><a-textarea v-model:value="outlineForm.description" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
