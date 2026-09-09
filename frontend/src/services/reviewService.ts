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
