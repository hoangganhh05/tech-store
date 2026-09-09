import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { appTheme } from '../configs/theme'
import { AdminNotificationBell } from '../modules/admin/components/AdminNotificationBell'
import {
  getAdminNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  type AdminNotificationListResponse,
} from '../services/notificationService'

vi.mock('../services/notificationService', () => ({
  getAdminNotifications: vi.fn(),
  markNotificationAsRead: vi.fn(),
  markAllNotificationsAsRead: vi.fn(),
}))

const mockedGetAdminNotifications = vi.mocked(getAdminNotifications)
const mockedMarkNotificationAsRead = vi.mocked(markNotificationAsRead)
const mockedMarkAllNotificationsAsRead = vi.mocked(markAllNotificationsAsRead)

const sampleData: AdminNotificationListResponse = {
  items: [
    {
      id: 1,
      title: 'Đơn hàng mới #TS-TEST-001',
      message: 'Khách hàng Nguyễn Văn An đã đặt đơn hàng trị giá 500000 VND.',
      type: 'ORDER_CREATED',
      orderId: 101,
      orderNumber: 'TS-TEST-001',
      isRead: false,
      createdAt: '2026-09-09T10:00:00Z',
      readAt: null,
    },
    {
      id: 2,
      title: 'Đơn hàng mới #TS-TEST-002',
      message: 'Khách hàng Trần Thị B đã đặt đơn hàng trị giá 300000 VND.',
      type: 'ORDER_CREATED',
      orderId: 102,
      orderNumber: 'TS-TEST-002',
      isRead: true,
      createdAt: '2026-09-09T09:30:00Z',
      readAt: '2026-09-09T09:45:00Z',
    },
  ],
  unreadCount: 1,
  page: 0,
  size: 10,
  totalElements: 2,
  totalPages: 1,
}

function renderBell() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route path="/admin" element={<AdminNotificationBell />} />
          <Route path="/admin/orders/:id" element={<div data-testid="order-detail-page">Chi tiết đơn</div>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('US-10.2: Admin in-app notification bell', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('hiển thị badge số lượng thông báo chưa đọc', async () => {
    mockedGetAdminNotifications.mockResolvedValueOnce(sampleData)
    renderBell()

    expect(await screen.findByText('1')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thông báo quản trị' })).toBeInTheDocument()
  })

  it('mở danh sách thông báo khi click vào chuông', async () => {
    mockedGetAdminNotifications.mockResolvedValue(sampleData)
    renderBell()

    const bellBtn = await screen.findByRole('button', { name: 'Thông báo quản trị' })
    fireEvent.click(bellBtn)

    expect(await screen.findByText(/Thông báo \(1\)/)).toBeInTheDocument()
    expect(screen.getByText('Đơn hàng mới #TS-TEST-001')).toBeInTheDocument()
    expect(screen.getByText('Đơn hàng mới #TS-TEST-002')).toBeInTheDocument()
  })

  it('click vào thông báo chưa đọc: gọi API đánh dấu đã đọc và điều hướng tới chi tiết đơn hàng', async () => {
    mockedGetAdminNotifications.mockResolvedValue(sampleData)
    mockedMarkNotificationAsRead.mockResolvedValueOnce({
      ...sampleData.items[0],
      isRead: true,
      readAt: '2026-09-09T10:05:00Z',
    })

    renderBell()

    const bellBtn = await screen.findByRole('button', { name: 'Thông báo quản trị' })
    fireEvent.click(bellBtn)

    const item = await screen.findByText('Đơn hàng mới #TS-TEST-001')
    fireEvent.click(item)

    await waitFor(() => {
      expect(mockedMarkNotificationAsRead).toHaveBeenCalledWith(1)
    })

    expect(await screen.findByTestId('order-detail-page')).toBeInTheDocument()
  })

  it('click Đọc tất cả: gọi API đánh dấu tất cả và reset badge về 0', async () => {
    mockedGetAdminNotifications.mockResolvedValue(sampleData)
    mockedMarkAllNotificationsAsRead.mockResolvedValueOnce(1)

    renderBell()

    const bellBtn = await screen.findByRole('button', { name: 'Thông báo quản trị' })
    fireEvent.click(bellBtn)

    const markAllBtn = await screen.findByRole('button', { name: 'Đọc tất cả' })
    fireEvent.click(markAllBtn)

    await waitFor(() => {
      expect(mockedMarkAllNotificationsAsRead).toHaveBeenCalled()
    })

    // Sau khi đánh dấu tất cả, nút Đọc tất cả biến mất (vì unreadCount = 0)
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Đọc tất cả' })).not.toBeInTheDocument()
    })
  })
})
