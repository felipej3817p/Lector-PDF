import { defineStore } from 'pinia'
import http from '../api/http'

const safeParse = (value) => {
  try {
    return JSON.parse(value || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('auth_token') || '',
    user: safeParse(localStorage.getItem('auth_user'))
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token),

    isSuperAdmin: (state) =>
      Array.isArray(state.user?.roles) && state.user.roles.includes('SUPER_ADMIN'),

    isApprover: (state) =>
      Array.isArray(state.user?.roles) && state.user.roles.includes('APROBADOR'),

    isOperator: (state) =>
      Array.isArray(state.user?.roles) && state.user.roles.includes('OPERADOR'),

    canReviewDocuments: (state) =>
      Array.isArray(state.user?.roles) &&
      (
        state.user.roles.includes('SUPER_ADMIN') ||
        state.user.roles.includes('APROBADOR')
      ),

    allowedAreas: (state) =>
      Array.isArray(state.user?.allowedAreas) ? state.user.allowedAreas : []
  },

  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user || null

      localStorage.setItem('auth_token', token)
      localStorage.setItem('auth_user', JSON.stringify(user || null))
    },

    clearSession() {
      this.token = ''
      this.user = null

      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_user')
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
        roles: Array.isArray(data?.roles) ? data.roles : [],
        allowedAreas: Array.isArray(data?.allowedAreas) ? data.allowedAreas : []
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