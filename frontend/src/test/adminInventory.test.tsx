import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AdminInventoryPage } from "../modules/admin/AdminInventoryPage";
import {
  getInventories,
  getInventorySummary,
  type InventoryItem,
  type InventorySummary,
} from "../services/inventoryService";
import { getAdminCategories, type Category } from "../services/categoryService";

vi.mock("../services/inventoryService", () => ({
  getInventories: vi.fn(),
  getInventorySummary: vi.fn(),
  getInventoryByVariantId: vi.fn(),
}));

vi.mock("../services/categoryService", () => ({
  getAdminCategories: vi.fn(),
}));

const mockedGetInventories = vi.mocked(getInventories);
const mockedGetInventorySummary = vi.mocked(getInventorySummary);
const mockedGetAdminCategories = vi.mocked(getAdminCategories);

const mockCategories: Category[] = [
  {
    id: 1,
    name: "Điện thoại",
    description: "Điện thoại",
    isActive: true,
    displayOrder: 1,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 2,
    name: "Laptop",
    description: "Laptop",
    isActive: true,
    displayOrder: 2,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

const mockSummary: InventorySummary = {
  totalVariants: 3,
  inStockCount: 1,
  lowStockCount: 1,
  outOfStockCount: 1,
};

const mockItems: InventoryItem[] = [
  {
    id: 1,
    variantId: 101,
    sku: "IP16P-128-BLK",
    productId: 10,
    productName: "iPhone 16 Pro",
    categoryName: "Điện thoại",
    brandName: "Apple",
    color: "Titan Đen",
    storage: "128GB",
    price: 28990000,
    quantityOnHand: 20,
    quantityReserved: 2,
    availableQuantity: 18,
    lowStockThreshold: 5,
    stockStatus: "IN_STOCK",
    updatedAt: "2026-09-06T12:00:00Z",
  },
  {
    id: 2,
    variantId: 102,
    sku: "IP16P-256-WHT",
    productId: 10,
    productName: "iPhone 16 Pro",
    categoryName: "Điện thoại",
    brandName: "Apple",
    color: "Titan Trắng",
    storage: "256GB",
    price: 31990000,
    quantityOnHand: 4,
    quantityReserved: 1,
    availableQuantity: 3,
    lowStockThreshold: 5,
    stockStatus: "LOW_STOCK",
    updatedAt: "2026-09-06T12:00:00Z",
  },
  {
    id: 3,
    variantId: 103,
    sku: "MBP-M3-SILVER",
    productId: 11,
    productName: "MacBook Pro M3",
    categoryName: "Laptop",
    brandName: "Apple",
    color: "Bạc",
    storage: "512GB",
    price: 45000000,
    quantityOnHand: 1,
    quantityReserved: 1,
    availableQuantity: 0,
    lowStockThreshold: 5,
    stockStatus: "OUT_OF_STOCK",
    updatedAt: "2026-09-06T12:00:00Z",
  },
];

const renderComponent = () => {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <AdminInventoryPage />
      </MemoryRouter>
    </ThemeProvider>,
  );
};

describe("AdminInventoryPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetAdminCategories.mockResolvedValue(mockCategories);
    mockedGetInventorySummary.mockResolvedValue(mockSummary);
    mockedGetInventories.mockResolvedValue({
      items: mockItems,
      page: 0,
      size: 10,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });
  });

  test("renders page header and stat summary cards with numbers", async () => {
    renderComponent();

    expect(screen.getByText("Quản lý tồn kho")).toBeInTheDocument();
    expect(
      screen.getByText(
        /Theo dõi và kiểm soát số lượng tồn kho theo từng biến thể/,
      ),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("TỔNG BIẾN THỂ")).toBeInTheDocument();
      expect(screen.getByText("CÒN HÀNG")).toBeInTheDocument();
      expect(screen.getByText("SẮP HẾT HÀNG")).toBeInTheDocument();
      expect(screen.getByText("HẾT HÀNG")).toBeInTheDocument();
    });

    expect(
      screen.getByText("TỔNG BIẾN THỂ").parentElement?.textContent,
    ).toContain("3");
    expect(screen.getByText("CÒN HÀNG").parentElement?.textContent).toContain(
      "1",
    );
    expect(
      screen.getByText("SẮP HẾT HÀNG").parentElement?.textContent,
    ).toContain("1");
    expect(screen.getByText("HẾT HÀNG").parentElement?.textContent).toContain(
      "1",
    );
  });

  test("renders inventory table with columns and variant data", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText("iPhone 16 Pro")).toHaveLength(2);
      expect(screen.getByText("MacBook Pro M3")).toBeInTheDocument();
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
      expect(screen.getByText("IP16P-256-WHT")).toBeInTheDocument();
      expect(screen.getByText("MBP-M3-SILVER")).toBeInTheDocument();
    });

    // Check available quantities
    expect(screen.getByText("18")).toBeInTheDocument();
    expect(screen.getAllByText("3").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("0")).toBeInTheDocument();

    // Check status chips
    expect(screen.getByText("Còn hàng")).toBeInTheDocument();
    expect(screen.getByText("Sắp hết")).toBeInTheDocument();
    expect(screen.getByText("Hết hàng")).toBeInTheDocument();
  });

  test("submits search form and triggers API with search term", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const searchInput = screen.getByLabelText(/Tìm kiếm sản phẩm, SKU.../i);
    fireEvent.change(searchInput, { target: { value: "MacBook" } });

    const filterButton = screen.getByRole("button", { name: /^Lọc$/i });
    fireEvent.click(filterButton);

    await waitFor(() => {
      expect(mockedGetInventories).toHaveBeenCalledWith(
        expect.objectContaining({
          search: "MacBook",
        }),
      );
    });
  });

  test("resets filters when clicking reset button", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const searchInput = screen.getByLabelText(/Tìm kiếm sản phẩm, SKU.../i);
    fireEvent.change(searchInput, { target: { value: "MacBook" } });

    const resetButton = screen.getByTitle("Đặt lại bộ lọc");
    fireEvent.click(resetButton);

    expect(searchInput).toHaveValue("");
  });

  test("shows empty state when no items match filter", async () => {
    mockedGetInventories.mockResolvedValueOnce({
      items: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    renderComponent();

    await waitFor(() => {
      expect(
        screen.getByText(
          /Không tìm thấy biến thể nào phù hợp với điều kiện tìm kiếm/i,
        ),
      ).toBeInTheDocument();
    });
  });

  test("shows error alert and provides retry button when API fails", async () => {
    mockedGetInventories.mockRejectedValueOnce(
      new Error("Lỗi kết nối cơ sở dữ liệu"),
    );

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Lỗi kết nối cơ sở dữ liệu")).toBeInTheDocument();
    });

    const retryButton = screen.getByRole("button", { name: "Thử lại" });
    expect(retryButton).toBeInTheDocument();

    mockedGetInventories.mockResolvedValueOnce({
      items: mockItems,
      page: 0,
      size: 10,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });

    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });
  });
});
