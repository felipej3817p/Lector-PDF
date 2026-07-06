const fs = require('fs');

const filePath = 'c:\\Users\\sen_atorres\\Downloads\\Lector-PDF-CODEX-PR4\\alturas-frontend\\src\\views\\DocumentDetailView.vue';
let content = fs.readFileSync(filePath, 'utf-8');

const target1 = `            <div class="info-item">
              <span>Estado técnico</span>
              <strong>{{ documentData.processingStatus || '-' }}</strong>
            </div>`;

const replacement1 = `            <div class="info-item">
              <span>Estado técnico</span>
              <strong>{{ technicalStatusLabel(documentData.processingStatus) }}</strong>
            </div>`;

const target2 = `const resultLabel = (status) => {`;

const replacement2 = `const technicalStatusLabel = (status) => {
  if (status === 'STORED') return 'GUARDADO'
  if (status === 'PROCESSING') return 'PROCESANDO'
  if (status === 'COMPLETED') return 'COMPLETADO'
  if (status === 'ERROR') return 'ERROR'
  return status || '-'
}

const resultLabel = (status) => {`;

if (content.includes(target1) && content.includes(target2)) {
    content = content.replace(target1, replacement1);
    content = content.replace(target2, replacement2);
    fs.writeFileSync(filePath, content, 'utf-8');
    console.log('Fixed.');
} else {
    console.log('Targets not found.');
}
