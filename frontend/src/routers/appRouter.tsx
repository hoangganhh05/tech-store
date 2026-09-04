import { createBrowserRouter } from 'react-router-dom'
import { StorefrontLayout } from '../layouts/StorefrontLayout'
import { AdminLayout } from '../layouts/AdminLayout'
import { HomePage } from '../modules/home/HomePage'
import { ProductListPage } from '../modules/products/ProductListPage'
import { ProductDetailPage } from '../modules/products/ProductDetailPage'
import { CartPage } from '../modules/cart/CartPage'
import { CheckoutPage } from '../modules/checkout/CheckoutPage'
import { LoginPage } from '../modules/auth/LoginPage'
import { RegisterPage } from '../modules/auth/RegisterPage'
import { RequireAuth } from '../modules/auth/RequireAuth'
import { AdminDashboardPage } from '../modules/admin/AdminDashboardPage'
import { AdminProductsPage } from '../modules/admin/AdminProductsPage'
import { AdminOrdersPage } from '../modules/admin/AdminOrdersPage'
import { NotFoundPage } from '../modules/not-found/NotFoundPage'
import { ROUTES } from '../constants/routes'

export const appRouter = createBrowserRouter([
  {
    element: <StorefrontLayout />,
    children: [
      { path: ROUTES.home, element: <HomePage /> },
      { path: ROUTES.products, element: <ProductListPage /> },
      { path: ROUTES.productDetail, element: <ProductDetailPage /> },
      { path: ROUTES.cart, element: <CartPage /> },
      { path: ROUTES.checkout, element: <RequireAuth><CheckoutPage /></RequireAuth> },
      { path: ROUTES.login, element: <LoginPage /> },
      { path: ROUTES.register, element: <RegisterPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  {
    path: ROUTES.admin,
    element: <RequireAuth><AdminLayout /></RequireAuth>,
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: 'products', element: <AdminProductsPage /> },
      { path: 'orders', element: <AdminOrdersPage /> },
    ],
  },
])
