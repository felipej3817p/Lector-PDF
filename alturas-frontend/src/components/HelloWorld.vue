<template>
  <section class="hero card">
    <div class="card-body">
      <div class="hero-top">
        <div class="hero-copy">
          <span class="hero-tag">EBSA · Plataforma interna</span>
          <h1 class="h1">Sistema de Gestión de Aptitud para Trabajo en Alturas</h1>
          <p class="p">
            Plataforma interna para cargar conceptos médicos, revisar resultados,
            gestionar funcionarios y dar trazabilidad al proceso.
          </p>
        </div>

        <div class="hero-status">
          <div class="status-card">
            <div class="status-label">Estado de sesión</div>
            <div class="status-value">
              {{ auth.isAuthenticated ? 'Activa' : 'No iniciada' }}
            </div>
            <div class="status-note">
              {{ auth.isAuthenticated ? 'Acceso disponible a módulos del sistema' : 'Ingresa para consultar y gestionar información' }}
            </div>
          </div>
        </div>
      </div>

      <div class="actions">
        <router-link class="btn btn-primary" to="/documents" v-if="auth.isAuthenticated">
          Ir a documentos
        </router-link>

        <router-link class="btn btn-primary" to="/login" v-else>
          Iniciar sesión
        </router-link>

        <router-link class="btn" to="/employees" v-if="auth.isAuthenticated">
          Ver funcionarios
        </router-link>

        <a class="btn" href="javascript:void(0)" @click="copyInfo">
          Copiar resumen
        </a>
      </div>

      <div class="hr"></div>

      <div class="grid info">
        <div class="mini">
          <div class="mini-title">Módulo principal</div>
          <div class="mini-value">Lectura y gestión de conceptos médicos en PDF</div>
        </div>

        <div class="mini">
          <div class="mini-title">Frontend</div>
          <div class="mini-value">Interfaz de seguimiento, revisión y consulta</div>
        </div>

        <div class="mini">
          <div class="mini-title">Trazabilidad</div>
          <div class="mini-value">Control de carga, análisis y gestión documental</div>
        </div>
      </div>

      <div class="hr"></div>

      <div class="grid highlights">
        <div class="highlight">
          <div class="highlight-title">Carga de documentos</div>
          <p class="highlight-text">
            Permite subir certificados y conceptos médicos para su consulta y análisis.
          </p>
        </div>

        <div class="highlight">
          <div class="highlight-title">Consulta centralizada</div>
          <p class="highlight-text">
            Organiza documentos y funcionarios en una sola plataforma interna.
          </p>
        </div>

        <div class="highlight">
          <div class="highlight-title">Apoyo al proceso</div>
          <p class="highlight-text">
            Facilita la revisión, seguimiento y preparación de información para reportes.
          </p>
        </div>
      </div>

      <p class="hint" v-if="copied">Resumen copiado.</p>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const copied = ref(false)

async function copyInfo() {
  const text = [
    'Sistema de Gestión de Aptitud para Trabajo en Alturas',
    `Sesión: ${auth.isAuthenticated ? 'activa' : 'no iniciada'}`,
    'Módulo principal: lectura y gestión de conceptos médicos en PDF',
    'Frontend: seguimiento, revisión y consulta'
  ].join('\n')

  try {
    await navigator.clipboard.writeText(text)
    copied.value = true
    setTimeout(() => (copied.value = false), 1200)
  } catch {
    // El navegador puede bloquear clipboard.
  }
}
</script>

<style scoped>
.hero{
  overflow: hidden;
}

.hero-top{
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 18px;
  align-items: stretch;
}

.hero-copy{
  display: grid;
  align-content: start;
  gap: 8px;
}

.hero-tag{
  display: inline-flex;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .02em;
}

.hero-status{
  display: flex;
}

.status-card{
  width: 100%;
  border-radius: 18px;
  padding: 18px;
  background: linear-gradient(180deg, #0f3d75 0%, #174f96 100%);
  color: white;
  box-shadow: var(--shadow);
}

.status-label{
  font-size: 12px;
  opacity: .85;
  text-transform: uppercase;
  letter-spacing: .05em;
}

.status-value{
  margin-top: 8px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.status-note{
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.5;
  opacity: .92;
}

.actions{
  margin-top: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.info{
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mini{
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: var(--surface-2);
}

.mini-title{
  font-size: 12px;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: .04em;
}

.mini-value{
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
}

.highlights{
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.highlight{
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
}

.highlight-title{
  font-size: 15px;
  font-weight: 800;
  color: var(--text);
}

.highlight-text{
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--muted);
  line-height: 1.6;
}

.hint{
  margin-top: 12px;
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 960px){
  .hero-top{
    grid-template-columns: 1fr;
  }

  .info,
  .highlights{
    grid-template-columns: 1fr;
  }
}
</style>