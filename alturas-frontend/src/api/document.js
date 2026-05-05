import http from './http'

export const getDocuments = () => http.get('/api/documents')

export const getDocumentById = (id) => http.get(`/api/documents/${id}`)

export const getDocumentsByEmployeeId = (employeeId) =>
  http.get(`/api/documents/employee/${employeeId}`)

export const uploadDocument = (formData) =>
  http.post('/api/documents/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

export const analyzeDocument = (id) => http.get(`/api/documents/${id}/analyze`)

export const approveDocument = (id, comment = '') =>
  http.post(`/api/documents/${id}/approve`, {
    comment
  })

export const rejectDocument = (id, comment = '') =>
  http.post(`/api/documents/${id}/reject`, {
    comment
  })

export const downloadConsolidatedExcel = () =>
  http.get('/api/documents/exports/consolidated-excel', {
    responseType: 'blob'
  })

export const deleteDocument = (id) => http.delete(`/api/documents/${id}`)