<template>
  <!-- Global Alert Modal -->
  <div v-if="ui.alertData.show" class="modal-overlay">
    <div class="modal-content text-center">
      <div class="modal-icon mb-3">
        <span style="font-size: 3rem;">
          {{ ui.alertData.type === 'success' ? '✅' : ui.alertData.type === 'error' ? '❌' : ui.alertData.type === 'warning' ? '⚠️' : 'ℹ️' }}
        </span>
      </div>
      <h3 class="mb-3">{{ ui.alertData.title }}</h3>
      
      <p v-if="ui.alertData.message" class="mb-4">{{ ui.alertData.message }}</p>
      
      <div v-if="ui.alertData.htmlContent" class="mb-4 text-start" v-html="ui.alertData.htmlContent"></div>
      
      <button class="btn btn-primary w-100" @click="ui.closeAlert">
        {{ ui.alertData.buttonText }}
      </button>
    </div>
  </div>

  <!-- Global Confirm Modal -->
  <div v-if="ui.confirmData.show" class="modal-overlay">
    <div class="modal-content text-center">
      <div class="modal-icon mb-3">
        <span style="font-size: 3rem;">
          {{ ui.confirmData.type === 'danger' ? '🗑️' : '⚠️' }}
        </span>
      </div>
      <h3 class="mb-3">{{ ui.confirmData.title }}</h3>
      <p class="mb-4">{{ ui.confirmData.message }}</p>
      
      <div class="d-flex gap-2">
        <button class="btn btn-outline-secondary w-50" @click="ui.resolveConfirmDialog(false)">
          {{ ui.confirmData.cancelText }}
        </button>
        <button class="btn w-50" :class="ui.confirmData.type === 'danger' ? 'btn-danger' : 'btn-primary'" @click="ui.resolveConfirmDialog(true)">
          {{ ui.confirmData.confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useUIStore } from '../stores/ui'

const ui = useUIStore()
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
  max-width: 450px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  animation: modal-fade-in 0.3s ease-out;
}

@keyframes modal-fade-in {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

