import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom'
import { AuthProvider } from '../modules/auth/AuthContext'
import { RequireAuth } from '../modules/auth/RequireAuth'
import { useAuth } from '../hooks/useAuth'
import { logoutAccount } from '../services/authService'
import { tokenStorage } from '../utils/tokenStorage'

vi.mock('../services/authService', () => ({ logoutAccount: vi.fn() }))

const mockedLogoutAccount = vi.mocked(logoutAccount)
const storedUser = {
  id: 1,
  email: 'customer@example.com',
  fullName: 'Nguyen Van A',
  phone: '0901234567',
  status: 'ACTIVE',
  roles: ['CUSTOMER'],
  emailVerified: false,
  createdAt: '2026-09-04T08:00:00Z',
}

function SignOutControl() {
  const { isAuthenticated, signOut } = useAuth()

  return (
    <>
      <output>{isAuthenticated ? 'authenticated' : 'anonymous'}</output>
      <button type="button" onClick={() => void signOut()}>Đăng xuất</button>
    </>
  )
}

function SignOutAndVisitCheckout() {
  const navigate = useNavigate()
  const { signOut } = useAuth()

  const handleLogout = async () => {
    await signOut()
    navigate('/checkout')
  }

  return <button type="button" onClick={() => void handleLogout()}>Đăng xuất và đến checkout</button>
}

function seedAuthenticatedSession() {
  tokenStorage.setTokens('access-token', 'refresh-token')
  window.localStorage.setItem('techstore.authUser', JSON.stringify(storedUser))
}

function renderSignOutControl() {
  return render(
    <AuthProvider>
      <SignOutControl />
    </AuthProvider>,
  )
}

describe('account logout', () => {
  afterEach(() => {
    mockedLogoutAccount.mockReset()
    tokenStorage.clear()
    window.localStorage.removeItem('techstore.authUser')
  })

  it('sends the refresh token to the logout endpoint and clears the complete client session', async () => {
    mockedLogoutAccount.mockResolvedValue(undefined)
    seedAuthenticatedSession()
    renderSignOutControl()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }))

    await waitFor(() => expect(mockedLogoutAccount).toHaveBeenCalledWith({ refreshToken: 'refresh-token' }))
    await waitFor(() => expect(screen.getByText('anonymous')).toBeInTheDocument())
    expect(tokenStorage.getAccessToken()).toBeNull()
    expect(tokenStorage.getRefreshToken()).toBeNull()
    expect(window.localStorage.getItem('techstore.authUser')).toBeNull()
  })

  it('still clears the complete client session when the server logout request fails', async () => {
    mockedLogoutAccount.mockRejectedValue(new Error('Network unavailable'))
    seedAuthenticatedSession()
    renderSignOutControl()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }))

    await waitFor(() => expect(screen.getByText('anonymous')).toBeInTheDocument())
    expect(tokenStorage.getAccessToken()).toBeNull()
    expect(tokenStorage.getRefreshToken()).toBeNull()
    expect(window.localStorage.getItem('techstore.authUser')).toBeNull()
  })

  it('redirects to login when a user tries a protected route after logout', async () => {
    mockedLogoutAccount.mockResolvedValue(undefined)
    seedAuthenticatedSession()
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route path="/" element={<SignOutAndVisitCheckout />} />
            <Route path="/login" element={<h1>Đăng nhập</h1>} />
            <Route path="/checkout" element={<RequireAuth><h1>Thanh toán riêng tư</h1></RequireAuth>} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất và đến checkout' }))

    expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Thanh toán riêng tư' })).not.toBeInTheDocument()
  })
})
