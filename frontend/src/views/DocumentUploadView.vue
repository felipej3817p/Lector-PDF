<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Carga documental</span>
        <h1 class="h1 mb-2">Subir concepto médico PDF</h1>
        <p class="p mb-0">
          El operador carga el PDF y lo asocia a la persona evaluada. La app registra automáticamente
          quién lo subió y ejecuta el análisis al finalizar.
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
        <span class="label">Usuario actual</span>
        <span>{{ auth.user?.username || '-' }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Rol</span>
        <span>{{ auth.isSuperAdmin ? 'SUPER_ADMIN' : 'OPERADOR' }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Áreas visibles</span>
        <span>{{ visibleAreas }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Personas disponibles</span>
        <span>{{ employees.length }}</span>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Datos del cargue</h2>
            <p class="helper-text mb-0">
              Selecciona la persona evaluada, adjunta el PDF y la app analizará el documento al guardarlo.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div v-if="loadingEmployees" class="state-box info">
          Cargando personas disponibles...
        </div>

        <div
          v-else-if="!employees.length"
          class="state-box"
        >
          No hay personas disponibles para asociar documentos en tu alcance actual.
          <template v-if="auth.isSuperAdmin">
            Crea una persona desde el módulo interno o carga primero la base correspondiente.
          </template>
        </div>

        <form v-else class="form-grid" @submit.prevent="handleSubmit">
          <div class="form-field full-span">
            <label class="label" for="employeeSearch">Buscar persona evaluada</label>
            <input
              id="employeeSearch"
              v-model.trim="employeeSearch"
              type="text"
              class="form-control"
              placeholder="Nombre, documento, cargo, área..."
              :disabled="saving"
            />
          </div>

          <div class="form-field full-span">
            <label class="label" for="employeeId">Persona evaluada</label>
            <select
              id="employeeId"
              v-model="form.employeeId"
              class="form-select"
              :disabled="saving"
            >
              <option value="">Seleccione</option>
              <option
                v-for="employee in filteredEmployees"
                :key="employee.id"
                :value="employee.id"
              >
                {{ employeeLabel(employee) }}
              </option>
            </select>
            <small class="helper-text">
              Aquí eliges a la persona del examen médico. El usuario que sube el PDF se registra automáticamente.
            </small>
          </div>

          <div v-if="selectedEmployee" class="field-card full-span">
            <div class="fields-grid">
              <div class="field-card">
                <span class="label">Persona evaluada</span>
                <span>{{ fullName(selectedEmployee) }}</span>
              </div>

              <div class="field-card">
                <span class="label">Documento</span>
                <span>{{ documentLabel(selectedEmployee) }}</span>
              </div>

              <div class="field-card">
                <span class="label">Cargo</span>
                <span>{{ selectedEmployee.currentPosition || '-' }}</span>
              </div>

              <div class="field-card">
                <span class="label">Área / dependencia</span>
                <span>{{ selectedEmployee.workArea || '-' }}</span>
              </div>

              <div class="field-card">
                <span class="label">Área código</span>
                <span>{{ selectedEmployee.areaCode || '-' }}</span>
              </div>

              <div class="field-card">
                <span class="label">Correo</span>
                <span>{{ selectedEmployee.email || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="form-field">
            <label class="label" for="documentType">Tipo de documento</label>
            <select
              id="documentType"
              v-model="form.documentType"
              class="form-select"
              :disabled="saving"
            >
              <option value="">Seleccione</option>
              <option value="CONCEPTO_MEDICO">CONCEPTO_MEDICO</option>
              <option value="CERTIFICADO_APTITUD">CERTIFICADO_APTITUD</option>
              <option value="EVALUACION_OCUPACIONAL">EVALUACION_OCUPACIONAL</option>
              <option value="OTRO">OTRO</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="examType">Tipo de examen</label>
            <select
              id="examType"
              v-model="form.examType"
              class="form-select"
              :disabled="saving"
            >
              <option value="">Seleccione</option>
              <option value="TRABAJO_EN_ALTURAS">TRABAJO_EN_ALTURAS</option>
              <option value="INGRESO">INGRESO</option>
              <option value="PERIODICO">PERIODICO</option>
              <option value="REINGRESO">REINGRESO</option>
              <option value="OTRO">OTRO</option>
            </select>
          </div>

          <div class="form-field full-span">
            <label class="label" for="file">Archivo PDF</label>
            <input
              id="file"
              ref="fileInput"
              type="file"
              class="form-control"
              accept="application/pdf,.pdf"
              :disabled="saving"
              @change="handleFileChange"
            />
            <small class="helper-text">
              Solo se permiten archivos PDF.
            </small>
          </div>

          <div v-if="selectedFileName" class="summary-card full-span">
            <span class="label">Archivo seleccionado</span>
            <span>{{ selectedFileName }}</span>
          </div>

          <div class="full-span actions-row">
            <RouterLink to="/documents" class="secondary-btn">
              Cancelar
            </RouterLink>

            <button
              type="button"
              class="secondary-btn"
              :disabled="saving"
              @click="resetForm"
            >
              Limpiar formulario
            </button>

            <button
              type="submit"
              class="btn btn-primary"
              :disabled="saving || !employees.length"
            >
              <span
                v-if="saving"
                class="spinner-border spinner-border-sm me-2"
                aria-hidden="true"
              ></span>
              {{ saving ? uploadStepLabel : 'Subir y analizar PDF' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getEmployees } from '../api/employee'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const employees = ref([])
const loadingEmployees = ref(false)
const saving = ref(false)
const error = ref('')
const successMessage = ref('')
const employeeSearch = ref('')
const selectedFile = ref(null)
const fileInput = ref(null)
const uploadStepLabel = ref('Subiendo PDF...')

const form = reactive({
  employeeId: '',
  documentType: 'CONCEPTO_MEDICO',
  examType: 'TRABAJO_EN_ALTURAS'
})

const allVisibleAreas = computed(() => {
  if (auth.isSuperAdmin) return ['Acceso global']
  return Array.isArray(auth.allowedAreas) && auth.allowedAreas.length
    ? auth.allowedAreas
    : ['Sin áreas asignadas']
})

const visibleAreas = computed(() => allVisibleAreas.value.join(', '))

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
    .trim() || 'Sin nombre registrado'
}

const documentLabel = (employee) => {
  return [employee.documentType, employee.documentNumber]
    .filter(Boolean)
    .join(' ')
    .trim() || '-'
}

const employeeLabel = (employee) => {
  const parts = [
    fullName(employee),
    documentLabel(employee),
    employee.currentPosition || '',
    employee.areaCode || ''
  ].filter(Boolean)

  return parts.join(' • ')
}

const filteredEmployees = computed(() => {
  const term = normalize(employeeSearch.value)
  if (!term) return employees.value

  return employees.value.filter((employee) => {
    const haystack = [
      fullName(employee),
      documentLabel(employee),
      employee.currentPosition,
      employee.workArea,
      employee.zone,
      employee.areaCode,
      employee.email
    ]
      .map(normalize)
      .join(' ')

    return haystack.includes(term)
  })
})

const selectedEmployee = computed(() => {
  return employees.value.find((employee) => employee.id === form.employeeId) || null
})

const selectedFileName = computed(() => selectedFile.value?.name || '')

const loadEmployees = async () => {
  try {
    loadingEmployees.value = true
    error.value = ''

    const { data } = await getEmployees()
    employees.value = Array.isArray(data) ? data : []
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudieron cargar las personas disponibles.'
    employees.value = []
  } finally {
    loadingEmployees.value = false
  }
}

const handleFileChange = (event) => {
  const file = event.target.files?.[0] || null
  selectedFile.value = file
}

const validateForm = () => {
  error.value = ''
  successMessage.value = ''

  if (!form.employeeId) {
    error.value = 'Debes seleccionar la persona evaluada.'
    return false
  }

  if (!form.documentType) {
    error.value = 'El tipo de documento es obligatorio.'
    return false
  }

  if (!form.examType) {
    error.value = 'El tipo de examen es obligatorio.'
    return false
  }

  if (!selectedFile.value) {
    error.value = 'Debes seleccionar un archivo PDF.'
    return false
  }

  const fileName = selectedFile.value.name?.toLowerCase() || ''
  if (!fileName.endsWith('.pdf')) {
    error.value = 'Solo se permiten archivos PDF.'
    return false
  }

  return true
}

const resetForm = () => {
  error.value = ''
  successMessage.value = ''
  form.employeeId = ''
  form.documentType = 'CONCEPTO_MEDICO'
  form.examType = 'TRABAJO_EN_ALTURAS'
  employeeSearch.value = ''
  selectedFile.value = null
  uploadStepLabel.value = 'Subiendo PDF...'

  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const handleSubmit = async () => {
  if (saving.value) return
  if (!validateForm()) return

  try {
    saving.value = true
    error.value = ''
    successMessage.value = ''

    uploadStepLabel.value = 'Subiendo PDF...'

    const formData = new FormData()
    formData.append('employeeId', form.employeeId)
    formData.append('documentType', form.documentType)
    formData.append('examType', form.examType)
    formData.append('file', selectedFile.value)

    const uploadResponse = await http.post('/api/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    const documentId = uploadResponse?.data?.id
    if (!documentId) {
      throw new Error('La respuesta del backend no devolvió el id del documento.')
    }

    uploadStepLabel.value = 'Analizando PDF...'
    await http.get(`/api/documents/${documentId}/analyze`)

    successMessage.value = 'El PDF se cargó y analizó correctamente.'
    await router.push(`/documents/${documentId}`)
  } catch (err) {
    error.value =
      err?.response?.data?.message ||
      err?.message ||
      'No se pudo subir y analizar el documento.'
  } finally {
    saving.value = false
    uploadStepLabel.value = 'Subiendo PDF...'
  }
}

onMounted(() => {
  loadEmployees()
})
</script>

<style scoped>
.area-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}
</style>