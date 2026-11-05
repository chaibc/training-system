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
const isDev = import.meta.env.DEV

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

const visibleGroups = computed(() => groups.map((g) => ({
  ...g,
  items: g.items.filter((i) => (!i.managerOnly || auth.isManager) && auth.can(i.module, 'view'))
})).filter((g) => g.items.length))

const dateLabel = dayjs().format('YYYY 年 M 月')
const pwdOpen = ref(false)
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

function openPassword() {
  Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  pwdOpen.value = true
}

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

function go(path) {
  router.push(path)
}

async function logout() {
  try { await authApi.logout() } catch { /* ignore */ }
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="side">
      <div class="side-brand">
        <div class="seal">翰</div>
        <div><b>翰墨学堂</b><span>ADMIN CONSOLE · B 端</span></div>
      </div>
      <div class="tenant">
        <span class="dot"></span>
        <div><b>翰墨学堂 · 营业中</b><span>{{ auth.roleName }} · {{ auth.displayName }}</span></div>
      </div>
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
      <div v-if="isDev" class="side-links">
        <a href="http://localhost:8080/swagger-ui.html" target="_blank">接口文档（开发）</a>
        <a href="http://localhost:8080/actuator/health" target="_blank">服务状态</a>
      </div>
      <div class="side-foot">
        <div class="av">{{ auth.displayName.slice(0, 1) }}</div>
        <div><b>{{ auth.displayName }}</b><span>{{ auth.roleName }}</span></div>
        <button class="logout" title="修改密码" @click="openPassword"><KeyOutlined /></button>
        <button class="logout" title="退出登录" @click="logout"><LogoutOutlined /></button>
      </div>
    </aside>

    <div class="main">
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
      <main class="content">
        <router-view />
      </main>
    </div>
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
