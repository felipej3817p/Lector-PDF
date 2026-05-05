<template>
  <section class="page">
    <div class="state-box info">
      Redirigiendo al panel principal...
    </div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

onMounted(() => {
  router.replace(auth.isAuthenticated ? '/documents' : '/login')
})
</script>