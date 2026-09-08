import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import MockAdapter from 'axios-mock-adapter'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { PaymentMethodStep } from '../modules/checkout/PaymentMethodStep'
import { httpClient } from '../services/httpClient'
import type { PaymentOption } from '../services/checkoutService'

const options: PaymentOption[] = [
  { paymentMethod: 'COD', label: 'Thanh toán khi nhận hàng (COD)', instructions: 'Trả tiền khi nhận hàng.' },
  { paymentMethod: 'BANK_TRANSFER', label: 'Chuyển khoản (giả lập)', instructions: 'Số tài khoản DEMO-001. Không chuyển tiền thật.' },
  { paymentMethod: 'ONLINE', label: 'Online (giả lập)', instructions: 'Cổng thanh toán giả lập, không thu tiền.' },
]
let mock: MockAdapter
afterEach(() => mock.restore())

describe('US-08.2 payment selection', () => {
  it.each(options)('loads instructions and confirms $paymentMethod through the API', async (option) => {
    mock = new MockAdapter(httpClient)
    mock.onGet('/checkout/payment-methods').reply(200, { data: options })
    mock.onPost('/checkout/payment-method', { paymentMethod: option.paymentMethod }).reply(200, { data: option })
    const onContinue = vi.fn()
    render(<PaymentMethodStep selected={null} onBack={vi.fn()} onContinue={onContinue} />)
    expect(screen.getByText('Đang tải phương thức thanh toán...')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tiếp tục xem lại đơn hàng' })).toBeDisabled()
    fireEvent.click(await screen.findByRole('radio', { name: option.label }))
    expect(screen.getByText(option.instructions)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tiếp tục xem lại đơn hàng' }))
    await waitFor(() => expect(onContinue).toHaveBeenCalledWith(option))
    expect(mock.history.post).toHaveLength(1)
  })

  it('retries a failed list request', async () => {
    mock = new MockAdapter(httpClient)
    mock.onGet('/checkout/payment-methods').replyOnce(500)
    mock.onGet('/checkout/payment-methods').reply(200, { data: options })
    render(<PaymentMethodStep selected={null} onBack={vi.fn()} onContinue={vi.fn()} />)
    fireEvent.click(await screen.findByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByRole('radio', { name: options[0].label })).toBeInTheDocument()
  })

  it('keeps the selection on error, blocks duplicate submission and allows retry', async () => {
    mock = new MockAdapter(httpClient, { delayResponse: 30 })
    mock.onGet('/checkout/payment-methods').reply(200, { data: options })
    mock.onPost('/checkout/payment-method').replyOnce(500)
    mock.onPost('/checkout/payment-method').reply(200, { data: options[1] })
    const onContinue = vi.fn()
    render(<PaymentMethodStep selected={options[1]} onBack={vi.fn()} onContinue={onContinue} />)
    expect(await screen.findByRole('radio', { name: options[1].label })).toBeChecked()
    fireEvent.click(screen.getByRole('button', { name: 'Tiếp tục xem lại đơn hàng' }))
    expect(screen.getByRole('button', { name: 'Đang xác nhận...' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Quay lại chọn địa chỉ' })).toBeDisabled()
    expect(await screen.findByText('Không thể xác nhận phương thức thanh toán. Vui lòng thử lại.')).toBeInTheDocument()
    expect(onContinue).not.toHaveBeenCalled()
    expect(screen.getByRole('radio', { name: options[1].label })).toBeChecked()
    fireEvent.click(screen.getByRole('button', { name: 'Tiếp tục xem lại đơn hàng' }))
    await waitFor(() => expect(onContinue).toHaveBeenCalledWith(options[1]))
  })

  it('switches exclusively between methods and supports going back', async () => {
    mock = new MockAdapter(httpClient)
    mock.onGet('/checkout/payment-methods').reply(200, { data: options })
    const onBack = vi.fn()
    render(<PaymentMethodStep selected={options[0]} onBack={onBack} onContinue={vi.fn()} />)
    fireEvent.click(await screen.findByRole('radio', { name: options[2].label }))
    expect(screen.getByRole('radio', { name: options[0].label })).not.toBeChecked()
    expect(screen.queryByText(options[0].instructions)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Quay lại chọn địa chỉ' }))
    expect(onBack).toHaveBeenCalledOnce()
  })
})
