<template>
  <section class="page reports-page">
    <div class="dashboard-toolbar">
      <div>
        <span class="mini-title">Reportes</span>
        <h1 class="h1 mb-2">Exportación de información</h1>
        <p class="p mb-0">
          Genera el reporte general de aptitud en Excel y el archivo plano CSV para cargue masivo al Ministerio.
        </p>
      </div>
    </div>

    <div class="reports-grid">
      <article class="card border-0 report-card">
        <div class="card-body">
          <div class="report-card__header">
            <div>
              <span class="mini-title">Excel</span>
              <h2>Reporte general de aptitud</h2>
              <p>
                Consolida trabajador, cédula, cargo, zona, resultado del concepto y fecha de evaluación.
              </p>
            </div>

            <span class="status-pill-active">XLSX</span>
          </div>

          <div class="hr"></div>

          <div class="form-grid">
            <div class="form-field">
              <label class="label" for="excelMode">Tipo de reporte</label>
              <select id="excelMode" v-model="excelFilters.mode" class="form-select">
                <option value="latest">Último concepto por trabajador</option>
                <option value="history">Histórico completo (Todas las evaluaciones)</option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="excelDocumentNumber">Cédula</label>
              <input
                id="excelDocumentNumber"
                v-model.trim="excelFilters.documentNumber"
                type="text"
                class="form-control"
                placeholder="Ej: 1002394905"
              />
            </div>

            <div class="form-field">
              <label class="label" for="excelName">Nombre</label>
              <input
                id="excelName"
                v-model.trim="excelFilters.name"
                type="text"
                class="form-control"
                placeholder="Nombre o apellido"
              />
            </div>

            <div class="form-field">
              <label class="label" for="excelArea">Zona / área</label>
              <select id="excelArea" v-model="excelPrimaryArea" class="form-select">
                <option value="">Todas</option>
                <option v-for="area in availablePrimaryAreas" :key="area.value" :value="area.value">
                  {{ area.label }}
                </option>
              </select>
            </div>

            <div v-if="excelSubzoneOptions.length" class="form-field">
              <label class="label" for="excelSubzone">Subzona</label>
              <select id="excelSubzone" v-model="excelFilters.areaCode" class="form-select">
                <option :value="excelPrimaryArea">Todas las subzonas</option>
                <option v-for="subzone in excelSubzoneOptions" :key="subzone.value" :value="subzone.value">
                  {{ subzone.label }}
                </option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="excelResult">Resultado</label>
              <select id="excelResult" v-model="excelFilters.resultStatus" class="form-select">
                <option value="">Todos</option>
                <option value="APTO">APTO</option>
                <option value="NO_APTO">NO APTO</option>
                <option value="VIGENCIA_VENCIDA">VENCIDOS</option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="excelEnabled">Estado de trabajador</label>
              <select id="excelEnabled" v-model="excelFilters.enabled" class="form-select">
                <option value="">Todos</option>
                <option value="true">ACTIVO</option>
                <option value="false">INACTIVO</option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="excelPosition">Cargo</label>
              <input
                id="excelPosition"
                v-model.trim="excelFilters.position"
                type="text"
                class="form-control"
                placeholder="Cargo"
              />
            </div>

            <div class="form-field">
              <label class="label" for="excelFrom">Fecha de evaluación de concepto médico (Desde)</label>
              <input
                id="excelFrom"
                v-model="excelFilters.from"
                type="date"
                class="form-control"
              />
            </div>

            <div class="form-field">
              <label class="label" for="excelTo">Fecha de evaluación de concepto médico (Hasta)</label>
              <input
                id="excelTo"
                v-model="excelFilters.to"
                type="date"
                class="form-control"
              />
            </div>
          </div>

          <div class="report-actions">
            <button
              type="button"
              class="secondary-btn"
              :disabled="downloadingExcel"
              @click="clearExcelFilters"
            >
              Limpiar filtros
            </button>

            <button
              type="button"
              class="primary-btn"
              :disabled="downloadingExcel"
              @click="exportExcel"
            >
              {{ downloadingExcel ? 'Generando...' : 'Descargar Excel' }}
            </button>
          </div>
        </div>
      </article>

      <article v-if="canDownloadCsv" class="card border-0 report-card">
        <div class="card-body">
          <div class="report-card__header">
            <div>
              <span class="mini-title">Archivo plano</span>
              <h2>CSV cargue masivo Ministerio</h2>
              <p>
                Genera el archivo plano separado por punto y coma, sin encabezados, según el formato enviado.
              </p>
            </div>

            <span class="status-pill-neutral">CSV</span>
          </div>

          <div class="hr"></div>

          <div class="form-grid">
            <div class="form-field">
              <label class="label" for="csvDocumentNumber">Cédula</label>
              <input
                id="csvDocumentNumber"
                v-model.trim="csvFilters.documentNumber"
                type="text"
                class="form-control"
                placeholder="Ej: 1002394905"
              />
            </div>

            <div class="form-field">
              <label class="label" for="csvName">Nombre</label>
              <input
                id="csvName"
                v-model.trim="csvFilters.name"
                type="text"
                class="form-control"
                placeholder="Nombre o apellido"
              />
            </div>

            <div class="form-field">
              <label class="label" for="csvArea">Zona / área</label>
              <select id="csvArea" v-model="csvPrimaryArea" class="form-select">
                <option value="">Todas</option>
                <option v-for="area in availablePrimaryAreas" :key="area.value" :value="area.value">
                  {{ area.label }}
                </option>
              </select>
            </div>

            <div v-if="csvSubzoneOptions.length" class="form-field">
              <label class="label" for="csvSubzone">Subzona</label>
              <select id="csvSubzone" v-model="csvFilters.areaCode" class="form-select">
                <option :value="csvPrimaryArea">Todas las subzonas</option>
                <option v-for="subzone in csvSubzoneOptions" :key="subzone.value" :value="subzone.value">
                  {{ subzone.label }}
                </option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="csvPosition">Cargo</label>
              <input
                id="csvPosition"
                v-model.trim="csvFilters.position"
                type="text"
                class="form-control"
                placeholder="Cargo"
              />
            </div>

            <div class="form-field">
              <label class="label" for="csvResult">Resultado</label>
              <select id="csvResult" v-model="csvFilters.resultStatus" class="form-select">
                <option value="">APTO y NO APTO</option>
                <option value="APTO">APTO</option>
                <option value="NO_APTO">NO APTO</option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="csvEnabled">Estado de trabajador</label>
              <select id="csvEnabled" v-model="csvFilters.enabled" class="form-select">
                <option value="">Todos</option>
                <option value="true">ACTIVO</option>
                <option value="false">INACTIVO</option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="csvFrom">Fecha de evaluación (Desde)</label>
              <input
                id="csvFrom"
                v-model="csvFilters.from"
                type="date"
                class="form-control"
              />
            </div>

            <div class="form-field">
              <label class="label" for="csvTo">Fecha de evaluación (Hasta)</label>
              <input
                id="csvTo"
                v-model="csvFilters.to"
                type="date"
                class="form-control"
              />
            </div>

          </div>

          <div class="csv-format-box">
            <span class="label">Formato de salida</span>
            <code>
              CC;Número documento;Primer nombre;Segundo nombre;Primer apellido;Segundo apellido;Sexo;País;Fecha nacimiento;Nivel educativo;Distribución;Cargo;Sector;Empresa;ARL
            </code>
          </div>

          <div class="report-actions">
            <button
              type="button"
              class="secondary-btn"
              :disabled="downloadingCsv"
              @click="clearCsvFilters"
            >
              Limpiar filtros
            </button>

            <button
              type="button"
              class="primary-btn"
              :disabled="downloadingCsv"
              @click="exportCsv"
            >
              {{ downloadingCsv ? 'Generando...' : 'Descargar CSV' }}
            </button>
          </div>
        </div>
      </article>
    </div>

    <Transition name="toast-slide">
      <div
        v-if="toastMessage"
        class="report-toast"
        :class="toastType"
      >
        <div class="report-toast__icon">
          {{ toastType === 'success' ? '✓' : '!' }}
        </div>

        <div class="report-toast__content">
          <strong>{{ toastTitle }}</strong>
          <span>{{ toastMessage }}</span>
        </div>
      </div>
    </Transition>
  </section>
</template>

<script setup>
import { reactive, ref, onBeforeUnmount } from 'vue'
import { downloadAptitudeExcel, downloadMinistryCsv } from '../api/reports'
import { useAuthStore } from '../stores/auth'
import { AREA_OPTIONS, PRIMARY_AREA_OPTIONS, primaryAreaCode, subzoneOptions, normalizeAreaCode } from '../utils/areaCatalog'
import { computed } from 'vue'

const auth = useAuthStore()
const canDownloadCsv = auth.isAdmin || auth.isOperator
const areaOptions = AREA_OPTIONS

const availablePrimaryAreas = computed(() => {
  if (auth.isAdmin || auth.hasGlobalAreaAccess) return PRIMARY_AREA_OPTIONS
  return PRIMARY_AREA_OPTIONS.filter(area => 
    auth.allowedAreas.some(allowed => allowed === area.value || allowed.startsWith(area.value + '_'))
  )
})

const createPrimaryAreaComputed = (filters) => computed({
  get: () => primaryAreaCode(filters.areaCode),
  set: (value) => {
    const normalized = normalizeAreaCode(value)
    if (normalized !== 'CENTRO' || !normalizeAreaCode(filters.areaCode).startsWith('CENTRO_')) {
      filters.areaCode = normalized
    }
  }
})

const createSubzoneOptionsComputed = (primaryAreaComputed) => computed(() => {
  const subzones = subzoneOptions(primaryAreaComputed.value).filter((s) => s.value !== 'SIN_SUBZONA')
  if (auth.isAdmin || auth.hasGlobalAreaAccess) return subzones
  return subzones.filter(s => auth.allowedAreas.includes(s.value) || auth.allowedAreas.includes(primaryAreaComputed.value))
})

const downloadingExcel = ref(false)
const downloadingCsv = ref(false)

const toastTitle = ref('')
const toastMessage = ref('')
const toastType = ref('success')
let toastTimeout = null
  
const initialExcelFilters = () => ({
  mode: 'latest',
  documentNumber: '',
  name: '',
  areaCode: '',
  resultStatus: 'APTO',
  enabled: '',
  position: '',
  from: '',
  to: ''
})

const formatDateInput = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

const todayIsoDate = () => formatDateInput(new Date())

const firstDayOfCurrentMonth = () => {
  const now = new Date()
  return formatDateInput(new Date(now.getFullYear(), now.getMonth(), 1))
}

const initialCsvFilters = () => ({
  documentNumber: '',
  name: '',
  areaCode: '',
  position: '',
  resultStatus: 'APTO',
  enabled: '',
  from: '',
  to: ''
})

const excelFilters = reactive(initialExcelFilters())
const csvFilters = reactive(initialCsvFilters())

const excelPrimaryArea = createPrimaryAreaComputed(excelFilters)
const excelSubzoneOptions = createSubzoneOptionsComputed(excelPrimaryArea)

const csvPrimaryArea = createPrimaryAreaComputed(csvFilters)
const csvSubzoneOptions = createSubzoneOptionsComputed(csvPrimaryArea)

const showToast = (title, message, type = 'success') => {
  toastTitle.value = title
  toastMessage.value = message
  toastType.value = type

  if (toastTimeout) {
    clearTimeout(toastTimeout)
  }

  toastTimeout = setTimeout(() => {
    toastTitle.value = ''
    toastMessage.value = ''
    toastType.value = 'success'
    toastTimeout = null
  }, 4500)
}

const showDownloadResult = (result) => {
  if (result?.empty) {
    showToast(
      'Sin datos',
      result?.message || 'No hay ningún trabajador evaluado.',
      'warning'
    )
    return
  }

  showToast(
    'Reporte descargado',
    'El archivo se descargó correctamente.',
    'success'
  )
}

const clearExcelFilters = () => {
  Object.assign(excelFilters, initialExcelFilters())
}

const clearCsvFilters = () => {
  Object.assign(csvFilters, initialCsvFilters())
}

const exportExcel = async () => {
  try {
    downloadingExcel.value = true

    const result = await downloadAptitudeExcel(excelFilters)

    showDownloadResult(result)
  } catch (err) {
    showToast(
      'Error',
      err?.message || 'No se pudo generar el reporte. Intenta nuevamente.',
      'error'
    )
  } finally {
    downloadingExcel.value = false
  }
}

const exportCsv = async () => {
  if (!canDownloadCsv) {
    showToast(
      'Acceso no permitido',
      'La descarga CSV está disponible solo para ADMIN y OPERADOR.',
      'warning'
    )
    return
  }

  try {
    downloadingCsv.value = true

    const result = await downloadMinistryCsv(csvFilters)

    showDownloadResult(result)
  } catch (err) {
    showToast(
      'Error',
      err?.message || 'No se pudo generar el reporte. Intenta nuevamente.',
      'error'
    )
  } finally {
    downloadingCsv.value = false
  }
}

onBeforeUnmount(() => {
  if (toastTimeout) {
    clearTimeout(toastTimeout)
  }
})
</script>

<style scoped>
.reports-page {
  gap: 1.25rem;
}

.reports-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1.25rem;
}

.report-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.report-card__header h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 780;
}

.report-card__header p {
  margin: 0.35rem 0 0;
  color: var(--text-muted);
}

.report-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  flex-wrap: wrap;
  margin-top: 1.25rem;
}

.csv-format-box {
  margin-top: 1rem;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.csv-format-box code {
  display: block;
  white-space: normal;
  word-break: break-word;
  color: var(--text-soft);
  font-size: 0.86rem;
}

.report-toast {
  position: fixed;
  left: 50%;
  bottom: 1.25rem;
  z-index: 120;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  width: min(460px, calc(100vw - 2rem));
  padding: 0.9rem 1rem;
  border: 1px solid #86efac;
  border-radius: 16px;
  background: #dcfce7;
  color: #166534;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.18);
  transform: translateX(-50%);
}

.report-toast.warning {
  border-color: #fde68a;
  background: #fef3c7;
  color: #92400e;
}

.report-toast.error {
  border-color: #fecaca;
  background: #fee2e2;
  color: #991b1b;
}

.report-toast__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: rgba(22, 101, 52, 0.12);
  color: #166534;
  font-size: 0.9rem;
  font-weight: 900;
}

.report-toast.warning .report-toast__icon {
  background: rgba(146, 64, 14, 0.12);
  color: #92400e;
}

.report-toast.error .report-toast__icon {
  background: rgba(153, 27, 27, 0.12);
  color: #991b1b;
}

.report-toast__content {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;
  min-width: 0;
}

.report-toast__content strong {
  font-size: 0.88rem;
  font-weight: 850;
  line-height: 1.2;
}

.report-toast__content span {
  font-size: 0.78rem;
  font-weight: 650;
  line-height: 1.35;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(18px);
}

.toast-slide-enter-to,
.toast-slide-leave-from {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

@media (max-width: 780px) {
  .report-card__header,
  .report-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .report-actions .primary-btn,
  .report-actions .secondary-btn {
    width: 100%;
  }
}
</style>
