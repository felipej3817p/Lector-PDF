<template>
  <div class="layout-wrapper">
    <template v-if="isAuthPage">
      <main class="auth-main">
        <slot />
      </main>
    </template>

    <template v-else>
      <div class="app-shell">
        <AppNavbar />

        <main class="main-content" :class="{ 'main-content--wide': isWidePage }">
          <div class="app-content-container" :class="{ 'app-content-container--wide': isWidePage }">
            <slot />
          </div>
        </main>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppNavbar from '../components/AppNavbar.vue'

const route = useRoute()
const isAuthPage = computed(() => route.name === 'login')
const isWidePage = computed(() => Boolean(route.meta.fullWidth))
</script>
