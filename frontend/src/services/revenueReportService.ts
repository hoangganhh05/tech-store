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

function getFilename(contentDisposition: string | undefined, fallback: string) {
  if (!contentDisposition) return fallback;
  const encoded = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (encoded) {
    try {
      return decodeURIComponent(encoded);
    } catch {
      return fallback;
    }
  }
  return contentDisposition.match(/filename="?([^";]+)"?/i)?.[1] ?? fallback;
}

export async function exportRevenueReport(params: {
  fromDate: string;
  toDate: string;
}): Promise<{ blob: Blob; filename: string }> {
  const fallback = `bao-cao-doanh-thu-${params.fromDate}-den-${params.toDate}.xlsx`;
  const response = await httpClient.get<Blob>(
    "/admin/reports/revenue/export",
    { params, responseType: "blob" },
  );
  return {
    blob: response.data,
    filename: getFilename(response.headers["content-disposition"], fallback),
  };
}
