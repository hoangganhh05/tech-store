import { RouterProvider } from "react-router-dom";
import { AuthProvider } from "./modules/auth/AuthContext";
import { CartProvider } from "./modules/cart/CartContext";
import { WishlistProvider } from "./modules/wishlist/WishlistContext";
import { appRouter } from "./routers/appRouter";

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <WishlistProvider>
          <RouterProvider router={appRouter} />
        </WishlistProvider>
      </CartProvider>
    </AuthProvider>
  );
}
