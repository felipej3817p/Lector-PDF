<template>
  <section class="page employees-page">
    <div class="dashboard-toolbar compact-toolbar">
      <div>
        <span class="mini-title">Seguimiento de trabajadores</span>
        <h1 class="page-title mb-1">Trabajadores</h1>
        <p class="p mb-0">
          Consulta estado, revision y notificacion de cada trabajador.
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

        <RouterLink
          v-if="auth.canUploadDocuments && !isApproverOnly"
          to="/documents/upload"
          class="primary-btn"
        >
          Cargar evaluaciones
        </RouterLink>

        <button
          v-if="auth.canWriteEmployees && !isApproverOnly"
          type="button"
          class="secondary-btn"
          @click="openEmployeeFormModal()"
        >
          Nuevo trabajador
        </button>
      </div>
    </div>

    <div v-if="error" class="state-box error">
      {{ error }}
    </div>

    <div v-if="employeeSuccessMessage" class="state-box info">
      {{ employeeSuccessMessage }}
    </div>

    <div class="card border-0 compact-card">
      <div class="card-body">
        <div class="compact-card-header">
          <div>
            <h2 class="h4 mb-1">Filtros</h2>
            <p class="helper-text mb-0">
              Busca por cedula, nombre, cargo, zona o resultado.
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
              placeholder="Nombre, cedula, zona..."
              :disabled="loading"
            />
          </div>

          <div class="form-field">
            <label class="label" for="areaFilter">Zona o area</label>
            <select
              id="areaFilter"
              v-model="areaFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todas</option>
              <option
                v-for="area in areaOptions"
                :key="area.value"
                :value="area.value"
              >
                {{ area.label }}
              </option>
            </select>
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
              <option value="PENDIENTE">PENDIENTE</option>
              <option value="SIN_EVALUACION">SIN EVALUACION</option>
            </select>
          </div>

          <div v-if="subzoneFilterOptions.length" class="form-field">
            <label class="label" for="subzoneFilter">Subzona</label>
            <select id="subzoneFilter" v-model="subzoneFilter" class="form-select" :disabled="loading">
              <option value="">Todas las subzonas</option>
              <option v-for="subzone in subzoneFilterOptions" :key="subzone.value" :value="subzone.value">
                {{ subzone.label }}
              </option>
            </select>
          </div>

          <div v-if="canViewWorkflowDetails" class="form-field">
            <label class="label" for="reviewFilter">Revision</label>
            <select
              id="reviewFilter"
              v-model="reviewFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todas</option>
              <option value="PENDING_REVIEW">PENDIENTE</option>
              <option value="APPROVED">APROBADO</option>
              <option value="REJECTED">RECHAZADO</option>
              <option value="SIN_REVISION">SIN REVISION</option>
            </select>
          </div>

          <div v-if="canViewWorkflowDetails" class="form-field">
            <label class="label" for="notificationFilter">Notificacion</label>
            <select
              id="notificationFilter"
              v-model="notificationFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todas</option>
              <option value="SENT">ENVIADO</option>
              <option value="FAILED">FALLO</option>
              <option value="SIN_NOTIFICACION">SIN NOTIFICACION</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="statusFilter">Estado</label>
            <select
              id="statusFilter"
              v-model="statusFilter"
              class="form-select"
              :disabled="loading"
            >
              <option value="">Todos</option>
              <option value="active">Habilitados</option>
              <option value="inactive">Inhabilitados</option>
            </select>
          </div>

          <div class="form-field">
            <label class="label" for="fromDate">Desde</label>
            <input
              id="fromDate"
              v-model="fromDate"
              type="date"
              class="form-control"
              :disabled="loading"
            />
          </div>

          <div class="form-field">
            <label class="label" for="toDate">Hasta</label>
            <input
              id="toDate"
              v-model="toDate"
              type="date"
              class="form-control"
              :disabled="loading"
            />
          </div>
        </div>

        <div class="merged-table-section">
          <div v-if="loading" class="state-box info">
            Cargando trabajadores y evaluaciones...
          </div>

          <div v-else-if="!employees.length" class="state-box">
            No hay trabajadores registrados en tu alcance actual.
          </div>

          <template v-else>
            <div class="table-header-compact">
              <div>
                <div class="section-title-with-count">
                  <h2 class="h4 mb-1">Estado por trabajador</h2>

                  <span class="section-count-pill">
                    {{ filteredRows.length }} trabajador{{ filteredRows.length === 1 ? '' : 'es' }}
                  </span>
                </div>

                <p class="helper-text mb-0">
                  Vista compacta de trabajadores segun permisos del usuario.
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

            <div class="table-fit-wrapper">
              <table class="table table-hover align-middle tracking-table">
                <colgroup>
                  <col class="col-document" />
                  <col class="col-worker" />
                  <col class="col-zone" />
                  <col class="col-result" />
                  <col v-if="canViewWorkflowDetails" class="col-review" />
                  <col v-if="canViewWorkflowDetails" class="col-notification" />
                  <col v-if="!isViewerOnly" class="col-date" />
                  <col class="col-actions" />
                </colgroup>

                <thead>
                  <tr>
                    <th>Cedula</th>
                    <th>Trabajador</th>
                    <th>Zona</th>
                    <th>Resultado</th>
                    <th v-if="canViewWorkflowDetails">Revision</th>
                    <th v-if="canViewWorkflowDetails">Notificacion</th>
                    <th v-if="!isViewerOnly">Fecha evaluacion</th>
                    <th class="text-center">Acciones</th>
                  </tr>
                </thead>

                <tbody>
                  <tr
                    v-for="row in paginatedRows"
                    :key="row.employee.id"
                    :class="{
                      'row-critical': row.resultStatus === 'NO_APTO',
                      'row-pending': row.reviewStatus === 'PENDING_REVIEW',
                      'row-expired': row.evaluationExpired
                    }"
                  >
                    <td>
                      <strong class="document-cell">{{ documentLabel(row.employee) }}</strong>
                    </td>

                    <td>
                      <div class="person-meta">
                        <strong>{{ fullName(row.employee) }}</strong>
                        <small>{{ row.employee.employer || 'Sin empleador' }}</small>
                      </div>
                    </td>

                    <td>
                      <div class="person-meta">
                        <strong>{{ areaLabel(row.employee.areaCode) || 'Sin zona' }}</strong>
                        <small>{{ row.employee.zone || row.employee.workArea || 'Sin zona' }}</small>
                      </div>
                    </td>

                    <td>
                      <span :class="resultClass(row.resultStatus)">
                        {{ resultLabel(row.resultStatus) }}
                      </span>
                    </td>

                    <td v-if="canViewWorkflowDetails">
                      <span :class="reviewClass(row.reviewStatus)">
                        {{ reviewLabel(row.reviewStatus) }}
                      </span>
                    </td>

                    <td v-if="canViewWorkflowDetails">
                      <span :class="notificationClass(row.notificationStatus)">
                        {{ notificationLabel(row.notificationStatus) }}
                      </span>
                    </td>

                    <td v-if="!isViewerOnly">
                      <div class="evaluation-date-cell">
                        <strong>{{ formatDate(row.fechaConcepto || row.uploadedAt) }}</strong>
                        <small v-if="row.evaluationExpired" class="expired-evaluation-note custom-expired-text">
                          Vigencia vencida
                        </small>
                        <small v-else-if="row.daysRemaining !== null && row.daysRemaining <= 30" class="warning-evaluation-note custom-warning-text">
                          ¡Por vencer en {{ row.daysRemaining }} días!
                        </small>
                        <small v-else-if="row.expiresAt" class="valid-evaluation-note custom-valid-text">
                          Vence el: {{ formatDate(row.expiresAt) }}
                        </small>
                      </div>
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
                            @click="openEmployeeModal(row)"
                          >
                            Ver datos
                          </button>

                          <RouterLink :to="`/employees/${row.employee.id}/history`">
                            Historial
                          </RouterLink>

                          <button
                            v-if="canOpenDocumentFiles && row.documentId"
                            type="button"
                            class="view-pdf-btn-link"
                            @click="viewPdf(row.documentId)"
                          >
                            Ver PDF
                          </button>

                          <RouterLink
                            v-if="canOpenDocumentFiles && row.documentId"
                            :to="`/documents/${row.documentId}`"
                          >
                            Ver ultima evaluacion
                          </RouterLink>

                          <RouterLink
                            v-if="auth.canUploadDocuments && !row.documentId"
                            to="/documents/upload"
                          >
                            Cargar evaluacion
                          </RouterLink>

                          <button
                            v-if="auth.canWriteEmployees"
                            type="button"
                            @click="openEmployeeFormModal(row.employee)"
                          >
                            Editar
                          </button>

                        </div>
                      </details>
                    </td>
                  </tr>

                  <tr v-if="!filteredRows.length">
                    <td :colspan="employeeTableColspan">
                      <div class="state-box m-2">
                        No hay trabajadores que coincidan con los filtros seleccionados.
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
          </template>
        </div>
      </div>
    </div>

    <div
      v-if="selectedRow"
      class="employee-modal-backdrop"
      @click.self="closeEmployeeModal"
    >
      <article class="employee-modal">
        <header class="employee-modal__header">
          <div>
            <span class="mini-title">Datos del trabajador</span>
            <h2>{{ fullName(selectedRow.employee) }}</h2>
            <p class="helper-text mb-0">
              Informacion completa registrada para consulta.
            </p>
          </div>

          <button
            type="button"
            class="secondary-btn"
            @click="closeEmployeeModal"
          >
            Cerrar
          </button>
        </header>

        <div class="compact-divider"></div>

        <div class="employee-detail-grid">
          <div class="detail-field">
            <span>Documento</span>
            <strong>{{ documentLabel(selectedRow.employee) }}</strong>
          </div>

          <div class="detail-field">
            <span>Correo</span>
            <strong>{{ valueOrDash(selectedRow.employee.email) }}</strong>
          </div>

          <div class="detail-field">
            <span>Cargo</span>
            <strong>{{ valueOrDash(selectedRow.employee.currentPosition) }}</strong>
          </div>

          <div class="detail-field">
            <span>Zona o area</span>
            <strong>{{ areaLabel(selectedRow.employee.areaCode) || 'Sin zona' }}</strong>
          </div>

          <div class="detail-field">
            <span>Zona / subzona</span>
            <strong>{{ valueOrDash(selectedRow.employee.zone) }}</strong>
          </div>

          <div class="detail-field">
            <span>Empleador</span>
            <strong>{{ valueOrDash(selectedRow.employee.employer) }}</strong>
          </div>

          <div class="detail-field">
            <span>ARL</span>
            <strong>{{ valueOrDash(selectedRow.employee.arl) }}</strong>
          </div>

          <div class="detail-field">
            <span>Genero</span>
            <strong>{{ valueOrDash(selectedRow.employee.gender) }}</strong>
          </div>

          <div class="detail-field">
            <span>Fecha nacimiento</span>
            <strong>{{ formatDate(selectedRow.employee.birthDate) }}</strong>
          </div>

          <div v-if="!isViewerOnly" class="detail-field">
            <span>Ultima evaluacion</span>
            <strong>{{ formatDate(selectedRow.fechaConcepto) }}</strong>
          </div>

          <div v-if="!isViewerOnly" class="detail-field">
            <span>Vigencia evaluacion</span>
            <strong>{{ selectedRow.evaluationExpired ? 'Vencida' : selectedRow.documentId ? 'Vigente' : 'Sin evaluacion' }}</strong>
          </div>

          <div v-if="!isViewerOnly" class="detail-field">
            <span>Fecha de carga</span>
            <strong>{{ formatDate(selectedRow.uploadedAt) }}</strong>
          </div>
        </div>

        <div class="compact-divider"></div>

        <div class="employee-status-row status-row-labeled">
          <div class="status-item">
            <small>Resultado</small>
            <span :class="resultClass(selectedRow.resultStatus)">
              {{ resultLabel(selectedRow.resultStatus) }}
            </span>
          </div>

          <div v-if="canViewWorkflowDetails" class="status-item">
            <small>Revision</small>
            <span :class="reviewClass(selectedRow.reviewStatus)">
              {{ reviewLabel(selectedRow.reviewStatus) }}
            </span>
          </div>

          <div v-if="canViewWorkflowDetails" class="status-item">
            <small>Correo</small>
            <span :class="notificationClass(selectedRow.notificationStatus)">
              {{ notificationLabel(selectedRow.notificationStatus) }}
            </span>
          </div>
        </div>

        <footer class="employee-modal__footer">
          <button
            type="button"
            class="secondary-btn"
            @click="closeEmployeeModal"
          >
            Cerrar
          </button>

          <RouterLink
            :to="`/employees/${selectedRow.employee.id}/history`"
            class="primary-btn"
          >
            Ver historial
          </RouterLink>

          <button
            v-if="canOpenDocumentFiles && selectedRow.documentId"
            type="button"
            class="secondary-btn"
            @click="viewPdf(selectedRow.documentId)"
          >
            Ver PDF
          </button>

          <RouterLink
            v-if="canOpenDocumentFiles && selectedRow.documentId"
            :to="`/documents/${selectedRow.documentId}`"
            class="secondary-btn"
          >
            Ver ultima evaluacion
          </RouterLink>

          <button
            v-if="auth.canWriteEmployees"
            type="button"
            class="secondary-btn"
            @click="openEmployeeFormModal(selectedRow.employee)"
          >
            Editar trabajador
          </button>

        </footer>
      </article>
    </div>

    <div
      v-if="employeeFormOpen"
      class="employee-modal-backdrop"
      @click.self="closeEmployeeFormModal"
    >
      <article class="employee-form-modal">
        <header class="employee-modal__header">
          <div>
            <span class="mini-title">Trabajador</span>
            <h2>{{ employeeFormMode === 'create' ? 'Nuevo trabajador' : 'Editar trabajador' }}</h2>
            <p class="helper-text mb-0">
              Registra o actualiza los datos principales del trabajador.
            </p>
          </div>

          <button
            type="button"
            class="secondary-btn"
            :disabled="employeeFormSaving"
            @click="closeEmployeeFormModal"
          >
            Cerrar
          </button>
        </header>

        <div class="compact-divider"></div>

        <div v-if="employeeFormError" class="state-box error employee-form-error">
          {{ employeeFormError }}
        </div>

        <form class="employee-form" @submit.prevent="saveEmployeeForm">
          <div class="employee-form-grid">
            <div class="form-field">
              <label class="label" for="documentType">Tipo documento</label>
              <input
                id="documentType"
                v-model.trim="employeeForm.documentType"
                type="text"
                class="form-control"
                placeholder="CC"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="documentNumber">Numero documento</label>
              <input
                id="documentNumber"
                v-model.trim="employeeForm.documentNumber"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="firstName">Primer nombre</label>
              <input
                id="firstName"
                v-model.trim="employeeForm.firstName"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="secondName">Segundo nombre</label>
              <input
                id="secondName"
                v-model.trim="employeeForm.secondName"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="firstLastName">Primer apellido</label>
              <input
                id="firstLastName"
                v-model.trim="employeeForm.firstLastName"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="secondLastName">Segundo apellido</label>
              <input
                id="secondLastName"
                v-model.trim="employeeForm.secondLastName"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="email">Correo</label>
              <input
                id="email"
                v-model.trim="employeeForm.email"
                type="email"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="currentPosition">Cargo</label>
              <input
                id="currentPosition"
                v-model.trim="employeeForm.currentPosition"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="primaryAreaCode">Zona o area de trabajo</label>
              <select
                id="primaryAreaCode"
                v-model="employeePrimaryArea"
                class="form-select"
                :disabled="employeeFormSaving"
              >
                <option value="">Sin zona o area</option>
                <option
                  v-for="area in areaOptions"
                  :key="area.value"
                  :value="area.value"
                >
                  {{ area.label }}
                </option>
              </select>
            </div>

            <div v-if="employeeSubzoneOptions.length" class="form-field">
              <label class="label" for="employeeSubzone">Subzona</label>
              <select
                id="employeeSubzone"
                v-model="employeeForm.areaCode"
                class="form-select"
                :disabled="employeeFormSaving"
              >
                <option v-for="subzone in employeeSubzoneOptions" :key="subzone.value" :value="subzone.value">
                  {{ subzone.label }}
                </option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="employer">Empleador</label>
              <input
                id="employer"
                v-model.trim="employeeForm.employer"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="arl">ARL</label>
              <input
                id="arl"
                v-model.trim="employeeForm.arl"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="gender">Genero</label>
              <input
                id="gender"
                v-model.trim="employeeForm.gender"
                type="text"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="employeeEducationalLevel">Nivel Educativo</label>
              <select
                id="employeeEducationalLevel"
                v-model.trim="employeeForm.educationalLevel"
                class="form-select"
                :disabled="employeeFormSaving"
              >
                <option value="">Seleccione...</option>
                <option value="Basica Primaria">Básica Primaria</option>
                <option value="Basica Secundaria">Básica Secundaria</option>
                <option value="Tecnico">Técnico</option>
                <option value="Tecnologo">Tecnólogo</option>
                <option value="Profesional">Profesional</option>
                <option value="Especializacion">Especialización</option>
                <option value="Maestria">Maestría</option>
                <option value="Doctorado">Doctorado</option>
                <option value="Ninguno">Ninguno</option>
              </select>
            </div>

            <div class="form-field">
              <label class="label" for="birthDate">Fecha nacimiento</label>
              <input
                id="birthDate"
                v-model="employeeForm.birthDate"
                type="date"
                class="form-control"
                :disabled="employeeFormSaving"
              />
            </div>

            <div class="form-field checkbox-field">
              <label class="checkbox-label">
                <input
                  type="checkbox"
                  v-model="employeeForm.active"
                  :disabled="employeeFormSaving"
                />
                Habilitar
              </label>
            </div>

          </div>

          <footer class="employee-modal__footer">
            <button
              v-if="employeeFormMode === 'edit'"
              type="button"
              class="secondary-btn danger-btn"
              :disabled="employeeFormSaving || deletingEmployeeId === employeeForm.id"
              @click="deleteEmployeeRecord(employeeForm)"
            >
              {{ deletingEmployeeId === employeeForm.id ? 'Eliminando...' : 'Eliminar trabajador' }}
            </button>

            <button
              type="button"
              class="secondary-btn"
              :disabled="employeeFormSaving"
              @click="closeEmployeeFormModal"
            >
              Cancelar
            </button>

            <button
              type="submit"
              class="primary-btn"
              :disabled="employeeFormSaving"
            >
              {{ employeeFormSaving ? 'Guardando...' : 'Guardar trabajador' }}
            </button>
          </footer>
        </form>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getEmployees } from '../api/employee'
import { getDocuments } from '../api/document'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import {
  PRIMARY_AREA_OPTIONS,
  areaLabel,
  employeeSubzoneCode as resolveEmployeeSubzoneCode,
  normalizeAreaCode,
  primaryAreaCode,
  subzoneOptions
} from '../utils/areaCatalog'

const auth = useAuthStore()

const areaOptions = computed(() => PRIMARY_AREA_OPTIONS)

const employeeSubzoneCode = (employee) => {
  return resolveEmployeeSubzoneCode(employee?.areaCode, employee?.zone)
}

const hasRole = (roleName) => {
  const target = String(roleName || '').toUpperCase()

  const directRole = String(
    auth.user?.role ||
    auth.currentUser?.role ||
    auth.profile?.role ||
    ''
  ).toUpperCase()

  const roles = [
    ...(Array.isArray(auth.user?.roles) ? auth.user.roles : []),
    ...(Array.isArray(auth.currentUser?.roles) ? auth.currentUser.roles : []),
    ...(Array.isArray(auth.profile?.roles) ? auth.profile.roles : [])
  ].map((role) => String(role || '').toUpperCase())

  return directRole === target || roles.includes(target)
}

const isApproverOnly = computed(() => {
  return (
    (hasRole('APROBADOR') || hasRole('APPROVER')) &&
    !auth.canWriteEmployees &&
    !auth.canUploadDocuments
  )
})

const isViewerOnly = computed(() => {
  return Boolean(auth.isReadOnlyViewer)
})

const canViewWorkflowDetails = computed(() => !isViewerOnly.value)

const canOpenDocumentFiles = computed(() => !isViewerOnly.value)

const employeeTableColspan = computed(() => {
  if (isViewerOnly.value) return 4
  return canViewWorkflowDetails.value ? 8 : 6
})

const employees = ref([])
const documents = ref([])
const analysisByDocumentId = ref({})
const EVALUATION_VALIDITY_DAYS = 365
const DAY_IN_MS = 24 * 60 * 60 * 1000

const loading = ref(false)
const error = ref('')

const search = ref(sessionStorage.getItem('emp_search') || '')
const areaFilter = ref(sessionStorage.getItem('emp_area') || '')
const subzoneFilter = ref(sessionStorage.getItem('emp_subzone') || '')
const resultFilter = ref(sessionStorage.getItem('emp_result') || '')
const reviewFilter = ref(sessionStorage.getItem('emp_review') || '')
const notificationFilter = ref(sessionStorage.getItem('emp_notif') || '')
const statusFilter = ref(sessionStorage.getItem('emp_status') || 'active')
const fromDate = ref(sessionStorage.getItem('emp_from') || '')
const toDate = ref(sessionStorage.getItem('emp_to') || '')

const pageSize = ref(Number(sessionStorage.getItem('emp_size')) || 15)
const currentPage = ref(Number(sessionStorage.getItem('emp_page')) || 1)

const isInitializing = ref(true)

watch(currentPage, (val) => sessionStorage.setItem('emp_page', val))
watch(pageSize, (val) => sessionStorage.setItem('emp_size', val))
watch(search, (val) => sessionStorage.setItem('emp_search', val))
watch(areaFilter, (val) => sessionStorage.setItem('emp_area', val))
watch(subzoneFilter, (val) => sessionStorage.setItem('emp_subzone', val))
watch(resultFilter, (val) => sessionStorage.setItem('emp_result', val))
watch(reviewFilter, (val) => sessionStorage.setItem('emp_review', val))
watch(notificationFilter, (val) => sessionStorage.setItem('emp_notif', val))
watch(statusFilter, (val) => sessionStorage.setItem('emp_status', val))
watch(fromDate, (val) => sessionStorage.setItem('emp_from', val))
watch(toDate, (val) => sessionStorage.setItem('emp_to', val))
const selectedRow = ref(null)

const employeeFormOpen = ref(false)
const employeeFormMode = ref('create')
const employeeFormSaving = ref(false)
const employeeFormError = ref('')
const employeeSuccessMessage = ref('')
const deletingEmployeeId = ref('')

const createEmptyEmployeeForm = () => ({
  id: '',
  documentType: 'CC',
  documentNumber: '',
  firstName: '',
  secondName: '',
  firstLastName: '',
  secondLastName: '',
  email: '',
  currentPosition: '',
  areaCode: '',
  zone: '',
  workArea: '',
  employer: 'EBSA',
  arl: '',
  gender: '',
  educationalLevel: '',
  birthDate: '',
  active: true
})

const employeeForm = ref(createEmptyEmployeeForm())

const subzoneFilterOptions = computed(() => subzoneOptions(areaFilter.value))

const employeePrimaryArea = computed({
  get: () => primaryAreaCode(employeeForm.value.areaCode),
  set: (value) => {
    const normalized = normalizeAreaCode(value)
    if (normalized !== 'CENTRO' || !normalizeAreaCode(employeeForm.value.areaCode).startsWith('CENTRO_')) {
      employeeForm.value.areaCode = normalized
    }
  }
})

const employeeSubzoneOptions = computed(() =>
  subzoneOptions(employeePrimaryArea.value).filter((subzone) => subzone.value !== 'SIN_SUBZONA')
)

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
    .replace(/\s+/g, ' ')
    .trim() || 'Trabajador sin nombre'
}

const documentLabel = (employee) => {
  return [
    employee.documentType,
    employee.documentNumber
  ]
    .filter(Boolean)
    .join(' ')
    .trim() || '-'
}

const parseDate = (value) => {
  if (!value) return 0

  const text = String(value || '').trim()

  if (text.includes('T')) {
    const timestamp = new Date(text).getTime()
    return Number.isNaN(timestamp) ? 0 : timestamp
  }

  const localDateMatch = text.match(/^(\d{4})-(\d{2})-(\d{2})/)

  if (localDateMatch) {
    return new Date(
      Number(localDateMatch[1]),
      Number(localDateMatch[2]) - 1,
      Number(localDateMatch[3])
    ).getTime()
  }

  const time = new Date(text).getTime()

  return Number.isNaN(time) ? 0 : time
}

const getDaysRemaining = (value) => {
  const evaluationTime = parseDate(value)
  if (!evaluationTime) return null

  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const expiresAt = evaluationTime + EVALUATION_VALIDITY_DAYS * DAY_IN_MS

  return Math.ceil((expiresAt - today) / DAY_IN_MS)
}

const getExpirationTime = (value) => {
  const evaluationTime = parseDate(value)
  if (!evaluationTime) return null
  return evaluationTime + EVALUATION_VALIDITY_DAYS * DAY_IN_MS
}

const isEvaluationExpired = (value) => {
  const days = getDaysRemaining(value)
  return days !== null && days < 0
}

const documentEvaluationDate = (document) => {
  return document?.evaluationDate || document?.fechaEvaluacion || document?.fechaConcepto || ''
}

const documentSortDate = (document) => {
  return documentEvaluationDate(document) || document?.uploadedAt || document?.createdAt || ''
}

const formatDate = (value) => {
  if (!value) return '-'
  const localDate = formatLocalDate(value)
  if (localDate !== null) return localDate

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return String(value)

  return date.toLocaleDateString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const formatLocalDate = (value) => {
  const match = String(value || '').trim().match(/^(\d{4})-(\d{2})-(\d{2})/)
  return match ? `${match[3]}/${match[2]}/${match[1]}` : null
}

const toDateInputValue = (value) => {
  if (!value) return ''

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return String(value).slice(0, 10)
  }

  return date.toISOString().slice(0, 10)
}

const normalizeDateForPayload = (value) => {
  const text = String(value || '').trim()
  if (!text) return ''

  const isoDate = text.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (isoDate) return text

  const localDate = text.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/)
  if (localDate) {
    const day = localDate[1].padStart(2, '0')
    const month = localDate[2].padStart(2, '0')
    return `${localDate[3]}-${month}-${day}`
  }

  return text
}

const valueOrDash = (value) => {
  const text = String(value || '').trim()
  return text || '-'
}

const latestDocumentByEmployeeId = computed(() => {
  const map = {}

  documents.value.forEach((document) => {
    if (!document.employeeId) return
    if (isHistoricalDocument(document)) return

    const previous = map[document.employeeId]

    const documentDate = documentSortDate(document)
    const previousDate = previous
      ? documentSortDate(previous)
      : ''

    const documentTime = parseDate(documentDate)
    const previousTime = parseDate(previousDate)
    const documentUploadTime = parseDate(document.uploadedAt || document.createdAt)
    const previousUploadTime = previous ? parseDate(previous.uploadedAt || previous.createdAt) : 0

    if (
      !previous ||
      documentTime > previousTime ||
      (documentTime === previousTime && documentUploadTime > previousUploadTime)
    ) {
      map[document.employeeId] = document
    }
  })

  return map
})

const isHistoricalDocument = (document) => {
  const status = String(document?.processingStatus || '').toUpperCase()
  const comment = String(document?.reviewComment || '').toLowerCase()
  const error = String(document?.notificationError || '').toLowerCase()

  return document?.historical === true ||
    status === 'STORED' ||
    comment.includes('carga historica') ||
    error.includes('carga historica')
}

const rows = computed(() => {
  return employees.value
    .map((employee) => {
      const latestDocument = latestDocumentByEmployeeId.value[employee.id] || null
      const analysis = latestDocument ? analysisByDocumentId.value[latestDocument.id] : null
      const fechaConcepto = documentEvaluationDate(latestDocument)
      const evaluationExpired = isEvaluationExpired(fechaConcepto)
      const daysRemaining = getDaysRemaining(fechaConcepto)
      const expiresAt = getExpirationTime(fechaConcepto)

      const rawResult = String(latestDocument?.resultStatus || analysis?.resultStatus || '').toUpperCase().trim()

      let resultStatus = 'SIN_EVALUACION'

      if (rawResult === 'APTO') {
        resultStatus = 'APTO'
      } else if (rawResult === 'NO_APTO' || rawResult === 'NO APTO') {
        resultStatus = 'NO_APTO'
      } else if (latestDocument) {
        resultStatus = 'PENDIENTE'
      }

      const reviewStatus = evaluationExpired
        ? 'SIN_REVISION'
        : latestDocument?.reviewStatus || 'SIN_REVISION'
      const notificationStatus = evaluationExpired
        ? 'SIN_NOTIFICACION'
        : latestDocument?.notificationStatus || 'SIN_NOTIFICACION'

      if (evaluationExpired) {
        resultStatus = 'SIN_EVALUACION'
      }

      return {
        employee,
        documentId: latestDocument?.id || '',
        uploadedAt: latestDocument?.uploadedAt || '',
        fechaConcepto,
        evaluationExpired,
        daysRemaining,
        expiresAt,
        resultStatus,
        reviewStatus,
        notificationStatus
      }
    })
    .sort((a, b) => fullName(a.employee).localeCompare(fullName(b.employee), 'es'))
})

const filteredRows = computed(() => {
  const term = normalize(search.value)

  return rows.value.filter((row) => {
    const employee = row.employee

    if (statusFilter.value === 'active' && employee.active === false) return false
    if (statusFilter.value === 'inactive' && employee.active !== false) return false

    const employeeAreaCode = normalizeAreaCode(employee.areaCode)
    const employeePrimaryArea = primaryAreaCode(employeeAreaCode)

    const matchesArea =
      !areaFilter.value ||
      employeePrimaryArea === areaFilter.value

    const matchesSubzone =
      !subzoneFilter.value ||
      employeeSubzoneCode(employee) === subzoneFilter.value

    const matchesResult =
      !resultFilter.value ||
      row.resultStatus === resultFilter.value

    const matchesReview =
      !reviewFilter.value ||
      row.reviewStatus === reviewFilter.value

    const matchesNotification =
      !notificationFilter.value ||
      row.notificationStatus === notificationFilter.value

    const rowDate = row.fechaConcepto || row.uploadedAt
    const parsedRowDate = parseDate(rowDate)

    const matchesFrom = !fromDate.value || parsedRowDate >= new Date(fromDate.value).getTime()
    const matchesTo = !toDate.value || parsedRowDate <= new Date(toDate.value + 'T23:59:59').getTime()

    if (!matchesArea || !matchesSubzone || !matchesResult || !matchesReview || !matchesNotification || !matchesFrom || !matchesTo) {
      return false
    }

    if (!term) return true

    const haystack = [
      fullName(employee),
      documentLabel(employee),
      employee.documentNumber,
      employee.currentPosition,
      employee.workArea,
      employee.zone,
      employee.areaCode,
      areaLabel(employee.areaCode),
      employee.email,
      employee.employer,
      employee.arl,
      row.resultStatus,
      row.reviewStatus,
      row.notificationStatus,
      row.evaluationExpired ? 'evaluacion vencida vigencia vencida' : ''
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

const goToPage = (page) => {
  const nextPage = Math.min(Math.max(page, 1), totalPages.value)
  currentPage.value = nextPage
  closeAllActionMenus()
}

const resetFilters = () => {
  search.value = ''
  areaFilter.value = ''
  subzoneFilter.value = ''
  resultFilter.value = ''
  reviewFilter.value = ''
  notificationFilter.value = ''
  statusFilter.value = 'active'
  fromDate.value = ''
  toDate.value = ''
  currentPage.value = 1
  closeAllActionMenus()
}

const openEmployeeModal = (row) => {
  selectedRow.value = row
}

const closeEmployeeModal = () => {
  selectedRow.value = null
}

const openEmployeeFormModal = (employee = null) => {
  if (!auth.canWriteEmployees) return

  closeAllActionMenus()
  closeEmployeeModal()

  employeeFormError.value = ''

  if (!employee) {
    employeeFormMode.value = 'create'
    employeeForm.value = createEmptyEmployeeForm()
    employeeFormOpen.value = true
    return
  }

  employeeFormMode.value = 'edit'
  employeeForm.value = {
    id: employee.id || '',
    documentType: employee.documentType || 'CC',
    documentNumber: employee.documentNumber || '',
    firstName: employee.firstName || '',
    secondName: employee.secondName || '',
    firstLastName: employee.firstLastName || '',
    secondLastName: employee.secondLastName || '',
    email: employee.email || '',
    currentPosition: employee.currentPosition || '',
    areaCode: normalizeAreaCode(employee.areaCode),
    zone: employee.zone || '',
    workArea: employee.workArea || '',
    employer: employee.employer || 'EBSA',
    arl: employee.arl || '',
    gender: employee.gender || '',
    educationalLevel: employee.educationalLevel || '',
    birthDate: toDateInputValue(employee.birthDate),
    active: employee.active !== false
  }

  employeeFormOpen.value = true
}

const closeEmployeeFormModal = (force = false) => {
  if (employeeFormSaving.value && !force) return

  employeeFormOpen.value = false
  employeeFormError.value = ''
  employeeForm.value = createEmptyEmployeeForm()
}

const validateEmployeeForm = () => {
  if (!employeeForm.value.documentType.trim()) {
    return 'El tipo de documento es obligatorio.'
  }

  if (!employeeForm.value.documentNumber.trim()) {
    return 'El numero de documento es obligatorio.'
  }

  return ''
}

const buildEmployeePayload = () => {
  return {
    documentType: employeeForm.value.documentType.trim(),
    documentNumber: employeeForm.value.documentNumber.trim(),
    firstName: employeeForm.value.firstName.trim(),
    secondName: employeeForm.value.secondName.trim(),
    firstLastName: employeeForm.value.firstLastName.trim(),
    secondLastName: employeeForm.value.secondLastName.trim(),
    email: employeeForm.value.email.trim(),
    currentPosition: employeeForm.value.currentPosition.trim(),
    areaCode: normalizeAreaCode(employeeForm.value.areaCode) || null,
    zone: employeeForm.value.zone.trim(),
    workArea: employeeForm.value.workArea.trim(),
    employer: employeeForm.value.employer.trim(),
    arl: employeeForm.value.arl.trim(),
    gender: employeeForm.value.gender.trim(),
    educationalLevel: employeeForm.value.educationalLevel.trim(),
    birthDate: normalizeDateForPayload(employeeForm.value.birthDate),
    active: Boolean(employeeForm.value.active)
  }
}

const saveEmployeeForm = async () => {
  if (!auth.canWriteEmployees) return

  const validationError = validateEmployeeForm()

  if (validationError) {
    employeeFormError.value = validationError
    employeeSuccessMessage.value = ''
    return
  }

  try {
    employeeFormSaving.value = true
    employeeFormError.value = ''
    employeeSuccessMessage.value = ''

    const payload = buildEmployeePayload()
    const isCreateMode = employeeFormMode.value === 'create'

    if (isCreateMode) {
      await http.post('/api/employees', payload)
    } else {
      await http.put(`/api/employees/${employeeForm.value.id}`, payload)
    }

    closeEmployeeFormModal(true)
    await loadData()
    employeeSuccessMessage.value = isCreateMode
      ? 'Trabajador creado satisfactoriamente.'
      : 'Trabajador actualizado satisfactoriamente.'
  } catch (err) {
    employeeFormError.value = err?.response?.data?.message || 'No se pudo guardar el trabajador.'
    employeeSuccessMessage.value = ''
  } finally {
    employeeFormSaving.value = false
  }
}

const deleteEmployeeRecord = async (employee) => {
  if (!auth.canWriteEmployees || !employee?.id) return

  const workerName = fullName(employee) || documentLabel(employee) || 'este trabajador'
  const confirmed = window.confirm(
    `Seguro que quieres eliminar a ${workerName}? Si tiene PDFs o evaluaciones asociadas, tambien se eliminaran del historial.`
  )

  if (!confirmed) return

  try {
    deletingEmployeeId.value = employee.id
    error.value = ''
    employeeFormError.value = ''
    employeeSuccessMessage.value = ''
    closeAllActionMenus()

    await http.delete(`/api/employees/${employee.id}`)

    if (selectedRow.value?.employee?.id === employee.id) {
      closeEmployeeModal()
    }

    if (employeeFormOpen.value && employeeForm.value.id === employee.id) {
      closeEmployeeFormModal(true)
    }

    await loadData()
    employeeSuccessMessage.value = 'Trabajador eliminado satisfactoriamente.'
  } catch (err) {
    const message = err?.response?.data?.message || 'No se pudo eliminar el trabajador.'
    employeeSuccessMessage.value = ''
    if (employeeFormOpen.value && employeeForm.value.id === employee.id) {
      employeeFormError.value = message
    } else {
      error.value = message
    }
  } finally {
    deletingEmployeeId.value = ''
  }
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

    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (err) {
    if (pdfWindow) pdfWindow.close()
    console.error('Error abriendo PDF:', err)
    alert('No se pudo abrir el PDF. Es posible que el archivo no exista o no tengas permisos.')
  }
}

const closeAllActionMenus = () => {
  document
    .querySelectorAll('.employees-page .row-actions-menu[open]')
    .forEach((menu) => {
      menu.removeAttribute('open')
    })
}

const closeOtherActionMenus = (event) => {
  const currentMenu = event.currentTarget

  if (!currentMenu.open) return

  document
    .querySelectorAll('.employees-page .row-actions-menu[open]')
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
  if (status === 'PENDIENTE') return 'NO APTO'
  return 'SIN EVALUACION'
}

const reviewLabel = (status) => {
  if (status === 'PENDING_REVIEW') return 'PENDIENTE'
  if (status === 'APPROVED') return 'APROBADO'
  if (status === 'REJECTED') return 'REVISIÓN'
  return 'SIN REVISION'
}

const notificationLabel = (status) => {
  if (status === 'NOT_PENDING') return 'NO ENVIADO'
  if (status === 'SENT') return 'ENVIADO'
  if (status === 'FAILED') return 'FALLO'
  if (status === 'SKIPPED') return 'OMITIDO'
  return 'SIN NOTIFICACION'
}

const resultClass = (status) => {
  if (status === 'APTO') return 'status-pill-active'
  if (status === 'NO_APTO') return 'status-pill-inactive'
  if (status === 'PENDIENTE') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const reviewClass = (status) => {
  if (status === 'APPROVED') return 'status-pill-active'
  if (status === 'REJECTED') return 'status-pill-inactive'
  if (status === 'PENDING_REVIEW') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const notificationClass = (status) => {
  if (status === 'SENT') return 'status-pill-active'
  if (status === 'FAILED') return 'status-pill-inactive'
  if (status === 'SKIPPED') return 'status-pill-warning'
  return 'status-pill-neutral'
}

const loadData = async () => {
  try {
    loading.value = true
    error.value = ''
    closeAllActionMenus()

    const [employeesResponse, documentsResponse] = await Promise.all([
      getEmployees(),
      getDocuments({ historical: false })
    ])

    employees.value = Array.isArray(employeesResponse.data)
      ? employeesResponse.data
      : []

    documents.value = Array.isArray(documentsResponse.data)
      ? documentsResponse.data
      : []

    analysisByDocumentId.value = {}
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar el seguimiento de trabajadores.'
    console.error('Error cargando trabajadores:', err)
  } finally {
    loading.value = false
  }
}

watch(
  [search, areaFilter, subzoneFilter, resultFilter, reviewFilter, notificationFilter, statusFilter, fromDate, toDate, pageSize],
  (newVals, oldVals) => {
    if (isInitializing.value) return;
    currentPage.value = 1
    closeAllActionMenus()
  }
)

watch(areaFilter, () => {
  subzoneFilter.value = ''
})

watch(totalPages, (value) => {
  if (value > 0 && !loading.value && currentPage.value > value) {
    currentPage.value = value
  }
})

onMounted(() => {
  loadData()
  window.addEventListener('click', closeActionMenusOnOutsideClick)
  setTimeout(() => {
    isInitializing.value = false
  }, 100)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', closeActionMenusOnOutsideClick)
})
</script>

<style scoped>
.employees-page {
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
  grid-template-columns: 1.2fr repeat(6, minmax(110px, 1fr));
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

.merged-table-section {
  margin-top: 0.95rem;
}

.merged-table-section .table-header-compact {
  margin-bottom: 0.55rem;
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

.table-fit-wrapper {
  width: 100%;
  overflow: visible;
}

.tracking-table {
  width: 100%;
  table-layout: fixed;
  margin-bottom: 0;
}

.tracking-table tr:has(.row-actions-menu[open]) {
  position: relative;
  z-index: 100;
}

.tracking-table .row-expired {
  background: rgba(245, 158, 11, 0.05);
}

.col-document {
  width: 11%;
}

.col-worker {
  width: 20%;
}

.col-zone {
  width: 12%;
}

.col-result {
  width: 12%;
}

.col-review {
  width: 12%;
}

.col-notification {
  width: 14%;
}

.col-date {
  width: 9%;
}

.col-actions {
  width: 10%;
}

.tracking-table th {
  padding: 0.65rem 0.45rem;
  color: var(--text-muted);
  font-size: 0.68rem;
  line-height: 1.1;
  white-space: normal;
}

.tracking-table td {
  padding: 0.65rem 0.45rem;
  font-size: 0.78rem;
  vertical-align: middle;
  word-break: break-word;
}

.document-cell {
  font-size: 0.78rem;
}

.person-meta {
  display: flex;
  flex-direction: column;
  gap: 0.12rem;
  min-width: 0;
}

.person-meta strong {
  font-size: 0.8rem;
  line-height: 1.25;
  word-break: break-word;
}

.person-meta small,
.muted-text {
  color: var(--text-muted);
  font-size: 0.72rem;
  line-height: 1.2;
}

.evaluation-date-cell {
  display: flex;
  flex-direction: column;
  gap: 0.16rem;
  line-height: 1.2;
}

.evaluation-date-cell strong {
  font-size: 0.78rem;
}

.expired-evaluation-note {
  display: inline-flex;
  width: fit-content;
  padding: 0.18rem 0.38rem;
  border: 1px solid #fed7aa;
  border-radius: 999px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 0.63rem;
  font-weight: 800;
  text-transform: uppercase;
}

.tracking-table .status-pill-active,
.tracking-table .status-pill-inactive,
.tracking-table .status-pill-warning,
.tracking-table .status-pill-neutral {
  padding: 0.35rem 0.5rem;
  font-size: 0.68rem;
  white-space: normal;
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
  min-height: 28px;
  padding: 0.32rem 0.48rem;
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
}

.row-actions-menu summary::-webkit-details-marker {
  display: none;
}

.row-actions-menu summary::after {
  content: "";
  width: 0.38rem;
  height: 0.38rem;
  margin-left: 0.28rem;
  border-right: 1.6px solid var(--text-muted);
  border-bottom: 1.6px solid var(--text-muted);
  transform: rotate(45deg) translateY(-1px);
}

.row-actions-menu[open] summary {
  border-color: var(--primary);
}

.row-actions-menu__content {
  position: absolute;
  top: calc(100% + 0.35rem);
  right: 0;
  z-index: 200;
  width: max-content;
  min-width: 150px;
  max-width: 235px;
  padding: 0.3rem;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: var(--surface);
  box-shadow: var(--shadow-lg);
}

.custom-expired-text {
  color: #ef4444 !important;
  font-weight: bold;
}

.custom-warning-text {
  color: #f59e0b !important;
  font-weight: bold;
}

.custom-valid-text {
  color: #10b981 !important;
}

.row-actions-menu__content a,
.row-actions-menu__content button {
  display: block;
  width: 100%;
  padding: 0.48rem 0.58rem;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text);
  text-align: left;
  text-decoration: none;
  font-size: 0.74rem;
  font-weight: 700;
  line-height: 1.15;
  white-space: nowrap;
}

.row-actions-menu__content a:hover,
.row-actions-menu__content button:hover {
  background: var(--surface-soft);
}

.danger-menu-item {
  color: #991b1b !important;
}

.danger-menu-item:hover {
  background: #fee2e2 !important;
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

.row-critical {
  background: rgba(254, 242, 242, 0.7);
}

.row-pending {
  background: rgba(255, 251, 235, 0.55);
}

.employee-modal-backdrop {
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

.employee-modal,
.employee-form-modal {
  width: min(880px, 100%);
  max-height: 90vh;
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-lg);
}

.employee-form-modal {
  width: min(940px, 100%);
}

.employee-modal__header,
.employee-modal__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.15rem;
}

.employee-modal__header h2 {
  margin: 0;
  font-size: 1.18rem;
  font-weight: 800;
  color: var(--text);
}

.employee-detail-grid {
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

.employee-status-row {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0 1.15rem;
}

.status-row-labeled {
  align-items: flex-start;
}

.status-item {
  display: inline-flex;
  flex-direction: column;
  gap: 0.16rem;
}

.status-item small {
  color: var(--text-muted);
  font-size: 0.56rem;
  font-weight: 850;
  line-height: 1;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-item span {
  min-height: 21px;
  padding: 0.25rem 0.48rem;
  font-size: 0.64rem;
  line-height: 1;
}

.employee-modal__footer {
  justify-content: flex-end;
}

.employee-form {
  padding: 0 1.15rem 1.1rem;
}

.employee-form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.7rem;
}

.employee-form-grid .label {
  margin-bottom: 0.28rem;
  font-size: 0.68rem;
}

.employee-form-grid .form-control,
.employee-form-grid .form-select {
  min-height: 38px;
  padding: 0.45rem 0.7rem;
  font-size: 0.84rem;
}

.employee-active-field {
  display: flex;
  align-items: flex-end;
}

.checkbox-field {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 38px;
  color: var(--text);
  font-size: 0.84rem;
  font-weight: 700;
}

.checkbox-field input {
  width: 16px;
  height: 16px;
}

.employee-form-error {
  margin: 0 1.15rem 0.85rem;
}

@media (max-width: 1180px) {
  .compact-filters {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .employee-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .tracking-table {
    table-layout: auto;
  }

  .table-fit-wrapper {
    overflow-x: auto;
  }
}

@media (max-width: 720px) {
  .compact-toolbar,
  .compact-card-header,
  .table-header-compact,
  .google-pagination,
  .employee-modal__header,
  .employee-modal__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .compact-filters,
  .employee-detail-grid,
  .employee-form-grid {
    grid-template-columns: 1fr;
  }

  .employee-modal__footer .primary-btn,
  .employee-modal__footer .secondary-btn {
    width: 100%;
  }
}
</style>
