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
    description: "Apple brand",
    logoUrl: null,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

const mockProductA: StorefrontProduct = {
  id: 101,
  name: "iPhone 13",
  brandId: 1,
  brandName: "Apple",
  categoryId: 1,
  categoryName: "Điện thoại",
  thumbnailUrl: "/images/ip13.png",
  minPrice: 14000000,
  maxPrice: 14000000,
  originalPrice: null,
  discountPercent: 0,
  totalStock: 15,
  hasStock: true,
  salesCount: 10,
  rating: 5,
  createdAt: "2026-09-01T10:00:00Z",
};

const mockProductB: StorefrontProduct = {
  id: 102,
  name: "iPhone 15 Pro",
  brandId: 1,
  brandName: "Apple",
  categoryId: 1,
  categoryName: "Điện thoại",
  thumbnailUrl: "/images/ip15.png",
  minPrice: 28000000,
  maxPrice: 32000000,
  originalPrice: 30000000,
  discountPercent: 7,
  totalStock: 8,
  hasStock: true,
  salesCount: 50,
  rating: 5,
  createdAt: "2026-09-05T10:00:00Z",
};

function renderProductListPage(initialEntries = ["/products"]) {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={initialEntries}>
        <ProductListPage />
      </MemoryRouter>
    </ThemeProvider>
  );
}

describe("US-05.5: ProductListPage - Sắp xếp danh sách sản phẩm (giá tăng/giảm, mới nhất, bán chạy)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetStorefrontCategories.mockResolvedValue(mockCategories);
    mockedGetStorefrontBrands.mockResolvedValue(mockBrands);
    mockedGetStorefrontProducts.mockResolvedValue([mockProductA, mockProductB]);
  });

  it("renders sort dropdown in the results header", async () => {
    renderProductListPage();

    await waitFor(() => {
      expect(screen.getByTestId("sort-select")).toBeInTheDocument();
    });

    expect(screen.getByText("Mới nhất")).toBeInTheDocument();
  });

  it("changes sort to price ascending when selecting 'Giá tăng dần'", async () => {
    renderProductListPage();

    await waitFor(() => {
      expect(screen.getByTestId("sort-select")).toBeInTheDocument();
    });

    const selectTrigger = screen.getByTestId("sort-select").querySelector('[role="combobox"]');
    expect(selectTrigger).toBeInTheDocument();
    fireEvent.mouseDown(selectTrigger!);

    const priceAscOption = await screen.findByTestId("sort-option-price-asc");
    fireEvent.click(priceAscOption);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          sortBy: "price",
          sortDir: "asc",
        })
      );
    });
  });

  it("changes sort to price descending when selecting 'Giá giảm dần'", async () => {
    renderProductListPage();

    await waitFor(() => {
      expect(screen.getByTestId("sort-select")).toBeInTheDocument();
    });

    const selectTrigger = screen.getByTestId("sort-select").querySelector('[role="combobox"]');
    fireEvent.mouseDown(selectTrigger!);

    const priceDescOption = await screen.findByTestId("sort-option-price-desc");
    fireEvent.click(priceDescOption);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          sortBy: "price",
          sortDir: "desc",
        })
      );
    });
  });

  it("changes sort to salesCount descending when selecting 'Bán chạy nhất'", async () => {
    renderProductListPage();

    await waitFor(() => {
      expect(screen.getByTestId("sort-select")).toBeInTheDocument();
    });

    const selectTrigger = screen.getByTestId("sort-select").querySelector('[role="combobox"]');
    fireEvent.mouseDown(selectTrigger!);

    const salesDescOption = await screen.findByTestId("sort-option-sales-desc");
    fireEvent.click(salesDescOption);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          sortBy: "salesCount",
          sortDir: "desc",
        })
      );
    });
  });

  it("preserves active filter parameters when changing sort criteria", async () => {
    renderProductListPage(["/products?categoryId=1&brandIds=1&priceMin=10000000"]);

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
          categoryId: 1,
          brandIds: [1],
          priceMin: 10000000,
          sortBy: "price",
          sortDir: "asc",
        })
      );
    });
  });

  it("reads initial sort criteria from URL search parameters", async () => {
    renderProductListPage(["/products?sortBy=price&sortDir=desc"]);

    await waitFor(() => {
      expect(mockedGetStorefrontProducts).toHaveBeenCalledWith(
        expect.objectContaining({
          sortBy: "price",
          sortDir: "desc",
        })
      );
      expect(screen.getByText("Giá giảm dần")).toBeInTheDocument();
    });
  });
});
