/**
 * 登录态 store（Pinia）。
 *
 * <p>token 存 hanmo_token，用户信息与权限点分别存 hanmo_user / hanmo_perms；
 * 刷新页面时直接从 localStorage 恢复，避免每次都请求 /auth/me。
 * 这三个 key 在 api/http.js 的 401 分支里也会被清除，改 key 名必须两处同步。</p>
 */
import { defineStore } from 'pinia'
import { authApi } from '../api'

// 安全读取 localStorage 中的 JSON：解析失败时返回兜底值，避免历史脏数据让整个应用白屏
const readJson = (key, fallback) => {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

export const useAuthStore = defineStore('auth', {
  // 初始 state 直接取自 localStorage，保证刷新后首屏就能渲染用户名与菜单
  state: () => ({
    token: localStorage.getItem('hanmo_token') || '',
    user: readJson('hanmo_user', null),
    permissions: readJson('hanmo_perms', [])
  }),
  getters: {
    // 是否已登录：只判断本地有无 token，token 是否过期由接口 401 兜底
    isLoggedIn: (state) => !!state.token,
    // 是否管理者：仅 roleCode === ADMIN 视为管理者，用于用户与权限页、列表中的数据范围控制
    isManager: (state) => state.user?.roleCode === 'ADMIN',
    // 顶栏与侧边栏展示的姓名
    displayName: (state) => state.user?.name || '未登录',
    // 角色名称（如 管理者、教务），与 displayName 一起显示在侧边栏底部
    roleName: (state) => state.user?.roleName || ''
  },
  actions: {
    // 登录成功后写入会话：state 与 localStorage 同时更新，后者用于刷新恢复
    setSession(data) {
      this.token = data.token || ''
      // 只挑页面实际用到的字段落库，避免把后端返回的其它字段一并写进 localStorage
      this.user = {
        userId: data.userId,
        username: data.username,
        name: data.name,
        userType: data.userType,
        roleCode: data.roleCode,
        roleName: data.roleName,
        dataScope: data.dataScope,
        forceChangePassword: data.forceChangePassword
      }
      this.permissions = data.permissions || []
      // 三个 key 与 api/http.js 的读取/清理逻辑一一对应
      localStorage.setItem('hanmo_token', this.token)
      localStorage.setItem('hanmo_user', JSON.stringify(this.user))
      localStorage.setItem('hanmo_perms', JSON.stringify(this.permissions))
    },
    // 账号密码登录：成功后写入会话，并返回原始 data 供登录页判断（如强制改密标记）
    async login(payload) {
      const data = await authApi.login(payload)
      this.setSession(data)
      return data
    },
    // 拉取当前用户信息与权限点（路由守卫在缺 user 时调用），不覆盖 token
    async fetchMe() {
      const data = await authApi.me()
      this.user = {
        userId: data.userId,
        username: data.username,
        name: data.name,
        userType: data.userType,
        roleCode: data.roleCode,
        roleName: data.roleName,
        dataScope: data.dataScope,
        forceChangePassword: data.forceChangePassword
      }
      this.permissions = data.permissions || []
      localStorage.setItem('hanmo_user', JSON.stringify(this.user))
      localStorage.setItem('hanmo_perms', JSON.stringify(this.permissions))
      return data
    },
    // 权限判断：ADMIN 直接放行；其余角色按 moduleKey 找权限点，再取对应动作字段
    can(module, action = 'view') {
      if (!this.user) return false
      if (this.user.roleCode === 'ADMIN') return true
      const p = this.permissions.find((x) => x.moduleKey === module)
      // 没有该模块的权限点记录即视为无权限（未授权默认拒绝）
      if (!p) return false
      // 动作名 → 权限点字段的映射，页面统一用 can(module, 'view'|'create'|'edit'|'delete'|'special')
      const map = {
        view: 'canView',
        create: 'canCreate',
        edit: 'canEdit',
        delete: 'canDelete',
        special: 'canSpecial'
      }
      return !!p[map[action] || 'canView']
    },
    // 前端登出：清空内存与 localStorage；服务端注销由调用方先调 authApi.logout()
    logout() {
      this.token = ''
      this.user = null
      this.permissions = []
      localStorage.removeItem('hanmo_token')
      localStorage.removeItem('hanmo_user')
      localStorage.removeItem('hanmo_perms')
    }
  }
})
