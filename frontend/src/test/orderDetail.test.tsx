import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { OrderDetailPage } from '../modules/orders/OrderDetailPage'
import { getOrderDetail, type OrderDetail } from '../services/orderService'

vi.mock('../services/orderService', () => ({ getOrderDetail: vi.fn() }))

const mockedGetOrderDetail = vi.mocked(getOrderDetail)

const order: OrderDetail = {
  id: 12, orderNumber: 'TS-DETAIL-12', status: 'SHIPPING', paymentMethod: 'BANK_TRANSFER', paymentStatus: 'PAID',
  subtotal: 1000000, discountAmount: 0, shippingFee: 30000, totalAmount: 1030000, placedAt: '2026-09-09T10:00:00Z',
  shippingAddress: { recipientName: 'Nguyễn Văn A', recipientPhone: '0900000000', line1: '1 Duy Tân', ward: 'Dịch Vọng', district: 'Cầu Giấy', province: 'Hà Nội' },
  items: [{ productName: 'Điện thoại', sku: 'PHONE-BLACK-128', variantLabel: 'Đen / 128GB', unitPrice: 500000, quantity: 2, subtotal: 1000000 }],
  statusHistory: [
    { status: 'PENDING', changedAt: '2026-09-09T10:00:00Z' },
    { status: 'CONFIRMED', changedAt: '2026-09-09T11:00:00Z' },
    { status: 'SHIPPING', changedAt: '2026-09-09T12:00:00Z' },
  ],
}

function renderPage(path = '/account/orders/12') {
  return render(<ThemeProvider theme={appTheme}><MemoryRouter initialEntries={[path]}><Routes><Route path="/account/orders/:id" element={<OrderDetailPage />} /></Routes></MemoryRouter></ThemeProvider>)
}

describe('US-09.2: order detail page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetOrderDetail.mockResolvedValue(order)
  })

  it('displays order products, shipping address, payment and visual status steps', async () => {
    renderPage()

    expect(await screen.findByText('TS-DETAIL-12')).toBeInTheDocument()
    expect(screen.getByText('Điện thoại')).toBeInTheDocument()
    expect(screen.getByText('Đen / 128GB')).toBeInTheDocument()
    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument()
    expect(screen.getByText('Chuyển khoản ngân hàng')).toBeInTheDocument()
    expect(screen.getByText('Đã thanh toán')).toBeInTheDocument()
    expect(screen.getByText('Chờ xác nhận')).toBeInTheDocument()
    expect(screen.getAllByText('Đang giao')).toHaveLength(2)
    expect(screen.getByText('Hoàn thành')).toBeInTheDocument()
    expect(mockedGetOrderDetail).toHaveBeenCalledWith(12)
  })

  it('shows a retryable error when detail loading fails', async () => {
    mockedGetOrderDetail.mockRejectedValueOnce(Object.assign(new Error('request failed'), {
      isAxiosError: true, response: { data: { message: 'Không thể kết nối máy chủ' } },
    }))
    renderPage()

    expect(await screen.findByText('Không thể kết nối máy chủ')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    await waitFor(() => expect(mockedGetOrderDetail).toHaveBeenCalledTimes(2))
    expect(await screen.findByText('TS-DETAIL-12')).toBeInTheDocument()
  })
})
