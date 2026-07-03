import re

file_path = r"c:\Users\andre\Downloads\Lector-PDF-CODEX-PR4\alturas-frontend\src\views\ReportsView.vue"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

correct_chunk = """            <div class="form-field">
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
              <label class="label" for="excelFrom">Evaluación desde</label>
              <input
                id="excelFrom"
                v-model="excelFilters.from"
                type="date"
                class="form-control"
              />
            </div>

            <div class="form-field">
              <label class="label" for="excelTo">Evaluación hasta</label>
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
              <h2>CSV cargue masivo Ministerio</h2>"""

# Let's find the broken part in the content
# We will find the start of `<div class="form-field">\n              <label class="label" for="excelPosition">Cargo</label>`
# and replace until `<h2>CSV cargue masivo Ministerio</h2>`

start_idx = content.find('<label class="label" for="excelPosition">Cargo</label>')
if start_idx != -1:
    start_idx = content.rfind('<div class="form-field">', 0, start_idx)

end_idx = content.find('<h2>CSV cargue masivo Ministerio</h2>', start_idx)
if end_idx != -1:
    end_idx += len('<h2>CSV cargue masivo Ministerio</h2>')

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + correct_chunk + content[end_idx:]
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("File fixed successfully.")
else:
    print("Could not find boundaries.")
