import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AuthProvider } from '../modules/auth/AuthContext'
import { ChangePasswordForm } from '../modules/profile/ChangePasswordForm'
import { changeMyPassword } from '../services/userService'
import { tokenStorage } from '../utils/tokenStorage'

vi.mock('../services/userService', () => ({ changeMyPassword: vi.fn() }))
vi.mock('../services/authService', () => ({ logoutAccount: vi.fn() }))

const mockedChangeMyPassword = vi.mocked(changeMyPassword)

function renderForm() {
  tokenStorage.setTokens('access-token', 'refresh-token')
  window.localStorage.setItem('techstore.authUser', JSON.stringify({
    id: 1,
    email: 'customer@example.com',
    fullName: 'Nguyen Van A',
    phone: '0901234567',
    status: 'ACTIVE',
    roles: ['CUSTOMER'],
    emailVerified: false,
    createdAt: '2026-09-04T08:00:00Z',
  }))
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider><MemoryRouter><ChangePasswordForm /></MemoryRouter></AuthProvider>
    </ThemeProvider>,
  )
}

function fillPasswords(currentPassword = 'old-password', newPassword = 'new-password', confirmation = newPassword) {
  fireEvent.change(screen.getByLabelText(/^Mật khẩu hiện tại/), { target: { value: currentPassword } })
  fireEvent.change(screen.getByLabelText(/^Mật khẩu mới/), { target: { value: newPassword } })
  fireEvent.change(screen.getByLabelText(/^Xác nhận mật khẩu mới/), { target: { value: confirmation } })
}

describe('change-password form', () => {
  afterEach(() => {
    mockedChangeMyPassword.mockReset()
    tokenStorage.clear()
    window.localStorage.removeItem('techstore.authUser')
  })

  it('validates required, strong, different, and matching passwords locally', () => {
    renderForm()
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect(screen.getByText('Vui lòng nhập mật khẩu hiện tại.')).toBeInTheDocument()
    expect(screen.getByText('Mật khẩu mới phải có từ 8 đến 72 ký tự.')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng xác nhận mật khẩu mới.')).toBeInTheDocument()
    expect(mockedChangeMyPassword).not.toHaveBeenCalled()
  })

  it('shows the current-password error returned by the API', async () => {
    mockedChangeMyPassword.mockRejectedValue({
      isAxiosError: true,
      response: { data: { code: 'INVALID_CURRENT_PASSWORD', message: 'Mật khẩu hiện tại không đúng' } },
    })
    renderForm()
    fillPasswords()
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect(await screen.findByText('Mật khẩu hiện tại không đúng')).toBeInTheDocument()
    expect(tokenStorage.getAccessToken()).toBe('access-token')
  })

  it('clears the old session after a successful password change', async () => {
    mockedChangeMyPassword.mockResolvedValue()
    renderForm()
    fillPasswords()
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    await waitFor(() => expect(mockedChangeMyPassword).toHaveBeenCalledWith({
      currentPassword: 'old-password',
      newPassword: 'new-password',
      confirmPassword: 'new-password',
    }))
    await waitFor(() => expect(tokenStorage.getAccessToken()).toBeNull())
    expect(window.localStorage.getItem('techstore.authUser')).toBeNull()
  })
})
