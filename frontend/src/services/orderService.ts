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

export async function placeOrder(addressId: number, paymentMethod: PaymentMethod, voucherCode?: string): Promise<PlacedOrder> {
  const response = await httpClient.post<{ data: PlacedOrder }>('/orders', {
    addressId,
    paymentMethod,
    ...(voucherCode?.trim() ? { voucherCode: voucherCode.trim() } : {}),
  })
  return response.data.data
}
