import http from './http'

export const requestPasswordReset = (email) => {
  return http.post('/api/auth/forgot-password', {
    email
  })
}

export const resetPassword = (token, newPassword) => {
  return http.post('/api/auth/reset-password', {
    token,
    newPassword
  })
}