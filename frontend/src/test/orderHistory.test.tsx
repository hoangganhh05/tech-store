import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { OrderHistoryPage } from '../modules/orders/OrderHistoryPage'
import { getMyOrders, type OrderHistoryPage as OrderHistoryPageResponse } from '../services/orderService'

vi.mock('../services/orderService', () => ({
  getMyOrders: vi.fn(),
}))

const mockedGetMyOrders = vi.mocked(getMyOrders)

const response: OrderHistoryPageResponse = {
  items: [
    {
      id: 3,
      orderNumber: 'TS-NEWEST',
      placedAt: '2026-09-09T10:00:00Z',
      totalAmount: 1030000,
      status: 'CONFIRMED',
    },
    {
      id: 2,
      orderNumber: 'TS-OLDER',
      placedAt: '2026-09-08T10:00:00Z',
      totalAmount: 500000,
      status: 'PENDING',
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
      <MemoryRouter>
        <OrderHistoryPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('US-09.1: order history page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetMyOrders.mockResolvedValue(response)
  })

  it('loads and displays order number, date, amount, and status', async () => {
    renderPage()

    expect(await screen.findByText('TS-NEWEST')).toBeInTheDocument()
    expect(screen.getByText('TS-OLDER')).toBeInTheDocument()
    expect(screen.getByText('Đã xác nhận')).toBeInTheDocument()
    expect(screen.getByText('Chờ xác nhận')).toBeInTheDocument()
    expect(screen.getByText(/1.030.000/)).toBeInTheDocument()
    expect(mockedGetMyOrders).toHaveBeenCalledWith({ page: 0, size: 10, status: undefined })
  })

  it('resets pagination and reloads when the status filter changes', async () => {
    renderPage()
    await screen.findByText('TS-NEWEST')

    fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Lọc trạng thái' }))
    fireEvent.click(await screen.findByRole('option', { name: 'Hoàn thành' }))

    await waitFor(() => expect(mockedGetMyOrders).toHaveBeenLastCalledWith({
      page: 0,
      size: 10,
      status: 'COMPLETED',
    }))
  })

  it('shows a retryable error when loading fails', async () => {
    mockedGetMyOrders.mockRejectedValueOnce(Object.assign(new Error('request failed'), {
      isAxiosError: true,
      response: { data: { message: 'Không thể kết nối máy chủ' } },
    }))
    renderPage()

    expect(await screen.findByText('Không thể kết nối máy chủ')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument()
  })
})
