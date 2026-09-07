import { httpClient } from "./httpClient";
import { getOrCreateSessionId } from "../utils/sessionStorage";

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export type StockIssueType =
  | "OUT_OF_STOCK"
  | "INSUFFICIENT_STOCK"
  | "INACTIVE_OR_DELETED";

export interface CartItemStockIssue {
  itemId: number;
  variantId: number;
  productName: string;
  sku: string;
  requestedQuantity: number;
  availableStock: number;
  issueType: StockIssueType;
  message: string;
}

export interface CartValidationResult {
  valid: boolean;
  issues: CartItemStockIssue[];
}

export interface CartItem {
  id: number;
  variantId: number;
  productId: number;
  productName: string;
  sku: string;
  color?: string | null;
  storage?: string | null;
  price: number;
  originalPrice?: number | null;
  imageUrl?: string | null;
  quantity: number;
  availableStock: number;
  subtotal: number;
  hasStockIssue?: boolean;
  stockStatusMessage?: string | null;
}

export interface Cart {
  id: number | null;
  totalItems: number;
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  total: number;
  hasStockIssue?: boolean;
  canCheckout?: boolean;
  items: CartItem[];
}

export interface AddToCartPayload {
  variantId: number;
  quantity: number;
}

export async function getCart(): Promise<Cart> {
  const sessionId = getOrCreateSessionId();
  const response = await httpClient.get<ApiResponse<Cart>>("/cart", {
    headers: {
      "X-Session-Id": sessionId,
    },
  });
  return response.data.data;
}

export async function addToCart(payload: AddToCartPayload): Promise<Cart> {
  const sessionId = getOrCreateSessionId();
  const response = await httpClient.post<ApiResponse<Cart>>(
    "/cart/items",
    payload,
    {
      headers: {
        "X-Session-Id": sessionId,
      },
    },
  );
  return response.data.data;
}

export async function updateCartItemQuantity(
  itemId: number,
  quantity: number,
): Promise<Cart> {
  const sessionId = getOrCreateSessionId();
  const response = await httpClient.patch<ApiResponse<Cart>>(
    `/cart/items/${itemId}`,
    { quantity },
    {
      headers: {
        "X-Session-Id": sessionId,
      },
    },
  );
  return response.data.data;
}

export async function removeCartItem(itemId: number): Promise<Cart> {
  const sessionId = getOrCreateSessionId();
  const response = await httpClient.delete<ApiResponse<Cart>>(
    `/cart/items/${itemId}`,
    {
      headers: {
        "X-Session-Id": sessionId,
      },
    },
  );
  return response.data.data;
}

export async function validateCartStock(): Promise<CartValidationResult> {
  const sessionId = getOrCreateSessionId();
  const response = await httpClient.post<ApiResponse<CartValidationResult>>(
    "/cart/validate",
    {},
    {
      headers: {
        "X-Session-Id": sessionId,
      },
    },
  );
  return response.data.data;
}
