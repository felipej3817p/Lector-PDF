<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Configuración</span>
        <h1 class="h1 mb-2">Configuración operativa</h1>
        <p class="p mb-0">
          Gestiona trabajadores nuevos, revisa tus áreas asignadas y solicita acceso a otras zonas.
        </p>
      </div>

      <div class="header-actions">
        <RouterLink to="/documents" class="secondary-btn">
          Volver a documentos
        </RouterLink>
      </div>
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div v-if="successMessage" class="state-box info">
      {{ successMessage }}
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span class="label">Usuario</span>
        <span>{{ auth.user?.username || '-' }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Rol</span>
        <span>{{ auth.isSuperAdmin ? 'SUPER_ADMIN' : 'OPERADOR' }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Áreas asignadas</span>
        <span>{{ visibleAreas }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Solicitudes pendientes</span>
        <span>{{ auth.isSuperAdmin ? pendingRequests.length : myPendingCount }}</span>
      </div>
    </div>

    <div class="settings-grid">
      <div class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Mis áreas</h2>
              <p class="helper-text mb-0">
                Áreas donde puedes ver trabajadores, cargar documentos y operar información.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <div v-if="auth.isSuperAdmin" class="state-box info mb-0">
            Tienes acceso global como SUPER_ADMIN.
          </div>

          <div v-else-if="auth.allowedAreas.length" class="area-list">
            <span
              v-for="area in auth.allowedAreas"
              :key="area"
              class="area-pill"
            >
              {{ area }}
            </span>
          </div>

          <div v-else class="state-box error mb-0">
            Este usuario no tiene áreas asignadas.
          </div>
        </div>
      </div>

      <div v-if="!auth.isSuperAdmin" class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Solicitar acceso a zona</h2>
              <p class="helper-text mb-0">
                Envía una solicitud al administrador para acceder a una nueva zona.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <form class="form-grid" @submit.prevent="submitAccessRequest">
            <div class="form-field full-span">
              <label class="label" for="requestedArea">Zona solicitada</label>
              <select
                id="requestedArea"
                v-model="accessForm.requestedArea"
                class="form-select"
                :disabled="savingRequest"
              >
                <option value="">Seleccione</option>
                <option
                  v-for="area in requestableAreas"
                  :key="area"
                  :value="area"
                >
                  {{ area }}
                </option>
              </select>
            </div>

            <div class="form-field full-span">
              <label class="label" for="reason">Motivo</label>
              <textarea
                id="reason"
                v-model.trim="accessForm.reason"
                class="form-control"
                rows="4"
                placeholder="Ejemplo: necesito cargar trabajadores nuevos de esta zona."
                :disabled="savingRequest"
              ></textarea>
            </div>

            <div class="full-span actions-row">
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="savingRequest || !accessForm.requestedArea"
              >
                {{ savingRequest ? 'Enviando...' : 'Enviar solicitud' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <div class="card border-0 mt-4">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Agregar trabajador nuevo</h2>
            <p class="helper-text mb-0">
              Crea trabajadores nuevos. El operador solo puede asignarlos a sus áreas permitidas.
            </p>
          </div>

          <RouterLink to="/employees/new" class="primary-btn">
            Agregar trabajador
          </RouterLink>
        </div>

        <div class="hr"></div>

        <div class="state-box info mb-0">
          El trabajador creado quedará disponible para carga automática por cédula desde los PDFs.
        </div>
      </div>
    </div>

    <div class="card border-0 mt-4">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Mis solicitudes</h2>
            <p class="helper-text mb-0">
              Historial de solicitudes de acceso enviadas por tu usuario.
            </p>
          </div>

          <button
            type="button"
            class="secondary-btn"
            :disabled="loading"
            @click="loadData"
          >
            {{ loading ? 'Actualizando...' : 'Actualizar' }}
          </button>
        </div>

        <div class="hr"></div>

        <div v-if="!myRequests.length" class="state-box mb-0">
          No tienes solicitudes registradas.
        </div>

        <div v-else class="table-responsive">
          <table class="table table-sm align-middle">
            <thead>
              <tr>
                <th>Área</th>
                <th>Estado</th>
                <th>Motivo</th>
                <th>Comentario admin</th>
                <th>Fecha</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="request in myRequests" :key="request.id">
                <td>{{ request.requestedArea }}</td>
                <td>
                  <span :class="statusClass(request.status)">
                    {{ statusLabel(request.status) }}
                  </span>
                </td>
                <td>{{ request.reason || '-' }}</td>
                <td>{{ request.adminComment || '-' }}</td>
                <td>{{ formatDate(request.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="auth.isSuperAdmin" class="card border-0 mt-4">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Solicitudes pendientes de aprobación</h2>
            <p class="helper-text mb-0">
              Aprueba o rechaza solicitudes de operadores para acceder a nuevas zonas.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div v-if="!pendingRequests.length" class="state-box mb-0">
          No hay solicitudes pendientes.
        </div>

        <div v-else class="table-responsive">
          <table class="table table-sm align-middle">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Área solicitada</th>
                <th>Motivo</th>
                <th>Fecha</th>
                <th>Comentario</th>
                <th class="text-center">Acción</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="request in pendingRequests" :key="request.id">
                <td>{{ request.requestedByUsername || '-' }}</td>
                <td>{{ request.requestedArea }}</td>
                <td>{{ request.reason || '-' }}</td>
                <td>{{ formatDate(request.createdAt) }}</td>
                <td>
                  <input
                    v-model.trim="reviewComments[request.id]"
                    type="text"
                    class="form-control form-control-sm"
                    placeholder="Comentario opcional"
                  />
                </td>
                <td>
                  <div class="actions justify-content-center">
                    <button
                      type="button"
                      class="secondary-btn"
                      :disabled="reviewingId === request.id"
                      @click="approveRequest(request.id)"
                    >
                      Aprobar
                    </button>

                    <button
                      type="button"
                      class="secondary-btn danger-btn"
                      :disabled="reviewingId === request.id"
                      @click="rejectRequest(request.id)"
                    >
                      Rechazar
                    </button>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  approveAccessRequest,
  createAccessRequest,
  getMyAccessRequests,
  getPendingAccessRequests,
  rejectAccessRequest
} from '../api/accessRequest'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const allAreas = [
  'CENTRO',
  'NORTE',
  'OCCIDENTE',
  'ORIENTE',
  'PUERTO',
  'RICAURTE',
  'SUGAMUXI',
  'TUNDAMA',
  'EDIFICIO'
]

const loading = ref(false)
const savingRequest = ref(false)
const reviewingId = ref('')
const error = ref('')
const successMessage = ref('')

const myRequests = ref([])
const pendingRequests = ref([])
const reviewComments = reactive({})

const accessForm = reactive({
  requestedArea: '',
  reason: ''
})

const visibleAreas = computed(() => {
  if (auth.isSuperAdmin) return 'Acceso global'
  return auth.allowedAreas.length ? auth.allowedAreas.join(', ') : 'Sin áreas asignadas'
})

const requestableAreas = computed(() => {
  if (auth.isSuperAdmin) return allAreas
  return allAreas.filter((area) => !auth.allowedAreas.includes(area))
})

const myPendingCount = computed(() =>
  myRequests.value.filter((request) => request.status === 'PENDING').length
)

const loadData = async () => {
  try {
    loading.value = true
    error.value = ''

    const myResponse = await getMyAccessRequests()
    myRequests.value = Array.isArray(myResponse.data) ? myResponse.data : []

    if (auth.isSuperAdmin) {
      const pendingResponse = await getPendingAccessRequests()
      pendingRequests.value = Array.isArray(pendingResponse.data) ? pendingResponse.data : []
    } else {
      pendingRequests.value = []
    }
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar la configuración.'
  } finally {
    loading.value = false
  }
}

const submitAccessRequest = async () => {
  if (!accessForm.requestedArea) return

  try {
    savingRequest.value = true
    error.value = ''
    successMessage.value = ''

    await createAccessRequest({
      requestedArea: accessForm.requestedArea,
      reason: accessForm.reason
    })

    successMessage.value = 'Solicitud enviada correctamente.'
    accessForm.requestedArea = ''
    accessForm.reason = ''

    await loadData()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo enviar la solicitud.'
  } finally {
    savingRequest.value = false
  }
}

const approveRequest = async (id) => {
  try {
    reviewingId.value = id
    error.value = ''
    successMessage.value = ''

    await approveAccessRequest(id, {
      adminComment: reviewComments[id] || ''
    })

    successMessage.value = 'Solicitud aprobada correctamente.'
    await loadData()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo aprobar la solicitud.'
  } finally {
    reviewingId.value = ''
  }
}

const rejectRequest = async (id) => {
  try {
    reviewingId.value = id
    error.value = ''
    successMessage.value = ''

    await rejectAccessRequest(id, {
      adminComment: reviewComments[id] || ''
    })

    successMessage.value = 'Solicitud rechazada correctamente.'
    await loadData()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo rechazar la solicitud.'
  } finally {
    reviewingId.value = ''
  }
}

const statusLabel = (status) => {
  if (status === 'PENDING') return 'PENDIENTE'
  if (status === 'APPROVED') return 'APROBADA'
  if (status === 'REJECTED') return 'RECHAZADA'
  return status || '-'
}

const statusClass = (status) => {
  if (status === 'APPROVED') return 'status-pill-active'
  if (status === 'REJECTED') return 'status-pill-inactive'
  if (status === 'PENDING') return 'status-pill-warning'
  return 'status-pill-neutral'
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

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
}

.area-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.area-pill {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: #eef2f7;
  color: #334155;
  font-weight: 800;
  font-size: 0.8rem;
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
</style>