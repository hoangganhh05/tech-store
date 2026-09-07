import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { ProductDetailPage } from "../modules/products/ProductDetailPage";
import {
  getStorefrontProductDetail,
  type StorefrontProductDetail,
} from "../services/storefrontService";

vi.mock("../services/storefrontService", () => ({
  getStorefrontProductDetail: vi.fn(),
  getStorefrontProducts: vi.fn(),
  getStorefrontCategories: vi.fn(),
  getStorefrontBrands: vi.fn(),
  searchStorefrontProducts: vi.fn(),
  getStorefrontHomeData: vi.fn(),
  getFeaturedProducts: vi.fn(),
  getNewArrivals: vi.fn(),
  getOnSaleProducts: vi.fn(),
}));

const mockedGetStorefrontProductDetail = vi.mocked(getStorefrontProductDetail);

const mockProductDetail: StorefrontProductDetail = {
  id: 1,
  name: "iPhone 15 Pro Max",
  description: "Điện thoại flagship hàng đầu từ Apple với vỏ titan siêu nhẹ.",
  brandId: 1,
  brandName: "Apple",
  categoryId: 1,
  categoryName: "Điện thoại",
  status: "ACTIVE",
  minPrice: 29990000,
  maxPrice: 34990000,
  originalPrice: 34990000,
  discountPercent: 14,
  totalStock: 25,
  hasStock: true,
  salesCount: 120,
  rating: 4.9,
  variants: [
    {
      id: 101,
      productId: 1,
      productName: "iPhone 15 Pro Max",
      sku: "IP15PM-256-TN",
      color: "Titan Tự Nhiên",
      storage: "256GB",
      price: 29990000,
      originalPrice: 34990000,
      stockQuantity: 15,
      status: "ACTIVE",
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
    {
      id: 102,
      productId: 1,
      productName: "iPhone 15 Pro Max",
      sku: "IP15PM-512-BL",
      color: "Titan Xanh",
      storage: "512GB",
      price: 34990000,
      originalPrice: 39990000,
      stockQuantity: 10,
      status: "ACTIVE",
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
  ],
  images: [
    {
      id: 201,
      productId: 1,
      variantId: 101,
      variantSku: "IP15PM-256-TN",
      variantColor: "Titan Tự Nhiên",
      imageUrl: "https://example.com/ip15pm-main.jpg",
      isPrimary: true,
      displayOrder: 1,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
    {
      id: 202,
      productId: 1,
      variantId: null,
      imageUrl: "https://example.com/ip15pm-side.jpg",
      isPrimary: false,
      displayOrder: 2,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
  ],
  specifications: [
    {
      id: 301,
      productId: 1,
      specKey: "Màn hình",
      specValue: "OLED 6.7 inch Super Retina XDR 120Hz",
      displayOrder: 1,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
    {
      id: 302,
      productId: 1,
      specKey: "Chip xử lý",
      specValue: "Apple A17 Pro (3nm)",
      displayOrder: 2,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
  ],
  createdAt: "2026-09-01T00:00:00Z",
  updatedAt: "2026-09-01T00:00:00Z",
};

function renderProductDetailPage(route = "/products/1") {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/products/:slug" element={<ProductDetailPage />} />
          <Route path="/products" element={<div>Danh sách sản phẩm</div>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-06.1: ProductDetailPage - Xem trang chi tiết sản phẩm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders full product information with name, brand, category, price, and stock", async () => {
    mockedGetStorefrontProductDetail.mockResolvedValue(mockProductDetail);

    renderProductDetailPage("/products/1");

    await waitFor(() => {
      expect(screen.getByTestId("product-title")).toHaveTextContent(
        "iPhone 15 Pro Max",
      );
      expect(screen.getByTestId("brand-badge")).toHaveTextContent("Apple");
      expect(screen.getByTestId("category-badge")).toHaveTextContent(
        "Điện thoại",
      );
      expect(screen.getByTestId("product-price")).toBeInTheDocument();
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Còn hàng (25 sản phẩm)",
      );
      expect(screen.getByTestId("product-short-description")).toHaveTextContent(
        "Điện thoại flagship hàng đầu từ Apple với vỏ titan siêu nhẹ.",
      );
    });

    expect(mockedGetStorefrontProductDetail).toHaveBeenCalledWith(1);
  });

  it("renders product specifications table correctly", async () => {
    mockedGetStorefrontProductDetail.mockResolvedValue(mockProductDetail);

    renderProductDetailPage("/products/1");

    await waitFor(() => {
      expect(screen.getByTestId("specifications-table")).toBeInTheDocument();
      expect(screen.getByText("Màn hình")).toBeInTheDocument();
      expect(
        screen.getByText("OLED 6.7 inch Super Retina XDR 120Hz"),
      ).toBeInTheDocument();
      expect(screen.getByText("Chip xử lý")).toBeInTheDocument();
      expect(screen.getByText("Apple A17 Pro (3nm)")).toBeInTheDocument();
    });
  });

  it("handles gallery image switching and lightbox zoom modal", async () => {
    mockedGetStorefrontProductDetail.mockResolvedValue(mockProductDetail);

    renderProductDetailPage("/products/1");

    await waitFor(() => {
      const mainImg = screen.getByTestId("main-product-image");
      expect(mainImg).toHaveAttribute(
        "src",
        "https://example.com/ip15pm-main.jpg",
      );
      expect(
        screen.getByTestId("image-thumbnails-container"),
      ).toBeInTheDocument();
    });

    // Click secondary thumbnail
    const secondaryThumb = screen.getByTestId("thumbnail-image-1");
    fireEvent.click(secondaryThumb);

    const updatedMainImg = screen.getByTestId("main-product-image");
    expect(updatedMainImg).toHaveAttribute(
      "src",
      "https://example.com/ip15pm-side.jpg",
    );

    // Click zoom button
    const zoomBtn = screen.getByTestId("zoom-image-btn");
    fireEvent.click(zoomBtn);

    await waitFor(() => {
      expect(screen.getByTestId("zoom-dialog")).toBeInTheDocument();
      expect(screen.getByTestId("zoomed-image")).toHaveAttribute(
        "src",
        "https://example.com/ip15pm-side.jpg",
      );
    });

    // Close zoom dialog
    const closeBtn = screen.getByTestId("close-zoom-btn");
    fireEvent.click(closeBtn);

    await waitFor(() => {
      expect(screen.queryByTestId("zoomed-image")).not.toBeInTheDocument();
    });
  });

  it("displays 404 not found empty state when product does not exist", async () => {
    const error404 = {
      response: {
        status: 404,
        data: {
          code: "PRODUCT_NOT_FOUND",
          message: "Không tìm thấy sản phẩm",
        },
      },
    };
    mockedGetStorefrontProductDetail.mockRejectedValue(error404);

    renderProductDetailPage("/products/9999");

    await waitFor(() => {
      expect(screen.getByTestId("product-not-found-state")).toBeInTheDocument();
      expect(screen.getByTestId("not-found-title")).toHaveTextContent(
        "Không tìm thấy sản phẩm",
      );
      expect(screen.getByTestId("back-to-products-btn")).toBeInTheDocument();
    });

    // Click back button
    fireEvent.click(screen.getByTestId("back-to-products-btn"));
    await waitFor(() => {
      expect(screen.getByText("Danh sách sản phẩm")).toBeInTheDocument();
    });
  });

  it("displays discontinued state when product has been discontinued", async () => {
    const errorDiscontinued = {
      response: {
        status: 404,
        data: {
          code: "PRODUCT_NOT_FOUND",
          message: "Sản phẩm đã ngừng kinh doanh",
        },
      },
    };
    mockedGetStorefrontProductDetail.mockRejectedValue(errorDiscontinued);

    renderProductDetailPage("/products/2");

    await waitFor(() => {
      expect(screen.getByTestId("product-not-found-state")).toBeInTheDocument();
      expect(screen.getByTestId("not-found-title")).toHaveTextContent(
        "Sản phẩm đã ngừng kinh doanh",
      );
    });
  });

  it("renders out-of-stock badge when total stock is 0", async () => {
    const outOfStockProduct: StorefrontProductDetail = {
      ...mockProductDetail,
      totalStock: 0,
      hasStock: false,
    };
    mockedGetStorefrontProductDetail.mockResolvedValue(outOfStockProduct);

    renderProductDetailPage("/products/1");

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Tạm hết hàng",
      );
    });
  });
});
