import { httpClient } from './httpClient'

export interface AdminNotification {
  id: number
  title: string
  message: string
  type: string
  orderId: number | null
  orderNumber: string | null
  isRead: boolean
  createdAt: string
  readAt: string | null
}

export interface AdminNotificationListResponse {
  items: AdminNotification[]
  unreadCount: number
  page: number
  size: number
  totalElements: number
  totalPages: number
}

interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
}

export async function getAdminNotifications(page = 0, size = 20): Promise<AdminNotificationListResponse> {
  const response = await httpClient.get<ApiResponse<AdminNotificationListResponse>>('/admin/notifications', {
    params: { page, size },
  })
  return response.data.data
}

export async function markNotificationAsRead(id: number): Promise<AdminNotification> {
  const response = await httpClient.patch<ApiResponse<AdminNotification>>(`/admin/notifications/${id}/read`)
  return response.data.data
}

export async function markAllNotificationsAsRead(): Promise<number> {
  const response = await httpClient.patch<ApiResponse<number>>('/admin/notifications/read-all')
  return response.data.data
}
