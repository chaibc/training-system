/**
 * 作品管理页：三个页签 —— 作品审核（分页/搜索/通过展示/下架/上传作品）、
 * 家长授权（授权列表/补录/撤回）、评论审核（通过/隐藏）。
 * 接口：workApi.page/review/create、workAuthApi.page/record/revoke、workCommentApi.page/update（api/index.js）
 * 辅助接口：studentApi.options、teacherApi.list、userApi.parentOptions、uploadApi.image
 *
 * 业务规则：作品必须已取得家长授权（authorized === 1）才能通过审核展示；
 * 撤回授权会联动下架该学员已展示的作品，所以撤回后要同时刷新授权与作品两个列表。
 */
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { workApi, workCommentApi, workAuthApi, studentApi, teacherApi, userApi, uploadApi } from '../api'
import { ReloadOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'

const activeTab = ref('review')
const loading = ref(false)
const works = ref([])
const workTotal = ref(0)
// 作品列表与查询条件（服务端分页，每页 12 条，适配作品墙密度）
const workQuery = reactive({ current: 1, size: 12, keyword: '' })
const students = ref([])
const teachers = ref([])

const authLoading = ref(false)
// 家长授权列表（前端不分页，接口按 size 10 返回）
const auths = ref([])
const authQuery = reactive({ current: 1, size: 10, keyword: '' })

const commentLoading = ref(false)
// 评论列表（前端不分页，接口按 size 10 返回）
const comments = ref([])
const commentQuery = reactive({ current: 1, size: 10, keyword: '' })
// 家长下拉数据（userApi.parentOptions），用于授权补录时选家长
const parents = ref([])

const uploadOpen = ref(false)
const uploadSaving = ref(false)
const uploading = ref(false)
// 上传表单：authorized 是前端必勾的授权确认，提交前必须为 true
const uploadForm = reactive({
  studentId: undefined, title: '', workType: 'HARD', contentChar: '',
  imageUrl: '', comment: '', authorized: false
})
const authOpen = ref(false)
const authSaving = ref(false)
// 授权补录表单：method 区分电子授权书/纸质/微信确认三种授权方式
const authForm = reactive({
  studentId: undefined, parentUserId: undefined, scope: '作品展示',
  method: 'ELECTRONIC', authorizedAt: dayjs(), evidenceUrl: ''
})

// 作品审核状态码 → 中文标签 + 标签配色
const statusMap = {
  PENDING: { text: '待审核', cls: 'gold' },
  DISPLAYED: { text: '已展示', cls: 'green' },
  HIDDEN: { text: '已下架', cls: 'gray' },
  REJECTED: { text: '已退回', cls: 'red' }
}
// 评论审核状态码 → 中文标签 + 标签配色（与作品状态是两套枚举）
const commentStatusMap = {
  PENDING: { text: '待审核', cls: 'gold' },
  APPROVED: { text: '已通过', cls: 'green' },
  HIDDEN: { text: '已隐藏', cls: 'gray' },
  REJECTED: { text: '已驳回', cls: 'red' }
}
// 授权状态码 → 中文标签 + 标签配色
const authStatusMap = {
  ACTIVE: { text: '已授权', cls: 'green' },
  REVOKED: { text: '已撤回', cls: 'gray' }
}

// 作品表格列：作品/学员/授权/状态都要在 bodyCell 里翻译
const workColumns = [
  { title: '作品', key: 'title', width: 240 },
  { title: '学员 / 班级', key: 'student', width: 160 },
  { title: '书体', dataIndex: 'workType', key: 'workType', width: 70 },
  { title: '授权', key: 'authorized', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '点赞', dataIndex: 'likeCount', key: 'likeCount', width: 60 },
  { title: '创作时间', key: 'createdAt', width: 110 },
  { title: '操作', key: 'actions', width: 170 }
]
// 家长授权表格列
const authColumns = [
  { title: '学员', key: 'student', width: 120 },
  { title: '家长', key: 'parent', width: 120 },
  { title: '授权范围', dataIndex: 'scope', key: 'scope' },
  { title: '授权方式', dataIndex: 'method', key: 'method', width: 110 },
  { title: '授权时间', dataIndex: 'authorizedAt', key: 'authorizedAt', width: 110 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 100 }
]
// 评论审核表格列
const commentColumns = [
  { title: '评论内容', dataIndex: 'content', key: 'content' },
  { title: '作品', key: 'work', width: 170 },
  { title: '评论人', key: 'parent', width: 110 },
  { title: '时间', key: 'createdAt', width: 120 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 130 }
]

// ID → 名称的查表函数（学员/教师/家长/作品），查不到统一显示 —
const studentName = (id) => students.value.find((x) => x.id === id)?.name || '—'
// 教师 ID → 姓名；查不到显示 —
const teacherName = (id) => teachers.value.find((x) => x.id === id)?.name || '—'
// 家长 ID → 姓名；查不到显示 —
const parentName = (id) => parents.value.find((x) => x.id === id)?.name || '—'
// 作品 ID → 标题；查不到显示「作品 #id」，用于评论列表
const workTitle = (id) => works.value.find((x) => x.id === id)?.title || `作品 #${id}`

// 作品分页查询；关键字为空时转 undefined，避免把空串当搜索条件
async function load() {
  loading.value = true
  try {
    const data = await workApi.page({ current: workQuery.current, size: workQuery.size, keyword: workQuery.keyword || undefined })
    works.value = data.records
    workTotal.value = data.total
  } finally { loading.value = false }
}
// 家长授权列表查询
async function loadAuths() {
  authLoading.value = true
  try {
    const data = await workAuthApi.page({ current: authQuery.current, size: authQuery.size, keyword: authQuery.keyword || undefined })
    auths.value = data.records
  } finally { authLoading.value = false }
}
// 评论列表查询
async function loadComments() {
  commentLoading.value = true
  try {
    const data = await workCommentApi.page({ current: commentQuery.current, size: commentQuery.size, keyword: commentQuery.keyword || undefined })
    comments.value = data.records
  } finally { commentLoading.value = false }
}
// 学员/教师/家长三份下拉数据并发加载，用于表格翻译与表单选择
async function loadBasics() {
  const [s, t, p] = await Promise.all([studentApi.options(), teacherApi.list(), userApi.parentOptions()])
  students.value = s
  teachers.value = t
  parents.value = p
}

// 通过审核前先校验家长授权：缺授权的作品直接拦截，避免违规展示未成年人作品
async function approve(row) {
  if (row.authorized !== 1) { message.warning('缺少家长授权，无法展示。请先在家长授权名单补录授权'); return }
  await workApi.review(row.id, 'DISPLAYED')
  message.success('作品已通过审核并在小程序作品墙展示')
  await load()
}
// 下架作品：状态改为 HIDDEN，小程序作品墙不再展示
async function hide(row) {
  await workApi.review(row.id, 'HIDDEN')
  message.success('作品已下架')
  await load()
}
// 撤回授权：后端会联动下架该学员已展示的作品，所以两个列表都要刷新
async function revokeAuth(row) {
  await workAuthApi.revoke(row.id)
  message.success('授权已撤回，该学员已展示作品已同步下架')
  await Promise.all([loadAuths(), load()])
}
// 评论通过：整行回传再改 status，后端按整条记录更新
async function approveComment(row) {
  await workCommentApi.update(row.id, { ...row, status: 'APPROVED' })
  message.success('评论已通过')
  await loadComments()
}
// 评论隐藏：隐藏后小程序端不再展示这条评论
async function hideComment(row) {
  await workCommentApi.update(row.id, { ...row, status: 'HIDDEN' })
  message.success('评论已隐藏')
  await loadComments()
}
// 切换页签：授权与评论列表按需加载，作品列表首屏已加载
function onTab(key) {
  activeTab.value = key
  if (key === 'auth') loadAuths()
  if (key === 'comments') loadComments()
}

// 打开上传弹窗：重置表单并清空上次的图片地址
function openUpload() {
  Object.assign(uploadForm, {
    studentId: undefined, title: '', workType: 'HARD', contentChar: '',
    imageUrl: '', comment: '', authorized: false
  })
  uploadOpen.value = true
}

// 图片上传：返回 false 阻止 antd 自动上传，改用自己的 uploadApi 上传并回填 URL
async function beforeUpload(file) {
  uploading.value = true
  try {
    const data = await uploadApi.image(file, 'work')
    uploadForm.imageUrl = data.url
    message.success('图片已上传')
  } finally { uploading.value = false }
  return false
}

// 提交作品：学员、标题、图片、授权勾选四项本地校验，全部通过才提交
async function submitUpload() {
  if (!uploadForm.studentId) return message.warning('请选择学员')
  if (!uploadForm.title.trim()) return message.warning('请输入作品标题')
  if (!uploadForm.imageUrl) return message.warning('请上传作品图片')
  if (!uploadForm.authorized) return message.warning('请确认已取得家长授权')
  uploadSaving.value = true
  try {
    await workApi.create({ ...uploadForm, title: uploadForm.title.trim() })
    message.success('作品已上传，等待审核')
    uploadOpen.value = false
    activeTab.value = 'review'
    await load()
  } finally { uploadSaving.value = false }
}

// 打开授权补录弹窗：清空选择，授权日期默认今天
function openAuth() {
  Object.assign(authForm, {
    studentId: undefined, parentUserId: undefined, scope: '作品展示',
    method: 'ELECTRONIC', authorizedAt: dayjs(), evidenceUrl: ''
  })
  authOpen.value = true
}

// 补录授权：学员与家长必填，授权日期格式化为 YYYY-MM-DD 后提交
async function submitAuth() {
  if (!authForm.studentId) return message.warning('请选择学员')
  if (!authForm.parentUserId) return message.warning('请选择家长')
  authSaving.value = true
  try {
    await workAuthApi.record({
      ...authForm,
      authorizedAt: authForm.authorizedAt ? authForm.authorizedAt.format('YYYY-MM-DD') : null
    })
    message.success('家长授权已补录')
    authOpen.value = false
    activeTab.value = 'auth'
    await loadAuths()
  } finally { authSaving.value = false }
}

// 先加载下拉数据（表格要翻译学员/家长姓名），再加载作品列表
onMounted(async () => {
  await loadBasics()
  await load()
})
</script>

<template>
  <div>
    <!-- 页头：审核规则提示、刷新与上传作品 -->
    <div class="page-head">
      <div>
        <div class="eyebrow">GALLERY & CONTENT REVIEW</div>
        <h1>作品管理</h1>
        <p>家长授权、作品审核、评论审核与对外展示</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <span class="muted small">作品须先取得家长授权，再经审核展示</span>
        <a-button size="small" @click="onTab(activeTab)"><ReloadOutlined /> 刷新</a-button>
        <a-button type="primary" size="small" @click="openUpload"><UploadOutlined /> 上传作品</a-button>
      </div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <!-- 三个页签：作品审核 / 家长授权 / 评论审核 -->
      <a-tabs :active-key="activeTab" @change="onTab">
        <!-- 作品审核页签：未展示的可通过，已展示的可下架 -->
        <a-tab-pane key="review" tab="作品审核">
          <!-- 顶部筛选区：按作品标题搜索 -->
          <div class="filters">
            <a-input v-model:value="workQuery.keyword" placeholder="搜索作品标题" allow-clear style="width:220px" @press-enter="() => { workQuery.current = 1; load() }" />
            <a-button size="small" @click="() => { workQuery.current = 1; load() }">查询</a-button>
            <div class="sp"></div>
            <span class="muted small">共 {{ workTotal }} 幅作品</span>
          </div>
          <!-- 作品表格：授权列直接标红提示是否可展示 -->
          <a-table :columns="workColumns" :data-source="works" :loading="loading" row-key="id" size="small"
                   :pagination="{ current: workQuery.current, pageSize: workQuery.size, total: workTotal, showSizeChanger: false }"
                   @change="(p) => { workQuery.current = p.current; load() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'title'"><b>{{ record.title }}</b><div class="muted" style="font-size:9px">{{ teacherName(record.teacherId) }} · {{ record.comment }}</div></template>
              <template v-if="column.key === 'student'">{{ studentName(record.studentId) }}</template>
              <template v-if="column.key === 'workType'">{{ record.workType === 'SOFT' ? '软笔' : '硬笔' }}</template>
              <template v-if="column.key === 'authorized'"><span class="tag" :class="record.authorized === 1 ? 'green' : 'red'">{{ record.authorized === 1 ? '已授权' : '缺授权' }}</span></template>
              <template v-if="column.key === 'status'"><span class="tag" :class="statusMap[record.status]?.cls">{{ statusMap[record.status]?.text || record.status }}</span></template>
              <template v-if="column.key === 'createdAt'">{{ record.createdAt ? dayjs(record.createdAt).format('MM-DD') : '—' }}</template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button v-if="record.status !== 'DISPLAYED'" type="link" size="small" @click="approve(record)">通过展示</a-button>
                  <a-button v-if="record.status === 'DISPLAYED'" type="link" size="small" danger @click="hide(record)">下架</a-button>
                </div>
              </template>
            </template>
          </a-table>
          <div class="footnote">作品须先取得家长授权（含未成年人肖像权与作品权），再经内容审核后才可展示。</div>
        </a-tab-pane>

        <!-- 家长授权页签：补录与撤回授权 -->
        <a-tab-pane key="auth" tab="家长授权">
          <!-- 授权页签工具栏：补录授权入口 -->
          <div class="filters">
            <a-button type="primary" size="small" @click="openAuth"><PlusOutlined /> 补录家长授权</a-button>
            <div class="sp"></div>
            <span class="muted small">授权范围须明确，授权书原件由教务留存</span>
          </div>
          <!-- 授权表格：只有已授权的记录才显示撤回按钮 -->
          <a-table :columns="authColumns" :data-source="auths" :loading="authLoading" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'student'"><b>{{ studentName(record.studentId) }}</b></template>
              <template v-if="column.key === 'parent'">{{ parentName(record.parentUserId) }}</template>
              <template v-if="column.key === 'status'"><span class="tag" :class="authStatusMap[record.status]?.cls">{{ authStatusMap[record.status]?.text || record.status }}</span></template>
              <template v-if="column.key === 'actions'"><a-button v-if="record.status === 'ACTIVE'" type="link" size="small" danger @click="revokeAuth(record)">撤回授权</a-button></template>
            </template>
          </a-table>
          <div class="footnote">授权范围须明确；家长可随时撤回授权，撤回后已展示作品应立即下架。</div>
        </a-tab-pane>

        <!-- 评论审核页签：可按状态通过或隐藏 -->
        <a-tab-pane key="comments" tab="评论审核">
          <!-- 评论表格：被隐藏的评论不再展示通过按钮 -->
          <a-table :columns="commentColumns" :data-source="comments" :loading="commentLoading" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'work'">{{ workTitle(record.workId) }}</template>
              <template v-if="column.key === 'parent'">{{ parentName(record.parentUserId) }}</template>
              <template v-if="column.key === 'createdAt'">{{ record.createdAt ? dayjs(record.createdAt).format('MM-DD HH:mm') : '—' }}</template>
              <template v-if="column.key === 'status'"><span class="tag" :class="commentStatusMap[record.status]?.cls">{{ commentStatusMap[record.status]?.text || record.status }}</span></template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button v-if="record.status !== 'APPROVED'" type="link" size="small" @click="approveComment(record)">通过</a-button>
                  <a-button v-if="record.status !== 'HIDDEN'" type="link" size="small" danger @click="hideComment(record)">隐藏</a-button>
                </div>
              </template>
            </template>
          </a-table>
          <div class="footnote">评论须审核后展示，禁止营销与不当内容。</div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 上传作品弹窗：图片走 uploadApi，授权勾选是提交的硬性前提 -->
    <a-modal v-model:open="uploadOpen" title="上传学员作品" ok-text="提交审核" cancel-text="取消"
             :confirm-loading="uploadSaving" @ok="submitUpload">
      <a-form layout="vertical">
        <a-form-item label="学员" required>
          <a-select v-model:value="uploadForm.studentId" show-search option-filter-prop="label" placeholder="选择学员">
            <a-select-option v-for="s in students" :key="s.id" :value="s.id" :label="s.name + ' ' + (s.studentNo || '')">
              {{ s.name }} <span class="muted">{{ s.studentNo }}</span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="14"><a-form-item label="作品标题" required><a-input v-model:value="uploadForm.title" placeholder="如《永字八法》" /></a-form-item></a-col>
          <a-col :span="10"><a-form-item label="书体"><a-select v-model:value="uploadForm.workType"><a-select-option value="HARD">硬笔</a-select-option><a-select-option value="SOFT">软笔</a-select-option></a-select></a-form-item></a-col>
        </a-row>
        <a-form-item label="作品内容 / 单字"><a-input v-model:value="uploadForm.contentChar" placeholder="如 静、上善若水" /></a-form-item>
        <a-form-item label="作品图片" required>
          <div class="upload-row">
            <!-- 选择图片：before-upload 拦截默认上传行为 -->
            <a-upload :before-upload="beforeUpload" :show-upload-list="false" accept="image/*">
              <a-button :loading="uploading"><UploadOutlined /> 选择图片</a-button>
            </a-upload>
            <a-image v-if="uploadForm.imageUrl" :src="uploadForm.imageUrl" :width="90" />
            <span class="muted small">JPG / PNG / WEBP，≤ 10MB</span>
          </div>
        </a-form-item>
        <a-form-item label="教师点评"><a-textarea v-model:value="uploadForm.comment" :rows="3" maxlength="300" show-count /></a-form-item>
        <a-checkbox v-model:checked="uploadForm.authorized">已取得家长作品展示授权，内容真实，不含其他学员隐私</a-checkbox>
      </a-form>
    </a-modal>

    <!-- 补录家长授权弹窗：授权范围与方式须明确 -->
    <a-modal v-model:open="authOpen" title="补录家长作品授权" ok-text="保存授权" cancel-text="取消"
             :confirm-loading="authSaving" @ok="submitAuth">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="学员" required>
              <a-select v-model:value="authForm.studentId" show-search option-filter-prop="label" placeholder="选择学员">
                <a-select-option v-for="s in students" :key="s.id" :value="s.id" :label="s.name + ' ' + (s.studentNo || '')">{{ s.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="家长" required>
              <a-select v-model:value="authForm.parentUserId" show-search option-filter-prop="label" placeholder="选择家长">
                <a-select-option v-for="p in parents" :key="p.id" :value="p.id" :label="p.name">{{ p.name }} · {{ p.phone }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="授权范围"><a-input v-model:value="authForm.scope" placeholder="作品展示 / 宣传" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="授权方式"><a-select v-model:value="authForm.method"><a-select-option value="ELECTRONIC">电子授权书</a-select-option><a-select-option value="PAPER">纸质授权</a-select-option><a-select-option value="WECHAT">微信确认</a-select-option></a-select></a-form-item></a-col>
        </a-row>
        <a-form-item label="授权日期"><a-date-picker v-model:value="authForm.authorizedAt" style="width:100%" /></a-form-item>
        <a-form-item label="授权书地址（可选）"><a-input v-model:value="authForm.evidenceUrl" placeholder="上传后的文件地址或线下留存编号" /></a-form-item>
      </a-form>
      <div class="footnote" style="padding-top:0">请确认授权书由监护人签署，授权范围明确；撤回授权后相关作品应立即下架。</div>
    </a-modal>
  </div>
</template>

<style scoped>
.upload-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
</style>
