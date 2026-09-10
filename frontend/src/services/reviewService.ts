import { httpClient } from "./httpClient";

export type ReviewStatus = "PENDING" | "APPROVED" | "HIDDEN";

export interface SubmitReviewPayload {
  rating: number;
  comment?: string;
}

export interface ReviewResponse {
  id: number;
  productId: number;
  userId: number;
  userFullName: string;
  rating: number;
  comment?: string | null;
  status: ReviewStatus;
  createdAt: string;
  updatedAt: string;
}

export type ProductReview = ReviewResponse;

export interface ReviewEligibilityResponse {
  canReview: boolean;
  myReview?: ReviewResponse | null;
}

export async function submitProductReview(
  productId: number,
  payload: SubmitReviewPayload,
): Promise<ReviewResponse> {
  const response = await httpClient.post<{ data: ReviewResponse }>(
    `/products/${productId}/reviews`,
    payload,
  );
  return response.data.data;
}

export async function getMyProductReview(
  productId: number,
): Promise<ReviewEligibilityResponse> {
  const response = await httpClient.get<{ data: ReviewEligibilityResponse }>(
    `/products/${productId}/reviews/my-review`,
  );
  return response.data.data;
}

export type ProductReviewPage = {
  items: ProductReview[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type ProductReviews = {
  averageRating: number;
  totalReviews: number;
  reviews: ProductReviewPage;
};

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export async function getProductReviews(
  productId: number | string,
  page = 0,
  size = 5,
): Promise<ProductReviews> {
  const response = await httpClient.get<ApiResponse<ProductReviews>>(
    `/products/${productId}/reviews`,
    { params: { page, size } },
  );
  return response.data.data;
}
