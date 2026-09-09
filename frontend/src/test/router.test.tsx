import { render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { ThemeProvider } from '@mui/material'
import { appTheme } from '../configs/theme'
import { StorefrontLayout } from '../layouts/StorefrontLayout'
import { AuthProvider } from '../modules/auth/AuthContext'
import { HomePage } from '../modules/home/HomePage'
import { ProductListPage } from '../modules/products/ProductListPage'
import { NotFoundPage } from '../modules/not-found/NotFoundPage'
import { vi } from 'vitest'

vi.mock('../services/storefrontService', () => ({
  getStorefrontHomeData: vi.fn().mockResolvedValue({
    featuredProducts: [], newArrivals: [], onSaleProducts: [], featuredCategories: [],
  }),
  getStorefrontProducts: vi.fn().mockResolvedValue([]),
  getStorefrontCategories: vi.fn().mockResolvedValue([]),
  getStorefrontBrands: vi.fn().mockResolvedValue([]),
  searchStorefrontProducts: vi.fn().mockResolvedValue([]),
  getFeaturedProducts: vi.fn().mockResolvedValue([]),
  getNewArrivals: vi.fn().mockResolvedValue([]),
  getOnSaleProducts: vi.fn().mockResolvedValue([]),
}))

function renderRoute(path: string) {
  const router = createMemoryRouter([
    {
      element: <StorefrontLayout />,
      children: [
        { path: '/', element: <HomePage /> },
        { path: '/products', element: <ProductListPage /> },
        { path: '*', element: <NotFoundPage /> },
      ],
    },
  ], { initialEntries: [path] })
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider><RouterProvider router={router} /></AuthProvider>
    </ThemeProvider>,
  )
}

describe('application routing', () => {
  it('renders the storefront home route', async () => {
    renderRoute('/')
    expect(screen.getByRole('heading', { name: 'Đăng Tùng Mobile' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Chưa có sản phẩm nổi bật nào.')).toBeInTheDocument())
  })

  it('renders the product list route', async () => {
    renderRoute('/products')
    expect(screen.getByRole('heading', { name: 'Sản phẩm' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Tìm thấy 0 sản phẩm')).toBeInTheDocument())
  })

  it('renders a friendly not-found route', () => {
    renderRoute('/khong-ton-tai')
    expect(screen.getByRole('heading', { name: 'Không tìm thấy trang' })).toBeInTheDocument()
  })
})
