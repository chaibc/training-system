/**
 * 课程与定价页：三个页签 —— 课程管理（分页/筛选/新建/编辑/删除）、班型与价格（增删改）、
 * 内容大纲（增删改 + 显示隐藏切换）。
 * 接口：courseApi.page/create/update/remove、priceApi.page/create/update/remove、
 *       outlineApi.page/create/update/remove（api/index.js）
 * 辅助接口：basicApi.list（课程分类 / 课程阶段 / 班型字典）
 *
 * 课程与大纲的布尔字段在页面上是 true/false，后端存 1/0，提交时统一转换；
 * 删除课程会级联删除其价格与大纲（后端行为），因此删除后两个列表都要重新加载。
 */
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { courseApi, priceApi, outlineApi, basicApi } from '../api'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'

// 当前页签：courses / prices / outline；页头刷新按钮复用 onTab(activeTab)
const activeTab = ref('courses')
const loading = ref(false)
const courses = ref([])
const courseTotal = ref(0)
// 课程列表查询条件（服务端分页，默认 20 条）
const query = reactive({ current: 1, size: 20, keyword: '' })
// 三个基础字典：用于表格里把 ID 显示成名称，也用于表单下拉
const categories = ref([])
const stages = ref([])
const classTypes = ref([])

// 班型价格列表（一次性取回，前端按课程过滤）
const prices = ref([])
const priceLoading = ref(false)
// 价格页签的课程筛选值；为空表示显示全部课程的价格
const priceFilter = ref(undefined)

// 大纲列表（一次性取回，前端按课程过滤）
const outlines = ref([])
const outlineLoading = ref(false)
// 大纲页签当前选中的课程；课程列表首次加载后会自动选中第一门，保证页签有内容
const outlineCourseId = ref(undefined)

const courseOpen = ref(false)
const courseSaving = ref(false)
const editingCourse = ref(null)
// 课程表单：featured 用布尔（提交时转 1/0），coverColor 是首页封面渐变色的预设值
const courseForm = reactive({
  name: '', categoryId: undefined, stageId: undefined, ageRange: '', lessons: 24,
  frequency: '', durationMinutes: 60, coverChar: '', coverColor: '145deg,#75422F,#A05B40',
  description: '', goal: '', tags: '', featured: false, status: 'ON', sort: 0
})

const priceOpen = ref(false)
const priceSaving = ref(false)
const editingPrice = ref(null)
// 班型价格表单：unit 区分按期（TERM，课时包）与按次（TIME）计价
const priceForm = reactive({ courseId: undefined, classTypeId: undefined, lessons: 24, price: 0, unit: 'TERM', status: 'ON', sort: 0 })

const outlineOpen = ref(false)
const outlineSaving = ref(false)
const editingOutline = ref(null)
// 大纲阶段表单：visible 用布尔，lessonRange 是文本（如「第 5–10 课时」）
const outlineForm = reactive({ courseId: undefined, title: '', lessonRange: '', description: '', visible: true, sort: 0 })

// 课程表格列：课程名/分类/课时等列靠 bodyCell 组合展示
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
// 班型价格表格列
const priceColumns = [
  { title: '课程', key: 'course', width: 220 },
  { title: '班型', key: 'classType', width: 120 },
  { title: '课时 / 次数', key: 'lessons', width: 100 },
  { title: '价格', key: 'price', width: 110 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 130 }
]
// 内容大纲表格列
const outlineColumns = [
  { title: '顺序', dataIndex: 'sort', key: 'sort', width: 60 },
  { title: '阶段标题', dataIndex: 'title', key: 'title', width: 140 },
  { title: '课时范围', dataIndex: 'lessonRange', key: 'lessonRange', width: 130 },
  { title: '阶段说明', dataIndex: 'description', key: 'description' },
  { title: '显示', key: 'visible', width: 80 },
  { title: '操作', key: 'actions', width: 190 }
]

// 字典 ID → 名称：分类、阶段、课程、班型四个查表函数，查不到统一显示 —
const categoryName = (id) => categories.value.find((x) => x.id === id)?.name || '—'
// 课程阶段 ID → 阶段名；查不到显示 —
const stageName = (id) => stages.value.find((x) => x.id === id)?.name || '—'
// 课程 ID → 课程名；查不到显示 —
const courseName = (id) => courses.value.find((x) => x.id === id)?.name || '—'
// 班型 ID → 班型名；查不到显示 —
const classTypeName = (id) => classTypes.value.find((x) => x.id === id)?.name || '—'

// 价格按选中课程过滤：价格接口一次取 200 条，不在服务端按课程查
const filteredPrices = computed(() => priceFilter.value ? prices.value.filter((p) => p.courseId === priceFilter.value) : prices.value)
// 大纲只在选定课程后展示；未选课程时为空，避免把多门课的大纲混在一起
const filteredOutlines = computed(() => outlineCourseId.value ? outlines.value.filter((o) => o.courseId === outlineCourseId.value) : [])

// 课程分页查询；若还没选中大纲课程，自动选中第一门，供大纲页签与新增大纲使用
async function loadCourses() {
  loading.value = true
  try {
    const data = await courseApi.page({ current: query.current, size: query.size, keyword: query.keyword || undefined })
    courses.value = data.records
    courseTotal.value = data.total
    if (!outlineCourseId.value && courses.value.length) outlineCourseId.value = courses.value[0].id
  } finally { loading.value = false }
}

// 三个字典并发加载：课程表格要显示分类/阶段名称，表单也需要它们做下拉
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

// 价格一次性取 200 条，前端过滤；课程数量与价格档位都很少，够用且省一次请求
async function loadPrices() {
  priceLoading.value = true
  try {
    const data = await priceApi.page({ current: 1, size: 200 })
    prices.value = data.records
  } finally { priceLoading.value = false }
}

// 大纲同样一次性取 200 条，前端按 outlineCourseId 过滤
async function loadOutlines() {
  outlineLoading.value = true
  try {
    const data = await outlineApi.page({ current: 1, size: 200 })
    outlines.value = data.records
  } finally { outlineLoading.value = false }
}

// 新建课程：分类/阶段默认取字典第一项，频率给一个常用默认文案
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

// 编辑课程：行里的 featured 是 1/0，需要转成布尔再交给 a-switch
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

// 提交课程：名称与分类必填；featured 转 1/0、sort 转数字后再提交
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

// 删除课程：后端级联删除关联价格与大纲，因此三个列表都要刷新
async function removeCourse(row) {
  await courseApi.remove(row.id)
  message.success('课程已删除，关联价格与大纲一并删除')
  await loadCourses()
  await Promise.all([loadPrices(), loadOutlines()])
}

// 新增班型价格：课程与班型默认取第一个，避免必填项为空
function openCreatePrice() {
  editingPrice.value = null
  Object.assign(priceForm, {
    courseId: courses.value[0]?.id, classTypeId: classTypes.value[0]?.id,
    lessons: 24, price: 0, unit: 'TERM', status: 'ON', sort: 0
  })
  priceOpen.value = true
}
// 编辑班型价格：整行回填到表单（字段与表单基本同名同义）
function openEditPrice(row) {
  editingPrice.value = row
  Object.assign(priceForm, row)
  priceOpen.value = true
}
// 提交价格：课程与班型必填，价格/课时/排序转数字
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
// 删除班型价格
async function removePrice(row) {
  await priceApi.remove(row.id)
  message.success('价格已删除')
  await loadPrices()
}

// 新增大纲阶段：默认归属当前筛选的课程，排序取现有条数 + 1
function openCreateOutline() {
  editingOutline.value = null
  Object.assign(outlineForm, {
    courseId: outlineCourseId.value, title: '', lessonRange: '',
    description: '', visible: true, sort: outlines.value.length + 1
  })
  outlineOpen.value = true
}
// 编辑大纲阶段：visible 后端存 1/0，这里转成布尔给 switch
function openEditOutline(row) {
  editingOutline.value = row
  Object.assign(outlineForm, { ...row, visible: row.visible === 1 })
  outlineOpen.value = true
}
// 提交大纲阶段：课程与阶段标题必填，visible 转 1/0
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
// 行内开关切换显示/隐藏：整行回传并翻转 visible；row 本身未改，提示语按旧值判断
async function toggleOutline(row) {
  await outlineApi.update(row.id, { ...row, visible: row.visible === 1 ? 0 : 1 })
  message.success(row.visible === 1 ? '阶段已隐藏，小程序不再展示' : '阶段已显示')
  await loadOutlines()
}
// 删除大纲阶段
async function removeOutline(row) {
  await outlineApi.remove(row.id)
  message.success('大纲阶段已删除')
  await loadOutlines()
}

// 切换页签：按需加载对应列表，课程页签的数据在首屏已加载
function onTab(key) {
  activeTab.value = key
  if (key === 'prices') loadPrices()
  if (key === 'outline') loadOutlines()
}

// 先加载字典（课程表格需要翻译分类/阶段），再并发加载课程、价格、大纲
onMounted(async () => {
  await loadBasics()
  await Promise.all([loadCourses(), loadPrices(), loadOutlines()])
})
</script>

<template>
  <div>
    <!-- 页头：刷新（按当前页签）与新建课程 -->
    <div class="page-head">
      <div>
        <div class="eyebrow">CURRICULUM & PRICING</div>
        <h1>课程与定价</h1>
        <p>课程、班型价格与内容大纲统一维护，小程序端只读展示</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="onTab(activeTab)"><ReloadOutlined /> 刷新</a-button>
        <a-button v-if="$auth.can('courses','create')" type="primary" size="small" @click="openCreateCourse"><PlusOutlined /> 新建课程</a-button>
      </div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <!-- 三个页签：课程管理 / 班型与价格 / 内容大纲 -->
      <a-tabs :active-key="activeTab" @change="onTab">
        <!-- 课程管理页签 -->
        <a-tab-pane key="courses" tab="课程管理">
          <!-- 顶部筛选区：课程名称关键字（回车或查询都会回到第一页） -->
          <div class="filters">
            <a-input v-model:value="query.keyword" placeholder="搜索课程名称" allow-clear style="width:220px" @press-enter="() => { query.current = 1; loadCourses() }" />
            <a-button size="small" @click="() => { query.current = 1; loadCourses() }">查询</a-button>
            <div class="sp"></div>
            <span class="muted small">共 {{ courseTotal }} 门课程 · 小程序仅展示上架课程</span>
          </div>
          <!-- 课程表格：上架状态、首页推荐用标签展示，行内可编辑/看大纲/删除 -->
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
                  <a-button v-if="$auth.can('courses','edit')" type="link" size="small" @click="openEditCourse(record)">编辑</a-button>
                  <a-button type="link" size="small" @click="() => { outlineCourseId = record.id; activeTab = 'outline'; loadOutlines() }">大纲</a-button>
                  <a-popconfirm v-if="$auth.can('courses','delete')" title="删除课程？关联价格与大纲将一并删除" @confirm="removeCourse(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
                </div>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 班型与价格页签：同一课程可维护多个班型价格 -->
        <a-tab-pane key="prices" tab="班型与价格">
          <!-- 顶部筛选区：按课程过滤价格（纯前端过滤） -->
          <div class="filters">
            <a-select v-model:value="priceFilter" placeholder="全部课程" allow-clear style="width:200px">
              <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
            </a-select>
            <a-button size="small" @click="loadPrices"><ReloadOutlined /> 刷新</a-button>
            <div class="sp"></div>
            <a-button v-if="$auth.can('courses','create')" type="primary" size="small" @click="openCreatePrice"><PlusOutlined /> 新增班型价格</a-button>
          </div>
          <!-- 价格表格：计价单位 TERM 显示「课时」、TIME 显示「次」 -->
          <a-table :columns="priceColumns" :data-source="filteredPrices" :loading="priceLoading" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'course'">{{ courseName(record.courseId) }}</template>
              <template v-if="column.key === 'classType'">{{ classTypeName(record.classTypeId) }}</template>
              <template v-if="column.key === 'lessons'">{{ record.lessons }} {{ record.unit === 'TIME' ? '次' : '课时' }}</template>
              <template v-if="column.key === 'price'"><b class="num">¥{{ Number(record.price).toLocaleString() }}</b></template>
              <template v-if="column.key === 'status'"><span class="tag" :class="record.status === 'ON' ? 'green' : 'gray'">{{ record.status === 'ON' ? '在售' : '停售' }}</span></template>
              <template v-if="column.key === 'actions'">
                <a-button v-if="$auth.can('courses','edit')" type="link" size="small" @click="openEditPrice(record)">编辑</a-button>
                <a-popconfirm v-if="$auth.can('courses','delete')" title="删除该价格？" @confirm="removePrice(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
              </template>
            </template>
          </a-table>
          <div class="footnote">班型来自「基础数据 → 班型」；同一课程可维护多个班型价格。不设活动价。</div>
        </a-tab-pane>

        <!-- 内容大纲页签：按课程查看与维护阶段 -->
        <a-tab-pane key="outline" tab="内容大纲">
          <!-- 顶部筛选区：选择课程（切换后重新加载大纲） -->
          <div class="filters">
            <a-select v-model:value="outlineCourseId" style="width:230px" @change="loadOutlines">
              <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
            </a-select>
            <a-button size="small" @click="loadOutlines"><ReloadOutlined /> 刷新</a-button>
            <div class="sp"></div>
            <a-button v-if="$auth.can('courses','create')" type="primary" size="small" @click="openCreateOutline"><PlusOutlined /> 新增阶段</a-button>
          </div>
          <!-- 大纲表格：显示列是开关，切换即时生效；隐藏后小程序详情不再展示 -->
          <a-table :columns="outlineColumns" :data-source="filteredOutlines" :loading="outlineLoading" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'visible'"><a-switch :checked="record.visible === 1" size="small" @change="toggleOutline(record)" /></template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button v-if="$auth.can('courses','edit')" type="link" size="small" @click="openEditOutline(record)">编辑</a-button>
                  <a-popconfirm v-if="$auth.can('courses','delete')" title="删除该大纲阶段？" @confirm="removeOutline(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
                </div>
              </template>
            </template>
          </a-table>
          <div class="footnote">大纲阶段支持新增、编辑、删除与显示 / 隐藏；隐藏后小程序课程详情不展示该阶段。</div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 新建/编辑课程弹窗：三种页签共用同一个页面状态，弹窗也是三种 -->
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

    <!-- 新增/编辑班型价格弹窗 -->
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

    <!-- 新增/编辑大纲阶段弹窗 -->
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
