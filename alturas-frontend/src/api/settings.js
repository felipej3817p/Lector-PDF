import http from './http'

export const getAllSettings = () => http.get('/settings')
export const getEmailSettings = () => http.get('/settings/email')
export const updateEmailSettings = (payload) => http.put('/settings/email', payload)
export const getZoneCoordinators = () => http.get('/settings/zone-coordinators')
export const updateZoneCoordinators = (payload) => http.put('/settings/zone-coordinators', payload)
export const updateSettingByKey = (key, value, category = 'GENERAL', description = '') =>
  http.put(`/settings/${encodeURIComponent(key)}`, { value, category, description })
