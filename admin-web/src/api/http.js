/**
 * axios 实例：统一后端基址、登录令牌注入、响应解包与错误提示。
 *
 * <p>后端统一响应体为 { code, data, message }，code === 0 视为成功，
 * 拦截器只把 data 抛给业务层，因此页面里的 await xxxApi.yyy() 拿到的就是 data 本身。</p>
 *
 * <p>token 的 localStorage key 与 stores/auth.js 保持一致（hanmo_token / hanmo_user / hanmo_perms），
 * 改其一必须同步改另一处，否则会出现「登录成功但接口 401」。</p>
 */
import axios from 'axios'
import { message } from 'ant-design-vue'

// 所有请求走前端同源 /api 前缀，开发环境由 Vite 代理、生产环境由 Nginx 转发到后端
const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 请求拦截器：有 token 就注入 Authorization: Bearer <token>，后端据此解析登录用户与数据范围
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('hanmo_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：成功时解包 data，失败时统一提示；401 额外清理登录态并跳登录页
http.interceptors.response.use(
  (response) => {
    // 业务失败（code !== 0）也走 HTTP 200，因此必须在成功分支里再判一次 code
    const body = response.data
    if (body && body.code === 0) return body.data
    // 后端 message 为可直接展示的中文提示，缺省时用兜底文案
    const msg = body?.message || '请求失败'
    message.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    // 错误分支：后端业务异常体同样带 message，网络错误则退回 axios 自带信息
    const msg = body?.message || error.message || '网络请求失败'
    const url = error.config?.url || ''
    // 401 统一登出；但登录/微信登录接口自身的 401 表示账号密码错误，不能跳转，否则登录页会自己刷掉
    if (status === 401 && !url.includes('/auth/login') && !url.includes('/auth/wx-login')) {
      localStorage.removeItem('hanmo_token')
      localStorage.removeItem('hanmo_user')
      localStorage.removeItem('hanmo_perms')
      // 三个 key 必须整体清除，只清 token 会留下过期的用户名与权限点
      if (!location.pathname.startsWith('/login')) {
        // 用整页跳转而不是 router.push：此时 Pinia 状态未清空，整页刷新可保证 store 重新初始化
        location.href = '/login?expired=1'
      }
    }
    message.error(msg)
    return Promise.reject(error)
  }
)

// 全站唯一实例，api/index.js 中各业务模块共用，便于统一加拦截逻辑
export default http
