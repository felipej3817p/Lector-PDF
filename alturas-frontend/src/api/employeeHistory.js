import http from './http'

export const getEmployeeHistory = (employeeId) => {
  return http.get(`/api/employees/${employeeId}/history`)
}