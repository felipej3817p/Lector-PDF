<template>
  <section class="page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Carga de evaluaciones</span>
        <h1 class="h1 mb-2">Cargar evaluaciones médicas</h1>
        <p class="p mb-0">
          Selecciona uno o varios PDFs. El sistema lee la cédula del documento,
          identifica al trabajador, analiza el resultado y deja cada caso pendiente de revisión.
        </p>
      </div>

      <div class="header-actions">
        <RouterLink to="/employees" class="secondary-btn">
          Volver a trabajadores
        </RouterLink>

        <RouterLink to="/documents" class="secondary-btn">
          Ver evaluaciones
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
        <span>{{ roleLabel }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Áreas visibles</span>
        <span>{{ visibleAreas }}</span>
      </div>

      <div class="summary-card">
        <span class="label">Archivos en cola</span>
        <span>{{ batchFiles.length }}</span>
      </div>
    </div>

    <div class="card border-0">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Seleccionar PDFs</h2>
            <p class="helper-text mb-0">
              Puedes cargar un solo PDF o varios al mismo tiempo. No debes seleccionar trabajador manualmente:
              la asociación se realiza automáticamente por cédula.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div
          class="dropzone"
          :class="{ 'dropzone--active': dragActive }"
          @dragover.prevent="dragActive = true"
          @dragleave.prevent="dragActive = false"
          @drop.prevent="handleDrop"
        >
          <p class="mb-2">
            <strong>Suelta aquí los PDFs o una carpeta</strong>
          </p>

          <p class="helper-text mb-3">
            El sistema solo procesará archivos con extensión .pdf.
          </p>

          <div class="d-flex gap-2 justify-content-center flex-wrap">
            <label class="secondary-btn mb-0">
              Seleccionar PDFs
              <input
                class="d-none"
                type="file"
                multiple
                accept="application/pdf,.pdf"
                :disabled="batchLoading"
                @change="handleBatchFilesSelect"
              />
            </label>

            <label class="secondary-btn mb-0">
              Seleccionar carpeta
              <input
                class="d-none"
                type="file"
                multiple
                webkitdirectory
                directory
                :disabled="batchLoading"
                @change="handleBatchFilesSelect"
              />
            </label>
          </div>
        </div>

        <div v-if="batchFiles.length" class="card border-0 mt-4">
          <div class="card-body">
            <div class="page-header border-0 pb-0">
              <div>
                <h2 class="h4 mb-1">Archivos seleccionados</h2>
                <p class="helper-text mb-0">
                  Revisa la cola antes de iniciar el procesamiento.
                </p>
              </div>
            </div>

            <div class="hr"></div>

            <div class="table-responsive">
              <table class="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Archivo</th>
                    <th>Tamaño</th>
                    <th class="text-center">Quitar</th>
                  </tr>
                </thead>

                <tbody>
                  <tr v-for="(file, index) in batchFiles" :key="fileKey(file)">
                    <td>{{ file.name }}</td>
                    <td>{{ formatFileSize(file.size) }}</td>
                    <td>
                      <div class="actions justify-content-center">
                        <button
                          type="button"
                          class="secondary-btn danger-btn"
                          :disabled="batchLoading"
                          @click="removeFile(index)"
                        >
                          Quitar
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="actions-row mt-4">
          <button
            type="button"
            class="secondary-btn"
            :disabled="batchLoading || !batchFiles.length"
            @click="clearBatch"
          >
            Limpiar cola
          </button>

          <button
            type="button"
            class="btn btn-primary"
            :disabled="batchLoading || !batchFiles.length"
            @click="submitBatch"
          >
            <span
              v-if="batchLoading"
              class="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            ></span>
            {{ batchLoading ? 'Procesando evaluaciones...' : 'Procesar evaluaciones' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="batchSummary" class="card border-0 mt-4">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Resultado de la carga</h2>
            <p class="helper-text mb-0">
              Cada PDF se procesa por separado. Si uno falla, los demás pueden quedar correctamente cargados.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div class="summary-grid">
          <div class="summary-card">
            <span class="label">Total</span>
            <span>{{ batchSummary.total }}</span>
          </div>

          <div class="summary-card">
            <span class="label">Exitosos</span>
            <span>{{ batchSummary.success }}</span>
          </div>

          <div class="summary-card">
            <span class="label">Fallidos</span>
            <span>{{ batchSummary.failed }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="batchResults.length" class="card border-0 mt-4">
      <div class="card-body">
        <div class="page-header border-0 pb-0">
          <div>
            <h2 class="h4 mb-1">Detalle del procesamiento</h2>
            <p class="helper-text mb-0">
              Los documentos exitosos quedan pendientes de revisión por Seguridad, Salud y Ambiente.
            </p>
          </div>
        </div>

        <div class="hr"></div>

        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead>
              <tr>
                <th>Archivo</th>
                <th>Estado</th>
                <th>Trabajador</th>
                <th>Cédula</th>
                <th>Resultado</th>
                <th>Revisión</th>
                <th>Mensaje</th>
                <th class="text-center">Detalle</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="(item, index) in batchResults" :key="`${item.fileName}-${index}`">
                <td>{{ item.fileName || '-' }}</td>

                <td>
                  <span :class="item.status === 'OK' ? 'status-pill-active' : 'status-pill-inactive'">
                    {{ item.status || '-' }}
                  </span>
                </td>

                <td>{{ item.employeeName || '-' }}</td>
                <td>{{ item.employeeDocument || '-' }}</td>

                <td>
                  <span
                    v-if="item.resultStatus === 'APTO'"
                    class="status-pill-active"
                  >
                    APTO
                  </span>

                  <span
                    v-else-if="item.resultStatus === 'NO_APTO'"
                    class="status-pill-inactive"
                  >
                    NO APTO
                  </span>

                  <span v-else class="status-pill-warning">
                    {{ item.resultStatus || 'PENDIENTE' }}
                  </span>
                </td>

                <td>{{ item.reviewStatus || '-' }}</td>
                <td>{{ item.message || '-' }}</td>

                <td>
                  <div class="actions justify-content-center">
                    <RouterLink
                      v-if="item.documentId"
                      :to="`/documents/${item.documentId}`"
                      class="secondary-btn"
                    >
                      Ver
                    </RouterLink>

                    <span v-else>-</span>
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
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const error = ref('')
const successMessage = ref('')
const batchFiles = ref([])
const batchLoading = ref(false)
const batchResults = ref([])
const batchSummary = ref(null)
const dragActive = ref(false)

const roleLabel = computed(() => {
  if (auth.isSuperAdmin) return 'SUPER_ADMIN'
  if (auth.isApprover) return 'APROBADOR'
  return 'OPERADOR'
})

const visibleAreas = computed(() => {
  if (auth.isSuperAdmin) return 'Acceso global'

  const areas = Array.isArray(auth.allowedAreas) ? auth.allowedAreas : []
  return areas.length ? areas.join(', ') : 'Sin áreas asignadas'
})

const fileKey = (file) => `${file.name}_${file.size}_${file.lastModified}`

const formatFileSize = (size) => {
  const value = Number(size || 0)

  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`

  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

const normalizeBatchFiles = (files) => {
  const input = Array.from(files || [])
  const onlyPdf = input.filter((file) => file?.name?.toLowerCase().endsWith('.pdf'))

  const existing = new Set(batchFiles.value.map(fileKey))
  const merged = [...batchFiles.value]

  for (const file of onlyPdf) {
    const key = fileKey(file)

    if (!existing.has(key)) {
      merged.push(file)
      existing.add(key)
    }
  }

  batchFiles.value = merged

  if (!onlyPdf.length && input.length) {
    error.value = 'No se encontraron archivos PDF válidos en la selección.'
  } else {
    error.value = ''
  }
}

const handleBatchFilesSelect = (event) => {
  normalizeBatchFiles(event.target.files)
  event.target.value = ''
}

const handleDrop = (event) => {
  dragActive.value = false
  normalizeBatchFiles(event.dataTransfer?.files)
}

const removeFile = (index) => {
  batchFiles.value.splice(index, 1)
}

const clearBatch = () => {
  batchFiles.value = []
  batchResults.value = []
  batchSummary.value = null
  error.value = ''
  successMessage.value = ''
}

const submitBatch = async () => {
  if (batchLoading.value || !batchFiles.value.length) return

  try {
    batchLoading.value = true
    error.value = ''
    successMessage.value = ''
    batchResults.value = []
    batchSummary.value = null

    const formData = new FormData()
    formData.append('documentType', 'CONCEPTO_MEDICO')
    formData.append('examType', 'TRABAJO_EN_ALTURAS')

    batchFiles.value.forEach((file) => {
      formData.append('files', file)
    })

    const { data } = await http.post('/api/documents/upload/batch-auto', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    batchSummary.value = {
      total: data?.total ?? batchFiles.value.length,
      success: data?.success ?? 0,
      failed: data?.failed ?? 0
    }

    batchResults.value = Array.isArray(data?.results) ? data.results : []

    successMessage.value = 'Procesamiento finalizado. Los casos correctos quedaron pendientes de revisión.'
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo completar la carga de evaluaciones.'
  } finally {
    batchLoading.value = false
  }
}
</script>

<style scoped>
.dropzone {
  border: 2px dashed #9ca3af;
  border-radius: 16px;
  padding: 2rem 1rem;
  text-align: center;
  background: #f8fafc;
}

.dropzone--active {
  border-color: #2563eb;
  background: #eff6ff;
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