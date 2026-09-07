import { createContext } from "react";
import type { Cart } from "../../services/cartService";

export interface CartContextType {
  cart: Cart | null;
  cartCount: number;
  loading: boolean;
  addToCart: (variantId: number, quantity: number) => Promise<Cart>;
  updateQuantity: (itemId: number, quantity: number) => Promise<Cart>;
  removeCartItem: (itemId: number) => Promise<Cart>;
  refreshCart: () => Promise<void>;
}

export const defaultCartContext: CartContextType = {
  cart: null,
  cartCount: 0,
  loading: false,
  addToCart: async () => ({
    id: null,
    totalItems: 0,
    subtotal: 0,
    shippingFee: 0,
    discountAmount: 0,
    total: 0,
    items: [],
  }),
  updateQuantity: async () => ({
    id: null,
    totalItems: 0,
    subtotal: 0,
    shippingFee: 0,
    discountAmount: 0,
    total: 0,
    items: [],
  }),
  removeCartItem: async () => ({
    id: null,
    totalItems: 0,
    subtotal: 0,
    shippingFee: 0,
    discountAmount: 0,
    total: 0,
    items: [],
  }),
  refreshCart: async () => {},
};

export const CartContext = createContext<CartContextType | undefined>(
  undefined,
);
