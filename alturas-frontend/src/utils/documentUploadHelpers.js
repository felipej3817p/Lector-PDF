export const MAX_FILES_PER_UPLOAD_BATCH = 5
export const MAX_FILES_PER_HISTORICAL_BATCH = 1
export const MAX_UPLOAD_BATCH_BYTES = 40 * 1024 * 1024
export const MAX_HISTORICAL_BATCH_BYTES = 150 * 1024 * 1024
export const MAX_SINGLE_PDF_BYTES = 150 * 1024 * 1024

const UPLOAD_SESSION_KEY = 'alturas_document_upload_session'

export const fileKey = (file) => `${file.name}_${file.size}_${file.lastModified}`

export const formatFileSize = (size) => {
  const value = Number(size || 0)

  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`

  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

export const formatDate = (value) => {
  if (!value) return '-'

  const text = String(value || '').trim()
  const localDateMatch = text.match(/^(\d{4})-(\d{2})-(\d{2})/)

  if (localDateMatch) {
    return `${localDateMatch[3]}/${localDateMatch[2]}/${localDateMatch[1]}`
  }

  const date = new Date(text)

  if (Number.isNaN(date.getTime())) return text || '-'

  return date.toLocaleDateString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

export const splitFilesIntoUploadBatches = (files, historicalMode = false) => {
  const batches = []
  let currentBatch = []
  let currentSize = 0
  const maxFiles = historicalMode ? MAX_FILES_PER_HISTORICAL_BATCH : MAX_FILES_PER_UPLOAD_BATCH
  const maxBytes = historicalMode ? MAX_HISTORICAL_BATCH_BYTES : MAX_UPLOAD_BATCH_BYTES

  for (const file of files) {
    const fileSize = Number(file?.size || 0)
    const exceedsFileCount = currentBatch.length >= maxFiles
    const exceedsBatchSize = currentBatch.length > 0 && currentSize + fileSize > maxBytes

    if (exceedsFileCount || exceedsBatchSize) {
      batches.push(currentBatch)
      currentBatch = []
      currentSize = 0
    }

    currentBatch.push(file)
    currentSize += fileSize
  }

  if (currentBatch.length) {
    batches.push(currentBatch)
  }

  return batches
}

export const batchUploadErrorMessage = (errorValue, files = []) => {
  const serverMessage = errorValue?.response?.data?.message
  const fallbackMessage = errorValue?.message
  const rawMessage = String(serverMessage || fallbackMessage || '').trim()
  const normalized = rawMessage.toLowerCase()
  const fileName = files.length === 1 ? ` "${files[0]?.name || 'sin nombre'}"` : ''

  if (
    normalized.includes('multipartexception') ||
    normalized.includes('multipart') ||
    normalized.includes('request was rejected') ||
    normalized.includes('connection') ||
    normalized.includes('network')
  ) {
    return `El servidor no recibio completo el PDF${fileName}. Puede pasar si el archivo es muy pesado, la conexion se corto o el navegador interrumpio la subida. Este archivo se omitio y la carga continua con los demas.`
  }

  if (
    normalized.includes('max') ||
    normalized.includes('tamano') ||
    normalized.includes('tamano') ||
    normalized.includes('size')
  ) {
    return `El PDF${fileName} supera el tamano permitido para la carga. Este archivo se omitio y la carga continua con los demas.`
  }

  return rawMessage || `No se pudo procesar el PDF${fileName}. Este archivo se omitio y la carga continua con los demas.`
}

export const saveUploadSession = (session) => {
  try {
    localStorage.setItem(UPLOAD_SESSION_KEY, JSON.stringify({
      ...session,
      updatedAt: new Date().toISOString()
    }))
  } catch {
    // No bloquea la carga si el navegador no permite persistir el estado.
  }
}

export const loadUploadSession = () => {
  try {
    return JSON.parse(localStorage.getItem(UPLOAD_SESSION_KEY) || 'null')
  } catch {
    return null
  }
}

export const clearUploadSession = () => {
  try {
    localStorage.removeItem(UPLOAD_SESSION_KEY)
  } catch {
    // No requiere accion.
  }
}
