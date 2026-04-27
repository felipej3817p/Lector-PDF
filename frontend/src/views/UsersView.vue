<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Gestión de usuarios</span>
        <h1 class="h1 mb-2">Usuarios y permisos</h1>
        <p class="p mb-0">
          Administra credenciales, roles y áreas permitidas del sistema.
        </p>
      </div>

      <div v-if="auth.isSuperAdmin" class="header-actions">
        <button
          type="button"
          class="secondary-btn"
          :disabled="loading"
          @click="loadUsers"
        >
          {{ loading ? 'Actualizando...' : 'Actualizar lista' }}
        </button>

        <button
          type="button"
          class="primary-btn"
          @click="startCreate"
        >
          Nuevo usuario
        </button>
      </div>
    </div>

    <div v-if="!auth.isSuperAdmin" class="state-box error">
      No tienes permisos para administrar usuarios.
    </div>

    <template v-else>
      <div v-if="error" class="state-box error">
        {{ error }}
      </div>

      <div class="kpi-grid">
        <div class="kpi-card">
          <span class="label">Total usuarios</span>
          <strong class="kpi-value">{{ users.length }}</strong>
          <span class="kpi-meta">Usuarios registrados en el sistema</span>
        </div>

        <div class="kpi-card">
          <span class="label">Super administradores</span>
          <strong class="kpi-value">{{ superAdminCount }}</strong>
          <span class="kpi-meta">Acceso global</span>
        </div>

        <div class="kpi-card">
          <span class="label">Operadores</span>
          <strong class="kpi-value">{{ operatorCount }}</strong>
          <span class="kpi-meta">Acceso restringido por áreas</span>
        </div>

        <div class="kpi-card">
          <span class="label">Activos</span>
          <strong class="kpi-value">{{ enabledCount }}</strong>
          <span class="kpi-meta">Disponibles para iniciar sesión</span>
        </div>
      </div>

      <div class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Filtros</h2>
              <p class="helper-text mb-0">
                Busca por usuario, correo, rol o áreas permitidas.
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
              :class="{ active: roleFilter === '' }"
              @click="roleFilter = ''"
            >
              Todos
            </button>

            <button
              type="button"
              class="filter-chip success"
              :class="{ active: roleFilter === 'SUPER_ADMIN' }"
              @click="roleFilter = 'SUPER_ADMIN'"
            >
              Super admin
            </button>

            <button
              type="button"
              class="filter-chip"
              :class="{ active: roleFilter === 'OPERADOR' }"
              @click="roleFilter = 'OPERADOR'"
            >
              Operadores
            </button>

            <button
              type="button"
              class="filter-chip success"
              :class="{ active: enabledFilter === 'true' }"
              @click="enabledFilter = enabledFilter === 'true' ? '' : 'true'"
            >
              Activos
            </button>

            <button
              type="button"
              class="filter-chip danger"
              :class="{ active: enabledFilter === 'false' }"
              @click="enabledFilter = enabledFilter === 'false' ? '' : 'false'"
            >
              Inactivos
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
                placeholder="Usuario, correo, rol, área..."
                :disabled="loading"
              />
            </div>

            <div class="form-field">
              <label class="label" for="roleFilter">Rol</label>
              <select
                id="roleFilter"
                v-model="roleFilter"
                class="form-select"
                :disabled="loading"
              >
                <option value="">Todos</option>
                <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                <option value="OPERADOR">OPERADOR</option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="enabledFilter">Estado</label>
              <select
                id="enabledFilter"
                v-model="enabledFilter"
                class="form-select"
                :disabled="loading"
              >
                <option value="">Todos</option>
                <option value="true">ACTIVO</option>
                <option value="false">INACTIVO</option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="areaFilter">Área</label>
              <select
                id="areaFilter"
                v-model="areaFilter"
                class="form-select"
                :disabled="loading"
              >
                <option value="">Todas</option>
                <option v-for="area in areaOptions" :key="area" :value="area">
                  {{ area }}
                </option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showForm" class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">{{ editingId ? 'Editar usuario' : 'Nuevo usuario' }}</h2>
              <p class="helper-text mb-0">
                Define el rol y las áreas permitidas del usuario.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <div v-if="formError" class="state-box error">
            {{ formError }}
          </div>

          <form class="form-grid" @submit.prevent="saveUser">
            <div class="form-field">
              <label class="label" for="username">Usuario</label>
              <input
                id="username"
                v-model.trim="form.username"
                type="text"
                class="form-control"
                :disabled="saving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="email">Correo</label>
              <input
                id="email"
                v-model.trim="form.email"
                type="email"
                class="form-control"
                :disabled="saving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="password">
                Contraseña {{ editingId ? '(opcional)' : '' }}
              </label>
              <input
                id="password"
                v-model="form.password"
                type="password"
                class="form-control"
                :disabled="saving"
              />
            </div>

            <div class="form-field">
              <label class="label">Rol</label>
              <div class="field-card d-flex flex-column gap-2">
                <label class="checkbox-field">
                  <input
                    type="radio"
                    name="role"
                    value="SUPER_ADMIN"
                    :checked="form.roles.includes('SUPER_ADMIN')"
                    :disabled="saving"
                    @change="selectRole('SUPER_ADMIN')"
                  />
                  <span>SUPER_ADMIN</span>
                </label>

                <label class="checkbox-field">
                  <input
                    type="radio"
                    name="role"
                    value="OPERADOR"
                    :checked="form.roles.includes('OPERADOR')"
                    :disabled="saving"
                    @change="selectRole('OPERADOR')"
                  />
                  <span>OPERADOR</span>
                </label>
              </div>
            </div>

            <div class="form-field full-span">
              <label class="label">Áreas permitidas</label>

              <div class="area-grid">
                <label
                  v-for="area in allAreas"
                  :key="area"
                  class="area-option"
                >
                  <input
                    type="checkbox"
                    :checked="form.allowedAreas.includes(area)"
                    :disabled="saving || form.roles.includes('SUPER_ADMIN')"
                    @change="toggleArea(area)"
                  />
                  <span>{{ area }}</span>
                </label>
              </div>

              <small class="helper-text">
                Si el usuario es <strong>SUPER_ADMIN</strong>, las áreas pueden quedar vacías porque
                tendrá acceso global.
              </small>
            </div>

            <div class="form-field full-span">
              <label class="label">Estado del usuario</label>
              <div class="field-card">
                <label class="checkbox-field">
                  <input
                    v-model="form.enabled"
                    type="checkbox"
                    :disabled="saving"
                  />
                  <span>Usuario habilitado</span>
                </label>
              </div>
            </div>

            <div class="full-span actions-row">
              <button
                type="button"
                class="secondary-btn"
                :disabled="saving"
                @click="closeForm"
              >
                Cancelar
              </button>

              <button
                type="submit"
                class="btn btn-primary"
                :disabled="saving"
              >
                <span
                  v-if="saving"
                  class="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                ></span>
                {{ saving ? 'Guardando...' : editingId ? 'Actualizar usuario' : 'Crear usuario' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <div v-if="loading" class="state-box info">
        Cargando usuarios...
      </div>

      <div v-else-if="!users.length" class="state-box">
        No hay usuarios registrados todavía.
      </div>

      <div v-else class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Usuarios registrados</h2>
              <p class="helper-text mb-0">
                Administración de cuentas con acceso al sistema.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <div class="table-responsive">
            <table class="table table-hover align-middle">
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Correo</th>
                  <th>Rol</th>
                  <th>Áreas</th>
                  <th>Estado</th>
                  <th class="text-center">Acciones</th>
                </tr>
              </thead>

              <tbody>
                <tr v-for="user in filteredUsers" :key="user.id">
                  <td>{{ user.username }}</td>
                  <td>{{ user.email || '-' }}</td>
                  <td>{{ user.roles?.join(', ') || '-' }}</td>
                  <td>
                    <span v-if="user.roles?.includes('SUPER_ADMIN')">TODAS</span>
                    <span v-else>{{ user.allowedAreas?.join(', ') || '-' }}</span>
                  </td>
                  <td>
                    <span :class="user.enabled ? 'status-pill-active' : 'status-pill-inactive'">
                      {{ user.enabled ? 'ACTIVO' : 'INACTIVO' }}
                    </span>
                  </td>
                  <td>
                    <div class="actions justify-content-center">
                      <button
                        type="button"
                        class="secondary-btn"
                        @click="startEdit(user)"
                      >
                        Editar
                      </button>

                      <button
                        type="button"
                        class="danger-btn"
                        :disabled="deletingId === user.id"
                        @click="deleteUser(user.id)"
                      >
                        {{ deletingId === user.id ? 'Eliminando...' : 'Eliminar' }}
                      </button>
                    </div>
                  </td>
                </tr>

                <tr v-if="!filteredUsers.length">
                  <td colspan="6">
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
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const users = ref([])
const loading = ref(false)
const saving = ref(false)
const deletingId = ref('')
const error = ref('')
const formError = ref('')
const showForm = ref(false)
const editingId = ref('')

const search = ref('')
const roleFilter = ref('')
const enabledFilter = ref('')
const areaFilter = ref('')

const allAreas = [
  'CENTRO',
  'NORTE',
  'OCCIDENTE',
  'ORIENTE',
  'PUERTO',
  'RICAURTE',
  'SUGAMUXI',
  'TUNDAMA'
]

const buildInitialForm = () => ({
  username: '',
  email: '',
  password: '',
  roles: ['OPERADOR'],
  allowedAreas: [],
  enabled: true
})

const form = reactive(buildInitialForm())

const normalize = (value) => String(value || '').toLowerCase().trim()

const filteredUsers = computed(() => {
  const term = normalize(search.value)

  return users.value.filter((user) => {
    const userRoles = Array.isArray(user.roles) ? user.roles : []
    const userAreas = Array.isArray(user.allowedAreas) ? user.allowedAreas : []

    const matchesRole = !roleFilter.value || userRoles.includes(roleFilter.value)
    const matchesEnabled =
      !enabledFilter.value || String(Boolean(user.enabled)) === enabledFilter.value
    const matchesArea =
      !areaFilter.value ||
      userRoles.includes('SUPER_ADMIN') ||
      userAreas.includes(areaFilter.value)

    if (!matchesRole || !matchesEnabled || !matchesArea) {
      return false
    }

    if (!term) return true

    const haystack = [
      user.username,
      user.email,
      userRoles.join(' '),
      userAreas.join(' ')
    ]
      .map(normalize)
      .join(' ')

    return haystack.includes(term)
  })
})

const superAdminCount = computed(() =>
  users.value.filter((user) => Array.isArray(user.roles) && user.roles.includes('SUPER_ADMIN')).length
)

const operatorCount = computed(() =>
  users.value.filter((user) => Array.isArray(user.roles) && user.roles.includes('OPERADOR')).length
)

const enabledCount = computed(() =>
  users.value.filter((user) => Boolean(user.enabled)).length
)

const areaOptions = computed(() => {
  const usedAreas = new Set(allAreas)

  users.value.forEach((user) => {
    ;(user.allowedAreas || []).forEach((area) => usedAreas.add(area))
  })

  return [...usedAreas].sort((a, b) => a.localeCompare(b, 'es'))
})

const loadUsers = async () => {
  if (!auth.isSuperAdmin) return

  try {
    loading.value = true
    error.value = ''
    const { data } = await http.get('/api/users')
    users.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e?.response?.data?.message || 'No se pudieron cargar los usuarios.'
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  search.value = ''
  roleFilter.value = ''
  enabledFilter.value = ''
  areaFilter.value = ''
}

const resetForm = () => {
  Object.assign(form, buildInitialForm())
}

const startCreate = () => {
  editingId.value = ''
  formError.value = ''
  resetForm()
  showForm.value = true
}

const startEdit = (user) => {
  editingId.value = user.id
  formError.value = ''
  Object.assign(form, {
    username: user.username || '',
    email: user.email || '',
    password: '',
    roles: Array.isArray(user.roles) ? [...user.roles] : ['OPERADOR'],
    allowedAreas: Array.isArray(user.allowedAreas) ? [...user.allowedAreas] : [],
    enabled: Boolean(user.enabled)
  })
  showForm.value = true
}

const closeForm = () => {
  editingId.value = ''
  formError.value = ''
  resetForm()
  showForm.value = false
}

const selectRole = (role) => {
  form.roles = [role]
  if (role === 'SUPER_ADMIN') {
    form.allowedAreas = []
  }
}

const toggleArea = (area) => {
  if (form.roles.includes('SUPER_ADMIN')) return

  const exists = form.allowedAreas.includes(area)
  form.allowedAreas = exists
    ? form.allowedAreas.filter((item) => item !== area)
    : [...form.allowedAreas, area]
}

const validateForm = () => {
  if (!form.username.trim()) return 'El usuario es obligatorio.'
  if (!form.email.trim()) return 'El correo es obligatorio.'
  if (!editingId.value && !form.password) return 'La contraseña es obligatoria.'
  if (!form.roles.length) return 'Debes seleccionar un rol.'
  if (form.roles.includes('OPERADOR') && form.allowedAreas.length === 0) {
    return 'Un operador debe tener al menos un área asignada.'
  }
  return ''
}

const saveUser = async () => {
  const validationError = validateForm()
  if (validationError) {
    formError.value = validationError
    return
  }

  try {
    saving.value = true
    formError.value = ''
    error.value = ''

    const payload = {
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password || undefined,
      roles: form.roles,
      allowedAreas: form.roles.includes('SUPER_ADMIN') ? [] : form.allowedAreas,
      enabled: form.enabled
    }

    if (!editingId.value) {
      await http.post('/api/users', payload)
    } else {
      await http.put(`/api/users/${editingId.value}`, payload)
    }

    await loadUsers()
    closeForm()
  } catch (e) {
    formError.value = e?.response?.data?.message || 'No se pudo guardar el usuario.'
  } finally {
    saving.value = false
  }
}

const deleteUser = async (id) => {
  const confirmed = window.confirm('¿Seguro que deseas eliminar este usuario?')
  if (!confirmed) return

  try {
    deletingId.value = id
    error.value = ''
    await http.delete(`/api/users/${id}`)
    await loadUsers()
  } catch (e) {
    error.value = e?.response?.data?.message || 'No se pudo eliminar el usuario.'
  } finally {
    deletingId.value = ''
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.area-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.area-option {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.8rem 0.9rem;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface-soft);
}
</style>   