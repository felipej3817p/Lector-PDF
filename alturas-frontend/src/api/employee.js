import http from './http'

export const getEmployees = () => http.get('/api/employees')

export const getEmployeesDashboard = () => http.get('/api/employees/dashboard')

export const getEmployeeById = (id) => http.get(`/api/employees/${id}`)

export const createEmployee = (data) => http.post('/api/employees', data)

export const updateEmployee = (id, data) => http.put(`/api/employees/${id}`, data)

export const deleteEmployee = (id) => http.delete(`/api/employees/${id}`)