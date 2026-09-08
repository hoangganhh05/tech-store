import { httpClient } from './httpClient'
import type { Cart } from './cartService'
import type { Address } from './userService'

export type PaymentMethod = 'COD' | 'BANK_TRANSFER' | 'ONLINE'
export type PaymentOption = {
  paymentMethod: PaymentMethod
  label: string
  instructions: string
}

export async function getPaymentMethods(): Promise<PaymentOption[]> {
  const response = await httpClient.get<{ data: PaymentOption[] }>('/checkout/payment-methods')
  return response.data.data
}

export async function selectPaymentMethod(paymentMethod: PaymentMethod): Promise<PaymentOption> {
  const response = await httpClient.post<{ data: PaymentOption }>('/checkout/payment-method', { paymentMethod })
  return response.data.data
}

export type CheckoutReview = {
  cart: Cart
  shippingAddress: Address
  paymentMethod: PaymentOption
  readyToPlaceOrder: boolean
}

export async function getCheckoutReview(addressId: number, paymentMethod: PaymentMethod): Promise<CheckoutReview> {
  const response = await httpClient.post<{ data: CheckoutReview }>('/checkout/review', { addressId, paymentMethod })
  return response.data.data
}
