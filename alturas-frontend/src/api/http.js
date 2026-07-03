import axios from 'axios'

const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL || '').trim() ||
  ''

const LOGIN_PATH = '/login'
let redirectingToLogin = false

const clearAuthStorage = () => {
  localStorage.removeItem('auth_token')
  localStorage.removeItem('auth_user')
}

const buildLoginRedirectUrl = () => {
  const current = `${window.location.pathname}${window.location.search}`
  const redirect = encodeURIComponent(current || '/')
  return `${LOGIN_PATH}?redirect=${redirect}`
}

const http = axios.create({
  baseURL: API_BASE_URL,

  /*
   * Carga masiva de PDFs puede tardar, especialmente con PDFs escaneados.
   * 600000 ms = 10 minutos.
   */
  timeout: 600000,

  headers: {
    Accept: 'application/json'
  }
})

http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token')

    config.headers = config.headers || {}

    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => Promise.reject(error)
)

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const requestUrl = String(error?.config?.url || '')

    const isAuthRequest =
      requestUrl.includes('/api/auth/login') ||
      requestUrl.includes('/api/auth/register')

    const isAlreadyOnLogin = window.location.pathname.startsWith(LOGIN_PATH)

    if (status === 401 && !isAuthRequest) {
      clearAuthStorage()

      if (!isAlreadyOnLogin && !redirectingToLogin) {
        redirectingToLogin = true
        window.location.href = buildLoginRedirectUrl()
      }
    }

    return Promise.reject(error)
  }
)

export default http


