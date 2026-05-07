<template>
  <header class="app-navbar">
    <div class="app-navbar__inner">
      <div class="app-navbar__brand">
        <RouterLink :to="brandTarget" class="brand-link">
          <span class="brand-title">EBSA Alturas</span>
          <span class="brand-subtitle">Seguimiento de aptitud para trabajo en alturas</span>
        </RouterLink>
      </div>

      <nav v-if="auth.isAuthenticated" class="app-navbar__nav">
        <RouterLink
          v-if="!isApproverOnly"
          to="/employees"
          class="nav-link"
          :class="{ active: isActive('/employees') }"
        >
          Trabajadores
        </RouterLink>

        <RouterLink
          v-if="!isApproverOnly"
          to="/documents/upload"
          class="nav-link"
          :class="{ active: isActive('/documents/upload') }"
        >
          Cargar evaluaciones
        </RouterLink>

        <RouterLink
          v-if="auth.canReviewDocuments"
          to="/review"
          class="nav-link"
          :class="{ active: isActive('/review') }"
        >
          Revisión SSA
        </RouterLink>

        <RouterLink
          v-if="auth.isSuperAdmin"
          to="/documents"
          class="nav-link"
          :class="{ active: isActive('/documents') && !isActive('/documents/upload') }"
        >
          Evaluaciones
        </RouterLink>

        <RouterLink
          v-if="!isApproverOnly"
          to="/settings"
          class="nav-link"
          :class="{ active: isActive('/settings') }"
        >
          ⚙️ Configuración
        </RouterLink>

        <RouterLink
          v-if="auth.isSuperAdmin"
          to="/users"
          class="nav-link"
          :class="{ active: isActive('/users') }"
        >
          👥 Usuarios
        </RouterLink>
      </nav>

      <div class="app-navbar__session">
        <template v-if="auth.isAuthenticated">
          <div class="session-box">
            <div class="session-line">
              <strong>{{ auth.user?.username || 'Usuario' }}</strong>

              <span
                class="role-pill"
                :class="roleClass"
              >
                {{ roleLabel }}
              </span>
            </div>

            <div class="session-meta">
              <template v-if="auth.isSuperAdmin">
                <span>Acceso global</span>
              </template>

              <template v-else-if="auth.isApprover">
                <span>Solo revisión y aprobación</span>
              </template>

              <template v-else>
                <span>
                  Áreas:
                  {{ visibleAreas }}
                </span>
              </template>
            </div>
          </div>

          <button type="button" class="logout-btn" @click="auth.logout">
            Cerrar sesión
          </button>
        </template>

        <template v-else>
          <RouterLink to="/login" class="login-btn">
            Iniciar sesión
          </RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()

const isActive = (path) => route.path === path || route.path.startsWith(`${path}/`)

const isApproverOnly = computed(() => auth.isApprover && !auth.isSuperAdmin)

const brandTarget = computed(() => {
  return isApproverOnly.value ? '/review' : '/employees'
})

const visibleAreas = computed(() => {
  const areas = Array.isArray(auth.allowedAreas) ? auth.allowedAreas : []
  return areas.length ? areas.join(', ') : 'Sin áreas asignadas'
})

const roleLabel = computed(() => {
  if (auth.isSuperAdmin) return 'SUPER_ADMIN'
  if (auth.isApprover) return 'APROBADOR'
  return 'OPERADOR'
})

const roleClass = computed(() => {
  if (auth.isSuperAdmin) return 'role-admin'
  if (auth.isApprover) return 'role-approver'
  return 'role-operator'
})
</script>

<style scoped>
.app-navbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}

.app-navbar__inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0.9rem 1.2rem;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1rem;
  align-items: center;
}

.brand-link {
  display: flex;
  flex-direction: column;
  text-decoration: none;
}

.brand-title {
  font-size: 1.05rem;
  font-weight: 800;
  color: #111827;
  line-height: 1.1;
}

.brand-subtitle {
  font-size: 0.82rem;
  color: #6b7280;
  line-height: 1.1;
  margin-top: 0.2rem;
}

.app-navbar__nav {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.nav-link {
  text-decoration: none;
  color: #334155;
  font-weight: 700;
  padding: 0.68rem 0.95rem;
  border-radius: 12px;
  transition: all 0.18s ease;
}

.nav-link:hover {
  background: #f8fafc;
  color: #0f172a;
}

.nav-link.active {
  background: rgba(63, 111, 143, 0.12);
  color: #24445d;
}

.app-navbar__session {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.session-box {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  text-align: right;
}

.session-line {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.session-meta {
  font-size: 0.82rem;
  color: #6b7280;
}

.role-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0.22rem 0.62rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 800;
}

.role-admin {
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
}

.role-approver {
  background: rgba(126, 34, 206, 0.12);
  color: #7e22ce;
}

.role-operator {
  background: rgba(22, 163, 74, 0.12);
  color: #15803d;
}

.logout-btn,
.login-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0.68rem 0.9rem;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  background: #ffffff;
  color: #111827;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
}

.logout-btn:hover,
.login-btn:hover {
  background: #f8fafc;
}

@media (max-width: 992px) {
  .app-navbar__inner {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .app-navbar__nav {
    justify-content: flex-start;
  }

  .app-navbar__session {
    justify-content: space-between;
  }

  .session-box {
    text-align: left;
  }

  .session-line {
    justify-content: flex-start;
  }
}
</style>