import http from './http'

const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()

  link.remove()
  window.URL.revokeObjectURL(url)
}

export const getTrainingCertificateEligibility = (employeeId) => {
  return http.get(`/api/employees/${employeeId}/certificates/eligibility`)
}

export const getTrainingCertificates = (employeeId) => {
  return http.get(`/api/employees/${employeeId}/certificates`)
}

export const uploadTrainingCertificate = (employeeId, file) => {
  const formData = new FormData()
  formData.append('file', file)

  return http.post(`/api/employees/${employeeId}/certificates`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const downloadTrainingCertificate = async (certificateId, filename = 'constancia.pdf') => {
  const response = await http.get(`/api/certificates/${certificateId}/download`, {
    responseType: 'blob'
  })

  downloadBlob(response.data, filename)
}

export const deleteTrainingCertificate = (certificateId) => {
  return http.delete(`/api/certificates/${certificateId}`)
}