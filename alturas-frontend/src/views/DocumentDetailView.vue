<template>
  <section class="page">
    <div class="page-header">
      <div>
        <span class="mini-title">Detalle documental</span>
        <h1>Detalle de la evaluación</h1>
        <p>
          Consulta el resultado del análisis, revisa la información extraída y aprueba o rechaza
          la notificación formal.
        </p>
      </div>

      <div class="header-actions">
        <button
          class="primary-btn"
          :disabled="loading || analyzing"
          @click="analyzeDocumentAction"
        >
          {{ analyzing ? 'Analizando...' : analysisData ? 'Reanalizar documento' : 'Analizar documento' }}
        </button>

        <button
          class="secondary-btn danger-btn"
          :disabled="loading || deleting"
          @click="deleteDocumentAction"
        >
          {{ deleting ? 'Eliminando...' : 'Eliminar documento' }}
        </button>

        <RouterLink to="/documents" class="secondary-btn">
          Volver al panel
        </RouterLink>

        <RouterLink to="/documents/upload" class="secondary-btn">
          Subir otro PDF
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
      <div v-if="successMessage" class="state-box info">
        {{ successMessage }}
      </div>

      <div v-if="analysisWarning" class="state-box info">
        {{ analysisWarning }}
      </div>

      <div class="summary-grid">
        <div class="summary-card">
          <span class="label">Archivo</span>
          <span>{{ documentData.originalFileName || '-' }}</span>
        </div>

        <div class="summary-card">
          <span class="label">Fecha y hora de carga</span>
          <span>{{ formatDate(documentData.uploadedAt) }}</span>
        </div>

        <div class="summary-card">
          <span class="label">Subido por</span>
          <span>{{ documentData.uploadedBy || '-' }}</span>
        </div>

        <div class="summary-card">
          <span class="label">Estado técnico</span>
          <span>{{ documentData.processingStatus || '-' }}</span>
        </div>

        <div class="summary-card">
          <span class="label">Estado revisión</span>
          <span :class="reviewClass(documentData.reviewStatus)">
            {{ reviewLabel(documentData.reviewStatus) }}
          </span>
        </div>

        <div class="summary-card">
          <span class="label">Estado notificación</span>
          <span :class="notificationClass(documentData.notificationStatus)">
            {{ notificationLabel(documentData.notificationStatus) }}
          </span>
        </div>
      </div>

      <div class="card border-0">
        <div class="card-body">
          <div class="detail-hero">
            <div>
              <span class="mini-title">Resultado médico</span>
              <h2 class="detail-title">{{ employeeDisplayName }}</h2>
              <p class="helper-text mb-0">
                {{ employeeDocumentLabel }}
                <span v-if="employeePosition"> • {{ employeePosition }}</span>
                <span v-if="employeeAreaCode"> • {{ employeeAreaCode }}</span>
              </p>
            </div>

            <div class="detail-result-box">
              <span
                v-if="normalizedResultStatus === 'APTO'"
                class="status-pill-active detail-result-pill"
              >
                APTO
              </span>

              <span
                v-else-if="normalizedResultStatus === 'NO_APTO'"
                class="status-pill-inactive detail-result-pill"
              >
                NO APTO
              </span>

              <span
                v-else
                class="status-pill-warning detail-result-pill"
              >
                PENDIENTE
              </span>
            </div>
          </div>

          <div class="hr"></div>

          <div class="fields-grid">
            <div class="field-card">
              <span class="label">Tipo de documento</span>
              <span>{{ documentData.documentType || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Tipo de examen</span>
              <span>{{ documentData.examType || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Trabajador</span>
              <span>{{ employeeDisplayName }}</span>
            </div>

            <div class="field-card">
              <span class="label">Documento</span>
              <span>{{ employeeDocumentLabel }}</span>
            </div>

            <div class="field-card">
              <span class="label">Cargo</span>
              <span>{{ employeePosition || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Área / dependencia</span>
              <span>{{ employeeArea || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Zona</span>
              <span>{{ employeeZone || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Área código</span>
              <span>{{ employeeAreaCode || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Correo trabajador</span>
              <span>{{ employeeEmail || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Revisado por</span>
              <span>{{ documentData.reviewedBy || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Fecha revisión</span>
              <span>{{ formatDate(documentData.reviewedAt) }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Comentario revisión</span>
              <span>{{ documentData.reviewComment || '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="analysisData" class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Campos extraídos del PDF</h2>
              <p class="helper-text mb-0">
                Información detectada automáticamente por el análisis documental.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <div class="fields-grid">
            <div class="field-card">
              <span class="label">Paciente</span>
              <span>{{ extractedFields.patientName || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Identificación</span>
              <span>{{ extractedFields.documentNumber || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Cargo</span>
              <span>{{ extractedFields.position || employeePosition || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Tipo de examen</span>
              <span>{{ extractedFields.examType || documentData.examType || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Fecha de nacimiento</span>
              <span>{{ extractedFields.birthDate || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">ARL</span>
              <span>{{ extractedFields.arl || '-' }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Concepto laboral</span>
              <span>{{ extractedFields.laborConcept || '-' }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Observaciones</span>
              <span>{{ extractedFields.observations || '-' }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Programa de vigilancia</span>
              <span>{{ extractedFields.surveillanceProgram || '-' }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Remisiones</span>

              <ul
                v-if="Array.isArray(extractedFields.referrals) && extractedFields.referrals.length"
                class="mb-0 ps-3"
              >
                <li v-for="(item, index) in extractedFields.referrals" :key="index">
                  {{ item }}
                </li>
              </ul>

              <span v-else>-</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="card border-0">
        <div class="card-body">
          <div class="state-box info mb-0">
            Este documento todavía no tiene análisis guardado. Usa el botón
            <strong>Analizar documento</strong>.
          </div>
        </div>
      </div>

      <div class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Revisión y notificación</h2>
              <p class="helper-text mb-0">
                La notificación al trabajador solo se envía cuando un administrador o aprobador
                valida el resultado.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <div v-if="!analysisData" class="state-box info mb-0">
            Primero debes analizar el documento antes de aprobar o rechazar.
          </div>

          <div v-else-if="!auth.canReviewDocuments" class="state-box info mb-0">
            Solo el administrador o aprobador de Seguridad, Salud y Ambiente puede aprobar y notificar.
          </div>

          <div v-else-if="documentData.reviewStatus === 'APPROVED'" class="state-box info mb-0">
            Este documento ya fue aprobado. Estado de notificación:
            <strong>{{ notificationLabel(documentData.notificationStatus) }}</strong>.
          </div>

          <div v-else-if="documentData.reviewStatus === 'REJECTED'" class="state-box error mb-0">
            Este documento fue rechazado. Comentario:
            <strong>{{ documentData.reviewComment || '-' }}</strong>
          </div>

          <div v-else>
            <div class="form-field full-span">
              <label class="label" for="reviewComment">Comentario de revisión</label>
              <textarea
                id="reviewComment"
                v-model.trim="reviewComment"
                class="form-control"
                rows="4"
                placeholder="Observación opcional antes de aprobar o rechazar..."
                :disabled="reviewing"
              ></textarea>
            </div>

            <div class="actions-row mt-3">
              <button
                class="primary-btn"
                :disabled="reviewing || !canApprove"
                @click="approveDocumentAction"
              >
                {{ reviewing ? 'Procesando...' : 'Aprobar y enviar correo' }}
              </button>

              <button
                class="secondary-btn danger-btn"
                :disabled="reviewing"
                @click="rejectDocumentAction"
              >
                Rechazar
              </button>
            </div>

            <div v-if="!canApprove" class="state-box error mt-3 mb-0">
              Solo se pueden aprobar documentos con resultado APTO o NO APTO.
              Si está pendiente, revisa el parser o el PDF cargado.
            </div>
          </div>
        </div>
      </div>

      <div v-if="analysisData" class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Texto extraído</h2>
              <p class="helper-text mb-0">
                Contenido crudo obtenido del PDF para revisión interna.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <textarea
            :value="analysisData.extractedText || ''"
            rows="18"
            class="form-control"
            readonly
          ></textarea>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  analyzeDocument,
  approveDocument,
  deleteDocument,
  getDocumentById,
  rejectDocument
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
const successMessage = ref('')
const analysisWarning = ref('')
const reviewComment = ref('')

const documentData = ref(null)
const analysisData = ref(null)
const employeeData = ref(null)

const documentId = computed(() => route.params.id)
const extractedFields = computed(() => analysisData.value?.extractedFields || {})

const normalizedResultStatus = computed(() => {
  const result = String(analysisData.value?.resultStatus || '').toUpperCase().trim()

  if (result === 'APTO') return 'APTO'
  if (result === 'NO_APTO' || result === 'NO APTO') return 'NO_APTO'

  return 'PENDIENTE'
})

const canApprove = computed(() => {
  return normalizedResultStatus.value === 'APTO' || normalizedResultStatus.value === 'NO_APTO'
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

const formatDate = (value) => {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const reviewLabel = (status) => {
  if (status === 'PENDING_REVIEW') return 'PENDIENTE DE REVISIÓN'
  if (status === 'APPROVED') return 'APROBADO'
  if (status === 'REJECTED') return 'RECHAZADO'
  return status || 'PENDIENTE DE REVISIÓN'
}

const notificationLabel = (status) => {
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  if (status === 'SENT') return 'ENVIADO'
  if (status === 'FAILED') return 'FALLÓ'
  if (status === 'SKIPPED') return 'OMITIDO'
  return status || 'NO ENVIADO'
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

const loadDetail = async () => {
  try {
    loading.value = true
    error.value = ''
    successMessage.value = ''
    analysisWarning.value = ''

    const documentResponse = await getDocumentById(documentId.value)
    documentData.value = documentResponse.data || null

    await loadEmployee()
    await loadAnalysis()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar el detalle del documento.'
    console.error('Error cargando detalle del documento:', err)
  } finally {
    loading.value = false
  }
}

const analyzeDocumentAction = async () => {
  try {
    analyzing.value = true
    error.value = ''
    successMessage.value = ''

    await analyzeDocument(documentId.value)

    successMessage.value = 'El análisis se ejecutó correctamente. El documento queda pendiente de revisión.'
    await loadDetail()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo analizar el documento.'
    console.error('Error analizando documento:', err)
  } finally {
    analyzing.value = false
  }
}

const approveDocumentAction = async () => {
  const confirmed = window.confirm(
    '¿Seguro que quieres aprobar este documento y enviar la notificación automática?'
  )

  if (!confirmed) return

  try {
    reviewing.value = true
    error.value = ''
    successMessage.value = ''

    await approveDocument(documentId.value, reviewComment.value)

    successMessage.value = 'Documento aprobado. El backend intentó enviar la notificación automática.'
    reviewComment.value = ''

    await loadDetail()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo aprobar y notificar el documento.'
    console.error('Error aprobando documento:', err)
  } finally {
    reviewing.value = false
  }
}

const rejectDocumentAction = async () => {
  const confirmed = window.confirm('¿Seguro que quieres rechazar este documento?')
  if (!confirmed) return

  try {
    reviewing.value = true
    error.value = ''
    successMessage.value = ''

    await rejectDocument(documentId.value, reviewComment.value)

    successMessage.value = 'Documento rechazado correctamente.'
    reviewComment.value = ''

    await loadDetail()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo rechazar el documento.'
    console.error('Error rechazando documento:', err)
  } finally {
    reviewing.value = false
  }
}

const deleteDocumentAction = async () => {
  const confirmed = window.confirm('¿Seguro que quieres eliminar este documento? Esta acción no se puede deshacer.')
  if (!confirmed) return

  try {
    deleting.value = true
    error.value = ''

    await deleteDocument(documentId.value)
    await router.push('/documents')
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo eliminar el documento.'
    console.error('Error eliminando documento:', err)
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.detail-title {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 800;
  color: var(--text);
}

.detail-result-box {
  display: flex;
  align-items: center;
}

.detail-result-pill {
  min-height: 40px;
  padding-inline: 1rem;
  font-size: 0.9rem;
}

.fields-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.full-span {
  grid-column: 1 / -1;
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

@media (max-width: 767.98px) {
  .detail-hero {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>