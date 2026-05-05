import http from './http'

export const createAccessRequest = (data) =>
  http.post('/api/access-requests', data)

export const getMyAccessRequests = () =>
  http.get('/api/access-requests/my')

export const getPendingAccessRequests = () =>
  http.get('/api/access-requests/pending')

export const getPendingAccessRequestsCount = () =>
  http.get('/api/access-requests/pending/count')

export const approveAccessRequest = (id, data = {}) =>
  http.post(`/api/access-requests/${id}/approve`, data)

export const rejectAccessRequest = (id, data = {}) =>
  http.post(`/api/access-requests/${id}/reject`, data)