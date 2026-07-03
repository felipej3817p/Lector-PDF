<template>
  <section class="page settings-page">
    <div class="dashboard-toolbar settings-hero">
      <div>
        <span class="mini-title">Configuracion</span>
        <h1 class="h1 mb-2">Configuracion</h1>
        <p class="p mb-0">
          Administra correos, usuarios, zonas, permisos y apariencia.
        </p>
      </div>

      <div class="header-actions">
        <button
          type="button"
          class="secondary-btn"
          :disabled="loading || usersLoading"
          @click="refreshAll"
        >
          {{ loading || usersLoading ? 'Actualizando...' : 'Actualizar' }}
        </button>
      </div>
    </div>

    <div v-if="!auth.canAccessSettings" class="state-box error">
      No tienes permisos para administrar la configuracion.
    </div>

    <template v-else>
      <div v-if="error" class="state-box error">
        {{ error }}
      </div>

      <div v-if="successMessage" class="state-box info">
        {{ successMessage }}
      </div>

      <div class="settings-shell">
        <aside class="settings-sidebar" aria-label="Secciones de configuracion">
          <button
            v-for="section in visibleSections"
            :key="section.key"
            type="button"
            class="settings-nav-item"
            :class="{ active: activeSection === section.key }"
            @click="activeSection = section.key"
          >
            <span class="settings-nav-item__icon">{{ section.icon }}</span>

            <span>
              <strong>{{ section.label }}</strong>
              <small>{{ section.description }}</small>
            </span>
          </button>
        </aside>

        <div class="settings-content">
          <!-- GENERAL -->
          <section v-if="activeSection === 'general'" class="settings-section">
            <div class="section-heading compact-heading">
              <div>
                <span class="mini-title">General</span>
                <h2>Operacion actual</h2>
                <p>
                  Resumen del usuario actual, sus permisos y el flujo documental.
                </p>
              </div>
            </div>

            <div class="summary-grid">
              <div class="summary-card">
                <span class="label">Usuario</span>
                <span>{{ auth.user?.username || '-' }}</span>
              </div>

              <div class="summary-card">
                <span class="label">Rol</span>
                <span>{{ roleLabel }}</span>
              </div>

              <div class="summary-card">
                <span class="label">Areas</span>
                <strong class="area-scope-title" :title="areaScope.detail">
                  {{ visibleAreas }}
                </strong>
                <small class="area-scope-meta">{{ areaScopeMeta }}</small>
              </div>
            </div>

            <div v-if="auth.canManageUsers" class="card border-0">
              <div class="card-body compact-card-body">
                <div class="page-header border-0 pb-0">
                  <div>
                    <h3 class="h4 mb-1">Retencion de auditoria</h3>
                    <p class="helper-text mb-0">
                      Define durante cuanto tiempo se conservan los cambios de usuarios, roles, zonas y vigencias.
                    </p>
                  </div>
                </div>

                <div class="hr"></div>

                <div class="audit-retention-row">
                  <div class="form-field">
                    <label class="label" for="auditRetentionMonths">Guardar registros durante</label>
                    <select
                      id="auditRetentionMonths"
                      v-model.number="auditRetentionMonths"
                      class="form-select"
                      :disabled="loading"
                    >
                      <option
                        v-for="option in auditRetentionOptions"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </option>
                    </select>
                    <p class="helper-text mb-0">
                      Los registros mas antiguos se limpiaran automaticamente al consultar o registrar auditoria.
                    </p>
                  </div>

                  <button
                    type="button"
                    class="primary-btn"
                    :disabled="loading"
                    @click="saveAuditRetention"
                  >
                    {{ loading ? 'Guardando...' : 'Guardar auditoria' }}
                  </button>
                </div>
              </div>
            </div>

            <div class="card border-0">
              <div class="card-body compact-card-body">
                <div class="page-header border-0 pb-0">
                  <div>
                    <h3 class="h4 mb-1">Flujo documental</h3>
                    <p class="helper-text mb-0">
                      Regla funcional aplicada actualmente.
                    </p>
                  </div>
                </div>

                <div class="hr"></div>

                <div class="business-flow-grid">
                  <div class="business-flow-card">
                    <span class="status-dot success"></span>
                    <strong>PDF analizado</strong>
                    <p>
                      El sistema extrae informacion y deja el documento pendiente de revision.
                    </p>
                  </div>

                  <div class="business-flow-card">
                    <span class="status-dot warning"></span>
                    <strong>Documento aprobado</strong>
                    <p>
                      Se notifica al trabajador y a las copias configuradas.
                    </p>
                  </div>

                  <div class="business-flow-card">
                    <span class="status-dot danger"></span>
                    <strong>Documento rechazado</strong>
                    <p>
                      Se notifica al trabajador con el comentario de revision.
                    </p>
                  </div>

                  <div class="business-flow-card">
                    <span class="status-dot neutral"></span>
                    <strong>Constancia</strong>
                    <p>
                      Se habilita cuando el proceso esta aprobado, notificado y sin errores.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- CORREOS -->
          <section v-if="activeSection === 'emails'" class="settings-section">
            <div class="section-heading compact-heading">
              <div>
                <span class="mini-title">Correos</span>
                <h2>Destinatarios y reglas de envio</h2>
                <p>
                  Configura aprobadores, copias, areas y coordinadores desde un solo modulo.
                </p>
              </div>
            </div>

            <div class="mail-grid">
              <article
                v-for="group in mailGroupList"
                :key="group.key"
                class="mail-card"
              >
                <div class="mail-card__header">
                  <span class="mini-title">{{ group.step }}</span>
                  <h3>{{ group.title }}</h3>
                  <p>{{ group.description }}</p>
                </div>

                <div class="mail-add-row">
                  <input
                    v-model.trim="mailGroups[group.key].input"
                    type="email"
                    class="form-control"
                    :placeholder="group.placeholder"
                    @keyup.enter="addMail(group.key)"
                  />

                  <button
                    type="button"
                    class="secondary-btn"
                    @click="addMail(group.key)"
                  >
                    Agregar
                  </button>
                </div>

                <div v-if="mailGroups[group.key].items.length" class="chip-list">
                  <span
                    v-for="email in mailGroups[group.key].items"
                    :key="`${group.key}-${email}`"
                    class="mail-chip"
                  >
                    {{ email }}

                    <button
                      type="button"
                      aria-label="Quitar correo"
                      @click="removeMail(group.key, email)"
                    >
                      x
                    </button>
                  </span>
                </div>

                <div v-else class="empty-mail">
                  Sin correos configurados.
                </div>
              </article>
            </div>

            <div class="card border-0">
              <div class="card-body compact-card-body">
                <div class="page-header border-0 pb-0">
                  <div>
                    <span class="mini-title">Copias por zona</span>
                    <h3 class="h4 mb-1">Coordinadores por zona</h3>
                    <p class="helper-text mb-0">
                      Correos regionales que van como copia cuando se notifica al trabajador.
                    </p>
                  </div>
                </div>

                <div class="hr"></div>

                <div class="zone-grid">
                  <div
                    v-for="area in allAreas"
                    :key="area"
                    class="zone-card"
                  >
                    <label class="label" :for="`zone-${area}`">{{ areaLabel(area) || area }}</label>

                    <div class="mail-add-row">
                      <input
                        :id="`zone-${area}`"
                        v-model.trim="coordinatorSettings[area].input"
                        type="email"
                        class="form-control"
                        placeholder="coordinador@empresa.com"
                        @keyup.enter="addCoordinator(area)"
                      />

                      <button
                        type="button"
                        class="secondary-btn"
                        @click="addCoordinator(area)"
                      >
                        Agregar
                      </button>
                    </div>

                    <div v-if="coordinatorSettings[area].items.length" class="chip-list">
                      <span
                        v-for="email in coordinatorSettings[area].items"
                        :key="`${area}-${email}`"
                        class="mail-chip"
                      >
                        {{ email }}

                        <button
                          type="button"
                          aria-label="Quitar correo"
                          @click="removeCoordinator(area, email)"
                        >
                          x
                        </button>
                      </span>
                    </div>

                    <div v-else class="empty-mail">
                      Sin coordinadores.
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="card border-0">
              <div class="card-body compact-card-body">
                <div class="page-header border-0 pb-0">
                  <div>
                    <h3 class="h4 mb-1">Regla activa</h3>
                    <p class="helper-text mb-0">
                      Flujo de envio aplicado por el sistema.
                    </p>
                  </div>
                </div>

                <div class="hr"></div>

                <div class="rule-list">
                  <span class="status-pill-active">PDF analizado -> aprobador</span>
                  <span class="status-pill-warning">Aprobado -> trabajador + copias</span>
                  <span class="status-pill-neutral">Rechazado -> trabajador + comentario</span>
                </div>

                <div class="actions-row mt-4">
                  <button
                    type="button"
                    class="primary-btn"
                    :disabled="loading"
                    @click="saveLocalSettings"
                  >
                    {{ loading ? 'Guardando...' : 'Guardar correos' }}
                  </button>
                </div>
              </div>
            </div>
          </section>

          <!-- USUARIOS -->
          <section v-if="activeSection === 'users'" class="settings-section">
            <div class="section-heading compact-heading">
              <div>
                <span class="mini-title">Usuarios</span>
                <h2>Usuarios y roles</h2>
                <p>
                  Gestiona cuentas, permisos, zonas y estado de acceso.
                </p>
              </div>

              <div class="header-actions">
                <RouterLink to="/settings/users/audit" class="secondary-btn">
                  Auditoria
                </RouterLink>

                <button
                  type="button"
                  class="primary-btn"
                  @click="startCreate"
                >
                  Nuevo usuario
                </button>
              </div>
            </div>

            <div v-if="usersError" class="state-box error">
              {{ usersError }}
            </div>

            <div class="card border-0">
              <div class="card-body compact-card-body">
                <div class="filters-grid">
                  <div class="form-field">
                    <label class="label" for="search">Buscar</label>
                    <input
                      id="search"
                      v-model.trim="search"
                      type="text"
                      class="form-control"
                      placeholder="Usuario, correo, rol, zona..."
                      :disabled="usersLoading"
                    />
                  </div>

                  <div class="form-field">
                    <label class="label" for="roleFilter">Rol</label>
                    <select
                      id="roleFilter"
                      v-model="roleFilter"
                      class="form-select"
                      :disabled="usersLoading"
                    >
                      <option value="">Todos</option>
                      <option value="ADMIN">ADMIN</option>
                      <option value="APROBADOR">APROBADOR</option>
                      <option value="OPERADOR">OPERADOR</option>
                      <option value="VISUALIZADOR">VISUALIZADOR</option>
                    </select>
                  </div>

                  <div class="form-field">
                    <label class="label" for="enabledFilter">Estado</label>
                    <select
                      id="enabledFilter"
                      v-model="enabledFilter"
                      class="form-select"
                      :disabled="usersLoading"
                    >
                      <option value="">Todos</option>
                      <option value="true">ACTIVO</option>
                      <option value="false">INACTIVO</option>
                    </select>
                  </div>

                  <div class="form-field">
                    <label class="label" for="areaFilter">Zona</label>
                    <select
                      id="areaFilter"
                      v-model="areaFilter"
                      class="form-select"
                      :disabled="usersLoading"
                    >
                      <option value="">Todas</option>
                      <option
                        v-for="area in areaOptions"
                        :key="area"
                        :value="area"
                      >
                        {{ areaLabel(area) || area }}
                      </option>
                    </select>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="usersLoading" class="state-box info">
              Cargando usuarios...
            </div>

            <div v-else-if="!users.length" class="state-box">
              No hay usuarios registrados todavia.
            </div>

            <div v-else class="card border-0">
              <div class="card-body compact-card-body">
                <div v-if="filteredUsers.length" class="users-card-list">
                  <article v-for="user in paginatedUsers" :key="user.id" class="user-account-card">
                    <div class="user-account-card__identity">
                      <div class="user-account-card__avatar">{{ userInitials(user) }}</div>
                      <div>
                        <strong>{{ user.username }}</strong>
                        <small :title="user.email || '-'">{{ user.email || '-' }}</small>
                      </div>
                    </div>

                    <div class="user-account-card__detail">
                      <span>Rol</span>
                      <strong>{{ formatRoles(user.roles) }}</strong>
                    </div>

                    <div class="user-account-card__detail">
                      <span>Zonas</span>
                      <strong>{{ formatUserAreas(user) }}</strong>
                      <details v-if="userAreaDetail(user)" class="area-detail-disclosure">
                        <summary>Ver zonas</summary>
                        <small>{{ userAreaDetail(user) }}</small>
                      </details>
                    </div>

                    <div class="user-account-card__detail">
                      <span>Vigencia</span>
                      <div class="validity-period">
                        <span>
                          <small>Desde</small>
                          <strong>{{ formatValidityDate(user.accountStartDate, 'Acceso inmediato') }}</strong>
                        </span>
                        <span>
                          <small>Hasta</small>
                          <strong>{{ formatValidityDate(user.accountExpirationDate, 'Sin fecha limite') }}</strong>
                        </span>
                      </div>
                    </div>

                    <div class="user-account-card__detail">
                      <span>Estado de acceso</span>
                      <strong>
                        <span :class="userAccessStatus(user).className">
                          {{ userAccessStatus(user).label }}
                        </span>
                      </strong>
                      <small>{{ userAccessStatus(user).detail }}</small>
                    </div>

                    <div class="user-account-card__actions">
                      <div class="user-actions-menu">
                        <button
                          type="button"
                          class="secondary-btn user-actions-menu__trigger"
                          :aria-expanded="openActionsUserId === user.id"
                          @click="toggleActionsMenu(user.id)"
                        >
                          Acciones
                        </button>

                        <div v-if="openActionsUserId === user.id" class="user-actions-menu__panel">
                          <button type="button" @click="runUserAction(() => startEdit(user))">
                            Editar usuario
                          </button>
                          <button
                            v-if="!isAccessExpired(user)"
                            type="button"
                            :class="{ danger: user.enabled }"
                            :disabled="togglingId === user.id"
                            @click="runUserAction(() => toggleUserEnabled(user))"
                          >
                            {{ togglingId === user.id ? 'Actualizando...' : user.enabled ? 'Inactivar usuario' : 'Activar usuario' }}
                          </button>
                          <button
                            type="button"
                            class="danger"
                            :disabled="togglingId === user.id"
                            @click="runUserAction(() => deleteUser(user))"
                          >
                            Eliminar usuario
                          </button>
                        </div>
                      </div>
                    </div>
                  </article>
                </div>

                <div v-else class="state-box">
                  No hay coincidencias con los filtros actuales.
                </div>

                <div v-if="filteredUsers.length" class="users-pagination">
                  <small>{{ paginationSummary }}</small>

                  <div>
                    <button
                      type="button"
                      class="secondary-btn"
                      :disabled="currentPage === 1"
                      @click="currentPage -= 1"
                    >
                      Anterior
                    </button>

                    <span>Pagina {{ currentPage }} de {{ totalPages }}</span>

                    <button
                      type="button"
                      class="secondary-btn"
                      :disabled="currentPage === totalPages"
                      @click="currentPage += 1"
                    >
                      Siguiente
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- APARIENCIA -->
          <section v-if="activeSection === 'appearance'" class="settings-section">
            <div class="section-heading compact-heading">
              <div>
                <span class="mini-title">Apariencia</span>
                <h2>Tema de la aplicacion</h2>
                <p>
                  La preferencia se guarda en el navegador.
                </p>
              </div>
            </div>

            <div class="card border-0">
              <div class="card-body compact-card-body">
                <div class="theme-options">
                  <button
                    v-for="option in themeOptions"
                    :key="option.value"
                    type="button"
                    class="theme-option"
                    :class="{ active: selectedTheme === option.value }"
                    @click="setTheme(option.value)"
                  >
                    <span
                      class="theme-option__preview"
                      :class="`theme-option__preview--${option.value}`"
                    ></span>

                    <strong>{{ option.label }}</strong>
                    <small>{{ option.description }}</small>
                  </button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- MODAL USUARIO -->
      <div v-if="showForm" class="modal-backdrop-custom" @click.self="closeForm">
        <section class="user-modal">
          <div class="user-modal__header">
            <div>
              <span class="mini-title">Usuario</span>
              <h2>{{ editingId ? 'Editar usuario' : 'Nuevo usuario' }}</h2>
              <p>
                Define cuenta, rol, zonas permitidas y estado.
              </p>
            </div>

            <button
              type="button"
              class="secondary-btn"
              :disabled="saving"
              @click="closeForm"
            >
              Cerrar
            </button>
          </div>

          <div v-if="formError" class="state-box error">
            {{ formError }}
          </div>

          <form class="form-grid" @submit.prevent="saveUser">
            <div class="form-field">
              <label class="label" for="username">Usuario</label>
              <input
                id="username"
                v-model.trim="form.username"
                type="text"
                class="form-control"
                :disabled="saving"
              />
            </div>

            <div class="form-field">
              <label class="label" for="email">Correo</label>
              <input
                id="email"
                v-model.trim="form.email"
                type="email"
                class="form-control"
                :disabled="saving"
              />
            </div>

            <div class="form-field">
              <label v-if="editingId" class="label">Contraseña</label>
              <label v-else class="label" for="password">Contraseña</label>

              <label v-if="editingId" class="checkbox-field">
                <input
                  v-model="form.changePassword"
                  type="checkbox"
                  :disabled="saving"
                  @change="handleChangePasswordToggle"
                />
                <span>Cambiar contraseña</span>
              </label>

              <input
                v-if="!editingId || form.changePassword"
                id="password"
                v-model="form.password"
                :type="showUserPassword ? 'text' : 'password'"
                class="form-control"
                autocomplete="new-password"
                placeholder="Ingresa una nueva contraseña"
                :disabled="saving"
              />

              <button
                v-if="!editingId || form.changePassword"
                type="button"
                class="link password-toggle-btn"
                :disabled="saving"
                @click="showUserPassword = !showUserPassword"
              >
                {{ showUserPassword ? 'Ocultar contraseña' : 'Ver contraseña' }}
              </button>

              <small v-if="editingId && !form.changePassword" class="helper-text">
                La contraseña actual se conservara sin cambios.
              </small>
            </div>

            <div class="form-field full-span">
              <label class="label">Roles con vigencia</label>

              <div class="assignment-grid">
                <div
                  v-for="role in roleOptions"
                  :key="role"
                  class="assignment-card"
                  :class="{ selected: hasRoleAssignment(role) }"
                >
                  <label class="checkbox-field">
                    <input
                      type="checkbox"
                      :checked="hasRoleAssignment(role)"
                      :disabled="saving"
                      @change="toggleRoleAssignment(role)"
                    />
                    <span>{{ displayRole(role) }}</span>
                  </label>

                  <div v-if="hasRoleAssignment(role)" class="assignment-dates">
                    <label>
                      <small>Desde</small>
                      <input
                        v-model="findRoleAssignment(role).startDate"
                        type="datetime-local"
                        class="form-control"
                        :disabled="saving"
                      />
                    </label>

                    <label>
                      <small>Hasta</small>
                      <input
                        v-model="findRoleAssignment(role).endDate"
                        type="datetime-local"
                        class="form-control"
                        :disabled="saving"
                      />
                    </label>

                    <label class="checkbox-field compact-check">
                      <input
                        v-model="findRoleAssignment(role).enabled"
                        type="checkbox"
                        :disabled="saving"
                      />
                      <span>Rol activo</span>
                    </label>
                  </div>
                </div>
              </div>

              <small class="helper-text">
                Puedes asignar varios roles. Si un rol vence, solo se bloquea ese rol; el usuario conserva los demas roles vigentes.
              </small>
            </div>

            <div class="form-field full-span">
              <label class="label">Zonas permitidas</label>

              <label class="global-access-option">
                <input
                  v-model="form.globalAreaAccess"
                  type="checkbox"
                  :disabled="saving || form.roles.includes('ADMIN') || form.roles.includes('SUPER_ADMIN') || form.roles.includes('APROBADOR')"
                  @change="toggleGlobalAreaAccess"
                />

                <span>
                  <strong>Acceso global a zonas</strong>
                  <small>Permite consultar todas las zonas sin seleccionarlas manualmente.</small>
                </span>
              </label>

              <div class="area-grid area-assignment-grid">
                <div
                  v-for="area in allAreas"
                  :key="area"
                  class="area-option"
                >
                  <label class="checkbox-field">
                    <input
                      type="checkbox"
                      :checked="hasAreaAssignment(area)"
                      :disabled="saving || form.globalAreaAccess || form.roles.includes('ADMIN') || form.roles.includes('SUPER_ADMIN') || form.roles.includes('APROBADOR')"
                      @change="toggleAreaAssignment(area)"
                    />

                    <span>{{ areaLabel(area) || area }}</span>
                  </label>

                  <div v-if="hasAreaAssignment(area)" class="assignment-dates compact">
                    <label>
                      <small>Desde</small>
                      <input
                        v-model="findAreaAssignment(area).startDate"
                        type="datetime-local"
                        class="form-control"
                        :disabled="saving || form.globalAreaAccess"
                      />
                    </label>

                    <label>
                      <small>Hasta</small>
                      <input
                        v-model="findAreaAssignment(area).endDate"
                        type="datetime-local"
                        class="form-control"
                        :disabled="saving || form.globalAreaAccess"
                      />
                    </label>
                  </div>
                </div>
              </div>

              <small class="helper-text">
                ADMIN y APROBADOR tienen alcance global. OPERADOR y VISUALIZADOR requieren acceso global o al menos una zona.
                Las direcciones de mantenimiento, operacion y perdidas son dependencias operativas para clasificar trabajadores, documentos y correos.
              </small>
            </div>

            <div class="form-field full-span">
              <label class="label">Estado</label>

              <div class="field-card">
                <label class="checkbox-field">
                  <input
                    v-model="form.enabled"
                    type="checkbox"
                    :disabled="saving"
                  />
                  <span>Usuario habilitado</span>
                </label>
              </div>
            </div>

            <div class="form-field full-span">
              <label class="label">Vigencia general de cuenta</label>

              <div class="field-card account-validity-card">
                <label class="checkbox-field">
                  <input
                    v-model="form.useAccountValidity"
                    type="checkbox"
                    :disabled="saving"
                    @change="handleAccountValidityToggle"
                  />
                  <span>Definir fechas para bloquear o habilitar toda la cuenta</span>
                </label>

                <small class="helper-text">
                  Usalo solo cuando quieras limitar el ingreso completo del usuario. Para permisos temporales usa la vigencia de cada rol o zona.
                </small>

                <div v-if="form.useAccountValidity" class="account-validity-grid">
                  <div class="form-field">
                    <label class="label" for="accountStartDate">Desde</label>
                    <input id="accountStartDate" v-model="form.accountStartDate" type="datetime-local" class="form-control" :disabled="saving" />
                  </div>

                  <div class="form-field">
                    <label class="label" for="accountExpirationDate">Hasta</label>
                    <input id="accountExpirationDate" v-model="form.accountExpirationDate" type="datetime-local" class="form-control" :disabled="saving" />
                    <small class="helper-text">Dejalo vacio si la cuenta no tiene fecha limite.</small>
                  </div>
                </div>
              </div>
            </div>

            <div class="full-span actions-row modal-actions">
              <button
                type="button"
                class="secondary-btn"
                :disabled="saving"
                @click="closeForm"
              >
                Cancelar
              </button>

              <button
                type="submit"
                class="primary-btn"
                :disabled="saving"
              >
                {{ saving ? 'Guardando...' : editingId ? 'Actualizar usuario' : 'Crear usuario' }}
              </button>
            </div>
          </form>
        </section>
      </div>

    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'
import { getSystemSettings, updateSystemSettings } from '../api/systemSettings'
import { useAuthStore } from '../stores/auth'
import { AREA_CODES, areaLabel, areaScopeSummary, normalizeAreaCode, normalizeAreaList } from '../utils/areaCatalog'
import { applyTheme, getStoredTheme, saveTheme } from '../utils/themePreferences'

const auth = useAuthStore()

const PRODUCTION_FRONTEND_URL = 'https://sstalturas.ebsa.com.co'

const allAreas = AREA_CODES

const sections = [
  {
    key: 'general',
    label: 'General',
    description: 'Flujo y accesos',
    icon: 'G'
  },
  {
    key: 'emails',
    label: 'Correos',
    description: 'Destinatarios',
    icon: 'C'
  },
  {
    key: 'users',
    label: 'Usuarios',
    description: 'Roles y zonas',
    icon: 'U'
  },
  {
    key: 'appearance',
    label: 'Apariencia',
    description: 'Claro / oscuro',
    icon: 'A'
  }
]

const visibleSections = computed(() => sections.filter((section) => {
  if (section.key === 'emails') return auth.canEditEmailSettings
  if (section.key === 'users') return auth.canManageUsers
  return true
}))

const themeOptions = [
  {
    value: 'light',
    label: 'Claro',
    description: 'Interfaz luminosa para uso diario.'
  },
  {
    value: 'dark',
    label: 'Oscuro',
    description: 'Contraste oscuro para baja iluminacion.'
  },
  {
    value: 'system',
    label: 'Sistema',
    description: 'Sigue la preferencia del equipo.'
  }
]

const mailGroupList = [
  {
    key: 'approver',
    step: 'Primer envio',
    title: 'Aprobadores',
    description: 'Reciben correo cuando se cargan y analizan PDFs.',
    placeholder: 'aprobador@empresa.com'
  },
  {
    key: 'approverCc',
    step: 'Primer envio',
    title: 'Copias al aprobador',
    description: 'Copias adicionales del correo inicial al aprobador.',
    placeholder: 'copia.aprobador@empresa.com'
  },
  {
    key: 'humanTalent',
    step: 'Segundo envio',
    title: 'Talento humano',
    description: 'Copias cuando se aprueba y notifica al trabajador.',
    placeholder: 'talento@empresa.com'
  },
  {
    key: 'payroll',
    step: 'Segundo envio',
    title: 'Nomina',
    description: 'Copias cuando se aprueba y notifica al trabajador.',
    placeholder: 'nomina@empresa.com'
  }
]

const auditRetentionOptions = [
  { value: 0, label: '24 horas' },
  { value: 3, label: '3 meses' },
  { value: 6, label: '6 meses' },
  { value: 12, label: '12 meses' },
  { value: 24, label: '24 meses' },
  { value: 60, label: '60 meses' }
]

const activeSection = ref('general')

const loading = ref(false)
const error = ref('')
const successMessage = ref('')

const users = ref([])
const usersLoading = ref(false)
const saving = ref(false)
const togglingId = ref('')
const openActionsUserId = ref('')
const usersError = ref('')
const formError = ref('')
const showForm = ref(false)
const editingId = ref('')
const showUserPassword = ref(false)
const accessClock = ref(Date.now())
let accessClockInterval = null
const auditRetentionMonths = ref(12)

const search = ref('')
const roleFilter = ref('')
const enabledFilter = ref('')
const areaFilter = ref('')
const currentPage = ref(1)
const USERS_PER_PAGE = 6
const roleOptions = ['ADMIN', 'APROBADOR', 'OPERADOR', 'VISUALIZADOR']

const selectedTheme = ref(getStoredTheme(auth.user))

const createMailGroup = () => ({
  input: '',
  items: []
})

const mailGroups = reactive({
  approver: createMailGroup(),
  approverCc: createMailGroup(),
  humanTalent: createMailGroup(),
  payroll: createMailGroup()
})

const coordinatorSettings = reactive(
  allAreas.reduce((settings, area) => {
    settings[area] = {
      input: '',
      items: []
    }

    return settings
  }, {})
)

const buildInitialForm = () => ({
  username: '',
  email: '',
  password: '',
  changePassword: false,
  roles: ['OPERADOR'],
  roleAssignments: [
    { role: 'OPERADOR', startDate: '', endDate: '', enabled: true }
  ],
  allowedAreas: [],
  areaAssignments: [],
  globalAreaAccess: false,
  enabled: true,
  useAccountValidity: false,
  accountStartDate: '',
  accountExpirationDate: ''
})

const form = reactive(buildInitialForm())

const roleLabel = computed(() => {
  if (auth.isSuperAdmin) return 'ADMIN'
  if (auth.isAdmin) return 'ADMIN'
  if (auth.isApprover) return 'APROBADOR'
  if (auth.isViewer) return 'VISUALIZADOR'
  return 'OPERADOR'
})

const areaScope = computed(() => areaScopeSummary(auth.allowedAreas))

const visibleAreas = computed(() => {
  if (auth.isAdmin || auth.hasGlobalAreaAccess) return 'Acceso global'
  return areaScope.value.summary
})

const areaScopeMeta = computed(() => {
  if (auth.isAdmin || auth.hasGlobalAreaAccess) return 'Todas las zonas disponibles'
  if (!areaScope.value.count) return 'No hay zonas asignadas a este usuario'
  return `${areaScope.value.count} ${areaScope.value.count === 1 ? 'zona asignada' : 'zonas asignadas'}`
})

const normalize = (value) => String(value || '').toLowerCase().trim()

const toDateTimeLocal = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

const formatValidityDate = (value, fallback) => value
  ? new Date(value).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })
  : fallback

const isAccessExpired = (user) => {
  if (!user?.accountExpirationDate) return false
  return accessClock.value >= new Date(user.accountExpirationDate).getTime()
}

const isAccessPending = (user) => {
  if (!user?.accountStartDate) return false
  return accessClock.value < new Date(user.accountStartDate).getTime()
}

const userAccessStatus = (user) => {
  if (!user?.enabled) {
    return {
      className: 'status-pill-inactive',
      label: 'INACTIVO',
      detail: `Inhabilitado por ${user?.enabledChangedBy || 'administracion'}`,
      isActive: false
    }
  }

  if (isAccessExpired(user)) {
    return {
      className: 'status-pill-inactive',
      label: 'INACTIVO',
      detail: 'Vigencia finalizada. El usuario ya no puede iniciar sesion',
      isActive: false
    }
  }

  if (isAccessPending(user)) {
    return {
      className: 'status-pill-warning',
      label: 'PENDIENTE DE INICIO',
      detail: 'El acceso se activara al iniciar la vigencia',
      isActive: false
    }
  }

  return {
    className: 'status-pill-active',
    label: 'ACTIVO',
    detail: 'Acceso habilitado',
    isActive: true
  }
}

const displayRole = (role) => {
  const value = String(role || '').trim().toUpperCase()

  if (value === 'SUPER_ADMIN') return 'ADMIN'
  return value || '-'
}

const roleCode = (role) => {
  const value = String(role || '').trim().toUpperCase()
  return value === 'SUPER_ADMIN' ? 'ADMIN' : value
}

const formatRoles = (roles) => {
  if (!Array.isArray(roles) || !roles.length) return '-'

  return roles.map(displayRole).join(', ')
}

const normalizeRoleAssignmentsForForm = (user = {}) => {
  const assignments = Array.isArray(user.roleAssignments) && user.roleAssignments.length
    ? user.roleAssignments
    : Array.isArray(user.roles)
      ? user.roles.map((role) => ({ role: roleCode(role), startDate: '', endDate: '', enabled: true }))
      : []

  return assignments
    .map((assignment) => ({
      role: roleCode(assignment.role),
      startDate: toDateTimeLocal(assignment.startDate),
      endDate: toDateTimeLocal(assignment.endDate),
      enabled: assignment.enabled !== false
    }))
    .filter((assignment) => assignment.role)
}

const normalizeAreaAssignmentsForForm = (user = {}) => {
  const assignments = Array.isArray(user.areaAssignments) && user.areaAssignments.length
    ? user.areaAssignments
    : Array.isArray(user.allowedAreas)
      ? normalizeAreaList(user.allowedAreas).map((areaCode) => ({ areaCode, startDate: '', endDate: '', enabled: true }))
      : []

  return assignments
    .map((assignment) => ({
      areaCode: normalizeAreaCode(assignment.areaCode),
      startDate: toDateTimeLocal(assignment.startDate),
      endDate: toDateTimeLocal(assignment.endDate),
      enabled: assignment.enabled !== false
    }))
    .filter((assignment) => assignment.areaCode)
}

const assignmentDatePayload = (value) => value || null

const splitEmails = (value) => {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .filter((item, index, list) => list.indexOf(item) === index)
}

const joinEmails = (items) => {
  return Array.isArray(items)
    ? items.map((item) => item.trim()).filter(Boolean).join(',')
    : ''
}

const isValidEmail = (value) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || '').trim())
}

const userInitials = (user) => String(user?.username || 'U')
  .split(/[\s._-]+/)
  .filter(Boolean)
  .slice(0, 2)
  .map((part) => part.charAt(0).toUpperCase())
  .join('') || 'U'

const formatUserAreas = (user) => {
  if (user?.roles?.includes('ADMIN') || user?.roles?.includes('SUPER_ADMIN')) return 'TODAS'
  if (user?.roles?.includes('APROBADOR')) return 'TODAS PARA REVISION'
  if (user?.globalAreaAccess) return 'ACCESO GLOBAL'
  return areaScopeSummary(user?.allowedAreas || []).summary
}

const userAreaDetail = (user) => {
  if (user?.roles?.includes('ADMIN') || user?.roles?.includes('SUPER_ADMIN') || user?.roles?.includes('APROBADOR') || user?.globalAreaAccess) {
    return ''
  }

  return areaScopeSummary(user?.allowedAreas || []).detail
}

const resolveFrontendBaseUrlForSave = () => {
  const origin = window.location.origin
  const normalized = origin.toLowerCase()

  if (
    normalized.includes('localhost') ||
    normalized.includes('127.0.0.1') ||
    normalized.includes('0.0.0.0')
  ) {
    return PRODUCTION_FRONTEND_URL
  }

  return origin
}

const addMail = (groupKey) => {
  const group = mailGroups[groupKey]
  if (!group) return

  const email = group.input.trim().toLowerCase()

  if (!email) return

  if (!isValidEmail(email)) {
    error.value = 'Ingresa un correo valido.'
    return
  }

  error.value = ''

  if (!group.items.includes(email)) {
    group.items.push(email)
  }

  group.input = ''
}

const removeMail = (groupKey, email) => {
  const group = mailGroups[groupKey]
  if (!group) return

  group.items = group.items.filter((item) => item !== email)
}

const addCoordinator = (area) => {
  const group = coordinatorSettings[area]
  if (!group) return

  const email = group.input.trim().toLowerCase()

  if (!email) return

  if (!isValidEmail(email)) {
    error.value = 'Ingresa un correo valido.'
    return
  }

  error.value = ''

  if (!group.items.includes(email)) {
    group.items.push(email)
  }

  group.input = ''
}

const removeCoordinator = (area, email) => {
  const group = coordinatorSettings[area]
  if (!group) return

  group.items = group.items.filter((item) => item !== email)
}

const filteredUsers = computed(() => {
  const term = normalize(search.value)

  return users.value.filter((user) => {
    const userRoles = Array.isArray(user.roles) ? user.roles : []
    const userAreas = normalizeAreaList(Array.isArray(user.allowedAreas) ? user.allowedAreas : [])

    const matchesRole =
      !roleFilter.value ||
      userRoles.includes(roleFilter.value) ||
      (roleFilter.value === 'ADMIN' && userRoles.includes('SUPER_ADMIN'))

    const matchesEnabled =
      !enabledFilter.value || String(userAccessStatus(user).isActive) === enabledFilter.value

    const matchesArea =
      !areaFilter.value ||
      userRoles.includes('ADMIN') ||
      userRoles.includes('SUPER_ADMIN') ||
      userRoles.includes('APROBADOR') ||
      userAreas.includes(normalizeAreaCode(areaFilter.value))

    if (!matchesRole || !matchesEnabled || !matchesArea) return false
    if (!term) return true

    const haystack = [
      user.username,
      user.email,
      userRoles.map(displayRole).join(' '),
      userAreas.map(areaLabel).join(' ')
    ]
      .map(normalize)
      .join(' ')

    return haystack.includes(term)
  })
})

const superAdminCount = computed(() =>
  users.value.filter((user) => Array.isArray(user.roles) && user.roles.includes('SUPER_ADMIN')).length
)

const approverCount = computed(() =>
  users.value.filter((user) => Array.isArray(user.roles) && user.roles.includes('APROBADOR')).length
)

const operatorCount = computed(() =>
  users.value.filter((user) => Array.isArray(user.roles) && user.roles.includes('OPERADOR')).length
)

const viewerCount = computed(() =>
  users.value.filter((user) => Array.isArray(user.roles) && user.roles.includes('VISUALIZADOR')).length
)

const areaOptions = computed(() => {
  const usedAreas = new Set(allAreas)

  users.value.forEach((user) => {
    normalizeAreaList(user.allowedAreas || []).forEach((area) => usedAreas.add(area))
  })

  return [...usedAreas].sort((a, b) => areaLabel(a).localeCompare(areaLabel(b), 'es'))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredUsers.value.length / USERS_PER_PAGE)))

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * USERS_PER_PAGE
  return filteredUsers.value.slice(start, start + USERS_PER_PAGE)
})

const paginationSummary = computed(() => {
  if (!filteredUsers.value.length) return ''

  const start = (currentPage.value - 1) * USERS_PER_PAGE + 1
  const end = Math.min(currentPage.value * USERS_PER_PAGE, filteredUsers.value.length)

  return `Mostrando ${start}-${end} de ${filteredUsers.value.length} usuarios`
})

watch([search, roleFilter, enabledFilter, areaFilter], () => {
  currentPage.value = 1
})

watch(totalPages, (pages) => {
  if (currentPage.value > pages) {
    currentPage.value = pages
  }
})

const setTheme = (theme) => {
  selectedTheme.value = saveTheme(theme, auth.user)
}

const loadLocalSettings = async () => {
  if (!auth.canEditEmailSettings && !auth.canManageUsers) return

  try {
    loading.value = true
    error.value = ''

    const { data } = await getSystemSettings()

    if (auth.canManageUsers) {
      auditRetentionMonths.value = Number(data.userAuditRetentionMonths || 12)
    }

    if (auth.canEditEmailSettings) {
      mailGroups.approver.items = splitEmails(data.approverEmails)
      mailGroups.approverCc.items = splitEmails(data.approverCc)
      mailGroups.humanTalent.items = splitEmails(data.humanTalentEmails)
      mailGroups.payroll.items = splitEmails(data.payrollEmails)

      const zoneEmails = data.zoneCoordinatorEmails || {}

      allAreas.forEach((area) => {
        coordinatorSettings[area].items = splitEmails(zoneEmails[area] || zoneEmails[areaLabel(area)])
      })
    }
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo cargar la configuracion del sistema.'
  } finally {
    loading.value = false
  }
}

const saveLocalSettings = async () => {
  if (!auth.canEditEmailSettings) return

  try {
    loading.value = true
    error.value = ''
    successMessage.value = ''

    const zoneCoordinatorEmails = {}

    allAreas.forEach((area) => {
      zoneCoordinatorEmails[area] = joinEmails(coordinatorSettings[area].items)
    })

    await updateSystemSettings({
      autoSendApproverEmail: true,
      frontendBaseUrl: resolveFrontendBaseUrlForSave(),
      ...(auth.canManageUsers ? { userAuditRetentionMonths: auditRetentionMonths.value } : {}),
      approverEmails: joinEmails(mailGroups.approver.items),
      approverCc: joinEmails(mailGroups.approverCc.items),
      humanTalentEmails: joinEmails(mailGroups.humanTalent.items),
      payrollEmails: joinEmails(mailGroups.payroll.items),
      zoneCoordinatorEmails
    })

    successMessage.value = 'Configuracion de correos guardada correctamente.'
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo guardar la configuracion de correos.'
  } finally {
    loading.value = false
  }
}

const saveAuditRetention = async () => {
  if (!auth.canManageUsers) return

  try {
    loading.value = true
    error.value = ''
    successMessage.value = ''

    await updateSystemSettings({
      userAuditRetentionMonths: auditRetentionMonths.value
    })

    successMessage.value = 'Retencion de auditoria guardada correctamente.'
  } catch (err) {
    error.value = err?.response?.data?.message || 'No se pudo guardar la retencion de auditoria.'
  } finally {
    loading.value = false
  }
}

const loadUsers = async () => {
  if (!auth.canManageUsers) return

  try {
    usersLoading.value = true
    usersError.value = ''

    const { data } = await http.get('/api/users')
    users.value = Array.isArray(data) ? data : []
  } catch (e) {
    usersError.value = e?.response?.data?.message || 'No se pudieron cargar los usuarios.'
  } finally {
    usersLoading.value = false
  }
}

const refreshAll = async () => {
  successMessage.value = ''

  await Promise.all([
    loadLocalSettings(),
    loadUsers()
  ])
}

const resetForm = () => {
  Object.assign(form, buildInitialForm())
}

const startCreate = () => {
  editingId.value = ''
  showUserPassword.value = false
  formError.value = ''
  resetForm()
  showForm.value = true
}

const startEdit = (user) => {
  editingId.value = user.id
  showUserPassword.value = false
  formError.value = ''
  const roleAssignments = normalizeRoleAssignmentsForForm(user)
  const areaAssignments = normalizeAreaAssignmentsForForm(user)

  Object.assign(form, {
    username: user.username || '',
    email: user.email || '',
    password: '',
    changePassword: false,
    roles: roleAssignments.map((assignment) => assignment.role),
    roleAssignments,
    allowedAreas: areaAssignments.map((assignment) => assignment.areaCode),
    areaAssignments,
    globalAreaAccess: Boolean(user.globalAreaAccess || areaScopeSummary(user.allowedAreas || []).hasAllAreas),
    enabled: Boolean(user.enabled),
    useAccountValidity: Boolean(user.accountStartDate || user.accountExpirationDate),
    accountStartDate: toDateTimeLocal(user.accountStartDate),
    accountExpirationDate: toDateTimeLocal(user.accountExpirationDate)
  })

  showForm.value = true
}

const closeForm = () => {
  editingId.value = ''
  showUserPassword.value = false
  formError.value = ''
  resetForm()
  showForm.value = false
}

const findRoleAssignment = (role) => {
  const normalizedRole = roleCode(role)
  let assignment = form.roleAssignments.find((item) => roleCode(item.role) === normalizedRole)

  if (!assignment) {
    assignment = { role: normalizedRole, startDate: '', endDate: '', enabled: true }
    form.roleAssignments.push(assignment)
  }

  return assignment
}

const hasRoleAssignment = (role) =>
  form.roleAssignments.some((assignment) => roleCode(assignment.role) === roleCode(role))

const syncRoleCodes = () => {
  form.roles = [...new Set(form.roleAssignments.map((assignment) => roleCode(assignment.role)).filter(Boolean))]
}

const toggleRoleAssignment = (role) => {
  const normalizedRole = roleCode(role)

  if (hasRoleAssignment(normalizedRole)) {
    form.roleAssignments = form.roleAssignments.filter((assignment) => roleCode(assignment.role) !== normalizedRole)
  } else {
    form.roleAssignments = [
      ...form.roleAssignments,
      { role: normalizedRole, startDate: '', endDate: '', enabled: true }
    ]
  }

  syncRoleCodes()

  if (form.roles.includes('ADMIN') || form.roles.includes('SUPER_ADMIN') || form.roles.includes('APROBADOR')) {
    form.globalAreaAccess = true
    form.allowedAreas = []
    form.areaAssignments = []
  }
}

const selectRole = (role) => {
  form.roles = [role]
  form.roleAssignments = [{ role: roleCode(role), startDate: '', endDate: '', enabled: true }]

  if (role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'APROBADOR') {
    form.allowedAreas = []
    form.globalAreaAccess = true
  } else {
    form.globalAreaAccess = false
  }
}

const findAreaAssignment = (area) => {
  const normalizedArea = normalizeAreaCode(area)
  let assignment = form.areaAssignments.find((item) => normalizeAreaCode(item.areaCode) === normalizedArea)

  if (!assignment) {
    assignment = { areaCode: normalizedArea, startDate: '', endDate: '', enabled: true }
    form.areaAssignments.push(assignment)
  }

  return assignment
}

const hasAreaAssignment = (area) =>
  form.areaAssignments.some((assignment) => normalizeAreaCode(assignment.areaCode) === normalizeAreaCode(area))

const syncAreaCodes = () => {
  form.allowedAreas = normalizeAreaList(form.areaAssignments.map((assignment) => assignment.areaCode))
}

const toggleAreaAssignment = (area) => {
  const normalizedArea = normalizeAreaCode(area)

  if (hasAreaAssignment(normalizedArea)) {
    form.areaAssignments = form.areaAssignments.filter((assignment) => normalizeAreaCode(assignment.areaCode) !== normalizedArea)
  } else {
    form.areaAssignments = [
      ...form.areaAssignments,
      { areaCode: normalizedArea, startDate: '', endDate: '', enabled: true }
    ]
  }

  syncAreaCodes()
}

const handleChangePasswordToggle = () => {
  showUserPassword.value = false
  if (!form.changePassword) {
    form.password = ''
  }
}

const handleAccountValidityToggle = () => {
  if (!form.useAccountValidity) {
    form.accountStartDate = ''
    form.accountExpirationDate = ''
  }
}

const toggleArea = (area) => {
  if (form.globalAreaAccess || form.roles.includes('ADMIN') || form.roles.includes('SUPER_ADMIN') || form.roles.includes('APROBADOR')) return

  const exists = form.allowedAreas.includes(area)

  form.allowedAreas = exists
    ? form.allowedAreas.filter((item) => item !== area)
    : [...form.allowedAreas, area]
}

const toggleGlobalAreaAccess = () => {
  if (form.globalAreaAccess) {
    form.allowedAreas = []
    form.areaAssignments = []
  }
}

const validateForm = () => {
  syncRoleCodes()
  syncAreaCodes()

  if (!form.username.trim()) return 'El usuario es obligatorio.'
  if (!form.email.trim()) return 'El correo es obligatorio.'
  if (!editingId.value && !form.password) return 'La contraseña es obligatoria.'
  if (editingId.value && form.changePassword && !form.password) return 'Ingresa la nueva contraseña.'
  if (!form.roleAssignments.length) return 'Debes seleccionar al menos un rol.'

  if (
    (form.roles.includes('OPERADOR') || form.roles.includes('VISUALIZADOR')) &&
    !form.globalAreaAccess &&
    form.areaAssignments.length === 0
  ) {
    return 'OPERADOR y VISUALIZADOR deben tener acceso global o al menos una zona asignada.'
  }

  const invalidRoleRange = form.roleAssignments.some((assignment) =>
    assignment.startDate && assignment.endDate && assignment.endDate <= assignment.startDate
  )

  if (invalidRoleRange) return 'En roles, la fecha hasta debe ser posterior a la fecha desde.'

  const invalidAreaRange = form.areaAssignments.some((assignment) =>
    assignment.startDate && assignment.endDate && assignment.endDate <= assignment.startDate
  )

  if (invalidAreaRange) return 'En zonas, la fecha hasta debe ser posterior a la fecha desde.'

  return ''
}

const saveUser = async () => {
  const validationError = validateForm()

  if (validationError) {
    formError.value = validationError
    return
  }

  try {
    saving.value = true
    formError.value = ''
    usersError.value = ''

    const payload = {
      username: form.username.trim(),
      email: form.email.trim(),
      password: !editingId.value || form.changePassword ? form.password : undefined,
      roles: form.roles,
      roleAssignments: form.roleAssignments.map((assignment) => ({
        role: roleCode(assignment.role),
        startDate: assignmentDatePayload(assignment.startDate),
        endDate: assignmentDatePayload(assignment.endDate),
        enabled: assignment.enabled !== false
      })),
      allowedAreas:
        form.roles.includes('ADMIN') || form.roles.includes('SUPER_ADMIN') || form.roles.includes('APROBADOR') || form.roles.includes('IT')
          ? []
          : normalizeAreaList(form.allowedAreas),
      areaAssignments:
        form.globalAreaAccess ||
        form.roles.includes('ADMIN') ||
        form.roles.includes('SUPER_ADMIN') ||
        form.roles.includes('APROBADOR') ||
        form.roles.includes('IT')
          ? []
          : form.areaAssignments.map((assignment) => ({
            areaCode: normalizeAreaCode(assignment.areaCode),
            startDate: assignmentDatePayload(assignment.startDate),
            endDate: assignmentDatePayload(assignment.endDate),
            enabled: assignment.enabled !== false
      })),
      globalAreaAccess: form.globalAreaAccess,
      enabled: form.enabled,
      accountStartDate: form.useAccountValidity ? form.accountStartDate || null : null,
      accountExpirationDate: form.useAccountValidity ? form.accountExpirationDate || null : null
    }

    if (!editingId.value) {
      await http.post('/api/users', payload)
    } else {
      await http.put(`/api/users/${editingId.value}`, payload)
    }

    await loadUsers()
    closeForm()
    successMessage.value = 'Usuario guardado correctamente.'
  } catch (e) {
    formError.value = e?.response?.data?.message || 'No se pudo guardar el usuario.'
  } finally {
    saving.value = false
  }
}

const toggleUserEnabled = async (user) => {
  const nextEnabled = !user.enabled
  const confirmed = window.confirm(`Seguro que deseas ${nextEnabled ? 'activar' : 'inactivar'} este usuario?`)
  if (!confirmed) return

  try {
    togglingId.value = user.id
    usersError.value = ''

    await http.patch(`/api/users/${user.id}/enabled`, { enabled: nextEnabled })
    await loadUsers()
    successMessage.value = `Usuario ${nextEnabled ? 'activado' : 'inactivado'} correctamente.`
  } catch (e) {
    usersError.value = e?.response?.data?.message || 'No se pudo actualizar el estado del usuario.'
  } finally {
    togglingId.value = ''
  }
}

const deleteUser = async (user) => {
  if (!user?.id) return

  const confirmed = window.confirm(`Seguro que deseas eliminar el usuario ${user.username}? Esta accion no se puede deshacer.`)
  if (!confirmed) return

  try {
    togglingId.value = user.id
    usersError.value = ''

    await http.delete(`/api/users/${user.id}`)
    await loadUsers()
    successMessage.value = 'Usuario eliminado correctamente.'
  } catch (e) {
    usersError.value = e?.response?.data?.message || 'No se pudo eliminar el usuario.'
  } finally {
    togglingId.value = ''
  }
}

const toggleActionsMenu = (userId) => {
  openActionsUserId.value = openActionsUserId.value === userId ? '' : userId
}

const runUserAction = (action) => {
  openActionsUserId.value = ''
  return action()
}

onMounted(async () => {
  accessClockInterval = window.setInterval(() => {
    accessClock.value = Date.now()
  }, 15000)

  selectedTheme.value = getStoredTheme(auth.user)
  applyTheme(selectedTheme.value)

  if (auth.canAccessSettings) {
    await refreshAll()
  }
})

onUnmounted(() => {
  if (accessClockInterval) {
    window.clearInterval(accessClockInterval)
  }
})
</script>

<style scoped>
.settings-page {
  gap: 0.9rem;
}

.settings-hero {
  margin-bottom: 0;
  padding: 1rem 1.15rem;
}

.settings-hero h1 {
  font-size: 1.35rem;
}

.settings-shell {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 1rem;
  align-items: flex-start;
}

.settings-sidebar {
  position: sticky;
  top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.6rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.settings-nav-item {
  width: 100%;
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: 0.7rem;
  align-items: center;
  padding: 0.65rem;
  border: 1px solid transparent;
  border-radius: 14px;
  background: transparent;
  color: var(--text-muted);
  text-align: left;
  transition: 0.18s ease;
}

.settings-nav-item:hover {
  background: var(--surface-soft);
  color: var(--text);
}

.settings-nav-item.active {
  background: var(--primary-soft);
  border-color: rgba(63, 111, 143, 0.22);
  color: var(--text);
}

.settings-nav-item strong,
.settings-nav-item small {
  display: block;
}

.settings-nav-item strong {
  font-size: 0.84rem;
  font-weight: 760;
}

.settings-nav-item small {
  margin-top: 0.08rem;
  color: var(--text-muted);
  font-size: 0.71rem;
}

.settings-nav-item__icon {
  width: 31px;
  height: 31px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  color: var(--text-soft);
  font-size: 0.84rem;
}

.settings-content,
.settings-section {
  min-width: 0;
}

.settings-section {
  display: grid;
  gap: 0.85rem;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.compact-heading h2,
.user-modal__header h2 {
  margin: 0;
  color: var(--text);
  font-size: 1.08rem;
  font-weight: 820;
  letter-spacing: -0.03em;
}

.compact-heading p,
.user-modal__header p {
  margin: 0.2rem 0 0;
  color: var(--text-muted);
  font-size: 0.84rem;
}

.compact-card-body {
  padding: 0.95rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 0.7rem;
}

.summary-card,
.kpi-card {
  min-height: 70px;
  padding: 0.75rem 0.85rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.summary-card span:last-child {
  display: block;
  margin-top: 0.3rem;
  color: var(--text);
  font-size: 0.9rem;
  font-weight: 800;
  word-break: break-word;
}

.audit-retention-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 0.9rem;
  align-items: end;
}

.audit-retention-row .primary-btn {
  min-width: 170px;
}

.account-validity-card {
  display: grid;
  gap: 0.65rem;
}

.account-validity-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 0.75rem;
}

.area-scope-title {
  color: var(--text);
  font-size: 1rem;
  font-weight: 820;
  line-height: 1.25;
}

.area-scope-meta {
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 650;
}

.business-flow-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(155px, 1fr));
  gap: 0.7rem;
}

.business-flow-card {
  position: relative;
  padding: 0.85rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.business-flow-card strong {
  display: block;
  margin-top: 1.2rem;
  color: var(--text);
  font-size: 0.9rem;
  font-weight: 780;
}

.business-flow-card p {
  margin: 0.4rem 0 0;
  color: var(--text-muted);
  font-size: 0.8rem;
  line-height: 1.35;
}

.status-dot {
  position: absolute;
  top: 0.8rem;
  left: 0.8rem;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--text-muted);
}

.status-dot.success {
  background: var(--success);
}

.status-dot.warning {
  background: var(--warning);
}

.status-dot.danger {
  background: var(--danger);
}

.status-dot.neutral {
  background: var(--text-muted);
}

.mail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(230px, 1fr));
  gap: 0.85rem;
}

.mail-card,
.zone-card {
  padding: 0.9rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.mail-card__header h3 {
  margin: 0.15rem 0 0;
  color: var(--text);
  font-size: 1rem;
  font-weight: 820;
}

.mail-card__header p {
  margin: 0.25rem 0 0.7rem;
  color: var(--text-muted);
  font-size: 0.8rem;
}

.mail-add-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.5rem;
  align-items: center;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-top: 0.7rem;
}

.mail-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  max-width: 100%;
  padding: 0.38rem 0.55rem;
  border: 1px solid rgba(13, 148, 136, 0.24);
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--text);
  font-size: 0.76rem;
  font-weight: 740;
  word-break: break-word;
}

.mail-chip button {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  color: var(--text-muted);
  font-weight: 900;
  line-height: 1;
}

.empty-mail {
  margin-top: 0.65rem;
  color: var(--text-muted);
  font-size: 0.78rem;
}

.zone-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(190px, 1fr));
  gap: 0.75rem;
}

.rule-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(130px, 1fr));
  gap: 0.7rem;
}

.kpi-value {
  display: block;
  margin-top: 0.28rem;
  color: var(--text);
  font-size: 1.25rem;
  line-height: 1;
  font-weight: 850;
}

.kpi-meta {
  display: block;
  margin-top: 0.28rem;
  color: var(--text-muted);
  font-size: 0.75rem;
}

.filters-grid {
  display: grid;
  grid-template-columns: 1.3fr 0.8fr 0.8fr 0.8fr;
  gap: 0.7rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 0.85rem;
}

.form-field {
  min-width: 0;
}

.full-span {
  grid-column: 1 / -1;
}

.field-card {
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.role-card {
  display: grid;
  gap: 0.45rem;
}

.assignment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 0.65rem;
}

.assignment-card {
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface-soft);
}

.assignment-card.selected {
  border-color: rgba(13, 148, 136, 0.38);
  background: var(--primary-soft);
}

.assignment-dates {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  margin-top: 0.65rem;
}

.assignment-dates.compact {
  grid-template-columns: 1fr;
  margin-top: 0.45rem;
}

.assignment-dates small {
  display: block;
  margin-bottom: 0.25rem;
  color: var(--text-muted);
  font-size: 0.68rem;
  font-weight: 800;
  text-transform: uppercase;
}

.assignment-dates .form-control {
  min-height: 36px;
  padding: 0.4rem 0.55rem;
  font-size: 0.78rem;
}

.compact-check {
  grid-column: 1 / -1;
  padding: 0.4rem 0.5rem;
  background: var(--surface);
}

.area-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.5rem;
}

.area-assignment-grid {
  grid-template-columns: repeat(auto-fit, minmax(235px, 1fr));
}

.global-access-option {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  margin-bottom: 0.7rem;
  padding: 0.75rem;
  border: 1px solid rgba(13, 148, 136, 0.3);
  border-radius: 14px;
  background: var(--primary-soft);
  color: var(--text);
}

.global-access-option span {
  display: grid;
  gap: 0.18rem;
}

.global-access-option small {
  color: var(--text-muted);
  font-size: 0.76rem;
}

.area-option,
.checkbox-field {
  display: flex;
  align-items: center;
  gap: 0.52rem;
  padding: 0.58rem 0.65rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-soft);
  color: var(--text);
  font-size: 0.82rem;
  font-weight: 700;
}

.area-option {
  display: block;
}

.area-option .checkbox-field {
  padding: 0;
  border: 0;
  background: transparent;
}

.users-card-list {
  display: grid;
  gap: 0.48rem;
}

.user-account-card {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
  gap: 0.42rem 0.68rem;
  align-items: center;
  padding: 0.58rem 0.7rem;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface);
}

.user-account-card__identity {
  display: flex;
  grid-column: 1 / -2;
  min-width: 0;
  align-items: center;
  gap: 0.58rem;
}

.user-account-card__avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 10px;
  color: var(--primary);
  background: var(--surface-soft);
  font-weight: 850;
}

.user-account-card__identity div:last-child,
.user-account-card__detail {
  display: grid;
  min-width: 0;
  gap: 0.2rem;
}

.user-account-card__identity div:last-child {
  overflow: hidden;
}

.user-account-card__identity small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-account-card small,
.user-account-card__detail span {
  color: var(--text-muted);
  font-size: 0.72rem;
}

.user-account-card strong {
  overflow-wrap: anywhere;
  font-size: 0.82rem;
}

.validity-period {
  display: grid;
  gap: 0.32rem;
}

.validity-period > span {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 0.35rem;
  align-items: baseline;
}

.validity-period small {
  color: var(--text-muted);
  font-size: 0.68rem;
  font-weight: 780;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.validity-period strong {
  color: var(--text);
  font-size: 0.78rem;
  font-weight: 760;
  line-height: 1.35;
}

.user-account-card__actions {
  display: flex;
  grid-column: -2 / -1;
  grid-row: 1;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.user-actions-menu {
  position: relative;
}

.user-actions-menu__trigger {
  cursor: pointer;
  min-height: 30px;
  padding: 0.28rem 0.52rem;
  font-size: 0.72rem;
  user-select: none;
}

.user-actions-menu__trigger::after {
  content: ' v';
  color: var(--text-muted);
}

.user-actions-menu__trigger[aria-expanded='true'] {
  border-color: rgba(63, 111, 143, 0.4);
  background: var(--surface-soft);
}

.user-actions-menu__panel {
  position: absolute;
  top: calc(100% + 0.28rem);
  right: 0;
  z-index: 5;
  display: grid;
  width: 168px;
  padding: 0.25rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: var(--shadow-lg);
}

.user-actions-menu__panel button {
  padding: 0.46rem 0.5rem;
  border: 0;
  border-radius: 9px;
  color: var(--text);
  background: transparent;
  font-size: 0.73rem;
  font-weight: 720;
  text-align: left;
}

.user-actions-menu__panel button:hover {
  background: var(--surface-soft);
}

.user-actions-menu__panel button.danger {
  color: var(--danger);
}

.users-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.7rem;
  margin-top: 0.7rem;
  padding-top: 0.65rem;
  border-top: 1px solid var(--border);
}

.users-pagination small,
.users-pagination span {
  color: var(--text-muted);
  font-size: 0.76rem;
  font-weight: 680;
}

.users-pagination div {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.users-pagination .secondary-btn {
  min-height: 32px;
  padding: 0.35rem 0.58rem;
  font-size: 0.75rem;
}

.area-detail-disclosure {
  margin-top: 0.2rem;
}

.area-detail-disclosure summary {
  color: var(--primary);
  cursor: pointer;
  font-size: 0.74rem;
  font-weight: 780;
}

.area-detail-disclosure small {
  display: block;
  margin-top: 0.35rem;
  line-height: 1.45;
}

.theme-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 0.85rem;
}

.theme-option {
  display: grid;
  gap: 0.5rem;
  padding: 0.72rem;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--surface-soft);
  color: var(--text);
  text-align: left;
  transition: 0.18s ease;
}

.theme-option:hover,
.theme-option.active {
  border-color: rgba(63, 111, 143, 0.45);
  box-shadow: 0 0 0 3px rgba(63, 111, 143, 0.12);
}

.theme-option strong {
  font-weight: 780;
}

.theme-option small {
  color: var(--text-muted);
}

.theme-option__preview {
  width: 100%;
  height: 70px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: linear-gradient(135deg, #ffffff 0%, #f4f7fb 100%);
}

.theme-option__preview--dark {
  background: linear-gradient(135deg, #020617 0%, #111827 100%);
}

.theme-option__preview--system {
  background: linear-gradient(135deg, #ffffff 0%, #f4f7fb 49%, #111827 50%, #020617 100%);
}

.danger-btn {
  color: var(--danger);
  border-color: rgba(194, 65, 65, 0.28);
  background: var(--danger-soft);
}

.danger-btn:hover {
  color: var(--danger);
  border-color: rgba(194, 65, 65, 0.42);
  background: var(--danger-soft);
}

.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.58);
  backdrop-filter: blur(3px);
}

.user-modal {
  width: min(920px, 100%);
  max-height: 92vh;
  overflow: auto;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 22px;
  background: var(--surface);
  box-shadow: var(--shadow-lg);
}

.user-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.9rem;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid var(--border);
}

.modal-actions {
  justify-content: flex-end;
}

:global([data-theme='dark'] .table),
:global([data-theme='dark'] .app-table) {
  --bs-table-bg: var(--surface);
  --bs-table-color: var(--text);
  --bs-table-border-color: var(--border);
  --bs-table-hover-bg: rgba(255, 255, 255, 0.04);
  --bs-table-hover-color: var(--text);
  color: var(--text);
  background: var(--surface);
}

:global([data-theme='dark'] .table thead th),
:global([data-theme='dark'] .app-table thead th) {
  color: var(--text-muted);
  background: var(--surface-soft);
  border-color: var(--border);
}

:global([data-theme='dark'] .table tbody td),
:global([data-theme='dark'] .app-table tbody td) {
  color: var(--text);
  border-color: var(--border);
}

:global([data-theme='dark'] .table-responsive) {
  border-color: var(--border);
}

@media (max-width: 1180px) {
  .settings-shell {
    grid-template-columns: 1fr;
  }

  .settings-sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  }

  .summary-grid,
  .business-flow-grid,
  .mail-grid,
  .zone-grid,
  .theme-options,
  .filters-grid,
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }

  .user-account-card {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .user-account-card__identity {
    grid-column: 1 / -1;
  }

  .user-account-card__actions {
    grid-column: 1 / -1;
    grid-row: auto;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .section-heading,
  .page-header,
  .user-modal__header {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid,
  .business-flow-grid,
  .mail-grid,
  .zone-grid,
  .theme-options,
  .filters-grid,
  .kpi-grid,
  .mail-add-row {
    grid-template-columns: 1fr;
  }

  .modal-actions {
    justify-content: stretch;
  }

  .modal-actions .secondary-btn,
  .modal-actions .primary-btn {
    width: 100%;
  }

  .user-account-card {
    grid-template-columns: 1fr;
  }

  .user-account-card__identity {
    grid-column: auto;
  }

  .user-account-card__actions {
    grid-column: auto;
    grid-row: auto;
    justify-content: stretch;
  }

  .user-account-card__actions .secondary-btn {
    flex: 1;
  }

  .users-pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .users-pagination div {
    justify-content: space-between;
  }

  .audit-retention-row {
    grid-template-columns: 1fr;
  }

  .audit-retention-row .primary-btn {
    width: 100%;
  }

  .account-validity-grid {
    grid-template-columns: 1fr;
  }
}
</style>
