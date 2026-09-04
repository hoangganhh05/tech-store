import { httpClient } from './httpClient'

export type RegisterPayload = {
  fullName: string
  email: string
  phone: string
  password: string
  confirmPassword: string
}

export type AuthenticatedUser = {
  id: number
  email: string
  fullName: string
  phone: string
  status: string
  roles: string[]
  emailVerified: boolean
  createdAt: string
}

export type LoginPayload = {
  email: string
  password: string
}

export type LoginResult = {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  accessTokenExpiresAt: string
  refreshTokenExpiresAt: string
  user: AuthenticatedUser
}

export type LogoutPayload = {
  refreshToken: string
}

type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export async function registerAccount(payload: RegisterPayload) {
  const response = await httpClient.post<ApiResponse<AuthenticatedUser>>('/auth/register', payload)
  return response.data
}

export async function loginAccount(payload: LoginPayload) {
  const response = await httpClient.post<ApiResponse<LoginResult>>('/auth/login', payload)
  return response.data.data
}

export async function logoutAccount(payload: LogoutPayload): Promise<void> {
  await httpClient.post('/auth/logout', payload)
}
