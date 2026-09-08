import { httpClient } from './httpClient'
import type { PaymentMethod } from './checkoutService'

export type PlacedOrder = { id: number; orderNumber: string; status: string; totalAmount: number; placedAt: string }

export async function placeOrder(addressId: number, paymentMethod: PaymentMethod): Promise<PlacedOrder> {
  const response = await httpClient.post<{ data: PlacedOrder }>('/orders', { addressId, paymentMethod })
  return response.data.data
}
