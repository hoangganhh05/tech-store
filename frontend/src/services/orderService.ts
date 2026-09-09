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

export type OrderStatusHistory = {
  status: string
  changedAt: string
}

export type OrderDetail = {
  id: number
  orderNumber: string
  status: string
  paymentMethod: PaymentMethod
  paymentStatus: string
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
  }
  items: Array<{
    productName: string
    sku: string
    variantLabel: string
    unitPrice: number
    quantity: number
    subtotal: number
  }>
  statusHistory: OrderStatusHistory[]
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

export async function getOrderDetail(orderId: number): Promise<OrderDetail> {
  const response = await httpClient.get<{ data: OrderDetail }>(`/orders/${orderId}`)
  return response.data.data
}
