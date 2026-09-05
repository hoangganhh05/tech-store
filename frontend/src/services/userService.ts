import { httpClient } from './httpClient'

export type UserProfile = {
  id: number
  email: string
  fullName: string
  phone: string
  dateOfBirth: string | null
  updatedAt: string
}

export type UpdateProfilePayload = {
  fullName: string
  phone: string
  dateOfBirth: string | null
}

export type ChangePasswordPayload = {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export async function getMyProfile(): Promise<UserProfile> {
  const response = await httpClient.get<ApiResponse<UserProfile>>('/users/me')
  return response.data.data
}

export async function updateMyProfile(payload: UpdateProfilePayload): Promise<UserProfile> {
  const response = await httpClient.put<ApiResponse<UserProfile>>('/users/me', payload)
  return response.data.data
}

export async function changeMyPassword(payload: ChangePasswordPayload): Promise<void> {
  await httpClient.put('/users/me/password', payload)
}
