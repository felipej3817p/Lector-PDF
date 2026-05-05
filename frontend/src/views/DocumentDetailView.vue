<template>
  <section class="page">
    <div class="page-header">
      <div>
        <span class="mini-title">Detalle documental</span>
        <h1>Detalle de la evaluación</h1>
        <p>
          Consulta el estado del documento, analiza el PDF cuando esté pendiente y revisa el
          borrador de correo solo cuando ya exista un resultado.
        </p>
      </div>

      <div class="header-actions">
        <button
          class="primary-btn"
          :disabled="loading || analyzing"
          @click="analyzeDocument"
        >
          {{ analyzing ? 'Analizando...' : analysisData ? 'Reanalizar documento' : 'Analizar documento' }}
        </button>

        <RouterLink to="/documents" class="secondary-btn">
          Volver al panel
        </RouterLink>

        <RouterLink to="/documents/upload" class="secondary-btn">
          Subir otro PDF
        </RouterLink>
      </div>
    </div>

    <div v-if="loading" class="state-box info">
      Cargando detalle del documento...
    </div>

    <div v-else-if="error" class="state-box error">
      {{ error }}
    </div>

    <template v-else-if="documentData">
      <div v-if="analyzeMessage" class="state-box info">
        {{ analyzeMessage }}
      </div>

      <div v-if="analysisWarning" class="state-box info">
        {{ analysisWarning }}
      </div>

      <div class="summary-grid">
        <div class="summary-card">
          <span class="label">Archivo</span>
          <span>{{ documentData.originalFileName || '-' }}</span>
        </div>

        <div class="summary-card">
          <span class="label">Fecha y hora de carga</span>
          <span>{{ formatDate(documentData.uploadedAt) }}</span>
        </div>

        <div class="summary-card">
          <span class="label">Subido por</span>
          <span>{{ documentData.uploadedBy || '-' }}</span>
        </div>

        <div class="summary-card">
          <span class="label">Estado del análisis</span>
          <span>{{ analysisStateLabel }}</span>
        </div>
      </div>

      <div class="card border-0">
        <div class="card-body">
          <div class="detail-hero">
            <div>
              <span class="mini-title">Resultado médico</span>
              <h2 class="detail-title">{{ employeeDisplayName }}</h2>
              <p class="helper-text mb-0">
                {{ employeeDocumentLabel }}
                <span v-if="employeePosition"> • {{ employeePosition }}</span>
                <span v-if="employeeAreaCode"> • {{ employeeAreaCode }}</span>
              </p>
            </div>

            <div class="detail-result-box">
              <span
                v-if="normalizedResultStatus === 'APTO'"
                class="status-pill-active detail-result-pill"
              >
                APTO
              </span>

              <span
                v-else-if="normalizedResultStatus === 'NO_APTO'"
                class="status-pill-inactive detail-result-pill"
              >
                NO APTO
              </span>

              <span
                v-else
                class="status-pill-warning detail-result-pill"
              >
                PENDIENTE
              </span>
            </div>
          </div>

          <div class="hr"></div>

          <div class="fields-grid">
            <div class="field-card">
              <span class="label">Tipo de documento</span>
              <span>{{ documentData.documentType || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Tipo de examen</span>
              <span>{{ documentData.examType || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Funcionario</span>
              <span>{{ employeeDisplayName }}</span>
            </div>

            <div class="field-card">
              <span class="label">Documento</span>
              <span>{{ employeeDocumentLabel }}</span>
            </div>

            <div class="field-card">
              <span class="label">Cargo</span>
              <span>{{ employeePosition || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Área / dependencia</span>
              <span>{{ employeeArea || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Zona</span>
              <span>{{ employeeZone || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Área código</span>
              <span>{{ employeeAreaCode || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Correo funcionario</span>
              <span>{{ employeeEmail || '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="analysisData" class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Campos extraídos del PDF</h2>
              <p class="helper-text mb-0">
                Información detectada por el análisis documental.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <div class="fields-grid">
            <div class="field-card">
              <span class="label">Paciente</span>
              <span>{{ extractedFields.patientName || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Identificación</span>
              <span>{{ extractedFields.documentNumber || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Cargo</span>
              <span>{{ extractedFields.position || employeePosition || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Tipo de examen</span>
              <span>{{ extractedFields.examType || documentData.examType || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">Fecha de nacimiento</span>
              <span>{{ extractedFields.birthDate || '-' }}</span>
            </div>

            <div class="field-card">
              <span class="label">ARL</span>
              <span>{{ extractedFields.arl || '-' }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Concepto laboral</span>
              <span>{{ extractedFields.laborConcept || '-' }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Observaciones</span>
              <span>{{ extractedFields.observations || '-' }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Programa de vigilancia</span>
              <span>{{ extractedFields.surveillanceProgram || '-' }}</span>
            </div>

            <div class="field-card full-span">
              <span class="label">Remisiones</span>

              <ul
                v-if="Array.isArray(extractedFields.referrals) && extractedFields.referrals.length"
                class="mb-0 ps-3"
              >
                <li v-for="(item, index) in extractedFields.referrals" :key="index">
                  {{ item }}
                </li>
              </ul>

              <span v-else>-</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="card border-0">
        <div class="card-body">
          <div class="state-box info mb-0">
            Este documento todavía no tiene análisis guardado. Puedes abrir el detalle, revisar los
            datos base y usar el botón <strong>Analizar documento</strong>.
          </div>
        </div>
      </div>

      <div v-if="analysisData" class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Borrador de correo corporativo</h2>
              <p class="helper-text mb-0">
                El correo solo se habilita cuando el documento ya tiene análisis.
              </p>
            </div>

            <div class="header-actions">
              <button
                class="secondary-btn"
                @click="loadEmailTemplate"
                :disabled="emailLoading"
              >
                {{ emailLoading ? 'Recargando...' : 'Recargar plantilla' }}
              </button>

              <button
                class="secondary-btn"
                @click="applySuggestedTemplate"
                :disabled="emailLoading"
              >
                Aplicar plantilla sugerida
              </button>
            </div>
          </div>

          <div class="hr"></div>

          <div v-if="emailError" class="state-box error">
            {{ emailError }}
          </div>

          <div v-if="emailSuccess" class="state-box info">
            {{ emailSuccess }}
          </div>

          <div class="summary-grid">
            <div class="summary-card">
              <span class="label">Resultado base</span>
              <span>{{ normalizedResultStatus }}</span>
            </div>

            <div class="summary-card">
              <span class="label">Saludo</span>
              <span>Buenas tardes,</span>
            </div>

            <div class="summary-card">
              <span class="label">Canal principal</span>
              <span>Outlook / cliente corporativo</span>
            </div>

            <div class="summary-card">
              <span class="label">Canal alterno</span>
              <span>Gmail</span>
            </div>
          </div>

          <div class="hr"></div>

          <div class="form-grid">
            <div class="form-field full-span">
              <label class="label" for="to">Para</label>
              <input
                id="to"
                v-model="emailForm.to"
                type="text"
                class="form-control"
                placeholder="correo@empresa.com"
                :disabled="emailLoading"
              />
            </div>

            <div class="form-field full-span">
              <label class="label" for="cc">CC</label>
              <input
                id="cc"
                v-model="emailForm.cc"
                type="text"
                class="form-control"
                placeholder="correo1@empresa.com, correo2@empresa.com"
                :disabled="emailLoading"
              />
            </div>

            <div class="form-field full-span">
              <label class="label" for="subject">Asunto</label>
              <input
                id="subject"
                v-model="emailForm.subject"
                type="text"
                class="form-control"
                placeholder="Asunto del correo"
                :disabled="emailLoading"
              />
            </div>

            <div class="form-field full-span">
              <label class="label" for="body">Cuerpo</label>
              <textarea
                id="body"
                v-model="emailForm.body"
                rows="14"
                class="form-control"
                placeholder="Contenido del correo"
                :disabled="emailLoading"
              ></textarea>
            </div>
          </div>

          <div class="actions-row mt-3">
            <button
              class="primary-btn"
              @click="openOutlookMail"
              :disabled="emailLoading"
            >
              Abrir en Outlook
            </button>

            <button
              class="secondary-btn"
              @click="openMailClient"
              :disabled="emailLoading"
            >
              Abrir correo local
            </button>

            <button
              class="secondary-btn"
              @click="openGmailCompose"
              :disabled="emailLoading"
            >
              Abrir en Gmail
            </button>

            <button
              class="secondary-btn"
              @click="copyEmailDraft"
              :disabled="emailLoading"
            >
              Copiar correo
            </button>
          </div>
        </div>
      </div>

      <div v-if="analysisData" class="card border-0">
        <div class="card-body">
          <div class="page-header border-0 pb-0">
            <div>
              <h2 class="h4 mb-1">Texto extraído</h2>
              <p class="helper-text mb-0">
                Contenido crudo obtenido del PDF para revisión interna.
              </p>
            </div>
          </div>

          <div class="hr"></div>

          <textarea
            :value="analysisData.extractedText || ''"
            rows="18"
            class="form-control"
            readonly
          ></textarea>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getDocumentById, getDocumentEmailTemplate } from '../api/document'
import { getEmployeeById } from '../api/employee'
import http from '../api/http'

const route = useRoute()

const loading = ref(false)
const analyzing = ref(false)
const emailLoading = ref(false)

const error = ref('')
const analysisWarning = ref('')
const analyzeMessage = ref('')
const emailError = ref('')
const emailSuccess = ref('')

const documentData = ref(null)
const analysisData = ref(null)
const employeeData = ref(null)

const emailForm = ref({
  to: '',
  cc: '',
  subject: '',
  body: ''
})

const documentId = computed(() => route.params.id)
const extractedFields = computed(() => analysisData.value?.extractedFields || {})

const normalizedResultStatus = computed(() => {
  const result = analysisData.value?.resultStatus
  if (result === 'APTO' || result === 'NO_APTO') return result
  return 'PENDIENTE'
})

const analysisStateLabel = computed(() => {
  if (analysisData.value) return 'ANALIZADO'

  const processing = String(documentData.value?.processingStatus || '').toUpperCase()
  if (processing === 'ERROR') return 'ERROR'
  return 'PENDIENTE'
})

const employeeDisplayName = computed(() => {
  const fromEmployee = [
    employeeData.value?.firstName,
    employeeData.value?.secondName,
    employeeData.value?.firstLastName,
    employeeData.value?.secondLastName
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return fromEmployee || extractedFields.value.patientName || 'Funcionario no identificado'
})

const employeeDocumentLabel = computed(() => {
  const fromEmployee = [
    employeeData.value?.documentType,
    employeeData.value?.documentNumber
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return fromEmployee || extractedFields.value.documentNumber || '-'
})

const employeePosition = computed(() => {
  return employeeData.value?.currentPosition || extractedFields.value.position || ''
})

const employeeArea = computed(() => employeeData.value?.workArea || '')
const employeeZone = computed(() => employeeData.value?.zone || '')
const employeeEmail = computed(() => employeeData.value?.email || '')
const employeeAreaCode = computed(() => employeeData.value?.areaCode || documentData.value?.areaCode || '')

const defaultSignatureName = 'Hector Hernan Contreras Pena'
const defaultSignatureRole = 'Director de Seguridad, Salud y Ambiente'
const defaultSignaturePhone = 'Tel: +57 (8) 740 5000 Ext: 9508 | +57 315 313 6988'
const defaultSignatureAddress = 'Carrera 10 N° 15 - 87 Tunja, Boyacá, Colombia'

const formatDate = (value) => {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}

const suggestedSubject = computed(() => {
  if (normalizedResultStatus.value === 'NO_APTO') {
    return 'RESTRICCIÓN PARA TRABAJO EN ALTURAS'
  }

  if (normalizedResultStatus.value === 'APTO') {
    return 'LEVANTAMIENTO DE RESTRICCIÓN TRABAJO EN ALTURAS'
  }

  return 'SEGUIMIENTO DE CONCEPTO MÉDICO PARA TRABAJO EN ALTURAS'
})

const suggestedBody = computed(() => {
  if (normalizedResultStatus.value === 'NO_APTO') {
    return [
      'Buenas tardes,',
      '',
      'Me permito manifestarle que según el concepto médico para trabajo en alturas usted presenta una restricción temporal, por lo cual debe continuar en control y tratamiento para la condición identificada.',
      '',
      'Cordialmente,',
      defaultSignatureName,
      defaultSignatureRole,
      defaultSignaturePhone,
      defaultSignatureAddress
    ].join('\n')
  }

  if (normalizedResultStatus.value === 'APTO') {
    return [
      'Buenas tardes,',
      '',
      'Le comunico que con base en el concepto médico reciente para trabajo en alturas usted se encuentra sin restricción, por lo que puede continuar realizando sus actividades normalmente.',
      '',
      'Es importante mantener sus controles médicos y fortalecer estilos de vida saludable.',
      '',
      'Cordialmente,',
      defaultSignatureName,
      defaultSignatureRole,
      defaultSignaturePhone,
      defaultSignatureAddress
    ].join('\n')
  }

  return [
    'Buenas tardes,',
    '',
    'Le informo que el documento aún no cuenta con análisis final para generar una comunicación formal.',
    '',
    'Cordialmente,',
    defaultSignatureName,
    defaultSignatureRole,
    defaultSignaturePhone,
    defaultSignatureAddress
  ].join('\n')
})

const buildDefaultCc = () => {
  const ccList = []

  const uploader = documentData.value?.uploadedBy
  if (uploader && uploader !== '-') ccList.push(uploader)

  return [...new Set(ccList)].join(', ')
}

const applySuggestedTemplate = () => {
  emailForm.value.subject = suggestedSubject.value
  emailForm.value.body = suggestedBody.value

  if (!emailForm.value.to?.trim() && employeeEmail.value) {
    emailForm.value.to = employeeEmail.value
  }

  if (!emailForm.value.cc?.trim()) {
    emailForm.value.cc = buildDefaultCc()
  }

  emailSuccess.value = 'Se aplicó la plantilla sugerida.'
  emailError.value = ''
}

const loadEmployee = async () => {
  const employeeId = documentData.value?.employeeId
  if (!employeeId) {
    employeeData.value = null
    return
  }

  try {
    const response = await getEmployeeById(employeeId)
    employeeData.value = response.data || null
  } catch (err) {
    employeeData.value = null
    console.error('Error cargando persona asociada:', err)
  }
}

const loadAnalysis = async () => {
  analysisWarning.value = ''

  try {
    const response = await http.get(`/api/documents/${documentId.value}/analysis`)
    analysisData.value = response.data || null
  } catch (err) {
    analysisData.value = null
    analysisWarning.value = 'Este documento aún no tiene análisis guardado.'
  }
}

const loadEmailTemplate = async () => {
  if (!analysisData.value) {
    emailForm.value = {
      to: employeeEmail.value || '',
      cc: buildDefaultCc(),
      subject: suggestedSubject.value,
      body: suggestedBody.value
    }
    return
  }

  try {
    emailLoading.value = true
    emailError.value = ''
    emailSuccess.value = ''

    const response = await getDocumentEmailTemplate(documentId.value)
    const data = response.data || {}

    emailForm.value = {
      to: data.to || employeeEmail.value || '',
      cc: data.cc || buildDefaultCc(),
      subject: data.subject || suggestedSubject.value,
      body: data.body || suggestedBody.value
    }
  } catch (err) {
    emailForm.value = {
      to: employeeEmail.value || '',
      cc: buildDefaultCc(),
      subject: suggestedSubject.value,
      body: suggestedBody.value
    }

    emailError.value = 'No se pudo cargar la plantilla del backend. Se aplicó una plantilla local.'
    console.error('Error cargando plantilla de correo:', err)
  } finally {
    emailLoading.value = false
  }
}

const loadDetail = async () => {
  try {
    loading.value = true
    error.value = ''
    analysisWarning.value = ''
    analyzeMessage.value = ''

    const documentResponse = await getDocumentById(documentId.value)
    documentData.value = documentResponse.data || null

    await loadEmployee()
    await loadAnalysis()
    await loadEmailTemplate()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar el detalle del documento.'
    console.error('Error cargando detalle del documento:', err)
  } finally {
    loading.value = false
  }
}

const analyzeDocument = async () => {
  try {
    analyzing.value = true
    error.value = ''
    analyzeMessage.value = ''
    emailError.value = ''
    emailSuccess.value = ''

    await http.get(`/api/documents/${documentId.value}/analyze`)

    analyzeMessage.value = 'El análisis del documento se ejecutó correctamente.'
    await loadAnalysis()
    await loadEmailTemplate()
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo analizar el documento.'
    console.error('Error analizando documento:', err)
  } finally {
    analyzing.value = false
  }
}

const validateEmailDraft = () => {
  emailError.value = ''
  emailSuccess.value = ''

  const to = (emailForm.value.to || '').trim()
  const subject = (emailForm.value.subject || '').trim()
  const body = (emailForm.value.body || '').trim()

  if (!to) {
    emailError.value = 'El destinatario principal es obligatorio.'
    return null
  }

  if (!subject) {
    emailError.value = 'El asunto es obligatorio.'
    return null
  }

  if (!body) {
    emailError.value = 'El cuerpo del correo es obligatorio.'
    return null
  }

  return {
    to,
    cc: (emailForm.value.cc || '').trim(),
    subject,
    body
  }
}

const openMailClient = () => {
  const draft = validateEmailDraft()
  if (!draft) return

  const params = new URLSearchParams()
  if (draft.cc) params.set('cc', draft.cc)
  params.set('subject', draft.subject)
  params.set('body', draft.body)

  const mailtoUrl = `mailto:${encodeURIComponent(draft.to)}?${params.toString()}`
  window.location.href = mailtoUrl

  emailSuccess.value = 'Se abrió el cliente de correo predeterminado.'
}

const openOutlookMail = () => {
  const draft = validateEmailDraft()
  if (!draft) return

  const params = new URLSearchParams()
  params.set('path', '/mail/action/compose')
  params.set('to', draft.to)
  if (draft.cc) params.set('cc', draft.cc)
  params.set('subject', draft.subject)
  params.set('body', draft.body)

  const outlookUrl = `https://outlook.office.com/mail/deeplink/compose?${params.toString()}`
  window.open(outlookUrl, '_blank', 'noopener,noreferrer')

  emailSuccess.value = 'Se abrió un borrador en Outlook Web.'
}

const openGmailCompose = () => {
  const draft = validateEmailDraft()
  if (!draft) return

  const params = new URLSearchParams()
  params.set('view', 'cm')
  params.set('fs', '1')
  params.set('to', draft.to)
  if (draft.cc) params.set('cc', draft.cc)
  params.set('su', draft.subject)
  params.set('body', draft.body)

  const gmailUrl = `https://mail.google.com/mail/?${params.toString()}`
  window.open(gmailUrl, '_blank', 'noopener,noreferrer')

  emailSuccess.value = 'Se abrió Gmail con el borrador.'
}

const copyEmailDraft = async () => {
  const draft = validateEmailDraft()
  if (!draft) return

  const content = [
    `PARA: ${draft.to}`,
    `CC: ${draft.cc}`,
    `ASUNTO: ${draft.subject}`,
    '',
    draft.body
  ].join('\n')

  try {
    await navigator.clipboard.writeText(content)
    emailSuccess.value = 'Se copió el contenido del correo.'
  } catch (err) {
    emailError.value = 'No se pudo copiar el contenido del correo.'
    console.error('Error copiando borrador:', err)
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.detail-title {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 800;
  color: var(--text);
}

.detail-result-box {
  display: flex;
  align-items: center;
}

.detail-result-pill {
  min-height: 40px;
  padding-inline: 1rem;
  font-size: 0.9rem;
}

.fields-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.full-span {
  grid-column: 1 / -1;
}

@media (max-width: 767.98px) {
  .detail-hero {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>