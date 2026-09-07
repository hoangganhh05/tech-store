import { httpClient } from "./httpClient";
import type { Category } from "./categoryService";

export type StorefrontProduct = {
  id: number;
  name: string;
  description?: string | null;
  brandId?: number | null;
  brandName?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  thumbnailUrl?: string | null;
  minPrice: number;
  maxPrice: number;
  originalPrice?: number | null;
  discountPercent: number;
  totalStock: number;
  hasStock: boolean;
  salesCount: number;
  rating: number;
  createdAt: string;
};

export type StorefrontHomeData = {
  featuredProducts: StorefrontProduct[];
  newArrivals: StorefrontProduct[];
  onSaleProducts: StorefrontProduct[];
  featuredCategories: Category[];
};

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export async function getStorefrontHomeData(
  limit = 8,
): Promise<StorefrontHomeData> {
  const response = await httpClient.get<ApiResponse<StorefrontHomeData>>(
    "/storefront/home",
    {
      params: { limit },
    },
  );
  return response.data.data;
}

export async function getFeaturedProducts(
  limit = 8,
): Promise<StorefrontProduct[]> {
  const response = await httpClient.get<ApiResponse<StorefrontProduct[]>>(
    "/products/featured",
    {
      params: { limit },
    },
  );
  return response.data.data;
}

export async function getNewArrivals(limit = 8): Promise<StorefrontProduct[]> {
  const response = await httpClient.get<ApiResponse<StorefrontProduct[]>>(
    "/products/new-arrivals",
    {
      params: { limit },
    },
  );
  return response.data.data;
}

export async function getOnSaleProducts(
  limit = 8,
): Promise<StorefrontProduct[]> {
  const response = await httpClient.get<ApiResponse<StorefrontProduct[]>>(
    "/products/on-sale",
    {
      params: { limit },
    },
  );
  return response.data.data;
}
