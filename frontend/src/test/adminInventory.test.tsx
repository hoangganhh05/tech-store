import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AdminInventoryPage } from "../modules/admin/AdminInventoryPage";
import {
  adjustInventory,
  getInventories,
  getInventorySummary,
  getInventoryTransactions,
  importInventory,
  type InventoryItem,
  type InventorySummary,
  type InventoryTransactionItem,
} from "../services/inventoryService";
import { getAdminCategories, type Category } from "../services/categoryService";

vi.mock("../services/inventoryService", () => ({
  getInventories: vi.fn(),
  getInventorySummary: vi.fn(),
  getInventoryByVariantId: vi.fn(),
  getInventoryTransactions: vi.fn(),
  importInventory: vi.fn(),
  adjustInventory: vi.fn(),
}));

vi.mock("../services/categoryService", () => ({
  getAdminCategories: vi.fn(),
}));

const mockedGetInventories = vi.mocked(getInventories);
const mockedGetInventorySummary = vi.mocked(getInventorySummary);
const mockedGetAdminCategories = vi.mocked(getAdminCategories);
const mockedGetInventoryTransactions = vi.mocked(getInventoryTransactions);
const mockedImportInventory = vi.mocked(importInventory);
const mockedAdjustInventory = vi.mocked(adjustInventory);

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

const mockTransactions: InventoryTransactionItem[] = [
  {
    id: 1,
    inventoryId: 1,
    variantId: 101,
    productName: "iPhone 16 Pro",
    sku: "IP16P-128-BLK",
    color: "Titan Đen",
    storage: "128GB",
    transactionType: "IMPORT",
    quantityChange: 10,
    referenceType: "MANUAL_IMPORT",
    referenceId: null,
    note: "Nhập hàng từ nhà cung cấp Apple",
    createdById: 1,
    createdByName: "Admin User",
    createdByEmail: "admin@techstore.com",
    createdAt: "2026-09-06T15:00:00Z",
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
    mockedGetInventoryTransactions.mockResolvedValue({
      items: mockTransactions,
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
    mockedImportInventory.mockResolvedValue({
      ...mockItems[0],
      quantityOnHand: 30,
      availableQuantity: 28,
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

  test("opens import dialog from row with pre-selected variant", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const importRowBtn = screen.getByTestId("btn-import-row-101");
    fireEvent.click(importRowBtn);

    const dialog = screen.getByTestId("dialog-import-inventory");
    expect(dialog).toBeInTheDocument();
    expect(screen.getByText("Nhập kho biến thể sản phẩm")).toBeInTheDocument();
    expect(
      within(dialog).getAllByText(/IP16P-128-BLK/).length,
    ).toBeGreaterThanOrEqual(1);
  });

  test("submits import inventory successfully and triggers API call", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const headerImportBtn = screen.getByTestId("btn-header-import");
    fireEvent.click(headerImportBtn);

    expect(screen.getByTestId("dialog-import-inventory")).toBeInTheDocument();

    const quantityInput = screen.getByLabelText(/Số lượng nhập/i);
    fireEvent.change(quantityInput, { target: { value: "15" } });

    const noteInput = screen.getByLabelText(/Ghi chú nhập hàng/i);
    fireEvent.change(noteInput, {
      target: { value: "Nhập lô hàng Apple mới" },
    });

    const submitBtn = screen.getByTestId("btn-submit-import");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedImportInventory).toHaveBeenCalledWith(
        expect.objectContaining({
          variantId: 101,
          quantity: 15,
          note: "Nhập lô hàng Apple mới",
        }),
      );
    });

    await waitFor(() => {
      expect(
        screen.getByText(/Nhập kho thành công 15 sản phẩm!/i),
      ).toBeInTheDocument();
    });
  });

  test("switches to Lịch sử nhập kho tab and displays transaction list", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const historyTab = screen.getByTestId("tab-inventory-history");
    fireEvent.click(historyTab);

    await waitFor(() => {
      expect(mockedGetInventoryTransactions).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 0,
          size: 10,
        }),
      );
    });

    await waitFor(() => {
      expect(screen.getByText("+10")).toBeInTheDocument();
      expect(
        screen.getByText("Nhập hàng từ nhà cung cấp Apple"),
      ).toBeInTheDocument();
      expect(screen.getByText("Admin User")).toBeInTheDocument();
      expect(screen.getByText("admin@techstore.com")).toBeInTheDocument();
    });
  });

  test("validates import form preventing submission when quantity is invalid", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const headerImportBtn = screen.getByTestId("btn-header-import");
    fireEvent.click(headerImportBtn);

    const quantityInput = screen.getByLabelText(/Số lượng nhập/i);
    fireEvent.change(quantityInput, { target: { value: "0" } });

    const submitBtn = screen.getByTestId("btn-submit-import");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText("Số lượng nhập phải lớn hơn 0"),
      ).toBeInTheDocument();
    });

    expect(mockedImportInventory).not.toHaveBeenCalled();
  });

  test("opens adjust dialog from header and validates required fields", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const headerAdjustBtn = screen.getByTestId("btn-header-adjust");
    fireEvent.click(headerAdjustBtn);

    expect(screen.getByTestId("dialog-adjust-inventory")).toBeInTheDocument();
    expect(screen.getByText("Điều chỉnh tồn kho kiểm kê")).toBeInTheDocument();

    const submitBtn = screen.getByTestId("btn-submit-adjust");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText("Số lượng điều chỉnh phải khác 0"),
      ).toBeInTheDocument();
    });

    expect(mockedAdjustInventory).not.toHaveBeenCalled();
  });

  test("opens adjust dialog from row and submits adjustment successfully", async () => {
    mockedAdjustInventory.mockResolvedValueOnce({
      id: 1,
      variantId: 101,
      productId: 1,
      productName: "iPhone 16 Pro",
      sku: "IP16P-128-BLK",
      color: "Titan Đen",
      storage: "128GB",
      categoryName: "Điện thoại",
      quantityOnHand: 17,
      quantityReserved: 0,
      availableQuantity: 17,
      lowStockThreshold: 5,
      stockStatus: "IN_STOCK",
      updatedAt: new Date().toISOString(),
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const rowAdjustBtn = screen.getByTestId("btn-adjust-row-101");
    fireEvent.click(rowAdjustBtn);

    const dialog = screen.getByTestId("dialog-adjust-inventory");
    expect(dialog).toBeInTheDocument();

    const qtyInput = screen
      .getByTestId("input-adjust-quantity")
      .querySelector("input")!;
    fireEvent.change(qtyInput, { target: { value: "-3" } });

    const reasonInput = screen
      .getByTestId("input-adjust-reason")
      .querySelector("textarea")!;
    fireEvent.change(reasonInput, {
      target: { value: "Kiểm kê định kỳ phát hiện thiếu 3 chiếc" },
    });

    const submitBtn = screen.getByTestId("btn-submit-adjust");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedAdjustInventory).toHaveBeenCalledWith({
        variantId: 101,
        quantityChange: -3,
        reason: "Kiểm kê định kỳ phát hiện thiếu 3 chiếc",
      });
    });

    await waitFor(() => {
      expect(
        screen.getByText(/Điều chỉnh tồn kho thành công!/i),
      ).toBeInTheDocument();
    });
  });

  test("validates negative expected stock prevents adjustment submission", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const rowAdjustBtn = screen.getByTestId("btn-adjust-row-101");
    fireEvent.click(rowAdjustBtn);

    const qtyInput = screen
      .getByTestId("input-adjust-quantity")
      .querySelector("input")!;
    fireEvent.change(qtyInput, { target: { value: "-25" } });

    const reasonInput = screen
      .getByTestId("input-adjust-reason")
      .querySelector("textarea")!;
    fireEvent.change(reasonInput, { target: { value: "Thất thoát lớn" } });

    const submitBtn = screen.getByTestId("btn-submit-adjust");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/không thể âm/i)).toBeInTheDocument();
    });

    expect(mockedAdjustInventory).not.toHaveBeenCalled();
  });

  test("filters transactions by type in history tab and displays ADJUSTMENT badge", async () => {
    const mockAdjustTx: InventoryTransactionItem[] = [
      {
        id: 201,
        inventoryId: 1,
        variantId: 101,
        productName: "iPhone 16 Pro",
        sku: "IP16P-128-BLK",
        color: "Titan Đen",
        storage: "128GB",
        transactionType: "ADJUSTMENT",
        quantityChange: -4,
        referenceType: "MANUAL_ADJUSTMENT",
        referenceId: null,
        note: "Hàng hư hỏng vỡ kính khi vận chuyển",
        createdById: 1,
        createdByName: "Admin User",
        createdByEmail: "admin@techstore.com",
        createdAt: "2026-03-30T10:00:00Z",
      },
    ];

    mockedGetInventoryTransactions.mockResolvedValue({
      items: mockAdjustTx,
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("IP16P-128-BLK")).toBeInTheDocument();
    });

    const historyTab = screen.getByTestId("tab-inventory-history");
    fireEvent.click(historyTab);

    const adjustFilterBtn = await screen.findByTestId("filter-tx-adjustment");
    fireEvent.click(adjustFilterBtn);

    await waitFor(() => {
      expect(mockedGetInventoryTransactions).toHaveBeenCalledWith(
        expect.objectContaining({
          type: "ADJUSTMENT",
        }),
      );
    });

    await waitFor(() => {
      expect(screen.getAllByText("Điều chỉnh").length).toBeGreaterThanOrEqual(
        2,
      );
      expect(screen.getByText("-4")).toBeInTheDocument();
      expect(
        screen.getByText("Hàng hư hỏng vỡ kính khi vận chuyển"),
      ).toBeInTheDocument();
    });
  });
});
