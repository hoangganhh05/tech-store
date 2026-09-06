import { httpClient } from "./httpClient";

export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE";

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
