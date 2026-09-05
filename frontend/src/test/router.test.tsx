import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { ThemeProvider } from '@mui/material'
import { appTheme } from '../configs/theme'
import { StorefrontLayout } from '../layouts/StorefrontLayout'
import { AuthProvider } from '../modules/auth/AuthContext'
import { HomePage } from '../modules/home/HomePage'
import { ProductListPage } from '../modules/products/ProductListPage'
import { NotFoundPage } from '../modules/not-found/NotFoundPage'

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
  it('renders the storefront home route', () => {
    renderRoute('/')
    expect(screen.getByRole('heading', { name: /thiết bị công nghệ/i })).toBeInTheDocument()
  })

  it('renders the product list route', () => {
    renderRoute('/products')
    expect(screen.getByRole('heading', { name: 'Sản phẩm' })).toBeInTheDocument()
  })

  it('renders a friendly not-found route', () => {
    renderRoute('/khong-ton-tai')
    expect(screen.getByRole('heading', { name: 'Không tìm thấy trang' })).toBeInTheDocument()
  })
})
