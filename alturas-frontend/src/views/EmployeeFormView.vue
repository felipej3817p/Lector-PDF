<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">{{ isEditMode ? 'Edición de persona' : 'Creación de persona' }}</span>
        <h1 class="h1 mb-2">{{ isEditMode ? 'Editar persona' : 'Nueva persona' }}</h1>
        <p class="p mb-0">
          Registra o actualiza la información base del personal para asociar documentos y evaluaciones médicas.
        </p>
      </div>

      <div class="header-actions">
        <RouterLink to="/employees" class="secondary-btn">
          Volver a personas
        </RouterLink>
      </div>
    </div>

    <div v-if="loading" class="state-box info">
      Cargando información de la persona...
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span class="label">Modo</span>
        <span>{{ isEditMode ? 'Edición' : 'Creación' }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Documento</span>
        <span>{{ form.documentType || '-' }} {{ form.documentNumber || '' }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Nombre visible</span>
        <span>{{ fullNamePreview }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Zona o área</span>
        <span>{{ form.areaCode || '-' }}</span>
      </div>

    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Datos básicos</h2>
            <p class="helper-text mb-0">
              Completa la identificación, datos personales y datos operativos de la persona.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <form class="form-grid" @submit.prevent="handleSubmit">
          <div class="form-field">
            <label class="label" for="documentType">Tipo de documento</label>
            <select
              id="documentType"
              v-model="form.documentType"
              class="form-select"
              :disabled="loading || saving"
            >
              <option value="">Sin zona asignada</option>
              <option value="CC">CC</option>
              <option value="CE">CE</option>
              <option value="TI">TI</option>
              <option value="PASAPORTE">PASAPORTE</option>
              <option value="OTRO">OTRO</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="documentNumber">Número de documento</label>
            <input
              id="documentNumber"
              v-model.trim="form.documentNumber"
              type="text"
              class="form-control"
              placeholder="Ejemplo: 123456789"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="firstName">Primer nombre</label>
            <input
              id="firstName"
              v-model.trim="form.firstName"
              type="text"
              class="form-control"
              placeholder="Primer nombre"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="secondName">Segundo nombre</label>
            <input
              id="secondName"
              v-model.trim="form.secondName"
              type="text"
              class="form-control"
              placeholder="Segundo nombre"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="firstLastName">Primer apellido</label>
            <input
              id="firstLastName"
              v-model.trim="form.firstLastName"
              type="text"
              class="form-control"
              placeholder="Primer apellido"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="secondLastName">Segundo apellido</label>
            <input
              id="secondLastName"
              v-model.trim="form.secondLastName"
              type="text"
              class="form-control"
              placeholder="Segundo apellido"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="gender">Género</label>
            <select
              id="gender"
              v-model="form.gender"
              class="form-select"
              :disabled="loading || saving"
            >
              <option value="">Seleccione</option>
              <option value="M">M</option>
              <option value="F">F</option>
              <option value="OTRO">OTRO</option>
              <option value="NO_REPORTA">NO_REPORTA</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="birthDate">Fecha de nacimiento</label>
            <input
              id="birthDate"
              v-model="form.birthDate"
              type="date"
              class="form-control"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="email">Correo</label>
            <input
              id="email"
              v-model.trim="form.email"
              type="email"
              class="form-control"
              placeholder="correo@empresa.com"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="currentPosition">Cargo</label>
            <input
              id="currentPosition"
              v-model.trim="form.currentPosition"
              type="text"
              class="form-control"
              placeholder="Cargo actual"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="areaCode">Zona o área de trabajo</label>
            <select
              id="areaCode"
              v-model="form.areaCode"
              class="form-select"
              :disabled="loading || saving"
            >
              <option value="">Seleccione</option>
              <option v-for="area in areaOptions" :key="area" :value="area">
                {{ areaLabel(area) || area }}
              </option>
            </select>
            <small v-if="!auth.isAdmin" class="helper-text">
              Solo puedes asignar personas a tus zonas o áreas permitidas.
            </small>
          </div>

          <div class="form-field">
            <label class="label" for="zone">Zona</label>
            <input
              id="zone"
              v-model.trim="form.zone"
              type="text"
              class="form-control"
              placeholder="Zona operativa"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="employer">Empleador</label>
            <input
              id="employer"
              v-model.trim="form.employer"
              type="text"
              class="form-control"
              placeholder="Empresa o contratista"
              :disabled="loading || saving"
            />
          </div>

          <div class="form-field">
            <label class="label" for="arl">ARL</label>
            <input
              id="arl"
              v-model.trim="form.arl"
              type="text"
              class="form-control"
              placeholder="ARL"
              :disabled="loading || saving"
            />
          </div>

          <div class="full-span actions-row">
            <RouterLink to="/employees" class="secondary-btn">
              Cancelar
            </RouterLink>

            <button
              type="button"
              class="secondary-btn"
              :disabled="loading || saving"
              @click="resetForm"
            >
              {{ isEditMode ? 'Restaurar datos' : 'Limpiar formulario' }}
            </button>

            <button
              type="submit"
              class="btn btn-primary"
              :disabled="loading || saving"
            >
              <span
                v-if="saving"
                class="spinner-border spinner-border-sm me-2"
                aria-hidden="true"
              ></span>
              {{ saving ? 'Guardando...' : isEditMode ? 'Actualizar persona' : 'Crear persona' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="isEditMode" class="card border-0 mt-3">
      <div class="card-body">
        <div class="page-header border-0 pb-0"><div><h2 class="h4 mb-1">Historial de evaluaciones de trabajo en alturas</h2></div></div>
        <div class="hr"></div>
        <div v-if="historyLoading" class="state-box info">Cargando historial...</div>
        <div v-else-if="!historyRows.length" class="state-box">Sin evaluaciones registradas.</div>
        <div v-else class="table-responsive">
          <table class="table table-sm align-middle"><thead><tr>
            <th>Fecha carga</th><th>Archivo</th><th>Resultado médico</th><th>Estado revisión</th><th>Aprobado por</th><th>Fecha aprobación</th><th>Estado notificación</th><th>Fecha notificación</th><th>Detalle</th>
          </tr></thead><tbody>
            <tr v-for="row in historyRows" :key="row.id">
              <td>{{ formatDate(row.uploadedAt) }}</td><td>{{ row.originalFileName || '-' }}</td><td>{{ row.resultStatus }}</td><td>{{ reviewLabel(row.reviewStatus) }}</td><td>{{ row.reviewedBy || '-' }}</td><td>{{ formatDate(row.reviewedAt) }}</td><td>{{ notificationLabel(row.notificationStatus) }}</td><td>{{ formatDate(row.notifiedAt) }}</td>
              <td><RouterLink :to="`/documents/${row.id}`" class="secondary-btn">Ver</RouterLink></td>
            </tr>
          </tbody></table>
        </div>
      </div>
    </div>

  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { createEmployee, getEmployeeById, updateEmployee } from '../api/employee'
import { getDocumentsByEmployeeId } from '../api/document'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { AREA_CODES, areaLabel, normalizeAreaCode } from '../utils/areaCatalog'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const employeeId = computed(() => route.params.id)
const isEditMode = computed(() => Boolean(employeeId.value))

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const historyRows = ref([])
const historyLoading = ref(false)

const allAreas = AREA_CODES

const areaOptions = computed(() => {
  if (auth.isAdmin) return allAreas
  return Array.isArray(auth.allowedAreas) && auth.allowedAreas.length
    ? auth.allowedAreas.map(normalizeAreaCode).filter(Boolean)
    : []
})

const initialFormState = () => ({
  documentType: '',
  documentNumber: '',
  firstName: '',
  secondName: '',
  firstLastName: '',
  secondLastName: '',
  gender: '',
  birthDate: '',
  workArea: '',
  currentPosition: '',
  employer: '',
  arl: '',
  email: '',
  zone: '',
  areaCode: auth.isAdmin
    ? ''
    : (Array.isArray(auth.allowedAreas) && auth.allowedAreas.length === 1 ? normalizeAreaCode(auth.allowedAreas[0]) : ''),
  active: true
})

const form = reactive(initialFormState())

const fullNamePreview = computed(() => {
  const fullName = [
    form.firstName,
    form.secondName,
    form.firstLastName,
    form.secondLastName
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return fullName || 'Sin nombre digitado'
})


const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('es-CO')
}

const normalizeDateForPayload = (value) => {
  const text = String(value || '').trim()
  if (!text) return ''

  const isoDate = text.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (isoDate) return text

  const localDate = text.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/)
  if (localDate) {
    const day = localDate[1].padStart(2, '0')
    const month = localDate[2].padStart(2, '0')
    return `${localDate[3]}-${month}-${day}`
  }

  return text
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

const setFormData = (data = {}) => {
  form.documentType = data.documentType || ''
  form.documentNumber = data.documentNumber || ''
  form.firstName = data.firstName || ''
  form.secondName = data.secondName || ''
  form.firstLastName = data.firstLastName || ''
  form.secondLastName = data.secondLastName || ''
  form.gender = data.gender || ''
  form.birthDate = data.birthDate || ''
  form.workArea = data.workArea || ''
  form.currentPosition = data.currentPosition || ''
  form.employer = data.employer || ''
  form.arl = data.arl || ''
  form.email = data.email || ''
  form.zone = data.zone || ''
  form.areaCode = normalizeAreaCode(data.areaCode) || ''
  form.active = typeof data.active === 'boolean' ? data.active : true
}

const validateForm = () => {
  error.value = ''

  if (!form.documentType.trim()) {
    error.value = 'El tipo de documento es obligatorio.'
    return false
  }

  if (!form.documentNumber.trim()) {
    error.value = 'El número de documento es obligatorio.'
    return false
  }

  if (!form.firstName.trim()) {
    error.value = 'El primer nombre es obligatorio.'
    return false
  }

  if (!form.firstLastName.trim()) {
    error.value = 'El primer apellido es obligatorio.'
    return false
  }

  if (!form.email.trim()) {
    error.value = 'El correo es obligatorio.'
    return false
  }

  if (form.areaCode && !auth.isAdmin && !areaOptions.value.includes(normalizeAreaCode(form.areaCode))) {
    error.value = 'No tienes permiso para asignar personas a esa área.'
    return false
  }

  return true
}

const buildPayload = () => ({
  documentType: form.documentType.trim(),
  documentNumber: form.documentNumber.trim(),
  firstName: form.firstName.trim(),
  secondName: form.secondName.trim(),
  firstLastName: form.firstLastName.trim(),
  secondLastName: form.secondLastName.trim(),
  gender: form.gender,
  birthDate: normalizeDateForPayload(form.birthDate),
  workArea: form.workArea.trim(),
  currentPosition: form.currentPosition.trim(),
  employer: form.employer.trim(),
  arl: form.arl.trim(),
  email: form.email.trim(),
  zone: form.zone.trim(),
  areaCode: normalizeAreaCode(form.areaCode) || null,
  active: form.active
})

const loadEmployee = async () => {
  if (!isEditMode.value) return

  try {
    loading.value = true
    error.value = ''

    const { data } = await getEmployeeById(employeeId.value)
    setFormData(data)
    await loadHistory()
  } catch (err) {
    error.value = 'No se pudo cargar la persona.'
    console.error('Error cargando persona:', err)
  } finally {
    loading.value = false
  }
}


const loadHistory = async () => {
  if (!isEditMode.value) return
  try {
    historyLoading.value = true
    const { data } = await getDocumentsByEmployeeId(employeeId.value)
    const docs = Array.isArray(data) ? data : []
    const analysisReq = await Promise.allSettled(docs.map((d) => http.get(`/api/documents/${d.id}/analysis`)))
    historyRows.value = docs.map((doc, idx) => {
      const analysis = analysisReq[idx]?.status === 'fulfilled' ? analysisReq[idx].value.data : null
      return { ...doc, resultStatus: ['APTO','NO_APTO'].includes(analysis?.resultStatus) ? analysis.resultStatus : 'PENDIENTE' }
    })
  } finally { historyLoading.value = false }
}

const resetForm = () => {
  error.value = ''

  if (isEditMode.value) {
    loadEmployee()
    return
  }

  setFormData(initialFormState())
}

const handleSubmit = async () => {
  if (loading.value || saving.value) return
  if (!validateForm()) return

  try {
    saving.value = true
    error.value = ''

    const payload = buildPayload()

    if (isEditMode.value) {
      await updateEmployee(employeeId.value, payload)
    } else {
      await createEmployee(payload)
    }

    router.push('/employees')
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo guardar la persona.'
    console.error('Error guardando persona:', err)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadEmployee()
})
</script>
