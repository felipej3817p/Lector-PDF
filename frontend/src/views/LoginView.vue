<template>
  <div class="login-shell">
    <div class="login-bg" aria-hidden="true">
      <div class="login-orb login-orb-1"></div>
      <div class="login-orb login-orb-2"></div>
      <div class="login-gridlines"></div>
    </div>

    <section class="login-card card border-0">
      <div class="card-body p-4 p-md-5">
        <div class="login-brand">
          <span class="login-brand-mark"></span>
          <div>
            <div class="login-brand-title">EBSA</div>
            <div class="login-brand-subtitle">Gestión de aptitud en alturas</div>
          </div>
        </div>

        <div class="mb-4">
          <h1 class="title mb-2">Iniciar sesión</h1>
          <p class="subtitle mb-0">
            Accede al módulo de funcionarios, documentos PDF y análisis de conceptos médicos.
          </p>
        </div>

        <div class="hr"></div>

        <form class="d-flex flex-column gap-3" @submit.prevent="onSubmit">
          <div class="field">
            <label class="label" for="username">Usuario</label>
            <input
              id="username"
              v-model.trim="username"
              class="input"
              type="text"
              autocomplete="username"
              placeholder="Ingresa tu usuario"
              :disabled="loading"
              @keydown.enter.prevent="focusPassword"
            />
          </div>

          <div class="field">
            <div class="label-row">
              <label class="label" for="password">Contraseña</label>
              <button
                class="link"
                type="button"
                :disabled="loading"
                @click="toggleShow"
              >
                {{ showPassword ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>

            <input
              id="password"
              ref="passwordRef"
              v-model="password"
              class="input"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="••••••••"
              :disabled="loading"
            />
          </div>

          <div v-if="error" class="state-box error">
            {{ error }}
          </div>

          <button
            type="submit"
            class="btn btn-primary w-100"
            :disabled="loading || !canSubmit"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            ></span>
            {{ loading ? 'Ingresando...' : 'Ingresar al sistema' }}
          </button>
        </form>

        <div class="login-note">
          <div class="mini">
            <div class="mini-title">Acceso</div>
            <div class="mini-value">JWT con sesión autenticada</div>
          </div>
          <div class="mini">
            <div class="mini-title">Prueba local</div>
            <div class="mini-value">admin / Admin123*</div>
          </div>
        </div>

        <div class="mt-4 d-flex justify-content-center">
          <RouterLink to="/" class="secondary-btn">
            Volver al inicio
          </RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('admin')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')
const passwordRef = ref(null)

const canSubmit = computed(() => {
  return username.value.trim().length > 0 && password.value.length > 0
})

const toggleShow = () => {
  showPassword.value = !showPassword.value
}

const focusPassword = () => {
  passwordRef.value?.focus?.()
}

const onSubmit = async () => {
  if (!canSubmit.value || loading.value) return

  error.value = ''
  loading.value = true

  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch (e) {
    const status = e?.response?.status
    const message = e?.response?.data?.message

    if (status === 404) {
      error.value = 'No se encontró el endpoint de autenticación. Revisa la URL base del backend en src/api/http.js.'
    } else if (status === 401 || status === 403) {
      error.value = message || 'Usuario o contraseña inválidos.'
    } else {
      error.value = message || 'No fue posible iniciar sesión. Revisa la conexión con el backend.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 1.5rem;
  overflow: hidden;
}

.login-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  opacity: 0.9;
}

.login-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(75px);
}

.login-orb-1 {
  width: 320px;
  height: 320px;
  background: rgba(63, 111, 143, 0.22);
  top: -90px;
  left: -70px;
}

.login-orb-2 {
  width: 340px;
  height: 340px;
  background: rgba(124, 110, 230, 0.18);
  right: -90px;
  bottom: -110px;
}

.login-gridlines {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(203, 213, 225, 0.45) 1px, transparent 1px),
    linear-gradient(90deg, rgba(203, 213, 225, 0.45) 1px, transparent 1px);
  background-size: 38px 38px;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 460px;
  border-radius: 24px !important;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1.5rem;
}

.login-brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--primary) 0%, #79a9c7 100%);
  position: relative;
  flex-shrink: 0;
}

.login-brand-mark::after {
  content: '';
  position: absolute;
  inset: 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
}

.login-brand-title {
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.login-brand-subtitle {
  font-size: 0.8rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.login-note {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
  margin-top: 1.4rem;
}

@media (max-width: 575.98px) {
  .login-note {
    grid-template-columns: 1fr;
  }
}
</style>