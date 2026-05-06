<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Panel de aprobación</span>
        <h1 class="h1 mb-2">Aprobaciones pendientes</h1>
        <p class="p mb-0">
          Revisa los conceptos médicos analizados y aprueba o rechaza la notificación formal.
        </p>
      </div>

      <div class="header-actions">
        <button
          type="button"
          class="secondary-btn"
          :disabled="loading"
          @click="loadData"
        >
          {{ loading ? 'Actualizando...' : 'Actualizar panel' }}
        </button>
      </div>
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div v-if="bulkSummary" class="state-box info">
      Operación masiva completada. {{ JSON.stringify(bulkSummary) }}
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <span class="label">Pendientes de revisión</span>
        <strong class="kpi-value">{{ pendingRows.length }}</strong>
        <span class="kpi-meta">Documentos listos para revisar</span>
      </div>

      <div class="kpi-card">
        <span class="label">Aptos pendientes</span>
        <strong class="kpi-value">{{ pendingAptCount }}</strong>
        <span class="kpi-meta">Resultados favorables por aprobar</span>
      </div>

      <div class="kpi-card">
        <span class="label">No aptos pendientes</span>
        <strong class="kpi-value">{{ pendingNotAptCount }}</strong>
        <span class="kpi-meta">Casos críticos por revisar</span>
      </div>

      <div class="kpi-card">
        <span class="label">Aprobados</span>
        <strong class="kpi-value">{{ approvedCount }}</strong>
        <span class="kpi-meta">Documentos ya revisados</span>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Filtros</h2>
            <p class="helper-text mb-0">
              Busca por trabajador, documento, archivo, zona o resultado.
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
              placeholder="Trabajador, documento, archivo, zona..."
              :disabled="loading"
            />
          </div>

          <div class="form-field">
            <label class="label" for="resultFilter">Resultado</label>
            <select
              id="resultFilter"
              v-model="resultFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todos</option>
              <option value="APTO">APTO</option>
              <option value="NO_APTO">NO_APTO</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="reviewFilter">Estado revisión</label>
            <select id="reviewFilter" v-model="reviewFilter" class="form-select" :disabled="loading">
              <option value="">Todos</option><option value="PENDING_REVIEW">PENDIENTE</option><option value="APPROVED">APROBADO</option><option value="REJECTED">RECHAZADO</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="notificationFilter">Estado notificación</label>
            <select id="notificationFilter" v-model="notificationFilter" class="form-select" :disabled="loading">
              <option value="">Todos</option><option value="NOT_PENDING">NO ENVIADO</option><option value="SENT">ENVIADO</option><option value="FAILED">FALLÓ</option><option value="SKIPPED">OMITIDO</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="batchFilter">Lote</label>
            <select id="batchFilter" v-model="batchFilter" class="form-select" :disabled="loading">
              <option value="">Todos</option>
              <option v-for="batch in batchOptions" :key="batch" :value="batch">{{ batch }}</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="zoneFilter">Zona / área</label>
            <select
              id="zoneFilter"
              v-model="zoneFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todas</option>
              <option
                v-for="zone in zoneOptions"
                :key="zone"
                :value="zone"
              >
                {{ zone }}
              </option>
            </select>
          </div>
        </div>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Documentos pendientes</h2>
            <p class="helper-text mb-0">
              Solo se muestran documentos analizados, con resultado APTO o NO APTO, pendientes de revisión.
            </p>
          </div>
          <div class="header-actions">
            <input v-model.trim="bulkComment" class="form-control" placeholder="Comentario para acción masiva" style="min-width:260px" />
            <button type="button" class="secondary-btn" :disabled="!filteredRows.length" @click="toggleSelectVisible">
              {{ allVisibleSelected ? 'Quitar selección visible' : 'Seleccionar todos visibles' }}
            </button>
            <button type="button" class="primary-btn" :disabled="bulkLoading || !selectedIds.length" @click="approveSelected">
              {{ bulkLoading ? 'Procesando...' : `Aprobar seleccionados (${selectedIds.length})` }}
            </button>
            <button type="button" class="secondary-btn danger-btn" :disabled="bulkLoading || !selectedIds.length" @click="rejectSelected">
              Rechazar seleccionados
            </button>
          </div>
        </div>

        <div class="hr"></div>

        <div v-if="loading" class="state-box info">
          Cargando documentos pendientes...
        </div>

        <div v-else-if="!pendingRows.length" class="state-box">
          No hay documentos pendientes de revisión.
        </div>

        <div v-else class="table-responsive">
          <table class="table table-hover align-middle">
            <thead>
              <tr>
                <th class="text-center"><input type="checkbox" :checked="allVisibleSelected" @change="toggleSelectVisible" /></th>
                <th>Fecha carga</th>
                <th>Trabajador</th>
                <th>Documento</th>
                <th>Cargo</th>
                <th>Zona</th>
                <th>Resultado médico</th>
                <th>Estado revisión</th>
                <th>Estado notificación</th>
                <th>Fecha aprobación</th>
                <th>Aprobado por</th>
                <th>Archivo</th>
                <th class="text-center">Acción</th>
              </tr>
            </thead>

            <tbody>
              <tr
                v-for="row in filteredRows"
                :key="row.id"
                :class="row.resultStatus === 'NO_APTO' ? 'row-critical' : ''"
              >
                <td class="text-center"><input type="checkbox" :checked="selectedIds.includes(row.id)" @change="toggleSelected(row.id)" /></td>
                <td>
                  <div class="date-stack">
                    <strong>{{ row.uploadedDatePart }}</strong>
                    <small>{{ row.uploadedTimePart }}</small>
                  </div>
                </td>

                <td>
                  <div class="person-meta">
                    <strong>{{ row.fullName }}</strong>
                    <small>{{ row.email || 'Sin correo registrado' }}</small>
                  </div>
                </td>

                <td>{{ row.documentLabel }}</td>
                <td>{{ row.currentPosition || '-' }}</td>
                <td>{{ row.zone || row.areaCode || '-' }}</td>

                <td>
                  <span
                    v-if="row.resultStatus === 'APTO'"
                    class="status-pill-active"
                  >
                    APTO
                  </span>

                  <span
                    v-else
                    class="status-pill-inactive"
                  >
                    NO APTO
                  </span>
                </td>
                <td><span :class="reviewClass(row.reviewStatus)">{{ reviewLabel(row.reviewStatus) }}</span></td>
                <td><span :class="notificationClass(row.notificationStatus)">{{ notificationLabel(row.notificationStatus) }}</span></td>
                <td>{{ formatDate(row.reviewedAt) }}</td>
                <td>{{ row.reviewedBy || '-' }}</td>

                <td>
                  <span :title="row.originalFileName">
                    {{ shortText(row.originalFileName, 42) }}
                  </span>
                </td>

                <td>
                  <div class="actions justify-content-center">
                    <RouterLink
                      :to="`/documents/${row.id}`"
                      class="primary-btn"
                    >
                      Revisar
                    </RouterLink>
                  </div>
                </td>
              </tr>

              <tr v-if="!filteredRows.length">
                <td colspan="13">
                  <div class="state-box m-2">
                    No hay coincidencias con los filtros actuales.
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Trazabilidad reciente</h2>
            <p class="helper-text mb-0">
              Documentos ya aprobados o rechazados.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div v-if="!reviewedRows.length" class="state-box mb-0">
          No hay revisiones registradas todavía.
        </div>

        <div v-else class="table-responsive">
          <table class="table table-sm align-middle">
            <thead>
              <tr>
                <th>Fecha revisión</th>
                <th>Trabajador</th>
                <th>Resultado</th>
                <th>Revisión</th>
                <th>Notificación</th>
                <th>Revisado por</th>
                <th class="text-center">Detalle</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="row in reviewedRows.slice(0, 10)" :key="row.id">
                <td>{{ formatDate(row.reviewedAt) }}</td>
                <td>{{ row.fullName }}</td>
                <td>{{ row.resultStatus }}</td>
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
                <td>{{ row.reviewedBy || '-' }}</td>
                <td>
                  <div class="actions justify-content-center">
                    <RouterLink
                      :to="`/documents/${row.id}`"
                      class="secondary-btn"
                    >
                      Ver detalle
                    </RouterLink>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getDocuments, approveBulkDocuments, rejectBulkDocuments } from '../api/document'
import { getEmployees } from '../api/employee'
import http from '../api/http'

const documents = ref([])
const employees = ref([])
const analysisById = ref({})

const loading = ref(false)
const error = ref('')

const search = ref('')
const resultFilter = ref('')
const zoneFilter = ref('')
const reviewFilter = ref('')
const notificationFilter = ref('')
const batchFilter = ref('')
const selectedIds = ref([])
const bulkComment = ref('')
const bulkLoading = ref(false)
const bulkSummary = ref(null)

const normalize = (value) => String(value || '').toLowerCase().trim()

const parseDate = (value) => {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

const formatDate = (value) => {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDatePart = (value) => {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleDateString('es-CO')
}

const formatTimePart = (value) => {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleTimeString('es-CO', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const shortText = (value, max = 60) => {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (!text) return '-'
  return text.length > max ? `${text.slice(0, max).trim()}...` : text
}

const employeeMap = computed(() => {
  const map = {}

  for (const employee of employees.value) {
    const fullName = [
      employee.firstName,
      employee.secondName,
      employee.firstLastName,
      employee.secondLastName
    ]
      .filter(Boolean)
      .join(' ')
      .trim()

    const documentLabel = [employee.documentType, employee.documentNumber]
      .filter(Boolean)
      .join(' ')
      .trim()

    map[employee.id] = {
      fullName: fullName || 'Trabajador sin nombre',
      documentLabel: documentLabel || '-',
      currentPosition: employee.currentPosition || '',
      workArea: employee.workArea || '',
      zone: employee.zone || '',
      areaCode: employee.areaCode || '',
      email: employee.email || ''
    }
  }

  return map
})

const rows = computed(() => {
  return documents.value
    .map((doc) => {
      const analysis = analysisById.value[doc.id] || null
      const employee = employeeMap.value[doc.employeeId] || {
        fullName: 'Trabajador no identificado',
        documentLabel: doc.employeeId || '-',
        currentPosition: '',
        workArea: '',
        zone: '',
        areaCode: doc.areaCode || '',
        email: ''
      }

      const resultStatus =
        analysis?.resultStatus === 'APTO' || analysis?.resultStatus === 'NO_APTO'
          ? analysis.resultStatus
          : 'PENDIENTE'

      return {
        id: doc.id,
        originalFileName: doc.originalFileName || '-',
        uploadedAt: doc.uploadedAt || '',
        uploadedDatePart: formatDatePart(doc.uploadedAt),
        uploadedTimePart: formatTimePart(doc.uploadedAt),
        uploadedBy: doc.uploadedBy || '-',
        processingStatus: doc.processingStatus || '-',
        reviewStatus: doc.reviewStatus || 'PENDING_REVIEW',
        reviewedBy: doc.reviewedBy || '',
        reviewedAt: doc.reviewedAt || '',
        reviewComment: doc.reviewComment || '',
        notificationStatus: doc.notificationStatus || 'NOT_PENDING',
        batchCode: doc.batchCode || '',
        areaCode: doc.areaCode || employee.areaCode || '',
        resultStatus,
        fullName: employee.fullName,
        documentLabel: employee.documentLabel,
        currentPosition: employee.currentPosition,
        workArea: employee.workArea,
        zone: employee.zone,
        email: employee.email
      }
    })
    .sort((a, b) => parseDate(b.uploadedAt) - parseDate(a.uploadedAt))
})

const pendingRows = computed(() => {
  return rows.value.filter((row) => {
    return (
      row.reviewStatus === 'PENDING_REVIEW' &&
      (row.resultStatus === 'APTO' || row.resultStatus === 'NO_APTO')
    )
  })
})

const reviewedRows = computed(() => {
  return rows.value.filter((row) => {
    return row.reviewStatus === 'APPROVED' || row.reviewStatus === 'REJECTED'
  })
})

const filteredRows = computed(() => {
  const term = normalize(search.value)

  return pendingRows.value.filter((row) => {
    const matchesResult = !resultFilter.value || row.resultStatus === resultFilter.value
    const matchesZone =
      !zoneFilter.value ||
      row.zone === zoneFilter.value ||
      row.areaCode === zoneFilter.value ||
      row.workArea === zoneFilter.value
    const matchesReview = !reviewFilter.value || row.reviewStatus === reviewFilter.value
    const matchesNotification = !notificationFilter.value || row.notificationStatus === notificationFilter.value
    const matchesBatch = !batchFilter.value || row.batchCode === batchFilter.value

    if (!matchesResult || !matchesZone || !matchesReview || !matchesNotification || !matchesBatch) return false

    if (!term) return true

    const haystack = [
      row.fullName,
      row.documentLabel,
      row.originalFileName,
      row.currentPosition,
      row.workArea,
      row.zone,
      row.areaCode,
      row.resultStatus,
      row.batchCode
    ]
      .map(normalize)
      .join(' ')

    return haystack.includes(term)
  })
})


const batchOptions = computed(() => {
  return [...new Set(rows.value.map((r) => r.batchCode).filter(Boolean))].sort((a,b)=>a.localeCompare(b,'es'))
})

const allVisibleSelected = computed(() => {
  if (!filteredRows.value.length) return false
  return filteredRows.value.every((row) => selectedIds.value.includes(row.id))
})

const toggleSelected = (id) => {
  if (selectedIds.value.includes(id)) selectedIds.value = selectedIds.value.filter((item) => item !== id)
  else selectedIds.value = [...selectedIds.value, id]
}

const toggleSelectVisible = () => {
  if (allVisibleSelected.value) {
    selectedIds.value = selectedIds.value.filter((id) => !filteredRows.value.some((row) => row.id === id))
  } else {
    const merged = new Set([...selectedIds.value, ...filteredRows.value.map((row) => row.id)])
    selectedIds.value = [...merged]
  }
}

const runBulkAction = async (type) => {
  if (!selectedIds.value.length) return
  const actionLabel = type === 'approve' ? 'aprobar' : 'rechazar'
  if (!window.confirm(`¿Seguro que deseas ${actionLabel} ${selectedIds.value.length} documentos?`)) return
  try {
    bulkLoading.value = true
    const fn = type === 'approve' ? approveBulkDocuments : rejectBulkDocuments
    const { data } = await fn(selectedIds.value, bulkComment.value || '')
    bulkSummary.value = data
    selectedIds.value = []
    await loadData()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo completar la acción masiva.'
  } finally {
    bulkLoading.value = false
  }
}

const approveSelected = () => runBulkAction('approve')
const rejectSelected = () => runBulkAction('reject')

const zoneOptions = computed(() => {
  const zones = new Set()

  for (const row of rows.value) {
    if (row.zone) zones.add(row.zone)
    if (row.areaCode) zones.add(row.areaCode)
    if (row.workArea) zones.add(row.workArea)
  }

  return [...zones].sort((a, b) => a.localeCompare(b, 'es'))
})

const pendingAptCount = computed(() =>
  pendingRows.value.filter((row) => row.resultStatus === 'APTO').length
)

const pendingNotAptCount = computed(() =>
  pendingRows.value.filter((row) => row.resultStatus === 'NO_APTO').length
)

const approvedCount = computed(() =>
  rows.value.filter((row) => row.reviewStatus === 'APPROVED').length
)

const resetFilters = () => {
  search.value = ''
  resultFilter.value = ''
  zoneFilter.value = ''
  reviewFilter.value = ''
  notificationFilter.value = ''
  batchFilter.value = ''
  selectedIds.value = []
  bulkComment.value = ''
}

const reviewLabel = (status) => {
  if (status === 'PENDING_REVIEW') return 'PENDIENTE'
  if (status === 'APPROVED') return 'APROBADO'
  if (status === 'REJECTED') return 'RECHAZADO'
  return status || '-'
}

const notificationLabel = (status) => {
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  if (status === 'SENT') return 'ENVIADO'
  if (status === 'FAILED') return 'FALLÓ'
  if (status === 'SKIPPED') return 'OMITIDO'
  return status || '-'
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

const loadData = async () => {
  try {
    loading.value = true
    error.value = ''

    const [documentsResponse, employeesResponse] = await Promise.all([
      getDocuments(),
      getEmployees()
    ])

    documents.value = Array.isArray(documentsResponse.data) ? documentsResponse.data : []
    employees.value = Array.isArray(employeesResponse.data) ? employeesResponse.data : []

    const analysisRequests = await Promise.allSettled(
      documents.value.map(async (doc) => {
        const response = await http.get(`/api/documents/${doc.id}/analysis`)
        return [doc.id, response.data]
      })
    )

    const nextAnalysis = {}

    for (const request of analysisRequests) {
      if (request.status === 'fulfilled') {
        const [documentId, analysis] = request.value
        nextAnalysis[documentId] = analysis
      }
    }

    analysisById.value = nextAnalysis
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar el panel de aprobación.'
    console.error('Error cargando panel de aprobación:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
