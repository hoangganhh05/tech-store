import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../modules/auth/AuthContext'
import { RequireAuth } from '../modules/auth/RequireAuth'
import { tokenStorage } from '../utils/tokenStorage'

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

function renderProtectedRoute(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<h1>Đăng nhập</h1>} />
          <Route path="/checkout" element={<RequireAuth><h1>Thanh toán riêng tư</h1></RequireAuth>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('protected routes', () => {
  afterEach(() => {
    tokenStorage.clear()
    window.localStorage.removeItem('techstore.authUser')
  })

  it('redirects an anonymous visitor from checkout to the login page', async () => {
    renderProtectedRoute('/checkout')

    expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Thanh toán riêng tư' })).not.toBeInTheDocument()
  })

  it('allows an authenticated user to access checkout', () => {
    tokenStorage.setTokens('access-token', 'refresh-token')
    window.localStorage.setItem('techstore.authUser', JSON.stringify(storedUser))

    renderProtectedRoute('/checkout')

    expect(screen.getByRole('heading', { name: 'Thanh toán riêng tư' })).toBeInTheDocument()
  })
})
