import { RouterProvider } from "react-router-dom";
import { AuthProvider } from "./modules/auth/AuthContext";
import { CartProvider } from "./modules/cart/CartContext";
import { appRouter } from "./routers/appRouter";

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <RouterProvider router={appRouter} />
      </CartProvider>
    </AuthProvider>
  );
}
