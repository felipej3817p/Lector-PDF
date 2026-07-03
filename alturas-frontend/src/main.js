import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { applyTheme, getStoredTheme } from './utils/themePreferences'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import './assets/main.css'

applyTheme()

window.matchMedia?.('(prefers-color-scheme: dark)').addEventListener('change', () => {
  if (getStoredTheme() === 'system') {
    applyTheme('system')
  }
})

createApp(App)
  .use(createPinia())
  .use(router)
  .mount('#app')
