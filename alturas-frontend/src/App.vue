<template>
  <RouterView v-if="isAuthPage" />

  <div v-else class="app-shell">
    <AppNavbar />

    <main class="app-main">
      <RouterView />
    </main>

    <!-- Modal for forced password change -->
    <div class="modal-overlay" v-if="authStore.user?.mustChangePassword">
      <div class="modal-content">
        <h2>Cambio de Contraseña Obligatorio</h2>
        <p>Por razones de seguridad, debes actualizar tu contraseña generada temporalmente antes de continuar.</p>
        
        <div class="form-group">
          <label class="label">Nueva Contraseña</label>
          <input type="password" class="input" v-model="newPassword" placeholder="Al menos 6 caracteres" />
        </div>
        
        <div class="form-group">
          <label class="label">Confirmar Contraseña</label>
          <input type="password" class="input" v-model="confirmPassword" />
        </div>

        <p class="error-msg" v-if="passwordError">{{ passwordError }}</p>

        <div class="modal-actions">
          <button class="primary-btn" @click="changePassword" :disabled="isChangingPassword">Guardar Contraseña</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppNavbar from './components/AppNavbar.vue'
import { useAuthStore } from './stores/auth'
import http from './api/http'

const route = useRoute()
const authStore = useAuthStore()

const newPassword = ref('')
const confirmPassword = ref('')
const passwordError = ref('')
const isChangingPassword = ref(false)

const isAuthPage = computed(() => {
  return (
    route.path === '/login' ||
    route.path === '/forgot-password' ||
    route.path === '/reset-password'
  )
})

const changePassword = async () => {
  passwordError.value = ''
  
  if (newPassword.value.length < 6) {
    passwordError.value = 'La contraseña debe tener al menos 6 caracteres.'
    return
  }
  
  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = 'Las contraseñas no coinciden.'
    return
  }

  try {
    isChangingPassword.value = true
    await http.post('/api/auth/change-password', {
      newPassword: newPassword.value
    })
    
    // Update store state so modal disappears
    if (authStore.user) {
      authStore.user.mustChangePassword = false
      authStore.setSession(authStore.token, authStore.user)
    }
    
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (error) {
    passwordError.value = error.response?.data?.message || 'Error al cambiar contraseña.'
  } finally {
    isChangingPassword.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #0f172a;
}

.modal-content p {
  color: #475569;
  margin-bottom: 1.5rem;
  font-size: 0.95rem;
}

.form-group {
  margin-bottom: 1rem;
}

.error-msg {
  color: #ef4444 !important;
  font-size: 0.85rem !important;
  margin-bottom: 1rem !important;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.5rem;
}
</style>