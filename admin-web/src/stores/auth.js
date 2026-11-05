import { defineStore } from 'pinia'
import { authApi } from '../api'

const readJson = (key, fallback) => {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('hanmo_token') || '',
    user: readJson('hanmo_user', null),
    permissions: readJson('hanmo_perms', [])
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isManager: (state) => state.user?.roleCode === 'ADMIN',
    displayName: (state) => state.user?.name || '未登录',
    roleName: (state) => state.user?.roleName || ''
  },
  actions: {
    setSession(data) {
      this.token = data.token || ''
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
      localStorage.setItem('hanmo_token', this.token)
      localStorage.setItem('hanmo_user', JSON.stringify(this.user))
      localStorage.setItem('hanmo_perms', JSON.stringify(this.permissions))
    },
    async login(payload) {
      const data = await authApi.login(payload)
      this.setSession(data)
      return data
    },
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
    can(module, action = 'view') {
      if (!this.user) return false
      if (this.user.roleCode === 'ADMIN') return true
      const p = this.permissions.find((x) => x.moduleKey === module)
      if (!p) return false
      const map = {
        view: 'canView',
        create: 'canCreate',
        edit: 'canEdit',
        delete: 'canDelete',
        special: 'canSpecial'
      }
      return !!p[map[action] || 'canView']
    },
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
