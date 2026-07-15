<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  deleteAllHistoricalImportIssues,
  deleteHistoricalImportIssue,
  getHistoricalImportIssues,
  viewHistoricalImportIssuePdf
} from '../api/document'

const loading = ref(false)
const error = ref('')
const issues = ref([])
const search = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const currentTab = ref('REGULAR')

const normalize = (value) => String(value || '').toLowerCase().trim()

const filteredIssues = computed(() => {
  const term = normalize(search.value)

  return issues.value.filter((issue) => {
    const issueTab = issue.uploadType || 'REGULAR'
    if (issueTab !== currentTab.value) return false

    if (!term) return true

    const haystack = [
      issue.fileName,
      issue.documentNumber,
      issue.patientName,
      issue.message,
      issue.fechaEvaluacion,
      issue.evaluationDate,
      issue.uploadedBy
    ].map(normalize).join(' ')

    return haystack.includes(term)
  })
})

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredIssues.value.length / pageSize.value))
})

const visiblePageItems = computed(() => {
  const total = totalPages.value
  const current = currentPage.value

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

const paginatedIssues = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredIssues.value.slice(start, start + pageSize.value)
})

const pageStart = computed(() => {
  if (!filteredIssues.value.length) return 0
  return (currentPage.value - 1) * pageSize.value + 1
})

const pageEnd = computed(() => {
  return Math.min(currentPage.value * pageSize.value, filteredIssues.value.length)
})

const goToPage = (page) => {
  currentPage.value = Math.min(
    Math.max(Number(page) || 1, 1),
    totalPages.value
  )
}

const formatDate = (value) => {
  if (!value) return '-'

  try {
    return new Date(value).toLocaleString('es-CO', {
      timeZone: 'America/Bogota',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return value
  }
}

const formatDateOnly = (value) => {
  if (!value) return '-'

  const raw = String(value).slice(0, 10)
  const match = raw.match(/^(\d{4})-(\d{2})-(\d{2})$/)

  if (!match) return formatDate(value)

  return `${match[3]}/${match[2]}/${match[1]}`
}

const loadIssues = async () => {
  try {
    loading.value = true
    error.value = ''
    const { data } = await getHistoricalImportIssues()
    issues.value = Array.isArray(data) ? data : []
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudieron cargar los PDFs no asociados.'
  } finally {
    loading.value = false
  }
}

const removeIssue = async (issue) => {
  if (!issue?.id) return

  const fileName = issue.fileName || 'este registro'
  const confirmed = window.confirm(`Seguro que deseas eliminar "${fileName}" de PDFs no asociados?`)

  if (!confirmed) return

  try {
    await deleteHistoricalImportIssue(issue.id)
    issues.value = issues.value.filter((item) => item.id !== issue.id)
    goToPage(currentPage.value)
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo eliminar el registro.'
  }
}

const viewIssuePdf = async (issue) => {
  if (!issue?.id) return

  try {
    const response = await viewHistoricalImportIssuePdf(issue.id)
    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)

    window.open(url, '_blank')
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo abrir el PDF no asociado.'
  }
}

const removeAllIssues = async () => {
  if (!issues.value.length) return

  const confirmed = window.confirm('Seguro que deseas eliminar todos los PDFs no asociados?')

  if (!confirmed) return

  try {
    await deleteAllHistoricalImportIssues()
    issues.value = []
    currentPage.value = 1
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudieron eliminar los PDFs no asociados.'
  }
}

onMounted(loadIssues)

watch(search, () => {
  currentPage.value = 1
})

watch(currentTab, () => {
  currentPage.value = 1
  search.value = ''
})

watch(pageSize, () => {
  currentPage.value = 1
})
</script>

<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Carga historica y masiva</span>
        <h1 class="h1 mb-2">PDFs no asociados</h1>
        <p class="p mb-0">
          Archivos que no se pudieron guardar porque no se encontró trabajador o no se pudo leer la cédula.
        </p>
      </div>

      <div class="header-actions">
        <button class="secondary-btn" :disabled="loading" @click="loadIssues">
          {{ loading ? 'Actualizando...' : 'Actualizar' }}
        </button>

        <button
          class="secondary-btn danger-btn"
          :disabled="loading || !issues.length"
          @click="removeAllIssues"
        >
          Borrar todo
        </button>

        <RouterLink to="/documents/upload" class="primary-btn">
          Volver a carga
        </RouterLink>
      </div>
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Pendientes por revisar</h2>
            <p class="helper-text mb-0">
              Nombre leido del PDF es el nombre que la app alcanzo a extraer del archivo; no viene de la base de datos.
            </p>
          </div>

          <span class="status-pill-warning">
            {{ filteredIssues.length }} pendiente{{ filteredIssues.length === 1 ? '' : 's' }}
          </span>
        </div>

        <div class="hr mt-0 mb-3"></div>

        <div class="history-section-tabs mb-4">
          <button
            type="button"
            class="section-tab"
            :class="{ active: currentTab === 'REGULAR' }"
            @click="currentTab = 'REGULAR'"
          >
            Evaluaciones
          </button>
          
          <button
            type="button"
            class="section-tab"
            :class="{ active: currentTab === 'HISTORICAL' }"
            @click="currentTab = 'HISTORICAL'"
          >
            Historial
          </button>
          
          <button
            type="button"
            class="section-tab"
            :class="{ active: currentTab === 'CONSTANCIA' }"
            @click="currentTab = 'CONSTANCIA'"
          >
            Constancias
          </button>
        </div>

        <div class="hr"></div>

        <div class="form-field mb-3">
          <label class="label" for="issueSearch">Buscar</label>
          <input
            id="issueSearch"
            v-model.trim="search"
            type="text"
            class="form-control"
            placeholder="Archivo, cedula, nombre, fecha o usuario..."
          />
        </div>

        <div class="pagination-toolbar">
          <div class="form-field page-size-field">
            <label class="label" for="issuePageSize">Mostrar</label>
            <select id="issuePageSize" v-model.number="pageSize" class="form-select" @change="goToPage(1)">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
          </div>

          <span class="helper-text mb-0">
            Mostrando {{ pageStart }}-{{ pageEnd }} de {{ filteredIssues.length }}
          </span>
        </div>

        <div v-if="loading" class="state-box info">
          Cargando PDFs no asociados...
        </div>

        <div v-else-if="!issues.length" class="state-box">
          No hay PDFs no asociados registrados.
        </div>

        <div v-else class="table-responsive">
          <table class="table table-hover align-middle">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Fecha evaluacion PDF</th>
                <th>Archivo</th>
                <th>Cedula leida</th>
                <th>Nombre leido del PDF</th>
                <th>Motivo</th>
                <th>Subido por</th>
                <th class="text-center">Acciones</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="issue in paginatedIssues" :key="issue.id">
                <td>{{ formatDate(issue.createdAt) }}</td>
                <td>{{ formatDateOnly(issue.fechaEvaluacion || issue.evaluationDate) }}</td>
                <td><strong>{{ issue.fileName || '-' }}</strong></td>
                <td>{{ issue.documentNumber || '-' }}</td>
                <td>{{ issue.patientName || '-' }}</td>
                <td>{{ issue.message || '-' }}</td>
                <td>{{ issue.uploadedBy || '-' }}</td>
                <td class="text-center issue-actions">
                  <button
                    type="button"
                    class="secondary-btn small-btn"
                    :disabled="!issue.filePath"
                    :title="issue.filePath ? 'Abrir PDF' : 'Este registro no conserva copia del PDF'"
                    @click="viewIssuePdf(issue)"
                  >
                    Ver PDF
                  </button>

                  <button
                    type="button"
                    class="secondary-btn danger-btn small-btn"
                    @click="removeIssue(issue)"
                  >
                    Eliminar
                  </button>
                </td>
              </tr>

              <tr v-if="!filteredIssues.length">
                <td colspan="8">
                  <div class="state-box m-2">
                    No hay coincidencias con la busqueda actual.
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="filteredIssues.length" class="pagination-bar google-pagination">
          <div class="pagination-info">
            Mostrando <strong>{{ pageStart }}</strong>-<strong>{{ pageEnd }}</strong> de <strong>{{ filteredIssues.length }}</strong>
          </div>

          <div class="pagination-actions">
          <button
            type="button"
            class="page-nav"
            :disabled="currentPage === 1"
            @click="goToPage(currentPage - 1)"
          >
            Anterior
          </button>

          <template v-for="(item, index) in visiblePageItems" :key="`${item}-${index}`">
            <span v-if="item === '...'" class="page-ellipsis">...</span>
            <button
              v-else
              type="button"
              class="page-number"
              :class="{ active: item === currentPage }"
              @click="goToPage(item)"
            >
              {{ item }}
            </button>
          </template>

          <button
            type="button"
            class="page-nav"
            :disabled="currentPage === totalPages"
            @click="goToPage(currentPage + 1)"
          >
            Siguiente
          </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.pagination-toolbar,
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.pagination-toolbar {
  margin-bottom: 0.85rem;
}

.pagination-bar {
  margin-top: 0.85rem;
  padding-top: 0.85rem;
  border-top: 1px solid var(--border);
}

.page-size-field {
  max-width: 130px;
}

.pagination-info {
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 800;
}

.pagination-info strong {
  color: var(--text);
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.page-nav,
.page-number {
  min-height: 32px;
  padding: 0.38rem 0.62rem;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--surface);
  color: var(--text);
  font-size: 0.74rem;
  font-weight: 800;
}

.page-number {
  min-width: 32px;
  padding-inline: 0.5rem;
}

.page-number.active {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--primary);
}

.page-nav:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.page-ellipsis {
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 800;
  padding: 0 0.12rem;
}

.danger-btn {
  color: #991b1b;
  border-color: #fecaca;
  background: #fff1f2;
}

.small-btn {
  min-height: 32px;
  padding: 0.35rem 0.55rem;
  font-size: 0.74rem;
}

.issue-actions {
  display: flex;
  justify-content: center;
  gap: 0.4rem;
  flex-wrap: wrap;
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
</style>
