/**
 * 机构内容页：四个页签 —— 机构资料（含教学环境图片）、师资团队、资质公示、公告管理。
 * 接口：orgApi.info/save/images/createImage/updateImage/removeImage、teacherApi.list/create/update、
 *       qualificationApi.page/create/update/remove、noticeApi.page/create/update/remove、uploadApi.image
 *
 * 机构资料是整表单保存；教学环境图片上传后立即创建记录，图片说明在输入框失焦时单独保存。
 */
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { orgApi, teacherApi, qualificationApi, noticeApi, uploadApi } from '../api'
import { PlusOutlined, ReloadOutlined, UploadOutlined, DeleteOutlined } from '@ant-design/icons-vue'

const activeTab = ref('info')
const infoLoading = ref(false)
const infoSaving = ref(false)
// 机构资料表单：经纬度供小程序 wx.openLocation 地图导航使用，全站只维护一个机构地址
const info = reactive({
  name: '', brand: '', slogan: '', heroIntro: '', intro: '', philosophy: '',
  phone: '', wechat: '', hours: '', address: '', longitude: null, latitude: null,
  traffic: '', sinceYear: 2014, status: 'OPEN'
})
// 机构 ID：新增图片记录时要带上；加载资料后会用后端返回的 id 覆盖这个默认值
const institutionId = ref(1)
// 教学环境图片列表（含说明与排序），上传成功后立即刷新
const images = ref([])
const imageUploading = ref(false)
const imageLoading = ref(false)

const teachers = ref([])
const teacherLoading = ref(false)
const teacherOpen = ref(false)
const teacherSaving = ref(false)
const editingTeacher = ref(null)
// 教师表单：phone 是教师微信登录的匹配依据，必须是 11 位手机号
const teacherForm = reactive({ name: '', phone: '', title: '', years: 0, styles: '', coursesText: '', bio: '', quote: '', tags: '', status: 'ACTIVE', sort: 0 })

const quals = ref([])
const qualLoading = ref(false)
const qualOpen = ref(false)
const qualSaving = ref(false)
const editingQual = ref(null)
// 资质表单：validUntil 是文本（支持「长期」），visible 用布尔，提交时转 1/0
const qualForm = reactive({ name: '', code: '', validUntil: '', verifyStatus: 'VERIFIED', visible: true, sort: 0 })

const notices = ref([])
const noticeLoading = ref(false)
const noticeOpen = ref(false)
const noticeSaving = ref(false)
const editingNotice = ref(null)
// 公告表单：publishDate 是 dayjs 对象，提交时格式化为 YYYY-MM-DD
const noticeForm = reactive({ title: '', type: 'NOTICE', content: '', publishDate: null, status: 'PUBLISHED' })

// 公告类型与公告状态的中文映射，表格里直接查表展示
const noticeTypes = { NOTICE: '通知公告', ADMISSION: '招生信息', HOLIDAY: '放假安排' }
const noticeStatus = { DRAFT: '草稿', PUBLISHED: '展示中', HIDDEN: '已隐藏' }

// 读取机构资料回填表单；字段缺失时给默认值，避免输入框里出现 undefined
async function loadInfo() {
  infoLoading.value = true
  try {
    const data = await orgApi.info()
    if (data.id) institutionId.value = data.id
    Object.assign(info, {
      name: data.name || '', brand: data.brand || '', slogan: data.slogan || '',
      heroIntro: data.heroIntro || '', intro: data.intro || '', philosophy: data.philosophy || '',
      phone: data.phone || '', wechat: data.wechat || '', hours: data.hours || '',
      address: data.address || '', longitude: data.longitude, latitude: data.latitude,
      traffic: data.traffic || '', sinceYear: data.sinceYear || 2014, status: data.status || 'OPEN'
    })
  } finally { infoLoading.value = false }
}
// 保存机构资料：整表单提交，保存后小程序端同步更新
async function saveInfo() {
  infoSaving.value = true
  try {
    await orgApi.save({ ...info })
    message.success('机构资料已保存，小程序同步更新')
  } finally { infoSaving.value = false }
}
// 加载教学环境图片列表（按后端排序返回）
async function loadImages() {
  imageLoading.value = true
  try { images.value = await orgApi.images() } finally { imageLoading.value = false }
}
// 上传环境图片：先传文件拿 URL，再创建图片记录（标题固定「教学环境」，排序追加到末尾）
// 返回 false 阻止 antd 的默认上传行为，改用项目自己的上传接口
async function beforeImageUpload(file) {
  imageUploading.value = true
  try {
    const data = await uploadApi.image(file, 'institution')
    await orgApi.createImage({
      institutionId: institutionId.value,
      imageUrl: data.url,
      title: '教学环境',
      sort: images.value.length + 1
    })
    message.success('环境图片已上传')
    await loadImages()
  } finally { imageUploading.value = false }
  return false
}
// 图片说明保存：输入框失焦即触发，所以界面上没有单独的保存按钮
async function saveImage(row) {
  await orgApi.updateImage(row.id, { ...row })
  message.success('图片说明已保存')
}
// 删除环境图片
async function removeImage(row) {
  await orgApi.removeImage(row.id)
  message.success('图片已删除')
  await loadImages()
}
// 师资列表（教师数量少，一次性取回不分页）
async function loadTeachers() {
  teacherLoading.value = true
  try { teachers.value = await teacherApi.list() } finally { teacherLoading.value = false }
}
// 资质列表：接口分页，这里固定取前 200 条
async function loadQuals() {
  qualLoading.value = true
  try { const d = await qualificationApi.page({ current: 1, size: 200 }); quals.value = d.records } finally { qualLoading.value = false }
}
// 公告列表：接口分页，这里固定取前 200 条
async function loadNotices() {
  noticeLoading.value = true
  try { const d = await noticeApi.page({ current: 1, size: 200 }); notices.value = d.records } finally { noticeLoading.value = false }
}

// 打开教师弹窗：传 row 为编辑（整行回填），不传为新增（用默认值），两种状态共用一个弹窗
function openTeacher(row) {
  editingTeacher.value = row || null
  Object.assign(teacherForm, row ? {
    name: row.name, phone: row.phone || '', title: row.title, years: row.years, styles: row.styles,
    coursesText: row.coursesText, bio: row.bio, quote: row.quote, tags: row.tags,
    status: row.status, sort: row.sort || 0
  } : { name: '', phone: '', title: '', years: 0, styles: '', coursesText: '', bio: '', quote: '', tags: '', status: 'ACTIVE', sort: 0 })
  teacherOpen.value = true
}
// 保存教师：姓名与 11 位手机号必填（手机号用于教师微信登录匹配），教龄与排序转数字
async function submitTeacher() {
  if (!teacherForm.name) { message.warning('请填写教师姓名'); return }
  if (!/^1\d{10}$/.test(teacherForm.phone)) { message.warning('请填写正确的 11 位手机号'); return }
  teacherSaving.value = true
  try {
    const payload = { ...teacherForm, years: Number(teacherForm.years || 0), sort: Number(teacherForm.sort || 0) }
    if (editingTeacher.value) await teacherApi.update(editingTeacher.value.id, payload)
    else await teacherApi.create(payload)
    message.success('教师资料已保存')
    teacherOpen.value = false
    await loadTeachers()
  } finally { teacherSaving.value = false }
}
// 启停教师：整行回传并翻转状态；row 本身未被修改，所以提示语仍按旧状态判断
async function toggleTeacher(row) {
  await teacherApi.update(row.id, { ...row, status: row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' })
  message.success(row.status === 'ACTIVE' ? '教师已停用，小程序不再展示' : '教师已启用')
  await loadTeachers()
}

// 打开资质弹窗：row 为编辑，visible 由 1/0 转成布尔给 switch
function openQual(row) {
  editingQual.value = row || null
  Object.assign(qualForm, row ? {
    name: row.name, code: row.code, validUntil: row.validUntil,
    verifyStatus: row.verifyStatus, visible: row.visible === 1, sort: row.sort || 0
  } : { name: '', code: '', validUntil: '', verifyStatus: 'VERIFIED', visible: true, sort: 0 })
  qualOpen.value = true
}
// 保存资质：名称必填，visible 转 1/0、排序转数字
async function submitQual() {
  if (!qualForm.name) { message.warning('请填写资质名称'); return }
  qualSaving.value = true
  try {
    const payload = { ...qualForm, visible: qualForm.visible ? 1 : 0, sort: Number(qualForm.sort || 0) }
    if (editingQual.value) await qualificationApi.update(editingQual.value.id, payload)
    else await qualificationApi.create(payload)
    message.success('资质已保存')
    qualOpen.value = false
    await loadQuals()
  } finally { qualSaving.value = false }
}
// 切换资质是否在小程序展示：整行回传并翻转 visible
async function toggleQual(row) {
  await qualificationApi.update(row.id, { ...row, visible: row.visible === 1 ? 0 : 1 })
  message.success(row.visible === 1 ? '资质已隐藏' : '资质已展示')
  await loadQuals()
}
// 删除资质
async function removeQual(row) {
  await qualificationApi.remove(row.id)
  message.success('资质已删除')
  await loadQuals()
}

// 打开公告弹窗：编辑时把发布日期字符串转成 dayjs 供日期选择器使用
function openNotice(row) {
  editingNotice.value = row || null
  Object.assign(noticeForm, row ? {
    title: row.title, type: row.type, content: row.content || '',
    publishDate: row.publishDate ? dayjs(row.publishDate) : null, status: row.status
  } : { title: '', type: 'NOTICE', content: '', publishDate: null, status: 'PUBLISHED' })
  noticeOpen.value = true
}
// 保存公告：标题必填；发布日期格式化后再提交
async function submitNotice() {
  if (!noticeForm.title) { message.warning('请填写公告标题'); return }
  noticeSaving.value = true
  try {
    const payload = { ...noticeForm, publishDate: noticeForm.publishDate?.format('YYYY-MM-DD') }
    if (editingNotice.value) await noticeApi.update(editingNotice.value.id, payload)
    else await noticeApi.create(payload)
    message.success('公告已保存')
    noticeOpen.value = false
    await loadNotices()
  } finally { noticeSaving.value = false }
}
// 发布/隐藏公告：整行回传后在 PUBLISHED 与 HIDDEN 之间切换
async function toggleNotice(row) {
  await noticeApi.update(row.id, { ...row, status: row.status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED' })
  message.success(row.status === 'PUBLISHED' ? '公告已隐藏' : '公告已发布')
  await loadNotices()
}
// 删除公告
async function removeNotice(row) {
  await noticeApi.remove(row.id)
  message.success('公告已删除')
  await loadNotices()
}

// 切换页签：除机构资料外的三个列表按需加载
function onTab(key) {
  activeTab.value = key
  if (key === 'teachers') loadTeachers()
  if (key === 'quals') loadQuals()
  if (key === 'notices') loadNotices()
}
// 首屏并发加载机构资料、环境图片与师资列表（默认停在资料页签）
onMounted(async () => {
  await Promise.all([loadInfo(), loadImages(), loadTeachers()])
})
</script>

<template>
  <div>
    <!-- 页头：主操作按钮随页签变化（保存资料 / 新增教师 / 新增资质 / 新增公告） -->
    <div class="page-head">
      <div>
        <div class="eyebrow">INSTITUTION CONTENT</div>
        <h1>机构内容</h1>
        <p>小程序机构资料、师资团队、资质公示与公告统一维护</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="onTab(activeTab)"><ReloadOutlined /> 刷新</a-button>
        <a-button v-if="activeTab === 'info' && $auth.can('org','edit')" type="primary" size="small" :loading="infoSaving" @click="saveInfo">保存资料</a-button>
        <a-button v-else-if="activeTab === 'teachers' && $auth.can('org','create')" type="primary" size="small" @click="openTeacher()"><PlusOutlined /> 新增教师</a-button>
        <a-button v-else-if="activeTab === 'quals' && $auth.can('org','create')" type="primary" size="small" @click="openQual()"><PlusOutlined /> 新增资质</a-button>
        <a-button v-else-if="$auth.can('org','create')" type="primary" size="small" @click="openNotice()"><PlusOutlined /> 新增公告</a-button>
      </div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <!-- 四个页签：机构资料 / 师资团队 / 资质公示 / 公告管理 -->
      <a-tabs :active-key="activeTab" @change="onTab">
        <!-- 机构资料页签：基础信息表单 + 教学环境图片管理 -->
        <a-tab-pane key="info" tab="机构资料">
          <a-spin :spinning="infoLoading">
            <a-form layout="vertical">
              <a-row :gutter="12">
                <a-col :span="12"><a-form-item label="机构名称"><a-input v-model:value="info.name" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="品牌简称"><a-input v-model:value="info.brand" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="经营状态"><a-select v-model:value="info.status"><a-select-option value="OPEN">营业中</a-select-option><a-select-option value="PAUSED">暂停营业</a-select-option></a-select></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="创办年份"><a-input-number v-model:value="info.sinceYear" style="width:100%" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="首页主标语"><a-input v-model:value="info.slogan" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="首页简介"><a-textarea v-model:value="info.heroIntro" :rows="2" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="机构简介"><a-textarea v-model:value="info.intro" :rows="3" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="办学理念"><a-textarea v-model:value="info.philosophy" :rows="2" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="联系电话"><a-input v-model:value="info.phone" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="机构微信"><a-input v-model:value="info.wechat" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="营业时间"><a-input v-model:value="info.hours" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="机构地址"><a-input v-model:value="info.address" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="交通指引"><a-input v-model:value="info.traffic" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="经度"><a-input-number v-model:value="info.longitude" style="width:100%" :step="0.000001" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="纬度"><a-input-number v-model:value="info.latitude" style="width:100%" :step="0.000001" /></a-form-item></a-col>
              </a-row>
              <div class="login-note">小程序仅展示一个机构地址，用于 wx.openLocation 地图导航；不设校区切换。</div>
            </a-form>
            <!-- 教学环境图片区：上传即入库，说明文字失焦保存 -->
            <div class="org-images">
              <div class="between" style="margin-bottom:10px">
                <div class="card-title" style="margin:0">教学环境图片</div>
                <!-- 上传环境图片：before-upload 内完成上传与建记录 -->
                <a-upload :before-upload="beforeImageUpload" :show-upload-list="false" accept="image/*">
                  <a-button v-if="$auth.can('org','create')" size="small" :loading="imageUploading"><UploadOutlined /> 上传环境图片</a-button>
                </a-upload>
              </div>
              <a-spin :spinning="imageLoading">
                <div class="org-image-grid">
                  <div v-for="img in images" :key="img.id" class="org-image-card">
                    <a-image :src="img.imageUrl" :height="100" style="width:100%;object-fit:cover" />
                    <a-input v-model:value="img.title" size="small" placeholder="图片说明" @blur="saveImage(img)" />
                    <a-button v-if="$auth.can('org','delete')" type="text" size="small" danger @click="removeImage(img)"><DeleteOutlined /> 删除</a-button>
                  </div>
                  <a-upload :before-upload="beforeImageUpload" :show-upload-list="false" accept="image/*">
                    <div class="org-image-add">
                      <UploadOutlined />
                      <span>{{ imageUploading ? '上传中…' : '添加图片' }}</span>
                    </div>
                  </a-upload>
                </div>
                <div v-if="!images.length" class="empty" style="padding:20px 0">暂无环境图片，上传后在小程序首页与机构介绍展示</div>
              </a-spin>
            </div>
          </a-spin>
        </a-tab-pane>

        <!-- 师资团队页签 -->
        <a-tab-pane key="teachers" tab="师资团队">
          <!-- 教师表格：行内可编辑与启停 -->
          <a-table :data-source="teachers" :loading="teacherLoading" row-key="id" size="small" :pagination="false">
            <a-table-column title="教师" key="name" width="180">
              <template #default="{ record }"><b>{{ record.name }}</b><div class="muted" style="font-size:9px">{{ record.title }} · 教龄 {{ record.years }} 年</div></template>
            </a-table-column>
            <a-table-column title="擅长书体" data-index="styles" key="styles" width="150" />
            <a-table-column title="所授课程" data-index="coursesText" key="coursesText" />
            <a-table-column title="状态" key="status" width="80">
              <template #default="{ record }"><span class="tag" :class="record.status === 'ACTIVE' ? 'green' : 'gray'">{{ record.status === 'ACTIVE' ? '在职' : '停用' }}</span></template>
            </a-table-column>
            <a-table-column title="操作" key="actions" width="160">
              <template #default="{ record }"><a-button v-if="$auth.can('org','edit')" type="link" size="small" @click="openTeacher(record)">编辑</a-button><a-button v-if="$auth.can('org','edit')" type="link" size="small" @click="toggleTeacher(record)">{{ record.status === 'ACTIVE' ? '停用' : '启用' }}</a-button></template>
            </a-table-column>
          </a-table>
          <div class="footnote">教师账号由后台师资团队创建；教师微信登录后按手机号自动匹配，小程序不提供身份申请。</div>
        </a-tab-pane>

        <!-- 资质公示页签 -->
        <a-tab-pane key="quals" tab="资质公示">
          <!-- 资质表格：展示列是开关，切换即时生效 -->
          <a-table :data-source="quals" :loading="qualLoading" row-key="id" size="small" :pagination="false">
            <a-table-column title="资质名称" data-index="name" key="name" width="180" />
            <a-table-column title="编号" data-index="code" key="code" width="180" />
            <a-table-column title="有效期至" data-index="validUntil" key="validUntil" width="120" />
            <a-table-column title="核验状态" data-index="verifyStatus" key="verifyStatus" width="100" />
            <a-table-column title="展示" key="visible" width="80">
              <template #default="{ record }"><a-switch :checked="record.visible === 1" size="small" @change="toggleQual(record)" /></template>
            </a-table-column>
            <a-table-column title="操作" key="actions" width="140">
              <template #default="{ record }"><a-button v-if="$auth.can('org','edit')" type="link" size="small" @click="openQual(record)">编辑</a-button><a-popconfirm v-if="$auth.can('org','delete')" title="删除资质？" @confirm="removeQual(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm></template>
            </a-table-column>
          </a-table>
        </a-tab-pane>

        <!-- 公告管理页签 -->
        <a-tab-pane key="notices" tab="公告管理">
          <!-- 公告表格：类型与状态查表显示中文，行内可发布/隐藏/删除 -->
          <a-table :data-source="notices" :loading="noticeLoading" row-key="id" size="small" :pagination="false">
            <a-table-column title="标题" data-index="title" key="title" />
            <a-table-column title="类型" key="type" width="100">
              <template #default="{ record }">{{ noticeTypes[record.type] || record.type }}</template>
            </a-table-column>
            <a-table-column title="发布日期" data-index="publishDate" key="publishDate" width="110" />
            <a-table-column title="状态" key="status" width="90">
              <template #default="{ record }"><span class="tag" :class="record.status === 'PUBLISHED' ? 'green' : 'gray'">{{ noticeStatus[record.status] || record.status }}</span></template>
            </a-table-column>
            <a-table-column title="操作" key="actions" width="180">
              <template #default="{ record }"><a-button v-if="$auth.can('org','edit')" type="link" size="small" @click="openNotice(record)">编辑</a-button><a-button v-if="$auth.can('org','edit')" type="link" size="small" @click="toggleNotice(record)">{{ record.status === 'PUBLISHED' ? '隐藏' : '发布' }}</a-button><a-popconfirm v-if="$auth.can('org','delete')" title="删除公告？" @confirm="removeNotice(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm></template>
            </a-table-column>
          </a-table>
          <div class="footnote">公告在小程序「作品墙 → 机构公告」展示；本期不设活动、招生海报与投票评选。</div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 新增/编辑教师弹窗 -->
    <a-modal v-model:open="teacherOpen" :title="editingTeacher ? '编辑教师' : '新增教师'" ok-text="保存" cancel-text="取消" :confirm-loading="teacherSaving" @ok="submitTeacher">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="姓名" required><a-input v-model:value="teacherForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="手机号" required><a-input v-model:value="teacherForm.phone" maxlength="11" placeholder="用于教师微信登录匹配" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="职务 / 头衔"><a-input v-model:value="teacherForm.title" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="教龄（年）"><a-input-number v-model:value="teacherForm.years" style="width:100%" :min="0" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="擅长书体"><a-input v-model:value="teacherForm.styles" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="所授课程"><a-input v-model:value="teacherForm.coursesText" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="教学标签"><a-input v-model:value="teacherForm.tags" placeholder="逗号分隔" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="教师寄语"><a-input v-model:value="teacherForm.quote" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="个人简介"><a-textarea v-model:value="teacherForm.bio" :rows="3" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="teacherForm.status"><a-select-option value="ACTIVE">在职</a-select-option><a-select-option value="DISABLED">停用</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="排序"><a-input-number v-model:value="teacherForm.sort" style="width:100%" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 新增/编辑资质弹窗 -->
    <a-modal v-model:open="qualOpen" :title="editingQual ? '编辑资质' : '新增资质'" ok-text="保存" cancel-text="取消" :confirm-loading="qualSaving" @ok="submitQual">
      <a-form layout="vertical">
        <a-form-item label="资质名称" required><a-input v-model:value="qualForm.name" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="编号"><a-input v-model:value="qualForm.code" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="有效期至"><a-input v-model:value="qualForm.validUntil" placeholder="长期 / 2028-06-30" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="核验状态"><a-select v-model:value="qualForm.verifyStatus"><a-select-option value="VERIFIED">已核验</a-select-option><a-select-option value="PENDING">待更新</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="小程序展示"><a-switch v-model:checked="qualForm.visible" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 新增/编辑公告弹窗 -->
    <a-modal v-model:open="noticeOpen" :title="editingNotice ? '编辑公告' : '新增公告'" ok-text="保存" cancel-text="取消" :confirm-loading="noticeSaving" @ok="submitNotice">
      <a-form layout="vertical">
        <a-form-item label="公告标题" required><a-input v-model:value="noticeForm.title" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="公告类型"><a-select v-model:value="noticeForm.type"><a-select-option value="NOTICE">通知公告</a-select-option><a-select-option value="ADMISSION">招生信息</a-select-option><a-select-option value="HOLIDAY">放假安排</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="发布日期"><a-date-picker v-model:value="noticeForm.publishDate" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="noticeForm.status"><a-select-option value="PUBLISHED">展示中</a-select-option><a-select-option value="HIDDEN">已隐藏</a-select-option><a-select-option value="DRAFT">草稿</a-select-option></a-select></a-form-item></a-col>
        </a-row>
        <a-form-item label="公告内容"><a-textarea v-model:value="noticeForm.content" :rows="4" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.org-images { margin-top: 8px; padding-top: 14px; border-top: 1px dashed #E7DFD0; }
.org-image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px; }
.org-image-card { border: 1px solid #F0E9DC; border-radius: 9px; padding: 8px; background: #FFFDF8; }
.org-image-card :deep(.ant-image) { display: block; border-radius: 6px; overflow: hidden; margin-bottom: 6px; }
.org-image-card .ant-input { margin-bottom: 4px; }
.org-image-add {
  height: 150px; border: 1px dashed #D8CEBC; border-radius: 9px; display: flex;
  flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  color: #948C7C; font-size: 11px; background: #F9F5EC;
}
</style>
