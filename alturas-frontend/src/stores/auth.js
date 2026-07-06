import { defineStore } from 'pinia'
import http from '../api/http'
import { applyTheme, getStoredTheme } from '../utils/themePreferences'

const safeParse = (value) => {
  try {
    return JSON.parse(value || 'null')
  } catch {
    return null
  }
}

const normalizeRole = (role) => {
  const normalized = String(role || '')
    .trim()
    .toUpperCase()
    .replace(/^ROLE_/, '')

  return normalized === 'APPROVER' ? 'APROBADOR' : normalized
}

const getRoles = (user) => {
  const roles = Array.isArray(user?.roles) ? user.roles : []
  return [...new Set(roles.map(normalizeRole).filter(Boolean))]
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('auth_token') || '',
    user: safeParse(localStorage.getItem('auth_user'))
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token),

    roles: (state) => getRoles(state.user),

    isSuperAdmin: (state) => getRoles(state.user).includes('SUPER_ADMIN'),

    isAdmin: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN')
    },

    isApprover: (state) => getRoles(state.user).includes('APROBADOR'),

    isOperator: (state) => getRoles(state.user).includes('OPERADOR'),

    isViewer: (state) => getRoles(state.user).includes('VISUALIZADOR'),

    canReviewDocuments: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN') || roles.includes('APROBADOR')
    },

    canManageSettings: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN') || roles.includes('IT') ||
        roles.includes('APROBADOR') || roles.includes('OPERADOR')
    },

    canAccessSettings: (state) => Boolean(state.token),

    canEditEmailSettings: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN') || roles.includes('IT') ||
        roles.includes('OPERADOR')
    },

    canManageUsers: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN')
    },

    canUploadDocuments: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN') || roles.includes('OPERADOR')
    },

    canWriteEmployees: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN') || roles.includes('OPERADOR')
    },

    canManageNotifications: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN') || roles.includes('APROBADOR')
    },

    canDeleteDocuments: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN') || roles.includes('OPERADOR')
    },

    isReadOnlyViewer: (state) => {
      const roles = getRoles(state.user)
      return roles.includes('VISUALIZADOR') &&
        !roles.includes('SUPER_ADMIN') &&
        !roles.includes('ADMIN') &&
        !roles.includes('APROBADOR') &&
        !roles.includes('OPERADOR')
    },

    allowedAreas: (state) =>
      Array.isArray(state.user?.allowedAreas) ? state.user.allowedAreas : [],

    hasGlobalAreaAccess: (state) => Boolean(state.user?.globalAreaAccess)
  },

  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user || null

      localStorage.setItem('auth_token', token)
      localStorage.setItem('auth_user', JSON.stringify(user || null))
      applyTheme(getStoredTheme(user))
    },

    clearSession() {
      this.token = ''
      this.user = null

      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_user')
      applyTheme(getStoredTheme(null))
    },

    async login(identifier, password) {
      const { data } = await http.post('/api/auth/login', {
        identifier,
        password
      })

      const token =
        data?.token ||
        data?.accessToken ||
        data?.jwt ||
        data?.bearer ||
        data?.data?.token

      if (!token) {
        throw new Error('Login exitoso pero la respuesta no trajo token.')
      }

      const user = {
        username: data?.username || identifier,
        email: data?.email || '',
        roles: getRoles(data),
        allowedAreas: Array.isArray(data?.allowedAreas) ? data.allowedAreas : [],
        globalAreaAccess: Boolean(data?.globalAreaAccess)
      }

      this.setSession(token, user)

      return data
    },

    logout() {
      this.clearSession()
      window.location.href = '/login'
    }
  }
})
