import { createContext } from "react";

export type WishlistContextType = {
  isAuthenticated: boolean;
  favoriteIds: ReadonlySet<number>;
  loadingIds: ReadonlySet<number>;
  loading: boolean;
  error: string | null;
  refreshWishlist: () => Promise<void>;
  toggleFavorite: (productId: number) => Promise<boolean>;
  clearError: () => void;
};

export const defaultWishlistContext: WishlistContextType = {
  isAuthenticated: false,
  favoriteIds: new Set<number>(),
  loadingIds: new Set<number>(),
  loading: false,
  error: null,
  refreshWishlist: async () => {},
  toggleFavorite: async () => false,
  clearError: () => {},
};

export const WishlistContext = createContext<WishlistContextType | undefined>(
  undefined,
);
