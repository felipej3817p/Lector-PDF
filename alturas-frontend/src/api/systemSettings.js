import http from './http'

export const getSystemSettings = () => {
  return http.get('/api/settings')
}

export const updateSystemSettings = (payload) => {
  return http.put('/api/settings', payload)
}