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

export type Address = {
  id: number
  recipientName: string
  phone: string
  province: string
  district: string
  ward: string
  streetAddress: string
  isDefault: boolean
  createdAt: string
}

export type AddressPayload = {
  recipientName: string
  phone: string
  province: string
  district: string
  ward: string
  streetAddress: string
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

export async function getMyAddresses(): Promise<Address[]> {
  const response = await httpClient.get<ApiResponse<Address[]>>('/users/me/addresses')
  return response.data.data
}

export async function addMyAddress(payload: AddressPayload): Promise<Address> {
  const response = await httpClient.post<ApiResponse<Address>>('/users/me/addresses', payload)
  return response.data.data
}

export async function updateMyAddress(id: number, payload: AddressPayload): Promise<Address> {
  const response = await httpClient.put<ApiResponse<Address>>(`/users/me/addresses/${id}`, payload)
  return response.data.data
}

export async function deleteMyAddress(id: number): Promise<void> {
  await httpClient.delete(`/users/me/addresses/${id}`)
}

export async function setDefaultAddress(id: number): Promise<Address> {
  const response = await httpClient.patch<ApiResponse<Address>>(`/users/me/addresses/${id}/default`)
  return response.data.data
}

