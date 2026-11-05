/**
 * 登录页：后台账号（管理者 / 教务）账号密码登录。
 * 接口：POST /api/auth/login（api/index.js 的 authApi.login，经 stores/auth.js 包装）
 *
 * 教师与家长不走这里，使用微信小程序登录；登录成功后 token 与权限点由 auth store 写入 localStorage。
 */
<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
// 提交中标记：绑定到登录按钮 loading，防止重复提交
const loading = ref(false)
// 表单级错误文案：登录失败原因直接显示在卡片底部，不弹全局提示
const error = ref('')
const form = reactive({ username: '', password: '' })

// http.js 的 401 拦截器跳转时会带 ?expired=1，这里就地提示登录已过期
if (route.query.expired) error.value = '登录已过期，请重新登录'

// 提交登录：前端只做非空校验，账号锁定、密码错误等判断全部交给后端
async function submit() {
  // 空值拦截：不请求接口，直接给本地提示
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.login({ username: form.username.trim(), password: form.password })
    message.success('登录成功')
    // 用 replace 而非 push：避免登录页留在浏览器历史里被「后退」再次打开
    router.replace(route.query.redirect || '/dashboard')
  } catch (e) {
    // 失败原因优先取后端 message（如「账号或密码错误」「账号已锁定」），再退回 axios 自带信息
    error.value = e?.response?.data?.message || e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <!-- 左侧品牌区：静态宣传文案，不参与交互 -->
    <div class="login-brand">
      <div class="seal">翰</div>
      <div class="kicker">CALLIGRAPHY EDUCATION · ADMIN CONSOLE</div>
      <h1>翰墨学堂<br>管理后台</h1>
      <p>以传统书法教育，陪孩子养成专注与耐心。<br>学员、课程、排课、课时与作品，一套系统完成。</p>
    </div>
    <!-- 右侧登录表单区 -->
    <div class="login-panel">
      <div class="login-card">
        <h2>欢迎回来</h2>
        <div class="sub">请使用管理者或教务账号登录。教师与家长请使用微信小程序。</div>
        <!-- 账号密码表单：按钮点击与密码框回车都会触发 submit() -->
        <a-form layout="vertical" @submit.prevent="submit">
          <a-form-item label="用户名">
            <a-input v-model:value="form.username" size="large" placeholder="请输入用户名" autocomplete="username">
              <template #prefix><UserOutlined style="color:#B9B1A2" /></template>
            </a-input>
          </a-form-item>
          <a-form-item label="密码">
            <a-input-password v-model:value="form.password" size="large" placeholder="请输入密码" autocomplete="current-password" @press-enter="submit">
              <template #prefix><LockOutlined style="color:#B9B1A2" /></template>
            </a-input-password>
          </a-form-item>
          <a-button type="primary" size="large" block :loading="loading" @click="submit">登录</a-button>
        </a-form>
        <!-- 表单级错误提示（登录失败原因） -->
        <div v-if="error" class="login-error">{{ error }}</div>
        <!-- 登录规则说明：输错锁定策略与权限边界 -->
        <div class="login-note">
          连续输错密码 5 次将锁定 5 分钟；不启用二次验证。<br>
          管理者账号唯一，仅管理者可修改用户与权限。
        </div>
        <div style="font-size:9.5px;color:#948C7C;line-height:1.8;margin-top:14px">
          本地演示账号：<b>admin / Admin@123456</b><br>
          教务账号：<b>jiaowu / Jiaowu@123456</b>
        </div>
      </div>
    </div>
  </div>
</template>
