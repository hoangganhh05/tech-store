import { httpClient } from './httpClient'

type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type AdminOrderSummary = {
  id: number
  orderNumber: string
  customerName: string
  customerPhone: string | null
  totalAmount: number
  status: string
  placedAt: string
}

export type GetAdminOrdersParams = {
  search?: string
  status?: string
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

export async function getAdminOrders(params: GetAdminOrdersParams = {}): Promise<PageResponse<AdminOrderSummary>> {
  const response = await httpClient.get<ApiResponse<PageResponse<AdminOrderSummary>>>('/admin/orders', {
    params: {
      search: params.search || undefined,
      status: params.status || undefined,
      fromDate: params.fromDate || undefined,
      toDate: params.toDate || undefined,
      page: params.page ?? 0,
      size: params.size ?? 10,
    },
  })
  return response.data.data
}
