import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../modules/auth/AuthContext'
import { RequireAuth } from '../modules/auth/RequireAuth'
import { tokenStorage } from '../utils/tokenStorage'

const customerUser = {
  id: 1,
  email: 'customer@example.com',
  fullName: 'Nguyen Van A',
  phone: '0901234567',
  status: 'ACTIVE',
  roles: ['CUSTOMER'],
  emailVerified: false,
  createdAt: '2026-09-04T08:00:00Z',
}

const adminUser = {
  id: 2,
  email: 'admin@example.com',
  fullName: 'Quản Trị Viên',
  phone: '0909999999',
  status: 'ACTIVE',
  roles: ['ADMIN'],
  emailVerified: true,
  createdAt: '2026-09-04T08:00:00Z',
}

function renderRoutes(initialPath: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/login" element={<h1>Đăng nhập</h1>} />
          <Route path="/admin/login" element={<h1>Đăng nhập quản trị</h1>} />
          <Route path="/forbidden" element={<h1>Truy cập bị từ chối</h1>} />
          <Route
            path="/checkout"
            element={
              <RequireAuth>
                <h1>Thanh toán riêng tư</h1>
              </RequireAuth>
            }
          />
          <Route
            path="/admin"
            element={
              <RequireAuth requiredRoles={['ADMIN']}>
                <h1>Khu vực quản trị</h1>
              </RequireAuth>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('protected routes with role authorization', () => {
  afterEach(() => {
    tokenStorage.clear()
    window.localStorage.removeItem('techstore.authUser')
  })

  it('redirects an anonymous visitor from checkout to the login page', async () => {
    renderRoutes('/checkout')

    expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Thanh toán riêng tư' })).not.toBeInTheDocument()
  })

  it('allows an authenticated user to access checkout', () => {
    tokenStorage.setTokens('access-token', 'refresh-token')
    window.localStorage.setItem('techstore.authUser', JSON.stringify(customerUser))

    renderRoutes('/checkout')

    expect(screen.getByRole('heading', { name: 'Thanh toán riêng tư' })).toBeInTheDocument()
  })

  it('redirects an anonymous visitor from admin to the admin login page', async () => {
    renderRoutes('/admin')

    expect(await screen.findByRole('heading', { name: 'Đăng nhập quản trị' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Khu vực quản trị' })).not.toBeInTheDocument()
  })

  it('redirects a customer user from admin to the forbidden page', async () => {
    tokenStorage.setTokens('access-token', 'refresh-token')
    window.localStorage.setItem('techstore.authUser', JSON.stringify(customerUser))

    renderRoutes('/admin')

    expect(await screen.findByRole('heading', { name: 'Truy cập bị từ chối' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Khu vực quản trị' })).not.toBeInTheDocument()
  })

  it('allows an admin user to access the admin area', () => {
    tokenStorage.setTokens('access-token', 'refresh-token')
    window.localStorage.setItem('techstore.authUser', JSON.stringify(adminUser))

    renderRoutes('/admin')

    expect(screen.getByRole('heading', { name: 'Khu vực quản trị' })).toBeInTheDocument()
  })
})