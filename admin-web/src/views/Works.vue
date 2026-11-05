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
const workQuery = reactive({ current: 1, size: 12, keyword: '' })
const students = ref([])
const teachers = ref([])

const authLoading = ref(false)
const auths = ref([])
const authQuery = reactive({ current: 1, size: 10, keyword: '' })

const commentLoading = ref(false)
const comments = ref([])
const commentQuery = reactive({ current: 1, size: 10, keyword: '' })
const parents = ref([])

const uploadOpen = ref(false)
const uploadSaving = ref(false)
const uploading = ref(false)
const uploadForm = reactive({
  studentId: undefined, title: '', workType: 'HARD', contentChar: '',
  imageUrl: '', comment: '', authorized: false
})
const authOpen = ref(false)
const authSaving = ref(false)
const authForm = reactive({
  studentId: undefined, parentUserId: undefined, scope: '作品展示',
  method: 'ELECTRONIC', authorizedAt: dayjs(), evidenceUrl: ''
})

const statusMap = {
  PENDING: { text: '待审核', cls: 'gold' },
  DISPLAYED: { text: '已展示', cls: 'green' },
  HIDDEN: { text: '已下架', cls: 'gray' },
  REJECTED: { text: '已退回', cls: 'red' }
}
const commentStatusMap = {
  PENDING: { text: '待审核', cls: 'gold' },
  APPROVED: { text: '已通过', cls: 'green' },
  HIDDEN: { text: '已隐藏', cls: 'gray' },
  REJECTED: { text: '已驳回', cls: 'red' }
}
const authStatusMap = {
  ACTIVE: { text: '已授权', cls: 'green' },
  REVOKED: { text: '已撤回', cls: 'gray' }
}

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
const authColumns = [
  { title: '学员', key: 'student', width: 120 },
  { title: '家长', key: 'parent', width: 120 },
  { title: '授权范围', dataIndex: 'scope', key: 'scope' },
  { title: '授权方式', dataIndex: 'method', key: 'method', width: 110 },
  { title: '授权时间', dataIndex: 'authorizedAt', key: 'authorizedAt', width: 110 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 100 }
]
const commentColumns = [
  { title: '评论内容', dataIndex: 'content', key: 'content' },
  { title: '作品', key: 'work', width: 170 },
  { title: '评论人', key: 'parent', width: 110 },
  { title: '时间', key: 'createdAt', width: 120 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 130 }
]

const studentName = (id) => students.value.find((x) => x.id === id)?.name || '—'
const teacherName = (id) => teachers.value.find((x) => x.id === id)?.name || '—'
const parentName = (id) => parents.value.find((x) => x.id === id)?.name || '—'
const workTitle = (id) => works.value.find((x) => x.id === id)?.title || `作品 #${id}`

async function load() {
  loading.value = true
  try {
    const data = await workApi.page({ current: workQuery.current, size: workQuery.size, keyword: workQuery.keyword || undefined })
    works.value = data.records
    workTotal.value = data.total
  } finally { loading.value = false }
}
async function loadAuths() {
  authLoading.value = true
  try {
    const data = await workAuthApi.page({ current: authQuery.current, size: authQuery.size, keyword: authQuery.keyword || undefined })
    auths.value = data.records
  } finally { authLoading.value = false }
}
async function loadComments() {
  commentLoading.value = true
  try {
    const data = await workCommentApi.page({ current: commentQuery.current, size: commentQuery.size, keyword: commentQuery.keyword || undefined })
    comments.value = data.records
  } finally { commentLoading.value = false }
}
async function loadBasics() {
  const [s, t, p] = await Promise.all([studentApi.options(), teacherApi.list(), userApi.parentOptions()])
  students.value = s
  teachers.value = t
  parents.value = p
}

async function approve(row) {
  if (row.authorized !== 1) { message.warning('缺少家长授权，无法展示。请先在家长授权名单补录授权'); return }
  await workApi.review(row.id, 'DISPLAYED')
  message.success('作品已通过审核并在小程序作品墙展示')
  await load()
}
async function hide(row) {
  await workApi.review(row.id, 'HIDDEN')
  message.success('作品已下架')
  await load()
}
async function revokeAuth(row) {
  await workAuthApi.revoke(row.id)
  message.success('授权已撤回，该学员已展示作品已同步下架')
  await Promise.all([loadAuths(), load()])
}
async function approveComment(row) {
  await workCommentApi.update(row.id, { ...row, status: 'APPROVED' })
  message.success('评论已通过')
  await loadComments()
}
async function hideComment(row) {
  await workCommentApi.update(row.id, { ...row, status: 'HIDDEN' })
  message.success('评论已隐藏')
  await loadComments()
}
function onTab(key) {
  activeTab.value = key
  if (key === 'auth') loadAuths()
  if (key === 'comments') loadComments()
}

function openUpload() {
  Object.assign(uploadForm, {
    studentId: undefined, title: '', workType: 'HARD', contentChar: '',
    imageUrl: '', comment: '', authorized: false
  })
  uploadOpen.value = true
}

async function beforeUpload(file) {
  uploading.value = true
  try {
    const data = await uploadApi.image(file, 'work')
    uploadForm.imageUrl = data.url
    message.success('图片已上传')
  } finally { uploading.value = false }
  return false
}

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

function openAuth() {
  Object.assign(authForm, {
    studentId: undefined, parentUserId: undefined, scope: '作品展示',
    method: 'ELECTRONIC', authorizedAt: dayjs(), evidenceUrl: ''
  })
  authOpen.value = true
}

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

onMounted(async () => {
  await loadBasics()
  await load()
})
</script>

<template>
  <div>
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
      <a-tabs :active-key="activeTab" @change="onTab">
        <a-tab-pane key="review" tab="作品审核">
          <div class="filters">
            <a-input v-model:value="workQuery.keyword" placeholder="搜索作品标题" allow-clear style="width:220px" @press-enter="() => { workQuery.current = 1; load() }" />
            <a-button size="small" @click="() => { workQuery.current = 1; load() }">查询</a-button>
            <div class="sp"></div>
            <span class="muted small">共 {{ workTotal }} 幅作品</span>
          </div>
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

        <a-tab-pane key="auth" tab="家长授权">
          <div class="filters">
            <a-button type="primary" size="small" @click="openAuth"><PlusOutlined /> 补录家长授权</a-button>
            <div class="sp"></div>
            <span class="muted small">授权范围须明确，授权书原件由教务留存</span>
          </div>
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

        <a-tab-pane key="comments" tab="评论审核">
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
