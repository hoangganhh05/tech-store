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

export async function placeOrder(addressId: number, paymentMethod: PaymentMethod): Promise<PlacedOrder> {
  const response = await httpClient.post<{ data: PlacedOrder }>('/orders', { addressId, paymentMethod })
  return response.data.data
}
