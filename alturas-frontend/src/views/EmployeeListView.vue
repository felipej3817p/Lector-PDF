<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Seguimiento de trabajadores</span>
        <h1 class="page-title mb-2">Trabajadores</h1>
        <p class="p mb-0">
          Consulta el estado actual de cada trabajador: último resultado, revisión SSA y notificación.
        </p>
      </div>

      <div class="header-actions">
        <button
          type="button"
          class="secondary-btn"
          :disabled="loading"
          @click="loadData"
        >
          {{ loading ? 'Actualizando...' : 'Actualizar' }}
        </button>

        <RouterLink to="/documents/upload" class="primary-btn">
          Cargar evaluaciones
        </RouterLink>

        <RouterLink
          v-if="!auth.isApprover"
          to="/employees/new"
          class="secondary-btn"
        >
          Nuevo trabajador
        </RouterLink>
      </div>
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span class="label">Trabajadores</span>
        <strong class="summary-value">{{ employees.length }}</strong>
        <span class="summary-meta">Registrados en tu alcance</span>
      </div>

      <div class="summary-card">
        <span class="label">Aptos</span>
        <strong class="summary-value">{{ aptCount }}</strong>
        <span class="summary-meta">Último concepto APTO</span>
      </div>

      <div class="summary-card">
        <span class="label">No aptos</span>
        <strong class="summary-value">{{ notAptCount }}</strong>
        <span class="summary-meta">Último concepto NO APTO</span>
      </div>

      <div class="summary-card">
        <span class="label">Pendientes SSA</span>
        <strong class="summary-value">{{ pendingReviewCount }}</strong>
        <span class="summary-meta">En espera de revisión</span>
      </div>

      <div class="summary-card">
        <span class="label">Correo fallido</span>
        <strong class="summary-value">{{ failedNotificationCount }}</strong>
        <span class="summary-meta">Aprobados sin notificar</span>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Filtros</h2>
            <p class="helper-text mb-0">
              Busca por cédula, nombre, zona, cargo, resultado o estado de revisión.
            </p>
          </div>

          <div class="header-actions">
            <button
              type="button"
              class="secondary-btn"
              :disabled="loading"
              @click="resetFilters"
            >
              Limpiar filtros
            </button>
          </div>
        </div>

        <div class="hr"></div>

        <div class="filter-chip-group">
          <button
            type="button"
            class="filter-chip"
            :class="{ active: resultFilter === '' }"
            @click="resultFilter = ''"
          >
            Todos
          </button>

          <button
            type="button"
            class="filter-chip success"
            :class="{ active: resultFilter === 'APTO' }"
            @click="resultFilter = 'APTO'"
          >
            Aptos
          </button>

          <button
            type="button"
            class="filter-chip danger"
            :class="{ active: resultFilter === 'NO_APTO' }"
            @click="resultFilter = 'NO_APTO'"
          >
            No aptos
          </button>

          <button
            type="button"
            class="filter-chip"
            :class="{ active: reviewFilter === 'PENDING_REVIEW' }"
            @click="reviewFilter = reviewFilter === 'PENDING_REVIEW' ? '' : 'PENDING_REVIEW'"
          >
            Pendientes SSA
          </button>

          <button
            type="button"
            class="filter-chip danger"
            :class="{ active: notificationFilter === 'FAILED' }"
            @click="notificationFilter = notificationFilter === 'FAILED' ? '' : 'FAILED'"
          >
            Correo fallido
          </button>
        </div>

        <div class="hr"></div>

        <div class="filters-grid">
          <div class="form-field">
            <label class="label" for="search">Buscar</label>
            <input
              id="search"
              v-model.trim="search"
              type="text"
              class="form-control"
              placeholder="Nombre, cédula, cargo, zona..."
              :disabled="loading"
            />
          </div>

          <div class="form-field">
            <label class="label" for="areaFilter">Zona / área</label>
            <select
              id="areaFilter"
              v-model="areaFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todas</option>
              <option
                v-for="area in areaOptions"
                :key="area"
                :value="area"
              >
                {{ area }}
              </option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="resultFilter">Último resultado</label>
            <select
              id="resultFilter"
              v-model="resultFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todos</option>
              <option value="APTO">APTO</option>
              <option value="NO_APTO">NO APTO</option>
              <option value="PENDIENTE">PENDIENTE</option>
              <option value="SIN_EVALUACION">SIN EVALUACIÓN</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="reviewFilter">Revisión SSA</label>
            <select
              id="reviewFilter"
              v-model="reviewFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todas</option>
              <option value="PENDING_REVIEW">PENDIENTE</option>
              <option value="APPROVED">APROBADO</option>
              <option value="REJECTED">RECHAZADO</option>
              <option value="SIN_REVISION">SIN REVISIÓN</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="notificationFilter">Notificación</label>
            <select
              id="notificationFilter"
              v-model="notificationFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todas</option>
              <option value="NOT_PENDING">NO ENVIADO</option>
              <option value="SENT">ENVIADO</option>
              <option value="FAILED">FALLÓ</option>
              <option value="SKIPPED">OMITIDO</option>
              <option value="SIN_NOTIFICACION">SIN NOTIFICACIÓN</option>
            </select>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="state-box info">
      Cargando trabajadores y evaluaciones...
    </div>

    <div v-else-if="!employees.length" class="state-box">
      No hay trabajadores registrados en tu alcance actual.
    </div>

    <div v-else class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Estado por trabajador</h2>
            <p class="helper-text mb-0">
              Vista principal del seguimiento. Usa la barra superior para desplazarte horizontalmente.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div class="table-scroll-wrapper">
          <div
            ref="topScroll"
            class="table-scroll-top"
            @scroll="syncBodyScroll"
          >
            <div class="table-scroll-spacer"></div>
          </div>

          <div
            ref="bodyScroll"
            class="table-responsive table-scroll-body"
            @scroll="syncTopScroll"
          >
            <table class="table table-hover align-middle tracking-table">
              <thead>
                <tr>
                  <th>Cédula</th>
                  <th>Trabajador</th>
                  <th>Zona</th>
                  <th>Cargo</th>
                  <th>Correo</th>
                  <th>Último resultado</th>
                  <th>Revisión SSA</th>
                  <th>Notificación</th>
                  <th>Última fecha</th>
                  <th class="text-center">Acción</th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="row in filteredRows"
                  :key="row.employee.id"
                  :class="{
                    'row-critical': row.resultStatus === 'NO_APTO',
                    'row-pending': row.reviewStatus === 'PENDING_REVIEW'
                  }"
                >
                  <td>
                    <strong>{{ documentLabel(row.employee) }}</strong>
                  </td>

                  <td>
                    <div class="person-meta">
                      <strong>{{ fullName(row.employee) }}</strong>
                      <small>{{ row.employee.employer || 'Sin empleador' }}</small>
                    </div>
                  </td>

                  <td>
                    <div class="person-meta">
                      <strong>{{ row.employee.areaCode || '-' }}</strong>
                      <small>{{ row.employee.zone || row.employee.workArea || '-' }}</small>
                    </div>
                  </td>

                  <td>
                    <span :title="row.employee.currentPosition || '-'">
                      {{ row.employee.currentPosition || '-' }}
                    </span>
                  </td>

                  <td>
                    <span
                      v-if="row.employee.email"
                      :title="row.employee.email"
                    >
                      {{ row.employee.email }}
                    </span>

                    <span v-else class="muted-text">
                      Sin correo
                    </span>
                  </td>

                  <td>
                    <span :class="resultClass(row.resultStatus)">
                      {{ resultLabel(row.resultStatus) }}
                    </span>
                  </td>

                  <td>
                    <span :class="reviewClass(row.reviewStatus)">
                      {{ reviewLabel(row.reviewStatus) }}
                    </span>
                  </td>

                  <td>
                    <span :class="notificationClass(row.notificationStatus)">
                      {{ notificationLabel(row.notificationStatus) }}
                    </span>
                  </td>

                  <td>{{ formatDate(row.uploadedAt) }}</td>

                  <td>
                    <div class="actions justify-content-center">
                      <RouterLink
                        v-if="row.documentId"
                        :to="`/documents/${row.documentId}`"
                        class="secondary-btn"
                      >
                        Ver evaluación
                      </RouterLink>

                      <RouterLink
                        v-else
                        to="/documents/upload"
                        class="secondary-btn"
                      >
                        Cargar
                      </RouterLink>

                      <RouterLink
                        v-if="auth.isSuperAdmin"
                        :to="`/employees/${row.employee.id}/edit`"
                        class="secondary-btn"
                      >
                        Editar
                      </RouterLink>
                    </div>
                  </td>
                </tr>

                <tr v-if="!filteredRows.length">
                  <td colspan="10">
                    <div class="state-box m-2">
                      No hay trabajadores que coincidan con los filtros seleccionados.
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="card border-0 mt-4">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Lectura rápida</h2>
            <p class="helper-text mb-0">
              Interpretación de los estados principales.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div class="legend-grid">
          <div class="legend-item">
            <span class="status-pill-active">APTO</span>
            <p>El último concepto indica que el trabajador está apto para trabajo en alturas.</p>
          </div>

          <div class="legend-item">
            <span class="status-pill-inactive">NO APTO</span>
            <p>El último concepto indica restricción o no aptitud. Requiere atención.</p>
          </div>

          <div class="legend-item">
            <span class="status-pill-warning">PENDIENTE</span>
            <p>El documento fue leído, pero está pendiente de revisión por SSA.</p>
          </div>

          <div class="legend-item">
            <span class="status-pill-neutral">SIN EVALUACIÓN</span>
            <p>No hay evaluación médica cargada para el trabajador.</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getEmployees } from '../api/employee'
import { getDocuments } from '../api/document'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const employees = ref([])
const documents = ref([])
const analysisByDocumentId = ref({})

const loading = ref(false)
const error = ref('')

const search = ref('')
const areaFilter = ref('')
const resultFilter = ref('')
const reviewFilter = ref('')
const notificationFilter = ref('')

const topScroll = ref(null)
const bodyScroll = ref(null)

const normalize = (value) => String(value || '').toLowerCase().trim()

const fullName = (employee) => {
  return [
    employee.firstName,
    employee.secondName,
    employee.firstLastName,
    employee.secondLastName
  ]
    .filter(Boolean)
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim() || 'Trabajador sin nombre'
}

const documentLabel = (employee) => {
  return [
    employee.documentType,
    employee.documentNumber
  ]
    .filter(Boolean)
    .join(' ')
    .trim() || '-'
}

const parseDate = (value) => {
  if (!value) return 0

  const time = new Date(value).getTime()

  return Number.isNaN(time) ? 0 : time
}

const formatDate = (value) => {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleDateString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const syncTopScroll = () => {
  if (!topScroll.value || !bodyScroll.value) return
  topScroll.value.scrollLeft = bodyScroll.value.scrollLeft
}

const syncBodyScroll = () => {
  if (!topScroll.value || !bodyScroll.value) return
  bodyScroll.value.scrollLeft = topScroll.value.scrollLeft
}

const refreshTopScrollbarWidth = async () => {
  await nextTick()

  if (!topScroll.value || !bodyScroll.value) return

  const table = bodyScroll.value.querySelector('table')
  const spacer = topScroll.value.querySelector('.table-scroll-spacer')

  if (!table || !spacer) return

  spacer.style.width = `${table.scrollWidth}px`
}

const areaOptions = computed(() => {
  const areas = new Set()

  employees.value.forEach((employee) => {
    if (employee.areaCode) areas.add(employee.areaCode)
    if (employee.zone) areas.add(employee.zone)
    if (employee.workArea) areas.add(employee.workArea)
  })

  return [...areas].sort((a, b) => a.localeCompare(b, 'es'))
})

const latestDocumentByEmployeeId = computed(() => {
  const map = {}

  documents.value.forEach((document) => {
    if (!document.employeeId) return

    const previous = map[document.employeeId]

    if (!previous || parseDate(document.uploadedAt) > parseDate(previous.uploadedAt)) {
      map[document.employeeId] = document
    }
  })

  return map
})

const rows = computed(() => {
  return employees.value
    .map((employee) => {
      const latestDocument = latestDocumentByEmployeeId.value[employee.id] || null
      const analysis = latestDocument ? analysisByDocumentId.value[latestDocument.id] : null

      const rawResult = String(analysis?.resultStatus || '').toUpperCase().trim()

      let resultStatus = 'SIN_EVALUACION'

      if (rawResult === 'APTO') {
        resultStatus = 'APTO'
      } else if (rawResult === 'NO_APTO' || rawResult === 'NO APTO') {
        resultStatus = 'NO_APTO'
      } else if (latestDocument) {
        resultStatus = 'PENDIENTE'
      }

      const reviewStatus = latestDocument?.reviewStatus || 'SIN_REVISION'
      const notificationStatus = latestDocument?.notificationStatus || 'SIN_NOTIFICACION'

      return {
        employee,
        documentId: latestDocument?.id || '',
        uploadedAt: latestDocument?.uploadedAt || '',
        resultStatus,
        reviewStatus,
        notificationStatus
      }
    })
    .sort((a, b) => fullName(a.employee).localeCompare(fullName(b.employee), 'es'))
})

const filteredRows = computed(() => {
  const term = normalize(search.value)

  return rows.value.filter((row) => {
    const employee = row.employee

    const matchesArea =
      !areaFilter.value ||
      employee.areaCode === areaFilter.value ||
      employee.zone === areaFilter.value ||
      employee.workArea === areaFilter.value

    const matchesResult =
      !resultFilter.value ||
      row.resultStatus === resultFilter.value

    const matchesReview =
      !reviewFilter.value ||
      row.reviewStatus === reviewFilter.value

    const matchesNotification =
      !notificationFilter.value ||
      row.notificationStatus === notificationFilter.value

    if (!matchesArea || !matchesResult || !matchesReview || !matchesNotification) {
      return false
    }

    if (!term) return true

    const haystack = [
      fullName(employee),
      documentLabel(employee),
      employee.documentNumber,
      employee.currentPosition,
      employee.workArea,
      employee.zone,
      employee.areaCode,
      employee.email,
      row.resultStatus,
      row.reviewStatus,
      row.notificationStatus
    ]
      .map(normalize)
      .join(' ')

    return haystack.includes(term)
  })
})

const aptCount = computed(() =>
  rows.value.filter((row) => row.resultStatus === 'APTO').length
)

const notAptCount = computed(() =>
  rows.value.filter((row) => row.resultStatus === 'NO_APTO').length
)

const pendingReviewCount = computed(() =>
  rows.value.filter((row) => row.reviewStatus === 'PENDING_REVIEW').length
)

const failedNotificationCount = computed(() =>
  rows.value.filter((row) => row.notificationStatus === 'FAILED').length
)

const resetFilters = async () => {
  search.value = ''
  areaFilter.value = ''
  resultFilter.value = ''
  reviewFilter.value = ''
  notificationFilter.value = ''

  await refreshTopScrollbarWidth()
}

const resultLabel = (status) => {
  if (status === 'APTO') return 'APTO'
  if (status === 'NO_APTO') return 'NO APTO'
  if (status === 'PENDIENTE') return 'PENDIENTE'
  return 'SIN EVALUACIÓN'
}

const reviewLabel = (status) => {
  if (status === 'PENDING_REVIEW') return 'PENDIENTE'
  if (status === 'APPROVED') return 'APROBADO'
  if (status === 'REJECTED') return 'RECHAZADO'
  return 'SIN REVISIÓN'
}

const notificationLabel = (status) => {
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  if (status === 'SENT') return 'ENVIADO'
  if (status === 'FAILED') return 'FALLÓ'
  if (status === 'SKIPPED') return 'OMITIDO'
  return 'SIN NOTIFICACIÓN'
}

const resultClass = (status) => {
  if (status === 'APTO') return 'status-pill-active'
  if (status === 'NO_APTO') return 'status-pill-inactive'
  if (status === 'PENDIENTE') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const reviewClass = (status) => {
  if (status === 'APPROVED') return 'status-pill-active'
  if (status === 'REJECTED') return 'status-pill-inactive'
  if (status === 'PENDING_REVIEW') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const notificationClass = (status) => {
  if (status === 'SENT') return 'status-pill-active'
  if (status === 'FAILED') return 'status-pill-inactive'
  if (status === 'SKIPPED') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const loadAnalyses = async (documentsList) => {
  const requests = await Promise.allSettled(
    documentsList.map(async (document) => {
      const response = await http.get(`/api/documents/${document.id}/analysis`)
      return [document.id, response.data]
    })
  )

  const next = {}

  requests.forEach((request) => {
    if (request.status === 'fulfilled') {
      const [documentId, analysis] = request.value
      next[documentId] = analysis
    }
  })

  analysisByDocumentId.value = next
}

const loadData = async () => {
  try {
    loading.value = true
    error.value = ''

    const [employeesResponse, documentsResponse] = await Promise.all([
      getEmployees(),
      getDocuments()
    ])

    employees.value = Array.isArray(employeesResponse.data)
      ? employeesResponse.data
      : []

    documents.value = Array.isArray(documentsResponse.data)
      ? documentsResponse.data
      : []

    await loadAnalyses(documents.value)
    await refreshTopScrollbarWidth()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar el seguimiento de trabajadores.'
    console.error('Error cargando trabajadores:', err)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadData()
  await refreshTopScrollbarWidth()
})
</script>

<style scoped>
.page-title {
  margin: 0;
  font-size: clamp(1.55rem, 2vw, 2rem);
  line-height: 1.15;
  font-weight: 800;
  color: var(--text);
}

.summary-value {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--text);
}

.summary-meta {
  display: block;
  margin-top: 0.25rem;
  color: var(--text-muted);
  font-size: 0.82rem;
}

.person-meta {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.person-meta small,
.muted-text {
  color: var(--text-muted);
  font-size: 0.82rem;
}

.table-scroll-wrapper {
  width: 100%;
}

.table-scroll-top {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  height: 16px;
  margin-bottom: 0.65rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #f8fafc;
}

.table-scroll-spacer {
  height: 1px;
}

.table-scroll-body {
  overflow-x: auto;
}

.tracking-table {
  min-width: 1450px;
}

.tracking-table th {
  font-size: 0.72rem;
  line-height: 1.15;
  white-space: nowrap;
}

.tracking-table td {
  font-size: 0.86rem;
  vertical-align: middle;
}

.tracking-table .secondary-btn,
.tracking-table .primary-btn {
  min-height: 34px;
  padding: 0.45rem 0.6rem;
  font-size: 0.75rem;
  border-radius: 10px;
  white-space: nowrap;
}

.table-scroll-body::-webkit-scrollbar,
.table-scroll-top::-webkit-scrollbar {
  height: 10px;
}

.table-scroll-body::-webkit-scrollbar-thumb,
.table-scroll-top::-webkit-scrollbar-thumb {
  background: #9ca3af;
  border-radius: 999px;
}

.table-scroll-body::-webkit-scrollbar-track,
.table-scroll-top::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 999px;
}

.legend-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.legend-item {
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--surface-soft);
}

.legend-item p {
  margin: 0.75rem 0 0;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.row-critical {
  background: rgba(254, 242, 242, 0.7);
}

.row-pending {
  background: rgba(255, 251, 235, 0.55);
}
</style>