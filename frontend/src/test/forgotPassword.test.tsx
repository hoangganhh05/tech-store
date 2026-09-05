import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { ForgotPasswordPage } from '../modules/auth/ForgotPasswordPage'
import { requestPasswordReset } from '../services/authService'

vi.mock('../services/authService', () => ({
  requestPasswordReset: vi.fn(),
}))

const mockedRequestPasswordReset = vi.mocked(requestPasswordReset)
const genericSuccessMessage = 'Nếu email này thuộc về một tài khoản, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu. Vui lòng kiểm tra hộp thư của bạn.'

function renderForgotPasswordPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('forgot-password form', () => {
  afterEach(() => {
    mockedRequestPasswordReset.mockReset()
  })

  it('validates an email before sending a request', () => {
    renderForgotPasswordPage()

    fireEvent.click(screen.getByRole('button', { name: 'Gửi hướng dẫn' }))
    expect(screen.getByText('Vui lòng nhập email.')).toBeInTheDocument()
    expect(mockedRequestPasswordReset).not.toHaveBeenCalled()

    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'not-an-email' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi hướng dẫn' }))
    expect(screen.getByText('Email không đúng định dạng.')).toBeInTheDocument()
    expect(mockedRequestPasswordReset).not.toHaveBeenCalled()
  })

  it('submits a trimmed email and always displays the generic completion message', async () => {
    mockedRequestPasswordReset.mockResolvedValue(undefined)
    renderForgotPasswordPage()

    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: '  customer@example.com  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi hướng dẫn' }))

    await waitFor(() => expect(mockedRequestPasswordReset).toHaveBeenCalledWith({ email: 'customer@example.com' }))
    expect(screen.getByText(genericSuccessMessage)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Gửi hướng dẫn' })).toBeDisabled()
  })

  it('does not expose an API failure response that could reveal account information', async () => {
    mockedRequestPasswordReset.mockRejectedValue(new Error('Email does not exist'))
    renderForgotPasswordPage()

    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'unknown@example.com' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi hướng dẫn' }))

    expect(await screen.findByText('Không thể gửi yêu cầu lúc này. Vui lòng thử lại sau.')).toBeInTheDocument()
    expect(screen.queryByText('Email does not exist')).not.toBeInTheDocument()
  })
})
