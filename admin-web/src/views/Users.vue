<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { userApi } from '../api'
import {
  PlusOutlined, ReloadOutlined, SafetyCertificateOutlined, TeamOutlined
} from '@ant-design/icons-vue'

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
const userQuery = reactive({ current: 1, size: 10, type: undefined, status: undefined, keyword: '' })

const roles = ref([])
const activeRole = ref(null)
const roleScope = ref('ALL')
const permRows = ref([])
const roleLoading = ref(false)

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
const auditQuery = reactive({ current: 1, size: 20, actionType: undefined, keyword: '' })

const staffOpen = ref(false)
const staffSaving = ref(false)
const staffForm = reactive({ name: '', username: '', phone: '', password: '' })
const editOpen = ref(false)
const editSaving = ref(false)
const editForm = reactive({ id: null, name: '', username: '', phone: '', type: 'ADMIN' })
const resetOpen = ref(false)
const resetPassword = ref('')
const resetName = ref('')

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

const typeText = { ADMIN: '后台账号', TEACHER: '教师', PARENT: '家长' }
const statusText = { ENABLED: '启用', DISABLED: '停用', LOCKED: '锁定' }
const relationText = { MOTHER: '母亲', FATHER: '父亲', GRANDPARENT: '祖父母', OTHER: '其他监护人' }

const isManagerRole = computed(() => activeRole.value?.code === 'ADMIN')
const isMiniRole = computed(() => ['TEACHER', 'PARENT'].includes(activeRole.value?.code))

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

function onTabChange(key) {
  activeTab.value = key
  if (key === 'roles') loadRoles()
  if (key === 'bindings') loadBindings()
  if (key === 'logs') loadAudit()
}

function openCreate() {
  Object.assign(staffForm, { name: '', username: '', phone: '', password: '' })
  staffOpen.value = true
}

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

function openEdit(row) {
  Object.assign(editForm, {
    id: row.id, name: row.name, username: row.username || '',
    phone: (row.phone || '').replace(/\*/g, ''), type: row.userType
  })
  editOpen.value = true
}

async function submitEdit() {
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

async function doReset(row) {
  const data = await userApi.resetPassword(row.id)
  resetPassword.value = data.password
  resetName.value = row.name
  resetOpen.value = true
}

async function copyPassword() {
  try {
    await navigator.clipboard.writeText(resetPassword.value)
    message.success('临时密码已复制')
  } catch {
    message.info('请手动复制：' + resetPassword.value)
  }
}

async function toggleStatus(row) {
  const data = await userApi.toggleStatus(row.id)
  message.success(data.status === 'DISABLED' ? '账号已停用' : '账号已启用')
  await loadUsers()
}

async function unbind(row) {
  await userApi.unbindWechat(row.id)
  message.success('微信已解绑，重新登录需再次匹配')
  await loadUsers()
}

async function approveBinding(row) {
  await userApi.approveBinding(row.id)
  message.success('绑定已通过')
  await loadBindings()
}

async function rejectBinding(row) {
  await userApi.rejectBinding(row.id)
  message.success('绑定已驳回')
  await loadBindings()
}

async function unbindBinding(row) {
  await userApi.unbindBinding(row.id)
  message.success('绑定关系已解除')
  await loadBindings()
}

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

onMounted(loadUsers)
</script>

<template>
  <div>
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

    <div class="stats">
      <div class="stat-card"><div class="lbl"><TeamOutlined /> 后台账号</div><div class="v">{{ users.filter(u => u.userType === 'ADMIN').length }}<em>个</em></div><div class="foot">管理者 1 · 教务若干</div></div>
      <div class="stat-card jade"><div class="lbl">教师账号</div><div class="v">{{ users.filter(u => u.userType === 'TEACHER').length }}<em>个</em></div><div class="foot">后台创建 · 微信手机号匹配</div></div>
      <div class="stat-card blue"><div class="lbl">家长账号</div><div class="v">{{ users.filter(u => u.userType === 'PARENT').length }}<em>个</em></div><div class="foot">微信登录自动创建</div></div>
      <div class="stat-card red"><div class="lbl">待审核绑定</div><div class="v">{{ pending.length }}<em>条</em></div><div class="foot">家长-学员关系</div></div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <a-tabs :active-key="activeTab" @change="onTabChange">
        <a-tab-pane key="accounts" tab="用户账号">
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

        <a-tab-pane key="roles" tab="角色权限">
          <a-spin :spinning="roleLoading">
            <div class="grid-2" style="align-items:start">
              <div class="paper-card card-pad" style="border:none;padding:0">
                <div class="card-title">内置角色</div>
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

        <a-tab-pane key="bindings" tab="家长绑定">
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

        <a-tab-pane key="logs" tab="登录与变更记录">
          <div class="filters">
            <a-input v-model:value="auditQuery.keyword" placeholder="搜索操作人 / 内容" allow-clear style="width:220px" @press-enter="() => { auditQuery.current = 1; loadAudit() }" />
            <a-select v-model:value="auditQuery.actionType" placeholder="全部类型" allow-clear style="width:140px" @change="() => { auditQuery.current = 1; loadAudit() }">
              <a-select-option value="登录">登录</a-select-option>
              <a-select-option value="账号变更">账号变更</a-select-option>
              <a-select-option value="绑定审核">绑定审核</a-select-option>
            </a-select>
            <a-button size="small" @click="() => { auditQuery.current = 1; loadAudit() }">查询</a-button>
          </div>
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

    <a-modal v-model:open="editOpen" title="编辑账号" ok-text="保存" cancel-text="取消" :confirm-loading="editSaving" @ok="submitEdit">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="姓名"><a-input v-model:value="editForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="用户名"><a-input v-model:value="editForm.username" :disabled="editForm.type !== 'ADMIN'" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="手机号"><a-input v-model:value="editForm.phone" maxlength="11" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="类型"><a-input :value="typeText[editForm.type]" disabled /></a-form-item></a-col>
        </a-row>
        <div class="login-note">教师 / 家长手机号变更后会自动解除微信绑定，需要重新登录匹配。</div>
      </a-form>
    </a-modal>

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
