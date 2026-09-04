import { fireEvent, render, screen } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { RegisterPage } from '../modules/auth/RegisterPage'

function renderRegisterPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('account registration form', () => {
  it('shows a field error for every missing required value', () => {
    renderRegisterPage()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(screen.getByText('Vui lòng nhập họ tên.')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập email.')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập số điện thoại.')).toBeInTheDocument()
    expect(screen.getByText('Mật khẩu phải có ít nhất 8 ký tự.')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng xác nhận mật khẩu.')).toBeInTheDocument()
  })

  it('shows a mismatch error before sending the form', () => {
    renderRegisterPage()

    fireEvent.change(screen.getByRole('textbox', { name: /họ tên/i }), { target: { value: 'Nguyen Van A' } })
    fireEvent.change(screen.getByRole('textbox', { name: /email/i }), { target: { value: 'customer@example.com' } })
    fireEvent.change(screen.getByRole('textbox', { name: /số điện thoại/i }), { target: { value: '0901234567' } })
    const passwordInputs = document.querySelectorAll<HTMLInputElement>('input[type="password"]')
    fireEvent.change(passwordInputs[0], { target: { value: 'strong-password' } })
    fireEvent.change(passwordInputs[1], { target: { value: 'different-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(screen.getByText('Mật khẩu xác nhận không khớp.')).toBeInTheDocument()
  })
})
