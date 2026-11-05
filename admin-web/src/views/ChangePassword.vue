<!--
  强制修改密码页：账号首次登录或被重置密码后（forceChangePassword=true），
  路由守卫会把任何业务页拦到这里，改密前不能进入后台。
  接口：POST /api/auth/change-password（body: oldPassword / newPassword）
-->
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined } from '@ant-design/icons-vue'
import { authApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
// 提交中标记：绑定按钮 loading，防止重复提交
const loading = ref(false)
// 表单级错误文案：直接显示在卡片底部，不弹全局提示
const error = ref('')
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 提交改密：先做前端一致性校验，最终规则（原密码是否正确、新旧是否相同）仍以后端为准
async function submit() {
  error.value = ''
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    error.value = '请填写原密码、新密码与确认密码'
    return
  }
  if (form.newPassword.length < 8 || form.newPassword.length > 32) {
    error.value = '新密码长度需为 8–32 位'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }
  if (form.newPassword === form.oldPassword) {
    error.value = '新密码不能与原密码相同'
    return
  }
  loading.value = true
  try {
    await authApi.changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    // 后端已把 force_change_password 置 0；这里同步本地标记，守卫才会放行其他页面
    auth.markPasswordChanged()
    message.success('密码已修改')
    router.replace('/dashboard')
  } catch (e) {
    // 原密码错误等业务异常由后端返回 message
    error.value = e?.response?.data?.message || e?.message || '修改失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <!-- 左侧品牌区：与登录页保持一致，说明当前处于强制改密流程 -->
    <div class="login-brand">
      <div class="seal">翰</div>
      <div class="kicker">CALLIGRAPHY EDUCATION · ADMIN CONSOLE</div>
      <h1>翰墨学堂<br>管理后台</h1>
      <p>账号安全提示：首次登录或密码被重置后，需要先设置自己的密码才能继续使用后台。</p>
    </div>
    <!-- 右侧改密表单 -->
    <div class="login-panel">
      <div class="login-card">
        <h2>请先修改密码</h2>
        <div class="sub">当前账号使用的是初始密码，修改后方可进入后台。密码长度 8–32 位。</div>
        <a-form layout="vertical" @submit.prevent="submit">
          <a-form-item label="原密码">
            <a-input-password v-model:value="form.oldPassword" size="large" placeholder="请输入当前密码" autocomplete="current-password">
              <template #prefix><LockOutlined style="color:#B9B1A2" /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item label="新密码">
            <a-input-password v-model:value="form.newPassword" size="large" placeholder="8–32 位，建议字母+数字" autocomplete="new-password">
              <template #prefix><LockOutlined style="color:#B9B1A2" /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item label="确认新密码">
            <a-input-password v-model:value="form.confirmPassword" size="large" placeholder="再次输入新密码" autocomplete="new-password" @press-enter="submit">
              <template #prefix><LockOutlined style="color:#B9B1A2" /></template>
            </a-input-password>
          </a-form-item>
          <a-button type="primary" size="large" block :loading="loading" @click="submit">确认修改</a-button>
        </a-form>
        <!-- 表单级错误：原密码错误、新密码不合规等 -->
        <div v-if="error" class="login-error">{{ error }}</div>
        <div class="login-note">修改成功后旧密码立即失效；当前登录状态保持，无需重新登录。</div>
      </div>
    </div>
  </div>
</template>
