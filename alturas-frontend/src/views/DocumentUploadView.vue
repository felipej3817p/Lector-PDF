<template>
  <section class="page document-upload-page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Carga de evaluaciones</span>
        <h1 class="h1 mb-2">Cargar evaluaciones médicas</h1>
        <p class="p mb-0">
          Selecciona uno o varios PDFs. El sistema identifica al trabajador y deja cada caso pendiente de revisión.
        </p>
      </div>

      <div class="header-actions">
        <RouterLink to="/employees" class="secondary-btn">
          Volver a trabajadores
        </RouterLink>

        <RouterLink to="/documents/historical/issues" class="secondary-btn">
          PDFs no asociados
        </RouterLink>

        <RouterLink
          v-if="canShowReview"
          to="/review"
          class="secondary-btn"
        >
          Revisión
        </RouterLink>
      </div>
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div v-if="successMessage" class="state-box info">
      {{ successMessage }}
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Seleccionar PDFs</h2>
            <p class="helper-text mb-0">
              Carga archivos PDF de conceptos médicos. La asociación con el trabajador se realiza automáticamente por cédula.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div
          class="dropzone"
          :class="{ 'dropzone--active': dragActive }"
          @dragover.prevent="dragActive = true"
          @dragleave.prevent="dragActive = false"
          @drop.prevent="handleDrop"
        >
          <div class="dropzone-icon">PDF</div>

          <p class="mb-2">
            <strong>Suelta aquí los PDFs o una carpeta</strong>
          </p>

          <p class="helper-text mb-3">
            Solo se procesan archivos con extensión <strong>.pdf</strong>.
          </p>

          <div class="d-flex gap-2 justify-content-center flex-wrap">
            <label class="secondary-btn mb-0">
              Seleccionar PDFs
              <input
                class="d-none"
                type="file"
                multiple
                accept="application/pdf,.pdf"
                :disabled="batchLoading"
                @change="handleBatchFilesSelect"
              />
            </label>

            <label class="secondary-btn mb-0">
              Seleccionar carpeta
              <input
                class="d-none"
                type="file"
                multiple
                webkitdirectory
                directory
                :disabled="batchLoading"
                @change="handleBatchFilesSelect"
              />
            </label>
          </div>
        </div>

        <div class="historical-upload-option">
          <label class="checkbox-field historical-upload-option__label">
            <input
              v-model="historicalMode"
              type="checkbox"
              :disabled="batchLoading"
            />
            <span>
              <strong>Guardar carpeta historica</strong>
              <small>Usala para PDFs antiguos. Solo guarda el archivo asociado al trabajador; no califica, no envia correos y reporta los PDFs sin trabajador.</small>
            </span>
          </label>
        </div>

        <div v-if="batchFiles.length" class="selected-files-card">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Archivos seleccionados</h2>
              <p class="helper-text mb-0">
                {{ selectedFilesHelpText }}
              </p>
            </div>

            <span class="status-pill-neutral">
              {{ batchFiles.length }} PDF{{ batchFiles.length === 1 ? '' : 's' }} · {{ selectedFilesSize }}
            </span>
          </div>

          <div class="hr"></div>

          <div class="table-responsive">
            <table class="table table-sm align-middle selected-files-table">
              <thead>
                <tr>
                  <th>Archivo</th>
                  <th>Tamaño</th>
                  <th>Estado</th>
                  <th class="text-center">Quitar</th>
                </tr>
              </thead>

              <tbody>
                <tr v-for="entry in paginatedSelectedFiles" :key="fileKey(entry.file)">
                  <td>
                    <div class="file-cell">
                      <strong>{{ entry.file.name }}</strong>
                      <small>{{ entry.file.webkitRelativePath || 'Archivo seleccionado manualmente' }}</small>
                    </div>
                  </td>

                  <td>{{ formatFileSize(entry.file.size) }}</td>

                  <td>
                    <span :class="selectedFileStatusClass(entry.file)">
                      {{ selectedFileStatusLabel(entry.file) }}
                    </span>
                  </td>

                  <td>
                    <div class="actions justify-content-center">
                      <button
                        type="button"
                        class="secondary-btn danger-btn"
                        :disabled="batchLoading"
                        @click="removeFile(entry.index)"
                      >
                        Quitar
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-if="batchFiles.length" class="pagination-bar upload-results-pagination">
              <span class="helper-text mb-0">
                Mostrando {{ selectedFilesPageStart }}-{{ selectedFilesPageEnd }} de {{ batchFiles.length }} PDFs seleccionados
              </span>

              <div class="pagination-actions">
                <button
                  type="button"
                  class="secondary-btn small-btn"
                  :disabled="selectedFilesCurrentPage === 1"
                  @click="goToSelectedFilesPage(selectedFilesCurrentPage - 1)"
                >
                  Anterior
                </button>

                <input
                  v-model.number="selectedFilesCurrentPage"
                  class="page-jump-input"
                  type="number"
                  min="1"
                  :max="selectedFilesTotalPages"
                  @change="goToSelectedFilesPage(selectedFilesCurrentPage)"
                />

                <span class="status-pill-neutral">
                  de {{ selectedFilesTotalPages }}
                </span>

                <button
                  type="button"
                  class="secondary-btn small-btn"
                  :disabled="selectedFilesCurrentPage === selectedFilesTotalPages"
                  @click="goToSelectedFilesPage(selectedFilesCurrentPage + 1)"
                >
                  Siguiente
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="actions-row mt-4">
          <button
            type="button"
            class="secondary-btn"
            :disabled="batchLoading || !batchFiles.length"
            @click="clearBatch"
          >
            Limpiar cola
          </button>

          <button
            type="button"
            class="primary-btn"
            :disabled="batchLoading || !batchFiles.length"
            @click="submitBatch"
          >
            <span
              v-if="batchLoading"
              class="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            ></span>
            {{ batchLoading ? 'Procesando...' : (historicalMode ? 'Guardar historicos' : 'Procesar evaluaciones') }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="batchSummary"
      ref="resultsSectionRef"
      class="card border-0 mt-4 result-card"
    >
      <div class="card-body">
        <div class="page-header border-0 pb-0 result-header">
          <div>
            <span class="mini-title">Resultado de carga</span>
            <h2 class="h4 mb-1">Carga finalizada</h2>
            <p class="helper-text mb-0">
              Cada PDF se procesó por separado. Los casos correctos quedaron pendientes de revisión.
            </p>
          </div>

          <RouterLink
            v-if="canShowReview"
            to="/review"
            class="primary-btn"
          >
            Ir a Revisión
          </RouterLink>
        </div>

        <div class="hr"></div>

        <div class="upload-summary-strip">
          <div class="upload-summary-item">
            <span>Total</span>
            <strong>{{ batchSummary.total }}</strong>
          </div>

          <div class="upload-summary-item success">
            <span>Correctos</span>
            <strong>{{ batchSummary.success }}</strong>
          </div>

          <div class="upload-summary-item danger">
            <span>Fallidos</span>
            <strong>{{ batchSummary.failed }}</strong>
          </div>

          <div class="upload-summary-item warning">
            <span>Pendientes</span>
            <strong>{{ batchSummary.pendingReview || 0 }}</strong>
          </div>

          <div class="upload-summary-item success">
            <span>Aptos</span>
            <strong>{{ batchSummary.apt || 0 }}</strong>
          </div>

          <div class="upload-summary-item danger">
            <span>No aptos</span>
            <strong>{{ batchSummary.notApt || 0 }}</strong>
          </div>
        </div>

        <div
          v-if="!batchSummary.historical && batchSummary.approverNotificationStatus"
          class="approver-mail-alert"
          :class="approverEmailAlertClass(batchSummary.approverNotificationStatus)"
        >
          <div class="approver-mail-alert__header">
            <div>
              <span class="label">Correo al aprobador</span>
              <strong>{{ approverEmailLabel(batchSummary.approverNotificationStatus) }}</strong>
            </div>

            <button
              v-if="batchSummary.approverNotificationError"
              type="button"
              class="secondary-btn small-btn"
              @click="showFullApproverError = !showFullApproverError"
            >
              {{ showFullApproverError ? 'Ocultar detalle' : 'Ver detalle' }}
            </button>
          </div>

          <p v-if="!batchSummary.approverNotificationError">
            Notificación inicial registrada.
          </p>

          <p v-else-if="!showFullApproverError">
            {{ shortText(mailErrorLabel(batchSummary.approverNotificationError), 180) }}
          </p>

          <pre v-else>{{ mailErrorLabel(batchSummary.approverNotificationError) }}</pre>
        </div>

        <div v-if="!batchSummary.historical" class="process-note">
          <strong>Flujo aplicado:</strong>
          <span>
            La carga notifica al aprobador. El trabajador solo recibe correo cuando el documento sea aprobado.
          </span>
        </div>
      </div>
    </div>

    <div v-if="batchResults.length" class="card border-0 mt-4">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Detalle del procesamiento</h2>
            <p class="helper-text mb-0">
              Revisa archivo por archivo: trabajador, resultado, estado de revisión y observaciones.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div class="table-responsive">
            <table class="table table-sm align-middle processing-table">
            <thead>
              <tr>
                <th>Archivo</th>
                <th>Estado</th>
                <th>Trabajador</th>
                <th>Cédula</th>
                  <th v-if="!batchSummary?.historical">Fecha evaluacion</th>
                  <th v-if="!batchSummary?.historical">Fecha nacimiento</th>
                  <th v-if="!batchSummary?.historical">Resultado</th>
                  <th v-if="!batchSummary?.historical">Revisión</th>
                <th>Observación</th>
                <th class="text-center">Detalle</th>
              </tr>
            </thead>

            <tbody>
                  <tr
                    v-for="(item, index) in paginatedBatchResults"
                :key="`${item.fileName}-${index}`"
                :class="{ 'result-row-error': item.status !== 'OK' }"
              >
                <td>
                  <div class="file-cell">
                    <strong>{{ item.fileName || '-' }}</strong>
                    <small>{{ item.documentId ? `ID: ${item.documentId}` : 'Sin documento generado' }}</small>
                  </div>
                </td>

                <td>
                  <span :class="processStatusClass(item.status)">
                    {{ processStatusLabel(item.status) }}
                  </span>
                </td>

                <td>
                  <strong>{{ item.employeeName || '-' }}</strong>
                </td>

                <td>{{ item.employeeDocument || '-' }}</td>

                  <td v-if="!batchSummary?.historical">
                    {{ formatDate(item.fechaEvaluacion || item.evaluationDate) }}
                  </td>

                  <td v-if="!batchSummary?.historical">
                    {{ formatDate(item.birthDate || item.fechaNacimiento) }}
                  </td>

                  <td v-if="!batchSummary?.historical">
                    <span :class="resultStatusClass(item.resultStatus)">
                      {{ resultStatusLabel(item.resultStatus) }}
                    </span>
                  </td>

                  <td v-if="!batchSummary?.historical">
                    <span :class="reviewStatusClass(item.reviewStatus)">
                      {{ reviewStatusLabel(item.reviewStatus) }}
                    </span>
                  </td>

                <td class="message-cell">
                  {{ userProcessingMessage(item) }}
                </td>

                <td>
                  <div class="actions justify-content-center">
                    <RouterLink
                      v-if="item.documentId"
                      :to="`/documents/${item.documentId}`"
                      class="secondary-btn"
                    >
                      Ver detalle
                    </RouterLink>

                    <button
                      v-else
                      type="button"
                      class="secondary-btn small-btn danger-btn"
                      @click="openProcessingIssueModal(item)"
                    >
                      Ver motivo
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
            </table>

            <div v-if="batchResults.length" class="pagination-bar upload-results-pagination">
              <span class="helper-text mb-0">
                Mostrando {{ batchResultPageStart }}-{{ batchResultPageEnd }} de {{ batchResults.length }}
              </span>

              <div class="pagination-actions">
                <button
                  type="button"
                  class="secondary-btn small-btn"
                  :disabled="batchResultCurrentPage === 1"
                  @click="goToBatchResultPage(batchResultCurrentPage - 1)"
                >
                  Anterior
                </button>

                <span class="status-pill-neutral">
                  Pagina
                </span>

                <input
                  v-model.number="batchResultCurrentPage"
                  class="page-jump-input"
                  type="number"
                  min="1"
                  :max="batchResultTotalPages"
                  @change="goToBatchResultPage(batchResultCurrentPage)"
                />

                <span class="status-pill-neutral">
                  de {{ batchResultTotalPages }}
                </span>

                <button
                  type="button"
                  class="secondary-btn small-btn"
                  :disabled="batchResultCurrentPage === batchResultTotalPages"
                  @click="goToBatchResultPage(batchResultCurrentPage + 1)"
                >
                  Siguiente
                </button>
              </div>
            </div>
          </div>

        <div class="actions-row mt-3">
          <RouterLink
            v-if="canShowReview"
            to="/review"
            class="primary-btn"
          >
            Ir a Revisión
          </RouterLink>

          <button
            type="button"
            class="secondary-btn"
            @click="clearBatch"
          >
            Nueva carga
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="processingIssueModal"
      class="processing-modal-backdrop"
      @click.self="closeProcessingIssueModal"
    >
      <article class="processing-modal">
        <header class="processing-modal__header">
          <div>
            <span class="mini-title">Archivo no procesado</span>
            <h2>{{ processingIssueModal.title }}</h2>
            <p class="helper-text mb-0">
              {{ processingIssueModal.description }}
            </p>
          </div>

          <button
            type="button"
            class="secondary-btn"
            @click="closeProcessingIssueModal"
          >
            Cerrar
          </button>
        </header>

        <div class="hr"></div>

        <div class="processing-modal__body">
          <div class="processing-modal-field">
            <span>Archivo</span>
            <strong>{{ processingIssueModal.fileName }}</strong>
          </div>

          <div class="processing-modal-field">
            <span>Motivo</span>
            <strong>{{ processingIssueModal.message }}</strong>
          </div>

          <div class="processing-modal-field">
            <span>Qué hacer</span>
            <strong>{{ processingIssueModal.recommendation }}</strong>
          </div>
        </div>

        <footer class="processing-modal__footer">
          <button
            type="button"
            class="secondary-btn"
            @click="closeProcessingIssueModal"
          >
            Entendido
          </button>
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import {
  MAX_SINGLE_PDF_BYTES,
  clearUploadSession,
  fileKey,
  formatDate,
  formatFileSize,
  loadUploadSession,
  saveUploadSession,
  splitFilesIntoUploadBatches
} from '../utils/documentUploadHelpers'

const auth = useAuthStore()

const error = ref('')
const successMessage = ref('')
const batchFiles = ref([])
const selectedFilesPageSize = ref(20)
const selectedFilesCurrentPage = ref(1)
const batchLoading = ref(false)
const batchResults = ref([])
const batchResultPageSize = ref(20)
const batchResultCurrentPage = ref(1)
const batchSummary = ref(null)
const dragActive = ref(false)
const historicalMode = ref(false)
const showFullApproverError = ref(false)
const processingIssueModal = ref(null)
const resultsSectionRef = ref(null)
let uploadSessionPoller = null

const canShowReview = computed(() => {
  return auth.isSuperAdmin || auth.isAdmin || auth.isApprover
})

const batchResultTotalPages = computed(() => {
  return Math.max(1, Math.ceil(batchResults.value.length / batchResultPageSize.value))
})

const paginatedBatchResults = computed(() => {
  const start = (batchResultCurrentPage.value - 1) * batchResultPageSize.value
  return batchResults.value.slice(start, start + batchResultPageSize.value)
})

const batchResultPageStart = computed(() => {
  if (!batchResults.value.length) return 0
  return (batchResultCurrentPage.value - 1) * batchResultPageSize.value + 1
})

const batchResultPageEnd = computed(() => {
  return Math.min(batchResultCurrentPage.value * batchResultPageSize.value, batchResults.value.length)
})

const goToBatchResultPage = (page) => {
  batchResultCurrentPage.value = Math.min(
    Math.max(Number(page) || 1, 1),
    batchResultTotalPages.value
  )
}

const selectedFilesSize = computed(() => {
  const total = batchFiles.value.reduce((sum, file) => sum + Number(file.size || 0), 0)
  return formatFileSize(total)
})

const selectedFilesTotalPages = computed(() => {
  return Math.max(1, Math.ceil(batchFiles.value.length / selectedFilesPageSize.value))
})

const paginatedSelectedFiles = computed(() => {
  const start = (selectedFilesCurrentPage.value - 1) * selectedFilesPageSize.value

  return batchFiles.value
    .map((file, index) => ({ file, index }))
    .slice(start, start + selectedFilesPageSize.value)
})

const selectedFilesPageStart = computed(() => {
  if (!batchFiles.value.length) return 0
  return (selectedFilesCurrentPage.value - 1) * selectedFilesPageSize.value + 1
})

const selectedFilesPageEnd = computed(() => {
  return Math.min(selectedFilesCurrentPage.value * selectedFilesPageSize.value, batchFiles.value.length)
})

const goToSelectedFilesPage = (page) => {
  selectedFilesCurrentPage.value = Math.min(
    Math.max(Number(page) || 1, 1),
    selectedFilesTotalPages.value
  )
}

const selectedFilesHelpText = computed(() => {
  if (batchLoading.value) {
    return 'Procesando archivos seleccionados.'
  }

  if (batchResults.value.length) {
    return 'Resultado de cada archivo después del procesamiento.'
  }

  return 'Revisa la cola antes de iniciar el procesamiento.'
})

const persistUploadState = () => {
  saveUploadSession({
    batchSummary: batchSummary.value,
    batchResults: batchResults.value,
    successMessage: successMessage.value,
    error: error.value,
    historicalMode: historicalMode.value,
    batchLoading: batchLoading.value
  })
}

const restoreUploadState = () => {
  const session = loadUploadSession()

  if (!session) return

  const updatedAtTime = new Date(session.updatedAt || '').getTime()
  const isRecent = updatedAtTime && Date.now() - updatedAtTime < 2 * 60 * 1000

  batchSummary.value = session.batchSummary || null
  batchResults.value = Array.isArray(session.batchResults) ? session.batchResults : []
  successMessage.value = session.successMessage || ''
  error.value = session.error || ''
  historicalMode.value = Boolean(session.historicalMode)
  batchLoading.value = Boolean(session.batchLoading && isRecent)
}

const startUploadSessionPolling = () => {
  uploadSessionPoller = window.setInterval(() => {
    const session = loadUploadSession()

    if (!session?.batchLoading) {
      restoreUploadState()
      stopUploadSessionPolling()
      return
    }

    restoreUploadState()
  }, 1200)
}

const stopUploadSessionPolling = () => {
  if (uploadSessionPoller) {
    window.clearInterval(uploadSessionPoller)
    uploadSessionPoller = null
  }
}

const normalizeText = (value) => {
  return String(value || '').trim().toLowerCase()
}

const findResultForFile = (file) => {
  const fileName = normalizeText(file?.name)
  const relativePath = normalizeText(file?.webkitRelativePath)

  if (!fileName && !relativePath) return null

  return batchResults.value.find((item) => {
    const itemFileName = normalizeText(item?.fileName)

    return (
      itemFileName === fileName ||
      itemFileName === relativePath ||
      itemFileName.endsWith(`/${fileName}`) ||
      itemFileName.endsWith(`\\${fileName}`)
    )
  }) || null
}

const selectedFileStatusLabel = (file) => {
  if (batchLoading.value) return 'Procesando'

  const result = findResultForFile(file)

  if (result) {
    return processStatusLabel(result.status)
  }

  return batchResults.value.length ? 'Sin resultado' : 'Pendiente de procesar'
}

const selectedFileStatusClass = (file) => {
  if (batchLoading.value) return 'status-pill-warning'

  const result = findResultForFile(file)

  if (result) {
    return processStatusClass(result.status)
  }

  return batchResults.value.length ? 'status-pill-neutral' : 'status-pill-neutral'
}

const scrollToResults = async () => {
  await nextTick()

  resultsSectionRef.value?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  })
}

const normalizeBatchFiles = (files) => {
  const input = Array.from(files || [])
  const onlyPdf = input.filter((file) => file?.name?.toLowerCase().endsWith('.pdf'))
  const validPdf = onlyPdf.filter((file) => Number(file?.size || 0) <= MAX_SINGLE_PDF_BYTES)
  const oversizedCount = onlyPdf.length - validPdf.length

  const existing = new Set(batchFiles.value.map(fileKey))
  const merged = [...batchFiles.value]

  for (const file of validPdf) {
    const key = fileKey(file)

    if (!existing.has(key)) {
      merged.push(file)
      existing.add(key)
    }
  }

  batchFiles.value = merged
  selectedFilesCurrentPage.value = 1

  if (oversizedCount > 0) {
    error.value = `${oversizedCount} PDF${oversizedCount === 1 ? '' : 's'} fueron ignorados porque superan 150 MB.`
  } else if (!onlyPdf.length && input.length) {
    error.value = 'No se encontraron archivos PDF válidos en la selección.'
  } else if (onlyPdf.length < input.length) {
    error.value = 'Algunos archivos fueron ignorados porque no son PDF.'
  } else {
    error.value = ''
  }

  successMessage.value = ''
  batchSummary.value = null
  batchResults.value = []
  showFullApproverError.value = false
  processingIssueModal.value = null
  clearUploadSession()
}

const uploadFileBatch = async (files) => {
  const formData = new FormData()
  formData.append('documentType', 'CONCEPTO_MEDICO')
  formData.append('examType', 'TRABAJO_EN_ALTURAS')
  formData.append('historical', historicalMode.value ? 'true' : 'false')

  files.forEach((file) => {
    formData.append('files', file)
  })

  const { data } = await http.post('/api/documents/upload/batch-auto', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

  return data || {}
}

const batchUploadErrorMessage = (errorValue, files = []) => {
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

  if (normalized.includes('max') || normalized.includes('tamano') || normalized.includes('tamaño') || normalized.includes('size')) {
    return `El PDF${fileName} supera el tamano permitido para la carga. Este archivo se omitio y la carga continua con los demas.`
  }

  return rawMessage || `No se pudo procesar el PDF${fileName}. Este archivo se omitio y la carga continua con los demas.`
}

const emptyBatchSummary = () => ({
  batchId: '',
  batchCode: '',
  total: 0,
  success: 0,
  failed: 0,
  pendingReview: 0,
  apt: 0,
  notApt: 0,
  historical: historicalMode.value,
  approverNotificationStatus: '',
  approverNotificationError: '',
  batches: 0
})

const addBatchSummary = (summary, data, fileCount) => {
  summary.batchId = [summary.batchId, data?.batchId].filter(Boolean).join(', ')
  summary.batchCode = [summary.batchCode, data?.batchCode].filter(Boolean).join(', ')
  summary.total += Number(data?.total ?? data?.totalFiles ?? fileCount ?? 0)
  summary.success += Number(data?.success ?? data?.successCount ?? 0)
  summary.failed += Number(data?.failed ?? data?.failedCount ?? 0)
  summary.pendingReview += Number(data?.pendingReview ?? data?.pendingReviewCount ?? 0)
  summary.apt += Number(data?.apt ?? data?.aptCount ?? 0)
  summary.notApt += Number(data?.notApt ?? data?.notAptCount ?? 0)
  summary.historical = summary.historical || Boolean(data?.historical)
  summary.approverNotificationStatus =
    data?.approverNotificationStatus ||
    data?.approverEmailStatus ||
    data?.notificationStatus ||
    summary.approverNotificationStatus
  summary.approverNotificationError = [
    summary.approverNotificationError,
    data?.approverNotificationError || data?.approverEmailError || ''
  ].filter(Boolean).join(' | ')
  summary.batches += 1
}

const handleBatchFilesSelect = (event) => {
  normalizeBatchFiles(event.target.files)
  event.target.value = ''
}

const handleDrop = (event) => {
  dragActive.value = false
  normalizeBatchFiles(event.dataTransfer?.files)
}

const removeFile = (index) => {
  batchFiles.value.splice(index, 1)
}

const clearBatch = () => {
  batchFiles.value = []
  selectedFilesCurrentPage.value = 1
  batchResults.value = []
  batchResultCurrentPage.value = 1
  batchSummary.value = null
  error.value = ''
  successMessage.value = ''
  showFullApproverError.value = false
  processingIssueModal.value = null
  clearUploadSession()
}

const submitBatch = async () => {
  if (batchLoading.value || !batchFiles.value.length) return

  try {
    batchLoading.value = true
    error.value = ''
    successMessage.value = ''
    batchResults.value = []
    batchResultCurrentPage.value = 1
    batchSummary.value = null
    showFullApproverError.value = false
    processingIssueModal.value = null
    persistUploadState()

    const uploadBatches = splitFilesIntoUploadBatches(batchFiles.value, historicalMode.value)
    const summary = emptyBatchSummary()
    const allResults = []

    for (let index = 0; index < uploadBatches.length; index += 1) {
      const files = uploadBatches[index]
      const processedBeforeBatch = allResults.length
      const batchStart = processedBeforeBatch + 1
      const batchEnd = processedBeforeBatch + files.length
      successMessage.value = `Procesando PDFs ${batchStart}-${batchEnd} de ${batchFiles.value.length}. Tanda ${index + 1} de ${uploadBatches.length}.`

      try {
        const data = await uploadFileBatch(files)
        addBatchSummary(summary, data, files.length)
        allResults.push(...(Array.isArray(data?.results) ? data.results : []))
      } catch (batchError) {
        const message = batchUploadErrorMessage(batchError, files)

        summary.total += files.length
        summary.failed += files.length
        summary.batches += 1
        summary.approverNotificationError = [summary.approverNotificationError, `Tanda ${index + 1}: ${message}`]
          .filter(Boolean)
          .join(' | ')

        allResults.push(...files.map((file) => ({
          fileName: file?.name || '',
          status: 'ERROR',
          message,
          employeeName: '',
          employeeDocument: '',
          areaCode: '',
          fechaEvaluacion: '',
          evaluationDate: '',
          birthDate: '',
          fechaNacimiento: '',
          resultStatus: '',
          reviewStatus: '',
          notificationStatus: ''
        })))
      }

      batchSummary.value = { ...summary }
      batchResults.value = [...allResults]
      persistUploadState()
    }
    const failed = Number(batchSummary.value.failed || 0)
    const success = Number(batchSummary.value.success || 0)
    const batches = Number(batchSummary.value.batches || 0)
    const approverStatus = String(batchSummary.value.approverNotificationStatus || '').toUpperCase()

    if (batchSummary.value.historical && failed > 0) {
      successMessage.value = `Carpeta historica guardada en ${batches} tanda${batches === 1 ? '' : 's'}. PDFs guardados: ${success}. No encontrados o con error: ${failed}. No se analizaron ni se enviaron correos.`
    } else if (batchSummary.value.historical) {
      successMessage.value = `Carpeta historica guardada en ${batches} tanda${batches === 1 ? '' : 's'}. PDFs guardados: ${success}. No se analizaron ni se enviaron correos.`
    } else if (failed > 0) {
      successMessage.value = 'Procesamiento finalizado con algunos errores. Los casos correctos quedaron pendientes de revisión.'
    } else if (approverStatus === 'FAILED') {
      successMessage.value = 'Carga procesada correctamente, pero falló el correo al aprobador. Revisa la configuración SMTP o la conexión al servidor de correo.'
    } else {
      successMessage.value = 'Procesamiento finalizado. Los casos correctos quedaron pendientes de revisión.'
    }
    if (!batchSummary.value.historical && failed > 0) {
      successMessage.value = `Procesamiento finalizado. Se intentaron leer ${batchSummary.value.total} PDF en ${batches} tanda${batches === 1 ? '' : 's'}. Correctos: ${success}. Con error: ${failed}.`
    } else if (!batchSummary.value.historical && approverStatus !== 'FAILED') {
      successMessage.value = `Procesamiento finalizado. Se leyeron ${success} PDF en ${batches} tanda${batches === 1 ? '' : 's'}. Los casos correctos quedaron pendientes de revision.`
    }
    persistUploadState()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo completar la carga de evaluaciones.'
    persistUploadState()
  } finally {
    batchLoading.value = false
    persistUploadState()

    if (batchSummary.value || batchResults.value.length) {
      await scrollToResults()

    }
  }
}

onMounted(() => {
  restoreUploadState()

  if (batchLoading.value) {
    startUploadSessionPolling()
  }
})

onBeforeUnmount(() => {
  stopUploadSessionPolling()
})

const processStatusLabel = (status) => {
  if (status === 'OK') return 'Correcto'
  if (status === 'ERROR') return 'Error'
  if (status === 'FAILED') return 'Fallido'
  return status || 'Sin estado'
}

const processStatusClass = (status) => {
  if (status === 'OK') return 'status-pill-active'
  return 'status-pill-inactive'
}

const resultStatusLabel = (status) => {
  if (status === 'APTO') return 'APTO'
  if (status === 'NO_APTO') return 'NO APTO'
  if (status === 'STORED') return 'GUARDADO'
  if (status === 'ARCHIVED') return 'GUARDADO'
  if (status === 'PENDIENTE') return 'PENDIENTE'
  return status || 'PENDIENTE'
}

const resultStatusClass = (status) => {
  if (status === 'APTO') return 'status-pill-active'
  if (status === 'NO_APTO') return 'status-pill-inactive'
  return 'status-pill-warning'
}

const reviewStatusLabel = (status) => {
  if (status === 'PENDING_REVIEW') return 'Pendiente'
  if (status === 'APPROVED') return 'Aprobado'
  if (status === 'REJECTED') return 'Rechazado'
  if (status === 'ARCHIVED') return 'Guardado'
  return status || '-'
}

const reviewStatusClass = (status) => {
  if (status === 'APPROVED') return 'status-pill-active'
  if (status === 'REJECTED') return 'status-pill-inactive'
  if (status === 'PENDING_REVIEW') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const approverEmailLabel = (status) => {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'SENT') return 'ENVIADO'
  if (normalized === 'FAILED') return 'FALLÓ'
  if (normalized === 'SKIPPED') return 'OMITIDO'

  return 'No informado'
}

const approverEmailAlertClass = (status) => {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'SENT') return 'approver-mail-alert--success'
  if (normalized === 'FAILED') return 'approver-mail-alert--danger'
  if (normalized === 'SKIPPED') return 'approver-mail-alert--warning'

  return 'approver-mail-alert--neutral'
}

const shortText = (value, maxLength = 160) => {
  const text = String(value || '').trim()

  if (text.length <= maxLength) {
    return text
  }

  return `${text.slice(0, maxLength)}...`
}

const normalizeProcessingError = (item) => {
  const rawMessage = String(item?.message || '').trim()
  const normalized = rawMessage.toLowerCase()

  if (
    normalized.includes('multipartexception') ||
    normalized.includes('multipart') ||
    normalized.includes('no recibio completo') ||
    normalized.includes('no recibió completo') ||
    normalized.includes('conexion se corto') ||
    normalized.includes('conexión se cortó') ||
    normalized.includes('interrumpio la subida') ||
    normalized.includes('interrumpió la subida')
  ) {
    return {
      title: 'PDF no recibido completo',
      description: 'El servidor no pudo recibir completo este archivo.',
      message: rawMessage || 'La subida de este PDF se corto antes de llegar completa al servidor.',
      recommendation: 'Vuelve a cargar solo este PDF. Si vuelve a fallar, revisa que no supere 150 MB y que el archivo abra correctamente.'
    }
  }

  if (
    normalized.includes('no tienes permiso') ||
    normalized.includes('no tiene permiso') ||
    normalized.includes('consultar esta zona') ||
    normalized.includes('operar sobre esta área') ||
    normalized.includes('operar sobre esta area') ||
    normalized.includes('zona autorizada') ||
    normalized.includes('fuera de zona')
  ) {
    return {
      title: 'No se puede procesar este PDF',
      description: 'El archivo fue leído, pero no se pudo registrar como evaluación.',
      message: 'El trabajador identificado no pertenece a una zona autorizada para tu usuario.',
      recommendation: 'Verifica que el PDF corresponda a tu zona asignada o solicita apoyo al administrador.'
    }
  }

  if (
    normalized.includes('trabajador no encontrado') ||
    normalized.includes('persona no encontrada') ||
    normalized.includes('no existe') ||
    normalized.includes('no se encontró el trabajador') ||
    normalized.includes('no se encontro el trabajador') ||
    normalized.includes('employee not found')
  ) {
    return {
      title: 'Persona no encontrada',
      description: 'El sistema no pudo asociar el PDF a un trabajador registrado.',
      message: 'No existe un trabajador registrado con la cédula identificada en el PDF.',
      recommendation: 'Verifica que el PDF sea correcto o registra el trabajador antes de procesar la evaluación.'
    }
  }

  if (
    normalized.includes('cédula') ||
    normalized.includes('cedula') ||
    normalized.includes('documento') ||
    normalized.includes('identificación') ||
    normalized.includes('identificacion')
  ) {
    return {
      title: 'No se identificó la cédula',
      description: 'El sistema no pudo identificar correctamente al trabajador.',
      message: 'No fue posible identificar correctamente la cédula del trabajador en el PDF.',
      recommendation: 'Verifica que el archivo corresponda a un concepto médico válido y que la cédula sea legible.'
    }
  }

  return {
    title: 'Archivo no procesado',
    description: 'El sistema no pudo registrar este archivo como evaluación.',
    message: rawMessage || 'No fue posible procesar este archivo.',
    recommendation: 'Verifica que el archivo sea un PDF válido de concepto médico y vuelve a intentarlo.'
  }
}

const userProcessingMessage = (item) => {
  if (item?.status === 'OK') {
    if (item?.reviewStatus === 'ARCHIVED' || item?.resultStatus === 'STORED') {
      return 'PDF guardado como historial. No se analizo y no se enviaron correos.'
    }

    return 'Documento cargado correctamente y pendiente de revision.'
  }

  return normalizeProcessingError(item).message
}

const openProcessingIssueModal = (item) => {
  const normalized = normalizeProcessingError(item)

  processingIssueModal.value = {
    fileName: item?.fileName || 'Archivo sin nombre',
    title: normalized.title,
    description: normalized.description,
    message: normalized.message,
    recommendation: normalized.recommendation
  }
}

const closeProcessingIssueModal = () => {
  processingIssueModal.value = null
}

const openFirstProcessingError = () => {
  const firstError = batchResults.value.find((item) => item.status !== 'OK')

  if (firstError) {
    openProcessingIssueModal(firstError)
  }
}

const mailErrorLabel = (value) => {
  const text = String(value || '').trim()

  if (!text) return '-'

  const normalized = text.toLowerCase()

  if (
    normalized.includes('mailconnectexception') ||
    normalized.includes('connect timed out') ||
    normalized.includes("couldn't connect") ||
    normalized.includes('connection timed out') ||
    normalized.includes('mail server connection failed') ||
    normalized.includes('sockettimeoutexception')
  ) {
    return 'No fue posible conectar con el servidor de correo. Verifica la red, VPN o configuración SMTP.'
  }

  if (
    normalized.includes('authentication failed') ||
    normalized.includes('bad credentials') ||
    normalized.includes('535')
  ) {
    return 'No fue posible autenticar el correo. Verifica usuario y contraseña SMTP.'
  }

  if (
    normalized.includes('recipient') ||
    normalized.includes('invalid address') ||
    normalized.includes('send failed')
  ) {
    return 'No fue posible enviar el correo. Revisa los destinatarios configurados.'
  }

  return 'No fue posible enviar el correo. Revisa la configuración de correo.'
}

const defaultProcessingMessage = (item) => {
  if (item.status === 'OK') {
    return 'Documento cargado y pendiente de revisión.'
  }

  return 'No fue posible procesar este archivo.'
}
</script>

<style scoped>
.document-upload-page {
  gap: 0.85rem;
}

.dropzone {
  border: 2px dashed var(--border);
  border-radius: var(--radius-lg);
  padding: 1.35rem 1rem;
  text-align: center;
  background: var(--surface-soft);
  transition: border-color 0.15s ease, background 0.15s ease, transform 0.15s ease;
}

.dropzone--active {
  border-color: var(--primary);
  background: var(--primary-soft);
  transform: translateY(-1px);
}

.historical-upload-option {
  margin-top: 1rem;
}

.historical-upload-option__label {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid rgba(146, 64, 14, 0.24);
  border-radius: 14px;
  background: #fffbeb;
  color: #111827;
}

.historical-upload-option__label input {
  margin-top: 0.15rem;
}

.historical-upload-option__label span {
  display: grid;
  gap: 0.2rem;
}

.historical-upload-option__label strong {
  font-size: 0.9rem;
  font-weight: 820;
}

.historical-upload-option__label small {
  color: #92400e;
  font-size: 0.78rem;
  line-height: 1.35;
}

.dropzone-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  margin-bottom: 0.7rem;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface);
  color: var(--text-muted);
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.selected-files-card {
  margin-top: 1rem;
  padding: 0.9rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
}

.selected-files-table th,
.selected-files-table td {
  padding: 0.48rem 0.45rem;
  font-size: 0.78rem;
}

.file-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.file-cell strong {
  color: var(--text);
  word-break: break-word;
}

.file-cell small {
  color: var(--text-muted);
  word-break: break-word;
}

.result-card {
  border: 1px solid color-mix(in srgb, var(--primary) 18%, var(--border));
  scroll-margin-top: 1rem;
}

.result-card .card-body {
  padding: 0.9rem 1rem;
}

.result-header .primary-btn {
  min-height: 34px;
  padding: 0.42rem 0.7rem;
  font-size: 0.8rem;
}

.upload-summary-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(92px, 1fr));
  gap: 0.5rem;
}

.upload-summary-item {
  min-height: 58px;
  padding: 0.58rem 0.62rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-soft);
}

.upload-summary-item span {
  display: block;
  color: var(--text-muted);
  font-size: 0.6rem;
  font-weight: 760;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.upload-summary-item strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--text);
  font-size: 0.92rem;
  line-height: 1.1;
  word-break: break-word;
}

.upload-summary-item.success {
  border-color: color-mix(in srgb, var(--success) 28%, var(--border));
}

.upload-summary-item.danger {
  border-color: color-mix(in srgb, var(--danger) 28%, var(--border));
}

.upload-summary-item.warning {
  border-color: color-mix(in srgb, var(--warning) 30%, var(--border));
}

.approver-mail-alert {
  margin-top: 0.8rem;
  padding: 0.75rem 0.85rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.approver-mail-alert__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.45rem;
}

.approver-mail-alert__header strong {
  display: block;
  margin-top: 0.2rem;
}

.approver-mail-alert p {
  margin: 0;
  color: var(--text-muted);
  word-break: break-word;
}

.approver-mail-alert pre {
  max-height: 180px;
  margin: 0.5rem 0 0;
  padding: 0.85rem;
  overflow: auto;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--text);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.82rem;
}

.approver-mail-alert--success {
  border-color: color-mix(in srgb, var(--success) 28%, var(--border));
  background: color-mix(in srgb, var(--success) 7%, var(--surface));
}

.approver-mail-alert--danger {
  border-color: color-mix(in srgb, var(--danger) 28%, var(--border));
  background: var(--danger-soft);
}

.approver-mail-alert--warning {
  border-color: color-mix(in srgb, var(--warning) 30%, var(--border));
  background: var(--warning-soft);
}

.approver-mail-alert--neutral {
  background: var(--surface-soft);
}

.small-btn {
  min-height: 32px;
  padding: 0.4rem 0.65rem;
  font-size: 0.76rem;
}

.process-note {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
  margin-top: 0.8rem;
  padding: 0.65rem 0.8rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
  color: var(--text-muted);
  font-size: 0.8rem;
}

.process-note strong {
  color: var(--text);
}

.upload-results-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 0.85rem;
  padding-top: 0.85rem;
  border-top: 1px solid var(--border);
}

.upload-results-pagination .pagination-actions {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.page-jump-input {
  width: 72px;
  min-height: 32px;
  padding: 0.35rem 0.45rem;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--surface);
  color: var(--text);
  font-size: 0.82rem;
  font-weight: 700;
  text-align: center;
}

.processing-table {
  min-width: 1080px;
}

.message-cell {
  max-width: 360px;
  color: var(--text-muted);
  word-break: break-word;
}

.result-row-error {
  background: var(--danger-soft);
}

.danger-btn {
  color: #991b1b;
  border-color: #fecaca;
  background: #fff1f2;
}

.danger-btn:hover {
  color: #7f1d1d;
  border-color: #fca5a5;
  background: #ffe4e6;
}

.processing-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(3px);
}

.processing-modal {
  width: min(680px, 100%);
  max-height: 90vh;
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-lg);
}

.processing-modal__header,
.processing-modal__footer,
.processing-modal__body {
  padding: 1rem 1.1rem;
}

.processing-modal__header,
.processing-modal__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.processing-modal__header h2 {
  margin: 0;
  color: var(--text);
  font-size: 1.2rem;
  font-weight: 820;
  line-height: 1.15;
}

.processing-modal__body {
  display: grid;
  gap: 0.65rem;
}

.processing-modal-field {
  padding: 0.75rem 0.85rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-soft);
}

.processing-modal-field span {
  display: block;
  color: var(--text-muted);
  font-size: 0.68rem;
  font-weight: 760;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.processing-modal-field strong {
  display: block;
  margin-top: 0.28rem;
  color: var(--text);
  font-size: 0.88rem;
  line-height: 1.35;
  word-break: break-word;
}

.processing-modal__footer {
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .upload-summary-strip {
    grid-template-columns: repeat(3, minmax(92px, 1fr));
  }
}

@media (max-width: 780px) {
  .upload-summary-strip {
    grid-template-columns: repeat(2, minmax(92px, 1fr));
  }

  .approver-mail-alert__header,
  .process-note {
    flex-direction: column;
    align-items: flex-start;
  }

  .small-btn {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .upload-summary-strip {
    grid-template-columns: 1fr;
  }
}
</style>
