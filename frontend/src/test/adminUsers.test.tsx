import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AuthProvider } from '../modules/auth/AuthContext'
import { AdminUsersPage } from '../modules/admin/AdminUsersPage'
import { getAdminUsers, updateAdminUserStatus } from '../services/adminUserService'
import type { AuthenticatedUser } from '../services/authService'
import { tokenStorage } from '../utils/tokenStorage'

vi.mock('../services/adminUserService', () => ({
  getAdminUsers: vi.fn(),
  updateAdminUserStatus: vi.fn(),
}))

const mockedGetAdminUsers = vi.mocked(getAdminUsers)
const mockedUpdateAdminUserStatus = vi.mocked(updateAdminUserStatus)

const currentAdmin: AuthenticatedUser = {
  id: 1,
  email: 'admin@example.com',
  fullName: 'Alice Admin',
  phone: '0911111111',
  status: 'ACTIVE',
  roles: ['ADMIN'],
  emailVerified: true,
  createdAt: '2026-09-01T00:00:00Z',
}

const customerUser: AuthenticatedUser = {
  id: 2,
  email: 'bob@example.com',
  fullName: 'Bob Customer',
  phone: '0922222222',
  status: 'ACTIVE',
  roles: ['CUSTOMER'],
  emailVerified: true,
  createdAt: '2026-09-02T00:00:00Z',
}

const lockedUser: AuthenticatedUser = {
  id: 3,
  email: 'charlie@example.com',
  fullName: 'Charlie Locked',
  phone: '0933333333',
  status: 'LOCKED',
  roles: ['CUSTOMER'],
  emailVerified: false,
  createdAt: '2026-09-03T00:00:00Z',
}

function renderAdminUsersPage() {
  tokenStorage.setTokens('mock-access', 'mock-refresh')
  window.localStorage.setItem('techstore.authUser', JSON.stringify(currentAdmin))

  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider>
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      </AuthProvider>
    </ThemeProvider>,
  )
}

describe('AdminUsersPage', () => {
  beforeEach(() => {
    mockedGetAdminUsers.mockResolvedValue({
      items: [currentAdmin, customerUser, lockedUser],
      page: 0,
      size: 10,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    })
  })

  afterEach(() => {
    mockedGetAdminUsers.mockReset()
    mockedUpdateAdminUserStatus.mockReset()
    tokenStorage.clear()
    window.localStorage.removeItem('techstore.authUser')
  })

  it('renders user list from API with badges and roles', async () => {
    renderAdminUsersPage()

    expect(screen.getByRole('heading', { name: /danh sách người dùng/i })).toBeInTheDocument()

    expect(await screen.findByText('Alice Admin')).toBeInTheDocument()
    expect(screen.getByText('Bob Customer')).toBeInTheDocument()
    expect(screen.getByText('Charlie Locked')).toBeInTheDocument()

    expect(screen.getByText('Quản trị viên')).toBeInTheDocument()
    expect(screen.getAllByText('Khách hàng')).toHaveLength(2)

    expect(screen.getAllByText('Hoạt động')).toHaveLength(2)
    expect(screen.getByText('Đã khoá')).toBeInTheDocument()
  })

  it('searches users when keyword is submitted', async () => {
    renderAdminUsersPage()
    await screen.findByText('Alice Admin')

    const searchInput = screen.getByPlaceholderText('Tìm theo họ tên hoặc email...')
    fireEvent.change(searchInput, { target: { value: 'bob' } })
    fireEvent.click(screen.getByRole('button', { name: /tìm kiếm/i }))

    await waitFor(() => {
      expect(mockedGetAdminUsers).toHaveBeenCalledWith({
        keyword: 'bob',
        page: 0,
        size: 10,
      })
    })
  })

  it('disables lock button for current authenticated admin', async () => {
    renderAdminUsersPage()
    await screen.findByText('Alice Admin')

    // Find all lock buttons
    const lockButtons = screen.getAllByRole('button', { name: /^khoá$/i })
    // The first user is Alice Admin (id 1 == currentAdmin.id), lock button should be disabled
    expect(lockButtons[0]).toBeDisabled()
    // The second user is Bob Customer (id 2), lock button should not be disabled
    expect(lockButtons[1]).not.toBeDisabled()
  })

  it('locks customer account after confirmation in dialog', async () => {
    mockedUpdateAdminUserStatus.mockResolvedValue({
      ...customerUser,
      status: 'LOCKED',
    })

    renderAdminUsersPage()
    await screen.findByText('Bob Customer')

    // Click lock on Bob Customer
    const lockButtons = screen.getAllByRole('button', { name: /^khoá$/i })
    fireEvent.click(lockButtons[1])

    // Dialog should be open
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Khoá tài khoản người dùng?')).toBeInTheDocument()
    expect(screen.getByText(/Bạn có chắc chắn muốn khoá tài khoản "bob@example.com"/i)).toBeInTheDocument()

    // Confirm lock
    const confirmButton = screen.getByRole('button', { name: 'Khoá tài khoản' })
    fireEvent.click(confirmButton)

    await waitFor(() => {
      expect(mockedUpdateAdminUserStatus).toHaveBeenCalledWith(2, 'LOCKED')
    })
    expect(await screen.findByText(/Khoá tài khoản "bob@example.com" thành công/i)).toBeInTheDocument()
  })

  it('unlocks a locked user account after confirmation', async () => {
    mockedUpdateAdminUserStatus.mockResolvedValue({
      ...lockedUser,
      status: 'ACTIVE',
    })

    renderAdminUsersPage()
    await screen.findByText('Charlie Locked')

    // Click unlock on Charlie Locked
    const unlockButton = screen.getByRole('button', { name: /mở khoá/i })
    fireEvent.click(unlockButton)

    // Dialog should be open
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Mở khoá tài khoản người dùng?')).toBeInTheDocument()

    // Confirm unlock
    const confirmButton = screen.getByRole('button', { name: 'Mở khoá' })
    fireEvent.click(confirmButton)

    await waitFor(() => {
      expect(mockedUpdateAdminUserStatus).toHaveBeenCalledWith(3, 'ACTIVE')
    })
    expect(await screen.findByText(/Mở khoá tài khoản "charlie@example.com" thành công/i)).toBeInTheDocument()
  })
})