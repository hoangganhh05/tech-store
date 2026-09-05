import { httpClient } from './httpClient'
import { httpClient } from "./httpClient";

export type Category = {
  id: number
  name: string
  description?: string | null
  parentId?: number | null
  parentName?: string | null
  imageUrl?: string | null
  createdAt: string
  updatedAt: string
}
  id: number;
  name: string;
  description?: string | null;
  parentId?: number | null;
  parentName?: string | null;
  imageUrl?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CategoryTree = {
  id: number
  name: string
  description?: string | null
  parentId?: number | null
  imageUrl?: string | null
  children: CategoryTree[]
}
  id: number;
  name: string;
  description?: string | null;
  parentId?: number | null;
  imageUrl?: string | null;
  children: CategoryTree[];
};

export type CategoryPayload = {
  name: string
  description?: string
  parentId?: number | null
  imageUrl?: string
}
  name: string;
  description?: string;
  parentId?: number | null;
  imageUrl?: string;
};

type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export async function getAdminCategories(): Promise<Category[]> {
  const response = await httpClient.get<ApiResponse<Category[]>>('/admin/categories')
  return response.data.data
  const response =
    await httpClient.get<ApiResponse<Category[]>>("/admin/categories");
  return response.data.data;
}

export async function getAdminCategoryTree(): Promise<CategoryTree[]> {
  const response = await httpClient.get<ApiResponse<CategoryTree[]>>('/admin/categories/tree')
  return response.data.data
  const response = await httpClient.get<ApiResponse<CategoryTree[]>>(
    "/admin/categories/tree",
  );
  return response.data.data;
}

export async function getAdminCategoryById(id: number): Promise<Category> {
  const response = await httpClient.get<ApiResponse<Category>>(/admin/categories/)
  return response.data.data
  const response = await httpClient.get<ApiResponse<Category>>(
    `/admin/categories/${id}`,
  );
  return response.data.data;
}

export async function createAdminCategory(payload: CategoryPayload): Promise<Category> {
  const response = await httpClient.post<ApiResponse<Category>>('/admin/categories', payload)
  return response.data.data
export async function createAdminCategory(
  payload: CategoryPayload,
): Promise<Category> {
  const response = await httpClient.post<ApiResponse<Category>>(
    "/admin/categories",
    payload,
  );
  return response.data.data;
}

export async function updateAdminCategory(id: number, payload: CategoryPayload): Promise<Category> {
  const response = await httpClient.put<ApiResponse<Category>>(/admin/categories/, payload)
  return response.data.data
export async function updateAdminCategory(
  id: number,
  payload: CategoryPayload,
): Promise<Category> {
  const response = await httpClient.put<ApiResponse<Category>>(
    `/admin/categories/${id}`,
    payload,
  );
  return response.data.data;
}

export async function deleteAdminCategory(id: number): Promise<void> {
  await httpClient.delete<ApiResponse<void>>(/admin/categories/)
  await httpClient.delete<ApiResponse<void>>(`/admin/categories/${id}`);
}