<template>
  <div class="layout-wrapper">
    <template v-if="isAuthPage">
      <main class="auth-main">
        <slot />
      </main>
    </template>

    <template v-else>
      <header class="header-corporativo">
        <AppNavbar />
      </header>

      <main class="main-content">
        <div class="container-xl py-4">
          <slot />
        </div>
      </main>

      <footer class="footer-corporativo">
        <div class="container-xl footer-inner">
          <div>
            <strong>EBSA</strong>
            <span class="footer-divider">•</span>
            Panel de gestión documental de aptitud en alturas
          </div>
          <small>Vue 3 + Spring Boot + MongoDB</small>
        </div>
      </footer>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppNavbar from '../components/AppNavbar.vue'

const route = useRoute()
const isAuthPage = computed(() => route.name === 'login')
</script>