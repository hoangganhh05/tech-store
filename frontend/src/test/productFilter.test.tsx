import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { ProductListPage } from "../modules/products/ProductListPage";
import {
  getStorefrontProducts,
  getStorefrontCategories,
  getStorefrontBrands,
  type StorefrontProduct,
} from "../services/storefrontService";
import type { Category } from "../services/categoryService";
import type { Brand } from "../services/brandService";

vi.mock("../services/storefrontService", () => ({
  getStorefrontProducts: vi.fn(),
  getStorefrontCategories: vi.fn(),
  getStorefrontBrands: vi.fn(),
  searchStorefrontProducts: vi.fn(),
  getStorefrontHomeData: vi.fn(),
  getFeaturedProducts: vi.fn(),
  getNewArrivals: vi.fn(),
  getOnSaleProducts: vi.fn(),
}));

const mockedGetStorefrontProducts = vi.mocked(getStorefrontProducts);
const mockedGetStorefrontCategories = vi.mocked(getStorefrontCategories);
const mockedGetStorefrontBrands = vi.mocked(getStorefrontBrands);

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
];

const mockBrands: Brand[] = [
  {
    id: 1,
    name: "Apple",
    description: "Apple inc",
    logoUrl: null,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 2,
    name: "Samsung",
    description: "Samsung Corp",
    logoUrl: null,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

const mockProductApple: StorefrontProduct = {
  id: 1,
  name: "iPhone 15 Pro",
  description: "Flagship",
  brandId: 1,
  brandName: "Apple",
  categoryId: 1,
  categoryName: "Điện thoại",
  thumbnailUrl: null,
  minPrice: 25000000,
  maxPrice: 25000000,
  originalPrice: null,
  discountPercent: 0,
  totalStock: 15,
  hasStock: true,
  salesCount: 80,
  rating: 5.0,
  createdAt: "2026-09-01T00:00:00Z",
};

const mockProductSamsung: StorefrontProduct = {
  id: 2,
  name: "Galaxy A55",
  description: "Midrange",
  brandId: 2,
  brandName: "Samsung",
  categoryId: 1,
  categoryName: "Điện thoại",
  thumbnailUrl: null,
  minPrice: 9500000,
  maxPrice: 9500000,
  originalPrice: null,
  discountPercent: 0,
  totalStock: 20,
  hasStock: true,
  salesCount: 60,
  rating: 4.8,
  createdAt: "2026-09-02T00:00:00Z",
};

function renderProductListPage(initialRoute = "/products") {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={[initialRoute]}>
        <ProductListPage />
      </MemoryRouter>
    </ThemeProvider>
  );
}

describe("US-05.4: ProductListPage - Lọc sản phẩm theo thương hiệu, khoảng giá và danh mục", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetStorefrontCategories.mockResolvedValue(mockCategories);
    mockedGetStorefrontBrands.mockResolvedValue(mockBrands);
  });

  it("renders brands checkboxes and price presets in filter sidebar", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([mockProductApple, mockProductSamsung]);

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByText("Bộ lọc")).toBeInTheDocument();
      expect(screen.getByText("Thương hiệu")).toBeInTheDocument();
      expect(screen.getByText("Apple")).toBeInTheDocument();
      expect(screen.getByText("Samsung")).toBeInTheDocument();
      expect(screen.getByText("Dưới 5 triệu")).toBeInTheDocument();
      expect(screen.getByText("5 - 15 triệu")).toBeInTheDocument();
    });
  });

  it("filters products by brand multi-select checkbox", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([mockProductApple]);

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByTestId("brand-checkbox-1")).toBeInTheDocument();
    });

    // Check Apple checkbox
    fireEvent.click(screen.getByTestId("brand-checkbox-1"));

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          brandIds: [1],
        })
      );
    });
  });

  it("filters products by price preset chip", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([mockProductSamsung]);

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByTestId("preset-price-1")).toBeInTheDocument();
    });

    // Click "5 - 15 triệu" preset
    fireEvent.click(screen.getByTestId("preset-price-1"));

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          priceMin: 5000000,
          priceMax: 15000000,
        })
      );
    });
  });

  it("filters products by custom price range input", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([mockProductApple]);

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByTestId("custom-price-min")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId("custom-price-min"), {
      target: { value: "20000000" },
    });
    fireEvent.change(screen.getByTestId("custom-price-max"), {
      target: { value: "30000000" },
    });
    fireEvent.click(screen.getByTestId("apply-price-filter-btn"));

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          priceMin: 20000000,
          priceMax: 30000000,
        })
      );
    });
  });

  it("validates that custom min price cannot exceed max price", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([]);

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByTestId("custom-price-min")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId("custom-price-min"), {
      target: { value: "30000000" },
    });
    fireEvent.change(screen.getByTestId("custom-price-max"), {
      target: { value: "10000000" },
    });
    fireEvent.click(screen.getByTestId("apply-price-filter-btn"));

    expect(
      screen.getByText("Giá tối thiểu không được lớn hơn giá tối đa")
    ).toBeInTheDocument();
  });

  it("renders active filter badges and clears all filters on clear button click", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([mockProductApple]);

    renderProductListPage("/products?categoryId=1&brandIds=1&priceMin=20000000");

    await waitFor(() => {
      expect(screen.getByText("Đang lọc:")).toBeInTheDocument();
      expect(screen.getByText("Danh mục: Điện thoại")).toBeInTheDocument();
      expect(screen.getByText("Hãng: Apple")).toBeInTheDocument();
      expect(screen.getByText("Giá từ: 20.000.000₫")).toBeInTheDocument();
    });

    // Click "Xóa tất cả" in sidebar
    const clearBtn = screen.getByTestId("clear-all-filters-btn");
    fireEvent.click(clearBtn);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          categoryId: null,
          brandIds: [],
          priceMin: null,
          priceMax: null,
        })
      );
    });
  });

  it("displays empty filter state when no products match combined filter", async () => {
    mockedGetStorefrontProducts.mockResolvedValue([]);

    renderProductListPage("/products?brandIds=1&priceMax=1000000");

    await waitFor(() => {
      expect(screen.getByTestId("empty-filter-heading")).toHaveTextContent(
        "Không có sản phẩm phù hợp"
      );
      expect(screen.getByTestId("clear-filters-btn")).toBeInTheDocument();
    });
  });
});
