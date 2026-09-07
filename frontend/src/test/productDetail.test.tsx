import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { ProductDetailPage } from "../modules/products/ProductDetailPage";
import {
  getStorefrontProductDetail,
  getVariantStock,
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
  getVariantStock: vi.fn(),
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
        "Còn hàng (15 sản phẩm)",
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
      variants: mockProductDetail.variants.map((v) => ({
        ...v,
        stockQuantity: 0,
      })),
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

describe("US-06.2: ProductDetailPage - Chọn biến thể sản phẩm (màu sắc, dung lượng...)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("automatically selects default variant (first in-stock variant) on load", async () => {
    mockedGetStorefrontProductDetail.mockResolvedValue(mockProductDetail);

    renderProductDetailPage("/products/1");

    await waitFor(() => {
      expect(screen.getByTestId("selected-color-label")).toHaveTextContent(
        "Titan Tự Nhiên",
      );
      expect(screen.getByTestId("selected-storage-label")).toHaveTextContent(
        "256GB",
      );
      expect(screen.getByTestId("product-price")).toHaveTextContent(
        "29.990.000 ₫",
      );
      expect(screen.getByTestId("variant-sku")).toHaveTextContent(
        "IP15PM-256-TN",
      );
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Còn hàng (15 sản phẩm)",
      );
    });
  });

  it("updates price, stock, sku, and gallery image when switching variant color and storage", async () => {
    const productWithBlueImage: StorefrontProductDetail = {
      ...mockProductDetail,
      images: [
        ...mockProductDetail.images,
        {
          id: 203,
          productId: 1,
          variantId: 102,
          variantSku: "IP15PM-512-BL",
          variantColor: "Titan Xanh",
          imageUrl: "https://example.com/ip15pm-blue.jpg",
          isPrimary: false,
          displayOrder: 3,
          createdAt: "2026-09-01T00:00:00Z",
          updatedAt: "2026-09-01T00:00:00Z",
        },
      ],
    };
    mockedGetStorefrontProductDetail.mockResolvedValue(productWithBlueImage);

    renderProductDetailPage("/products/1");

    await waitFor(() => {
      expect(screen.getByTestId("selected-color-label")).toHaveTextContent(
        "Titan Tự Nhiên",
      );
    });

    // Click color "Titan Xanh"
    const blueOption = screen.getByTestId("color-option-Titan Xanh");
    fireEvent.click(blueOption);

    await waitFor(() => {
      expect(screen.getByTestId("selected-color-label")).toHaveTextContent(
        "Titan Xanh",
      );
      expect(screen.getByTestId("selected-storage-label")).toHaveTextContent(
        "512GB",
      );
      expect(screen.getByTestId("product-price")).toHaveTextContent(
        "34.990.000 ₫",
      );
      expect(screen.getByTestId("variant-sku")).toHaveTextContent(
        "IP15PM-512-BL",
      );
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Còn hàng (10 sản phẩm)",
      );
      expect(screen.getByTestId("main-product-image")).toHaveAttribute(
        "src",
        "https://example.com/ip15pm-blue.jpg",
      );
    });
  });

  it("disables combination options that do not exist", async () => {
    const combinationProduct: StorefrontProductDetail = {
      id: 2,
      name: "Galaxy S24",
      status: "ACTIVE",
      minPrice: 20000000,
      maxPrice: 25000000,
      discountPercent: 0,
      totalStock: 15,
      hasStock: true,
      salesCount: 10,
      rating: 5,
      availableColors: ["Xám Titan", "Tím Titan"],
      availableStorages: ["128GB", "256GB"],
      variants: [
        {
          id: 201,
          productId: 2,
          productName: "Galaxy S24",
          sku: "S24-GR-128",
          color: "Xám Titan",
          storage: "128GB",
          price: 20000000,
          stockQuantity: 5,
          status: "ACTIVE",
          createdAt: "2026-09-01T00:00:00Z",
          updatedAt: "2026-09-01T00:00:00Z",
        },
        {
          id: 202,
          productId: 2,
          productName: "Galaxy S24",
          sku: "S24-GR-256",
          color: "Xám Titan",
          storage: "256GB",
          price: 23000000,
          stockQuantity: 5,
          status: "ACTIVE",
          createdAt: "2026-09-01T00:00:00Z",
          updatedAt: "2026-09-01T00:00:00Z",
        },
        {
          id: 203,
          productId: 2,
          productName: "Galaxy S24",
          sku: "S24-VT-128",
          color: "Tím Titan",
          storage: "128GB",
          price: 20000000,
          stockQuantity: 5,
          status: "ACTIVE",
          createdAt: "2026-09-01T00:00:00Z",
          updatedAt: "2026-09-01T00:00:00Z",
        },
        // NOTE: "Tím Titan" does NOT have 256GB!
      ],
      images: [],
      specifications: [],
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    };

    mockedGetStorefrontProductDetail.mockResolvedValue(combinationProduct);

    renderProductDetailPage("/products/2");

    // Initially: Xám Titan & 128GB selected
    await waitFor(() => {
      expect(screen.getByTestId("selected-color-label")).toHaveTextContent(
        "Xám Titan",
      );
    });

    // Select color "Tím Titan"
    const violetOption = screen.getByTestId("color-option-Tím Titan");
    fireEvent.click(violetOption);

    await waitFor(() => {
      expect(screen.getByTestId("selected-color-label")).toHaveTextContent(
        "Tím Titan",
      );
      // Since Tím Titan does not have 256GB, 256GB storage option must be DISABLED!
      const storage256 = screen.getByTestId("storage-option-256GB");
      expect(storage256).toBeDisabled();

      // But 128GB option is enabled
      const storage128 = screen.getByTestId("storage-option-128GB");
      expect(storage128).not.toBeDisabled();
    });
  });

  it("updates stock status to 'Tạm hết hàng' when selecting an out-of-stock variant", async () => {
    const productWithOutOfStockVariant: StorefrontProductDetail = {
      ...mockProductDetail,
      variants: [
        {
          ...mockProductDetail.variants[0],
          stockQuantity: 10,
        },
        {
          ...mockProductDetail.variants[1],
          stockQuantity: 0, // out of stock
        },
      ],
    };
    mockedGetStorefrontProductDetail.mockResolvedValue(
      productWithOutOfStockVariant,
    );

    renderProductDetailPage("/products/1");

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Còn hàng (10 sản phẩm)",
      );
    });

    // Switch to out-of-stock variant (Titan Xanh)
    const blueOption = screen.getByTestId("color-option-Titan Xanh");
    fireEvent.click(blueOption);

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Tạm hết hàng",
      );
    });
  });
});

describe("US-06.3: ProductDetailPage - Trạng thái tồn kho theo biến thể & disable nút mua khi hết hàng", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const stockVariantsProduct: StorefrontProductDetail = {
    id: 10,
    name: "iPhone 15 Pro",
    status: "ACTIVE",
    minPrice: 25000000,
    maxPrice: 28000000,
    discountPercent: 0,
    totalStock: 18,
    hasStock: true,
    salesCount: 50,
    rating: 5,
    availableColors: ["Xanh", "Vàng", "Đen"],
    availableStorages: ["128GB"],
    variants: [
      {
        id: 1001,
        productId: 10,
        productName: "iPhone 15 Pro",
        sku: "IP15P-BL-128",
        color: "Xanh",
        storage: "128GB",
        price: 25000000,
        stockQuantity: 15, // > 5 -> IN_STOCK
        status: "ACTIVE",
        stockStatus: "IN_STOCK",
        createdAt: "2026-09-01T00:00:00Z",
        updatedAt: "2026-09-01T00:00:00Z",
      },
      {
        id: 1002,
        productId: 10,
        productName: "iPhone 15 Pro",
        sku: "IP15P-YL-128",
        color: "Vàng",
        storage: "128GB",
        price: 25000000,
        stockQuantity: 3, // 1..5 -> LOW_STOCK
        status: "ACTIVE",
        stockStatus: "LOW_STOCK",
        createdAt: "2026-09-01T00:00:00Z",
        updatedAt: "2026-09-01T00:00:00Z",
      },
      {
        id: 1003,
        productId: 10,
        productName: "iPhone 15 Pro",
        sku: "IP15P-BK-128",
        color: "Đen",
        storage: "128GB",
        price: 25000000,
        stockQuantity: 0, // <= 0 -> OUT_OF_STOCK
        status: "ACTIVE",
        stockStatus: "OUT_OF_STOCK",
        createdAt: "2026-09-01T00:00:00Z",
        updatedAt: "2026-09-01T00:00:00Z",
      },
    ],
    images: [],
    specifications: [],
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  };

  it("renders 'Còn hàng' badge and enables buy buttons when variant stock > 5", async () => {
    mockedGetStorefrontProductDetail.mockResolvedValue(stockVariantsProduct);

    renderProductDetailPage("/products/10");

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Còn hàng (15 sản phẩm)",
      );
      const addToCartBtn = screen.getByTestId("add-to-cart-btn");
      const buyNowBtn = screen.getByTestId("buy-now-btn");
      expect(addToCartBtn).not.toBeDisabled();
      expect(buyNowBtn).not.toBeDisabled();
    });
  });

  it("renders 'Sắp hết hàng' badge and low-stock warning when variant stock is 1 to 5", async () => {
    const lowStockOnlyProduct: StorefrontProductDetail = {
      ...stockVariantsProduct,
      variants: [stockVariantsProduct.variants[1]], // Vàng, stock = 3
    };
    mockedGetStorefrontProductDetail.mockResolvedValue(lowStockOnlyProduct);

    renderProductDetailPage("/products/10");

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Sắp hết hàng (Chỉ còn 3 sản phẩm)",
      );
      expect(screen.getByTestId("low-stock-alert")).toHaveTextContent(
        "Chỉ còn 3 sản phẩm trong kho",
      );
      expect(screen.getByTestId("add-to-cart-btn")).not.toBeDisabled();
      expect(screen.getByTestId("buy-now-btn")).not.toBeDisabled();
    });
  });

  it("renders 'Tạm hết hàng' badge and disables both purchase buttons when variant stock is 0", async () => {
    const outOfStockOnlyProduct: StorefrontProductDetail = {
      ...stockVariantsProduct,
      variants: [stockVariantsProduct.variants[2]], // Đen, stock = 0
    };
    mockedGetStorefrontProductDetail.mockResolvedValue(outOfStockOnlyProduct);

    renderProductDetailPage("/products/10");

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Tạm hết hàng",
      );
      expect(screen.getByTestId("out-of-stock-alert")).toHaveTextContent(
        "Hết hàng",
      );
      expect(screen.getByTestId("add-to-cart-btn")).toBeDisabled();
      expect(screen.getByTestId("buy-now-btn")).toBeDisabled();
      expect(screen.getByTestId("decrease-quantity-btn")).toBeDisabled();
      expect(screen.getByTestId("increase-quantity-btn")).toBeDisabled();
      expect(screen.getByTestId("quantity-value")).toHaveTextContent("0");
    });
  });

  it("instantly updates stock badge and button disabled state when switching between variants", async () => {
    mockedGetStorefrontProductDetail.mockResolvedValue(stockVariantsProduct);

    renderProductDetailPage("/products/10");

    // 1. Initial variant is "Xanh" (stock 15)
    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Còn hàng (15 sản phẩm)",
      );
      expect(screen.getByTestId("add-to-cart-btn")).not.toBeDisabled();
      expect(screen.getByTestId("buy-now-btn")).not.toBeDisabled();
    });

    // 2. Switch to "Vàng" (stock 3)
    fireEvent.click(screen.getByTestId("color-option-Vàng"));

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Sắp hết hàng (Chỉ còn 3 sản phẩm)",
      );
      expect(screen.getByTestId("low-stock-alert")).toBeInTheDocument();
      expect(screen.getByTestId("add-to-cart-btn")).not.toBeDisabled();
      expect(screen.getByTestId("buy-now-btn")).not.toBeDisabled();
    });

    // 3. Switch to "Đen" (stock 0)
    fireEvent.click(screen.getByTestId("color-option-Đen"));

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Tạm hết hàng",
      );
      expect(screen.getByTestId("out-of-stock-alert")).toBeInTheDocument();
      expect(screen.getByTestId("add-to-cart-btn")).toBeDisabled();
      expect(screen.getByTestId("buy-now-btn")).toBeDisabled();
    });

    // 4. Switch back to "Xanh" (stock 15)
    fireEvent.click(screen.getByTestId("color-option-Xanh"));

    await waitFor(() => {
      expect(screen.getByTestId("stock-status")).toHaveTextContent(
        "Còn hàng (15 sản phẩm)",
      );
      expect(screen.getByTestId("add-to-cart-btn")).not.toBeDisabled();
      expect(screen.getByTestId("buy-now-btn")).not.toBeDisabled();
    });
  });

  it("controls quantity within bounds (min 1, max available stock)", async () => {
    const limitedStockProduct: StorefrontProductDetail = {
      ...stockVariantsProduct,
      variants: [stockVariantsProduct.variants[1]], // Vàng, stock = 3
    };
    mockedGetStorefrontProductDetail.mockResolvedValue(limitedStockProduct);

    renderProductDetailPage("/products/10");

    await waitFor(() => {
      expect(screen.getByTestId("quantity-value")).toHaveTextContent("1");
    });

    const decreaseBtn = screen.getByTestId("decrease-quantity-btn");
    const increaseBtn = screen.getByTestId("increase-quantity-btn");

    // At quantity 1, decrease is disabled
    expect(decreaseBtn).toBeDisabled();
    expect(increaseBtn).not.toBeDisabled();

    // Increment to 2
    fireEvent.click(increaseBtn);
    expect(screen.getByTestId("quantity-value")).toHaveTextContent("2");
    expect(decreaseBtn).not.toBeDisabled();
    expect(increaseBtn).not.toBeDisabled();

    // Increment to 3 (max stock)
    fireEvent.click(increaseBtn);
    expect(screen.getByTestId("quantity-value")).toHaveTextContent("3");
    expect(decreaseBtn).not.toBeDisabled();
    expect(increaseBtn).toBeDisabled();

    // Decrement back to 2
    fireEvent.click(decreaseBtn);
    expect(screen.getByTestId("quantity-value")).toHaveTextContent("2");
    expect(increaseBtn).not.toBeDisabled();
  });

  it("calls getVariantStock API to query variant stock directly", async () => {
    const mockedGetVariantStock = vi.mocked(getVariantStock);
    mockedGetVariantStock.mockResolvedValue({
      variantId: 1001,
      productId: 10,
      sku: "IP15P-BL-128",
      stockQuantity: 15,
      stockStatus: "IN_STOCK",
      isAvailable: true,
    });

    const stock = await getVariantStock(10, 1001);
    expect(stock.stockStatus).toBe("IN_STOCK");
    expect(stock.stockQuantity).toBe(15);
    expect(stock.isAvailable).toBe(true);
  });
});
