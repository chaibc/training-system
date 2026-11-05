/**
 * 后台主框架：左侧分组导航 + 顶栏（页面标题、日期、缴费快捷入口）+ 内容区，另含修改密码与退出登录。
 * 接口：authApi.changePassword、authApi.logout（api/index.js）；登录态与权限来自 stores/auth.js
 *
 * 菜单按权限过滤：managerOnly 项仅管理者可见，其余按 auth.can(module, 'view') 判断，
 * 过滤规则与 router/index.js 的守卫保持一致，改一处要同步另一处。
 */
<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '../stores/auth'
import { authApi } from '../api'
import dayjs from 'dayjs'
import {
  AppstoreOutlined, BarChartOutlined, TeamOutlined, FileTextOutlined,
  BookOutlined, CalendarOutlined, CheckSquareOutlined, EditOutlined,
  PictureOutlined, BankOutlined, DatabaseOutlined, SettingOutlined,
  SafetyCertificateOutlined, LogoutOutlined, KeyOutlined
} from '@ant-design/icons-vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
// 开发环境标记：只有 dev 构建才显示接口文档、服务状态这些内部链接
const isDev = import.meta.env.DEV

// 侧边栏菜单分组：module 是权限点 key（与路由 meta.module 对应），managerOnly 表示仅管理者可见
const groups = [
  {
    name: '经营概览',
    items: [
      { path: '/dashboard', label: '数据看板', module: 'dashboard', icon: AppstoreOutlined },
      { path: '/reports', label: '统计报表', module: 'reports', icon: BarChartOutlined }
    ]
  },
  {
    name: '教务管理',
    items: [
      { path: '/students', label: '学员管理', module: 'students', icon: TeamOutlined },
      { path: '/enrollments', label: '报名与缴费', module: 'enrollments', icon: FileTextOutlined },
      { path: '/courses', label: '课程与定价', module: 'courses', icon: BookOutlined },
      { path: '/classes', label: '班级与排课', module: 'classes', icon: CalendarOutlined },
      { path: '/attendance', label: '点名与消课', module: 'attendance', icon: CheckSquareOutlined },
      { path: '/leave', label: '请假审批', module: 'leave', icon: EditOutlined }
    ]
  },
  {
    name: '内容管理',
    items: [
      { path: '/works', label: '作品管理', module: 'works', icon: PictureOutlined },
      { path: '/org', label: '机构内容', module: 'org', icon: BankOutlined }
    ]
  },
  {
    name: '基础设置',
    items: [
      { path: '/basic', label: '基础数据', module: 'basic', icon: DatabaseOutlined },
      { path: '/settings', label: '业务规则', module: 'settings', icon: SettingOutlined }
    ]
  },
  {
    name: '系统管理',
    items: [
      { path: '/users', label: '用户与权限', module: 'users', icon: SafetyCertificateOutlined, managerOnly: true }
    ]
  }
]

// 菜单过滤：先按权限去掉不可见的菜单项，再丢掉空分组，避免只留下一个空分组标题
const visibleGroups = computed(() => groups.map((g) => ({
  ...g,
  items: g.items.filter((i) => (!i.managerOnly || auth.isManager) && auth.can(i.module, 'view'))
})).filter((g) => g.items.length))

// 顶栏显示的当前年月，组件渲染时计算一次
const dateLabel = dayjs().format('YYYY 年 M 月')
const pwdOpen = ref(false)
const pwdSaving = ref(false)
// 修改密码表单：仅后台账号可用，教师与家长走微信登录、没有后台密码
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 打开改密弹窗：清空上一次输入，避免残留
function openPassword() {
  Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  pwdOpen.value = true
}

// 提交改密：前端先校验非空、8–32 位长度、两次输入一致，与后端规则保持一致
async function submitPassword() {
  if (!pwdForm.oldPassword) return message.warning('请输入原密码')
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 8 || pwdForm.newPassword.length > 32) {
    return message.warning('新密码长度需为 8–32 位')
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) return message.warning('两次输入的新密码不一致')
  pwdSaving.value = true
  try {
    await authApi.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    message.success('密码已修改')
    pwdOpen.value = false
  } finally { pwdSaving.value = false }
}

// 菜单跳转：统一走 router.push，当前项高亮由 route.path 比较决定
function go(path) {
  router.push(path)
}

// 退出登录：先通知后端注销 token；即使接口失败也要继续清理本地状态，否则会卡在已登录界面
async function logout() {
  try { await authApi.logout() } catch { /* ignore */ }
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-shell">
    <!-- 左侧边栏：品牌、租户信息、分组菜单、开发链接与用户信息 -->
    <aside class="side">
      <!-- 品牌区：静态标识与副标题 -->
      <div class="side-brand">
        <div class="seal">翰</div>
        <div><b>翰墨学堂</b><span>ADMIN CONSOLE · B 端</span></div>
      </div>
      <!-- 当前机构与登录人：角色名 + 姓名 -->
      <div class="tenant">
        <span class="dot"></span>
        <div><b>翰墨学堂 · 营业中</b><span>{{ auth.roleName }} · {{ auth.displayName }}</span></div>
      </div>
      <!-- 分组菜单：只渲染 visibleGroups 里过滤后的项 -->
      <div class="nav-scroll">
        <template v-for="group in visibleGroups" :key="group.name">
          <div class="nav-group">{{ group.name }}</div>
          <button
            v-for="item in group.items"
            :key="item.path"
            class="nav-item"
            :class="{ on: route.path === item.path }"
            @click="go(item.path)"
          >
            <component :is="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </template>
      </div>
      <!-- 开发环境快捷链接：Swagger 文档与健康检查，生产构建不渲染 -->
      <div v-if="isDev" class="side-links">
        <a href="http://localhost:8080/swagger-ui.html" target="_blank">接口文档（开发）</a>
        <a href="http://localhost:8080/actuator/health" target="_blank">服务状态</a>
      </div>
      <!-- 底部用户区：修改密码与退出登录 -->
      <div class="side-foot">
        <div class="av">{{ auth.displayName.slice(0, 1) }}</div>
        <div><b>{{ auth.displayName }}</b><span>{{ auth.roleName }}</span></div>
        <button class="logout" title="修改密码" @click="openPassword"><KeyOutlined /></button>
        <button class="logout" title="退出登录" @click="logout"><LogoutOutlined /></button>
      </div>
    </aside>

    <div class="main">
      <!-- 顶栏：当前页面标题/副标题（来自路由 meta）、日期与缴费快捷入口 -->
      <header class="topbar">
        <div class="where">
          <b>{{ route.meta.title || '管理后台' }}</b>
          <span>{{ route.meta.sub || '' }}</span>
        </div>
        <div class="sp"></div>
        <div class="date-chip">
          <CalendarOutlined />
          {{ dateLabel }}
        </div>
        <a-button v-if="auth.can('enrollments', 'view')" type="primary" size="small" @click="go('/enrollments')">
          登记线下缴费
        </a-button>
        <a-avatar :size="30" style="background:#7A5B3D;font-size:11px">{{ auth.displayName.slice(0, 1) }}</a-avatar>
      </header>
      <!-- 内容区：业务页面渲染在这里 -->
      <main class="content">
        <router-view />
      </main>
    </div>
    <!-- 修改登录密码弹窗：只在侧边栏点击钥匙图标时打开 -->
    <a-modal v-model:open="pwdOpen" title="修改登录密码" ok-text="确认修改" cancel-text="取消"
             :confirm-loading="pwdSaving" @ok="submitPassword">
      <a-form layout="vertical">
        <a-form-item label="原密码" required><a-input-password v-model:value="pwdForm.oldPassword" /></a-form-item>
        <a-form-item label="新密码" required><a-input-password v-model:value="pwdForm.newPassword" placeholder="8–32 位" /></a-form-item>
        <a-form-item label="确认新密码" required><a-input-password v-model:value="pwdForm.confirmPassword" /></a-form-item>
      </a-form>
      <div class="login-note">仅后台账号（管理者/教务）可修改密码；教师与家长使用微信登录。</div>
    </a-modal>
  </div>
</template>
