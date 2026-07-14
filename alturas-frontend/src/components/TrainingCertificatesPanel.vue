<template>
  <article class="card border-0 certificates-panel">
    <div class="card-body">
      <div class="certificates-header">
        <div>
          <span class="mini-title">Constancia</span>
          <h2>Constancia</h2>
          <p class="p mb-0">
            Carga, consulta y administra las constancias del trabajador cuando el proceso esté aprobado y notificado correctamente.
          </p>
        </div>

        <span :class="eligibilityClass">
          {{ eligibilityLabel }}
        </span>
      </div>

      <div class="hr"></div>

      <div v-if="error" class="state-box error">
        {{ error }}
      </div>

      <div v-if="successMessage" class="state-box info">
        {{ successMessage }}
      </div>

      <template v-if="canModify">
        <div v-if="eligibilityLoading" class="state-box">
          Validando si el trabajador puede cargar constancia...
        </div>

        <div v-else-if="!certificateEligibility.eligible" class="state-box warning-box">
          {{ certificateEligibility.message || 'La carga de constancia se habilita cuando el trabajador tenga una evaluación aprobada y notificada correctamente.' }}
        </div>

        <div v-if="certificateEligibility.eligible" class="upload-box">
          <div>
            <label class="label" for="certificateFile">Archivo de constancia</label>
            <input
              id="certificateFile"
              ref="fileInput"
              type="file"
              class="form-control"
              accept="application/pdf,.pdf"
              :disabled="uploading"
              @change="handleFileChange"
            />

            <p class="help-text">
              Solo se permite PDF. El archivo quedará asociado al trabajador.
            </p>
          </div>

          <button
            type="button"
            class="primary-btn"
            :disabled="uploading || !selectedFile"
            @click="uploadFile"
          >
            {{ uploading ? 'Cargando...' : 'Cargar constancia' }}
          </button>
        </div>

        <div class="hr"></div>
      </template>

      <div class="certificates-table-header">
        <div>
          <div class="section-title-with-count">
            <h3>Constancias registradas</h3>

            <span class="section-count-pill">
              {{ certificates.length }} registro{{ certificates.length === 1 ? '' : 's' }}
            </span>
          </div>

          <p class="helper-text mb-0">
            Tabla de constancias cargadas para el trabajador.
          </p>
        </div>

        <div class="pagination-size">
          <label class="label" for="certificatePageSize">Mostrar</label>
          <select id="certificatePageSize" v-model.number="certificatePageSize" class="form-select">
            <option :value="10">10</option>
            <option :value="15">15</option>
            <option :value="25">25</option>
            <option :value="50">50</option>
          </select>
        </div>
      </div>

      <div class="compact-divider"></div>

      <div v-if="loading" class="state-box">
        Cargando constancias...
      </div>

      <div v-else-if="!certificates.length" class="state-box mb-0">
        Este trabajador todavía no tiene constancias registradas.
      </div>

      <template v-else>
        <div class="table-fit-wrapper">
          <table class="table table-sm align-middle certificate-table">
            <thead>
              <tr>
                <th>Fecha carga</th>
                <th>Constancia</th>
                <th>Estado</th>
                <th class="text-center">Acciones</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="certificate in paginatedCertificates" :key="certificate.id">
                <td>
                  <div class="date-stack">
                    <strong>{{ formatDateOnly(certificate.uploadedAt) }}</strong>
                    <small>{{ formatTimeOnly(certificate.uploadedAt) }}</small>
                  </div>
                </td>

                <td>
                  <strong class="certificate-name">
                    {{ certificate.originalFileName || 'Constancia' }}
                  </strong>
                  <small class="muted-line">
                    Tamaño: {{ formatSize(certificate.sizeBytes) }}
                  </small>
                </td>

                <td>
                  <span class="status-pill-active">
                    Disponible
                  </span>
                </td>

                <td class="text-center">
                  <div class="table-actions">
                    <button type="button" class="secondary-btn tiny-btn" @click="viewFile(certificate)">
                      Ver
                    </button>

                    <button type="button" class="primary-btn tiny-btn" @click="downloadFile(certificate)">
                      Descargar
                    </button>

                    <button v-if="canModify" type="button" class="secondary-btn danger-btn tiny-btn" @click="removeFile(certificate)">
                      Eliminar
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar google-pagination">
          <div class="pagination-info">
            Mostrando
            <strong>{{ certificatePageStart }}</strong>
            -
            <strong>{{ certificatePageEnd }}</strong>
            de
            <strong>{{ certificates.length }}</strong>
          </div>

          <div class="pagination-actions">
            <button
              type="button"
              class="page-nav"
              :disabled="certificateCurrentPage === 1"
              @click="goToCertificatePage(certificateCurrentPage - 1)"
            >
              Anterior
            </button>

            <template v-for="(item, index) in visibleCertificatePageItems" :key="`${item}-${index}`">
              <span v-if="item === '...'" class="page-ellipsis">...</span>

              <button
                v-else
                type="button"
                class="page-number"
                :class="{ active: item === certificateCurrentPage }"
                @click="goToCertificatePage(item)"
              >
                {{ item }}
              </button>
            </template>

            <button
              type="button"
              class="page-nav"
              :disabled="certificateCurrentPage === certificateTotalPages"
              @click="goToCertificatePage(certificateCurrentPage + 1)"
            >
              Siguiente
            </button>
          </div>
        </div>
      </template>
    </div>
  </article>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import http from '../api/http'
import {
  deleteTrainingCertificate,
  downloadTrainingCertificate,
  getTrainingCertificateEligibility,
  getTrainingCertificates,
  uploadTrainingCertificate
} from '../api/trainingCertificates'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const canModify = computed(() => {
  return authStore.isSuperAdmin || authStore.isAdmin || authStore.isOperator
})

const props = defineProps({
  employeeId: {
    type: String,
    required: true
  }
})

const loading = ref(false)
const eligibilityLoading = ref(false)
const uploading = ref(false)
const error = ref('')
const successMessage = ref('')
const selectedFile = ref(null)
const certificates = ref([])
const fileInput = ref(null)

const certificatePageSize = ref(10)
const certificateCurrentPage = ref(1)

const certificateEligibility = ref({
  eligible: false,
  message: 'La carga de constancia se habilita cuando el trabajador tenga una evaluación aprobada y notificada correctamente.'
})

const eligibilityLabel = computed(() => {
  return certificateEligibility.value?.eligible ? 'Habilitada' : 'Bloqueada'
})

const eligibilityClass = computed(() => {
  return certificateEligibility.value?.eligible
    ? 'status-pill-active'
    : 'status-pill-warning'
})

const certificateTotalPages = computed(() => {
  return Math.max(1, Math.ceil(certificates.value.length / certificatePageSize.value))
})

const paginatedCertificates = computed(() => {
  const start = (certificateCurrentPage.value - 1) * certificatePageSize.value
  const end = start + certificatePageSize.value

  return certificates.value.slice(start, end)
})

const certificatePageStart = computed(() => {
  if (!certificates.value.length) return 0
  return (certificateCurrentPage.value - 1) * certificatePageSize.value + 1
})

const certificatePageEnd = computed(() => {
  return Math.min(certificateCurrentPage.value * certificatePageSize.value, certificates.value.length)
})

const visibleCertificatePageItems = computed(() => {
  const total = certificateTotalPages.value
  const current = certificateCurrentPage.value

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

const goToCertificatePage = (page) => {
  certificateCurrentPage.value = Math.min(
    Math.max(Number(page) || 1, 1),
    certificateTotalPages.value
  )
}

const loadEligibility = async () => {
  if (!props.employeeId) return

  try {
    eligibilityLoading.value = true

    const { data } = await getTrainingCertificateEligibility(props.employeeId)

    certificateEligibility.value = {
      eligible: Boolean(data?.eligible),
      message: data?.message || ''
    }
  } catch (err) {
    certificateEligibility.value = {
      eligible: false,
      message: err?.response?.data?.message || 'No se pudo validar si el trabajador puede cargar constancia.'
    }
  } finally {
    eligibilityLoading.value = false
  }
}

const loadCertificates = async () => {
  if (!props.employeeId) return

  try {
    loading.value = true
    error.value = ''

    const { data } = await getTrainingCertificates(props.employeeId)
    certificates.value = Array.isArray(data) ? data : []
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudieron cargar las constancias.'
  } finally {
    loading.value = false
  }
}

const handleFileChange = (event) => {
  const file = event.target.files?.[0] || null

  selectedFile.value = file
  error.value = ''
  successMessage.value = ''

  if (!certificateEligibility.value?.eligible) {
    selectedFile.value = null
    error.value = certificateEligibility.value?.message || 'La carga de constancia no está habilitada.'

    if (fileInput.value) {
      fileInput.value.value = ''
    }

    return
  }

  if (file && !file.name.toLowerCase().endsWith('.pdf')) {
    selectedFile.value = null
    error.value = 'Solo se permite cargar archivos PDF.'

    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

const uploadFile = async () => {
  if (!selectedFile.value) return

  if (!certificateEligibility.value?.eligible) {
    error.value = certificateEligibility.value?.message || 'La carga de constancia no está habilitada.'
    return
  }

  try {
    uploading.value = true
    error.value = ''
    successMessage.value = ''

    await uploadTrainingCertificate(props.employeeId, selectedFile.value)

    successMessage.value = 'Constancia cargada correctamente.'
    selectedFile.value = null

    if (fileInput.value) {
      fileInput.value.value = ''
    }

    await Promise.all([
      loadEligibility(),
      loadCertificates()
    ])
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar la constancia.'
  } finally {
    uploading.value = false
  }
}

const viewFile = async (certificate) => {
  if (!certificate?.id) {
    error.value = 'No se pudo abrir la constancia.'
    return
  }

  try {
    error.value = ''
    successMessage.value = ''

    const response = await http.get(`/api/certificates/${certificate.id}/download`, {
      responseType: 'blob'
    })

    const blob = new Blob([response.data], {
      type: response.headers?.['content-type'] || 'application/pdf'
    })

    const url = window.URL.createObjectURL(blob)
    window.open(url, '_blank', 'noopener,noreferrer')

    setTimeout(() => {
      window.URL.revokeObjectURL(url)
    }, 60000)
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo abrir la constancia.'
  }
}

const downloadFile = async (certificate) => {
  try {
    error.value = ''
    successMessage.value = ''

    await downloadTrainingCertificate(
      certificate.id,
      certificate.originalFileName || 'constancia.pdf'
    )
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo descargar la constancia.'
  }
}

const removeFile = async (certificate) => {
  const confirmed = window.confirm('¿Deseas eliminar esta constancia?')

  if (!confirmed) return

  try {
    error.value = ''
    successMessage.value = ''

    await deleteTrainingCertificate(certificate.id)

    successMessage.value = 'Constancia eliminada correctamente.'

    await Promise.all([
      loadEligibility(),
      loadCertificates()
    ])
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo eliminar la constancia.'
  }
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

const formatSize = (bytes) => {
  const size = Number(bytes || 0)

  if (size <= 0) return '0 KB'

  if (size < 1024 * 1024) {
    return `${Math.round(size / 1024)} KB`
  }

  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

const refresh = async () => {
  await Promise.all([
    loadEligibility(),
    loadCertificates()
  ])
}

watch(certificatePageSize, () => {
  certificateCurrentPage.value = 1
})

watch(certificateTotalPages, (value) => {
  if (certificateCurrentPage.value > value) {
    certificateCurrentPage.value = value
  }
})

watch(
  () => props.employeeId,
  () => {
    certificateCurrentPage.value = 1
    refresh()
  }
)

onMounted(refresh)
</script>

<style scoped>
.certificates-panel {
  margin-top: 1rem;
}

.certificates-header,
.certificates-table-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.certificates-header h2,
.certificates-table-header h3 {
  margin: 0;
  color: var(--text);
  font-size: 1.2rem;
  font-weight: 780;
}

.certificates-table-header h3 {
  font-size: 1rem;
}

.section-title-with-count {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.section-title-with-count h3 {
  margin: 0;
}

.upload-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 1rem;
}

.help-text {
  margin: 0.35rem 0 0;
  color: var(--text-muted);
  font-size: 0.85rem;
}

.warning-box {
  border-color: color-mix(in srgb, var(--warning) 35%, var(--border));
  background: var(--warning-soft);
}

.compact-divider {
  height: 1px;
  margin: 0.75rem 0;
  background: var(--border);
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

.table-fit-wrapper {
  width: 100%;
  overflow: visible;
}

.certificate-table {
  width: 100%;
  table-layout: fixed;
  margin-bottom: 0;
}

.certificate-table th {
  padding: 0.62rem 0.45rem;
  color: var(--text-muted);
  font-size: 0.68rem;
  line-height: 1.1;
  white-space: normal;
}

.certificate-table td {
  padding: 0.62rem 0.45rem;
  color: var(--text);
  font-size: 0.78rem;
  line-height: 1.25;
  vertical-align: middle;
  word-break: break-word;
}

.certificate-name {
  display: block;
  color: var(--text);
  font-size: 0.8rem;
  line-height: 1.25;
  word-break: break-word;
}

.muted-line {
  display: block;
  margin-top: 0.18rem;
  color: var(--text-muted);
  font-size: 0.7rem;
}

.date-stack {
  display: flex;
  flex-direction: column;
  gap: 0.12rem;
}

.date-stack strong {
  color: var(--text);
  font-size: 0.78rem;
  line-height: 1.2;
}

.date-stack small {
  color: var(--text-muted);
  font-size: 0.7rem;
}

.table-actions {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.tiny-btn {
  min-height: 28px;
  padding: 0.34rem 0.52rem;
  font-size: 0.7rem;
  white-space: nowrap;
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

@media (max-width: 900px) {
  .table-fit-wrapper {
    overflow-x: auto;
  }

  .certificate-table {
    min-width: 680px;
    table-layout: auto;
  }
}

@media (max-width: 780px) {
  .certificates-header,
  .certificates-table-header,
  .upload-box,
  .google-pagination {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .upload-box .primary-btn {
    width: 100%;
  }
}
</style>
