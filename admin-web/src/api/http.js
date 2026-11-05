import axios from 'axios'
import { message } from 'ant-design-vue'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('hanmo_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code === 0) return body.data
    const msg = body?.message || '请求失败'
    message.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    const msg = body?.message || error.message || '网络请求失败'
    const url = error.config?.url || ''
    if (status === 401 && !url.includes('/auth/login') && !url.includes('/auth/wx-login')) {
      localStorage.removeItem('hanmo_token')
      localStorage.removeItem('hanmo_user')
      localStorage.removeItem('hanmo_perms')
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login?expired=1'
      }
    }
    message.error(msg)
    return Promise.reject(error)
  }
)

export default http
