<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDocuments } from '../api/document'
import { getEmployees } from '../api/employee'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const documents = ref([])
const employees = ref([])
const analysisById = ref({})
const loading = ref(false)
const backfillLoading = ref(false)
const error = ref('')

const search = ref('')
const resultFilter = ref('')
const uploadedByFilter = ref('')
const dateFilter = ref('')
const statusFilter = ref('')

const normalize = (value) => String(value || '').toLowerCase().trim()

const parseDate = (value) => {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

const formatDateTime = (value) => {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}

const formatDatePart = (value) => {
  if (!value) return '-'
  const localDate = formatLocalDate(value)
  if (localDate !== null) return localDate

  try {
    return new Date(value).toLocaleDateString()
  } catch {
    return value
  }
}

const formatTimePart = (value) => {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleTimeString()
  } catch {
    return value
  }
}

const formatDateOnly = (value) => {
  if (!value) return ''
  const match = String(value).trim().match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (match) return `${match[1]}-${match[2]}-${match[3]}`

  try {
    return new Date(value).toISOString().slice(0, 10)
  } catch {
    return ''
  }
}

const formatLocalDate = (value) => {
  const match = String(value || '').trim().match(/^(\d{4})-(\d{2})-(\d{2})/)
  return match ? `${match[3]}/${match[2]}/${match[1]}` : null
}

const shortText = (value, max = 110) => {
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
      fullName: fullName || 'Funcionario sin nombre',
      documentLabel: documentLabel || '-',
      currentPosition: employee.currentPosition || '',
      workArea: employee.workArea || '',
      zone: employee.zone || '',
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
        fullName: 'Funcionario no identificado',
        documentLabel: doc.employeeId || '-',
        currentPosition: '',
        workArea: '',
        zone: '',
        email: ''
      }

      const resultStatus =
        analysis?.resultStatus === 'APTO' || analysis?.resultStatus === 'NO_APTO'
          ? analysis.resultStatus
          : 'PENDIENTE'

      const analysisState = analysis
        ? 'ANALIZADO'
        : doc.processingStatus === 'ERROR'
          ? 'ERROR'
          : 'PENDIENTE'

      const observations = analysis?.extractedFields?.observations || ''
      const evaluationDate =
        doc.evaluationDate ||
        doc.fechaEvaluacion ||
        doc.fechaConcepto ||
        analysis?.evaluationDate ||
        analysis?.fechaEvaluacion ||
        analysis?.fechaConcepto ||
        analysis?.conceptDate ||
        ''

      return {
        id: doc.id,
        originalFileName: doc.originalFileName || '-',
        employeeId: doc.employeeId || '',
        fullName: employee.fullName,
        documentLabel: employee.documentLabel,
        currentPosition: employee.currentPosition,
        workArea: employee.workArea,
        zone: employee.zone,
        email: employee.email,
        documentType: doc.documentType || '-',
        examType: doc.examType || '-',
        uploadedBy: doc.uploadedBy || '-',
        uploadedAt: doc.uploadedAt || '',
        uploadedAtLabel: formatDateTime(doc.uploadedAt),
        uploadedDatePart: formatDatePart(doc.uploadedAt),
        uploadedTimePart: formatTimePart(doc.uploadedAt),
        uploadedDateOnly: formatDateOnly(doc.uploadedAt),
        evaluationDate,
        evaluationDateLabel: evaluationDate ? formatDatePart(evaluationDate) : '-',
        evaluationDateOnly: evaluationDate ? String(evaluationDate).slice(0, 10) : '',
        resultStatus,
        analysisState,
        observations,
        observationExcerpt: shortText(observations, 120),
        processingStatus: doc.processingStatus || '-'
      }
    })
    .sort((a, b) => parseDate(b.evaluationDate || b.uploadedAt) - parseDate(a.evaluationDate || a.uploadedAt))
})

const filteredRows = computed(() => {
  const term = normalize(search.value)

  return rows.value.filter((row) => {
    const matchesResult = !resultFilter.value || row.resultStatus === resultFilter.value
    const matchesUploader = !uploadedByFilter.value || row.uploadedBy === uploadedByFilter.value
    const matchesDate = !dateFilter.value || row.evaluationDateOnly === dateFilter.value
    const matchesStatus = !statusFilter.value || row.analysisState === statusFilter.value

    if (!matchesResult || !matchesUploader || !matchesDate || !matchesStatus) {
      return false
    }

    if (!term) return true

    const haystack = [
      row.originalFileName,
      row.fullName,
      row.documentLabel,
      row.uploadedBy,
      row.resultStatus,
      row.analysisState,
      row.currentPosition,
      row.workArea,
      row.zone,
      row.observations
    ]
      .map(normalize)
      .join(' ')

    return haystack.includes(term)
  })
})

const totalCount = computed(() => rows.value.length)
const aptCount = computed(() => rows.value.filter((row) => row.resultStatus === 'APTO').length)
const notAptCount = computed(() => rows.value.filter((row) => row.resultStatus === 'NO_APTO').length)
const pendingCount = computed(() => rows.value.filter((row) => row.resultStatus === 'PENDIENTE').length)
const analyzedCount = computed(() => rows.value.filter((row) => row.analysisState === 'ANALIZADO').length)

const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return rows.value.filter((row) => row.uploadedDateOnly === today).length
})

const lastUploadLabel = computed(() => {
  return rows.value.length ? rows.value[0].evaluationDateLabel : 'Sin registros'
})

const latestCriticalLabel = computed(() => {
  const firstCritical = rows.value.find((row) => row.resultStatus === 'NO_APTO')
  return firstCritical ? `${firstCritical.fullName} • ${firstCritical.evaluationDateLabel}` : 'Sin casos NO APTO'
})

const uploaderOptions = computed(() => {
  return [...new Set(rows.value.map((row) => row.uploadedBy).filter((value) => value && value !== '-'))]
    .sort((a, b) => a.localeCompare(b, 'es'))
})

const visibleUploaders = computed(() => {
  return [...new Set(filteredRows.value.map((row) => row.uploadedBy).filter((value) => value && value !== '-'))].length
})

const setQuickFilter = (value) => {
  resultFilter.value = value
}

const resetFilters = () => {
  search.value = ''
  resultFilter.value = ''
  uploadedByFilter.value = ''
  dateFilter.value = ''
  statusFilter.value = ''
}

const rowClass = (row) => {
  if (row.resultStatus === 'NO_APTO') return 'row-critical'
  if (row.resultStatus === 'PENDIENTE') return 'row-pending'
  return ''
}

const escapeCsv = (value) => {
  const text = String(value ?? '')
  if (text.includes('"') || text.includes(',') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const exportCsv = () => {
  const headers = [
    'fecha_evaluacion',
    'fecha_carga',
    'archivo_pdf',
    'funcionario',
    'documento',
    'cargo',
    'area',
    'zona',
    'resultado',
    'estado_analisis',
    'subido_por',
    'observaciones'
  ]

  const csvRows = filteredRows.value.map((row) => ({
    fecha_evaluacion: row.evaluationDateLabel,
    fecha_carga: row.uploadedDatePart,
    archivo_pdf: row.originalFileName,
    funcionario: row.fullName,
    documento: row.documentLabel,
    cargo: row.currentPosition,
    area: row.workArea,
    zona: row.zone,
    resultado: row.resultStatus,
    estado_analisis: row.analysisState,
    subido_por: row.uploadedBy,
    observaciones: row.observations
  }))

  const csv = [
    headers.join(','),
    ...csvRows.map((row) => headers.map((header) => escapeCsv(row[header])).join(','))
  ].join('\n')

  const blob = new Blob(['\ufeff' + csv], {
    type: 'text/csv;charset=utf-8;'
  })

  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'panel-documentos-alturas.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const loadDocuments = async () => {
  try {
    loading.value = true
    error.value = ''

    const [documentsResponse, employeesResponse] = await Promise.all([
      getDocuments({ historical: false }),
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
    error.value = err?.response?.data?.message || 'No se pudieron cargar los documentos.'
    console.error('Error cargando documentos:', err)
  } finally {
    loading.value = false
  }
}

const backfillExtractedData = async () => {
  const confirmed = window.confirm('Esto reanaliza documentos antiguos para completar fecha de evaluación y fecha de nacimiento sin cambiar la trazabilidad. ¿Continuar?')
  if (!confirmed) return

  try {
    backfillLoading.value = true
    error.value = ''

    const { data } = await http.post('/api/documents/backfill-extracted-data')
    await loadDocuments()

    window.alert(`Reproceso terminado. Revisados: ${data?.checked ?? 0}. Actualizados: ${data?.updated ?? 0}. Errores: ${data?.failed ?? 0}.`)
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo reprocesar la información extraída.'
  } finally {
    backfillLoading.value = false
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

    // Clean up URL object after a delay
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (err) {
    if (pdfWindow) pdfWindow.close()
    console.error('Error abriendo PDF:', err)
    alert('No se pudo abrir el PDF. Es posible que el archivo no exista o no tengas permisos.')
  }
}

onMounted(() => {
  loadDocuments()
})
</script>

<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Panel principal</span>
        <h1 class="h1 mb-2">Panel ejecutivo de documentos</h1>
        <p class="p mb-0">
          Revisa qué PDFs se han cargado, qué resultado arrojaron, quién los subió y cuáles requieren atención inmediata.
        </p>
      </div>

      <div class="header-actions">
        <button class="secondary-btn" :disabled="loading" @click="loadDocuments">
          {{ loading ? 'Actualizando...' : 'Actualizar panel' }}
        </button>

        <button class="secondary-btn" :disabled="loading || !filteredRows.length" @click="exportCsv">
          Exportar CSV
        </button>

        <button
          v-if="auth.canDeleteDocuments"
          class="secondary-btn"
          :disabled="loading || backfillLoading"
          @click="backfillExtractedData"
        >
          {{ backfillLoading ? 'Reprocesando...' : 'Reprocesar fechas' }}
        </button>

        <RouterLink to="/documents/upload" class="primary-btn">
          Subir evaluación
        </RouterLink>
      </div>
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <span class="label">Total documentos</span>
        <strong class="kpi-value">{{ totalCount }}</strong>
        <span class="kpi-meta">Registros visibles en el sistema</span>
      </div>

      <div class="kpi-card">
        <span class="label">Aptos</span>
        <strong class="kpi-value">{{ aptCount }}</strong>
        <span class="kpi-meta">Resultado favorable</span>
      </div>

      <div class="kpi-card">
        <span class="label">No aptos</span>
        <strong class="kpi-value">{{ notAptCount }}</strong>
        <span class="kpi-meta">Casos que requieren atención</span>
      </div>

      <div class="kpi-card">
        <span class="label">Pendientes</span>
        <strong class="kpi-value">{{ pendingCount }}</strong>
        <span class="kpi-meta">Sin análisis final</span>
      </div>

      <div class="kpi-card">
        <span class="label">Analizados</span>
        <strong class="kpi-value">{{ analyzedCount }}</strong>
        <span class="kpi-meta">Con resultado persistido</span>
      </div>

      <div class="kpi-card">
        <span class="label">Subidos hoy</span>
        <strong class="kpi-value">{{ todayCount }}</strong>
        <span class="kpi-meta">Cargas del día actual</span>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span class="label">Última carga</span>
        <span>{{ lastUploadLabel }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Filas visibles</span>
        <span>{{ filteredRows.length }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Usuarios cargando</span>
        <span>{{ visibleUploaders }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Último caso crítico</span>
        <span>{{ latestCriticalLabel }}</span>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Filtros</h2>
            <p class="helper-text mb-0">
              Busca por funcionario, documento, archivo u observación. También puedes filtrar por resultado, fecha, usuario o estado.
            </p>
          </div>

          <div class="header-actions">
            <button class="secondary-btn" :disabled="loading" @click="resetFilters">
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
            @click="setQuickFilter('')"
          >
            Todos
          </button>

          <button
            type="button"
            class="filter-chip success"
            :class="{ active: resultFilter === 'APTO' }"
            @click="setQuickFilter('APTO')"
          >
            Aptos
          </button>

          <button
            type="button"
            class="filter-chip danger"
            :class="{ active: resultFilter === 'NO_APTO' }"
            @click="setQuickFilter('NO_APTO')"
          >
            No aptos
          </button>

          <button
            type="button"
            class="filter-chip warning"
            :class="{ active: resultFilter === 'PENDIENTE' }"
            @click="setQuickFilter('PENDIENTE')"
          >
            Pendientes
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
              placeholder="Archivo, funcionario, documento, usuario, observación..."
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
              <option value="PENDIENTE">PENDIENTE</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="statusFilter">Estado</label>
            <select
              id="statusFilter"
              v-model="statusFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todos</option>
              <option value="ANALIZADO">ANALIZADO</option>
              <option value="PENDIENTE">PENDIENTE</option>
              <option value="ERROR">ERROR</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="uploadedByFilter">Subido por</label>
            <select
              id="uploadedByFilter"
              v-model="uploadedByFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todos</option>
              <option v-for="uploader in uploaderOptions" :key="uploader" :value="uploader">
                {{ uploader }}
              </option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="dateFilter">Fecha de evaluación</label>
            <input
              id="dateFilter"
              v-model="dateFilter"
              type="date"
              class="form-control"
              :disabled="loading"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Documentos cargados</h2>
            <p class="helper-text mb-0">
              Tabla principal de consulta para seguimiento operativo y revisión ejecutiva.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div v-if="loading" class="state-box info">
          Cargando documentos, funcionarios y análisis...
        </div>

        <div v-else-if="!rows.length" class="state-box">
          No hay documentos cargados todavía.
        </div>

        <div v-else class="table-responsive">
          <table class="table table-hover align-middle">
            <thead>
              <tr>
                <th>Fecha evaluación</th>
                <th>Archivo PDF</th>
                <th>Funcionario</th>
                <th>Documento</th>
                <th>Resultado</th>
                <th>Estado</th>
                <th>Subido por</th>
                <th>Observación</th>
                <th class="text-center">Acción</th>
              </tr>
            </thead>

            <tbody>
              <tr
                v-for="row in filteredRows"
                :key="row.id"
                :class="rowClass(row)"
              >
                <td>
                  <div class="date-stack">
                    <strong>{{ row.evaluationDateLabel }}</strong>
                    <small>Carga: {{ row.uploadedDatePart }}</small>
                  </div>
                </td>

                <td>
                  <div class="doc-meta">
                    <strong>{{ row.originalFileName }}</strong>
                    <small>{{ row.examType }}</small>
                  </div>
                </td>

                <td>
                  <div class="person-meta">
                    <strong>{{ row.fullName }}</strong>
                    <small>{{ row.currentPosition || 'Sin cargo registrado' }}</small>
                  </div>
                </td>

                <td>{{ row.documentLabel }}</td>

                <td>
                  <span
                    v-if="row.resultStatus === 'APTO'"
                    class="status-pill-active"
                  >
                    APTO
                  </span>

                  <span
                    v-else-if="row.resultStatus === 'NO_APTO'"
                    class="status-pill-inactive"
                  >
                    NO APTO
                  </span>

                  <span v-else class="status-pill-warning">
                    PENDIENTE
                  </span>
                </td>

                <td>
                  <span class="session-chip-custom">
                    {{ row.analysisState }}
                  </span>
                </td>

                <td>{{ row.uploadedBy }}</td>

                <td>
                  <span :title="row.observations || ''" class="observation-snippet">
                    {{ row.observationExcerpt }}
                  </span>
                </td>

                <td>
                  <div class="actions justify-content-center">
                    <RouterLink :to="`/documents/${row.id}`" class="secondary-btn">
                      Ver detalle
                    </RouterLink>

                    <button
                      type="button"
                      class="secondary-btn"
                      title="Ver PDF original"
                      @click="viewPdf(row.id)"
                    >
                      Ver PDF
                    </button>
                  </div>
                </td>
              </tr>

              <tr v-if="!filteredRows.length">
                <td colspan="9">
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
  </section>
</template>
