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
          <div class="login-brand-logo-container">
            <img
              src="/logofinalblanco.png"
              alt="Logo SST Alturas"
              class="login-brand-logo light-theme-logo"
            />
            <img
              src="/logofinaloscuro.png"
              alt="Logo SST Alturas"
              class="login-brand-logo dark-theme-logo"
            />
          </div>

          <div>
            <div class="login-brand-subtitle">Gestión de aptitudes</div>
          </div>
        </div>

        <div class="mb-4" v-if="!isChangingPasswordState">
          <h1 class="title mb-2">Iniciar sesión</h1>
          <p class="subtitle mb-0">
            Accede al seguimiento de trabajadores, carga de evaluaciones y revisión de conceptos médicos.
          </p>
        </div>
        <div class="mb-4" v-else>
          <h1 class="title mb-2">Cambio Obligatorio</h1>
          <p class="subtitle mb-0">
            Por seguridad, debes actualizar tu contraseña generada temporalmente antes de continuar.
          </p>
        </div>

        <div class="hr"></div>

        <form v-if="!isChangingPasswordState" class="d-flex flex-column gap-3" @submit.prevent="onSubmit">
          <div class="field">
            <label class="label" for="identifier">Usuario o correo</label>
            <input
              id="identifier"
              name="login_identifier"
              v-model.trim="identifier"
              class="input"
              type="text"
              autocomplete="off"
              autocapitalize="none"
              spellcheck="false"
              placeholder="Ingresa tu usuario o correo"
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
              name="login_password"
              ref="passwordRef"
              v-model="password"
              class="input"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="••••••••"
              :disabled="loading"
            />
          </div>

          <div class="forgot-password-row">
            <RouterLink to="/forgot-password" class="link">
              ¿Olvidaste tu contraseña?
            </RouterLink>
          </div>

          <div v-if="successMessage" class="state-box success" style="color: var(--success); background: var(--success-soft); padding: 0.75rem; border-radius: 8px; border: 1px solid rgba(62,207,142,0.34); font-size: 0.85rem; text-align: center;">
            {{ successMessage }}
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

        <form v-else class="d-flex flex-column gap-3" @submit.prevent="onSubmitChangePassword">
          <div class="field">
            <label class="label">Nueva Contraseña</label>
            <input
              type="password"
              v-model="newPassword"
              class="input"
              placeholder="Al menos 6 caracteres"
              :disabled="loading"
            />
          </div>

          <div class="field">
            <label class="label">Confirmar Contraseña</label>
            <input
              type="password"
              v-model="confirmPassword"
              class="input"
              placeholder="Confirmar"
              :disabled="loading"
            />
          </div>

          <div v-if="error" class="state-box error">
            {{ error }}
          </div>

          <button
            type="submit"
            class="btn btn-primary w-100"
            :disabled="loading || !canSubmitChangePassword"
          >
            <span v-if="loading" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
            {{ loading ? 'Guardando...' : 'Guardar y Continuar' }}
          </button>
        </form>

        <div class="login-note">
          <div class="mini">
            <div class="mini-title"></div>
            <div class="mini-value">Carga evaluaciones y gestiona trabajadores de su zona</div>
          </div>

          <div class="mini">
            <div class="mini-title"></div>
            <div class="mini-value">Revisa conceptos y autoriza notificaciones</div>
          </div>
        </div>

        <div class="mt-4 d-flex justify-content-center"></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const isChangingPasswordState = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')

const identifier = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')
const successMessage = ref('')
const passwordRef = ref(null)

const canSubmit = computed(() => {
  return identifier.value.trim().length > 0 && password.value.length > 0
})

const toggleShow = () => {
  showPassword.value = !showPassword.value
}

const focusPassword = () => {
  passwordRef.value?.focus?.()
}

onMounted(() => {
  if (auth.user?.mustChangePassword) {
    // Si el usuario presiona F5 estando atrapado aquí, cerramos su sesión parcial
    auth.logout()
    isChangingPasswordState.value = false
  }
  identifier.value = ''
  password.value = ''
})

const resolvePostLoginRoute = () => {
  const roles = Array.isArray(auth.user?.roles) ? auth.user.roles : []

  if (roles.includes('APROBADOR') && !roles.includes('SUPER_ADMIN')) {
    return '/review'
  }

  return '/employees'
}

const isInactiveUserError = (rawMessage) => {
  const message = String(rawMessage || '').toLowerCase()

  return (
    message.includes('inactivo') ||
    message.includes('inhabilitado') ||
    message.includes('inactive') ||
    message.includes('disabled') ||
    message.includes('deshabilitado') ||
    message.includes('usuario está inactivo') ||
    message.includes('usuario esta inactivo')
  )
}

const isExpiredUserError = (rawMessage) => {
  const message = String(rawMessage || '').toLowerCase()
  return message.includes('vigencia') && (message.includes('finalizó') || message.includes('finalizo') || message.includes('venc'))
}

const isNotStartedUserError = (rawMessage) => {
  const message = String(rawMessage || '').toLowerCase()
  return message.includes('vigencia') && (message.includes('no ha iniciado') || message.includes('todavía no') || message.includes('todavia no'))
}

const isBadCredentialsError = (status, rawMessage) => {
  const message = String(rawMessage || '').toLowerCase()

  return (
    status === 401 ||
    status === 403 ||
    status === 400 ||
    message.includes('badcredentials') ||
    message.includes('bad credentials') ||
    message.includes('credenciales') ||
    message.includes('contraseña') ||
    message.includes('password') ||
    message.includes('inválidos') ||
    message.includes('invalid')
  )
}

const onSubmit = async () => {
  if (!canSubmit.value || loading.value) return

  error.value = ''
  successMessage.value = ''
  loading.value = true

  try {
    await auth.login(identifier.value, password.value)

    if (auth.user?.mustChangePassword) {
      isChangingPasswordState.value = true
      return
    }

    const redirect = typeof route.query.redirect === 'string'
      ? route.query.redirect
      : resolvePostLoginRoute()

    router.push(redirect)
  } catch (e) {
    const status = e?.response?.status
    const rawMessage =
      e?.response?.data?.message ||
      e?.response?.data?.error ||
      e?.message ||
      ''

    if (isInactiveUserError(rawMessage)) {
      error.value = 'Tu usuario está inhabilitado. Comunícate con tu jefe inmediato o con el administrador del sistema.'
    } else if (isExpiredUserError(rawMessage)) {
      error.value = 'La vigencia de tu usuario finalizó. Comunícate con tu jefe inmediato o con el administrador del sistema.'
    } else if (isNotStartedUserError(rawMessage)) {
      error.value = 'Tu acceso todavía no está habilitado porque la vigencia no ha iniciado.'
    } else if (isBadCredentialsError(status, rawMessage)) {
      error.value = 'Usuario o contraseña inválidos.'
    } else if (status === 404) {
      error.value = 'En este momento no pudimos conectarnos al sistema. Intenta nuevamente en unos minutos.'
    } else {
      error.value = 'No pudimos iniciar sesión en este momento. Intenta nuevamente o comunícate con soporte si el problema continúa.'
    }
  } finally {
    loading.value = false
  }
}

const canSubmitChangePassword = computed(() => {
  return newPassword.value.length >= 6 && confirmPassword.value.length > 0
})

const onSubmitChangePassword = async () => {
  if (newPassword.value.length < 6) {
    error.value = 'La contraseña debe tener al menos 6 caracteres.'
    return
  }
  
  if (newPassword.value !== confirmPassword.value) {
    error.value = 'Las contraseñas no coinciden.'
    return
  }

  error.value = ''
  successMessage.value = ''
  loading.value = true

  try {
    await http.post('/api/auth/change-password', {
      newPassword: newPassword.value
    })
    
    auth.logout()

    password.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    isChangingPasswordState.value = false
    
    successMessage.value = 'Contraseña actualizada correctamente. Por favor inicia sesión con tu nueva contraseña.'
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al cambiar contraseña.'
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
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 2rem;
  text-align: center;
}

.login-brand-logo {
  height: 130px;
  width: auto;
  max-width: 100%;
  object-fit: contain;
  flex-shrink: 0;
}

.login-brand-logo.dark-theme-logo {
  height: 130px;
}

.login-brand-title {
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.login-brand-subtitle {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.forgot-password-row {
  display: flex;
  justify-content: flex-end;
  margin-top: -0.35rem;
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
