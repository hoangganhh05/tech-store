import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { appTheme } from "../configs/theme";
import { StorefrontLayout } from "../layouts/StorefrontLayout";
import { ProductDetailPage } from "../modules/products/ProductDetailPage";
import { CartPage } from "../modules/cart/CartPage";
import { CartProvider } from "../modules/cart/CartContext";
import { AuthContext } from "../modules/auth/AuthStore";
import * as cartService from "../services/cartService";
import * as storefrontService from "../services/storefrontService";

vi.mock("../services/cartService", () => ({
  getCart: vi.fn(),
  addToCart: vi.fn(),
  updateCartItemQuantity: vi.fn(),
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
const mockedUpdateCartItemQuantity = vi.mocked(cartService.updateCartItemQuantity);
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

describe("US-07.2: Xem giỏ hàng và cập nhật số lượng từng sản phẩm (CartPage UI & Quantity Update)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockCartWithItems: cartService.Cart = {
    id: 1,
    totalItems: 2,
    subtotal: 51980000,
    items: [
      {
        id: 1,
        variantId: 101,
        productId: 1,
        productName: "iPhone 15 Pro",
        sku: "IP15P-TITAN-128",
        color: "Titan Tự Nhiên",
        storage: "128GB",
        price: 25990000,
        originalPrice: 28990000,
        imageUrl: "https://example.com/ip15p.png",
        quantity: 2,
        availableStock: 5,
        subtotal: 51980000,
      },
    ],
  };

  it("displays empty state when cart has no items", async () => {
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
            <MemoryRouter initialEntries={["/cart"]}>
              <CartPage />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("empty-cart-card")).toBeInTheDocument();
      expect(screen.getByTestId("empty-cart-message")).toHaveTextContent(
        "Giỏ hàng của bạn đang trống.",
      );
      expect(screen.getByTestId("continue-shopping-btn")).toBeInTheDocument();
    });
  });

  it("displays cart items, variants, line subtotals, and order summary", async () => {
    mockedGetCart.mockResolvedValue(mockCartWithItems);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/cart"]}>
              <CartPage />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("cart-table")).toBeInTheDocument();
      expect(screen.getByTestId("cart-item-1")).toBeInTheDocument();
      expect(screen.getByTestId("item-name-1")).toHaveTextContent("iPhone 15 Pro");
      expect(screen.getByText("Titan Tự Nhiên")).toBeInTheDocument();
      expect(screen.getByText("128GB")).toBeInTheDocument();
      expect(screen.getByTestId("item-qty-1")).toHaveTextContent("2");
      expect(screen.getByTestId("cart-summary")).toBeInTheDocument();
      expect(screen.getByTestId("cart-total-items")).toHaveTextContent("2 sản phẩm");
      expect(screen.getByTestId("checkout-btn")).toBeInTheDocument();
    });
  });

  it("increases quantity when clicking '+' button and recalculates totals", async () => {
    mockedGetCart.mockResolvedValue(mockCartWithItems);
    const updatedCart: cartService.Cart = {
      id: 1,
      totalItems: 3,
      subtotal: 77970000,
      items: [
        {
          ...mockCartWithItems.items[0],
          quantity: 3,
          subtotal: 77970000,
        },
      ],
    };
    mockedUpdateCartItemQuantity.mockResolvedValue(updatedCart);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/cart"]}>
              <CartPage />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("item-qty-1")).toHaveTextContent("2");
    });

    const increaseBtn = screen.getByTestId("increase-qty-btn-1");
    fireEvent.click(increaseBtn);

    await waitFor(() => {
      expect(mockedUpdateCartItemQuantity).toHaveBeenCalledWith(1, 3);
    });

    await waitFor(() => {
      expect(screen.getByTestId("item-qty-1")).toHaveTextContent("3");
      expect(screen.getByTestId("cart-total-items")).toHaveTextContent("3 sản phẩm");
    });
  });

  it("decreases quantity when clicking '-' button and recalculates totals", async () => {
    mockedGetCart.mockResolvedValue(mockCartWithItems);
    const updatedCart: cartService.Cart = {
      id: 1,
      totalItems: 1,
      subtotal: 25990000,
      items: [
        {
          ...mockCartWithItems.items[0],
          quantity: 1,
          subtotal: 25990000,
        },
      ],
    };
    mockedUpdateCartItemQuantity.mockResolvedValue(updatedCart);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/cart"]}>
              <CartPage />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("item-qty-1")).toHaveTextContent("2");
    });

    const decreaseBtn = screen.getByTestId("decrease-qty-btn-1");
    fireEvent.click(decreaseBtn);

    await waitFor(() => {
      expect(mockedUpdateCartItemQuantity).toHaveBeenCalledWith(1, 1);
    });

    await waitFor(() => {
      expect(screen.getByTestId("item-qty-1")).toHaveTextContent("1");
      expect(screen.getByTestId("cart-total-items")).toHaveTextContent("1 sản phẩm");
    });
  });

  it("disables '-' button when quantity is 1", async () => {
    const cartQty1: cartService.Cart = {
      id: 1,
      totalItems: 1,
      subtotal: 25990000,
      items: [
        {
          ...mockCartWithItems.items[0],
          quantity: 1,
          availableStock: 5,
        },
      ],
    };
    mockedGetCart.mockResolvedValue(cartQty1);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/cart"]}>
              <CartPage />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      const decreaseBtn = screen.getByTestId("decrease-qty-btn-1");
      expect(decreaseBtn).toBeDisabled();
      const increaseBtn = screen.getByTestId("increase-qty-btn-1");
      expect(increaseBtn).toBeEnabled();
    });
  });

  it("disables '+' button and shows warning when quantity reaches availableStock", async () => {
    const cartMaxStock: cartService.Cart = {
      id: 1,
      totalItems: 3,
      subtotal: 77970000,
      items: [
        {
          ...mockCartWithItems.items[0],
          quantity: 3,
          availableStock: 3,
        },
      ],
    };
    mockedGetCart.mockResolvedValue(cartMaxStock);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/cart"]}>
              <CartPage />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      const increaseBtn = screen.getByTestId("increase-qty-btn-1");
      expect(increaseBtn).toBeDisabled();
      expect(screen.getByText("Đã đạt giới hạn tồn kho (3)")).toBeInTheDocument();
    });
  });

  it("shows error alert when updating quantity fails", async () => {
    mockedGetCart.mockResolvedValue(mockCartWithItems);
    mockedUpdateCartItemQuantity.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          success: false,
          code: "INSUFFICIENT_STOCK",
          message: "Số lượng yêu cầu vượt quá tồn kho khả dụng",
        },
      },
    });

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/cart"]}>
              <CartPage />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("increase-qty-btn-1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("increase-qty-btn-1"));

    await waitFor(() => {
      const alert = screen.getByTestId("cart-error-alert");
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent("Số lượng yêu cầu vượt quá tồn kho khả dụng");
    });
  });
});
