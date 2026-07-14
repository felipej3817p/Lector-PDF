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
          <img
            src="/logofinalblanco.png"
            alt="Alturas"
            class="login-brand-logo light-theme-logo"
          />
          <img
            src="/logofinaloscuro.png"
            alt="Alturas"
            class="login-brand-logo dark-theme-logo"
          />

          <div>
            <div class="login-brand-subtitle">Gestión de aptitudes</div>
          </div>
        </div>

        <div class="mb-4">
          <h1 class="title mb-2">Crear nueva contraseña</h1>
          <p class="subtitle mb-0">
            Ingresa y confirma tu nueva contraseña para recuperar el acceso.
          </p>
        </div>

        <div class="hr"></div>

        <form class="d-flex flex-column gap-3" @submit.prevent="onSubmit">
          <div class="field">
            <div class="label-row">
              <label class="label" for="password">Nueva contraseña</label>
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
              v-model="password"
              class="input"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="Nueva contraseña"
              :disabled="loading"
            />
          </div>

          <div class="field">
            <label class="label" for="confirmPassword">Confirmar contraseña</label>
            <input
              id="confirmPassword"
              v-model="confirmPassword"
              class="input"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="Confirma la contraseña"
              :disabled="loading"
            />
          </div>

          <div v-if="message" class="state-box info">
            {{ message }}
          </div>

          <div v-if="tokenError" class="state-box error">
            {{ tokenError }}
          </div>

          <div v-else-if="error" class="state-box error">
            {{ error }}
          </div>

          <button
            type="submit"
            class="btn btn-primary w-100"
            :disabled="loading || !canSubmit || Boolean(tokenError)"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            ></span>
            {{ loading ? 'Guardando...' : 'Guardar nueva contraseña' }}
          </button>

          <div class="forgot-password-row justify-content-center">
            <RouterLink to="/login" class="link">
              Volver al inicio de sesión
            </RouterLink>
          </div>
        </form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const router = useRouter()

const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')
const message = ref('')

const token = computed(() => {
  return typeof route.query.token === 'string' ? route.query.token : ''
})

const tokenError = computed(() => {
  return token.value ? '' : 'El enlace de recuperación no es válido o está incompleto. Solicita uno nuevo.'
})

const canSubmit = computed(() => {
  return (
    token.value &&
    password.value.length >= 8 &&
    confirmPassword.value.length >= 8
  )
})

const toggleShow = () => {
  showPassword.value = !showPassword.value
}

const onSubmit = async () => {
  if (!canSubmit.value || loading.value) return

  error.value = ''
  message.value = ''

  if (tokenError.value) {
    error.value = tokenError.value
    return
  }

  if (password.value !== confirmPassword.value) {
    error.value = 'Las contraseñas no coinciden.'
    return
  }

  try {
    loading.value = true

    await http.post('/api/auth/reset-password', {
      token: token.value,
      newPassword: password.value
    })

    message.value = 'Contraseña actualizada correctamente. Ya puedes iniciar sesión.'

    setTimeout(() => {
      router.push('/login')
    }, 1800)
  } catch (err) {
    error.value = err?.response?.data?.message || 'No fue posible cambiar la contraseña. Solicita un nuevo enlace.'
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
  margin-top: 0.25rem;
}
</style>
