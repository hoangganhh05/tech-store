import { httpClient } from './httpClient'
import type { AuthenticatedUser } from './authService'

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export type GetAdminUsersParams = {
  keyword?: string
  page?: number
  size?: number
}

export async function getAdminUsers(params: GetAdminUsersParams = {}): Promise<PageResponse<AuthenticatedUser>> {
  const response = await httpClient.get<ApiResponse<PageResponse<AuthenticatedUser>>>('/admin/users', {
    params: {
      keyword: params.keyword || undefined,
      page: params.page ?? 0,
      size: params.size ?? 10,
    },
  })
  return response.data.data
}

export async function updateAdminUserStatus(userId: number, status: 'ACTIVE' | 'LOCKED'): Promise<AuthenticatedUser> {
  const response = await httpClient.patch<ApiResponse<AuthenticatedUser>>(`/admin/users/${userId}/status`, {
    status,
  })
  return response.data.data
}