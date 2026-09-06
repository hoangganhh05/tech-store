import { httpClient } from './httpClient'

export type Brand = {
  id: number
  name: string
  logoUrl?: string | null
  description?: string | null
  createdAt: string
  updatedAt: string
}

export type BrandPayload = {
  name: string
  logoUrl?: string | null
  description?: string | null
}

type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export async function getAdminBrands(): Promise<Brand[]> {
  const response = await httpClient.get<ApiResponse<Brand[]>>('/admin/brands')
  return response.data.data
}

export async function getAdminBrandById(id: number): Promise<Brand> {
  const response = await httpClient.get<ApiResponse<Brand>>(`/admin/brands/${id}`)
  return response.data.data
}

export async function createAdminBrand(payload: BrandPayload): Promise<Brand> {
  const response = await httpClient.post<ApiResponse<Brand>>('/admin/brands', payload)
  return response.data.data
}

export async function updateAdminBrand(id: number, payload: BrandPayload): Promise<Brand> {
  const response = await httpClient.put<ApiResponse<Brand>>(`/admin/brands/${id}`, payload)
  return response.data.data
}

export async function deleteAdminBrand(id: number): Promise<void> {
  await httpClient.delete<ApiResponse<void>>(`/admin/brands/${id}`)
}
