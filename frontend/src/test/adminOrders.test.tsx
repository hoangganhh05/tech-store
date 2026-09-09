import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AdminOrdersPage } from '../modules/admin/AdminOrdersPage'
import { getAdminOrders, type PageResponse, type AdminOrderSummary } from '../services/adminOrderService'

vi.mock('../services/adminOrderService', () => ({ getAdminOrders: vi.fn() }))

const mockedGetAdminOrders = vi.mocked(getAdminOrders)

const response: PageResponse<AdminOrderSummary> = {
  items: [
    {
      id: 3,
      orderNumber: 'TS-ADMIN-NEW',
      customerName: 'Nguyễn Văn An',
      customerPhone: '0901000001',
      totalAmount: 1030000,
      status: 'COMPLETED',
      placedAt: '2026-09-09T10:00:00Z',
    },
    {
      id: 2,
      orderNumber: 'TS-ADMIN-PENDING',
      customerName: 'Trần Thị Bình',
      customerPhone: '0902000002',
      totalAmount: 500000,
      status: 'PENDING',
      placedAt: '2026-09-08T10:00:00Z',
    },
  ],
  page: 0,
  size: 10,
  totalElements: 12,
  totalPages: 2,
  first: true,
  last: false,
}

function renderPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter><AdminOrdersPage /></MemoryRouter>
    </ThemeProvider>,
  )
}

describe('US-09.4: admin order list page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetAdminOrders.mockResolvedValue(response)
  })

  it('renders paginated order summaries with customer, amount, status, and date', async () => {
    renderPage()

    expect(await screen.findByText('TS-ADMIN-NEW')).toBeInTheDocument()
    expect(screen.getByText('Nguyễn Văn An')).toBeInTheDocument()
    expect(screen.getByText('0902000002')).toBeInTheDocument()
    expect(screen.getByText('Hoàn thành')).toBeInTheDocument()
    expect(screen.getByText('Chờ xác nhận')).toBeInTheDocument()
    expect(screen.getByText(/1.030.000/)).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Xem chi tiết' })[0]).toHaveAttribute('href', '/admin/orders/3')
    expect(mockedGetAdminOrders).toHaveBeenCalledWith({
      search: '', status: '', fromDate: '', toDate: '', page: 0, size: 10,
    })
  })

  it('applies search, status, and date-range filters', async () => {
    renderPage()
    await screen.findByText('TS-ADMIN-NEW')

    fireEvent.change(screen.getByLabelText('Tìm kiếm'), { target: { value: 'TS-ADMIN' } })
    fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Trạng thái' }))
    fireEvent.click(await screen.findByRole('option', { name: 'Hoàn thành' }))
    fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2026-09-01' } })
    fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '2026-09-09' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lọc đơn hàng' }))

    await waitFor(() => expect(mockedGetAdminOrders).toHaveBeenLastCalledWith({
      search: 'TS-ADMIN', status: 'COMPLETED', fromDate: '2026-09-01', toDate: '2026-09-09', page: 0, size: 10,
    }))
  })

  it('shows an error and can retry loading', async () => {
    mockedGetAdminOrders.mockRejectedValueOnce(Object.assign(new Error('request failed'), {
      isAxiosError: true,
      response: { data: { message: 'Không thể kết nối máy chủ' } },
    }))
    renderPage()

    expect(await screen.findByText('Không thể kết nối máy chủ')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    await waitFor(() => expect(mockedGetAdminOrders).toHaveBeenCalledTimes(2))
    expect(await screen.findByText('TS-ADMIN-NEW')).toBeInTheDocument()
  })
})
