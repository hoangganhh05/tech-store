import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AuthProvider } from '../modules/auth/AuthContext'
import { AdminLoginPage } from '../modules/admin/AdminLoginPage'
import { loginAdminAccount } from '../services/authService'
import { tokenStorage } from '../utils/tokenStorage'

vi.mock('../services/authService', () => ({
  loginAdminAccount: vi.fn(),
  loginAccount: vi.fn(),
  logoutAccount: vi.fn(),
}))

const mockedLoginAdminAccount = vi.mocked(loginAdminAccount)

function renderAdminLoginPage(initialEntries = ['/admin/login']) {
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider>
        <MemoryRouter initialEntries={initialEntries}>
          <Routes>
            <Route path="/admin/login" element={<AdminLoginPage />} />
            <Route path="/admin" element={<h1>Bảng điều khiển Quản trị</h1>} />
            <Route path="/admin/products" element={<h1>Quản lý sản phẩm</h1>} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </ThemeProvider>,
  )
}

function fillCredentials(email = 'admin@example.com', password = 'AdminPassword123') {
  const emailInput = screen.getByRole('textbox', { name: /email/i })
  fireEvent.change(emailInput, { target: { value: email } })
  const passwordInput = document.querySelector<HTMLInputElement>('input[type="password"]')
  if (!passwordInput) throw new Error('Password input not found')
  fireEvent.change(passwordInput, { target: { value: password } })
}

describe('AdminLoginPage', () => {
  afterEach(() => {
    mockedLoginAdminAccount.mockReset()
    tokenStorage.clear()
    window.localStorage.removeItem('techstore.authUser')
  })

  it('renders the admin login portal heading and fields', () => {
    renderAdminLoginPage()

    expect(screen.getByRole('heading', { name: /đăng nhập quản trị/i })).toBeInTheDocument()
    expect(screen.getByText('Admin Portal')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /email quản trị viên/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /đăng nhập quản trị/i })).toBeInTheDocument()
  })

  it('validates required fields before calling the API', () => {
    renderAdminLoginPage()

    fireEvent.click(screen.getByRole('button', { name: /đăng nhập quản trị/i }))

    expect(screen.getByText('Vui lòng nhập email.')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập mật khẩu.')).toBeInTheDocument()
    expect(mockedLoginAdminAccount).not.toHaveBeenCalled()
  })

  it('authenticates admin successfully and redirects to /admin dashboard', async () => {
    mockedLoginAdminAccount.mockResolvedValue({
      accessToken: 'admin-access-token',
      refreshToken: 'admin-refresh-token',
      tokenType: 'Bearer',
      accessTokenExpiresAt: '2026-09-04T08:15:00Z',
      refreshTokenExpiresAt: '2026-09-11T08:00:00Z',
      user: {
        id: 99,
        email: 'admin@example.com',
        fullName: 'Quản Trị Viên',
        phone: '0912345678',
        status: 'ACTIVE',
        roles: ['ADMIN'],
        emailVerified: true,
        createdAt: '2026-09-01T00:00:00Z',
      },
    })

    renderAdminLoginPage()
    fillCredentials('admin@example.com', 'AdminPassword123')
    fireEvent.click(screen.getByRole('button', { name: /đăng nhập quản trị/i }))

    expect(await screen.findByRole('heading', { name: 'Bảng điều khiển Quản trị' })).toBeInTheDocument()
    expect(tokenStorage.getAccessToken()).toBe('admin-access-token')
    expect(tokenStorage.getRefreshToken()).toBe('admin-refresh-token')
    expect(window.localStorage.getItem('techstore.authUser')).toContain('admin@example.com')
  })

  it('redirects to the target admin page when state.from is provided', async () => {
    mockedLoginAdminAccount.mockResolvedValue({
      accessToken: 'admin-access-token',
      refreshToken: 'admin-refresh-token',
      tokenType: 'Bearer',
      accessTokenExpiresAt: '2026-09-04T08:15:00Z',
      refreshTokenExpiresAt: '2026-09-11T08:00:00Z',
      user: {
        id: 99,
        email: 'admin@example.com',
        fullName: 'Quản Trị Viên',
        phone: '0912345678',
        status: 'ACTIVE',
        roles: ['ADMIN'],
        emailVerified: true,
        createdAt: '2026-09-01T00:00:00Z',
      },
    })

    render(
      <ThemeProvider theme={appTheme}>
        <AuthProvider>
          <MemoryRouter
            initialEntries={[{ pathname: '/admin/login', state: { from: '/admin/products' } }]}
          >
            <Routes>
              <Route path="/admin/login" element={<AdminLoginPage />} />
              <Route path="/admin" element={<h1>Bảng điều khiển Quản trị</h1>} />
              <Route path="/admin/products" element={<h1>Quản lý sản phẩm</h1>} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </ThemeProvider>,
    )

    fillCredentials('admin@example.com', 'AdminPassword123')
    fireEvent.click(screen.getByRole('button', { name: /đăng nhập quản trị/i }))

    expect(await screen.findByRole('heading', { name: 'Quản lý sản phẩm' })).toBeInTheDocument()
  })

  it('displays error message when non-admin account receives 403 Forbidden', async () => {
    mockedLoginAdminAccount.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 403,
        data: {
          success: false,
          code: 'ACCESS_DENIED',
          message: 'Tài khoản không có quyền truy cập khu vực quản trị',
        },
      },
    })

    renderAdminLoginPage()
    fillCredentials('customer@example.com', 'CustomerPassword123')
    fireEvent.click(screen.getByRole('button', { name: /đăng nhập quản trị/i }))

    await waitFor(() => {
      expect(
        screen.getByText('Tài khoản không có quyền truy cập khu vực quản trị'),
      ).toBeInTheDocument()
    })
    expect(tokenStorage.getAccessToken()).toBeNull()
  })

  it('displays error message when login credentials are invalid', async () => {
    mockedLoginAdminAccount.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          success: false,
          code: 'INVALID_CREDENTIALS',
          message: 'Email hoặc mật khẩu không đúng',
        },
      },
    })

    renderAdminLoginPage()
    fillCredentials('admin@example.com', 'WrongPass')
    fireEvent.click(screen.getByRole('button', { name: /đăng nhập quản trị/i }))

    await waitFor(() => {
      expect(screen.getByText('Email hoặc mật khẩu không đúng')).toBeInTheDocument()
    })
  })
})