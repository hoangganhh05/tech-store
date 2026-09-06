import { httpClient } from "./httpClient";

export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE";
export type VariantStatus = "ACTIVE" | "INACTIVE";

export type Product = {
  id: number;
  name: string;
  description?: string | null;
  brandId: number;
  brandName: string;
  categoryId: number;
  categoryName: string;
  status: ProductStatus;
  createdAt: string;
  updatedAt: string;
};

export type ProductCreatePayload = {
  name: string;
  description?: string;
  brandId: number;
  categoryId: number;
  status?: ProductStatus;
};

export type ProductUpdatePayload = {
  name: string;
  description?: string;
  brandId: number;
  categoryId: number;
  status?: ProductStatus;
};

export type ProductVariant = {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  color?: string | null;
  storage?: string | null;
  price: number;
  originalPrice?: number | null;
  stockQuantity: number;
  status: VariantStatus;
  createdAt: string;
  updatedAt: string;
};

export type ProductVariantPayload = {
  sku: string;
  color?: string;
  storage?: string;
  price: number;
  originalPrice?: number;
  stockQuantity?: number;
  status?: VariantStatus;
};

export type ProductImage = {
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

export type ProductImageUpdatePayload = {
  variantId?: number | null;
  displayOrder?: number;
};

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export async function getAdminProducts(): Promise<Product[]> {
  const response =
    await httpClient.get<ApiResponse<Product[]>>("/admin/products");
  return response.data.data;
}

export async function getAdminProductById(id: number): Promise<Product> {
  const response = await httpClient.get<ApiResponse<Product>>(
    `/admin/products/${id}`,
  );
  return response.data.data;
}

export async function createAdminProduct(
  payload: ProductCreatePayload,
): Promise<Product> {
  const response = await httpClient.post<ApiResponse<Product>>(
    "/admin/products",
    payload,
  );
  return response.data.data;
}

export async function updateAdminProduct(
  id: number,
  payload: ProductUpdatePayload,
): Promise<Product> {
  const response = await httpClient.put<ApiResponse<Product>>(
    `/admin/products/${id}`,
    payload,
  );
  return response.data.data;
}

export async function getProductVariants(
  productId: number,
): Promise<ProductVariant[]> {
  const response = await httpClient.get<ApiResponse<ProductVariant[]>>(
    `/admin/products/${productId}/variants`,
  );
  return response.data.data;
}

export async function getProductVariantById(
  productId: number,
  variantId: number,
): Promise<ProductVariant> {
  const response = await httpClient.get<ApiResponse<ProductVariant>>(
    `/admin/products/${productId}/variants/${variantId}`,
  );
  return response.data.data;
}

export async function createProductVariant(
  productId: number,
  payload: ProductVariantPayload,
): Promise<ProductVariant> {
  const response = await httpClient.post<ApiResponse<ProductVariant>>(
    `/admin/products/${productId}/variants`,
    payload,
  );
  return response.data.data;
}

export async function updateProductVariant(
  productId: number,
  variantId: number,
  payload: ProductVariantPayload,
): Promise<ProductVariant> {
  const response = await httpClient.put<ApiResponse<ProductVariant>>(
    `/admin/products/${productId}/variants/${variantId}`,
    payload,
  );
  return response.data.data;
}

export async function deleteProductVariant(
  productId: number,
  variantId: number,
): Promise<void> {
  await httpClient.delete<ApiResponse<void>>(
    `/admin/products/${productId}/variants/${variantId}`,
  );
}

export async function getProductImages(
  productId: number,
): Promise<ProductImage[]> {
  const response = await httpClient.get<ApiResponse<ProductImage[]>>(
    `/admin/products/${productId}/images`,
  );
  return response.data.data;
}

export async function uploadProductImage(
  productId: number,
  file: File,
  variantId?: number | null,
  isPrimary?: boolean,
): Promise<ProductImage> {
  const formData = new FormData();
  formData.append("file", file);
  if (variantId !== undefined && variantId !== null) {
    formData.append("variantId", String(variantId));
  }
  if (isPrimary !== undefined && isPrimary !== null) {
    formData.append("isPrimary", String(isPrimary));
  }

  const response = await httpClient.post<ApiResponse<ProductImage>>(
    `/admin/products/${productId}/images`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    },
  );
  return response.data.data;
}

export async function setPrimaryProductImage(
  productId: number,
  imageId: number,
): Promise<ProductImage> {
  const response = await httpClient.put<ApiResponse<ProductImage>>(
    `/admin/products/${productId}/images/${imageId}/primary`,
  );
  return response.data.data;
}

export async function updateProductImage(
  productId: number,
  imageId: number,
  payload: ProductImageUpdatePayload,
): Promise<ProductImage> {
  const response = await httpClient.put<ApiResponse<ProductImage>>(
    `/admin/products/${productId}/images/${imageId}`,
    payload,
  );
  return response.data.data;
}

export async function deleteProductImage(
  productId: number,
  imageId: number,
): Promise<void> {
  await httpClient.delete<ApiResponse<void>>(
    `/admin/products/${productId}/images/${imageId}`,
  );
}
