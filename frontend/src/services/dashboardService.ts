import { httpClient } from "./httpClient";

export type DashboardPeriod = "DAY" | "WEEK" | "MONTH";

export type DashboardStatusCount = {
  status: string;
  count: number;
};

export type DashboardTopProduct = {
  productName: string;
  quantitySold: number;
  revenue: number;
};

export type DashboardRevenuePoint = {
  label: string;
  revenue: number;
  orderCount: number;
};

export type AdminDashboard = {
  period: DashboardPeriod;
  fromDate: string;
  toDate: string;
  totalRevenue: number;
  totalOrders: number;
  ordersByStatus: DashboardStatusCount[];
  topSellingProducts: DashboardTopProduct[];
  revenueTrend: DashboardRevenuePoint[];
};

type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export async function getAdminDashboard(params: {
  period: DashboardPeriod;
  date: string;
}): Promise<AdminDashboard> {
  const response = await httpClient.get<ApiResponse<AdminDashboard>>(
    "/admin/dashboard",
    { params },
  );
  return response.data.data;
}
