import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { appTheme } from "../configs/theme";
import { StorefrontLayout } from "../layouts/StorefrontLayout";
import { ProductDetailPage } from "../modules/products/ProductDetailPage";
import { CartProvider } from "../modules/cart/CartContext";
import { AuthContext } from "../modules/auth/AuthStore";
import * as cartService from "../services/cartService";
import * as storefrontService from "../services/storefrontService";

vi.mock("../services/cartService", () => ({
  getCart: vi.fn(),
  addToCart: vi.fn(),
}));

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
  getRelatedProducts: vi.fn(),
}));

const mockedGetCart = vi.mocked(cartService.getCart);
const mockedAddToCart = vi.mocked(cartService.addToCart);
const mockedGetProductDetail = vi.mocked(
  storefrontService.getStorefrontProductDetail,
);
const mockedGetRelatedProducts = vi.mocked(
  storefrontService.getRelatedProducts,
);

const mockAuthValue = {
  user: null,
  isAuthenticated: false,
  signIn: vi.fn(),
  signOut: vi.fn(),
  updateUserProfile: vi.fn(),
  clearSession: vi.fn(),
};

const mockProductDetail: storefrontService.StorefrontProductDetail = {
  id: 1,
  name: 'iPhone 15 Pro Max',
  description: 'Điện thoại flagship cao cấp.',
  brandId: 1,
  brandName: 'Apple',
  categoryId: 1,
  categoryName: 'Điện thoại',
  status: 'ACTIVE',
  minPrice: 29990000,
  maxPrice: 34990000,
  originalPrice: 34990000,
  discountPercent: 14,
  totalStock: 10,
  hasStock: true,
  salesCount: 50,
  rating: 4.9,
  variants: [
    {
      id: 101,
      productId: 1,
      productName: 'iPhone 15 Pro Max',
      sku: 'IP15PM-TITAN-256',
      color: 'Titan Tự Nhiên',
      storage: '256GB',
      price: 29990000,
      originalPrice: 34990000,
      stockQuantity: 10,
      status: 'ACTIVE',
      stockStatus: 'IN_STOCK',
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: '2026-09-01T00:00:00Z',
    },
  ],
  images: [
    {
      id: 1,
      productId: 1,
      imageUrl: 'https://example.com/ip15pm.jpg',
      isPrimary: true,
      displayOrder: 0,
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: '2026-09-01T00:00:00Z',
    },
  ],
  specifications: [],
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: '2026-09-01T00:00:00Z',
};

describe("US-07.1: Thêm sản phẩm vào giỏ hàng (Cart UI & Badge)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetRelatedProducts.mockResolvedValue([]);
  });

  it("renders cart icon on header with 0 count when cart is empty", async () => {
    mockedGetCart.mockResolvedValue({
      id: 1,
      totalItems: 0,
      subtotal: 0,
      items: [],
    });

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/"]}>
              <StorefrontLayout />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      const badge = screen.getByTestId("header-cart-badge");
      expect(badge).toBeInTheDocument();
      expect(badge).toHaveTextContent("0");
    });
  });

  it("renders cart icon on header with correct count when cart has items", async () => {
    mockedGetCart.mockResolvedValue({
      id: 1,
      totalItems: 5,
      subtotal: 100000,
      items: [],
    });

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/"]}>
              <StorefrontLayout />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      const badge = screen.getByTestId("header-cart-badge");
      expect(badge).toHaveTextContent("5");
    });
  });

  it("adds product to cart successfully and shows success toast notification", async () => {
    mockedGetCart.mockResolvedValue({
      id: 1,
      totalItems: 0,
      subtotal: 0,
      items: [],
    });
    mockedGetProductDetail.mockResolvedValue(mockProductDetail);
    mockedAddToCart.mockResolvedValue({
      id: 1,
      totalItems: 2,
      subtotal: 59980000,
      items: [
        {
          id: 1,
          variantId: 101,
          productId: 1,
          productName: "iPhone 15 Pro Max",
          sku: "IP15PM-TITAN-256",
          price: 29990000,
          originalPrice: 34990000,
          quantity: 2,
          availableStock: 10,
          subtotal: 59980000,
        },
      ],
    });

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/products/1"]}>
              <StorefrontLayout />
              <Routes>
                <Route path="/products/:slug" element={<ProductDetailPage />} />
              </Routes>
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    // Wait for product detail to load
    await waitFor(() => {
      expect(screen.getByTestId("product-title")).toHaveTextContent(
        "iPhone 15 Pro Max",
      );
    });

    // Increase quantity to 2
    const increaseBtn = screen.getByTestId("increase-quantity-btn");
    fireEvent.click(increaseBtn);
    expect(screen.getByTestId("quantity-value")).toHaveTextContent("2");

    // Click "Thêm vào giỏ" button
    const addToCartBtn = screen.getByTestId("add-to-cart-btn");
    expect(addToCartBtn).toBeEnabled();
    fireEvent.click(addToCartBtn);

    // Verify addToCart was called with variantId 101 and quantity 2
    await waitFor(() => {
      expect(mockedAddToCart).toHaveBeenCalledWith({
        variantId: 101,
        quantity: 2,
      });
    });

    // Verify success toast appears
    await waitFor(() => {
      const toast = screen.getByTestId("cart-toast");
      expect(toast).toBeInTheDocument();
      expect(toast).toHaveTextContent(
        "Đã thêm sản phẩm vào giỏ hàng thành công!",
      );
    });

    // Verify header badge updated to 2
    await waitFor(() => {
      expect(screen.getByTestId("header-cart-badge")).toHaveTextContent("2");
    });
  });

  it("displays error toast when API fails with insufficient stock", async () => {
    mockedGetCart.mockResolvedValue({
      id: 1,
      totalItems: 0,
      subtotal: 0,
      items: [],
    });
    mockedGetProductDetail.mockResolvedValue(mockProductDetail);
    mockedAddToCart.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          success: false,
          code: "INSUFFICIENT_STOCK",
          message: "Số lượng vượt quá tồn kho khả dụng",
        },
      },
    });

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/products/1"]}>
              <Routes>
                <Route path="/products/:slug" element={<ProductDetailPage />} />
              </Routes>
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("product-title")).toBeInTheDocument();
    });

    const addToCartBtn = screen.getByTestId("add-to-cart-btn");
    fireEvent.click(addToCartBtn);

    await waitFor(() => {
      const toast = screen.getByTestId("cart-toast");
      expect(toast).toBeInTheDocument();
      expect(toast).toHaveTextContent("Số lượng vượt quá tồn kho khả dụng");
    });
  });
});
