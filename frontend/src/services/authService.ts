import { httpClient } from './httpClient'

export type RegisterPayload = {
  fullName: string
  email: string
  phone: string
  password: string
  confirmPassword: string
}

export type RegisteredUser = {
  id: number
  email: string
  fullName: string
  phone: string
  status: string
  roles: string[]
  emailVerified: boolean
  createdAt: string
}

type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export async function registerAccount(payload: RegisterPayload) {
  const response = await httpClient.post<ApiResponse<RegisteredUser>>('/auth/register', payload)
  return response.data
}
