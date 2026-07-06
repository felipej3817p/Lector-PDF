import sys

file_path = r'c:\Users\sen_atorres\Downloads\Lector-PDF-CODEX-PR4\alturas-frontend\src\views\DocumentDetailView.vue'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

target1 = '''            <div class="info-item">
              <span>Estado técnico</span>
              <strong>{{ documentData.processingStatus || '-' }}</strong>
            </div>'''

replacement1 = '''            <div class="info-item">
              <span>Estado técnico</span>
              <strong>{{ technicalStatusLabel(documentData.processingStatus) }}</strong>
            </div>'''

target2 = 'const resultLabel = (status) => {'

replacement2 = '''const technicalStatusLabel = (status) => {
  if (status === 'STORED') return 'GUARDADO'
  if (status === 'PROCESSING') return 'PROCESANDO'
  if (status === 'COMPLETED') return 'COMPLETADO'
  if (status === 'ERROR') return 'ERROR'
  return status || '-'
}

const resultLabel = (status) => {'''

if target1 in content and target2 in content:
    content = content.replace(target1, replacement1)
    content = content.replace(target2, replacement2)
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed.")
else:
    print("Targets not found.")
