import { useCallback, useEffect, useState, type ReactNode } from "react";
import { isAxiosError } from "axios";
import { useAuth } from "../../hooks/useAuth";
import {
  addToWishlist,
  getWishlist,
  removeFromWishlist,
} from "../../services/wishlistService";
import { WishlistContext } from "./WishlistStore";

function getErrorMessage(error: unknown, fallback: string): string {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || fallback;
  }
  return error instanceof Error ? error.message : fallback;
}

export function WishlistProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const isAuthenticated = Boolean(user?.roles?.includes("CUSTOMER"));
  const [favoriteIds, setFavoriteIds] = useState<Set<number>>(new Set());
  const [loadingIds, setLoadingIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshWishlist = useCallback(async () => {
    if (!isAuthenticated) {
      setFavoriteIds(new Set());
      setError(null);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await getWishlist(0, 100);
      setFavoriteIds(new Set(response.items.map((product) => product.id)));
    } catch (requestError: unknown) {
      setError(getErrorMessage(requestError, "Không thể tải danh sách yêu thích."));
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    void refreshWishlist();
  }, [refreshWishlist]);

  const toggleFavorite = useCallback(
    async (productId: number): Promise<boolean> => {
      if (!isAuthenticated) {
        throw new Error("Vui lòng đăng nhập để sử dụng danh sách yêu thích.");
      }
      if (loadingIds.has(productId)) {
        return favoriteIds.has(productId);
      }

      setLoadingIds((current) => new Set(current).add(productId));
      setError(null);
      const currentlyFavorite = favoriteIds.has(productId);
      try {
        if (currentlyFavorite) {
          await removeFromWishlist(productId);
          setFavoriteIds((current) => {
            const next = new Set(current);
            next.delete(productId);
            return next;
          });
          return false;
        }

        await addToWishlist(productId);
        setFavoriteIds((current) => new Set(current).add(productId));
        return true;
      } catch (requestError: unknown) {
        const message = getErrorMessage(requestError, "Không thể cập nhật danh sách yêu thích.");
        setError(message);
        throw new Error(message, { cause: requestError });
      } finally {
        setLoadingIds((current) => {
          const next = new Set(current);
          next.delete(productId);
          return next;
        });
      }
    },
    [favoriteIds, isAuthenticated, loadingIds],
  );

  return (
    <WishlistContext.Provider
      value={{
        isAuthenticated,
        favoriteIds,
        loadingIds,
        loading,
        error,
        refreshWishlist,
        toggleFavorite,
        clearError: () => setError(null),
      }}
    >
      {children}
    </WishlistContext.Provider>
  );
}
