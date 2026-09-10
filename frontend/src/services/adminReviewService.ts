import { httpClient } from "./httpClient";
import type { ReviewResponse, ReviewStatus } from "./reviewService";

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export type AdminReview = ReviewResponse;

export type AdminReviewPage = {
  items: AdminReview[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type GetAdminReviewsParams = {
  status?: ReviewStatus;
  page?: number;
  size?: number;
};

export async function getAdminReviews(
  params: GetAdminReviewsParams = {},
): Promise<AdminReviewPage> {
  const response = await httpClient.get<ApiResponse<AdminReviewPage>>(
    "/admin/reviews",
    {
      params: {
        status: params.status || undefined,
        page: params.page ?? 0,
        size: params.size ?? 10,
      },
    },
  );
  return response.data.data;
}

export async function updateAdminReviewStatus(
  reviewId: number,
  status: ReviewStatus,
): Promise<AdminReview> {
  const response = await httpClient.patch<ApiResponse<AdminReview>>(
    `/admin/reviews/${reviewId}/status`,
    { status },
  );
  return response.data.data;
}
