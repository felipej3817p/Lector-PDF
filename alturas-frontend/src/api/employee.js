import http from './http'

export const getEmployees = () => http.get('/api/employees')

export const getEmployeesDashboard = () => http.get('/api/employees/dashboard')

export const getEmployeeById = (id) => http.get(`/api/employees/${id}`)

export const createEmployee = (data) => http.post('/api/employees', data)

export const updateEmployee = (id, data) => http.put(`/api/employees/${id}`, data)

export const deleteEmployee = (id) => http.delete(`/api/employees/${id}`)

export const deleteBulkEmployees = (ids) => http.delete('/api/employees/bulk', { data: ids })

export const deleteBulkEvaluations = (ids) => http.delete('/api/employees/bulk/evaluations', { data: ids })

export const deleteBulkHistorical = (ids) => http.delete('/api/employees/bulk/historical', { data: ids })

export const deleteBulkCertificates = (ids) => http.delete('/api/employees/bulk/certificates', { data: ids })