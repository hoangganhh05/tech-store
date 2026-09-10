import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AdminDashboardPage } from "../modules/admin/AdminDashboardPage";
import { getAdminDashboard, type AdminDashboard } from "../services/dashboardService";

vi.mock("../services/dashboardService", () => ({
  getAdminDashboard: vi.fn(),
}));

const mockedGetAdminDashboard = vi.mocked(getAdminDashboard);

const mockDashboard: AdminDashboard = {
  period: "MONTH",
  fromDate: "2026-09-01",
  toDate: "2026-09-30",
  totalRevenue: 1234000,
  totalOrders: 12,
  ordersByStatus: [
    { status: "PENDING", count: 2 },
    { status: "CONFIRMED", count: 3 },
    { status: "SHIPPING", count: 4 },
    { status: "COMPLETED", count: 3 },
  ],
  topSellingProducts: [
    { productName: "Điện thoại Alpha", quantitySold: 9, revenue: 900000 },
    { productName: "Tai nghe Beta", quantitySold: 5, revenue: 434000 },
  ],
  revenueTrend: [
    { label: "01/09", revenue: 1000000, orderCount: 8 },
    { label: "02/09", revenue: 334000, orderCount: 4 },
    { label: "03/09", revenue: 0, orderCount: 0 },
  ],
};

function renderPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <AdminDashboardPage />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-13.1: AdminDashboardPage", () => {
  afterEach(() => mockedGetAdminDashboard.mockReset());

  it("renders KPI cards, status counts, revenue chart and top products", async () => {
    mockedGetAdminDashboard.mockResolvedValue(mockDashboard);
    renderPage();

    expect(await screen.findByText("Điện thoại Alpha")).toBeInTheDocument();
    expect(screen.getByTestId("metric-Tổng doanh thu")).toHaveTextContent("1.234.000");
    expect(screen.getByTestId("metric-Tổng số đơn hàng")).toHaveTextContent("12");
    expect(screen.getByText("Chờ xác nhận")).toBeInTheDocument();
    expect(screen.getByText("Đang giao")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Biểu đồ doanh thu theo thời gian" })).toBeInTheDocument();
    expect(screen.getAllByTestId("revenue-bar")).toHaveLength(3);
  });

  it("reloads the dashboard after switching from month to day", async () => {
    mockedGetAdminDashboard.mockResolvedValue(mockDashboard);
    renderPage();
    await screen.findByText("Điện thoại Alpha");

    fireEvent.mouseDown(screen.getByRole("combobox", { name: "Khoảng xem" }));
    fireEvent.click(await screen.findByRole("option", { name: "Theo ngày" }));

    await waitFor(() => {
      expect(mockedGetAdminDashboard).toHaveBeenLastCalledWith(
        expect.objectContaining({ period: "DAY", date: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/) }),
      );
    });
  });

  it("shows a recoverable error when the dashboard API fails", async () => {
    mockedGetAdminDashboard.mockRejectedValueOnce(new Error("network"));
    renderPage();

    expect(await screen.findByText(/Không thể tải dữ liệu dashboard/i)).toBeInTheDocument();
    mockedGetAdminDashboard.mockResolvedValueOnce(mockDashboard);
    fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(await screen.findByText("Điện thoại Alpha")).toBeInTheDocument();
  });
});
