import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AuthProvider } from '../modules/auth/AuthContext'
import { LoginPage } from '../modules/auth/LoginPage'
import { loginAccount } from '../services/authService'
import { tokenStorage } from '../utils/tokenStorage'

vi.mock('../services/authService', () => ({
  loginAccount: vi.fn(),
  logoutAccount: vi.fn(),
}))

const mockedLoginAccount = vi.mocked(loginAccount)

function renderLoginPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider>
        <MemoryRouter><LoginPage /></MemoryRouter>
      </AuthProvider>
    </ThemeProvider>,
  )
}

function fillValidCredentials() {
  fireEvent.change(screen.getByRole('textbox', { name: /email/i }), { target: { value: ' Customer@Example.com ' } })
  const passwordInput = document.querySelector<HTMLInputElement>('input[type="password"]')
  if (!passwordInput) throw new Error('Password input was not rendered')
  fireEvent.change(passwordInput, { target: { value: 'strong-password' } })
}

describe('account login form', () => {
  afterEach(() => {
    mockedLoginAccount.mockReset()
    tokenStorage.clear()
    window.localStorage.removeItem('techstore.authUser')
  })

  it('shows field errors without calling the API for missing credentials', () => {
    renderLoginPage()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(screen.getByText('Vui lòng nhập email.')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập mật khẩu.')).toBeInTheDocument()
    expect(mockedLoginAccount).not.toHaveBeenCalled()
  })

  it('stores the token pair and authenticated user after a successful login', async () => {
    mockedLoginAccount.mockResolvedValue({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      accessTokenExpiresAt: '2026-09-04T08:15:00Z',
      refreshTokenExpiresAt: '2026-09-11T08:00:00Z',
      user: {
        id: 1,
        email: 'customer@example.com',
        fullName: 'Nguyen Van A',
        phone: '0901234567',
        status: 'ACTIVE',
        roles: ['CUSTOMER'],
        emailVerified: false,
        createdAt: '2026-09-04T08:00:00Z',
      },
    })
    renderLoginPage()
    fillValidCredentials()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    await waitFor(() => expect(mockedLoginAccount).toHaveBeenCalledWith({
      email: 'Customer@Example.com',
      password: 'strong-password',
    }))
    expect(tokenStorage.getAccessToken()).toBe('access-token')
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token')
    expect(window.localStorage.getItem('techstore.authUser')).toContain('customer@example.com')
  })

  it('shows the generic server error for incorrect credentials', async () => {
    mockedLoginAccount.mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'Email hoặc mật khẩu không đúng' } },
    })
    renderLoginPage()
    fillValidCredentials()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Email hoặc mật khẩu không đúng')).toBeInTheDocument()
    expect(tokenStorage.getAccessToken()).toBeNull()
  })

  it('shows the locked-account message returned by the API', async () => {
    mockedLoginAccount.mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.' } },
    })
    renderLoginPage()
    fillValidCredentials()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText(/tài khoản của bạn đã bị khóa/i)).toBeInTheDocument()
  })
})
