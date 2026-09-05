import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { ResetPasswordPage } from '../modules/auth/ResetPasswordPage'
import { resetPassword } from '../services/authService'

vi.mock('../services/authService', () => ({
  resetPassword: vi.fn(),
}))

const mockedResetPassword = vi.mocked(resetPassword)
const invalidLinkMessage = 'Liên kết đặt lại mật khẩu không hợp lệ, đã được sử dụng hoặc đã hết hạn. Vui lòng yêu cầu một liên kết mới.'

function renderResetPasswordPage(path = '/reset-password?token=reset-token') {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/login" element={<h1>Trang đăng nhập</h1>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

function fillPasswords(password: string, confirmPassword = password) {
  const passwordInputs = document.querySelectorAll<HTMLInputElement>('input[type="password"]')
  if (passwordInputs.length !== 2) throw new Error('Password inputs were not rendered')
  fireEvent.change(passwordInputs[0], { target: { value: password } })
  fireEvent.change(passwordInputs[1], { target: { value: confirmPassword } })
}

describe('reset-password form', () => {
  afterEach(() => {
    mockedResetPassword.mockReset()
  })

  it('shows a safe invalid-link view when no token is present', () => {
    renderResetPasswordPage('/reset-password')

    expect(screen.getByText(invalidLinkMessage)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Yêu cầu liên kết mới' })).toHaveAttribute('href', '/forgot-password')
    expect(mockedResetPassword).not.toHaveBeenCalled()
  })

  it('validates matching passwords before calling the API', () => {
    renderResetPasswordPage()

    fillPasswords('strong-password', 'different-password')
    fireEvent.click(screen.getByRole('button', { name: 'Đặt lại mật khẩu' }))

    expect(screen.getByText('Mật khẩu xác nhận không khớp.')).toBeInTheDocument()
    expect(mockedResetPassword).not.toHaveBeenCalled()
  })

  it('uses the URL token, resets the password, and returns the user to login', async () => {
    mockedResetPassword.mockResolvedValue(undefined)
    renderResetPasswordPage('/reset-password?token=single-use-token')

    fillPasswords('new-strong-password')
    fireEvent.click(screen.getByRole('button', { name: 'Đặt lại mật khẩu' }))

    await waitFor(() => expect(mockedResetPassword).toHaveBeenCalledWith({
      token: 'single-use-token',
      password: 'new-strong-password',
      confirmPassword: 'new-strong-password',
    }))
    expect(await screen.findByRole('heading', { name: 'Trang đăng nhập' })).toBeInTheDocument()
  })

  it('does not display a server error when the reset token is invalid or expired', async () => {
    mockedResetPassword.mockRejectedValue(new Error('Expired reset token: reset-token'))
    renderResetPasswordPage()

    fillPasswords('new-strong-password')
    fireEvent.click(screen.getByRole('button', { name: 'Đặt lại mật khẩu' }))

    expect(await screen.findByText(invalidLinkMessage)).toBeInTheDocument()
    expect(screen.queryByText('Expired reset token: reset-token')).not.toBeInTheDocument()
  })

  it('keeps the safe token wording but explains when the new password matches the current password', async () => {
    const passwordReuseError = Object.assign(new Error('Password must be different'), {
      isAxiosError: true,
      response: {
        data: {
          code: 'PASSWORD_MUST_BE_DIFFERENT',
          message: 'Mật khẩu mới phải khác mật khẩu hiện tại.',
        },
      },
    })
    mockedResetPassword.mockRejectedValue(passwordReuseError)
    renderResetPasswordPage()

    fillPasswords('current-strong-password')
    fireEvent.click(screen.getByRole('button', { name: 'Đặt lại mật khẩu' }))

    expect(await screen.findByText('Mật khẩu mới phải khác mật khẩu hiện tại.')).toBeInTheDocument()
    expect(screen.queryByText(invalidLinkMessage)).not.toBeInTheDocument()
  })
})
