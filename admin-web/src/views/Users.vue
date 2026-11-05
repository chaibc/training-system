/**
 * 用户与权限页：四个页签 —— 用户账号（分页/筛选/新建教务账号/编辑/重置密码/解绑微信/启停）、
 * 角色权限（模块 × 动作的权限矩阵）、家长绑定（待审核、已绑定、手工新增）、登录与变更记录（审计日志）。
 * 接口：userApi.page/create/update/resetPassword/toggleStatus/unbindWechat/roles/updateRole/
 *       pendingBindings/activeBindings/approveBinding/rejectBinding/addBinding/unbindBinding/
 *       auditLogs/parentOptions/studentOptions（api/index.js）
 *
 * 仅管理者可进入（路由 meta.managerOnly + users 权限点）；管理者账号唯一，页面没有物理删除入口，
 * 账号只能停用。权限变更对下一次登录生效。
 */
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { userApi } from '../api'
import {
  PlusOutlined, ReloadOutlined, SafetyCertificateOutlined, TeamOutlined
} from '@ant-design/icons-vue'

// 权限矩阵的模块清单：key 必须与后端权限点 moduleKey、路由 meta.module 保持一致；
// special 只是给使用者看的特殊操作说明，不参与权限判断
const MODULES = [
  { key: 'dashboard', name: '经营概览', special: '导出月报' },
  { key: 'reports', name: '统计报表', special: '导出 Excel' },
  { key: 'students', name: '学员管理', special: '调整课时 / 状态变更' },
  { key: 'enrollments', name: '报名与缴费', special: '确认报名 / 登记缴费' },
  { key: 'courses', name: '课程与定价', special: '定价 / 大纲 / 上下架' },
  { key: 'classes', name: '班级与排课', special: '排课 / 调课 / 代课' },
  { key: 'attendance', name: '点名与消课', special: '点名 / 修正' },
  { key: 'leave', name: '请假审批', special: '审批 / 补课' },
  { key: 'works', name: '作品管理', special: '审核 / 授权 / 评论' },
  { key: 'org', name: '机构内容', special: '师资 / 资质 / 公告' },
  { key: 'basic', name: '基础数据', special: '字典维护' },
  { key: 'settings', name: '业务规则', special: '规则配置' },
  { key: 'users', name: '用户与权限', special: '角色 / 绑定审核' }
]

const activeTab = ref('accounts')
const userLoading = ref(false)
const users = ref([])
const userTotal = ref(0)
// 账号列表查询条件（服务端分页）
const userQuery = reactive({ current: 1, size: 10, type: undefined, status: undefined, keyword: '' })

// 角色列表；activeRole 是当前选中的角色，permRows 是它展开后的模块权限矩阵
const roles = ref([])
const activeRole = ref(null)
// 角色数据范围：ALL 全机构 / OWN_CLASS 所带班级 / OWN_CHILDREN 本人子女 / SELF 仅本人
const roleScope = ref('ALL')
const permRows = ref([])
const roleLoading = ref(false)

// 家长绑定：待审核申请、已生效关系，以及新增绑定弹窗用的家长/学员下拉
const pending = ref([])
const activeBindings = ref([])
const bindingLoading = ref(false)
const parents = ref([])
const studentOptions = ref([])
const addBindOpen = ref(false)
const bindForm = reactive({ parentUserId: undefined, studentId: undefined, relation: 'MOTHER', primary: false })

const auditLoading = ref(false)
const auditRows = ref([])
const auditTotal = ref(0)
// 审计日志查询条件（服务端分页，默认每页 20 条）
const auditQuery = reactive({ current: 1, size: 20, actionType: undefined, keyword: '' })

const staffOpen = ref(false)
const staffSaving = ref(false)
// 新建后台账号表单：角色固定为教务，密码留空时由后端生成初始密码
const staffForm = reactive({ name: '', username: '', phone: '', password: '' })
const editOpen = ref(false)
const editSaving = ref(false)
// 编辑账号表单：id 为被编辑账号，type 用于判断用户名是否可改（仅后台账号可改）
const editForm = reactive({ id: null, name: '', username: '', phone: '', phoneMasked: false, type: 'ADMIN' })
const resetOpen = ref(false)
// 重置密码弹窗：后端返回的临时密码与对应账号名
const resetPassword = ref('')
const resetName = ref('')

// 账号表格列：类型/角色/微信/状态都要在 bodyCell 里翻译成中文
const userColumns = [
  { title: '用户', key: 'name', width: 190 },
  { title: '类型', dataIndex: 'userType', key: 'userType', width: 80 },
  { title: '角色', key: 'role', width: 110 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 120 },
  { title: '微信', key: 'wechat', width: 85 },
  { title: '关联数据', key: 'related', width: 200 },
  { title: '状态', key: 'status', width: 80 },
  { title: '最近登录', key: 'lastLoginAt', width: 130 },
  { title: '操作', key: 'actions', width: 250 }
]

// 枚举中文映射：用户类型、账号状态、亲属关系
const typeText = { ADMIN: '后台账号', TEACHER: '教师', PARENT: '家长' }
const statusText = { ENABLED: '启用', DISABLED: '停用', LOCKED: '锁定' }
const relationText = { MOTHER: '母亲', FATHER: '父亲', GRANDPARENT: '祖父母', OTHER: '其他监护人' }

// 当前选中的是否管理者角色：管理者权限不可削减，矩阵与数据范围整体禁用
const isManagerRole = computed(() => activeRole.value?.code === 'ADMIN')
// 教师/家长是小程序身份，不走后台权限矩阵，选中时只展示说明文字
const isMiniRole = computed(() => ['TEACHER', 'PARENT'].includes(activeRole.value?.code))

// 审计日志表格列：时间统一用 customRender 格式化
const auditColumns = [
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 140,
    customRender: ({ text }) => text ? dayjs(text).format('MM-DD HH:mm') : '—' },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '类型', dataIndex: 'actionType', key: 'actionType', width: 100 },
  { title: '对象', dataIndex: 'targetType', key: 'targetType', width: 110 },
  { title: '内容', dataIndex: 'content', key: 'content' },
  { title: '结果', dataIndex: 'result', key: 'result', width: 70 },
  { title: 'IP', dataIndex: 'ip', key: 'ip', width: 110 }
]

// 账号分页查询；空的筛选条件转 undefined，避免后端把空串当条件
async function loadUsers() {
  userLoading.value = true
  try {
    const data = await userApi.page({
      current: userQuery.current,
      size: userQuery.size,
      type: userQuery.type || undefined,
      status: userQuery.status || undefined,
      keyword: userQuery.keyword || undefined
    })
    users.value = data.records
    userTotal.value = data.total
  } finally {
    userLoading.value = false
  }
}

// 拉角色列表：首次自动选中第一个角色，已选中时用最新数据刷新右侧矩阵
async function loadRoles() {
  roleLoading.value = true
  try {
    roles.value = await userApi.roles()
    if (!activeRole.value && roles.value.length) selectRole(roles.value[0])
    else if (activeRole.value) {
      const fresh = roles.value.find((r) => r.id === activeRole.value.id)
      if (fresh) selectRole(fresh)
    }
  } finally {
    roleLoading.value = false
  }
}

// 选中角色后把权限展开成「模块 × 动作」矩阵；后端未返回的权限点一律按无权限(false)处理
function selectRole(role) {
  activeRole.value = role
  roleScope.value = role.dataScope || 'ALL'
  permRows.value = MODULES.map((m) => {
    const p = (role.permissions || []).find((x) => x.moduleKey === m.key)
    return {
      moduleKey: m.key,
      name: m.name,
      special: m.special,
      canView: !!p?.canView,
      canCreate: !!p?.canCreate,
      canEdit: !!p?.canEdit,
      canDelete: !!p?.canDelete,
      canSpecial: !!p?.canSpecial
    }
  })
}

// 保存角色权限：管理者与小程序身份不可修改，直接返回；提交后用返回的角色列表刷新矩阵
async function saveRole() {
  if (!activeRole.value || isMiniRole.value) return
  const data = await userApi.updateRole(activeRole.value.id, {
    dataScope: roleScope.value,
    permissions: permRows.value.map((p) => ({
      moduleKey: p.moduleKey,
      canView: p.canView,
      canCreate: p.canCreate,
      canEdit: p.canEdit,
      canDelete: p.canDelete,
      canSpecial: p.canSpecial
    }))
  })
  roles.value = data
  const fresh = data.find((r) => r.id === activeRole.value.id)
  if (fresh) selectRole(fresh)
  message.success('角色权限已保存，对下一次登录生效')
}

// 并发加载待审核申请、已绑定关系与两个下拉数据源（家长、学员）
async function loadBindings() {
  bindingLoading.value = true
  try {
    const [p, a, po, so] = await Promise.all([
      userApi.pendingBindings(),
      userApi.activeBindings(),
      userApi.parentOptions(),
      userApi.studentOptions()
    ])
    pending.value = p
    activeBindings.value = a
    parents.value = po
    studentOptions.value = so
  } finally {
    bindingLoading.value = false
  }
}

// 审计日志分页查询
async function loadAudit() {
  auditLoading.value = true
  try {
    const data = await userApi.auditLogs({
      current: auditQuery.current,
      size: auditQuery.size,
      actionType: auditQuery.actionType || undefined,
      keyword: auditQuery.keyword || undefined
    })
    auditRows.value = data.records
    auditTotal.value = data.total
  } finally {
    auditLoading.value = false
  }
}

// 切换页签：角色/绑定/日志三个页签首次进入时才加载
function onTabChange(key) {
  activeTab.value = key
  if (key === 'roles') loadRoles()
  if (key === 'bindings') loadBindings()
  if (key === 'logs') loadAudit()
}

// 打开新建账号弹窗：清空表单，避免残留上一次输入
function openCreate() {
  Object.assign(staffForm, { name: '', username: '', phone: '', password: '' })
  staffOpen.value = true
}

// 创建后台账号：姓名、用户名、11 位手机号必填；密码留空不传，由后端生成初始密码
async function submitCreate() {
  if (!staffForm.name || !staffForm.username || !/^1\d{10}$/.test(staffForm.phone)) {
    message.warning('请填写姓名、用户名和正确的手机号')
    return
  }
  staffSaving.value = true
  try {
    const row = await userApi.create({ ...staffForm, password: staffForm.password || undefined })
    message.success(`账号已创建${row.initialPassword ? '，初始密码：' + row.initialPassword : ''}`)
    staffOpen.value = false
    await loadUsers()
  } finally {
    staffSaving.value = false
  }
}

// 打开编辑弹窗：列表手机号是脱敏的（138****1234），不能去掉 * 回填——
// 否则用户只改姓名保存时会把 1381234 当成真实号码提交。含 * 时置空并提示重新输入，不填则按「未修改」提交。
function openEdit(row) {
  const masked = (row.phone || '').includes('*')
  Object.assign(editForm, {
    id: row.id, name: row.name, username: row.username || '',
    phone: masked ? '' : (row.phone || ''),
    phoneMasked: masked,
    type: row.userType
  })
  editOpen.value = true
}

// 保存账号资料：用户名/手机号为空时传 undefined，让后端按「未修改」处理
async function submitEdit() {
  // 手机号选填；填了就必须是 11 位大陆号码，避免脱敏残留或半截号码写进库
  if (editForm.phone && !/^1\d{10}$/.test(editForm.phone)) {
    return message.warning('手机号格式不正确，应为 11 位数字')
  }
  editSaving.value = true
  try {
    await userApi.update(editForm.id, {
      name: editForm.name,
      username: editForm.username || undefined,
      phone: editForm.phone || undefined
    })
    message.success('账号资料已保存')
    editOpen.value = false
    await loadUsers()
  } finally {
    editSaving.value = false
  }
}

// 重置密码：后端返回新的临时密码，直接在弹窗里展示
async function doReset(row) {
  const data = await userApi.resetPassword(row.id)
  resetPassword.value = data.password
  resetName.value = row.name
  resetOpen.value = true
}

// 复制临时密码：浏览器不支持剪贴板（非 HTTPS 等）时降级为手动复制的提示
async function copyPassword() {
  try {
    await navigator.clipboard.writeText(resetPassword.value)
    message.success('临时密码已复制')
  } catch {
    message.info('请手动复制：' + resetPassword.value)
  }
}

// 启停账号：后端返回最新状态，提示语按返回值判断而不是按旧行数据
async function toggleStatus(row) {
  const data = await userApi.toggleStatus(row.id)
  message.success(data.status === 'DISABLED' ? '账号已停用' : '账号已启用')
  await loadUsers()
}

// 解绑微信：解绑后该账号下次微信登录需要重新匹配
async function unbind(row) {
  await userApi.unbindWechat(row.id)
  message.success('微信已解绑，重新登录需再次匹配')
  await loadUsers()
}

// 审核通过绑定/解绑申请：通过后家长与学员的关系立即生效（解绑申请则是解除）
async function approveBinding(row) {
  await userApi.approveBinding(row.id)
  message.success('绑定已通过')
  await loadBindings()
}

// 驳回绑定/解绑申请：原有的绑定关系保持不变
async function rejectBinding(row) {
  await userApi.rejectBinding(row.id)
  message.success('绑定已驳回')
  await loadBindings()
}

// 解除已生效的绑定关系（管理员直接操作，不需要对方同意）
async function unbindBinding(row) {
  await userApi.unbindBinding(row.id)
  message.success('绑定关系已解除')
  await loadBindings()
}

// 手工新增绑定：家长与学员都必须选；主联系人只能有一个，由后端纠正其它主联系人
async function submitBinding() {
  if (!bindForm.parentUserId || !bindForm.studentId) {
    message.warning('请选择家长和学员')
    return
  }
  await userApi.addBinding({ ...bindForm })
  message.success('绑定关系已建立')
  addBindOpen.value = false
  await loadBindings()
}

// 首屏只加载账号列表，其它页签切换时再取
onMounted(async () => {
  await loadUsers()
  // 顶部指标卡的「待审核绑定」要在登录后立即有值，不能等用户切到绑定页签才加载
  loadBindings()
})
</script>

<template>
  <div>
    <!-- 页头：标题、刷新与新建后台账号 -->
    <div class="page-head">
      <div>
        <div class="eyebrow">USERS & PERMISSIONS</div>
        <h1>用户与权限</h1>
        <p>仅管理者可进入；账号、角色、数据范围与家长绑定审核 · 关键操作留痕</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" @click="loadUsers"><ReloadOutlined /> 刷新</a-button>
        <a-button type="primary" size="small" @click="openCreate"><PlusOutlined /> 新建后台账号</a-button>
      </div>
    </div>

    <!-- 顶部指标卡：按当前页账号列表统计各类型数量与待审核绑定数 -->
    <div class="stats">
      <div class="stat-card"><div class="lbl"><TeamOutlined /> 后台账号</div><div class="v">{{ users.filter(u => u.userType === 'ADMIN').length }}<em>个</em></div><div class="foot">管理者 1 · 教务若干</div></div>
      <div class="stat-card jade"><div class="lbl">教师账号</div><div class="v">{{ users.filter(u => u.userType === 'TEACHER').length }}<em>个</em></div><div class="foot">后台创建 · 微信手机号匹配</div></div>
      <div class="stat-card blue"><div class="lbl">家长账号</div><div class="v">{{ users.filter(u => u.userType === 'PARENT').length }}<em>个</em></div><div class="foot">微信登录自动创建</div></div>
      <div class="stat-card red"><div class="lbl">待审核绑定</div><div class="v">{{ pending.length }}<em>条</em></div><div class="foot">家长-学员关系</div></div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <!-- 四个页签：用户账号 / 角色权限 / 家长绑定 / 登录与变更记录 -->
      <a-tabs :active-key="activeTab" @change="onTabChange">
        <!-- 用户账号页签 -->
        <a-tab-pane key="accounts" tab="用户账号">
          <!-- 顶部筛选区：姓名/用户名/手机号关键字、类型与状态 -->
          <div class="filters">
            <a-input v-model:value="userQuery.keyword" placeholder="搜索姓名 / 用户名 / 手机号" allow-clear style="width:220px" @press-enter="() => { userQuery.current = 1; loadUsers() }" />
            <a-select v-model:value="userQuery.type" placeholder="全部类型" allow-clear style="width:130px" @change="() => { userQuery.current = 1; loadUsers() }">
              <a-select-option value="ADMIN">后台账号</a-select-option>
              <a-select-option value="TEACHER">教师</a-select-option>
              <a-select-option value="PARENT">家长</a-select-option>
            </a-select>
            <a-select v-model:value="userQuery.status" placeholder="全部状态" allow-clear style="width:120px" @change="() => { userQuery.current = 1; loadUsers() }">
              <a-select-option value="ENABLED">启用</a-select-option>
              <a-select-option value="DISABLED">停用</a-select-option>
            </a-select>
            <a-button size="small" @click="() => { userQuery.current = 1; loadUsers() }">查询</a-button>
            <div class="sp"></div>
            <span class="muted small">共 {{ userTotal }} 个账号 · 学员不单独建账号</span>
          </div>
          <!-- 账号表格：管理者行显示「唯一」且不出现停用按钮，教师手机号用于微信匹配 -->
          <a-table :columns="userColumns" :data-source="users" :loading="userLoading" row-key="id" size="small"
                   :pagination="{ current: userQuery.current, pageSize: userQuery.size, total: userTotal, showSizeChanger: false }"
                   @change="(p) => { userQuery.current = p.current; loadUsers() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div style="display:flex;align-items:center;gap:8px">
                  <a-avatar :size="28" style="background:#7A5B3D;font-size:10px">{{ record.name.slice(0,1) }}</a-avatar>
                  <div><b>{{ record.name }}</b><div class="muted" style="font-size:9px">{{ record.username || record.phone }}</div></div>
                </div>
              </template>
              <template v-if="column.key === 'userType'">{{ typeText[record.userType] || record.userType }}</template>
              <template v-if="column.key === 'role'">
                <span class="tag" :class="record.roleCode === 'ADMIN' ? 'red' : record.roleCode === 'TEACHER' ? 'blue' : record.roleCode === 'ACADEMIC' ? 'gold' : ''">{{ record.roleName }}</span>
                <span v-if="record.manager" class="tag gold" style="margin-left:4px">唯一</span>
              </template>
              <template v-if="column.key === 'wechat'">
                <span class="tag" :class="record.wechatBound ? 'green' : 'gray'">{{ record.wechatBound ? '已绑定' : '未绑定' }}</span>
              </template>
              <template v-if="column.key === 'related'">{{ record.related }}</template>
              <template v-if="column.key === 'status'">
                <span class="tag" :class="record.status === 'ENABLED' ? 'green' : 'gray'">{{ statusText[record.status] || record.status }}</span>
              </template>
              <template v-if="column.key === 'lastLoginAt'">{{ record.lastLoginAt ? dayjs(record.lastLoginAt).format('MM-DD HH:mm') : '—' }}</template>
              <template v-if="column.key === 'actions'">
                <div class="ops">
                  <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
                  <a-button v-if="record.userType === 'ADMIN'" type="link" size="small" @click="doReset(record)">重置密码</a-button>
                  <a-button v-if="record.wechatBound" type="link" size="small" danger @click="unbind(record)">解绑微信</a-button>
                  <a-button v-if="record.canDisable" type="link" size="small" danger @click="toggleStatus(record)">{{ record.status === 'DISABLED' ? '启用' : '停用' }}</a-button>
                  <span v-if="record.manager" class="muted" style="font-size:9px;align-self:center">管理者唯一</span>
                </div>
              </template>
            </template>
          </a-table>
          <div class="footnote">后台账号由管理者创建；教师账号由后台师资团队创建；家长账号首次微信登录自动创建。管理者账号唯一且不可停用；其他账号只停用、不物理删除。</div>
        </a-tab-pane>

        <!-- 角色权限页签：左侧内置角色列表，右侧模块权限矩阵 -->
        <a-tab-pane key="roles" tab="角色权限">
          <a-spin :spinning="roleLoading">
            <div class="grid-2" style="align-items:start">
              <div class="paper-card card-pad" style="border:none;padding:0">
                <div class="card-title">内置角色</div>
                <!-- 内置角色列表：角色固定，不可新增/删除 -->
                <div class="role-list">
                  <button v-for="role in roles" :key="role.id" class="role-card" :class="{ on: activeRole?.id === role.id }" @click="selectRole(role)">
                    <span class="av">{{ role.name.slice(0,1) }}</span>
                    <div><b>{{ role.name }}</b><span>{{ role.description }}</span></div>
                    <span class="num">{{ role.userCount }}</span>
                  </button>
                </div>
                <div class="footnote">首期不做自定义角色；角色固定，权限可按模块微调。</div>
              </div>
              <div class="paper-card card-pad" style="border:none;padding:0">
                <div class="card-title">角色权限 · {{ activeRole?.name || '—' }}</div>
                <!-- 数据范围选择：管理者与小程序身份不可调整，统一禁用 -->
                <div class="filters">
                  <a-select v-model:value="roleScope" style="width:130px" :disabled="isManagerRole || isMiniRole">
                    <a-select-option value="ALL">全机构</a-select-option>
                    <a-select-option value="OWN_CLASS">所带班级</a-select-option>
                    <a-select-option value="OWN_CHILDREN">本人子女</a-select-option>
                    <a-select-option value="SELF">仅本人</a-select-option>
                  </a-select>
                  <div class="sp"></div>
                  <span class="tag gold">{{ isManagerRole ? '内置角色 · 不可修改' : '内置角色' }}</span>
                </div>
                <div v-if="isMiniRole" class="empty">教师 / 家长为小程序身份，权限由小程序页面与数据范围控制，不在此矩阵中。</div>
                <template v-else>
                  <!-- 权限矩阵：每个模块一行，列对应查看/新增/编辑/删除；管理者权限全禁用 -->
                  <a-table :data-source="permRows" :pagination="false" row-key="moduleKey" size="small">
                    <a-table-column title="模块" data-index="name" key="name">
                      <template #default="{ record }">
                        <b>{{ record.name }}</b><div class="muted" style="font-size:8px">{{ record.special }}</div>
                      </template>
                    </a-table-column>
                    <a-table-column title="查看" key="view" width="55" align="center">
                      <template #default="{ record }"><a-checkbox v-model:checked="record.canView" :disabled="isManagerRole" /></template>
                    </a-table-column>
                    <a-table-column title="新增" key="create" width="55" align="center">
                      <template #default="{ record }"><a-checkbox v-model:checked="record.canCreate" :disabled="isManagerRole" /></template>
                    </a-table-column>
                    <a-table-column title="编辑" key="edit" width="55" align="center">
                      <template #default="{ record }"><a-checkbox v-model:checked="record.canEdit" :disabled="isManagerRole" /></template>
                    </a-table-column>
                    <a-table-column title="删除" key="delete" width="55" align="center">
                      <template #default="{ record }"><a-checkbox v-model:checked="record.canDelete" :disabled="isManagerRole" /></template>
                    </a-table-column>
                    <a-table-column title="特殊操作" data-index="special" key="special" width="180" />
                  </a-table>
                  <div style="display:flex;gap:8px;margin-top:12px;align-items:center">
                    <a-button type="primary" size="small" :disabled="isManagerRole" @click="saveRole">保存权限</a-button>
                    <span class="muted" style="font-size:9px">权限变更对下一次登录生效；管理者权限不可削减。</span>
                  </div>
                </template>
              </div>
            </div>
          </a-spin>
        </a-tab-pane>

        <!-- 家长绑定页签：左边待审核申请，右边已生效关系 -->
        <a-tab-pane key="bindings" tab="家长绑定">
          <!-- 绑定页签工具栏：刷新与手工新增绑定 -->
          <div class="filters">
            <a-button size="small" @click="loadBindings"><ReloadOutlined /> 刷新</a-button>
            <a-button type="primary" size="small" @click="addBindOpen = true"><PlusOutlined /> 新增绑定</a-button>
            <div class="sp"></div>
            <span class="muted small">一个学员最多 2 个有效监护人，其中 1 个主联系人</span>
          </div>
          <a-spin :spinning="bindingLoading">
            <div class="grid-2" style="align-items:start">
              <div class="paper-card card-pad" style="border:none;padding:0">
                <div class="card-title">待审核绑定 / 解绑申请</div>
                <!-- 待审核申请：source 为 UNBIND 时按钮文案变成「同意解绑 / 保留绑定」 -->
                <a-table :data-source="pending" :pagination="false" row-key="id" size="small">
                  <a-table-column title="申请人" key="parent" width="100">
                    <template #default="{ record }"><b>{{ record.parentName }}</b><div class="muted" style="font-size:8px">{{ record.parentPhone }}</div></template>
                  </a-table-column>
                  <a-table-column title="关联对象" key="student" width="100">
                    <template #default="{ record }">{{ record.studentName }}<div class="muted" style="font-size:8px">{{ record.studentNo }}</div></template>
                  </a-table-column>
                  <a-table-column title="关系 / 验证" key="verify">
                    <template #default="{ record }">{{ record.relationText }}<div class="muted" style="font-size:8px">{{ record.verifyText }}</div></template>
                  </a-table-column>
                  <a-table-column title="操作" key="ops" width="120">
                    <template #default="{ record }">
                      <a-button type="link" size="small" @click="approveBinding(record)">
                        {{ record.source === 'UNBIND' ? '同意解绑' : '通过' }}
                      </a-button>
                      <a-button type="link" size="small" danger @click="rejectBinding(record)">
                        {{ record.source === 'UNBIND' ? '保留绑定' : '驳回' }}
                      </a-button>
                    </template>
                  </a-table-column>
                </a-table>
                <div v-if="!pending.length" class="empty">暂无待审核绑定</div>
              </div>
              <div class="paper-card card-pad" style="border:none;padding:0">
                <div class="card-title">已绑定关系</div>
                <!-- 已生效绑定关系，可直接解绑 -->
                <a-table :data-source="activeBindings" :pagination="false" row-key="id" size="small">
                  <a-table-column title="家长" key="parent" width="100"><template #default="{ record }"><b>{{ record.parentName }}</b></template></a-table-column>
                  <a-table-column title="学员" key="student" width="100"><template #default="{ record }">{{ record.studentName }}</template></a-table-column>
                  <a-table-column title="关系" data-index="relationText" key="relation" width="70" />
                  <a-table-column title="优先级" key="primary" width="90">
                    <template #default="{ record }"><span v-if="record.primary" class="tag gold">主联系人</span><span v-else class="muted" style="font-size:9px">普通</span></template>
                  </a-table-column>
                  <a-table-column title="操作" key="ops" width="70">
                    <template #default="{ record }"><a-button type="link" size="small" danger @click="unbindBinding(record)">解绑</a-button></template>
                  </a-table-column>
                </a-table>
              </div>
            </div>
          </a-spin>
        </a-tab-pane>

        <!-- 审计日志页签：登录、账号变更、绑定审核等操作留痕 -->
        <a-tab-pane key="logs" tab="登录与变更记录">
          <!-- 顶部筛选区：操作人/内容关键字与操作类型 -->
          <div class="filters">
            <a-input v-model:value="auditQuery.keyword" placeholder="搜索操作人 / 内容" allow-clear style="width:220px" @press-enter="() => { auditQuery.current = 1; loadAudit() }" />
            <a-select v-model:value="auditQuery.actionType" placeholder="全部类型" allow-clear style="width:140px" @change="() => { auditQuery.current = 1; loadAudit() }">
              <a-select-option value="登录">登录</a-select-option>
              <a-select-option value="账号变更">账号变更</a-select-option>
              <a-select-option value="绑定审核">绑定审核</a-select-option>
            </a-select>
            <a-button size="small" @click="() => { auditQuery.current = 1; loadAudit() }">查询</a-button>
          </div>
          <!-- 审计日志表格：结果列 SUCCESS 标绿，其余标红 -->
          <a-table :columns="auditColumns" :data-source="auditRows" :loading="auditLoading" row-key="id" size="small"
                   :pagination="{ current: auditQuery.current, pageSize: auditQuery.size, total: auditTotal, showSizeChanger: false }"
                   @change="(p) => { auditQuery.current = p.current; loadAudit() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'result'"><span class="tag" :class="record.result === 'SUCCESS' ? 'green' : 'red'">{{ record.result === 'SUCCESS' ? '成功' : '失败' }}</span></template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 新建后台账号弹窗：只建教务账号，角色固定 -->
    <a-modal v-model:open="staffOpen" title="新建后台账号" ok-text="创建账号" cancel-text="取消" :confirm-loading="staffSaving" @ok="submitCreate">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="姓名" required><a-input v-model:value="staffForm.name" placeholder="真实姓名" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="用户名" required><a-input v-model:value="staffForm.username" placeholder="登录用户名" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="手机号" required><a-input v-model:value="staffForm.phone" maxlength="11" placeholder="11 位手机号" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="角色"><a-input value="教务（固定）" disabled /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="初始密码"><a-input v-model:value="staffForm.password" placeholder="留空则自动生成，首登强制改密" /></a-form-item></a-col>
        </a-row>
        <div class="login-note">管理者账号唯一，不可新增；此处仅创建教务账号。</div>
      </a-form>
    </a-modal>

    <!-- 编辑账号弹窗：用户名只有后台账号可改（教师/家长用手机号匹配微信） -->
    <a-modal v-model:open="editOpen" title="编辑账号" ok-text="保存" cancel-text="取消" :confirm-loading="editSaving" @ok="submitEdit">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="姓名"><a-input v-model:value="editForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="用户名"><a-input v-model:value="editForm.username" :disabled="editForm.type !== 'ADMIN'" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="手机号"><a-input v-model:value="editForm.phone" maxlength="11" :placeholder="editForm.phoneMasked ? '原号码已脱敏，如需修改请重新输入' : ''" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="类型"><a-input :value="typeText[editForm.type]" disabled /></a-form-item></a-col>
        </a-row>
        <div class="login-note">教师 / 家长手机号变更后会自动解除微信绑定，需要重新登录匹配。</div>
      </a-form>
    </a-modal>

    <!-- 重置密码结果弹窗：展示临时密码并支持复制，无确认按钮 -->
    <a-modal v-model:open="resetOpen" title="重置密码" :footer="null">
      <div style="font-size:11px;line-height:1.9">
        账号：<b>{{ resetName }}</b><br>
        临时密码：<b style="font-family:Georgia,serif;font-size:20px;letter-spacing:1px">{{ resetPassword }}</b>
      </div>
      <div class="login-note">请通过安全渠道告知本人，首次登录强制修改密码；旧密码立即失效。</div>
      <div style="display:flex;gap:8px;margin-top:14px;justify-content:flex-end">
        <a-button @click="resetOpen = false">关闭</a-button>
        <a-button type="primary" @click="copyPassword">复制临时密码</a-button>
      </div>
    </a-modal>

    <!-- 新增家长绑定弹窗 -->
    <a-modal v-model:open="addBindOpen" title="新增家长绑定" ok-text="保存绑定" cancel-text="取消" @ok="submitBinding">
      <a-form layout="vertical">
        <a-form-item label="家长账号" required>
          <a-select v-model:value="bindForm.parentUserId" show-search option-filter-prop="label" placeholder="选择家长">
            <a-select-option v-for="p in parents" :key="p.id" :value="p.id" :label="p.name + p.phone">{{ p.name }} · {{ p.phone }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="学员" required>
          <a-select v-model:value="bindForm.studentId" show-search option-filter-prop="label" placeholder="选择学员">
            <a-select-option v-for="s in studentOptions" :key="s.id" :value="s.id" :label="s.name + s.studentNo">{{ s.name }} · {{ s.studentNo }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="关系"><a-select v-model:value="bindForm.relation"><a-select-option value="MOTHER">母亲</a-select-option><a-select-option value="FATHER">父亲</a-select-option><a-select-option value="GRANDPARENT">祖父母</a-select-option><a-select-option value="OTHER">其他监护人</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="主联系人"><a-switch v-model:checked="bindForm.primary" /></a-form-item></a-col>
        </a-row>
        <div class="login-note">一个学员最多 2 个有效监护人；设置主联系人会自动取消其他主联系人。</div>
      </a-form>
    </a-modal>
  </div>
</template>
