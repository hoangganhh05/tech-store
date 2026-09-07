import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { ProductListPage } from "../modules/products/ProductListPage";
import {
  getStorefrontProducts,
  getStorefrontCategories,
  type StorefrontProduct,
} from "../services/storefrontService";
import type { Category } from "../services/categoryService";

vi.mock("../services/storefrontService", () => ({
  getStorefrontProducts: vi.fn(),
  getStorefrontCategories: vi.fn(),
  getStorefrontHomeData: vi.fn(),
  getFeaturedProducts: vi.fn(),
  getNewArrivals: vi.fn(),
  getOnSaleProducts: vi.fn(),
}));

const mockedGetStorefrontProducts = vi.mocked(getStorefrontProducts);
const mockedGetStorefrontCategories = vi.mocked(getStorefrontCategories);

const mockCategories: Category[] = [
  {
    id: 1,
    name: "Điện thoại",
    description: "Smartphone",
    parentId: null,
    parentName: null,
    imageUrl: null,
    displayOrder: 1,
    isActive: true,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 2,
    name: "Laptop",
    description: "Máy tính xách tay",
    parentId: null,
    parentName: null,
    imageUrl: null,
    displayOrder: 2,
    isActive: true,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

const mockProductPhone: StorefrontProduct = {
  id: 1,
  name: "iPhone 15 Pro Max",
  description: "Flagship Apple",
  brandId: 1,
  brandName: "Apple",
  categoryId: 1,
  categoryName: "Điện thoại",
  thumbnailUrl: "https://example.com/ip15pm.jpg",
  minPrice: 29990000,
  maxPrice: 34990000,
  originalPrice: null,
  discountPercent: 0,
  totalStock: 25,
  hasStock: true,
  salesCount: 150,
  rating: 5.0,
  createdAt: "2026-09-01T00:00:00Z",
};

const mockProductLaptop: StorefrontProduct = {
  id: 2,
  name: "MacBook Air M3",
  description: "Laptop mỏng nhẹ",
  brandId: 1,
  brandName: "Apple",
  categoryId: 2,
  categoryName: "Laptop",
  thumbnailUrl: null,
  minPrice: 27990000,
  maxPrice: 27990000,
  originalPrice: null,
  discountPercent: 0,
  totalStock: 10,
  hasStock: true,
  salesCount: 45,
  rating: 4.8,
  createdAt: "2026-09-02T00:00:00Z",
};

function renderProductListPage(initialRoute = "/products") {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={[initialRoute]}>
        <ProductListPage />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-05.2: ProductListPage - Xem danh sách sản phẩm theo từng danh mục", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetStorefrontCategories.mockResolvedValue(mockCategories);
  });

  it("renders page intro and all products with total count by default", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([
      mockProductPhone,
      mockProductLaptop,
    ]);

    renderProductListPage("/products");

    expect(
      screen.getByRole("heading", { name: "Sản phẩm" }),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("Tìm thấy 2 sản phẩm")).toBeInTheDocument();
    });

    expect(screen.getByText("iPhone 15 Pro Max")).toBeInTheDocument();
    expect(screen.getByText("MacBook Air M3")).toBeInTheDocument();
    expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(null);
  });

  it("filters products by categoryId from URL query param and displays category name", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([mockProductPhone]);

    renderProductListPage("/products?categoryId=1");

    await waitFor(() => {
      expect(screen.getByText("Tìm thấy 1 sản phẩm")).toBeInTheDocument();
    });

    expect(screen.getByText("iPhone 15 Pro Max")).toBeInTheDocument();
    expect(screen.queryByText("MacBook Air M3")).not.toBeInTheDocument();
    expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(1);
  });

  it("switches category when clicking category chip", async () => {
    mockedGetStorefrontProducts
      .mockResolvedValueOnce([mockProductPhone, mockProductLaptop])
      .mockResolvedValueOnce([mockProductLaptop]);

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByText("Tìm thấy 2 sản phẩm")).toBeInTheDocument();
    });

    // Click "Laptop" chip
    const laptopChip = screen.getByRole("button", { name: "Laptop" });
    fireEvent.click(laptopChip);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(2);
    });
  });

  it("renders error alert with retry button when API fails", async () => {
    mockedGetStorefrontProducts.mockRejectedValueOnce(
      new Error("Lỗi kết nối máy chủ"),
    );

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByText("Lỗi kết nối máy chủ")).toBeInTheDocument();
    });

    const retryBtn = screen.getByRole("button", { name: /Thử lại/i });
    expect(retryBtn).toBeInTheDocument();

    mockedGetStorefrontProducts.mockResolvedValueOnce([mockProductPhone]);
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.queryByText("Lỗi kết nối máy chủ")).not.toBeInTheDocument();
      expect(screen.getByText("iPhone 15 Pro Max")).toBeInTheDocument();
    });
  });

  it("renders friendly empty state when category has no products", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([]);

    renderProductListPage("/products?categoryId=2");

    await waitFor(() => {
      expect(screen.getByText("Tìm thấy 0 sản phẩm")).toBeInTheDocument();
      expect(screen.getByText("Chưa có sản phẩm nào")).toBeInTheDocument();
    });
  });
});
