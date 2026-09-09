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

export type AdminOrderDetail = {
  id: number
  orderNumber: string
  customer: {
    id: number
    fullName: string
    email: string
    phone: string | null
  }
  status: string
  paymentMethod: 'COD' | 'BANK_TRANSFER' | 'ONLINE'
  paymentStatus: string
  cancellationReason: string | null
  internalNote: string | null
  subtotal: number
  discountAmount: number
  shippingFee: number
  totalAmount: number
  placedAt: string
  shippingAddress: {
    recipientName: string
    recipientPhone: string
    line1: string
    ward: string | null
    district: string
    province: string
  } | null
  items: Array<{
    productName: string
    sku: string
    variantLabel: string | null
    unitPrice: number
    quantity: number
    subtotal: number
  }>
  statusHistory: Array<{
    status: string
    changedAt: string
  }>
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

export async function getAdminOrderDetail(orderId: number): Promise<AdminOrderDetail> {
  const response = await httpClient.get<ApiResponse<AdminOrderDetail>>(`/admin/orders/${orderId}`)
  return response.data.data
}
