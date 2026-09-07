import { httpClient } from "./httpClient";
import type { Category } from "./categoryService";
import type { Brand } from "./brandService";

export type StorefrontProductPageResponse = {
  items: StorefrontProduct[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type ProductFilterParams = {
  categoryId?: number | null;
  brandIds?: number[];
  priceMin?: number | null;
  priceMax?: number | null;
  sortBy?: string | null;
  sortDir?: "asc" | "desc" | null;
  page?: number | null;
  size?: number | null;
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
): Promise<StorefrontProduct[] | StorefrontProductPageResponse> {
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
    if (filtersOrCategoryId.page != null) {
      params.page = filtersOrCategoryId.page;
    }
    if (filtersOrCategoryId.size != null) {
      params.size = filtersOrCategoryId.size;
    }
  }

  const response = await httpClient.get<
    ApiResponse<StorefrontProduct[] | StorefrontProductPageResponse>
  >("/products", { params });
  return response.data.data;
}

export async function getStorefrontPaginatedProducts(
  filters?: ProductFilterParams,
): Promise<StorefrontProductPageResponse> {
  const params: Record<string, unknown> = {};
  if (filters) {
    if (filters.categoryId) params.categoryId = filters.categoryId;
    if (filters.brandIds && filters.brandIds.length > 0)
      params.brandIds = filters.brandIds.join(",");
    if (filters.priceMin != null) params.priceMin = filters.priceMin;
    if (filters.priceMax != null) params.priceMax = filters.priceMax;
    if (filters.sortBy) params.sortBy = filters.sortBy;
    if (filters.sortDir) params.sortDir = filters.sortDir;
    params.page = filters.page != null ? filters.page : 0;
    params.size = filters.size != null ? filters.size : 12;
  } else {
    params.page = 0;
    params.size = 12;
  }

  const response = await httpClient.get<
    ApiResponse<StorefrontProductPageResponse | StorefrontProduct[]>
  >("/products", { params });
  const data = response.data.data;
  if (Array.isArray(data)) {
    return {
      items: data,
      page: 0,
      size: data.length,
      totalElements: data.length,
      totalPages: 1,
      first: true,
      last: true,
    };
  }
  return data;
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

export type ProductVariantDetail = {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  color: string;
  storage: string;
  price: number;
  originalPrice?: number | null;
  stockQuantity: number;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type ProductImageDetail = {
  id: number;
  productId: number;
  variantId?: number | null;
  variantSku?: string | null;
  variantColor?: string | null;
  imageUrl: string;
  isPrimary: boolean;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
};

export type ProductSpecificationDetail = {
  id: number;
  productId: number;
  specKey: string;
  specValue: string;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
};

export type StorefrontProductDetail = {
  id: number;
  name: string;
  description?: string | null;
  brandId?: number | null;
  brandName?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  status: string;
  minPrice: number;
  maxPrice: number;
  originalPrice?: number | null;
  discountPercent: number;
  totalStock: number;
  hasStock: boolean;
  salesCount: number;
  rating: number;
  variants: ProductVariantDetail[];
  availableColors?: string[];
  availableStorages?: string[];
  images: ProductImageDetail[];
  specifications: ProductSpecificationDetail[];
  createdAt: string;
  updatedAt: string;
};

export async function getStorefrontProductDetail(
  id: number | string,
): Promise<StorefrontProductDetail> {
  const response = await httpClient.get<ApiResponse<StorefrontProductDetail>>(
    `/products/${id}`,
  );
  return response.data.data;
}
