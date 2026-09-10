import { httpClient } from "./httpClient";

export type ProductReportSort = "QUANTITY" | "REVENUE" | "NAME";
export type ProductReportDirection = "ASC" | "DESC";

export type ProductSalesReportItem = {
  productName: string;
  categoryId: number | null;
  categoryName: string | null;
  quantitySold: number;
  revenue: number;
};

export type LowStockReportItem = {
  variantId: number;
  productId: number;
  productName: string;
  categoryId: number | null;
  categoryName: string | null;
  sku: string;
  color: string | null;
  storage: string | null;
  quantityOnHand: number;
  quantityReserved: number;
  availableQuantity: number;
  lowStockThreshold: number;
  stockStatus: "LOW_STOCK" | "OUT_OF_STOCK" | string;
};

export type ProductInventoryReport = {
  fromDate: string;
  toDate: string;
  categoryId: number | null;
  sortBy: ProductReportSort;
  sortDirection: ProductReportDirection;
  topSellingProducts: ProductSalesReportItem[];
  lowStockVariants: LowStockReportItem[];
};

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export async function getProductInventoryReport(params: {
  fromDate: string;
  toDate: string;
  categoryId?: number;
  sortBy?: ProductReportSort;
  sortDirection?: ProductReportDirection;
}): Promise<ProductInventoryReport> {
  const response = await httpClient.get<ApiResponse<ProductInventoryReport>>(
    "/admin/reports/products",
    { params },
  );
  return response.data.data;
}
