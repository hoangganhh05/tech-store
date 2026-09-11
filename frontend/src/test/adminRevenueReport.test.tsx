import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AdminRevenueReportPage } from "../modules/admin/AdminRevenueReportPage";
import { exportRevenueReport, getRevenueReport, type RevenueReport } from "../services/revenueReportService";

vi.mock("../services/revenueReportService", () => ({
  exportRevenueReport: vi.fn(),
  getRevenueReport: vi.fn(),
}));

const mockedGetRevenueReport = vi.mocked(getRevenueReport);
const mockedExportRevenueReport = vi.mocked(exportRevenueReport);

const mockReport: RevenueReport = {
  fromDate: "2026-09-01",
  toDate: "2026-09-03",
  totalRevenue: 370000,
  totalOrders: 2,
  averageOrderValue: 185000,
  dailyRevenue: [
    { date: "2026-09-01", revenue: 370000, orderCount: 2, averageOrderValue: 185000 },
    { date: "2026-09-02", revenue: 0, orderCount: 0, averageOrderValue: 0 },
    { date: "2026-09-03", revenue: 0, orderCount: 0, averageOrderValue: 0 },
  ],
};

function renderPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <AdminRevenueReportPage />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-13.4: AdminRevenueReportPage", () => {
  afterEach(() => {
    mockedGetRevenueReport.mockReset();
    mockedExportRevenueReport.mockReset();
  });

  it("renders summary metrics and daily revenue table", async () => {
    mockedGetRevenueReport.mockResolvedValue(mockReport);
    renderPage();

    expect(await screen.findByText("Chi tiết doanh thu theo ngày")).toBeInTheDocument();
    expect(screen.getByTestId("revenue-metric-Tổng doanh thu")).toHaveTextContent("370.000");
    expect(screen.getByTestId("revenue-metric-Tổng số đơn hàng")).toHaveTextContent("2");
    expect(screen.getByTestId("revenue-metric-Giá trị đơn trung bình")).toHaveTextContent("185.000");
    expect(screen.getByRole("table", { name: "Bảng chi tiết doanh thu theo ngày" })).toBeInTheDocument();
    expect(screen.getByText("01/09/2026")).toBeInTheDocument();
  });

  it("validates the selected range before requesting a report", async () => {
    mockedGetRevenueReport.mockResolvedValue(mockReport);
    renderPage();
    await screen.findByText("Chi tiết doanh thu theo ngày");
    const dateInputs = screen.getAllByLabelText(/ngày/i);
    fireEvent.change(dateInputs[0], { target: { value: "2026-09-10" } });
    fireEvent.change(dateInputs[1], { target: { value: "2026-09-01" } });
    fireEvent.click(screen.getByRole("button", { name: "Xem báo cáo" }));

    expect(await screen.findByText("Ngày bắt đầu không được sau ngày kết thúc.")).toBeInTheDocument();
    expect(mockedGetRevenueReport).toHaveBeenCalledTimes(1);
  });

  it("shows a recoverable error when the API fails", async () => {
    mockedGetRevenueReport.mockRejectedValueOnce(new Error("network"));
    renderPage();

    expect(await screen.findByText(/Không thể tải báo cáo doanh thu/i)).toBeInTheDocument();
    mockedGetRevenueReport.mockResolvedValueOnce(mockReport);
    fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    await waitFor(() => expect(screen.getByText("Chi tiết doanh thu theo ngày")).toBeInTheDocument());
  });

  it("exports the currently displayed date range to Excel", async () => {
    mockedGetRevenueReport.mockResolvedValue(mockReport);
    mockedExportRevenueReport.mockResolvedValue({
      blob: new Blob(["xlsx"]),
      filename: "bao-cao-doanh-thu-2026-09-01-den-2026-09-03.xlsx",
    });
    const createObjectUrl = vi.fn(() => "blob:test");
    const revokeObjectUrl = vi.fn();
    const anchorClick = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);
    Object.defineProperty(window.URL, "createObjectURL", { configurable: true, value: createObjectUrl });
    Object.defineProperty(window.URL, "revokeObjectURL", { configurable: true, value: revokeObjectUrl });
    renderPage();

    await screen.findByText("Chi tiết doanh thu theo ngày");
    const dateInputs = screen.getAllByLabelText(/ngày/i) as HTMLInputElement[];
    const displayedRange = {
      fromDate: dateInputs[0].value,
      toDate: dateInputs[1].value,
    };
    fireEvent.click(screen.getByRole("button", { name: "Xuất báo cáo doanh thu ra Excel" }));

    await waitFor(() => expect(mockedExportRevenueReport).toHaveBeenCalledWith(displayedRange));
    expect(createObjectUrl).toHaveBeenCalled();
    expect(revokeObjectUrl).toHaveBeenCalledWith("blob:test");
    expect(anchorClick).toHaveBeenCalled();
    anchorClick.mockRestore();
  });
});
