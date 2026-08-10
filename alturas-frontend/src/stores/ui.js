import { defineStore } from 'pinia'
import { ref } from 'vue'

import { getStoredTheme, getSystemTheme, saveTheme, applyTheme } from '../utils/themePreferences'

export const useUIStore = defineStore('ui', () => {
  // Theme State
  const currentTheme = ref('system')
  const isDarkTheme = ref(false)

  const updateThemeState = () => {
    currentTheme.value = getStoredTheme()
    isDarkTheme.value = currentTheme.value === 'system' 
      ? getSystemTheme() === 'dark' 
      : currentTheme.value === 'dark'
  }

  const setTheme = (theme) => {
    saveTheme(theme)
    updateThemeState()
  }

  const toggleTheme = () => {
    const newTheme = isDarkTheme.value ? 'light' : 'dark'
    setTheme(newTheme)
  }

  const initTheme = () => {
    applyTheme()
    updateThemeState()
    
    window.matchMedia?.('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (currentTheme.value === 'system') updateThemeState()
    })
  }

  // Alert State
  const alertData = ref({
    show: false,
    title: '',
    message: '',
    htmlContent: '', // for custom html like passwords
    type: 'info', // info, success, warning, error
    buttonText: 'Entendido'
  })
  
  // Confirm State
  const confirmData = ref({
    show: false,
    title: '',
    message: '',
    confirmText: 'Aceptar',
    cancelText: 'Cancelar',
    type: 'warning' // warning, danger, info
  })
  
  // Promises resolvers
  let resolveAlert = null
  let resolveConfirm = null

  // API
  const showAlert = (options) => {
    return new Promise((resolve) => {
      alertData.value = {
        show: true,
        title: options.title || 'Alerta',
        message: options.message || '',
        htmlContent: options.htmlContent || '',
        type: options.type || 'info',
        buttonText: options.buttonText || 'Entendido'
      }
      resolveAlert = resolve
    })
  }

  const closeAlert = () => {
    alertData.value.show = false
    if (resolveAlert) {
      resolveAlert()
      resolveAlert = null
    }
  }

  const showConfirm = (options) => {
    return new Promise((resolve) => {
      confirmData.value = {
        show: true,
        title: options.title || '¿Estás seguro?',
        message: options.message || '',
        confirmText: options.confirmText || 'Aceptar',
        cancelText: options.cancelText || 'Cancelar',
        type: options.type || 'warning'
      }
      resolveConfirm = resolve
    })
  }

  const resolveConfirmDialog = (result) => {
    confirmData.value.show = false
    if (resolveConfirm) {
      resolveConfirm(result)
      resolveConfirm = null
    }
  }

  return {
    isDarkTheme,
    currentTheme,
    setTheme,
    toggleTheme,
    initTheme,
    alertData,
    confirmData,
    showAlert,
    closeAlert,
    showConfirm,
    resolveConfirmDialog
  }
})
