const VALID_THEMES = ['light', 'dark', 'system']
const THEME_STORAGE_PREFIX = 'alturas_theme'

const safeParse = (value) => {
  try {
    return JSON.parse(value || 'null')
  } catch {
    return null
  }
}

const getStoredSessionUser = () => safeParse(localStorage.getItem('auth_user'))

const userStorageKey = (user) => {
  const identity = String(user?.username || user?.email || 'anonymous')
    .trim()
    .toLowerCase()

  return `${THEME_STORAGE_PREFIX}:${encodeURIComponent(identity)}`
}

export const getStoredTheme = (user = getStoredSessionUser()) => {
  const storedTheme = localStorage.getItem(userStorageKey(user))
  return VALID_THEMES.includes(storedTheme) ? storedTheme : 'system'
}

export const getSystemTheme = () =>
  window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'

export const applyTheme = (themePreference = getStoredTheme()) => {
  const resolvedTheme = themePreference === 'system' ? getSystemTheme() : themePreference

  document.documentElement.dataset.theme = resolvedTheme
  document.documentElement.dataset.themePreference = themePreference
}

export const saveTheme = (themePreference, user = getStoredSessionUser()) => {
  const normalizedTheme = VALID_THEMES.includes(themePreference) ? themePreference : 'system'

  localStorage.setItem(userStorageKey(user), normalizedTheme)
  applyTheme(normalizedTheme)

  return normalizedTheme
}
