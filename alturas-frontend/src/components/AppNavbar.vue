<template>
  <aside class="app-sidebar" aria-label="Navegación principal">
    <div class="sidebar-brand">
      <RouterLink :to="brandTarget" class="sidebar-brand__link">
        <img
          src="/logo-light-theme.png"
          alt="SSTAlturas"
          class="sidebar-brand__logo light-theme-logo"
        />
        <img
          src="/logo-dark-theme.png"
          alt="SSTAlturas"
          class="sidebar-brand__logo dark-theme-logo"
        />

        <span class="sidebar-brand__text">
          <strong>SSTAlturas</strong>
          <small>Gestión de aptitud</small>
        </span>
      </RouterLink>
    </div>

    <nav v-if="auth.isAuthenticated" class="sidebar-nav">
      <div class="sidebar-nav__group">
        <span class="sidebar-nav__label">Operación</span>

        <RouterLink
          v-if="canShowEmployees"
          to="/employees"
          class="sidebar-link"
          :class="{ active: isActive('/employees') }"
        >
          <span class="sidebar-link__icon">👥</span>
          <span>Trabajadores</span>
        </RouterLink>

        <RouterLink
          v-if="auth.canUploadDocuments && !isApproverOnly"
          to="/documents/upload"
          class="sidebar-link"
          :class="{ active: isActive('/documents/upload') }"
        >
          <span class="sidebar-link__icon">⬆</span>
          <span>Cargar PDFs</span>
        </RouterLink>

        <RouterLink
          v-if="canShowReview"
          to="/review"
          class="sidebar-link"
          :class="{ active: isActive('/review') }"
        >
          <span class="sidebar-link__icon">✓</span>
          <span>Revisión</span>
        </RouterLink>

        <RouterLink
          v-if="canShowReports"
          to="/reports"
          class="sidebar-link"
          :class="{ active: isActive('/reports') }"
        >
          <span class="sidebar-link__icon">📄</span>
          <span>Reportes</span>
        </RouterLink>

        <RouterLink
          v-if="auth.canAccessSettings"
          to="/settings"
          class="sidebar-link"
          :class="{ active: isActive('/settings') || isActive('/users') }"
        >
          <span class="sidebar-link__icon">⚙</span>
          <span>Configuración</span>
        </RouterLink>
      </div>
    </nav>

    <div class="sidebar-bottom">
      <div v-if="auth.isAuthenticated" class="sidebar-user-card">
        <div class="sidebar-user-card__avatar">
          {{ userInitials }}
        </div>

        <div class="sidebar-user-card__body">
          <strong>{{ auth.user?.username || 'Usuario' }}</strong>
          <span>{{ roleLabel }}</span>
          <small :title="areaScope.detail">{{ sidebarScopeLabel }}</small>
        </div>

        <div class="sidebar-user-actions">
          <button
            type="button"
            class="sidebar-icon-action sidebar-icon-action--logout"
            aria-label="Cerrar sesión"
            title="Cerrar sesión"
            @click="auth.logout"
          >
            ⏻
          </button>
        </div>
      </div>

      <RouterLink v-else to="/login" class="sidebar-logout">
        Iniciar sesión
      </RouterLink>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { areaScopeSummary } from '../utils/areaCatalog'

const auth = useAuthStore()
const route = useRoute()

const isActive = (path) => route.path === path || route.path.startsWith(`${path}/`)

const isApproverOnly = computed(() => {
  return auth.isApprover && !auth.isAdmin && !auth.isSuperAdmin
})

const canShowEmployees = computed(() => {
  return auth.isAuthenticated
})

const canShowReview = computed(() => {
  return auth.canReviewDocuments
})

const canShowReports = computed(() => {
  return auth.isAuthenticated
})

const brandTarget = computed(() => {
  return '/employees'
})

const areaScope = computed(() => areaScopeSummary(auth.allowedAreas))

const sidebarScopeLabel = computed(() => {
  if (auth.isAdmin || auth.hasGlobalAreaAccess) return 'Acceso global'
  if (auth.isApprover) return 'Revisión y reportes'
  return areaScope.value.summary
})

const roleLabel = computed(() => {
  if (auth.isSuperAdmin) return 'Admin'
  if (auth.isAdmin) return 'ADMIN'
  if (auth.isApprover) return 'APROBADOR'
  if (auth.isViewer) return 'VISUALIZADOR'
  return 'OPERADOR'
})

const userInitials = computed(() => {
  const username = auth.user?.username || 'U'

  return username
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('') || 'U'
})
</script>

<style scoped>
.sidebar-brand__logo {
  height: 80px;
  width: auto;
  max-width: 100%;
  object-fit: contain;
  flex: 0 0 auto;
}

.sidebar-brand__logo.dark-theme-logo {
  height: 64px;
}
</style>
