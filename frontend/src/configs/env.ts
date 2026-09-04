const removeTrailingSlash = (value: string) => value.replace(/\/$/, '')

export const env = Object.freeze({
  appName: import.meta.env.VITE_APP_NAME || 'TechStore',
  apiBaseUrl: removeTrailingSlash(
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  ),
})
