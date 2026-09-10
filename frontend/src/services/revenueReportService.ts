import { httpClient } from "./httpClient";

export type RevenueReportDay = {
  date: string;
  revenue: number;
  orderCount: number;
  averageOrderValue: number;
};

export type RevenueReport = {
  fromDate: string;
  toDate: string;
  totalRevenue: number;
  totalOrders: number;
  averageOrderValue: number;
  dailyRevenue: RevenueReportDay[];
};

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export async function getRevenueReport(params: {
  fromDate: string;
  toDate: string;
}): Promise<RevenueReport> {
  const response = await httpClient.get<ApiResponse<RevenueReport>>(
    "/admin/reports/revenue",
    { params },
  );
  return response.data.data;
}
