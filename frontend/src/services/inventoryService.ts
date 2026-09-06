import { httpClient } from "./httpClient";

export type StockStatus = "ALL" | "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK";

export type InventoryItem = {
  id: number;
  variantId: number;
  sku: string;
  productId: number;
  productName: string;
  categoryName: string | null;
  brandName: string | null;
  color: string | null;
  storage: string | null;
  price: number;
  quantityOnHand: number;
  quantityReserved: number;
  availableQuantity: number;
  lowStockThreshold: number;
  stockStatus: StockStatus;
  updatedAt: string;
};

export type InventorySummary = {
  totalVariants: number;
  inStockCount: number;
  lowStockCount: number;
  outOfStockCount: number;
};

export type InventoryFilterParams = {
  search?: string;
  categoryId?: number;
  stockStatus?: StockStatus;
  page?: number;
  size?: number;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export async function getInventories(
  params?: InventoryFilterParams,
): Promise<PageResponse<InventoryItem>> {
  const queryParams: Record<string, string | number> = {};
  if (params?.search && params.search.trim()) {
    queryParams.search = params.search.trim();
  }
  if (params?.categoryId) {
    queryParams.categoryId = params.categoryId;
  }
  if (params?.stockStatus && params.stockStatus !== "ALL") {
    queryParams.stockStatus = params.stockStatus;
  }
  if (params?.page !== undefined) {
    queryParams.page = params.page;
  }
  if (params?.size !== undefined) {
    queryParams.size = params.size;
  }

  const response = await httpClient.get<
    ApiResponse<PageResponse<InventoryItem>>
  >("/admin/inventory", { params: queryParams });
  return response.data.data;
}

export async function getInventorySummary(): Promise<InventorySummary> {
  const response = await httpClient.get<ApiResponse<InventorySummary>>(
    "/admin/inventory/summary",
  );
  return response.data.data;
}

export async function getInventoryByVariantId(
  variantId: number,
): Promise<InventoryItem> {
  const response = await httpClient.get<ApiResponse<InventoryItem>>(
    `/admin/inventory/variants/${variantId}`,
  );
  return response.data.data;
}

export type InventoryImportRequest = {
  variantId: number;
  quantity: number;
  note?: string;
  referenceType?: string;
  referenceId?: number;
};

export type InventoryTransactionItem = {
  id: number;
  inventoryId: number;
  variantId: number;
  productName: string | null;
  sku: string;
  color: string | null;
  storage: string | null;
  transactionType:
    | "IMPORT"
    | "EXPORT"
    | "ADJUSTMENT"
    | "RESERVATION"
    | "RELEASE";
  quantityChange: number;
  referenceType: string | null;
  referenceId: number | null;
  note: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdByEmail: string | null;
  createdAt: string;
};

export async function importInventory(
  request: InventoryImportRequest,
): Promise<InventoryItem> {
  const response = await httpClient.post<ApiResponse<InventoryItem>>(
    "/admin/inventory/import",
    request,
  );
  return response.data.data;
}

export async function getInventoryTransactions(params?: {
  variantId?: number;
  type?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<InventoryTransactionItem>> {
  const queryParams: Record<string, string | number> = {};
  if (params?.variantId) {
    queryParams.variantId = params.variantId;
  }
  if (params?.type) {
    queryParams.type = params.type;
  }
  if (params?.page !== undefined) {
    queryParams.page = params.page;
  }
  if (params?.size !== undefined) {
    queryParams.size = params.size;
  }

  const response = await httpClient.get<
    ApiResponse<PageResponse<InventoryTransactionItem>>
  >("/admin/inventory/transactions", { params: queryParams });
  return response.data.data;
}
