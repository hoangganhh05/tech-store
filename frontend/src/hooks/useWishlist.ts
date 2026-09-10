import { useContext } from "react";
import {
  defaultWishlistContext,
  WishlistContext,
  type WishlistContextType,
} from "../modules/wishlist/WishlistStore";

export function useWishlist(): WishlistContextType {
  return useContext(WishlistContext) ?? defaultWishlistContext;
}
