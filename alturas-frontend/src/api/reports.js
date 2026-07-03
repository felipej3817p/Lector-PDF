import http from './http'

const EMPTY_REPORT_MESSAGE = 'No se encontraron trabajadores con esos filtros.'
const GENERIC_REPORT_ERROR = 'No se pudo generar el reporte. Intenta nuevamente.'

const buildQueryParams = (filters = {}) => {
  const params = new URLSearchParams()

  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      params.append(key, String(value).trim())
    }
  })

  const queryString = params.toString()

  return queryString ? `?${queryString}` : ''
}

const getFilenameFromDisposition = (disposition, fallback) => {
  if (!disposition) return fallback

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)

  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1].replace(/"/g, ''))
  }

  const normalMatch = disposition.match(/filename="?([^";]+)"?/i)

  if (normalMatch?.[1]) {
    return normalMatch[1]
  }

  return fallback
}

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

const readBlobAsText = async (blob) => {
  if (!(blob instanceof Blob)) return ''

  try {
    return await blob.text()
  } catch {
    return ''
  }
}

const readBlobAsJson = async (blob) => {
  const text = await readBlobAsText(blob)

  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

const normalizeErrorMessage = (message = '') => {
  const text = String(message || '').toLowerCase()

  if (
    text.includes('no se encontraron trabajadores') ||
    text.includes('no hay registros') ||
    text.includes('no hay información') ||
    text.includes('filtros seleccionados')
  ) {
    return EMPTY_REPORT_MESSAGE
  }

  if (
    text.includes('noresourcefoundexception') ||
    text.includes('no static resource') ||
    text.includes('exception') ||
    text.includes('/api/')
  ) {
    return GENERIC_REPORT_ERROR
  }

  return message || GENERIC_REPORT_ERROR
}

const getErrorPayload = async (error) => {
  const responseData = error?.response?.data

  if (responseData instanceof Blob) {
    const json = await readBlobAsJson(responseData)
    const text = await readBlobAsText(responseData)

    return {
      message: json?.message || json?.error || text || ''
    }
  }

  return {
    message:
      responseData?.message ||
      responseData?.error ||
      error?.message ||
      ''
  }
}

const requestReport = async ({ endpoint, filters }) => {
  const queryString = buildQueryParams(filters)

  return http.get(`${endpoint}${queryString}`, {
    responseType: 'blob',
    validateStatus: (status) => status >= 200 && status < 300
  })
}

const downloadReport = async ({ endpoints, filters, fallbackFilename }) => {
  let lastError = null

  for (const endpoint of endpoints) {
    try {
      const response = await requestReport({
        endpoint,
        filters
      })

      if (response.status === 204) {
        return {
          downloaded: false,
          empty: true,
          message: EMPTY_REPORT_MESSAGE
        }
      }

      const blob = response.data

      if (!(blob instanceof Blob)) {
        throw new Error(GENERIC_REPORT_ERROR)
      }

      if (blob.size === 0) {
        return {
          downloaded: false,
          empty: true,
          message: EMPTY_REPORT_MESSAGE
        }
      }

      const contentType = String(response.headers?.['content-type'] || '').toLowerCase()

      if (contentType.includes('application/json')) {
        const json = await readBlobAsJson(blob)

        return {
          downloaded: false,
          empty: true,
          message: normalizeErrorMessage(json?.message || json?.error)
        }
      }

      const filename = getFilenameFromDisposition(
        response.headers?.['content-disposition'],
        fallbackFilename
      )

      downloadBlob(blob, filename)

      return {
        downloaded: true,
        empty: false,
        message: ''
      }
    } catch (error) {
      lastError = error

      const status = error?.response?.status

      if (status === 404) {
        continue
      }

      if (status === 204) {
        return {
          downloaded: false,
          empty: true,
          message: EMPTY_REPORT_MESSAGE
        }
      }

      if (status === 400) {
        const payload = await getErrorPayload(error)

        return {
          downloaded: false,
          empty: true,
          message: normalizeErrorMessage(payload.message)
        }
      }

      break
    }
  }

  const payload = await getErrorPayload(lastError)

  throw new Error(normalizeErrorMessage(payload.message))
}

export const downloadAptitudeExcel = async (filters = {}) => {
  return downloadReport({
    endpoints: [
      '/api/reports/aptitude-excel',
      '/api/reports/aptitude/excel'
    ],
    filters,
    fallbackFilename: 'reporte-conceptos-aptitud-alturas.xlsx'
  })
}

export const downloadMinistryCsv = async (filters = {}) => {
  return downloadReport({
    endpoints: [
      '/api/reports/ministry-csv',
      '/api/reports/ministry/csv'
    ],
    filters,
    fallbackFilename: 'archivo-plano-cargue-masivo-ministerio.csv'
  })
}