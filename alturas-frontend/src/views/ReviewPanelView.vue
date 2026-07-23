<template>
  <section class="page review-page">
    <div class="dashboard-toolbar compact-toolbar">
      <div>
        <span class="mini-title">Panel de revisión</span>
        <h1 class="page-title mb-1">Revisión</h1>
        <p class="p mb-0">
          Aprueba o rechaza evaluaciones médicas pendientes.
        </p>
      </div>

      <div class="header-actions compact-actions">
        <button
          type="button"
          class="secondary-btn"
          :disabled="loading"
          @click="loadData"
        >
          {{ loading ? 'Actualizando...' : 'Actualizar' }}
        </button>
      </div>
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div class="card border-0 compact-card">
      <div class="card-body">
        <div class="compact-card-header">
          <div>
            <h2 class="h4 mb-1">Filtros</h2>
            <p class="helper-text mb-0">
              Busca por trabajador, documento, archivo o zona.
            </p>
          </div>

          <button
            type="button"
            class="secondary-btn small-control-btn"
            :disabled="loading"
            @click="resetFilters"
          >
            Limpiar filtros
          </button>
        </div>

        <div class="compact-divider"></div>

        <div class="filters-grid compact-filters">
          <div class="form-field">
            <label class="label" for="search">Buscar</label>
            <input
              id="search"
              v-model.trim="search"
              type="text"
              class="form-control"
              placeholder="Trabajador, cédula, archivo..."
              :disabled="loading"
            />
          </div>

          <div class="form-field">
            <label class="label" for="resultFilter">Resultado</label>
            <select
              id="resultFilter"
              v-model="resultFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todos</option>
              <option value="APTO">APTO</option>
              <option value="NO_APTO">NO APTO</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="zoneFilter">Zona</label>
            <select
              id="zoneFilter"
              v-model="zoneFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todas</option>
              <option
                v-for="zone in zoneOptions"
                :key="zone.value"
                :value="zone.value"
              >
                {{ zone.label }}
              </option>
            </select>
          </div>
        </div>

        <div class="merged-review-section">
          <div class="table-header-compact">
            <div>
              <div class="section-title-with-count">
                <h2 class="h4 mb-1">Documentos pendientes</h2>

                <span class="section-count-pill">
                  {{ filteredRows.length }} documento{{ filteredRows.length === 1 ? '' : 's' }}
                </span>
              </div>

              <p class="helper-text mb-0">
                Solo aparecen documentos listos para revisión.
              </p>
            </div>

            <div class="pagination-size">
              <label class="label" for="pageSize">Mostrar</label>
              <select
                id="pageSize"
                v-model.number="pageSize"
                class="form-select"
              >
                <option :value="15">15</option>
                <option :value="25">25</option>
                <option :value="50">50</option>
              </select>
            </div>
          </div>

          <div class="bulk-actions-panel">
            <div class="bulk-info">
              <strong>{{ selectedIds.length }}</strong>
              <span>
                documento{{ selectedIds.length === 1 ? '' : 's' }}
                seleccionado{{ selectedIds.length === 1 ? '' : 's' }}
              </span>
            </div>

            <div class="bulk-actions">
              <button
                type="button"
                class="secondary-btn compact-action-btn"
                :disabled="!paginatedRows.length"
                @click="toggleSelectVisible"
              >
                {{ allVisibleSelected ? 'Quitar visibles' : 'Seleccionar visibles' }}
              </button>

              <button
                type="button"
                class="primary-btn compact-action-btn"
                :disabled="bulkLoading || !selectedIds.length"
                @click="openReviewActionModal('approve')"
              >
                Aprobar
              </button>

              <button
                type="button"
                class="secondary-btn danger-btn compact-action-btn"
                :disabled="bulkLoading || !selectedIds.length"
                @click="openReviewActionModal('reject')"
              >
                Revisión
              </button>
            </div>
          </div>

          <div v-if="loading" class="state-box info mb-0">
            Cargando documentos pendientes...
          </div>

          <div v-else-if="!pendingRows.length" class="state-box mb-0">
            No hay documentos pendientes de revisión.
          </div>

          <div v-else class="table-fit-wrapper">
            <table class="table table-hover align-middle review-table">
              <colgroup>
                <col class="col-select" />
                <col class="col-date" />
                <col class="col-worker" />
                <col class="col-document" />
                <col class="col-zone" />
                <col class="col-result" />
                <col class="col-actions" />
              </colgroup>

              <thead>
                <tr>
                  <th class="text-center">
                    <input
                      type="checkbox"
                      :checked="allVisibleSelected"
                      @change="toggleSelectVisible"
                    />
                  </th>
                  <th>Fecha evaluación</th>
                  <th>Trabajador</th>
                  <th>Documento</th>
                  <th>Zona</th>
                  <th>Resultado</th>
                  <th class="text-center">Acciones</th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="row in paginatedRows"
                  :key="row.id"
                  :class="{ 'row-critical': row.resultStatus === 'NO_APTO' }"
                >
                  <td class="text-center">
                    <input
                      type="checkbox"
                      :checked="selectedIds.includes(row.id)"
                      @change="toggleSelected(row.id)"
                    />
                  </td>

                  <td>
                    <div class="date-stack">
                      <strong>{{ row.evaluationDatePart }}</strong>
                      <small>Carga: {{ row.uploadedDatePart }}</small>
                    </div>
                  </td>

                  <td>
                    <div class="person-meta">
                      <strong>{{ row.fullName }}</strong>
                      <small>{{ row.email || 'Sin correo' }}</small>
                    </div>
                  </td>

                  <td>
                    <strong class="document-label">{{ row.documentLabel }}</strong>
                  </td>

                  <td>
                    <div class="person-meta">
                      <strong>{{ row.zone || row.areaCode || '-' }}</strong>
                      <small>{{ row.workArea || '-' }}</small>
                    </div>
                  </td>

                  <td>
                    <span :class="resultClass(row.resultStatus)">
                      {{ resultLabel(row.resultStatus) }}
                    </span>
                  </td>

                  <td class="text-center action-cell">
                    <details
                      class="row-actions-menu"
                      @toggle="closeOtherActionMenus"
                    >
                      <summary>Acciones</summary>

                      <div
                        class="row-actions-menu__content"
                        @click="closeAllActionMenus"
                      >
                        <button
                          type="button"
                          @click="openDetailModal(row)"
                        >
                          Ver detalle
                        </button>

                        <RouterLink :to="`/documents/${row.id}`">
                          Abrir evaluación
                        </RouterLink>

                        <button
                          type="button"
                          @click="viewPdf(row.id)"
                        >
                          Ver PDF
                        </button>
                      </div>
                    </details>
                  </td>
                </tr>

                <tr v-if="!filteredRows.length">
                  <td colspan="7">
                    <div class="state-box m-2">
                      No hay coincidencias con los filtros actuales.
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="filteredRows.length" class="pagination-bar google-pagination">
            <div class="pagination-info">
              Mostrando
              <strong>{{ pageStart }}</strong>
              -
              <strong>{{ pageEnd }}</strong>
              de
              <strong>{{ filteredRows.length }}</strong>
            </div>

            <div class="pagination-actions">
              <button
                type="button"
                class="page-nav"
                :disabled="currentPage === 1"
                @click="goToPage(currentPage - 1)"
              >
                Anterior
              </button>

              <template
                v-for="(item, index) in visiblePageItems"
                :key="`${item}-${index}`"
              >
                <span
                  v-if="item === '...'"
                  class="page-ellipsis"
                >
                  ...
                </span>

                <button
                  v-else
                  type="button"
                  class="page-number"
                  :class="{ active: item === currentPage }"
                  @click="goToPage(item)"
                >
                  {{ item }}
                </button>
              </template>

              <button
                type="button"
                class="page-nav"
                :disabled="currentPage === totalPages"
                @click="goToPage(currentPage + 1)"
              >
                Siguiente
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="false" class="card border-0 compact-card">
      <div class="card-body">
        <div class="table-header-compact">
          <div>
            <div class="section-title-with-count">
              <h2 class="h4 mb-1">Trazabilidad reciente</h2>

              <span class="section-count-pill">
                {{ filteredTraceRows.length }} registro{{ filteredTraceRows.length === 1 ? '' : 's' }}
              </span>
            </div>

            <p class="helper-text mb-0">
              Revisiones aprobadas o rechazadas con estado de notificación.
            </p>
          </div>

          <div class="pagination-size">
            <label class="label" for="tracePageSize">Mostrar</label>
            <select
              id="tracePageSize"
              v-model.number="tracePageSize"
              class="form-select"
            >
              <option :value="10">10</option>
              <option :value="15">15</option>
              <option :value="25">25</option>
              <option :value="50">50</option>
            </select>
          </div>
        </div>

        <div class="compact-divider"></div>

        <div class="filters-grid compact-filters trace-filters">
          <div class="form-field">
            <label class="label" for="traceSearch">Buscar</label>
            <input
              id="traceSearch"
              v-model.trim="traceSearch"
              type="text"
              class="form-control"
              placeholder="Trabajador, cédula, archivo..."
              :disabled="loading"
            />
          </div>

          <div class="form-field">
            <label class="label" for="traceResultFilter">Resultado</label>
            <select id="traceResultFilter" v-model="traceResultFilter" class="form-select" :disabled="loading">
              <option value="">Todos</option>
              <option value="APTO">APTO</option>
              <option value="NO_APTO">NO APTO</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="traceReviewFilter">Revisión</label>
            <select id="traceReviewFilter" v-model="traceReviewFilter" class="form-select" :disabled="loading">
              <option value="">Todas</option>
              <option value="APPROVED">Aprobadas</option>
              <option value="REJECTED">Rechazadas</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="traceNotificationFilter">Correo</label>
            <select id="traceNotificationFilter" v-model="traceNotificationFilter" class="form-select" :disabled="loading">
              <option value="">Todos</option>
              <option value="SENT">Enviado</option>
              <option value="FAILED">Falló</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="traceFrom">Desde</label>
            <input id="traceFrom" v-model="traceFrom" type="date" class="form-control" :disabled="loading" />
          </div>

          <div class="form-field">
            <label class="label" for="traceTo">Hasta</label>
            <input id="traceTo" v-model="traceTo" type="date" class="form-control" :disabled="loading" />
          </div>

          <button type="button" class="secondary-btn small-control-btn trace-clear-btn" :disabled="loading" @click="resetTraceFilters">
            Limpiar trazabilidad
          </button>
        </div>

        <div class="compact-divider"></div>

        <div v-if="!filteredTraceRows.length" class="state-box mb-0">
          No hay revisiones que coincidan con los filtros actuales.
        </div>

        <template v-else>
          <div class="table-fit-wrapper">
            <table class="table table-sm align-middle trace-table">
              <thead>
                <tr>
                  <th>Fecha de revisión</th>
                  <th>Trabajador</th>
                  <th>Resultado</th>
                  <th>Estado de revisión</th>
                  <th>Estado del correo</th>
                  <th>Revisado por</th>
                  <th class="text-center">Acciones</th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="row in tracePaginatedRows"
                  :key="row.id"
                >
                  <td>
                    <div class="date-stack">
                      <strong>{{ formatDatePart(row.reviewedAt) }}</strong>
                      <small>{{ formatTimePart(row.reviewedAt) }}</small>
                    </div>
                  </td>

                  <td>
                    <div class="person-meta">
                      <strong>{{ row.fullName }}</strong>
                      <small>{{ row.documentLabel }}</small>
                    </div>
                  </td>

                  <td>
                    <span :class="resultClass(row.resultStatus)">
                      {{ resultLabel(row.resultStatus) }}
                    </span>
                  </td>

                  <td>
                    <span :class="reviewClass(row.reviewStatus)">
                      {{ reviewLabel(row.reviewStatus) }}
                    </span>
                  </td>

                  <td>
                    <span :class="notificationClass(row.notificationStatus)">
                      {{ notificationLabel(row.notificationStatus) }}
                    </span>
                  </td>

                  <td>{{ row.reviewedBy || '-' }}</td>

                  <td class="text-center">
                    <div class="trace-inline-actions">
                      <button
                        type="button"
                        class="secondary-btn small-btn"
                        @click="openDetailModal(row)"
                      >
                        Ver detalle
                      </button>

                      <RouterLink
                        :to="`/documents/${row.id}`"
                        class="primary-btn small-btn"
                      >
                        Abrir evaluación
                      </RouterLink>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pagination-bar google-pagination">
            <div class="pagination-info">
              Mostrando
              <strong>{{ tracePageStart }}</strong>
              -
              <strong>{{ tracePageEnd }}</strong>
              de
              <strong>{{ filteredTraceRows.length }}</strong>
            </div>

            <div class="pagination-actions">
              <button
                type="button"
                class="page-nav"
                :disabled="traceCurrentPage === 1"
                @click="goToTracePage(traceCurrentPage - 1)"
              >
                Anterior
              </button>

              <template
                v-for="(item, index) in traceVisiblePageItems"
                :key="`trace-${item}-${index}`"
              >
                <span
                  v-if="item === '...'"
                  class="page-ellipsis"
                >
                  ...
                </span>

                <button
                  v-else
                  type="button"
                  class="page-number"
                  :class="{ active: item === traceCurrentPage }"
                  @click="goToTracePage(item)"
                >
                  {{ item }}
                </button>
              </template>

              <button
                type="button"
                class="page-nav"
                :disabled="traceCurrentPage === traceTotalPages"
                @click="goToTracePage(traceCurrentPage + 1)"
              >
                Siguiente
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <div
      v-if="selectedRow"
      class="review-modal-backdrop"
      @click.self="closeDetailModal"
    >
      <article class="review-modal">
        <header class="review-modal__header">
          <div>
            <span class="mini-title">Detalle de revisión</span>
            <h2>{{ selectedRow.fullName }}</h2>
            <p class="helper-text mb-0">
              Información completa del documento y estado de revisión.
            </p>
          </div>

          <button
            type="button"
            class="secondary-btn"
            @click="closeDetailModal"
          >
            Cerrar
          </button>
        </header>

        <div class="compact-divider"></div>

        <div class="review-detail-grid">
          <div class="detail-field">
            <span>Documento trabajador</span>
            <strong>{{ selectedRow.documentLabel }}</strong>
          </div>

          <div class="detail-field">
            <span>Correo</span>
            <strong>{{ selectedRow.email || '-' }}</strong>
          </div>

          <div class="detail-field">
            <span>Cargo</span>
            <strong>{{ selectedRow.currentPosition || '-' }}</strong>
          </div>

          <div class="detail-field">
            <span>Zona</span>
            <strong>{{ selectedRow.zone || selectedRow.areaCode || '-' }}</strong>
          </div>

          <div class="detail-field">
            <span>Área</span>
            <strong>{{ selectedRow.workArea || '-' }}</strong>
          </div>

          <div class="detail-field">
            <span>Resultado médico</span>
            <strong>{{ resultLabel(selectedRow.resultStatus) }}</strong>
          </div>

          <div class="detail-field">
            <span>Revisión</span>
            <strong>{{ reviewLabel(selectedRow.reviewStatus) }}</strong>
          </div>

          <div class="detail-field">
            <span>Notificación</span>
            <strong>{{ notificationLabel(selectedRow.notificationStatus) }}</strong>
          </div>

          <div class="detail-field">
            <span>Fecha evaluación</span>
            <strong>{{ formatDate(selectedRow.evaluationDate) }}</strong>
          </div>

          <div class="detail-field">
            <span>Fecha carga</span>
            <strong>{{ formatDate(selectedRow.uploadedAt) }}</strong>
          </div>

          <div class="detail-field">
            <span>Fecha revisión</span>
            <strong>{{ formatDate(selectedRow.reviewedAt) }}</strong>
          </div>

          <div class="detail-field">
            <span>Revisado por</span>
            <strong>{{ selectedRow.reviewedBy || '-' }}</strong>
          </div>

          <div class="detail-field full-detail">
            <span>Archivo</span>
            <strong>{{ selectedRow.originalFileName || '-' }}</strong>
          </div>

          <div class="detail-field full-detail">
            <span>Comentario revisión</span>
            <strong>{{ selectedRow.reviewComment || '-' }}</strong>
          </div>
        </div>

        <div class="compact-divider"></div>

        <footer class="review-modal__footer">
          <RouterLink
            :to="`/documents/${selectedRow.id}`"
            class="secondary-btn"
          >
            Abrir evaluación
          </RouterLink>
        </footer>
      </article>
    </div>

    <div
      v-if="reviewActionModalOpen"
      class="review-modal-backdrop"
      @click.self="closeReviewActionModal"
    >
      <article class="review-action-modal">
        <header class="review-modal__header">
          <div>
            <span class="mini-title">Confirmar revisión</span>
            <h2>{{ reviewActionTitle }}</h2>
            <p class="helper-text mb-0">
              {{ reviewActionDescription }}
            </p>
          </div>

          <button
            type="button"
            class="secondary-btn"
            :disabled="bulkLoading"
            @click="closeReviewActionModal"
          >
            Cerrar
          </button>
        </header>

        <div class="compact-divider"></div>

        <div class="review-action-body">
          <div class="action-summary-box">
            <span>Documentos seleccionados</span>
            <strong>{{ reviewActionIds.length }}</strong>
          </div>

          <div class="form-field">
            <label class="label" for="reviewComment">Comentario</label>
            <textarea
              id="reviewComment"
              v-model.trim="reviewActionComment"
              class="form-control review-comment-box"
              rows="4"
              placeholder="Escribe un comentario para que quede en la app y en el correo del trabajador..."
              :disabled="bulkLoading"
            ></textarea>
            <small class="helper-text">
              Este comentario quedará registrado en la revisión y se enviará al correo del trabajador.
            </small>
          </div>
        </div>

        <footer class="review-modal__footer">
          <button
            type="button"
            class="secondary-btn"
            :disabled="bulkLoading"
            @click="closeReviewActionModal"
          >
            Cancelar
          </button>

          <button
            type="button"
            :class="reviewActionType === 'approve' ? 'primary-btn' : 'secondary-btn'"
            :disabled="bulkLoading || !reviewActionIds.length"
            @click="confirmReviewAction"
          >
            {{ bulkLoading ? 'Procesando...' : reviewActionConfirmText }}
          </button>
        </footer>
      </article>
    </div>

    <Transition name="toast-slide">
      <div
        v-if="toastMessage"
        class="review-toast"
        :class="toastType"
      >
        <div class="review-toast__icon">
          {{ toastType === 'success' ? '✓' : '!' }}
        </div>

        <div class="review-toast__content">
          <strong>{{ toastTitle }}</strong>
          <span>{{ toastMessage }}</span>
        </div>
      </div>
    </Transition>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getDocuments, approveBulkDocuments, rejectBulkDocuments } from '../api/document'
import { getEmployees } from '../api/employee'
import http from '../api/http'
import { useUIStore } from '../stores/ui'
import { AREA_OPTIONS, areaLabel, normalizeAreaCode } from '../utils/areaCatalog'

const ui = useUIStore()

const documents = ref([])
const employees = ref([])
const analysisById = ref({})

const loading = ref(false)
const error = ref('')

const search = ref('')
const resultFilter = ref('')
const zoneFilter = ref('')

const selectedIds = ref([])
const bulkLoading = ref(false)
const bulkSummary = ref(null)

const toastTitle = ref('')
const toastMessage = ref('')
const toastType = ref('success')
let toastTimeout = null

const pageSize = ref(15)
const currentPage = ref(1)
const selectedRow = ref(null)

const reviewActionModalOpen = ref(false)
const reviewActionType = ref('approve')
const reviewActionIds = ref([])
const reviewActionComment = ref('')

const normalize = (value) => String(value || '').toLowerCase().trim()

const parseDate = (value) => {
  if (!value) return 0

  const time = new Date(value).getTime()

  return Number.isNaN(time) ? 0 : time
}

const formatDate = (value) => {
  if (!value) return '-'
  const localDate = formatLocalDate(value)
  if (localDate !== null) return localDate

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleString('es-CO', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDatePart = (value) => {
  if (!value) return '-'
  const localDate = formatLocalDate(value)
  if (localDate !== null) return localDate

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleDateString('es-CO', {
    timeZone: 'America/Bogota'
  })
}

const formatLocalDate = (value) => {
  const match = String(value || '').trim().match(/^(\d{4})-(\d{2})-(\d{2})/)
  return match ? `${match[3]}/${match[2]}/${match[1]}` : null
}

const formatTimePart = (value) => {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleTimeString('es-CO', {
    timeZone: 'America/Bogota',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const employeeMap = computed(() => {
  const map = {}

  for (const employee of employees.value) {
    const fullName = [
      employee.firstName,
      employee.secondName,
      employee.firstLastName,
      employee.secondLastName
    ]
      .filter(Boolean)
      .join(' ')
      .replace(/\s+/g, ' ')
      .trim()

    const documentLabel = [
      employee.documentType,
      employee.documentNumber
    ]
      .filter(Boolean)
      .join(' ')
      .trim()

    map[employee.id] = {
      fullName: fullName || 'Trabajador sin nombre',
      documentLabel: documentLabel || '-',
      currentPosition: employee.currentPosition || '',
      workArea: employee.workArea || '',
      zone: employee.zone || '',
      areaCode: employee.areaCode || '',
      email: employee.email || ''
    }
  }

  return map
})

const rows = computed(() => {
  return documents.value
    .map((doc) => {
      const analysis = analysisById.value[doc.id] || null

      const employee = employeeMap.value[doc.employeeId] || {
        fullName: 'Trabajador no identificado',
        documentLabel: doc.employeeId || '-',
        currentPosition: '',
        workArea: '',
        zone: '',
        areaCode: doc.areaCode || '',
        email: ''
      }

      const normalizedResult = String(doc.resultStatus || analysis?.resultStatus || '').toUpperCase().trim()

      const resultStatus =
        normalizedResult === 'APTO' || normalizedResult === 'NO_APTO'
          ? normalizedResult
          : 'PENDIENTE'

      return {
        id: doc.id,
        originalFileName: doc.originalFileName || '-',
        uploadedAt: doc.uploadedAt || '',
        uploadedDatePart: formatDatePart(doc.uploadedAt),
        uploadedTimePart: formatTimePart(doc.uploadedAt),
        evaluationDate: doc.evaluationDate || doc.fechaEvaluacion || doc.fechaConcepto || analysis?.evaluationDate || analysis?.fechaEvaluacion || analysis?.fechaConcepto || analysis?.conceptDate || '',
        evaluationDatePart: formatDatePart(doc.evaluationDate || doc.fechaEvaluacion || doc.fechaConcepto || analysis?.evaluationDate || analysis?.fechaEvaluacion || analysis?.fechaConcepto || analysis?.conceptDate || ''),
        uploadedBy: doc.uploadedBy || '-',
        processingStatus: doc.processingStatus || '-',
        reviewStatus: doc.reviewStatus || 'PENDING_REVIEW',
        reviewedBy: doc.reviewedBy || '',
        reviewedAt: doc.reviewedAt || '',
        reviewComment: doc.reviewComment || '',
        notificationStatus: doc.notificationStatus || 'NOT_PENDING',
        areaCode: doc.areaCode || employee.areaCode || '',
        resultStatus,
        fullName: employee.fullName,
        documentLabel: employee.documentLabel,
        currentPosition: employee.currentPosition,
        workArea: employee.workArea,
        zone: employee.zone,
        email: employee.email
      }
    })
    .sort((a, b) => parseDate(b.uploadedAt) - parseDate(a.uploadedAt))
})

const pendingRows = computed(() => {
  return rows.value.filter((row) => {
    return (
      row.reviewStatus === 'PENDING_REVIEW' &&
      (row.resultStatus === 'APTO' || row.resultStatus === 'NO_APTO')
    )
  })
})

const filteredRows = computed(() => {
  const term = normalize(search.value)

  return pendingRows.value.filter((row) => {
    const matchesResult =
      !resultFilter.value ||
      row.resultStatus === resultFilter.value

    const rowAreaCode = normalizeAreaCode(row.areaCode)
    const matchesZone =
      !zoneFilter.value ||
      rowAreaCode === zoneFilter.value

    if (!matchesResult || !matchesZone) {
      return false
    }

    if (!term) return true

    const haystack = [
      row.fullName,
      row.documentLabel,
      row.originalFileName,
      row.currentPosition,
      row.workArea,
      row.zone,
      row.areaCode,
      row.resultStatus
    ]
      .map(normalize)
      .join(' ')

    return haystack.includes(term)
  })
})

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value))
})

const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value

  return filteredRows.value.slice(start, end)
})

const pageStart = computed(() => {
  if (!filteredRows.value.length) return 0
  return (currentPage.value - 1) * pageSize.value + 1
})

const pageEnd = computed(() => {
  return Math.min(currentPage.value * pageSize.value, filteredRows.value.length)
})

const visiblePageItems = computed(() => {
  const total = totalPages.value
  const current = currentPage.value

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


const zoneOptions = computed(() => {
  const usedAreas = new Set()

  for (const row of rows.value) {
    const areaCode = normalizeAreaCode(row.areaCode)
    if (areaCode) usedAreas.add(areaCode)
  }

  return AREA_OPTIONS
    .filter((area) => usedAreas.has(area.value))
    .map((area) => ({
      value: area.value,
      label: areaLabel(area.value) || area.label
    }))
})

const allVisibleSelected = computed(() => {
  if (!paginatedRows.value.length) return false

  return paginatedRows.value.every((row) => selectedIds.value.includes(row.id))
})

const reviewActionTitle = computed(() => {
  return reviewActionType.value === 'approve'
    ? 'Aprobar evaluación'
    : 'Solicitar revisión'
})

const reviewActionDescription = computed(() => {
  const count = reviewActionIds.value.length

  if (reviewActionType.value === 'approve') {
    return `Se aprobará${count === 1 ? '' : 'n'} ${count} documento${count === 1 ? '' : 's'} y se intentará enviar correo al trabajador.`
  }

  return `Se solicitará revisión para ${count} documento${count === 1 ? '' : 's'} y se notificará al encargado.`
})

const reviewActionConfirmText = computed(() => {
  return reviewActionType.value === 'approve'
    ? 'Aprobar y enviar'
    : 'Solicitar revisión y enviar'
})

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

const showBulkNotificationToast = (actionType, actionIds) => {
  const affectedRows = rows.value.filter((row) => actionIds.includes(row.id))

  const sentCount = affectedRows.filter((row) => row.notificationStatus === 'SENT').length
  const failedCount = affectedRows.filter((row) => row.notificationStatus === 'FAILED').length
  const skippedCount = affectedRows.filter((row) => row.notificationStatus === 'SKIPPED').length
  const notPendingCount = affectedRows.filter((row) => {
    return !row.notificationStatus || row.notificationStatus === 'NOT_PENDING'
  }).length

  const actionText = actionType === 'approve' ? 'aprobaron' : 'enviaron a revisión'
  const documentText = actionIds.length === 1 ? 'documento' : 'documentos'
  const total = actionIds.length

  if (failedCount > 0) {
    showToast(
      'Correo no enviado',
      `Se ${actionText} ${total} ${documentText}, pero ${failedCount} correo${failedCount === 1 ? '' : 's'} fall${failedCount === 1 ? 'ó' : 'aron'}. Revisa el historial.`,
      'error'
    )
    return
  }

  if (sentCount > 0 && failedCount === 0 && skippedCount === 0 && notPendingCount === 0) {
    showToast(
      'Correo enviado',
      `Se ${actionText} ${total} ${documentText}. ${sentCount} correo${sentCount === 1 ? '' : 's'} enviado${sentCount === 1 ? '' : 's'} correctamente.`,
      'success'
    )
    return
  }

  if (skippedCount > 0) {
    showToast(
      'Correo omitido',
      `Se ${actionText} ${total} ${documentText}, pero ${skippedCount} correo${skippedCount === 1 ? ' fue omitido' : 's fueron omitidos'}. Revisa el historial.`,
      'warning'
    )
    return
  }

  showToast(
    'Revisión guardada',
    `Se ${actionText} ${total} ${documentText}, pero no se encontró confirmación del envío de correo. Revisa el historial.`,
    'warning'
  )
}

const goToPage = (page) => {
  const nextPage = Math.min(Math.max(page, 1), totalPages.value)
  currentPage.value = nextPage
  closeAllActionMenus()
}

const toggleSelected = (id) => {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter((item) => item !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

const toggleSelectVisible = () => {
  if (allVisibleSelected.value) {
    selectedIds.value = selectedIds.value.filter((id) => {
      return !paginatedRows.value.some((row) => row.id === id)
    })
  } else {
    const merged = new Set([
      ...selectedIds.value,
      ...paginatedRows.value.map((row) => row.id)
    ])

    selectedIds.value = [...merged]
  }
}

const openReviewActionModal = (type, ids = selectedIds.value) => {
  const validIds = Array.isArray(ids) ? ids.filter(Boolean) : []

  if (!validIds.length) {
    return
  }

  reviewActionType.value = type
  reviewActionIds.value = validIds
  reviewActionComment.value = ''
  reviewActionModalOpen.value = true
  closeAllActionMenus()
}

const closeReviewActionModal = () => {
  if (bulkLoading.value) return

  reviewActionModalOpen.value = false
  reviewActionIds.value = []
  reviewActionComment.value = ''
}

const forceCloseReviewActionModal = () => {
  reviewActionModalOpen.value = false
  reviewActionIds.value = []
  reviewActionComment.value = ''
}

const confirmReviewAction = async () => {
  const actionIds = [...reviewActionIds.value]
  const actionType = reviewActionType.value
  const comment = reviewActionComment.value || ''

  if (!actionIds.length) return

  try {
    bulkLoading.value = true
    error.value = ''
    bulkSummary.value = null

    const fn = actionType === 'approve'
      ? approveBulkDocuments
      : rejectBulkDocuments

    const { data } = await fn(actionIds, comment)

    bulkSummary.value = data || {}
    selectedIds.value = selectedIds.value.filter((id) => !actionIds.includes(id))

    forceCloseReviewActionModal()
    closeDetailModal()
    closeAllActionMenus()

    await loadData()

    showBulkNotificationToast(actionType, actionIds)
  } catch (err) {
    const message = err?.response?.data?.message || 'No se pudo completar la acción.'

    error.value = message

    showToast(
      'No se pudo completar',
      message,
      'error'
    )
  } finally {
    bulkLoading.value = false
  }
}

const resetFilters = () => {
  search.value = ''
  resultFilter.value = ''
  zoneFilter.value = ''
  selectedIds.value = []
  currentPage.value = 1
  closeAllActionMenus()
}

const openDetailModal = (row) => {
  selectedRow.value = row
  closeAllActionMenus()
}

const closeDetailModal = () => {
  selectedRow.value = null
}

const closeAllActionMenus = () => {
  document
    .querySelectorAll('.review-page .row-actions-menu[open]')
    .forEach((menu) => {
      menu.removeAttribute('open')
    })
}

const closeOtherActionMenus = (event) => {
  const currentMenu = event.currentTarget

  if (!currentMenu.open) return

  document
    .querySelectorAll('.review-page .row-actions-menu[open]')
    .forEach((menu) => {
      if (menu !== currentMenu) {
        menu.removeAttribute('open')
      }
    })
}

const closeActionMenusOnOutsideClick = (event) => {
  if (!event.target.closest('.row-actions-menu')) {
    closeAllActionMenus()
  }
}

const resultLabel = (status) => {
  if (status === 'APTO') return 'APTO'
  if (status === 'NO_APTO') return 'NO APTO'
  return status || 'PENDIENTE'
}

const resultClass = (status) => {
  if (status === 'APTO') return 'status-pill-active'
  if (status === 'NO_APTO') return 'status-pill-inactive'
  return 'status-pill-warning'
}

const reviewLabel = (status) => {
  if (status === 'PENDING_REVIEW') return 'PENDIENTE'
  if (status === 'APPROVED') return 'APROBADO'
  if (status === 'REJECTED') return 'REVISIÓN'
  return status || '-'
}

const notificationLabel = (status) => {
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  if (status === 'SENT') return 'ENVIADO'
  if (status === 'FAILED') return 'FALLÓ'
  if (status === 'SKIPPED') return 'OMITIDO'
  return status || '-'
}

const reviewClass = (status) => {
  if (status === 'APPROVED') return 'status-pill-active'
  if (status === 'REJECTED') return 'status-pill-inactive'
  return 'status-pill-warning'
}

const notificationClass = (status) => {
  if (status === 'SENT') return 'status-pill-active'
  if (status === 'FAILED') return 'status-pill-inactive'
  if (status === 'SKIPPED') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const viewPdf = async (documentId) => {
  if (!documentId) return

  const pdfWindow = window.open('', '_blank')
  if (pdfWindow) {
    pdfWindow.document.write('Cargando PDF...')
  }

  try {
    const response = await http.get(`/api/documents/${documentId}/view`, {
      responseType: 'blob'
    })

    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)

    if (pdfWindow) {
      pdfWindow.location.href = url
    } else {
      window.open(url, '_blank')
    }

    // Clean up URL object after a delay
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (err) {
    if (pdfWindow) pdfWindow.close()
    console.error('Error abriendo PDF:', err)
    ui.showAlert({
      title: 'Error',
      message: 'No se pudo abrir el PDF. Es posible que el archivo no exista o no tengas permisos.',
      type: 'error'
    })
  }
}

const loadData = async () => {
  try {
    loading.value = true
    error.value = ''
    closeAllActionMenus()

    const [documentsResponse, employeesResponse] = await Promise.all([
      getDocuments({ historical: false }),
      getEmployees()
    ])

    documents.value = Array.isArray(documentsResponse.data)
      ? documentsResponse.data
      : []

    employees.value = Array.isArray(employeesResponse.data)
      ? employeesResponse.data
      : []

    const analysisRequests = await Promise.allSettled(
      documents.value.map(async (doc) => {
        const response = await http.get(`/api/documents/${doc.id}/analysis`)
        return [doc.id, response.data]
      })
    )

    const nextAnalysis = {}

    for (const request of analysisRequests) {
      if (request.status === 'fulfilled') {
        const [documentId, analysis] = request.value
        nextAnalysis[documentId] = analysis
      }
    }

    analysisById.value = nextAnalysis
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar el panel de revisión.'
    console.error('Error cargando panel de revisión:', err)
  } finally {
    loading.value = false
  }
}

watch(
  [search, resultFilter, zoneFilter, pageSize],
  () => {
    currentPage.value = 1
    closeAllActionMenus()
  }
)

watch(totalPages, (value) => {
  if (currentPage.value > value) {
    currentPage.value = value
  }
})

onMounted(() => {
  loadData()
  window.addEventListener('click', closeActionMenusOnOutsideClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', closeActionMenusOnOutsideClick)

  if (toastTimeout) {
    clearTimeout(toastTimeout)
  }
})
</script>

<style scoped>
.review-page {
  gap: 0.8rem;
}

.compact-toolbar {
  padding: 1rem 1.15rem;
  min-height: auto;
}

.compact-toolbar .p {
  max-width: 620px;
  font-size: 0.93rem;
}

.page-title {
  margin: 0;
  font-size: clamp(1.45rem, 1.8vw, 1.85rem);
  line-height: 1.1;
  font-weight: 800;
  color: var(--text);
}

.compact-actions {
  gap: 0.55rem;
}

.compact-actions .primary-btn,
.compact-actions .secondary-btn {
  min-height: 38px;
  padding: 0.55rem 0.85rem;
}

.compact-card .card-body {
  padding: 1rem 1.15rem;
}

.compact-card-header,
.table-header-compact {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.85rem;
}

.compact-card-header h2,
.table-header-compact h2 {
  font-size: 1.25rem;
}

.small-control-btn {
  min-height: 34px;
  padding: 0.42rem 0.7rem;
  font-size: 0.78rem;
}

.compact-divider {
  height: 1px;
  margin: 0.75rem 0;
  background: var(--border);
}

.compact-filters {
  display: grid;
  grid-template-columns: 1.6fr repeat(2, minmax(140px, 1fr));
  gap: 0.65rem;
}

.compact-filters .label {
  margin-bottom: 0.3rem;
  font-size: 0.7rem;
}

.compact-filters .form-control,
.compact-filters .form-select {
  min-height: 38px;
  padding: 0.45rem 0.7rem;
  font-size: 0.86rem;
}

.merged-review-section {
  margin-top: 1rem;
}

.merged-review-section .table-header-compact {
  margin-bottom: 0.65rem;
}

.section-title-with-count {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.section-title-with-count h2 {
  margin: 0;
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

.bulk-actions-panel {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.85rem;
}

.bulk-info {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--text-muted);
  font-size: 0.82rem;
}

.bulk-info strong {
  color: var(--text);
  font-size: 0.92rem;
}

.bulk-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.compact-action-btn {
  min-height: 34px;
  padding: 0.42rem 0.62rem;
  font-size: 0.76rem;
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

.table-fit-wrapper {
  width: 100%;
  overflow: visible;
}

.review-table,
.trace-table {
  width: 100%;
  table-layout: fixed;
  margin-bottom: 0;
}

.col-select {
  width: 4%;
}

.col-date {
  width: 10%;
}

.col-worker {
  width: 24%;
}

.col-document {
  width: 14%;
}

.col-zone {
  width: 16%;
}

.col-result {
  width: 14%;
}

.col-actions {
  width: 18%;
}

.review-table th,
.trace-table th {
  padding: 0.65rem 0.45rem;
  color: var(--text-muted);
  font-size: 0.68rem;
  line-height: 1.1;
  white-space: normal;
}

.review-table td,
.trace-table td {
  padding: 0.65rem 0.45rem;
  font-size: 0.78rem;
  vertical-align: middle;
  word-break: break-word;
}

.review-table .status-pill-active,
.review-table .status-pill-inactive,
.review-table .status-pill-warning,
.review-table .status-pill-neutral,
.trace-table .status-pill-active,
.trace-table .status-pill-inactive,
.trace-table .status-pill-warning,
.trace-table .status-pill-neutral {
  padding: 0.35rem 0.5rem;
  font-size: 0.68rem;
  white-space: normal;
}

.date-stack,
.person-meta {
  display: flex;
  flex-direction: column;
  gap: 0.12rem;
  min-width: 0;
}

.date-stack strong,
.person-meta strong,
.document-label {
  color: var(--text);
  font-size: 0.8rem;
  line-height: 1.25;
  word-break: break-word;
}

.date-stack small,
.person-meta small {
  color: var(--text-muted);
  font-size: 0.72rem;
  line-height: 1.2;
}

.action-cell {
  text-align: center;
}

.row-actions-menu {
  position: relative;
  display: inline-flex;
  justify-content: center;
  width: auto;
}

.row-actions-menu summary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-width: unset;
  min-height: 26px;
  padding: 0.3rem 0.45rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  font-size: 0.7rem;
  font-weight: 750;
  line-height: 1;
  cursor: pointer;
  list-style: none;
  user-select: none;
  white-space: nowrap;
  margin: 0 auto;
}

.row-actions-menu summary::-webkit-details-marker {
  display: none;
}

.row-actions-menu summary::after {
  content: "▾";
  margin-left: 0.28rem;
  color: var(--text-muted);
}

.row-actions-menu[open] summary {
  border-color: var(--primary);
}

.row-actions-menu__content {
  position: absolute;
  top: calc(100% + 0.35rem);
  left: 50%;
  right: auto;
  z-index: 30;
  min-width: 150px;
  padding: 0.32rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: var(--shadow-md);
  transform: translateX(-50%);
}

.row-actions-menu__content a,
.row-actions-menu__content button {
  display: block;
  width: 100%;
  padding: 0.5rem 0.6rem;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--text);
  text-align: left;
  text-decoration: none;
  font-size: 0.76rem;
  font-weight: 650;
}

.row-actions-menu__content a:hover,
.row-actions-menu__content button:hover {
  background: var(--surface-soft);
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

.row-critical {
  background: rgba(254, 242, 242, 0.7);
}

.small-btn {
  min-height: 30px;
  padding: 0.36rem 0.55rem;
  font-size: 0.72rem;
}

.trace-inline-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.review-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(3px);
}

.review-modal,
.review-action-modal {
  width: min(920px, 100%);
  max-height: 90vh;
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-md);
}

.review-action-modal {
  width: min(620px, 100%);
}

.review-modal__header,
.review-modal__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.15rem;
}

.review-modal__header h2 {
  margin: 0;
  font-size: 1.18rem;
  font-weight: 800;
  color: var(--text);
}

.review-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.65rem;
  padding: 0 1.15rem;
}

.detail-field {
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.detail-field span {
  display: block;
  color: var(--text-muted);
  font-size: 0.68rem;
  font-weight: 760;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-field strong {
  display: block;
  margin-top: 0.3rem;
  color: var(--text);
  font-size: 0.86rem;
  word-break: break-word;
}

.full-detail {
  grid-column: 1 / -1;
}

.review-modal__footer {
  justify-content: flex-end;
}

.review-action-body {
  display: grid;
  gap: 0.8rem;
  padding: 0 1.15rem 1rem;
}

.action-summary-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  padding: 0.75rem 0.85rem;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface-soft);
}

.action-summary-box span {
  color: var(--text-muted);
  font-size: 0.76rem;
  font-weight: 760;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.action-summary-box strong {
  color: var(--text);
  font-size: 1.15rem;
}

.review-comment-box {
  resize: vertical;
  min-height: 92px;
  font-size: 0.86rem;
}

.review-toast {
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

.review-toast.error {
  border-color: #fecaca;
  background: #fee2e2;
  color: #991b1b;
}

.review-toast.warning {
  border-color: #fde68a;
  background: #fef3c7;
  color: #92400e;
}

.review-toast__icon {
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

.review-toast.error .review-toast__icon {
  background: rgba(153, 27, 27, 0.12);
  color: #991b1b;
}

.review-toast.warning .review-toast__icon {
  background: rgba(146, 64, 14, 0.12);
  color: #92400e;
}

.review-toast__content {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;
  min-width: 0;
}

.review-toast__content strong {
  font-size: 0.88rem;
  font-weight: 850;
  line-height: 1.2;
}

.review-toast__content span {
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

@media (max-width: 1180px) {
  .compact-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bulk-actions-panel {
    grid-template-columns: 1fr;
  }

  .bulk-actions {
    justify-content: flex-start;
  }

  .review-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .table-fit-wrapper {
    overflow-x: auto;
  }

  .review-table,
  .trace-table {
    table-layout: auto;
    min-width: 860px;
  }
}

@media (max-width: 720px) {
  .compact-toolbar,
  .compact-card-header,
  .table-header-compact,
  .google-pagination,
  .review-modal__header,
  .review-modal__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .compact-filters,
  .review-detail-grid {
    grid-template-columns: 1fr;
  }

  .review-modal__footer .primary-btn,
  .review-modal__footer .secondary-btn {
    width: 100%;
  }
}
</style>
