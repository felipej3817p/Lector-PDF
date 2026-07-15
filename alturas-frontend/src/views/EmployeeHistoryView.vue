<template>
  <section class="page employee-history-page">
    <div class="dashboard-toolbar compact-toolbar">
      <div>
        <span class="mini-title">Historial del trabajador</span>
        <h1 class="page-title mb-1">{{ employeeName }}</h1>
        <p class="p mb-0">
          Consulta evaluaciones médicas registradas del trabajador.
        </p>
      </div>

      <div class="header-actions compact-actions">
        <button type="button" class="secondary-btn" :disabled="loading" @click="loadData">
          {{ loading ? 'Actualizando...' : 'Actualizar' }}
        </button>

        <RouterLink to="/employees" class="secondary-btn">
          Volver a trabajadores
        </RouterLink>
      </div>
    </div>

    <div v-if="loading" class="state-box info">
      Cargando historial del trabajador...
    </div>

    <div v-else-if="error" class="state-box error">
      {{ error }}
    </div>

    <template v-else>
      <div class="card border-0 compact-card">
        <div class="card-body">
          <div class="worker-summary">
            <div>
              <span class="mini-title">Datos principales</span>
              <h2>{{ employeeName }}</h2>
              <p class="helper-text mb-0">
                {{ employeeDocumentLabel }}
                <span v-if="employeePosition"> · {{ employeePosition }}</span>
                <span v-if="employeeZone"> · {{ employeeZone }}</span>
              </p>
            </div>

            <div class="status-row">
              <span :class="employeeActive ? 'status-pill-active' : 'status-pill-neutral'">
                {{ employeeActive ? 'ACTIVO' : 'INACTIVO' }}
              </span>

              <span class="status-pill-neutral">
                {{ evaluations.length }} evaluación{{ evaluations.length === 1 ? '' : 'es' }}
              </span>
            </div>
          </div>

          <div class="compact-divider"></div>

          <div class="summary-grid">
            <div class="summary-item">
              <span>Correo</span>
              <strong>{{ employeeEmail || '-' }}</strong>
            </div>

            <div class="summary-item">
              <span>Cargo</span>
              <strong>{{ employeePosition || '-' }}</strong>
            </div>

            <div class="summary-item">
              <span>Zona o area</span>
              <strong>{{ employeeAreaCode || '-' }}</strong>
            </div>

            <div class="summary-item">
              <span>Nivel Educativo</span>
              <strong>{{ employeeEducationalLevel || '-' }}</strong>
            </div>

            <div class="summary-item">
              <span>Ultima evaluacion</span>
              <strong>{{ latestEvaluationDate ? formatDateLocal(latestEvaluationDate) : '-' }}</strong>
            </div>
          </div>
        </div>
      </div>

      <div class="card border-0 compact-card">
        <div class="card-body">
          <div class="history-section-tabs">
            <button
              type="button"
              class="section-tab"
              :class="{ active: historyViewMode === 'evaluations' }"
              @click="setHistoryViewMode('evaluations')"
            >
              Evaluaciones
              <span>{{ normalEvaluations.length }}</span>
            </button>

            <button
              type="button"
              class="section-tab"
              :class="{ active: historyViewMode === 'historical' }"
              @click="setHistoryViewMode('historical')"
            >
              Historial
              <span>{{ historicalEvaluations.length }}</span>
            </button>
          </div>

          <div class="compact-divider"></div>

          <div class="section-header">
            <div>
              <div class="section-title-with-count">
                <h2 class="h4 mb-1">{{ historySectionTitle }}</h2>
                <span class="section-count-pill">
                  {{ filteredEvaluations.length }} registro{{ filteredEvaluations.length === 1 ? '' : 's' }}
                </span>
              </div>

              <p class="helper-text mb-0">
                Historial de conceptos médicos cargados para este trabajador.
              </p>
            </div>

            <div class="pagination-size">
              <label class="label" for="evaluationPageSize">Mostrar</label>
              <select id="evaluationPageSize" v-model.number="evaluationPageSize" class="form-select">
                <option :value="10">10</option>
                <option :value="15">15</option>
                <option :value="25">25</option>
                <option :value="50">50</option>
              </select>
            </div>
          </div>

          <div class="compact-divider"></div>

          <!-- Filtros por fecha de evaluación del PDF -->
          <div class="eval-filters">
            <div class="form-field">
              <label class="label" for="evalFrom">Evaluación desde</label>
              <input
                id="evalFrom"
                v-model="evalFilterFrom"
                type="date"
                class="form-control"
              />
            </div>

            <div class="form-field">
              <label class="label" for="evalTo">Evaluación hasta</label>
              <input
                id="evalTo"
                v-model="evalFilterTo"
                type="date"
                class="form-control"
              />
            </div>

            <div class="form-field">
              <label class="label" for="evalResult">Resultado</label>
              <select id="evalResult" v-model="evalFilterResult" class="form-select">
                <option value="">Todos</option>
                <option value="APTO">APTO</option>
                <option value="NO_APTO">NO APTO</option>
                <option value="PENDIENTE">PENDIENTE</option>
              </select>
            </div>

            <button type="button" class="secondary-btn filter-clear-btn" @click="clearEvalFilters">
              Limpiar
            </button>
          </div>

          <div class="compact-divider"></div>

          <div v-if="!filteredEvaluations.length" class="state-box mb-0">
            {{ filteredEmptyMessage }}
          </div>

          <template v-else>
            <div class="table-fit-wrapper">
              <table class="table table-hover align-middle history-table">
                <colgroup>
                  <col class="col-date" />
                  <col class="col-upload-date" />
                  <col class="col-file" :style="isViewerOnly ? 'width: 38%' : ''" />
                  <col class="col-result" />
                  <col v-if="showWorkflowColumns" class="col-review" />
                  <col v-if="showWorkflowColumns" class="col-notification" />
                  <col v-if="showWorkflowColumns" class="col-reviewed" />
                  <col v-if="!isViewerOnly" class="col-actions" />
                </colgroup>

                <thead>
                  <tr>
                    <th>Fecha evaluación</th>
                    <th>Fecha carga</th>
                    <th>Archivo</th>
                    <th>Resultado</th>
                    <th v-if="showWorkflowColumns">Revision</th>
                    <th v-if="showWorkflowColumns">Correo</th>
                    <th v-if="showWorkflowColumns">Revisado por</th>
                    <th v-if="!isViewerOnly" class="text-center">Acciones</th>
                  </tr>
                </thead>

                <tbody>
                  <tr
                    v-for="item in paginatedEvaluations"
                    :key="item.id"
                    :class="{ 'row-critical': item.resultStatus === 'NO_APTO' }"
                  >
                    <td>
                      <span v-if="item.fechaConcepto" class="concept-date-pill">
                        {{ formatDateLocal(item.fechaConcepto) }}
                      </span>
                      <span v-else class="text-muted">-</span>
                    </td>

                    <td>
                      <div class="date-stack">
                        <strong>{{ formatDateOnly(item.uploadedAt) }}</strong>
                        <small>{{ formatTimeOnly(item.uploadedAt) }}</small>
                      </div>
                    </td>

                    <td>
                      <strong class="file-name">{{ item.originalFileName || '-' }}</strong>
                    </td>

                    <td>
                      <span :class="resultClass(item.resultStatus)">
                        {{ resultLabel(item.resultStatus) }}
                      </span>
                    </td>

                    <td v-if="showWorkflowColumns">
                      <span :class="reviewClass(item.reviewStatus)">
                        {{ reviewLabel(item.reviewStatus) }}
                      </span>
                    </td>

                    <td v-if="showWorkflowColumns">
                      <span :class="notificationClass(item.notificationStatus)">
                        {{ notificationLabel(item.notificationStatus) }}
                      </span>
                    </td>

                    <td v-if="showWorkflowColumns">{{ item.reviewedBy || '-' }}</td>

                    <td v-if="!isViewerOnly" class="text-center actions-cell">
                      <details
                        class="row-actions-menu history-actions-menu"
                        @toggle="closeOtherActionMenus"
                      >
                        <summary>Acciones</summary>

                        <div class="row-actions-menu__content" @click="closeAllActionMenus">
                          <button type="button" @click="openEvaluationDetail(item)">
                            Ver detalle
                          </button>

                          <button
                            v-if="canAnalyzeHistoricalResult && item.historical && item.resultStatus === 'PENDIENTE'"
                            type="button"
                            @click="analyzeHistoricalResult(item)"
                          >
                            Evaluar resultado
                          </button>

                          <button
                            v-if="canOpenDocumentFiles"
                            type="button"
                            @click="viewPdf(item.id)"
                          >
                            Ver PDF
                          </button>

                          <RouterLink v-if="canOpenDocumentFiles" :to="`/documents/${item.id}`">
                            Evaluacion
                          </RouterLink>

                          <button
                            v-if="auth.isAdmin || auth.isSuperAdmin || auth.isOperator"
                            type="button"
                            class="danger-menu-item"
                            @click="deleteEvaluationDocument(item)"
                          >
                            Eliminar PDF
                          </button>
                        </div>
                      </details>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="pagination-bar google-pagination">
              <div class="pagination-info">
                Mostrando
                <strong>{{ evaluationPageStart }}</strong>
                -
                <strong>{{ evaluationPageEnd }}</strong>
                de
                <strong>{{ filteredEvaluations.length }}</strong>
              </div>

              <div class="pagination-actions">
                <button
                  type="button"
                  class="page-nav"
                  :disabled="evaluationCurrentPage === 1"
                  @click="goToEvaluationPage(evaluationCurrentPage - 1)"
                >
                  Anterior
                </button>

                <template v-for="(item, index) in visibleEvaluationPageItems" :key="`${item}-${index}`">
                  <span v-if="item === '...'" class="page-ellipsis">...</span>

                  <button
                    v-else
                    type="button"
                    class="page-number"
                    :class="{ active: item === evaluationCurrentPage }"
                    @click="goToEvaluationPage(item)"
                  >
                    {{ item }}
                  </button>
                </template>

                <button
                  type="button"
                  class="page-nav"
                  :disabled="evaluationCurrentPage === evaluationTotalPages"
                  @click="goToEvaluationPage(evaluationCurrentPage + 1)"
                >
                  Siguiente
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>

      <TrainingCertificatesPanel v-if="canSeeCertificates" :employee-id="employeeId" />

      <div v-if="selectedEvaluation" class="history-modal-backdrop" @click.self="closeEvaluationDetail">
        <article class="history-modal">
          <header class="history-modal__header">
            <div>
              <span class="mini-title">Detalle de evaluación</span>
              <h2>{{ selectedEvaluation.originalFileName || 'Evaluación registrada' }}</h2>
              <p class="helper-text mb-0">
                Información resumida de la evaluación seleccionada.
              </p>
            </div>

            <button type="button" class="secondary-btn" @click="closeEvaluationDetail">
              Cerrar
            </button>
          </header>

          <div class="compact-divider"></div>

          <div class="detail-grid">
            <div class="detail-field">
              <span>Resultado</span>
              <strong>
                <span :class="resultClass(selectedEvaluation.resultStatus)">
                  {{ resultLabel(selectedEvaluation.resultStatus) }}
                </span>
              </strong>
            </div>

            <div v-if="canViewWorkflowDetails && !selectedEvaluation.historical" class="detail-field">
              <span>Revision</span>
              <strong>
                <span :class="reviewClass(selectedEvaluation.reviewStatus)">
                  {{ reviewLabel(selectedEvaluation.reviewStatus) }}
                </span>
              </strong>
            </div>

            <div v-if="canViewWorkflowDetails && !selectedEvaluation.historical" class="detail-field">
              <span>Correo</span>
              <strong>
                <span :class="notificationClass(selectedEvaluation.notificationStatus)">
                  {{ notificationLabel(selectedEvaluation.notificationStatus) }}
                </span>
              </strong>
            </div>

            <div class="detail-field">
              <span>Fecha de evaluación</span>
              <strong>{{ formatDateLocal(selectedEvaluation.fechaConcepto) }}</strong>
            </div>

            <div class="detail-field">
              <span>Fecha de carga</span>
              <strong>{{ formatDate(selectedEvaluation.uploadedAt) }}</strong>
            </div>

            <div v-if="canViewWorkflowDetails && !selectedEvaluation.historical" class="detail-field">
              <span>Fecha de revision</span>
              <strong>{{ formatDate(selectedEvaluation.reviewedAt) }}</strong>
            </div>

            <div v-if="canViewWorkflowDetails && !selectedEvaluation.historical" class="detail-field">
              <span>Revisado por</span>
              <strong>{{ selectedEvaluation.reviewedBy || '-' }}</strong>
            </div>

            <div class="detail-field full-span">
              <span>Archivo</span>
              <strong>{{ selectedEvaluation.originalFileName || '-' }}</strong>
            </div>

            <div v-if="canViewWorkflowDetails && !selectedEvaluation.historical" class="detail-field full-span">
              <span>Comentario revision</span>
              <strong>{{ selectedEvaluation.reviewComment || '-' }}</strong>
            </div>
          </div>

          <div class="compact-divider"></div>

          <footer class="history-modal__footer">
            <button type="button" class="secondary-btn" @click="closeEvaluationDetail">
              Cerrar
            </button>

            <RouterLink v-if="canOpenDocumentFiles" :to="`/documents/${selectedEvaluation.id}`" class="primary-btn">
              Abrir evaluación
            </RouterLink>
          </footer>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { analyzeDocument, deleteDocument } from '../api/document'
import { getEmployeeHistory } from '../api/employeeHistory'
import http from '../api/http'
import TrainingCertificatesPanel from '../components/TrainingCertificatesPanel.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const loading = ref(false)
const error = ref('')

const employee = ref(null)
const historyItems = ref([])
const selectedEvaluation = ref(null)

const evaluationPageSize = ref(10)
const evaluationCurrentPage = ref(1)
const historyViewMode = ref('evaluations')

// ─── Filtros de evaluaciones ─────────────────────────────────────────────────
const evalFilterFrom = ref('')
const evalFilterTo = ref('')
const evalFilterResult = ref('')

const clearEvalFilters = () => {
  evalFilterFrom.value = ''
  evalFilterTo.value = ''
  evalFilterResult.value = ''
  evaluationCurrentPage.value = 1
}

const setHistoryViewMode = (mode) => {
  historyViewMode.value = mode === 'historical' ? 'historical' : 'evaluations'
  evaluationCurrentPage.value = 1
  closeAllActionMenus()
}

const employeeId = computed(() => String(route.params.id || route.params.employeeId || ''))

const hasRole = (roleName) => {
  const target = String(roleName || '').toUpperCase()

  const directRole = String(
    auth.user?.role ||
    auth.currentUser?.role ||
    auth.profile?.role ||
    ''
  ).toUpperCase()

  const roles = [
    ...(Array.isArray(auth.user?.roles) ? auth.user.roles : []),
    ...(Array.isArray(auth.currentUser?.roles) ? auth.currentUser.roles : []),
    ...(Array.isArray(auth.profile?.roles) ? auth.profile.roles : [])
  ].map((role) => String(role || '').toUpperCase())

  return directRole === target || roles.includes(target)
}

const isAdminLike = computed(() => {
  return (
    auth.isAdmin ||
    auth.isSuperAdmin ||
    hasRole('ADMIN') ||
    hasRole('SUPER_ADMIN')
  )
})

const isApproverOnly = computed(() => {
  return (
    (auth.isApprover || hasRole('APROBADOR') || hasRole('APPROVER')) &&
    !isAdminLike.value
  )
})

const isViewerOnly = computed(() => {
  return (
    (auth.isViewer || hasRole('VISUALIZADOR') || hasRole('VIEWER')) &&
    !isAdminLike.value &&
    !auth.isApprover &&
    !hasRole('APROBADOR') &&
    !hasRole('APPROVER')
  )
})

const canSeeCertificates = computed(() => {
  return !isApproverOnly.value
})

const canViewWorkflowDetails = computed(() => !isViewerOnly.value)

const canOpenDocumentFiles = computed(() => !isViewerOnly.value)

const showWorkflowColumns = computed(() => {
  return canViewWorkflowDetails.value && historyViewMode.value !== 'historical'
})

const canAnalyzeHistoricalResult = computed(() => {
  return !isViewerOnly.value && auth.canUploadDocuments
})

const employeeName = computed(() => {
  const current = employee.value || {}

  return [
    current.firstName,
    current.secondName,
    current.firstLastName,
    current.secondLastName
  ]
    .filter(Boolean)
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim() || 'Trabajador'
})

const employeeDocumentLabel = computed(() => {
  const current = employee.value || {}

  return [
    current.documentType,
    current.documentNumber
  ]
    .filter(Boolean)
    .join(' ')
    .trim() || '-'
})

const employeeEmail = computed(() => employee.value?.email || '')
const employeePosition = computed(() => employee.value?.currentPosition || '')
const employeeAreaCode = computed(() => employee.value?.areaCode || '')
const employeeEducationalLevel = computed(() => employee.value?.educationalLevel || '')
const employeeActive = computed(() => employee.value?.active ?? true)
const employeeCurrentlyActive = computed(() => employee.value?.currentlyActive ?? true)

const resolveEvaluationDate = (document, analysis = {}) => {
  const fields = analysis.extractedFields || {}

  return (
    document.fechaEvaluacion ||
    document.evaluationDate ||
    document.fechaConcepto ||
    document.conceptDate ||
    analysis.fechaEvaluacion ||
    analysis.evaluationDate ||
    analysis.analysisEvaluationDate ||
    analysis.fechaConcepto ||
    analysis.conceptDate ||
    fields.fechaEvaluacion ||
    fields.evaluationDate ||
    fields.fechaConcepto ||
    fields.conceptDate ||
    null
  )
}

const isHistoricalDocument = (document) => {
  const status = String(document?.processingStatus || '').toUpperCase()
  const comment = String(document?.reviewComment || '').toLowerCase()
  const error = String(document?.notificationError || '').toLowerCase()

  return document?.historical === true ||
    status === 'STORED' ||
    comment.includes('carga historica') ||
    error.includes('carga historica')
}

const evaluations = computed(() => {
  return historyItems.value
    .map((document) => {
      const analysis = document.analysis || {}
      const historical = isHistoricalDocument(document)

      return {
        ...document,
        id: document.id || document.documentId,
        historical,
        resultStatus: normalizeResultStatus(document.resultStatus || analysis.resultStatus),
        fechaConcepto: resolveEvaluationDate(document, analysis),
        analysis,
        uploadedAt: document.uploadedAt || '',
        reviewedAt: document.reviewedAt || '',
        reviewStatus: document.reviewStatus || (historical ? 'NOT_PENDING' : 'PENDING_REVIEW'),
        notificationStatus: document.notificationStatus || 'NOT_PENDING'
      }
    })
    .sort((a, b) => parseDate(b.uploadedAt) - parseDate(a.uploadedAt))
})

const normalEvaluations = computed(() => evaluations.value.filter((item) => !item.historical))

const historicalEvaluations = computed(() => evaluations.value.filter((item) => item.historical))

const visibleEvaluations = computed(() => {
  return historyViewMode.value === 'historical'
    ? historicalEvaluations.value
    : normalEvaluations.value
})

const historySectionTitle = computed(() => {
  return historyViewMode.value === 'historical' ? 'Historial cargado' : 'Evaluaciones registradas'
})

const emptySectionMessage = computed(() => {
  return historyViewMode.value === 'historical'
    ? 'Este trabajador no tiene PDFs historicos cargados.'
    : 'Este trabajador no tiene evaluaciones registradas.'
})

const filteredEmptyMessage = computed(() => {
  if (!evaluations.value.length) return 'Este trabajador no tiene historial registrado.'
  return 'Ninguna evaluacion coincide con los filtros aplicados.'
})

const latestEvaluationDate = computed(() => {
  return normalEvaluations.value.find((item) => item.fechaConcepto)?.fechaConcepto ||
    employee.value?.latestFechaEvaluacion ||
    employee.value?.latestEvaluationDate ||
    employee.value?.latestFechaConcepto ||
    null
})

// Evaluaciones filtradas por fecha de evaluación y resultado
const filteredEvaluations = computed(() => {
  return visibleEvaluations.value.filter((item) => {
    // Filtro resultado
    if (evalFilterResult.value) {
      if (item.resultStatus !== evalFilterResult.value) return false
    }

    // Filtro fecha de evaluación
    const fc = item.fechaConcepto
    if (evalFilterFrom.value || evalFilterTo.value) {
      if (!fc) return false
      if (evalFilterFrom.value && fc < evalFilterFrom.value) return false
      if (evalFilterTo.value && fc > evalFilterTo.value) return false
    }

    return true
  })
})

const evaluationTotalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredEvaluations.value.length / evaluationPageSize.value))
})

const paginatedEvaluations = computed(() => {
  const start = (evaluationCurrentPage.value - 1) * evaluationPageSize.value
  const end = start + evaluationPageSize.value

  return filteredEvaluations.value.slice(start, end)
})

const evaluationPageStart = computed(() => {
  if (!filteredEvaluations.value.length) return 0
  return (evaluationCurrentPage.value - 1) * evaluationPageSize.value + 1
})

const evaluationPageEnd = computed(() => {
  return Math.min(evaluationCurrentPage.value * evaluationPageSize.value, filteredEvaluations.value.length)
})

const visibleEvaluationPageItems = computed(() => {
  const total = evaluationTotalPages.value
  const current = evaluationCurrentPage.value

  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1)
  }

  const pages = new Set([1, total, current, current - 1, current + 1])

  if (current <= 3) {
    pages.add(2)
    pages.add(3)
    pages.add(4)
  }

  if (current >= total - 2) {
    pages.add(total - 1)
    pages.add(total - 2)
    pages.add(total - 3)
  }

  const sorted = [...pages]
    .filter((page) => page >= 1 && page <= total)
    .sort((a, b) => a - b)

  const result = []

  sorted.forEach((page, index) => {
    if (index > 0 && page - sorted[index - 1] > 1) {
      result.push('...')
    }

    result.push(page)
  })

  return result
})

const goToEvaluationPage = (page) => {
  evaluationCurrentPage.value = Math.min(
    Math.max(Number(page) || 1, 1),
    evaluationTotalPages.value
  )
}

const parseDate = (value) => {
  if (!value) return 0

  const date = new Date(value)
  const time = date.getTime()

  return Number.isNaN(time) ? 0 : time
}

const formatDate = (value) => {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return String(value)
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

/**
 * Formats a LocalDate string (YYYY-MM-DD) without timezone conversion.
 * Used for fechaConcepto/evaluationDate which is already in local date format.
 */
const formatDateLocal = (value) => {
  if (!value) return '-'
  // value is YYYY-MM-DD string from backend LocalDate
  const [year, month, day] = String(value).split('-')
  if (!year || !month || !day) return String(value)
  return `${day}/${month}/${year}`
}

const formatDateOnly = (value) => {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  return date.toLocaleDateString('es-CO', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const formatTimeOnly = (value) => {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return '-'
  }

  return date.toLocaleTimeString('es-CO', {
    timeZone: 'America/Bogota',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const normalizeResultStatus = (value) => {
  const status = String(value || '').toUpperCase().trim()

  if (status === 'APTO') return 'APTO'
  if (status === 'NO_APTO' || status === 'NO APTO') return 'NO_APTO'

  return 'NO_APTO'
}

const resultLabel = (status) => {
  if (status === 'APTO') return 'APTO'
  if (status === 'NO_APTO') return 'NO APTO'
  return 'NO APTO'
}

const reviewLabel = (status) => {
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  if (status === 'APPROVED') return 'APROBADO'
  if (status === 'REJECTED') return 'REVISIÓN'
  if (status === 'PENDING_REVIEW') return 'PENDIENTE'
  return status || 'PENDIENTE'
}

const notificationLabel = (status) => {
  if (status === 'SENT') return 'ENVIADO'
  if (status === 'FAILED') return 'FALLÓ'
  if (status === 'SKIPPED') return 'OMITIDO'
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  return status || 'NO ENVIADO'
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

const openEvaluationDetail = (item) => {
  selectedEvaluation.value = item
}

const closeEvaluationDetail = () => {
  selectedEvaluation.value = null
}

const closeAllActionMenus = () => {
  document
    .querySelectorAll('.employee-history-page .row-actions-menu[open]')
    .forEach((menu) => {
      menu.removeAttribute('open')
    })
}

const closeOtherActionMenus = (event) => {
  const currentMenu = event.currentTarget

  if (!currentMenu.open) return

  document
    .querySelectorAll('.employee-history-page .row-actions-menu[open]')
    .forEach((menu) => {
      if (menu !== currentMenu) {
        menu.removeAttribute('open')
      }
    })
}

const closeActionMenusOnOutsideClick = (event) => {
  if (!event.target.closest('.row-actions-menu')) {
    closeAllActionMenus()
  }
}

const viewPdf = async (documentId) => {
  if (!documentId) return

  const pdfWindow = window.open('', '_blank')
  if (pdfWindow) {
    pdfWindow.document.write('Cargando PDF...')
  }

  try {
    const response = await http.get(`/api/documents/${documentId}/view`, {
      responseType: 'blob'
    })

    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)

    if (pdfWindow) {
      pdfWindow.location.href = url
    } else {
      window.open(url, '_blank')
    }

    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (err) {
    if (pdfWindow) pdfWindow.close()
    error.value = err?.response?.data?.message || 'No se pudo abrir el PDF.'
  }
}

const analyzeHistoricalResult = async (item) => {
  if (!canAnalyzeHistoricalResult.value || !item?.id) return

  try {
    error.value = ''
    await analyzeDocument(item.id)
    await loadDocuments()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo evaluar el resultado de este PDF.'
  }
}

const deleteEvaluationDocument = async (item) => {
  if (!(auth.isAdmin || auth.isSuperAdmin || auth.isOperator) || !item?.id) return

  const confirmed = window.confirm(`Seguro que deseas eliminar el PDF ${item.originalFileName || ''}? Esta accion borra el soporte y su analisis.`)
  if (!confirmed) return

  try {
    await deleteDocument(item.id)
    await loadDocuments()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo eliminar el PDF del historial.'
  }
}

const loadEmployee = async () => {
  const response = await http.get(`/api/employees/${employeeId.value}`)
  employee.value = response.data || null
}

const loadDocuments = async () => {
  const response = await getEmployeeHistory(employeeId.value)
  historyItems.value = Array.isArray(response.data) ? response.data : []
}

const loadData = async () => {
  try {
    loading.value = true
    error.value = ''
    selectedEvaluation.value = null

    await loadEmployee()
    await loadDocuments()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar el historial del trabajador.'
    console.error('Error cargando historial del trabajador:', err)
  } finally {
    loading.value = false
  }
}

watch(evaluationPageSize, () => {
  closeAllActionMenus()
  evaluationCurrentPage.value = 1
})

watch(evaluationTotalPages, (value) => {
  closeAllActionMenus()
  if (evaluationCurrentPage.value > value) {
    evaluationCurrentPage.value = value
  }
})

watch(employeeId, () => {
  closeAllActionMenus()
  evaluationCurrentPage.value = 1
})

onMounted(() => {
  loadData()
  window.addEventListener('click', closeActionMenusOnOutsideClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', closeActionMenusOnOutsideClick)
})
</script>

<style scoped>
.employee-history-page {
  gap: 0.8rem;
}

.compact-toolbar {
  padding: 1rem 1.15rem;
  min-height: auto;
}

.compact-toolbar .p {
  max-width: 620px;
  font-size: 0.93rem;
}

.page-title {
  margin: 0;
  font-size: clamp(1.35rem, 1.7vw, 1.8rem);
  line-height: 1.1;
  font-weight: 820;
  color: var(--text);
}

.compact-actions {
  gap: 0.55rem;
}

.compact-actions .primary-btn,
.compact-actions .secondary-btn {
  min-height: 36px;
  padding: 0.48rem 0.78rem;
  font-size: 0.82rem;
}

.compact-card .card-body {
  padding: 0.95rem 1.1rem;
}

.worker-summary,
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.85rem;
  flex-wrap: wrap;
}

.worker-summary h2 {
  margin: 0;
  color: var(--text);
  font-size: 1.18rem;
  font-weight: 820;
  line-height: 1.15;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.compact-divider {
  height: 1px;
  margin: 0.75rem 0;
  background: var(--border);
}

.history-section-tabs {
  display: inline-flex;
  gap: 0.4rem;
  padding: 0.25rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-soft);
}

.section-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-height: 34px;
  padding: 0.42rem 0.75rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 800;
}

.section-tab.active {
  background: var(--surface);
  color: var(--text);
  box-shadow: var(--shadow-sm);
}

.section-tab span {
  min-width: 22px;
  padding: 0.12rem 0.4rem;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 0.68rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 0.55rem;
}

.summary-item {
  padding: 0.6rem 0.7rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-soft);
}

.summary-item span {
  display: block;
  color: var(--text-muted);
  font-size: 0.62rem;
  font-weight: 780;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-item strong {
  display: block;
  margin-top: 0.24rem;
  color: var(--text);
  font-size: 0.78rem;
  line-height: 1.22;
  word-break: break-word;
}

.section-header h2 {
  font-size: 1.12rem;
}

.section-title-with-count {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.section-title-with-count h2 {
  margin: 0;
}

.section-count-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0.25rem 0.55rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-soft);
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 800;
  line-height: 1;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.pagination-size .label {
  margin: 0;
  font-size: 0.72rem;
}

.pagination-size .form-select {
  width: 88px;
  min-height: 38px;
  padding: 0.4rem 0.7rem;
}

.eval-filters {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(150px, 1fr) minmax(170px, 1fr) auto;
  align-items: end;
  gap: 0.65rem;
}

.eval-filters .form-field {
  min-width: 0;
}

.eval-filters .form-control,
.eval-filters .form-select {
  min-height: 38px;
}

.filter-clear-btn {
  min-height: 38px;
  white-space: nowrap;
}

.table-fit-wrapper {
  width: 100%;
  overflow: visible;
}

.history-table {
  width: 100%;
  table-layout: fixed;
  margin-bottom: 0;
}

.col-date {
  width: 11%;
}

.col-upload-date {
  width: 11%;
}

.col-file {
  width: 24%;
}

.col-result {
  width: 9%;
}

.col-review {
  width: 10%;
}

.col-notification {
  width: 11%;
}

.col-reviewed {
  width: 10%;
}

.col-actions {
  width: 14%;
}

.history-table th {
  padding: 0.62rem 0.45rem;
  color: var(--text-muted);
  font-size: 0.68rem;
  line-height: 1.1;
  white-space: normal;
}

.history-table td {
  padding: 0.62rem 0.45rem;
  font-size: 0.78rem;
  vertical-align: middle;
  word-break: normal;
  overflow-wrap: anywhere;
}

.history-table tr:has(.row-actions-menu[open]) {
  position: relative;
  z-index: 100;
}

.date-stack {
  display: flex;
  flex-direction: column;
  gap: 0.12rem;
}

.date-stack strong,
.file-name {
  color: var(--text);
  font-size: 0.8rem;
  line-height: 1.25;
}

.date-stack small {
  color: var(--text-muted);
  font-size: 0.72rem;
  line-height: 1.2;
}

.file-name {
  display: block;
  max-width: 100%;
  white-space: normal;
  word-break: normal;
  overflow-wrap: anywhere;
}

.inline-actions {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.tiny-btn {
  min-height: 28px;
  padding: 0.32rem 0.46rem;
  font-size: 0.68rem;
  white-space: nowrap;
}

.actions-cell {
  position: relative;
  overflow: visible;
}

.row-actions-menu {
  position: relative;
  display: inline-flex;
  justify-content: center;
  width: auto;
}

.row-actions-menu summary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0.32rem 0.48rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  font-size: 0.7rem;
  font-weight: 760;
  line-height: 1;
  cursor: pointer;
  list-style: none;
  user-select: none;
  white-space: nowrap;
}

.row-actions-menu summary::-webkit-details-marker {
  display: none;
}

.row-actions-menu summary::after {
  content: "";
  width: 0.38rem;
  height: 0.38rem;
  margin-left: 0.28rem;
  border-right: 1.6px solid var(--text-muted);
  border-bottom: 1.6px solid var(--text-muted);
  transform: rotate(45deg) translateY(-1px);
}

.row-actions-menu[open] summary {
  border-color: var(--primary);
}

.row-actions-menu__content {
  position: absolute;
  top: calc(100% + 0.3rem);
  right: 0;
  z-index: 200;
  width: max-content;
  min-width: 145px;
  max-width: 220px;
  padding: 0.3rem;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: var(--surface);
  box-shadow: var(--shadow-lg);
}

.row-actions-menu__content a,
.row-actions-menu__content button {
  display: block;
  width: 100%;
  padding: 0.48rem 0.58rem;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text);
  font-size: 0.74rem;
  font-weight: 700;
  line-height: 1.15;
  text-align: left;
  text-decoration: none;
  white-space: nowrap;
}

.row-actions-menu__content a:hover,
.row-actions-menu__content button:hover {
  background: var(--surface-soft);
}

.danger-menu-item {
  color: #991b1b !important;
}

.danger-menu-item:hover {
  background: #fee2e2 !important;
}

.row-critical {
  background: rgba(254, 242, 242, 0.65);
}

.google-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.9rem;
  padding-top: 0.85rem;
  border-top: 1px solid var(--border);
}

.pagination-info {
  color: var(--text-muted);
  font-size: 0.86rem;
}

.pagination-info strong {
  color: var(--text);
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.page-number,
.page-nav {
  min-width: 30px;
  min-height: 30px;
  padding: 0.28rem 0.5rem;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: var(--primary);
  font-size: 0.82rem;
  font-weight: 700;
}

.page-number:hover,
.page-nav:hover:not(:disabled) {
  background: var(--primary-soft);
}

.page-number.active {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--text);
}

.page-nav:disabled {
  color: var(--text-muted);
  cursor: not-allowed;
  opacity: 0.5;
}

.page-ellipsis {
  padding: 0 0.25rem;
  color: var(--text-muted);
}

.history-modal-backdrop {
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

.history-modal {
  width: min(760px, 100%);
  max-height: 90vh;
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-md);
}

.history-modal__header,
.history-modal__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.15rem;
}

.history-modal__header h2 {
  margin: 0;
  color: var(--text);
  font-size: 1.1rem;
  font-weight: 820;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.6rem;
  padding: 0 1.15rem;
}

.detail-field {
  padding: 0.65rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-soft);
}

.detail-field span {
  display: block;
  color: var(--text-muted);
  font-size: 0.62rem;
  font-weight: 780;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-field strong {
  display: block;
  margin-top: 0.25rem;
  color: var(--text);
  font-size: 0.78rem;
  line-height: 1.22;
  word-break: break-word;
}

.full-span {
  grid-column: 1 / -1;
}

.history-modal__footer {
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(130px, 1fr));
  }

  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .eval-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-clear-btn {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .compact-toolbar,
  .worker-summary,
  .section-header,
  .google-pagination,
  .history-modal__header,
  .history-modal__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid,
  .detail-grid,
  .eval-filters {
    grid-template-columns: 1fr;
  }

  .history-modal__footer {
    justify-content: stretch;
  }

  .history-modal__footer .primary-btn,
  .history-modal__footer .secondary-btn {
    width: 100%;
  }
}
</style>
