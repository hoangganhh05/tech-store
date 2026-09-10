import { createBrowserRouter } from "react-router-dom";
import { StorefrontLayout } from "../layouts/StorefrontLayout";
import { AdminLayout } from "../layouts/AdminLayout";
import { HomePage } from "../modules/home/HomePage";
import { ProductListPage } from "../modules/products/ProductListPage";
import { ProductDetailPage } from "../modules/products/ProductDetailPage";
import { CartPage } from "../modules/cart/CartPage";
import { CheckoutPage } from "../modules/checkout/CheckoutPage";
import { OrderConfirmationPage } from "../modules/checkout/OrderConfirmationPage";
import { LoginPage } from "../modules/auth/LoginPage";
import { RegisterPage } from "../modules/auth/RegisterPage";
import { ForgotPasswordPage } from "../modules/auth/ForgotPasswordPage";
import { ResetPasswordPage } from "../modules/auth/ResetPasswordPage";
import { ProfilePage } from "../modules/profile/ProfilePage";
import { AddressesPage } from "../modules/profile/AddressesPage";
import { OrderHistoryPage } from "../modules/orders/OrderHistoryPage";
import { OrderDetailPage } from "../modules/orders/OrderDetailPage";
import { WishlistPage } from "../modules/wishlist/WishlistPage";
import { ForbiddenPage } from "../modules/auth/ForbiddenPage";
import { RequireAuth } from "../modules/auth/RequireAuth";
import { AdminDashboardPage } from "../modules/admin/AdminDashboardPage";
import { AdminLoginPage } from "../modules/admin/AdminLoginPage";
import { AdminUsersPage } from "../modules/admin/AdminUsersPage";
import { AdminCategoriesPage } from "../modules/admin/AdminCategoriesPage";
import { AdminBrandsPage } from "../modules/admin/AdminBrandsPage";
import { AdminProductsPage } from "../modules/admin/AdminProductsPage";
import { AdminInventoryPage } from "../modules/admin/AdminInventoryPage";
import { AdminOrdersPage } from "../modules/admin/AdminOrdersPage";
import { AdminOrderDetailPage } from "../modules/admin/AdminOrderDetailPage";
import { AdminReviewsPage } from "../modules/admin/AdminReviewsPage";
import { NotFoundPage } from "../modules/not-found/NotFoundPage";
import { ROUTES } from "../constants/routes";

export const appRouter = createBrowserRouter([
  {
    element: <StorefrontLayout />,
    children: [
      { path: ROUTES.home, element: <HomePage /> },
      { path: ROUTES.products, element: <ProductListPage /> },
      { path: ROUTES.productDetail, element: <ProductDetailPage /> },
      { path: ROUTES.cart, element: <CartPage /> },
      {
        path: ROUTES.checkout,
        element: (
          <RequireAuth>
            <CheckoutPage />
          </RequireAuth>
        ),
      },
      { path: ROUTES.orderConfirmation, element: <RequireAuth><OrderConfirmationPage /></RequireAuth> },
      { path: ROUTES.login, element: <LoginPage /> },
      { path: ROUTES.register, element: <RegisterPage /> },
      { path: ROUTES.forgotPassword, element: <ForgotPasswordPage /> },
      { path: ROUTES.resetPassword, element: <ResetPasswordPage /> },
      {
        path: ROUTES.profile,
        element: (
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        ),
      },
      {
        path: ROUTES.addresses,
        element: (
          <RequireAuth>
            <AddressesPage />
          </RequireAuth>
        ),
      },
      {
        path: ROUTES.orders,
        element: (
          <RequireAuth>
            <OrderHistoryPage />
          </RequireAuth>
        ),
      },
      {
        path: ROUTES.orderDetail,
        element: (
          <RequireAuth>
            <OrderDetailPage />
          </RequireAuth>
        ),
      },
      {
        path: ROUTES.wishlist,
        element: (
          <RequireAuth requiredRoles={["CUSTOMER"]}>
            <WishlistPage />
          </RequireAuth>
        ),
      },
      { path: ROUTES.forbidden, element: <ForbiddenPage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
  {
    path: ROUTES.adminLogin,
    element: <AdminLoginPage />,
  },
  {
    path: ROUTES.admin,
    element: (
      <RequireAuth requiredRoles={["ADMIN"]}>
        <AdminLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: "users", element: <AdminUsersPage /> },
      { path: "categories", element: <AdminCategoriesPage /> },
      { path: "brands", element: <AdminBrandsPage /> },
      { path: "products", element: <AdminProductsPage /> },
      { path: "inventory", element: <AdminInventoryPage /> },
      { path: "orders", element: <AdminOrdersPage /> },
      { path: "orders/:id", element: <AdminOrderDetailPage /> },
      { path: "reviews", element: <AdminReviewsPage /> },
    ],
  },
]);
