import axios, { AxiosError } from 'axios'
import { env } from '../configs/env'
import { tokenStorage } from '../utils/tokenStorage'

export const AUTH_UNAUTHORIZED_EVENT = 'techstore:auth-unauthorized'
export const AUTH_FORBIDDEN_EVENT = 'techstore:auth-forbidden'

export const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

httpClient.interceptors.request.use((config) => {
  const accessToken = tokenStorage.getAccessToken()
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      tokenStorage.clear()
      window.dispatchEvent(new CustomEvent(AUTH_UNAUTHORIZED_EVENT))
    }
    if (error.response?.status === 403) {
      window.dispatchEvent(new CustomEvent(AUTH_FORBIDDEN_EVENT))
    }
    return Promise.reject(error)
  },
)
