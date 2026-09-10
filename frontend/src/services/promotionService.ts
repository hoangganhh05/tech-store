import { httpClient } from "./httpClient";
import type { PageResponse } from "./voucherService";

type ApiResponse<T> = { success: boolean; code: string; message: string; data: T; timestamp: string };
export type PromotionTargetType = "PRODUCT" | "VARIANT" | "CATEGORY";
export type Promotion = {
  id: number; name: string; targetType: PromotionTargetType; productId?: number | null; productName?: string | null;
  variantId?: number | null; variantSku?: string | null; variantProductId?: number | null; categoryId?: number | null; categoryName?: string | null;
  discountPercent: number; startsAt: string; endsAt: string; active: boolean; createdAt: string; updatedAt: string;
};
export type PromotionPayload = {
  name: string; targetType: PromotionTargetType; productId?: number; variantId?: number; categoryId?: number;
  discountPercent: number; startsAt: string; endsAt: string; active: boolean;
};
export async function getAdminPromotions(page = 0, size = 10): Promise<PageResponse<Promotion>> {
  const response = await httpClient.get<ApiResponse<PageResponse<Promotion>>>("/admin/promotions", { params: { page, size } }); return response.data.data;
}
export async function createAdminPromotion(payload: PromotionPayload): Promise<Promotion> {
  const response = await httpClient.post<ApiResponse<Promotion>>("/admin/promotions", payload); return response.data.data;
}
export async function updateAdminPromotion(id: number, payload: PromotionPayload): Promise<Promotion> {
  const response = await httpClient.put<ApiResponse<Promotion>>(`/admin/promotions/${id}`, payload); return response.data.data;
}
export async function deleteAdminPromotion(id: number): Promise<void> { await httpClient.delete(`/admin/promotions/${id}`); }
