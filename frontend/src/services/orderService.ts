import { httpClient } from './httpClient'
import type { PaymentMethod } from './checkoutService'

export type PlacedOrderItem = {
  productName: string
  variantLabel: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export type PlacedOrder = {
  id: number
  orderNumber: string
  status: string
  totalAmount: number
  placedAt: string
  estimatedProcessingTime: string
  items: PlacedOrderItem[]
}

export type OrderHistoryItem = {
  id: number
  orderNumber: string
  placedAt: string
  totalAmount: number
  status: string
}

export type OrderHistoryPage = {
  items: OrderHistoryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export async function placeOrder(addressId: number, paymentMethod: PaymentMethod, voucherCode?: string): Promise<PlacedOrder> {
  const response = await httpClient.post<{ data: PlacedOrder }>('/orders', {
    addressId,
    paymentMethod,
    ...(voucherCode?.trim() ? { voucherCode: voucherCode.trim() } : {}),
  })
  return response.data.data
}

export async function getMyOrders(params: {
  page?: number
  size?: number
  status?: string
} = {}): Promise<OrderHistoryPage> {
  const searchParams = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 10),
  })
  if (params.status) searchParams.set('status', params.status)

  const response = await httpClient.get<{ data: OrderHistoryPage }>(`/orders/my-orders?${searchParams.toString()}`)
  return response.data.data
}
