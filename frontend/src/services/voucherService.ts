import { httpClient } from "./httpClient";

type ApiResponse<T> = { success: boolean; code: string; message: string; data: T; timestamp: string };
export type PageResponse<T> = { items: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean };
export type DiscountType = "PERCENT" | "FIXED";
export type Voucher = {
  id: number; code: string; name: string; discountType: DiscountType; discountValue: number;
  maxDiscount?: number | null; minimumOrder: number; usageLimit?: number | null; perUserLimit: number;
  usedCount: number; startsAt: string; endsAt: string; active: boolean; createdAt: string; updatedAt: string;
};
export type VoucherPayload = Omit<Voucher, "id" | "usedCount" | "createdAt" | "updatedAt">;

export async function getAdminVouchers(keyword = "", page = 0, size = 10): Promise<PageResponse<Voucher>> {
  const response = await httpClient.get<ApiResponse<PageResponse<Voucher>>>("/admin/vouchers", { params: { keyword: keyword || undefined, page, size } });
  return response.data.data;
}
export async function createAdminVoucher(payload: VoucherPayload): Promise<Voucher> {
  const response = await httpClient.post<ApiResponse<Voucher>>("/admin/vouchers", payload); return response.data.data;
}
export async function updateAdminVoucher(id: number, payload: VoucherPayload): Promise<Voucher> {
  const response = await httpClient.put<ApiResponse<Voucher>>(`/admin/vouchers/${id}`, payload); return response.data.data;
}
export async function deleteAdminVoucher(id: number): Promise<void> { await httpClient.delete(`/admin/vouchers/${id}`); }
