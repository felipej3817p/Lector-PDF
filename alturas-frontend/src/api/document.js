import http from './http'

export const getDocuments = (params = {}) => http.get('/api/documents', { params })

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

export const approveBulkDocuments = (documentIds = [], comment = '') =>
  http.post('/api/documents/approve-bulk', { documentIds, comment })

export const rejectBulkDocuments = (documentIds = [], comment = '') =>
  http.post('/api/documents/reject-bulk', { documentIds, comment })

export const resendDocumentEmail = (id) =>
  http.post(`/api/documents/${id}/resend-email`)

export const getDocumentEmailLogs = (id) =>
  http.get(`/api/email-logs/document/${id}`)

export const getDocumentBatches = () =>
  http.get('/api/document-batches')

export const notifyApproverBatch = (id) =>
  http.post(`/api/document-batches/${id}/notify-approver`)

export const getHistoricalImportIssues = (batchId = '') =>
  http.get('/api/documents/historical/issues', {
    params: batchId ? { batchId } : {}
  })

export const deleteHistoricalImportIssue = (id) =>
  http.delete(`/api/documents/historical/issues/${id}`)

export const deleteAllHistoricalImportIssues = () =>
  http.delete('/api/documents/historical/issues/all')

export const viewHistoricalImportIssuePdf = (id) =>
  http.get(`/api/documents/historical/issues/${id}/view`, {
    responseType: 'blob'
  })
