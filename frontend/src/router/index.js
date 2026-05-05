import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import UsersView from '../views/UsersView.vue'
import EmployeeListView from '../views/EmployeeListView.vue'
import EmployeeFormView from '../views/EmployeeFormView.vue'
import DocumentsListView from '../views/DocumentsListView.vue'
import DocumentUploadView from '../views/DocumentUploadView.vue'
import DocumentDetailView from '../views/DocumentDetailView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/documents'
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
      path: '/documents',
      name: 'documents',
      component: DocumentsListView,
      meta: { requiresAuth: true }
    },
    {
      path: '/documents/upload',
      name: 'document-upload',
      component: DocumentUploadView,
      meta: { requiresAuth: true }
    },
    {
      path: '/documents/:id',
      name: 'document-detail',
      component: DocumentDetailView,
      meta: { requiresAuth: true }
    },

    /**
     * Se dejan estas rutas vivas por ahora,
     * pero solo para SUPER_ADMIN.
     * Así no rompes el flujo interno mientras
     * Personas deja de ser módulo principal visible.
     */
    {
      path: '/employees',
      name: 'employees',
      component: EmployeeListView,
      meta: { requiresAuth: true, requiresSuperAdmin: true }
    },
    {
      path: '/employees/new',
      name: 'employee-create',
      component: EmployeeFormView,
      meta: { requiresAuth: true, requiresSuperAdmin: true }
    },
    {
      path: '/employees/:id/edit',
      name: 'employee-edit',
      component: EmployeeFormView,
      meta: { requiresAuth: true, requiresSuperAdmin: true }
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
  const token = localStorage.getItem('auth_token')
  const rawUser = localStorage.getItem('auth_user')

  let user = null
  try {
    user = rawUser ? JSON.parse(rawUser) : null
  } catch {
    user = null
  }

  const roles = Array.isArray(user?.roles) ? user.roles : []
  const isSuperAdmin = roles.includes('SUPER_ADMIN')

  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }

  if (to.meta.guestOnly && token) {
    return next('/documents')
  }

  if (to.meta.requiresSuperAdmin && !isSuperAdmin) {
    return next('/documents')
  }

  return next()
})

export default router