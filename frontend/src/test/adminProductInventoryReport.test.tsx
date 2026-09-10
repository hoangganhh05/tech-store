import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AdminProductInventoryReportPage } from "../modules/admin/AdminProductInventoryReportPage";
import { getAdminCategories } from "../services/categoryService";
import { getProductInventoryReport, type ProductInventoryReport } from "../services/productInventoryReportService";

vi.mock("../services/categoryService", () => ({
  getAdminCategories: vi.fn(),
}));
vi.mock("../services/productInventoryReportService", () => ({
  getProductInventoryReport: vi.fn(),
}));

const mockedGetCategories = vi.mocked(getAdminCategories);
const mockedGetReport = vi.mocked(getProductInventoryReport);

const mockReport: ProductInventoryReport = {
  fromDate: "2026-09-01",
  toDate: "2026-09-03",
  categoryId: null,
  sortBy: "QUANTITY",
  sortDirection: "DESC",
  topSellingProducts: [
    { productName: "Điện thoại Alpha", categoryId: 1, categoryName: "Điện thoại", quantitySold: 12, revenue: 1200000 },
    { productName: "Tai nghe Beta", categoryId: 2, categoryName: "Âm thanh", quantitySold: 5, revenue: 250000 },
  ],
  lowStockVariants: [
    {
      variantId: 8,
      productId: 3,
      productName: "Điện thoại Gamma",
      categoryId: 1,
      categoryName: "Điện thoại",
      sku: "GAMMA-128",
      color: "Đen",
      storage: "128GB",
      quantityOnHand: 4,
      quantityReserved: 1,
      availableQuantity: 3,
      lowStockThreshold: 5,
      stockStatus: "LOW_STOCK",
    },
  ],
};

function renderPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <AdminProductInventoryReportPage />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-13.3: AdminProductInventoryReportPage", () => {
  beforeEach(() => {
    mockedGetCategories.mockResolvedValue([
      { id: 1, name: "Điện thoại", displayOrder: 0, isActive: true, createdAt: "", updatedAt: "" },
      { id: 2, name: "Âm thanh", displayOrder: 1, isActive: true, createdAt: "", updatedAt: "" },
    ]);
  });

  afterEach(() => {
    mockedGetCategories.mockReset();
    mockedGetReport.mockReset();
  });

  it("renders both top-selling and low-stock tables", async () => {
    mockedGetReport.mockResolvedValue(mockReport);
    renderPage();

    expect(await screen.findByText("Top sản phẩm bán chạy")).toBeInTheDocument();
    expect(screen.getByText("Điện thoại Alpha")).toBeInTheDocument();
    expect(screen.getByText("Sản phẩm tồn kho thấp")).toBeInTheDocument();
    expect(screen.getByText("GAMMA-128")).toBeInTheDocument();
    expect(screen.getByRole("table", { name: "Bảng top sản phẩm bán chạy" })).toBeInTheDocument();
    expect(screen.getByRole("table", { name: "Bảng sản phẩm tồn kho thấp" })).toBeInTheDocument();
  });

  it("sends selected category and sorting to the API", async () => {
    mockedGetReport.mockResolvedValue(mockReport);
    renderPage();
    await screen.findByText("Top sản phẩm bán chạy");

    fireEvent.mouseDown(screen.getByRole("combobox", { name: "Danh mục" }));
    fireEvent.click(await screen.findByRole("option", { name: "Điện thoại" }));
    fireEvent.mouseDown(screen.getByRole("combobox", { name: "Sắp xếp top bán chạy" }));
    fireEvent.click(await screen.findByRole("option", { name: "Doanh thu tăng dần" }));
    fireEvent.click(screen.getByRole("button", { name: "Xem báo cáo" }));

    await waitFor(() => {
      expect(mockedGetReport).toHaveBeenLastCalledWith(expect.objectContaining({
        categoryId: 1,
        sortBy: "REVENUE",
        sortDirection: "ASC",
      }));
    });
  });

  it("validates the date range and allows retry after an API error", async () => {
    mockedGetReport.mockRejectedValueOnce(new Error("network"));
    renderPage();
    expect(await screen.findByText(/Không thể tải báo cáo sản phẩm/i)).toBeInTheDocument();

    const dateInputs = screen.getAllByLabelText(/ngày/i);
    fireEvent.change(dateInputs[0], { target: { value: "2026-09-10" } });
    fireEvent.change(dateInputs[1], { target: { value: "2026-09-01" } });
    fireEvent.click(screen.getByRole("button", { name: "Xem báo cáo" }));
    expect(await screen.findByText("Ngày bắt đầu không được sau ngày kết thúc.")).toBeInTheDocument();

    mockedGetReport.mockResolvedValueOnce(mockReport);
    fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(await screen.findByText("GAMMA-128")).toBeInTheDocument();
  });
});
