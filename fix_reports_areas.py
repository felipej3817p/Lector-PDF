import re

file_path = r"c:\Users\andre\Downloads\Lector-PDF-CODEX-PR4\alturas-frontend\src\views\ReportsView.vue"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update Excel template
excel_area_old = """            <div class="form-field">
              <label class="label" for="excelArea">Zona / área</label>
              <select id="excelArea" v-model="excelFilters.areaCode" class="form-select">
                <option value="">Todas</option>
                <option v-for="area in areaOptions" :key="area.value" :value="area.value">
                  {{ area.label }}
                </option>
              </select>
            </div>"""

excel_area_new = """            <div class="form-field">
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
            </div>"""

content = content.replace(excel_area_old, excel_area_new)

# 2. Update CSV template
csv_area_old = """            <div class="form-field">
              <label class="label" for="csvArea">Zona / área</label>
              <select id="csvArea" v-model="csvFilters.areaCode" class="form-select">
                <option value="">Todas</option>
                <option v-for="area in areaOptions" :key="area.value" :value="area.value">
                  {{ area.label }}
                </option>
              </select>
            </div>"""

csv_area_new = """            <div class="form-field">
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
            </div>"""

content = content.replace(csv_area_old, csv_area_new)

# 3. Update Imports and Script Setup
import_old = "import { AREA_OPTIONS } from '../utils/areaCatalog'"
import_new = "import { AREA_OPTIONS, PRIMARY_AREA_OPTIONS, primaryAreaCode, subzoneOptions, normalizeAreaCode } from '../utils/areaCatalog'\nimport { computed } from 'vue'"
content = content.replace(import_old, import_new)
content = content.replace("import { reactive, ref, onBeforeUnmount } from 'vue'", "import { reactive, ref, onBeforeUnmount } from 'vue'") # ensure computed is added above if not already present

script_vars_old = """const auth = useAuthStore()
const canDownloadCsv = auth.isAdmin || auth.isOperator
const areaOptions = AREA_OPTIONS"""

script_vars_new = """const auth = useAuthStore()
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
})"""

content = content.replace(script_vars_old, script_vars_new)

# 4. Attach computed instances right after reactive definitions
filters_old = """const excelFilters = reactive(initialExcelFilters())
const csvFilters = reactive(initialCsvFilters())"""

filters_new = """const excelFilters = reactive(initialExcelFilters())
const csvFilters = reactive(initialCsvFilters())

const excelPrimaryArea = createPrimaryAreaComputed(excelFilters)
const excelSubzoneOptions = createSubzoneOptionsComputed(excelPrimaryArea)

const csvPrimaryArea = createPrimaryAreaComputed(csvFilters)
const csvSubzoneOptions = createSubzoneOptionsComputed(csvPrimaryArea)"""

content = content.replace(filters_old, filters_new)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("ReportsView.vue updated.")
