import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { ProductListPage } from "../modules/products/ProductListPage";
import {
  getStorefrontProducts,
  getStorefrontCategories,
  getStorefrontBrands,
  type StorefrontProductPageResponse,
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
    description: "Apple brand",
    logoUrl: null,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

const mockProductPage1: StorefrontProductPageResponse = {
  items: [
    {
      id: 1,
      name: "iPhone 15 Pro",
      brandId: 1,
      brandName: "Apple",
      categoryId: 1,
      categoryName: "Điện thoại",
      thumbnailUrl: null,
      minPrice: 28990000,
      maxPrice: 28990000,
      originalPrice: null,
      discountPercent: 0,
      totalStock: 10,
      hasStock: true,
      salesCount: 100,
      rating: 5,
      createdAt: "2026-09-01T00:00:00Z",
    },
  ],
  page: 0,
  size: 12,
  totalElements: 25,
  totalPages: 3,
  first: true,
  last: false,
};

const mockProductPage2: StorefrontProductPageResponse = {
  items: [
    {
      id: 2,
      name: "iPhone 14",
      brandId: 1,
      brandName: "Apple",
      categoryId: 1,
      categoryName: "Điện thoại",
      thumbnailUrl: null,
      minPrice: 18990000,
      maxPrice: 18990000,
      originalPrice: null,
      discountPercent: 0,
      totalStock: 15,
      hasStock: true,
      salesCount: 80,
      rating: 4.8,
      createdAt: "2026-08-01T00:00:00Z",
    },
  ],
  page: 1,
  size: 12,
  totalElements: 25,
  totalPages: 3,
  first: false,
  last: false,
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

describe("US-05.6: ProductListPage - Phân trang danh sách sản phẩm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetStorefrontCategories.mockResolvedValue(mockCategories);
    mockedGetStorefrontBrands.mockResolvedValue(mockBrands);
  });

  it("renders pagination controls when totalPages > 1 and displays total items count", async () => {
    mockedGetStorefrontProducts.mockResolvedValue(mockProductPage1);

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByTestId("pagination-control")).toBeInTheDocument();
      expect(screen.getByText("Tìm thấy 25 sản phẩm")).toBeInTheDocument();
      expect(screen.getByText("iPhone 15 Pro")).toBeInTheDocument();
    });

    expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        size: 12,
      }),
    );
  });

  it("does not render pagination controls when totalPages <= 1", async () => {
    const singlePage: StorefrontProductPageResponse = {
      items: [mockProductPage1.items[0]],
      page: 0,
      size: 12,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    };
    mockedGetStorefrontProducts.mockResolvedValue(singlePage);

    renderProductListPage("/products");

    await waitFor(() => {
      expect(screen.getByText("iPhone 15 Pro")).toBeInTheDocument();
    });

    expect(screen.queryByTestId("pagination-control")).not.toBeInTheDocument();
  });

  it("reads initial page from URL search parameters (?page=2)", async () => {
    mockedGetStorefrontProducts.mockResolvedValue(mockProductPage2);

    renderProductListPage("/products?page=2");

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 1, // 0-indexed API param
          size: 12,
        }),
      );
    });

    expect(screen.getByText("iPhone 14")).toBeInTheDocument();
  });

  it("switches page when clicking a page button and keeps active filters and sorting", async () => {
    mockedGetStorefrontProducts
      .mockResolvedValueOnce(mockProductPage1)
      .mockResolvedValueOnce(mockProductPage2);

    renderProductListPage("/products?categoryId=1&sortBy=price&sortDir=asc");

    await waitFor(() => {
      expect(screen.getByTestId("pagination-control")).toBeInTheDocument();
    });

    // Find page 2 button inside pagination
    const page2Button = screen.getByRole("button", { name: "Go to page 2" });
    fireEvent.click(page2Button);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          categoryId: 1,
          sortBy: "price",
          sortDir: "asc",
          page: 1,
          size: 12,
        }),
      );
    });
  });

  it("resets page to 1 when changing category filter", async () => {
    mockedGetStorefrontProducts.mockResolvedValue(mockProductPage2);

    renderProductListPage("/products?page=2");

    await waitFor(() => {
      expect(screen.getByText("Điện thoại")).toBeInTheDocument();
    });

    // Click category "Điện thoại"
    const categoryChip = screen.getByRole("button", { name: "Điện thoại" });
    fireEvent.click(categoryChip);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          categoryId: 1,
          page: 0, // Reset to page 1 (0-indexed)
        }),
      );
    });
  });

  it("resets page to 1 when changing sort criteria", async () => {
    mockedGetStorefrontProducts.mockResolvedValue(mockProductPage2);

    renderProductListPage("/products?page=2");

    await waitFor(() => {
      expect(screen.getByTestId("sort-select")).toBeInTheDocument();
    });

    const selectTrigger = screen.getByTestId("sort-select").querySelector('[role="combobox"]');
    fireEvent.mouseDown(selectTrigger!);

    const priceAscOption = await screen.findByTestId("sort-option-price-asc");
    fireEvent.click(priceAscOption);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          sortBy: "price",
          sortDir: "asc",
          page: 0, // Reset to page 1 (0-indexed)
        }),
      );
    });
  });
});
