import { httpClient } from './httpClient'
import type { Cart } from './cartService'
import type { Address } from './userService'

export type PaymentMethod = 'COD' | 'BANK_TRANSFER' | 'ONLINE'
export type PaymentOption = {
  paymentMethod: PaymentMethod
  label: string
  instructions: string
}

export type VoucherApplication = {
  code: string
  name: string
  discountType: 'PERCENT' | 'FIXED'
  discountAmount: number
  subtotal: number
  shippingFee: number
  total: number
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
  voucher?: VoucherApplication | null
}

export async function getCheckoutReview(addressId: number, paymentMethod: PaymentMethod, voucherCode?: string): Promise<CheckoutReview> {
  const response = await httpClient.post<{ data: CheckoutReview }>('/checkout/review', {
    addressId,
    paymentMethod,
    ...(voucherCode?.trim() ? { voucherCode: voucherCode.trim() } : {}),
  })
  return response.data.data
}

export async function applyVoucher(code: string): Promise<VoucherApplication> {
  const response = await httpClient.post<{ data: VoucherApplication }>('/checkout/voucher', { code: code.trim() })
  return response.data.data
}
