import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AdminOrderDetailPage } from '../modules/admin/AdminOrderDetailPage'
import {
  getAdminOrderDetail,
  updateAdminOrderStatus,
  type AdminOrderDetail,
} from '../services/adminOrderService'

vi.mock('../services/adminOrderService', () => ({
  getAdminOrderDetail: vi.fn(),
  updateAdminOrderStatus: vi.fn(),
}))

const mockedGetAdminOrderDetail = vi.mocked(getAdminOrderDetail)
const mockedUpdateAdminOrderStatus = vi.mocked(updateAdminOrderStatus)

const order: AdminOrderDetail = {
  id: 12,
  orderNumber: 'TS-ADMIN-DETAIL-12',
  customer: { id: 8, fullName: 'Nguyễn Văn An', email: 'an@example.com', phone: '0900000000' },
  status: 'SHIPPING',
  paymentMethod: 'BANK_TRANSFER',
  paymentStatus: 'PAID',
  cancellationReason: null,
  internalNote: 'Gọi khách trước khi giao hàng',
  subtotal: 1000000,
  discountAmount: 100000,
  shippingFee: 30000,
  totalAmount: 930000,
  placedAt: '2026-09-09T10:00:00Z',
  shippingAddress: {
    recipientName: 'Nguyễn Văn An', recipientPhone: '0900000000', line1: '1 Duy Tân', ward: 'Dịch Vọng', district: 'Cầu Giấy', province: 'Hà Nội',
  },
  items: [{ productName: 'Điện thoại', sku: 'PHONE-BLACK-128', variantLabel: 'Đen / 128GB', unitPrice: 500000, quantity: 2, subtotal: 1000000 }],
  statusHistory: [
    { status: 'PENDING', changedAt: '2026-09-09T10:00:00Z', changedBy: null },
    { status: 'CONFIRMED', changedAt: '2026-09-09T10:30:00Z', changedBy: null },
    { status: 'SHIPPING', changedAt: '2026-09-09T11:00:00Z', changedBy: null },
  ],
}

const completedOrder: AdminOrderDetail = {
  ...order,
  status: 'COMPLETED',
  statusHistory: [
    ...order.statusHistory,
    {
      status: 'COMPLETED',
      changedAt: '2026-09-09T12:00:00Z',
      changedBy: { id: 1, fullName: 'Quản trị viên' },
    },
  ],
}

function renderPage(path = '/admin/orders/12') {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={[path]}>
        <Routes><Route path="/admin/orders/:id" element={<AdminOrderDetailPage />} /></Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('US-09.5: admin order detail page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetAdminOrderDetail.mockResolvedValue(order)
  })

  it('displays customer, products, shipping, payment, status history, and internal note', async () => {
    renderPage()

    expect(await screen.findByText('TS-ADMIN-DETAIL-12')).toBeInTheDocument()
    expect(screen.getAllByText('Nguyễn Văn An')).toHaveLength(2)
    expect(screen.getByText('an@example.com')).toBeInTheDocument()
    expect(screen.getByText('Điện thoại')).toBeInTheDocument()
    expect(screen.getByText('Đen / 128GB')).toBeInTheDocument()
    expect(screen.getByText('1 Duy Tân, Dịch Vọng, Cầu Giấy, Hà Nội')).toBeInTheDocument()
    expect(screen.getByText('Chuyển khoản ngân hàng')).toBeInTheDocument()
    expect(screen.getByText('Đã thanh toán')).toBeInTheDocument()
    expect(screen.getByText('Gọi khách trước khi giao hàng')).toBeInTheDocument()
    expect(screen.getAllByText('Đang giao')).toHaveLength(2)
    expect(screen.getByText(/930.000/)).toBeInTheDocument()
    expect(mockedGetAdminOrderDetail).toHaveBeenCalledWith(12)
  })

  it('shows a retryable error when the detail request fails', async () => {
    mockedGetAdminOrderDetail.mockRejectedValueOnce(Object.assign(new Error('request failed'), {
      isAxiosError: true, response: { data: { message: 'Không thể kết nối máy chủ' } },
    }))
    renderPage()

    expect(await screen.findByText('Không thể kết nối máy chủ')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    await waitFor(() => expect(mockedGetAdminOrderDetail).toHaveBeenCalledTimes(2))
    expect(await screen.findByText('TS-ADMIN-DETAIL-12')).toBeInTheDocument()
  })

  it('updates the order through the next valid status action and refreshes its timeline', async () => {
    mockedUpdateAdminOrderStatus.mockResolvedValueOnce(completedOrder)
    renderPage()

    await screen.findByText('TS-ADMIN-DETAIL-12')
    fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Trạng thái mới' }))
    fireEvent.click(await screen.findByRole('option', { name: 'Hoàn thành đơn hàng' }))
    fireEvent.click(screen.getByRole('button', { name: 'Cập nhật trạng thái' }))

    await waitFor(() => expect(mockedUpdateAdminOrderStatus).toHaveBeenCalledWith(12, 'COMPLETED'))
    expect(await screen.findByText('Đã cập nhật trạng thái đơn hàng.')).toBeInTheDocument()
    expect(screen.getAllByText('Hoàn thành')).toHaveLength(2)
    expect(screen.getByText(/Cập nhật bởi Quản trị viên/)).toBeInTheDocument()
    expect(screen.getByText('Đơn hàng ở trạng thái cuối, không thể cập nhật thêm.')).toBeInTheDocument()
  })

  it('shows the API message when a status update fails', async () => {
    mockedUpdateAdminOrderStatus.mockRejectedValueOnce(Object.assign(new Error('request failed'), {
      isAxiosError: true, response: { data: { message: 'Không thể chuyển trạng thái đơn hàng' } },
    }))
    renderPage()

    await screen.findByText('TS-ADMIN-DETAIL-12')
    fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Trạng thái mới' }))
    fireEvent.click(await screen.findByRole('option', { name: 'Hoàn thành đơn hàng' }))
    fireEvent.click(screen.getByRole('button', { name: 'Cập nhật trạng thái' }))

    await waitFor(() => expect(mockedUpdateAdminOrderStatus).toHaveBeenCalledWith(12, 'COMPLETED'))
    expect(await screen.findByText('Không thể chuyển trạng thái đơn hàng')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cập nhật trạng thái' })).toBeEnabled()
  })

  it('handles an invalid route id without requesting the API', async () => {
    renderPage('/admin/orders/invalid')

    expect(await screen.findByText('Mã đơn hàng không hợp lệ.')).toBeInTheDocument()
    expect(mockedGetAdminOrderDetail).not.toHaveBeenCalled()
  })
})
