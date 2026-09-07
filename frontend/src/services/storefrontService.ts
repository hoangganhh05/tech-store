import { httpClient } from "./httpClient";
import type { Category } from "./categoryService";
import type { Brand } from "./brandService";

export type ProductFilterParams = {
  categoryId?: number | null;
  brandIds?: number[];
  priceMin?: number | null;
  priceMax?: number | null;
  sortBy?: string | null;
  sortDir?: "asc" | "desc" | null;
};

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

export async function getStorefrontProducts(
  filtersOrCategoryId?: ProductFilterParams | number | null,
): Promise<StorefrontProduct[]> {
  const params: Record<string, unknown> = {};
  if (typeof filtersOrCategoryId === "number") {
    params.categoryId = filtersOrCategoryId;
  } else if (filtersOrCategoryId && typeof filtersOrCategoryId === "object") {
    if (filtersOrCategoryId.categoryId) {
      params.categoryId = filtersOrCategoryId.categoryId;
    }
    if (
      filtersOrCategoryId.brandIds &&
      filtersOrCategoryId.brandIds.length > 0
    ) {
      params.brandIds = filtersOrCategoryId.brandIds.join(",");
    }
    if (filtersOrCategoryId.priceMin != null) {
      params.priceMin = filtersOrCategoryId.priceMin;
    }
    if (filtersOrCategoryId.priceMax != null) {
      params.priceMax = filtersOrCategoryId.priceMax;
    }
    if (filtersOrCategoryId.sortBy) {
      params.sortBy = filtersOrCategoryId.sortBy;
    }
    if (filtersOrCategoryId.sortDir) {
      params.sortDir = filtersOrCategoryId.sortDir;
    }
  }

  const response = await httpClient.get<ApiResponse<StorefrontProduct[]>>(
    "/products",
    { params },
  );
  return response.data.data;
}

export async function getStorefrontCategories(): Promise<Category[]> {
  const response = await httpClient.get<ApiResponse<Category[]>>(
    "/storefront/categories",
  );
  return response.data.data;
}

export async function getStorefrontBrands(): Promise<Brand[]> {
  const response =
    await httpClient.get<ApiResponse<Brand[]>>("/storefront/brands");
  return response.data.data;
}

export async function searchStorefrontProducts(
  keyword: string,
): Promise<StorefrontProduct[]> {
  const response = await httpClient.get<ApiResponse<StorefrontProduct[]>>(
    "/products/search",
    {
      params: { q: keyword },
    },
  );
  return response.data.data;
}
