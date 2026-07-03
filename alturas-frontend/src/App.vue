<template>
  <RouterView v-if="isAuthPage" />

  <div v-else class="app-shell">
    <AppNavbar />

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppNavbar from './components/AppNavbar.vue'

const route = useRoute()

const isAuthPage = computed(() => {
  return (
    route.path === '/login' ||
    route.path === '/forgot-password' ||
    route.path === '/reset-password'
  )
})
</script>