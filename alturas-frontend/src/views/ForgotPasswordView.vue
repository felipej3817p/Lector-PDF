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
            src="/logo-light-theme.png"
            alt="Alturas"
            class="login-brand-logo light-theme-logo"
          />
          <img
            src="/logo-dark-theme.png"
            alt="Alturas"
            class="login-brand-logo dark-theme-logo"
          />

          <div>
            <div class="login-brand-title">Alturas</div>
            <div class="login-brand-subtitle">Gestión de aptitud en alturas</div>
          </div>
        </div>

        <div class="mb-4">
          <h1 class="title mb-2">Recuperar contraseña</h1>
          <p class="subtitle mb-0">
            Ingresa el correo asociado a tu usuario. Si está registrado, recibirás un enlace para crear una nueva contraseña.
          </p>
        </div>

        <div class="hr"></div>

        <form class="d-flex flex-column gap-3" @submit.prevent="onSubmit">
          <div class="field">
            <label class="label" for="email">Correo electrónico</label>
            <input
              id="email"
              v-model.trim="email"
              class="input"
              type="email"
              autocomplete="email"
              placeholder="usuario@empresa.com"
              :disabled="loading"
            />
          </div>

          <div v-if="message" class="state-box info">
            {{ message }}
          </div>

          <div v-if="error" class="state-box error">
            {{ error }}
          </div>

          <button
            type="submit"
            class="btn btn-primary w-100"
            :disabled="loading || cooldownSeconds > 0 || !canSubmit"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            ></span>
            {{ submitLabel }}
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
import { computed, onBeforeUnmount, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'

const RESEND_COOLDOWN_SECONDS = 20

const email = ref('')
const loading = ref(false)
const error = ref('')
const message = ref('')
const cooldownSeconds = ref(0)
let cooldownTimer = null

const canSubmit = computed(() => {
  return email.value.trim().length > 0
})

const submitLabel = computed(() => {
  if (loading.value) return 'Enviando...'
  if (cooldownSeconds.value > 0) return `Podras reenviar en ${cooldownSeconds.value}s`
  return 'Enviar enlace de recuperacion'
})

const startCooldown = () => {
  window.clearInterval(cooldownTimer)
  cooldownSeconds.value = RESEND_COOLDOWN_SECONDS

  cooldownTimer = window.setInterval(() => {
    cooldownSeconds.value -= 1

    if (cooldownSeconds.value <= 0) {
      window.clearInterval(cooldownTimer)
      cooldownTimer = null
      cooldownSeconds.value = 0
    }
  }, 1000)
}

const onSubmit = async () => {
  if (!canSubmit.value || loading.value || cooldownSeconds.value > 0) return

  loading.value = true
  error.value = ''
  message.value = ''

  try {
    await http.post('/api/auth/forgot-password', {
      email: email.value
    })

    message.value = 'Si el correo está registrado, recibirás instrucciones para recuperar tu contraseña.'
    startCooldown()
  } catch (err) {
    const status = err?.response?.status

    if (status >= 500) {
      error.value = 'No fue posible enviar el correo. Espera 20 segundos e intenta nuevamente.'
      startCooldown()
    } else {
      message.value = 'Si el correo está registrado, recibirás instrucciones para recuperar tu contraseña.'
      startCooldown()
    }
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  window.clearInterval(cooldownTimer)
})
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

.login-brand-logo {
  height: 100px;
  width: auto;
  max-width: 100%;
  object-fit: contain;
  flex-shrink: 0;
}

.login-brand-logo.dark-theme-logo {
  height: 80px;
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

.forgot-password-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.25rem;
}
</style>
