<template>
  <div class="global-top-actions fixed-global" v-if="isAuthPage">
    <button
      type="button"
      class="icon-action-btn"
      @click="uiStore.toggleTheme"
      title="Cambiar tema"
    >
      <svg v-if="uiStore.isDarkTheme" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>
      <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>
    </button>
  </div>

  <RouterView v-if="isAuthPage" />

  <div v-else class="app-shell">
    <AppNavbar />

    <main class="app-main">
      <RouterView />
    </main>
  </div>

  <GlobalModals />
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppNavbar from './components/AppNavbar.vue'
import GlobalModals from './components/GlobalModals.vue'
import { useAuthStore } from './stores/auth'
import { useUIStore } from './stores/ui'

const route = useRoute()
const authStore = useAuthStore()
const uiStore = useUIStore()

uiStore.initTheme()

const isAuthPage = computed(() => {
  return (
    route.path === '/login' ||
    route.path === '/forgot-password' ||
    route.path === '/reset-password'
  )
})
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
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--border);
  padding: 2rem;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: var(--text);
}

.modal-content p {
  color: var(--text-soft);
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

.app-main {
  position: relative;
}

.global-top-actions {
  position: absolute;
  top: 1.25rem;
  right: 1.25rem;
  display: flex;
  gap: 0.5rem;
  z-index: 50;
}

.fixed-global {
  position: fixed !important;
  z-index: 9999 !important;
}

.icon-action-btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  color: var(--text-muted);
  transition: 0.2s ease;
  cursor: pointer;
}

.icon-action-btn:hover {
  border-color: rgba(62, 207, 142, 0.4);
  background: var(--primary-soft);
  color: var(--text);
}

.icon-action-btn--logout:hover {
  border-color: rgba(220, 38, 38, 0.4);
  background: var(--danger-soft);
  color: var(--danger);
}
</style>