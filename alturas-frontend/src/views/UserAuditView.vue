<template>
  <section class="page audit-page">
    <div class="dashboard-toolbar">
      <div class="audit-title-block">
        <span class="mini-title">Administracion de usuarios</span>
        <h1 class="h1 mb-2">Auditoria general de usuarios</h1>
        <p class="p mb-0">
          Consulta centralizada de altas, cambios de acceso, roles, zonas y vigencias.
        </p>
      </div>

      <div class="header-actions">
        <div class="audit-inline-metrics" aria-label="Resumen de auditoria">
          <span><strong>{{ usersCount }}</strong> usuarios</span>
          <span><strong>{{ totalRecords }}</strong> cambios</span>
          <span><strong>{{ currentPage }}/{{ totalPages }}</strong> pagina</span>
        </div>
        <RouterLink to="/settings" class="secondary-btn">Volver a configuracion</RouterLink>
        <button type="button" class="primary-btn" :disabled="loading" @click="loadAudit(1)">
          {{ loading ? 'Actualizando...' : 'Actualizar' }}
        </button>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body compact-card-body">
        <div class="audit-filter-heading">
          <div>
            <span class="mini-title">Criterios de consulta</span>
            <h2 class="h4 mb-1">Filtros de auditoria</h2>
          </div>
          <button type="button" class="secondary-btn" @click="clearFilters">Limpiar filtros</button>
        </div>

        <form class="audit-filters" @submit.prevent="loadAudit(1)">
          <div class="form-field">
            <label class="label" for="auditUsername">Usuario afectado</label>
            <input id="auditUsername" v-model.trim="filters.username" class="form-control" placeholder="Ej. pruebas" />
          </div>
          <div class="form-field">
            <label class="label" for="auditModifiedBy">Modificado por</label>
            <input id="auditModifiedBy" v-model.trim="filters.modifiedBy" class="form-control" placeholder="Ej. admin" />
          </div>
          <div class="form-field">
            <label class="label" for="auditDateFrom">Desde</label>
            <input id="auditDateFrom" v-model="filters.dateFrom" type="datetime-local" class="form-control" />
          </div>
          <div class="form-field">
            <label class="label" for="auditDateTo">Hasta</label>
            <input id="auditDateTo" v-model="filters.dateTo" type="datetime-local" class="form-control" />
          </div>
          <div class="form-field audit-filter-submit">
            <button type="submit" class="primary-btn" :disabled="loading">Consultar</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="error" class="state-box error">{{ error }}</div>
    <div v-if="loading" class="state-box info">Cargando auditoria...</div>
    <div v-else-if="!logs.length" class="state-box">No hay registros para los criterios seleccionados.</div>

    <div v-else class="card border-0 audit-results">
      <div class="card-body compact-card-body">
        <div class="audit-results-heading">
          <div>
            <span class="mini-title">Trazabilidad</span>
            <h2 class="h4 mb-1">Registro de cambios de usuarios</h2>
          </div>
          <small>Ordenado del cambio mas reciente al mas antiguo. Se muestran hasta {{ PAGE_SIZE }} cambios por pagina.</small>
        </div>

        <div class="table-responsive audit-table-container">
          <table class="table table-hover align-middle audit-table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Fecha</th>
                <th>Hora</th>
                <th>Modificado por</th>
                <th>Accion</th>
                <th>Informacion modificada</th>
                <th>Valor anterior</th>
                <th>Valor nuevo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in logs" :key="log.id">
                <td><strong>{{ log.username || '-' }}</strong></td>
                <td>{{ formatDate(log.changedAt) }}</td>
                <td>{{ formatTime(log.changedAt) }}</td>
                <td>{{ log.modifiedBy || '-' }}</td>
                <td>{{ formatAction(log) }}</td>
                <td>{{ formatField(log.field) }}</td>
                <td>{{ formatValue(log.previousValue, log.field) }}</td>
                <td>{{ formatValue(log.newValue, log.field) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar google-pagination audit-pagination">
          <div class="pagination-info">
            {{ pageSummary }}
          </div>
          <div class="pagination-actions">
            <button type="button" class="page-nav" :disabled="currentPage === 1" @click="loadAudit(currentPage - 1)">
              Anterior
            </button>

            <template v-for="(item, index) in visiblePageItems" :key="`${item}-${index}`">
              <span v-if="item === '...'" class="page-ellipsis">...</span>
              <button
                v-else
                type="button"
                class="page-number"
                :class="{ active: item === currentPage }"
                @click="loadAudit(item)"
              >
                {{ item }}
              </button>
            </template>

            <button type="button" class="page-nav" :disabled="currentPage === totalPages" @click="loadAudit(currentPage + 1)">
              Siguiente
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'
import { areaLabel } from '../utils/areaCatalog'

const PAGE_SIZE = 50
const logs = ref([])
const usersCount = ref(0)
const totalRecords = ref(0)
const currentPage = ref(1)
const loading = ref(false)
const error = ref('')

const filters = reactive({
  username: '',
  modifiedBy: '',
  dateFrom: '',
  dateTo: ''
})

const fieldOptions = [
  { value: 'usuario', label: 'Cuenta de usuario' },
  { value: 'username', label: 'Nombre de usuario' },
  { value: 'email', label: 'Correo electronico' },
  { value: 'roles', label: 'Rol asignado' },
  { value: 'roleAssignments', label: 'Rol y vigencia' },
  { value: 'allowedAreas', label: 'Zonas permitidas' },
  { value: 'areaAssignments', label: 'Zona y vigencia' },
  { value: 'globalAreaAccess', label: 'Acceso global a zonas' },
  { value: 'enabled', label: 'Estado de acceso' },
  { value: 'fechaInicioAcceso', label: 'Inicio de vigencia' },
  { value: 'fechaFinAcceso', label: 'Fin de vigencia' },
  { value: 'password', label: 'Contraseña' }
]

const totalPages = computed(() => Math.max(1, Math.ceil(totalRecords.value / PAGE_SIZE)))

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

const pageSummary = computed(() => {
  if (!totalRecords.value) return 'Sin registros'
  const start = (currentPage.value - 1) * PAGE_SIZE + 1
  const end = Math.min(currentPage.value * PAGE_SIZE, totalRecords.value)
  return `Mostrando ${start}-${end} de ${totalRecords.value} cambios`
})

const displayRole = (role) => role === 'SUPER_ADMIN' ? 'ADMIN' : String(role || '-')
const formatDate = (value) => value ? new Date(value).toLocaleDateString('es-CO') : '-'
const formatTime = (value) => value ? new Date(value).toLocaleTimeString('es-CO') : '-'

const formatAction = (log) => {
  const direct = {
    CREATED: 'Usuario creado',
    ACTIVATED: 'Usuario activado',
    DEACTIVATED: 'Usuario inactivado',
    DELETED: 'Usuario eliminado',
    ROLE_ASSIGNED: 'Rol agregado al usuario',
    ROLE_UPDATED: 'Vigencia de rol modificada',
    ROLE_ENABLED: 'Rol habilitado',
    ROLE_DISABLED: 'Rol inhabilitado',
    ROLE_REMOVED: 'Rol retirado del usuario',
    AREA_ASSIGNED: 'Zona agregada al usuario',
    AREA_UPDATED: 'Vigencia de zona modificada',
    AREA_ENABLED: 'Zona habilitada',
    AREA_DISABLED: 'Zona inhabilitada',
    AREA_REMOVED: 'Zona retirada del usuario'
  }
  if (direct[log?.action]) return direct[log.action]
  const assigned = {
    email: 'Correo electronico asignado',
    roles: 'Rol asignado',
    roleAssignments: 'Rol con vigencia asignado',
    allowedAreas: 'Zonas permitidas asignadas',
    areaAssignments: 'Zona con vigencia asignada',
    globalAreaAccess: 'Acceso a zonas definido',
    enabled: 'Estado inicial definido',
    fechaInicioAcceso: 'Fecha inicial de vigencia definida',
    fechaFinAcceso: 'Fecha final de vigencia definida'
  }
  if (log?.action === 'ASSIGNED') return assigned[log.field] || 'Informacion inicial registrada'
  const updated = {
    username: 'Nombre de usuario modificado',
    email: 'Correo electronico modificado',
    roles: 'Rol modificado',
    roleAssignments: 'Rol o vigencia modificada',
    allowedAreas: 'Zonas permitidas modificadas',
    areaAssignments: 'Zona o vigencia modificada',
    globalAreaAccess: 'Acceso a zonas modificado',
    enabled: log?.newValue ? 'Usuario activado' : 'Usuario inactivado',
    fechaInicioAcceso: 'Fecha inicial de vigencia modificada',
    fechaFinAcceso: 'Fecha final de vigencia modificada',
    password: 'Contraseña modificada'
  }
  return updated[log?.field] || 'Informacion de la cuenta modificada'
}

const formatField = (field) => fieldOptions.find((option) => option.value === field)?.label || 'Informacion de la cuenta'

const formatValue = (value, field) => {
  if (value === null || value === undefined || value === '') return 'Sin informacion'
  if (field === 'enabled') return value ? 'Usuario activo' : 'Usuario inactivo'
  if (field === 'globalAreaAccess') return value ? 'Acceso permitido' : 'Acceso restringido'
  if (field === 'password') return 'Contraseña protegida'
  if (field === 'fechaInicioAcceso' || field === 'fechaFinAcceso') return new Date(value).toLocaleString('es-CO')
  if (Array.isArray(value)) {
    if (!value.length) return 'Sin asignacion'
    if (field === 'allowedAreas') return value.map((area) => areaLabel(area) || area).sort().join(', ')
    if (field === 'roles') return value.map(displayRole).sort().join(', ')
    if (field === 'roleAssignments') return value.map(formatRoleAssignment).join(' | ')
    if (field === 'areaAssignments') return value.map(formatAreaAssignment).join(' | ')
    return value.map(formatUnknownValue).join(', ')
  }
  if (typeof value === 'object') {
    if (field === 'roleAssignments') return formatRoleAssignment(value)
    if (field === 'areaAssignments') return formatAreaAssignment(value)
    return formatUnknownValue(value)
  }
  return String(value)
}

const formatAssignmentDate = (value, fallback) => value ? new Date(value).toLocaleString('es-CO') : fallback

const formatRoleAssignment = (value) => {
  if (typeof value === 'string') return value
  const role = displayRole(value?.role)
  const status = value?.enabled === false ? 'inhabilitado' : 'habilitado'
  const start = formatAssignmentDate(value?.startDate, 'acceso inmediato')
  const end = formatAssignmentDate(value?.endDate, 'sin fecha limite')
  return `Rol ${role}: ${status}, desde ${start}, hasta ${end}`
}

const formatAreaAssignment = (value) => {
  if (typeof value === 'string') return value
  const area = areaLabel(value?.areaCode) || value?.areaCode || '-'
  const status = value?.enabled === false ? 'inhabilitada' : 'habilitada'
  const start = formatAssignmentDate(value?.startDate, 'acceso inmediato')
  const end = formatAssignmentDate(value?.endDate, 'sin fecha limite')
  return `Zona ${area}: ${status}, desde ${start}, hasta ${end}`
}

const formatUnknownValue = (value) => {
  if (value === null || value === undefined || value === '') return 'Sin informacion'
  if (typeof value !== 'object') return String(value)
  return Object.entries(value)
    .map(([key, entryValue]) => `${key}: ${formatUnknownValue(entryValue)}`)
    .join(', ')
}

const buildParams = (page) => ({
  page: Math.max(0, page - 1),
  size: PAGE_SIZE,
  username: filters.username || undefined,
  modifiedBy: filters.modifiedBy || undefined,
  dateFrom: filters.dateFrom || undefined,
  dateTo: filters.dateTo || undefined
})

const loadAudit = async (page = 1) => {
  try {
    loading.value = true
    error.value = ''
    const { data } = await http.get('/api/users/audit-logs', { params: buildParams(page) })
    logs.value = Array.isArray(data?.content) ? data.content : []
    totalRecords.value = Number(data?.totalElements || 0)
    currentPage.value = Number(data?.number || 0) + 1
  } catch (e) {
    error.value = e?.response?.data?.message || 'No fue posible consultar la auditoria.'
  } finally {
    loading.value = false
  }
}

const clearFilters = () => {
  Object.assign(filters, { username: '', modifiedBy: '', dateFrom: '', dateTo: '' })
  loadAudit(1)
}

onMounted(async () => {
  const [{ data }] = await Promise.all([http.get('/api/users'), loadAudit(1)])
  usersCount.value = Array.isArray(data) ? data.length : 0
})
</script>

<style scoped>
.audit-page { gap: 0.75rem; min-height: calc(100vh - 1.5rem); }
.audit-page .dashboard-toolbar { align-items: center; padding: 0.9rem 1.1rem; }
.audit-title-block { min-width: 280px; }
.audit-results-heading small { color: var(--text-muted); }
.audit-inline-metrics {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.4rem;
}
.audit-inline-metrics span {
  display: inline-flex;
  align-items: baseline;
  gap: 0.28rem;
  min-height: 32px;
  padding: 0.35rem 0.6rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-soft);
  color: var(--text-muted);
  font-size: 0.74rem;
  font-weight: 760;
  white-space: nowrap;
}
.audit-inline-metrics strong {
  color: var(--text);
  font-size: 0.9rem;
  font-weight: 850;
}
.audit-filter-heading, .audit-results-heading { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; }
.audit-filters { display: grid; grid-template-columns: repeat(5, minmax(160px, 1fr)); gap: 0.75rem; align-items: end; }
.audit-filter-submit { display: flex; justify-content: flex-end; }
.audit-filter-submit .primary-btn { min-width: 132px; }
.audit-results { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.audit-results > .compact-card-body { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.audit-results .compact-card-body,
.audit-page .compact-card-body { padding: 0.95rem; }
.audit-table-container { flex: 1; max-height: calc(100vh - 255px); min-height: 580px; overflow: auto; border: 1px solid var(--border); border-radius: 16px; }
.audit-table { min-width: 1580px; font-size: 0.82rem; margin-bottom: 0; }
.audit-table thead { position: sticky; top: 0; z-index: 1; }
.audit-table :deep(th),
.audit-table :deep(td) { padding: 0.68rem 0.78rem; vertical-align: top; }
.audit-table :deep(th) { padding: 0.72rem 0.78rem; vertical-align: top; white-space: nowrap; }
.audit-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: 0.65rem;
  padding-top: 0.65rem;
  border-top: 1px solid var(--border);
}
.pagination-info {
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 800;
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
.page-nav:disabled,
.page-number:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.page-ellipsis {
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 800;
  padding: 0 0.12rem;
}
@media (max-width: 1280px) {
  .audit-filters { grid-template-columns: repeat(3, minmax(180px, 1fr)); }
  .audit-table-container { min-height: 430px; }
}
@media (max-width: 900px) {
  .audit-page .dashboard-toolbar { align-items: stretch; }
  .audit-inline-metrics { justify-content: flex-start; }
  .audit-filters { grid-template-columns: 1fr; }
  .audit-filter-submit { justify-content: stretch; }
  .audit-filter-submit .primary-btn { width: 100%; }
  .audit-pagination { align-items: stretch; flex-direction: column; }
  .pagination-actions { justify-content: space-between; }
}
</style>

