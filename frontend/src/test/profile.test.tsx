import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AuthProvider } from '../modules/auth/AuthContext'
import { ProfilePage } from '../modules/profile/ProfilePage'
import { getMyProfile, updateMyProfile } from '../services/userService'
import { tokenStorage } from '../utils/tokenStorage'

vi.mock('../services/userService', () => ({
  getMyProfile: vi.fn(),
  updateMyProfile: vi.fn(),
  changeMyPassword: vi.fn(),
}))
vi.mock('../services/authService', () => ({ logoutAccount: vi.fn() }))

const mockedGetMyProfile = vi.mocked(getMyProfile)
const mockedUpdateMyProfile = vi.mocked(updateMyProfile)
const profile = {
  id: 1,
  email: 'customer@example.com',
  fullName: 'Nguyen Van A',
  phone: '0901234567',
  dateOfBirth: '2000-05-20',
  updatedAt: '2026-09-05T08:00:00Z',
}

function renderProfilePage() {
  tokenStorage.setTokens('access-token', 'refresh-token')
  window.localStorage.setItem('techstore.authUser', JSON.stringify({
    ...profile, status: 'ACTIVE', roles: ['CUSTOMER'], emailVerified: false, createdAt: '2026-09-04T08:00:00Z',
  }))
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider><MemoryRouter><ProfilePage /></MemoryRouter></AuthProvider>
    </ThemeProvider>,
  )
}

describe('personal profile page', () => {
  afterEach(() => {
    vi.resetAllMocks()
    tokenStorage.clear()
    window.localStorage.removeItem('techstore.authUser')
  })

  it('loads personal information and keeps email read-only', async () => {
    mockedGetMyProfile.mockResolvedValue(profile)
    renderProfilePage()

    expect(await screen.findByDisplayValue('customer@example.com')).toBeDisabled()
    expect(screen.getByDisplayValue('Nguyen Van A')).toBeInTheDocument()
    expect(screen.getByDisplayValue('0901234567')).toBeInTheDocument()
    expect(screen.getByText('Email không thể thay đổi trực tiếp.')).toBeInTheDocument()
  })

  it('updates editable fields and synchronizes the stored user', async () => {
    mockedGetMyProfile.mockResolvedValue(profile)
    mockedUpdateMyProfile.mockResolvedValue({ ...profile, fullName: 'Nguyen Van B', phone: '0987654321' })
    renderProfilePage()

    fireEvent.change(await screen.findByLabelText(/họ tên/i), { target: { value: ' Nguyen Van B ' } })
    fireEvent.change(screen.getByLabelText(/số điện thoại/i), { target: { value: '0987654321' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    await waitFor(() => expect(mockedUpdateMyProfile).toHaveBeenCalledWith({
      fullName: 'Nguyen Van B', phone: '0987654321', dateOfBirth: '2000-05-20',
    }))
    expect(await screen.findByText('Cập nhật thông tin cá nhân thành công.')).toBeInTheDocument()
    expect(window.localStorage.getItem('techstore.authUser')).toContain('Nguyen Van B')
  })

  it('shows field errors before calling the API', async () => {
    mockedGetMyProfile.mockResolvedValue(profile)
    renderProfilePage()

    fireEvent.change(await screen.findByLabelText(/họ tên/i), { target: { value: '' } })
    fireEvent.change(screen.getByLabelText(/số điện thoại/i), { target: { value: 'abc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect(screen.getByText('Vui lòng nhập họ tên.')).toBeInTheDocument()
    expect(screen.getByText('Số điện thoại không đúng định dạng.')).toBeInTheDocument()
    expect(mockedUpdateMyProfile).not.toHaveBeenCalled()
  })
})
