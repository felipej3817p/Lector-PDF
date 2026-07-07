<template>
  <section class="page document-detail-page">
    <div class="dashboard-toolbar compact-toolbar">
      <div>
        <span class="mini-title">Detalle documental</span>
        <h1 class="detail-page-title">Detalle de evaluación</h1>
        <p class="p mb-0">
          Revisa estado, resultado médico, notificación y trazabilidad del documento.
        </p>
      </div>

      <div class="header-actions compact-actions">
        <button
          v-if="auth.canUploadDocuments"
          class="secondary-btn"
          :disabled="loading || analyzing"
          @click="analyzeDocumentAction"
        >
          {{ analyzing ? 'Analizando...' : analysisData ? 'Reanalizar' : 'Analizar' }}
        </button>

        <button
          v-if="auth.canDeleteDocuments"
          class="secondary-btn danger-btn"
          :disabled="loading || deleting"
          @click="deleteDocumentAction"
        >
          {{ deleting ? 'Eliminando...' : 'Eliminar' }}
        </button>

        <RouterLink
          v-if="auth.canReviewDocuments"
          to="/review"
          class="secondary-btn"
        >
          Volver a Revisión
        </RouterLink>

        <RouterLink
          to="/employees"
          class="secondary-btn"
        >
          Volver a trabajadores
        </RouterLink>

        <RouterLink
          v-if="auth.canUploadDocuments"
          to="/documents/upload"
          class="primary-btn"
        >
          Subir PDF
        </RouterLink>
      </div>
    </div>

    <div v-if="loading" class="state-box info">
      Cargando detalle del documento...
    </div>

    <div v-else-if="error" class="state-box error">
      {{ error }}
    </div>

    <template v-else-if="documentData">
      <div v-if="analysisWarning" class="state-box info">
        {{ analysisWarning }}
      </div>

      <div class="card border-0 compact-card">
        <div class="card-body">
          <div class="detail-main-header">
            <div>
              <span class="mini-title">Trabajador</span>
              <h2>{{ employeeDisplayName }}</h2>
              <p class="helper-text mb-0">
                {{ employeeDocumentLabel }}
                <span v-if="employeePosition"> · {{ employeePosition }}</span>
                <span v-if="employeeZone || employeeAreaCode">
                  · {{ employeeZone || employeeAreaCode }}
                </span>
              </p>
            </div>

            <div class="status-row">
              <div class="status-chip">
                <span class="status-chip-label">Resultado:</span>
                <span :class="resultClass(normalizedResultStatus)">
                  {{ resultLabel(normalizedResultStatus) }}
                </span>
              </div>

              <div class="status-chip">
                <span class="status-chip-label">Revisión:</span>
                <span :class="reviewClass(documentData.reviewStatus)">
                  {{ reviewLabel(documentData.reviewStatus) }}
                </span>
              </div>

              <div class="status-chip">
                <span class="status-chip-label">Correo:</span>
                <span :class="notificationClass(documentData.notificationStatus)">
                  {{ notificationLabel(documentData.notificationStatus) }}
                </span>
              </div>
            </div>
          </div>

          <div
            v-if="canShowReviewActions"
            class="compact-divider"
          ></div>

          <div
            v-if="canShowReviewActions"
            class="detail-actions-row"
          >
            <button
              class="primary-btn"
              :disabled="reviewing || !canApprove"
              @click="openReviewModal('approve')"
            >
              Aprobar y enviar
            </button>

            <button
              class="secondary-btn danger-btn"
              :disabled="reviewing"
              @click="openReviewModal('reject')"
            >
              Revisar
            </button>
          </div>

          <div v-if="!canApprove && canShowReviewActions" class="state-box error mt-3 mb-0">
            Solo se pueden aprobar documentos con resultado APTO o NO APTO.
          </div>

          <div v-if="documentData.reviewStatus === 'REJECTED'" class="state-box error mt-3 mb-0">
            Documento enviado a revisión. Comentario:
            <strong>{{ documentData.reviewComment || '-' }}</strong>
          </div>
        </div>
      </div>

      <div class="card border-0 compact-card">
        <div class="card-body">
          <div class="compact-section-header">
            <div>
              <h2 class="h4 mb-1">Datos principales</h2>
              <p class="helper-text mb-0">
                Información resumida del trabajador y del archivo cargado.
              </p>
            </div>
          </div>

          <div class="compact-divider"></div>

          <div class="info-grid">
            <div class="info-item">
              <span>Resultado</span>
              <strong>
                <span :class="resultClass(normalizedResultStatus)">
                  {{ resultLabel(normalizedResultStatus) }}
                </span>
              </strong>
            </div>

            <div class="info-item">
              <span>Archivo</span>
              <strong>{{ documentData.originalFileName || '-' }}</strong>
            </div>

            <div class="info-item">
              <span>Fecha evaluación</span>
              <strong>{{ formatLocalDate(evaluationDate) }}</strong>
            </div>

            <div class="info-item">
              <span>Fecha carga</span>
              <strong>{{ formatDate(documentData.uploadedAt) }}</strong>
            </div>

            <div class="info-item">
              <span>Subido por</span>
              <strong>{{ documentData.uploadedBy || '-' }}</strong>
            </div>

            <div class="info-item">
              <span>Estado técnico</span>
              <strong>{{ technicalStatusLabel(documentData.processingStatus) }}</strong>
            </div>

            <div class="info-item">
              <span>Correo trabajador</span>
              <strong>{{ employeeEmail || '-' }}</strong>
            </div>

            <div class="info-item">
              <span>Cargo</span>
              <strong>{{ employeePosition || '-' }}</strong>
            </div>

            <div class="info-item">
              <span>Área / dependencia</span>
              <strong>{{ employeeArea || '-' }}</strong>
            </div>

            <div class="info-item">
              <span>Zona</span>
              <strong>{{ employeeZone || employeeAreaCode || '-' }}</strong>
            </div>

            <div class="info-item">
              <span>Revisado por</span>
              <strong>{{ documentData.reviewedBy || '-' }}</strong>
            </div>

            <div class="info-item">
              <span>Fecha revisión</span>
              <strong>{{ formatDate(documentData.reviewedAt) }}</strong>
            </div>

            <div class="info-item full-span">
              <span>Comentario revisión</span>
              <strong>{{ documentData.reviewComment || '-' }}</strong>
            </div>
          </div>
        </div>
      </div>

      <div class="card border-0 compact-card">
        <div class="card-body">
          <div class="compact-section-header trace-header">
            <div>
              <h2 class="h4 mb-1">Notificación y trazabilidad</h2>
              <p class="helper-text mb-0">
                Historial de envíos, reenvíos, destinatarios y errores de correo.
              </p>
            </div>

            <div class="trace-header-actions">
              <div class="mail-summary">
                <span :class="notificationClass(documentData.notificationStatus)">
                  {{ notificationLabel(documentData.notificationStatus) }}
                </span>

                <span class="status-pill-neutral">
                  {{ emailLogs.length }} intento{{ emailLogs.length === 1 ? '' : 's' }}
                </span>
              </div>

              <button
                v-if="canResendEmail"
                type="button"
                class="secondary-btn resend-btn"
                :disabled="resendDisabled"
                @click="resendEmailAction"
              >
                {{ resendLoading ? 'Reenviando...' : 'Reenviar correo' }}
              </button>
            </div>
          </div>

          <div class="compact-divider"></div>

          <div class="trace-summary-grid">
            <div class="trace-summary-item">
              <span>Estado actual</span>
              <strong>{{ notificationLabel(documentData.notificationStatus) }}</strong>
            </div>

            <div class="trace-summary-item">
              <span>Última notificación</span>
              <strong>{{ formatDate(documentData.notifiedAt) }}</strong>
            </div>

            <div class="trace-summary-item">
              <span>Intentos registrados</span>
              <strong>{{ emailLogs.length }}</strong>
            </div>

            <div class="trace-summary-item">
              <span>Correo trabajador</span>
              <strong>{{ employeeEmail || '-' }}</strong>
            </div>
          </div>

          <div class="compact-divider"></div>

          <div v-if="emailLogs.length" class="table-fit-wrapper">
            <table class="table table-sm align-middle mail-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Para</th>
                  <th>CC</th>
                  <th>Estado</th>
                  <th>Asunto</th>
                  <th>Error</th>
                </tr>
              </thead>

              <tbody>
                <tr v-for="log in emailLogs" :key="log.id">
                  <td>{{ formatDate(log.attemptedAt || log.createdAt) }}</td>
                  <td>{{ emailTypeLabel(log.type) }}</td>
                  <td>{{ log.to || '-' }}</td>
                  <td>{{ log.cc || '-' }}</td>
                  <td>
                    <span :class="notificationClass(log.status)">
                      {{ notificationLabel(log.status) }}
                    </span>
                  </td>
                  <td>{{ log.subject || '-' }}</td>
                  <td>{{ mailErrorLabel(log.errorMessage) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="state-box mb-0">
            No hay logs de correo para este documento.
          </div>
        </div>
      </div>

      <div v-if="analysisData" class="card border-0 compact-card">
        <div class="card-body">
          <details class="compact-details" open>
            <summary>
              Información extraída
            </summary>

            <div class="compact-divider"></div>

            <div v-if="analysisData.extractedFields" class="info-grid">
              <div class="info-item">
                <span>Paciente</span>
                <strong>{{ analysisData.extractedFields.patientName || '-' }}</strong>
              </div>

              <div class="info-item">
                <span>Identificación</span>
                <strong>{{ analysisData.extractedFields.documentNumber || '-' }}</strong>
              </div>

              <div class="info-item">
                <span>Cargo extraído</span>
                <strong>{{ analysisData.extractedFields.position || '-' }}</strong>
              </div>

              <div class="info-item">
                <span>Tipo examen</span>
                <strong>{{ analysisData.extractedFields.examType || '-' }}</strong>
              </div>

              <div class="info-item">
                <span>Fecha nacimiento</span>
                <strong>{{ formatLocalDate(analysisData.extractedFields.birthDate) }}</strong>
              </div>

              <div class="info-item">
                <span>ARL</span>
                <strong>{{ analysisData.extractedFields.arl || '-' }}</strong>
              </div>

              <div class="info-item full-span">
                <span>Concepto laboral</span>
                <strong>{{ analysisData.extractedFields.laborConcept || '-' }}</strong>
              </div>
            </div>

            <div v-if="analysisData.extractedText" class="mt-4">
              <label class="label">Texto completo extraído</label>
              <textarea
                :value="analysisData.extractedText"
                rows="6"
                class="form-control extracted-textarea"
                readonly
              ></textarea>
            </div>
          </details>
        </div>
      </div>

      <div class="card border-0 compact-card">
        <div class="card-body">
          <div class="compact-section-header">
            <div>
              <h2 class="h4 mb-1">Visualización del documento</h2>
              <p class="helper-text mb-0">PDF original cargado en el sistema.</p>
            </div>
            <div class="header-actions">
              <button
                v-if="pdfUrl"
                type="button"
                class="secondary-btn"
                @click="downloadOriginalPdf"
              >
                Descargar original
              </button>
            </div>
          </div>

          <div class="compact-divider"></div>

          <div v-if="pdfLoading" class="pdf-state-box">
            <div class="spinner-mini"></div>
            <span>Cargando previsualización...</span>
          </div>

          <div v-else-if="pdfError" class="state-box error mb-0">
            {{ pdfError }}
            <button class="secondary-btn small-btn mt-2" @click="loadPdfBlob">Reintentar</button>
          </div>

          <div v-else-if="pdfUrl" class="pdf-viewer-frame">
            <iframe
              :src="pdfUrl"
              width="100%"
              height="800px"
              class="pdf-iframe"
              frameborder="0"
            ></iframe>
          </div>

          <div v-else class="state-box info mb-0">
            No hay previsualización disponible.
          </div>
        </div>
      </div>

      <div
        v-if="reviewModalOpen"
        class="review-modal-backdrop"
        @click.self="closeReviewModal"
      >
        <article class="review-modal">
          <header class="review-modal__header">
            <div>
              <span class="mini-title">Confirmar revisión</span>
              <h2>{{ reviewModalTitle }}</h2>
              <p class="helper-text mb-0">
                {{ reviewModalDescription }}
              </p>
            </div>

            <button
              type="button"
              class="secondary-btn"
              :disabled="reviewing"
              @click="closeReviewModal"
            >
              Cerrar
            </button>
          </header>

          <div class="compact-divider"></div>

          <div class="review-modal__body">
            <label class="label" for="reviewComment">Comentario</label>
            <textarea
              id="reviewComment"
              v-model.trim="reviewComment"
              class="form-control"
              rows="4"
              placeholder="Escribe una observación si aplica..."
              :disabled="reviewing"
            ></textarea>
          </div>

          <footer class="review-modal__footer">
            <button
              type="button"
              class="secondary-btn"
              :disabled="reviewing"
              @click="closeReviewModal"
            >
              Cancelar
            </button>

            <button
              type="button"
              :class="reviewAction === 'approve' ? 'primary-btn' : 'secondary-btn danger-btn'"
              :disabled="reviewing || (reviewAction === 'approve' && !canApprove)"
              @click="confirmReviewAction"
            >
              {{ reviewing ? 'Procesando...' : reviewModalConfirmLabel }}
            </button>
          </footer>
        </article>
      </div>
    </template>

    <Transition name="toast-slide">
      <div
        v-if="toastMessage"
        class="review-toast"
        :class="toastType"
      >
        <div class="review-toast__icon">
          {{ toastType === 'error' ? '!' : '✓' }}
        </div>

        <div class="review-toast__content">
          <strong>{{ toastTitle }}</strong>
          <span>{{ toastMessage }}</span>
        </div>
      </div>
    </Transition>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  analyzeDocument,
  approveDocument,
  deleteDocument,
  getDocumentById,
  rejectDocument,
  resendDocumentEmail,
  getDocumentEmailLogs
} from '../api/document'
import { getEmployeeById } from '../api/employee'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const analyzing = ref(false)
const deleting = ref(false)
const reviewing = ref(false)

const error = ref('')
const analysisWarning = ref('')
const reviewComment = ref('')

const toastTitle = ref('')
const toastMessage = ref('')
const toastType = ref('success')
let toastTimeout = null

const documentData = ref(null)
const analysisData = ref(null)
const employeeData = ref(null)
const emailLogs = ref([])
const resendLoading = ref(false)

const pdfUrl = ref(null)
const pdfLoading = ref(false)
const pdfError = ref('')

const reviewModalOpen = ref(false)
const reviewAction = ref('approve')

const documentId = computed(() => route.params.id)
const extractedFields = computed(() => analysisData.value?.extractedFields || {})

const evaluationDate = computed(() => {
  return (
    documentData.value?.evaluationDate ||
    documentData.value?.fechaEvaluacion ||
    documentData.value?.fechaConcepto ||
    analysisData.value?.evaluationDate ||
    analysisData.value?.fechaEvaluacion ||
    analysisData.value?.fechaConcepto ||
    analysisData.value?.conceptDate ||
    extractedFields.value?.evaluationDate ||
    extractedFields.value?.fechaEvaluacion ||
    extractedFields.value?.fechaConcepto ||
    extractedFields.value?.conceptDate ||
    ''
  )
})

const normalizedResultStatus = computed(() => {
  const result = String(analysisData.value?.resultStatus || documentData.value?.resultStatus || '').toUpperCase().trim()

  if (result === 'APTO') return 'APTO'
  if (result === 'NO_APTO' || result === 'NO APTO') return 'NO_APTO'

  return 'PENDIENTE'
})

const canApprove = computed(() => {
  return normalizedResultStatus.value === 'APTO' || normalizedResultStatus.value === 'NO_APTO'
})

const canShowReviewActions = computed(() => {
  return (
    analysisData.value &&
    auth.canReviewDocuments &&
    documentData.value?.reviewStatus !== 'APPROVED' &&
    documentData.value?.reviewStatus !== 'REJECTED'
  )
})

const canResendEmail = computed(() => {
  const reviewStatus = documentData.value?.reviewStatus
  const notificationStatus = documentData.value?.notificationStatus

  const hasValidStatus = 
    reviewStatus === 'APPROVED' || 
    reviewStatus === 'REJECTED' || 
    notificationStatus === 'FAILED' || 
    notificationStatus === 'SKIPPED' || 
    notificationStatus === 'SENT'

  return (
    auth.canManageNotifications &&
    hasValidStatus &&
    Boolean(employeeEmail.value)
  )
})

const resendDisabled = computed(() => {
  return resendLoading.value || !canResendEmail.value
})

const latestEmailLog = computed(() => {
  if (!emailLogs.value.length) return null

  return [...emailLogs.value].sort((a, b) => {
    const left = new Date(a.attemptedAt || a.createdAt || 0).getTime()
    const right = new Date(b.attemptedAt || b.createdAt || 0).getTime()

    return right - left
  })[0]
})

const employeeDisplayName = computed(() => {
  const fromEmployee = [
    employeeData.value?.firstName,
    employeeData.value?.secondName,
    employeeData.value?.firstLastName,
    employeeData.value?.secondLastName
  ]
    .filter(Boolean)
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim()

  return fromEmployee || extractedFields.value.patientName || 'Trabajador no identificado'
})

const employeeDocumentLabel = computed(() => {
  const fromEmployee = [
    employeeData.value?.documentType,
    employeeData.value?.documentNumber
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return fromEmployee || extractedFields.value.documentNumber || '-'
})

const employeePosition = computed(() => {
  return employeeData.value?.currentPosition || extractedFields.value.position || ''
})

const employeeArea = computed(() => employeeData.value?.workArea || '')
const employeeZone = computed(() => employeeData.value?.zone || '')
const employeeEmail = computed(() => employeeData.value?.email || '')
const employeeAreaCode = computed(() => employeeData.value?.areaCode || documentData.value?.areaCode || '')

const reviewModalTitle = computed(() => {
  return reviewAction.value === 'approve'
    ? 'Aprobar evaluación'
    : 'Revisar documento'
})

const reviewModalDescription = computed(() => {
  return reviewAction.value === 'approve'
    ? 'Al aprobar, el sistema intentará enviar el correo al trabajador y a las copias configuradas.'
    : 'Al solicitar la revisión, el sistema notificará al cargador original con el comentario registrado para que lo revise.'
})

const reviewModalConfirmLabel = computed(() => {
  return reviewAction.value === 'approve'
    ? 'Aprobar y enviar'
    : 'Revisar'
})

const formatDate = (value) => {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('es-CO', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatLocalDate = (value) => {
  if (!value) return '-'

  const text = String(value).trim()
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})/)

  if (match) {
    return `${match[3]}/${match[2]}/${match[1]}`
  }

  return formatDate(value)
}

const shortValue = (value, max = 90) => {
  const text = String(value || '').replace(/\s+/g, ' ').trim()

  if (!text) return '-'

  return text.length > max ? `${text.slice(0, max).trim()}...` : text
}

const technicalStatusLabel = (status) => {
  if (status === 'STORED') return 'GUARDADO'
  if (status === 'PROCESSING') return 'PROCESANDO'
  if (status === 'COMPLETED') return 'COMPLETADO'
  if (status === 'ERROR') return 'ERROR'
  return status || '-'
}

const resultLabel = (status) => {
  if (status === 'APTO') return 'APTO'
  if (status === 'NO_APTO') return 'NO APTO'
  return 'PENDIENTE'
}

const reviewLabel = (status) => {
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  if (status === 'PENDING_REVIEW') return 'PENDIENTE'
  if (status === 'APPROVED') return 'APROBADO'
  if (status === 'REJECTED') return 'A REVISAR'
  return status || 'PENDIENTE'
}

const notificationLabel = (status) => {
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  if (status === 'SENT') return 'ENVIADO'
  if (status === 'FAILED') return 'FALLÓ'
  if (status === 'SKIPPED') return 'OMITIDO'
  return status || 'NO ENVIADO'
}

const emailTypeLabel = (type) => {
  if (type === 'WORKER_NOTIFICATION') return 'Trabajador'
  if (type === 'WORKER_NOTIFICATION_RESEND') return 'Reenvío'
  if (type === 'APPROVER_BATCH_NOTIFICATION') return 'Aprobador'
  return type || '-'
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

const resultClass = (status) => {
  if (status === 'APTO') return 'status-pill-active'
  if (status === 'NO_APTO') return 'status-pill-inactive'
  return 'status-pill-warning'
}

const reviewClass = (status) => {
  if (status === 'APPROVED') return 'status-pill-active'
  if (status === 'REJECTED') return 'status-pill-inactive'
  return 'status-pill-warning'
}

const notificationClass = (status) => {
  if (status === 'SENT') return 'status-pill-active'
  if (status === 'FAILED') return 'status-pill-inactive'
  if (status === 'SKIPPED') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const showToast = (title, message, type = 'success') => {
  toastTitle.value = title
  toastMessage.value = message
  toastType.value = type

  if (toastTimeout) {
    clearTimeout(toastTimeout)
  }

  toastTimeout = setTimeout(() => {
    toastTitle.value = ''
    toastMessage.value = ''
    toastType.value = 'success'
    toastTimeout = null
  }, 4500)
}

const showReviewNotificationToast = (actionLabel) => {
  const status = documentData.value?.notificationStatus
  const log = latestEmailLog.value
  const isRejected = documentData.value?.reviewStatus === 'REJECTED'
  const recipient = isRejected ? 'al cargador original' : 'al trabajador'

  if (status === 'SENT') {
    showToast(
      'Correo enviado',
      `${actionLabel}. La notificación fue enviada correctamente ${recipient}.`,
      'success'
    )
    return
  }

  if (status === 'FAILED') {
    showToast(
      'Correo no enviado',
      `${actionLabel}, pero la notificación falló. ${mailErrorLabel(log?.errorMessage)}`,
      'error'
    )
    return
  }

  if (status === 'SKIPPED') {
    showToast(
      'Correo omitido',
      `${actionLabel}, pero el sistema omitió el envío. Revisa la trazabilidad del correo.`,
      'warning'
    )
    return
  }

  showToast(
    'Revisión guardada',
    `${actionLabel}. No se encontró confirmación de envío de correo; revisa la trazabilidad.`,
    'warning'
  )
}

const loadEmployee = async () => {
  const employeeId = documentData.value?.employeeId

  if (!employeeId) {
    employeeData.value = null
    return
  }

  try {
    const response = await getEmployeeById(employeeId)
    employeeData.value = response.data || null
  } catch (err) {
    employeeData.value = null
    console.error('Error cargando trabajador asociado:', err)
  }
}

const loadAnalysis = async () => {
  analysisWarning.value = ''

  try {
    const response = await http.get(`/api/documents/${documentId.value}/analysis`)
    analysisData.value = response.data || null
  } catch {
    analysisData.value = null
    analysisWarning.value = 'Este documento aún no tiene análisis guardado.'
  }
}

const loadEmailLogs = async () => {
  try {
    const { data } = await getDocumentEmailLogs(documentId.value)
    emailLogs.value = Array.isArray(data) ? data : []
  } catch {
    emailLogs.value = []
  }
}

const resendEmailAction = async () => {
  if (!auth.canManageNotifications) {
    error.value = 'No tienes permisos para reenviar correos.'
    return
  }

  if (documentData.value?.reviewStatus !== 'APPROVED' && documentData.value?.reviewStatus !== 'REJECTED') {
    error.value = 'Solo se puede reenviar correo en documentos aprobados o rechazados.'
    return
  }

  if (!employeeEmail.value) {
    error.value = 'El trabajador no tiene correo registrado en la base de datos. Actualiza su ficha antes de reenviar la notificación.'
    return
  }

  if (!window.confirm('¿Deseas reenviar la notificación al trabajador?')) {
    return
  }

  try {
    resendLoading.value = true
    error.value = ''

    await resendDocumentEmail(documentId.value)
    await loadDetail()

    showReviewNotificationToast('Reenvío ejecutado')
  } catch (err) {
    const message = err?.response?.data?.message || 'No se pudo reenviar el correo.'

    error.value = message
    showToast('No se pudo reenviar', message, 'error')
  } finally {
    resendLoading.value = false
  }
}

const loadDetail = async () => {
  try {
    loading.value = true
    error.value = ''
    analysisWarning.value = ''

    const documentResponse = await getDocumentById(documentId.value)
    documentData.value = documentResponse.data || null

    await loadEmployee()
    await loadAnalysis()
    await loadEmailLogs()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar el detalle del documento.'
    console.error('Error cargando detalle del documento:', err)
  } finally {
    loading.value = false
  }
}

const analyzeDocumentAction = async () => {
  if (!auth.canUploadDocuments) {
    error.value = 'No tienes permisos para analizar documentos.'
    return
  }

  try {
    analyzing.value = true
    error.value = ''

    await analyzeDocument(documentId.value)
    await loadDetail()
    await loadPdfBlob()

    showToast(
      'Análisis completado',
      'El análisis del documento se ejecutó correctamente.',
      'success'
    )
  } catch (err) {
    const message = err?.response?.data?.message || 'No se pudo analizar el documento.'

    error.value = message
    showToast('No se pudo analizar', message, 'error')
    console.error('Error analizando documento:', err)
  } finally {
    analyzing.value = false
  }
}

const openReviewModal = (action) => {
  if (!auth.canReviewDocuments) {
    error.value = 'No tienes permisos para revisar documentos.'
    return
  }

  reviewAction.value = action
  reviewComment.value = ''
  reviewModalOpen.value = true
}

const closeReviewModal = () => {
  if (reviewing.value) return

  reviewModalOpen.value = false
}

const forceCloseReviewModal = () => {
  reviewModalOpen.value = false
}

const confirmReviewAction = async () => {
  if (reviewAction.value === 'approve') {
    await approveDocumentAction()
  } else {
    await rejectDocumentAction()
  }
}

const approveDocumentAction = async () => {
  if (!auth.canReviewDocuments) {
    error.value = 'No tienes permisos para aprobar documentos.'
    return
  }

  if (!canApprove.value) {
    error.value = 'Solo se pueden aprobar documentos con resultado APTO o NO APTO.'
    return
  }

  try {
    reviewing.value = true
    error.value = ''

    await approveDocument(documentId.value, reviewComment.value)

    forceCloseReviewModal()
    await loadDetail()

    showReviewNotificationToast('Documento aprobado')
  } catch (err) {
    const message = err?.response?.data?.message || 'No se pudo aprobar y notificar el documento.'

    error.value = message
    showToast('No se pudo aprobar', message, 'error')
    console.error('Error aprobando documento:', err)
  } finally {
    reviewing.value = false
  }
}

const rejectDocumentAction = async () => {
  if (!auth.canReviewDocuments) {
    error.value = 'No tienes permisos para solicitar revisión.'
    return
  }

  try {
    reviewing.value = true
    error.value = ''

    await rejectDocument(documentId.value, reviewComment.value)

    forceCloseReviewModal()
    await loadDetail()

    showReviewNotificationToast('Documento enviado a revisión')
  } catch (err) {
    const message = err?.response?.data?.message || 'No se pudo solicitar la revisión del documento.'

    error.value = message
    showToast('No se pudo enviar', message, 'error')
    console.error('Error solicitando revisión:', err)
  } finally {
    reviewing.value = false
  }
}

const deleteDocumentAction = async () => {
  if (!auth.canDeleteDocuments) {
    error.value = 'No tienes permisos para eliminar documentos.'
    return
  }

  const confirmed = window.confirm('¿Seguro que quieres eliminar este documento? Esta acción no se puede deshacer.')

  if (!confirmed) return

  try {
    deleting.value = true
    error.value = ''

    await deleteDocument(documentId.value)
    await router.push('/review')
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo eliminar el documento.'
    console.error('Error eliminando documento:', err)
  } finally {
    deleting.value = false
  }
}

const loadPdfBlob = async () => {
  if (pdfUrl.value) {
    URL.revokeObjectURL(pdfUrl.value)
    pdfUrl.value = null
  }

  try {
    pdfLoading.value = true
    pdfError.value = ''

    const response = await http.get(`/api/documents/${documentId.value}/view`, {
      responseType: 'blob'
    })

    const blob = new Blob([response.data], { type: 'application/pdf' })
    pdfUrl.value = URL.createObjectURL(blob)
  } catch (err) {
    console.error('Error cargando PDF:', err)
    pdfError.value = 'No se pudo cargar la previsualización del PDF. Es posible que el archivo no exista o no tengas permisos.'
  } finally {
    pdfLoading.value = false
  }
}

const downloadOriginalPdf = () => {
  if (!pdfUrl.value) return

  const link = document.createElement('a')
  link.href = pdfUrl.value
  link.download = documentData.value?.originalFileName || `documento-${documentId.value}.pdf`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

onMounted(async () => {
  await loadDetail()
  loadPdfBlob()
})

onBeforeUnmount(() => {
  if (pdfUrl.value) {
    URL.revokeObjectURL(pdfUrl.value)
  }
  if (toastTimeout) {
    clearTimeout(toastTimeout)
  }
})
</script>

<style scoped>
.document-detail-page {
  gap: 0.75rem;
}

.compact-toolbar {
  padding: 0.85rem 0.95rem;
  min-height: auto;
}

.detail-page-title {
  margin: 0;
  color: var(--text);
  font-size: clamp(1.25rem, 1.55vw, 1.6rem);
  font-weight: 820;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.compact-actions {
  gap: 0.45rem;
}

.compact-actions .primary-btn,
.compact-actions .secondary-btn {
  min-height: 34px;
  padding: 0.42rem 0.7rem;
  font-size: 0.8rem;
}

.compact-card .card-body {
  padding: 0.75rem 0.85rem;
}

.detail-main-header,
.compact-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.detail-main-header h2 {
  margin: 0;
  color: var(--text);
  font-size: 1.05rem;
  font-weight: 820;
  line-height: 1.15;
}

.status-row,
.mail-summary,
.detail-actions-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.status-row {
  justify-content: flex-end;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 30px;
  padding: 0.25rem 0.32rem 0.25rem 0.55rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-soft);
  white-space: nowrap;
}

.status-chip-label {
  color: var(--text-muted);
  font-size: 0.68rem;
  font-weight: 800;
  line-height: 1;
}

.status-chip .status-pill-active,
.status-chip .status-pill-inactive,
.status-chip .status-pill-warning,
.status-chip .status-pill-neutral {
  margin: 0;
  font-size: 0.66rem;
}

.detail-actions-row {
  justify-content: flex-end;
}

.compact-divider {
  height: 1px;
  margin: 0.55rem 0;
  background: var(--border);
}

.compact-section-header h2,
.compact-card-header h2 {
  font-size: 1rem;
}

.compact-section-header p,
.compact-card-header p {
  font-size: 0.8rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 0.45rem;
}

.info-item {
  min-height: auto;
  padding: 0.5rem 0.6rem;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: var(--surface-soft);
}

.info-item span {
  display: block;
  color: var(--text-muted);
  font-size: 0.62rem;
  font-weight: 760;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-item strong {
  display: block;
  margin-top: 0.22rem;
  color: var(--text);
  font-size: 0.76rem;
  line-height: 1.22;
  word-break: break-word;
}

.info-item strong .status-pill-active,
.info-item strong .status-pill-inactive,
.info-item strong .status-pill-warning,
.info-item strong .status-pill-neutral {
  display: inline-flex;
  margin-top: 0;
  padding: 0.28rem 0.5rem;
  font-size: 0.66rem;
}

.full-span {
  grid-column: 1 / -1;
}

.trace-header {
  align-items: center;
}

.trace-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.resend-btn {
  min-height: 30px;
  padding: 0.34rem 0.58rem;
  font-size: 0.74rem;
  white-space: nowrap;
  background: #41d58d;
  border: 1px solid #20a96b;
  color: #052e1b;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(65, 213, 141, 0.24);
}

.resend-btn:hover:not(:disabled) {
  background: #34c77f;
  border-color: #168f59;
  color: #052e1b;
}

.resend-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.trace-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 0.45rem;
}

.trace-summary-item {
  padding: 0.5rem 0.6rem;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: var(--surface-soft);
}

.trace-summary-item span {
  display: block;
  color: var(--text-muted);
  font-size: 0.6rem;
  font-weight: 760;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.trace-summary-item strong {
  display: block;
  margin-top: 0.22rem;
  color: var(--text);
  font-size: 0.74rem;
  line-height: 1.22;
  word-break: break-word;
}

.table-fit-wrapper {
  width: 100%;
  overflow: visible;
}

.mail-table {
  width: 100%;
  table-layout: fixed;
  margin-bottom: 0;
}

.mail-table th {
  padding: 0.42rem 0.35rem;
  font-size: 0.62rem;
  line-height: 1.1;
  white-space: normal;
}

.mail-table td {
  padding: 0.42rem 0.35rem;
  font-size: 0.72rem;
  line-height: 1.2;
  word-break: break-word;
}

.mail-table td:nth-child(7) {
  max-width: 240px;
  color: var(--text-muted);
}

.compact-details summary {
  cursor: pointer;
  color: var(--text);
  font-size: 0.95rem;
  font-weight: 780;
  list-style: none;
}

.compact-details summary::-webkit-details-marker {
  display: none;
}

.compact-details summary::after {
  content: "▾";
  margin-left: 0.45rem;
  color: var(--text-muted);
  font-size: 0.8rem;
}

.extracted-textarea {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.76rem;
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

.review-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(3px);
}

.review-modal {
  width: min(620px, 100%);
  max-height: 90vh;
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-md);
}

.review-modal__header,
.review-modal__footer,
.review-modal__body {
  padding: 1rem 1.1rem;
}

.review-modal__header,
.review-modal__footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.review-modal__header h2 {
  margin: 0;
  color: var(--text);
  font-size: 1.12rem;
  font-weight: 820;
}

.review-modal__footer {
  justify-content: flex-end;
}

.review-toast {
  position: fixed;
  left: 50%;
  bottom: 1.25rem;
  z-index: 120;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  width: min(460px, calc(100vw - 2rem));
  padding: 0.9rem 1rem;
  border: 1px solid #86efac;
  border-radius: 16px;
  background: #dcfce7;
  color: #166534;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.18);
  transform: translateX(-50%);
}

.review-toast.error {
  border-color: #fecaca;
  background: #fee2e2;
  color: #991b1b;
}

.review-toast.warning {
  border-color: #fde68a;
  background: #fef3c7;
  color: #92400e;
}

.review-toast__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: rgba(22, 101, 52, 0.12);
  color: #166534;
  font-size: 0.9rem;
  font-weight: 900;
}

.review-toast.error .review-toast__icon {
  background: rgba(153, 27, 27, 0.12);
  color: #991b1b;
}

.review-toast.warning .review-toast__icon {
  background: rgba(146, 64, 14, 0.12);
  color: #92400e;
}

.review-toast__content {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;
  min-width: 0;
}

.review-toast__content strong {
  font-size: 0.88rem;
  font-weight: 850;
  line-height: 1.2;
}

.review-toast__content span {
  font-size: 0.78rem;
  font-weight: 650;
  line-height: 1.35;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(18px);
}

.toast-slide-enter-to,
.toast-slide-leave-from {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.pdf-viewer-frame {
  margin-top: 0.5rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--surface-soft);
}

.pdf-iframe {
  display: block;
  background: white;
}

.pdf-state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 3rem 1rem;
  color: var(--text-muted);
}

.spinner-mini {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1180px) {
  .info-grid,
  .trace-summary-grid {
    grid-template-columns: repeat(3, minmax(130px, 1fr));
  }
}

@media (max-width: 900px) {
  .table-fit-wrapper {
    overflow-x: auto;
  }

  .mail-table {
    min-width: 900px;
    table-layout: auto;
  }
}

@media (max-width: 720px) {
  .compact-toolbar,
  .review-modal__header,
  .review-modal__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .info-grid,
  .trace-summary-grid {
    grid-template-columns: 1fr;
  }

  .detail-actions-row,
  .trace-header-actions {
    justify-content: stretch;
  }

  .status-row {
    justify-content: flex-start;
    width: 100%;
  }

  .status-chip {
    width: 100%;
    justify-content: space-between;
  }

  .detail-actions-row .primary-btn,
  .detail-actions-row .secondary-btn,
  .trace-header-actions .secondary-btn,
  .review-modal__footer .primary-btn,
  .review-modal__footer .secondary-btn {
    width: 100%;
  }
}
</style>
