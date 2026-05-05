import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import UsersView from '../views/UsersView.vue'
import SettingsView from '../views/SettingsView.vue'
import ReviewPanelView from '../views/ReviewPanelView.vue'
import EmployeeListView from '../views/EmployeeListView.vue'
import EmployeeFormView from '../views/EmployeeFormView.vue'
import DocumentsListView from '../views/DocumentsListView.vue'
import DocumentUploadView from '../views/DocumentUploadView.vue'
import DocumentDetailView from '../views/DocumentDetailView.vue'

const getAuthSnapshot = () => {
  const token = localStorage.getItem('auth_token')
  const rawUser = localStorage.getItem('auth_user')

  let user = null

  try {
    user = rawUser ? JSON.parse(rawUser) : null
  } catch {
    user = null
  }

  const roles = Array.isArray(user?.roles) ? user.roles : []

  return {
    token,
    user,
    roles,
    isSuperAdmin: roles.includes('SUPER_ADMIN'),
    isApprover: roles.includes('APROBADOR'),
    isOperator: roles.includes('OPERADOR'),
    canReviewDocuments: roles.includes('SUPER_ADMIN') || roles.includes('APROBADOR')
  }
}

const defaultRouteForRoles = (roles = []) => {
  if (roles.includes('APROBADOR') && !roles.includes('SUPER_ADMIN')) {
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
      meta: { guestOnly: true }
    },

    {
      path: '/review',
      name: 'review',
      component: ReviewPanelView,
      meta: { requiresAuth: true, requiresReviewer: true }
    },

    {
      path: '/documents',
      name: 'documents',
      component: DocumentsListView,
      meta: { requiresAuth: true, blocksApproverOnly: true }
    },

    {
      path: '/documents/upload',
      name: 'document-upload',
      component: DocumentUploadView,
      meta: { requiresAuth: true, blocksApproverOnly: true }
    },

    {
      path: '/documents/:id',
      name: 'document-detail',
      component: DocumentDetailView,
      meta: { requiresAuth: true }
    },

    {
      path: '/employees',
      name: 'employees',
      component: EmployeeListView,
      meta: { requiresAuth: true, blocksApproverOnly: true }
    },

    {
      path: '/employees/new',
      name: 'employee-create',
      component: EmployeeFormView,
      meta: { requiresAuth: true, blocksApproverOnly: true }
    },

    {
      path: '/employees/:id/edit',
      name: 'employee-edit',
      component: EmployeeFormView,
      meta: { requiresAuth: true, requiresSuperAdmin: true }
    },

    {
      path: '/settings',
      name: 'settings',
      component: SettingsView,
      meta: { requiresAuth: true, blocksApproverOnly: true }
    },

    {
      path: '/users',
      name: 'users',
      component: UsersView,
      meta: { requiresAuth: true, requiresSuperAdmin: true }
    }
  ]
})

router.beforeEach((to, from, next) => {
  const {
    token,
    roles,
    isSuperAdmin,
    isApprover,
    canReviewDocuments
  } = getAuthSnapshot()

  const isApproverOnly = isApprover && !isSuperAdmin

  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }

  if (to.meta.guestOnly && token) {
    return next(defaultRouteForRoles(roles))
  }

  if (to.meta.requiresSuperAdmin && !isSuperAdmin) {
    return next(defaultRouteForRoles(roles))
  }

  if (to.meta.requiresReviewer && !canReviewDocuments) {
    return next('/employees')
  }

  if (to.meta.blocksApproverOnly && isApproverOnly) {
    return next('/review')
  }

  return next()
})

export default router