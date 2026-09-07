import { useContext } from "react";
import {
  CartContext,
  defaultCartContext,
  type CartContextType,
} from "../modules/cart/CartStore";

export function useCart(): CartContextType {
  const context = useContext(CartContext);
  return context ?? defaultCartContext;
}
