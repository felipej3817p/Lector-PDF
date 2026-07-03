import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ForgotPasswordView from '../views/ForgotPasswordView.vue'
import ResetPasswordView from '../views/ResetPasswordView.vue'
import SettingsView from '../views/SettingsView.vue'
import UserAuditView from '../views/UserAuditView.vue'
import ReportsView from '../views/ReportsView.vue'
import ReviewPanelView from '../views/ReviewPanelView.vue'
import EmployeeListView from '../views/EmployeeListView.vue'
import EmployeeFormView from '../views/EmployeeFormView.vue'
import EmployeeHistoryView from '../views/EmployeeHistoryView.vue'
import DocumentsListView from '../views/DocumentsListView.vue'
import DocumentUploadView from '../views/DocumentUploadView.vue'
import DocumentDetailView from '../views/DocumentDetailView.vue'
import HistoricalImportIssuesView from '../views/HistoricalImportIssuesView.vue'

const getAuthSnapshot = () => {
  const token = localStorage.getItem('auth_token')
  const rawUser = localStorage.getItem('auth_user')

  let user = null

  try {
    user = rawUser ? JSON.parse(rawUser) : null
  } catch {
    user = null
  }

  const roles = (Array.isArray(user?.roles) ? user.roles : []).map((role) => {
    const normalized = String(role || '').trim().toUpperCase().replace(/^ROLE_/, '')
    return normalized === 'APPROVER' ? 'APROBADOR' : normalized
  })

  const isSuperAdmin = roles.includes('SUPER_ADMIN')
  const isAdmin = isSuperAdmin || roles.includes('ADMIN')
  const isApprover = roles.includes('APROBADOR')
  const isOperator = roles.includes('OPERADOR')
  const isViewer = roles.includes('VISUALIZADOR')

  return {
    token,
    user,
    roles,
    isSuperAdmin,
    isAdmin,
    isApprover,
    isOperator,
    isViewer,
    canReviewDocuments: isAdmin || isApprover,
    canManageSettings: Boolean(token),
    canUploadDocuments: isAdmin || isOperator,
    canWriteEmployees: isAdmin || isOperator,
    isReadOnlyViewer: isViewer && !isAdmin && !isApprover && !isOperator
  }
}

const defaultRouteForRoles = (roles = []) => {
  const isSuperAdmin = roles.includes('SUPER_ADMIN')
  const isAdmin = isSuperAdmin || roles.includes('ADMIN')
  const isApprover = roles.includes('APROBADOR')

  if (isApprover && !isAdmin) {
    return '/review'
  }

  return '/employees'
}

const router = createRouter({
  history: createWebHistory(),

  routes: [
    {
      path: '/',
      redirect: () => {
        const { roles } = getAuthSnapshot()
        return defaultRouteForRoles(roles)
      }
    },

    {
      path: '/home',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true }
    },

    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true, title: 'Ingresar' }
    },

    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: ForgotPasswordView,
      meta: { public: true, title: 'Recuperar contrasena' }
    },

    {
      path: '/reset-password',
      name: 'reset-password',
      component: ResetPasswordView,
      meta: { public: true, title: 'Restablecer contrasena' }
    },

    {
      path: '/review',
      name: 'review',
      component: ReviewPanelView,
      meta: {
        requiresAuth: true,
        requiresReviewer: true,
        title: 'Revision'
      }
    },

    {
      path: '/reports',
      name: 'reports',
      component: ReportsView,
      meta: {
        requiresAuth: true,
        title: 'Reportes'
      }
    },

    {
      path: '/documents',
      name: 'documents',
      component: DocumentsListView,
      meta: {
        requiresAuth: true,
        blocksApproverOnly: true,
        blocksViewerOnly: true,
        title: 'Documentos'
      }
    },

    {
      path: '/documents/upload',
      name: 'document-upload',
      component: DocumentUploadView,
      meta: {
        requiresAuth: true,
        requiresDocumentUpload: true,
        title: 'Cargar PDFs'
      }
    },

    {
      path: '/documents/historical/issues',
      name: 'historical-import-issues',
      component: HistoricalImportIssuesView,
      meta: {
        requiresAuth: true,
        requiresDocumentUpload: true,
        title: 'PDFs no asociados'
      }
    },

    {
      path: '/documents/:id',
      name: 'document-detail',
      component: DocumentDetailView,
      meta: {
        requiresAuth: true,
        blocksViewerOnly: true,
        title: 'Detalle documental'
      }
    },

    {
      path: '/employees',
      name: 'employees',
      component: EmployeeListView,
      meta: {
        requiresAuth: true,
        title: 'Trabajadores'
      }
    },

    {
      path: '/employees/new',
      name: 'employee-create',
      component: EmployeeFormView,
      meta: {
        requiresAuth: true,
        requiresEmployeeWrite: true,
        title: 'Nuevo trabajador'
      }
    },

    {
      path: '/employees/:id/edit',
      name: 'employee-edit',
      component: EmployeeFormView,
      meta: {
        requiresAuth: true,
        requiresEmployeeWrite: true,
        title: 'Editar trabajador'
      }
    },

    {
      path: '/employees/:id/history',
      name: 'employee-history',
      component: EmployeeHistoryView,
      meta: {
        requiresAuth: true,
        title: 'Historial'
      }
    },

    {
      path: '/settings',
      name: 'settings',
      component: SettingsView,
      meta: {
        requiresAuth: true,
        requiresSettingsAdmin: true,
        title: 'Configuracion'
      }
    },

    {
      path: '/settings/users/audit',
      name: 'user-audit',
      component: UserAuditView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
        fullWidth: true,
        title: 'Auditoria de usuarios'
      }
    },

    {
      path: '/users',
      redirect: '/settings'
    }
  ]
})

router.beforeEach((to, from, next) => {
  const {
    token,
    roles,
    isAdmin,
    isApprover,
    canReviewDocuments,
    canUploadDocuments,
    canWriteEmployees,
    isReadOnlyViewer
  } = getAuthSnapshot()

  const isApproverOnly = isApprover && !isAdmin

  if (to.meta.public) {
    return next()
  }

  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }

  if (to.meta.guestOnly && token) {
    return next(defaultRouteForRoles(roles))
  }

  if (to.meta.requiresSettingsAdmin && !token) {
    return next(defaultRouteForRoles(roles))
  }

  if (to.meta.requiresAdmin && !isAdmin) {
    return next('/employees')
  }

  if (to.meta.requiresReviewer && !canReviewDocuments) {
    return next('/employees')
  }

  if (to.meta.requiresDocumentUpload && !canUploadDocuments) {
    return next('/employees')
  }

  if (to.meta.requiresEmployeeWrite && !canWriteEmployees) {
    return next('/employees')
  }

  if (to.meta.blocksApproverOnly && isApproverOnly) {
    return next('/employees')
  }

  if (to.meta.blocksViewerOnly && isReadOnlyViewer) {
    return next('/employees')
  }

  return next()
})

router.afterEach((to) => {
  const title = to.meta?.title ? `${to.meta.title} | SSTAlturas` : 'SSTAlturas'
  document.title = title
})

export default router

