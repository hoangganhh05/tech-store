import { httpClient } from "./httpClient";
import type { StorefrontProductPageResponse } from "./storefrontService";

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export type WishlistItem = {
  productId: number;
  favorite: boolean;
};

export type WishlistPage = StorefrontProductPageResponse;

export async function getWishlist(page = 0, size = 100): Promise<WishlistPage> {
  const response = await httpClient.get<ApiResponse<WishlistPage>>("/wishlist", {
    params: { page, size },
  });
  return response.data.data;
}

export async function addToWishlist(productId: number | string): Promise<WishlistItem> {
  const response = await httpClient.post<ApiResponse<WishlistItem>>(
    `/wishlist/${productId}`,
  );
  return response.data.data;
}

export async function removeFromWishlist(
  productId: number | string,
): Promise<WishlistItem> {
  const response = await httpClient.delete<ApiResponse<WishlistItem>>(
    `/wishlist/${productId}`,
  );
  return response.data.data;
}
