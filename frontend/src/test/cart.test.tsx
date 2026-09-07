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
  removeCartItem: vi.fn(),
  validateCartStock: vi.fn(),
  syncCart: vi.fn(),
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
const mockedUpdateCartItemQuantity = vi.mocked(
  cartService.updateCartItemQuantity,
);
const mockedRemoveCartItem = vi.mocked(cartService.removeCartItem);
const mockedSyncCart = vi.mocked(cartService.syncCart);
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
  name: "iPhone 15 Pro Max",
  description: "Điện thoại flagship cao cấp.",
  brandId: 1,
  brandName: "Apple",
  categoryId: 1,
  categoryName: "Điện thoại",
  status: "ACTIVE",
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
      productName: "iPhone 15 Pro Max",
      sku: "IP15PM-TITAN-256",
      color: "Titan Tự Nhiên",
      storage: "256GB",
      price: 29990000,
      originalPrice: 34990000,
      stockQuantity: 10,
      status: "ACTIVE",
      stockStatus: "IN_STOCK",
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
  ],
  images: [
    {
      id: 1,
      productId: 1,
      imageUrl: "https://example.com/ip15pm.jpg",
      isPrimary: true,
      displayOrder: 0,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
  ],
  specifications: [],
  createdAt: "2026-09-01T00:00:00Z",
  updatedAt: "2026-09-01T00:00:00Z",
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
      shippingFee: 0,
      discountAmount: 0,
      total: 0,
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
      shippingFee: 30000,
      discountAmount: 0,
      total: 130000,
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
      shippingFee: 0,
      discountAmount: 0,
      total: 0,
      items: [],
    });
    mockedGetProductDetail.mockResolvedValue(mockProductDetail);
    mockedAddToCart.mockResolvedValue({
      id: 1,
      totalItems: 2,
      subtotal: 59980000,
      shippingFee: 0,
      discountAmount: 0,
      total: 59980000,
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
      shippingFee: 0,
      discountAmount: 0,
      total: 0,
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
    shippingFee: 0,
    discountAmount: 0,
    total: 51980000,
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
      shippingFee: 0,
      discountAmount: 0,
      total: 0,
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
      expect(screen.getByTestId("item-name-1")).toHaveTextContent(
        "iPhone 15 Pro",
      );
      expect(screen.getByText("Titan Tự Nhiên")).toBeInTheDocument();
      expect(screen.getByText("128GB")).toBeInTheDocument();
      expect(screen.getByTestId("item-qty-1")).toHaveTextContent("2");
      expect(screen.getByTestId("cart-summary")).toBeInTheDocument();
      expect(screen.getByTestId("cart-total-items")).toHaveTextContent(
        "2 sản phẩm",
      );
      expect(screen.getByTestId("checkout-btn")).toBeInTheDocument();
    });
  });

  it("increases quantity when clicking '+' button and recalculates totals", async () => {
    mockedGetCart.mockResolvedValue(mockCartWithItems);
    const updatedCart: cartService.Cart = {
      id: 1,
      totalItems: 3,
      subtotal: 77970000,
      shippingFee: 0,
      discountAmount: 0,
      total: 77970000,
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
      expect(screen.getByTestId("cart-total-items")).toHaveTextContent(
        "3 sản phẩm",
      );
    });
  });

  it("decreases quantity when clicking '-' button and recalculates totals", async () => {
    mockedGetCart.mockResolvedValue(mockCartWithItems);
    const updatedCart: cartService.Cart = {
      id: 1,
      totalItems: 1,
      subtotal: 25990000,
      shippingFee: 0,
      discountAmount: 0,
      total: 25990000,
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
      expect(screen.getByTestId("cart-total-items")).toHaveTextContent(
        "1 sản phẩm",
      );
    });
  });

  it("disables '-' button when quantity is 1", async () => {
    const cartQty1: cartService.Cart = {
      id: 1,
      totalItems: 1,
      subtotal: 25990000,
      shippingFee: 0,
      discountAmount: 0,
      total: 25990000,
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
      shippingFee: 0,
      discountAmount: 0,
      total: 77970000,
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
      expect(
        screen.getByText("Đã đạt giới hạn tồn kho (3)"),
      ).toBeInTheDocument();
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
      expect(alert).toHaveTextContent(
        "Số lượng yêu cầu vượt quá tồn kho khả dụng",
      );
    });
  });
});

describe("US-07.3: Xoá một hoặc nhiều sản phẩm khỏi giỏ hàng (Remove Cart Item)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockCartWithItems: cartService.Cart = {
    id: 1,
    totalItems: 2,
    subtotal: 51980000,
    shippingFee: 0,
    discountAmount: 0,
    total: 51980000,
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

  it("displays delete button for each item and opens confirmation dialog when clicked", async () => {
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
      expect(screen.getByTestId("remove-item-btn-1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("remove-item-btn-1"));

    expect(screen.getByTestId("delete-confirm-dialog")).toBeInTheDocument();
    expect(
      screen.getByText(/Bạn có chắc chắn muốn xoá sản phẩm/i),
    ).toBeInTheDocument();
    expect(screen.getByTestId("cancel-delete-btn")).toBeInTheDocument();
    expect(screen.getByTestId("confirm-delete-btn")).toBeInTheDocument();

    // Clicking cancel closes the dialog without calling API
    fireEvent.click(screen.getByTestId("cancel-delete-btn"));

    await waitFor(() => {
      expect(
        screen.queryByTestId("delete-confirm-dialog"),
      ).not.toBeInTheDocument();
    });
    expect(mockedRemoveCartItem).not.toHaveBeenCalled();
  });

  it("confirms item deletion, calls API, updates cart to empty state and shows toast", async () => {
    mockedGetCart.mockResolvedValue(mockCartWithItems);
    mockedRemoveCartItem.mockResolvedValue({
      id: 1,
      totalItems: 0,
      subtotal: 0,
      shippingFee: 0,
      discountAmount: 0,
      total: 0,
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
      expect(screen.getByTestId("remove-item-btn-1")).toBeInTheDocument();
    });

    // Open confirmation dialog
    fireEvent.click(screen.getByTestId("remove-item-btn-1"));

    // Click confirm delete
    fireEvent.click(screen.getByTestId("confirm-delete-btn"));

    await waitFor(() => {
      expect(mockedRemoveCartItem).toHaveBeenCalledWith(1);
    });

    // Verify success toast notification
    await waitFor(() => {
      const toast = screen.getByTestId("cart-toast");
      expect(toast).toBeInTheDocument();
      expect(toast).toHaveTextContent(
        "Đã xoá sản phẩm khỏi giỏ hàng thành công!",
      );
    });

    // Verify cart switches to empty state
    await waitFor(() => {
      expect(screen.getByTestId("empty-cart-card")).toBeInTheDocument();
      expect(screen.getByTestId("empty-cart-message")).toHaveTextContent(
        "Giỏ hàng của bạn đang trống.",
      );
    });
  });

  it("displays error alert when remove cart item API fails", async () => {
    mockedGetCart.mockResolvedValue(mockCartWithItems);
    mockedRemoveCartItem.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          success: false,
          code: "CART_ITEM_NOT_FOUND",
          message: "Sản phẩm không có trong giỏ hàng",
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
      expect(screen.getByTestId("remove-item-btn-1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("remove-item-btn-1"));
    fireEvent.click(screen.getByTestId("confirm-delete-btn"));

    await waitFor(() => {
      const alert = screen.getByTestId("cart-error-alert");
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent("Sản phẩm không có trong giỏ hàng");
    });
  });
});

describe("US-07.4: Hệ thống tự tính tổng tiền giỏ hàng (tạm tính, phí vận chuyển dự kiến, tổng cộng)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockCartUnderThreshold: cartService.Cart = {
    id: 1,
    totalItems: 1,
    subtotal: 1000000,
    shippingFee: 30000,
    discountAmount: 0,
    total: 1030000,
    items: [
      {
        id: 1,
        variantId: 101,
        productId: 1,
        productName: "Tai nghe Bluetooth",
        sku: "TN-BT-01",
        price: 1000000,
        originalPrice: 1200000,
        quantity: 1,
        availableStock: 10,
        subtotal: 1000000,
      },
    ],
  };

  const mockCartOverThreshold: cartService.Cart = {
    id: 1,
    totalItems: 2,
    subtotal: 51980000,
    shippingFee: 0,
    discountAmount: 0,
    total: 51980000,
    items: [
      {
        id: 1,
        variantId: 101,
        productId: 1,
        productName: "iPhone 15 Pro Max",
        sku: "IP15PM-TITAN-256",
        price: 25990000,
        originalPrice: 28990000,
        quantity: 2,
        availableStock: 10,
        subtotal: 51980000,
      },
    ],
  };

  it("calculates and displays standard shipping fee and subtotal when below free shipping threshold", async () => {
    mockedGetCart.mockResolvedValue(mockCartUnderThreshold);

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
      expect(screen.getByTestId("cart-summary")).toBeInTheDocument();
    });

    expect(screen.getByTestId("cart-total-items")).toHaveTextContent(
      "1 sản phẩm",
    );
    expect(screen.getByTestId("cart-subtotal")).toHaveTextContent(
      "1.000.000 ₫",
    );
    expect(screen.getByTestId("cart-shipping-fee")).toHaveTextContent(
      "30.000 ₫",
    );
    expect(screen.getByTestId("cart-total")).toHaveTextContent("1.030.000 ₫");
    expect(screen.getByTestId("freeship-progress-notice")).toBeInTheDocument();
    expect(screen.getByTestId("freeship-progress-notice")).toHaveTextContent(
      "Mua thêm 4.000.000 ₫ để được Miễn phí vận chuyển!",
    );
  });

  it("calculates and displays free shipping and subtotal when equal or above 5M threshold", async () => {
    mockedGetCart.mockResolvedValue(mockCartOverThreshold);

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
      expect(screen.getByTestId("cart-summary")).toBeInTheDocument();
    });

    expect(screen.getByTestId("cart-total-items")).toHaveTextContent(
      "2 sản phẩm",
    );
    expect(screen.getByTestId("cart-subtotal")).toHaveTextContent(
      "51.980.000 ₫",
    );
    expect(screen.getByTestId("cart-shipping-fee")).toHaveTextContent(
      "Miễn phí",
    );
    expect(screen.getByText("FREE SHIP")).toBeInTheDocument();
    expect(screen.getByTestId("cart-total")).toHaveTextContent("51.980.000 ₫");
    expect(screen.getByTestId("freeship-eligible-notice")).toBeInTheDocument();
    expect(screen.getByTestId("freeship-eligible-notice")).toHaveTextContent(
      "Đơn hàng đủ điều kiện Miễn phí vận chuyển toàn quốc!",
    );
  });

  it("recalculates total and shipping dynamically when quantity increases above free shipping threshold", async () => {
    mockedGetCart.mockResolvedValue(mockCartUnderThreshold);
    mockedUpdateCartItemQuantity.mockResolvedValue({
      id: 1,
      totalItems: 5,
      subtotal: 5000000,
      shippingFee: 0,
      discountAmount: 0,
      total: 5000000,
      items: [
        {
          id: 1,
          variantId: 101,
          productId: 1,
          productName: "Tai nghe Bluetooth",
          sku: "TN-BT-01",
          price: 1000000,
          originalPrice: 1200000,
          quantity: 5,
          availableStock: 10,
          subtotal: 5000000,
        },
      ],
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
      expect(screen.getByTestId("cart-subtotal")).toHaveTextContent(
        "1.000.000 ₫",
      );
      expect(screen.getByTestId("cart-shipping-fee")).toHaveTextContent(
        "30.000 ₫",
      );
      expect(screen.getByTestId("cart-total")).toHaveTextContent("1.030.000 ₫");
    });

    fireEvent.click(screen.getByTestId("increase-qty-btn-1"));

    await waitFor(() => {
      expect(mockedUpdateCartItemQuantity).toHaveBeenCalledWith(1, 2);
    });

    await waitFor(() => {
      expect(screen.getByTestId("cart-subtotal")).toHaveTextContent(
        "5.000.000 ₫",
      );
      expect(screen.getByTestId("cart-shipping-fee")).toHaveTextContent(
        "Miễn phí",
      );
      expect(screen.getByTestId("cart-total")).toHaveTextContent("5.000.000 ₫");
      expect(
        screen.getByTestId("freeship-eligible-notice"),
      ).toBeInTheDocument();
    });
  });

  it("displays discount voucher row if discountAmount is greater than zero", async () => {
    mockedGetCart.mockResolvedValue({
      id: 1,
      totalItems: 1,
      subtotal: 2000000,
      shippingFee: 30000,
      discountAmount: 200000,
      total: 1830000,
      items: [
        {
          id: 1,
          variantId: 101,
          productId: 1,
          productName: "Tai nghe Bluetooth",
          sku: "TN-BT-01",
          price: 2000000,
          originalPrice: 2000000,
          quantity: 1,
          availableStock: 10,
          subtotal: 2000000,
        },
      ],
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
      expect(screen.getByTestId("cart-discount")).toBeInTheDocument();
      expect(screen.getByTestId("cart-discount")).toHaveTextContent(
        "-200.000 ₫",
      );
      expect(screen.getByTestId("cart-total")).toHaveTextContent("1.830.000 ₫");
    });
  });
});

describe("US-07.5: Kiểm tra tồn kho mỗi khi khách hàng thêm/cập nhật số lượng trong giỏ hàng", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("displays stock alert and warning chip when cart item quantity exceeds available stock", async () => {
    const mockCartExceedStock: cartService.Cart = {
      id: 1,
      totalItems: 5,
      subtotal: 129950000,
      shippingFee: 0,
      discountAmount: 0,
      total: 129950000,
      hasStockIssue: true,
      canCheckout: false,
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
          quantity: 5,
          availableStock: 2,
          subtotal: 129950000,
          hasStockIssue: true,
          stockStatusMessage: "Tồn kho không đủ (chỉ còn 2 sản phẩm)",
        },
      ],
    };

    mockedGetCart.mockResolvedValue(mockCartExceedStock);

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
      expect(screen.getByTestId("cart-stock-alert")).toBeInTheDocument();
      expect(
        screen.getByTestId("item-exceed-stock-chip-1"),
      ).toBeInTheDocument();
      expect(screen.getByTestId("item-exceed-stock-chip-1")).toHaveTextContent(
        "Vượt tồn kho",
      );
      expect(screen.getByTestId("stock-error-msg-1")).toHaveTextContent(
        "Tồn kho không đủ (chỉ còn 2 sản phẩm)",
      );
      expect(screen.getByTestId("checkout-btn")).toBeDisabled();
      expect(
        screen.getByTestId("checkout-disabled-reason"),
      ).toBeInTheDocument();
    });
  });

  it("displays out of stock chip and disables checkout when cart item is completely out of stock", async () => {
    const mockCartOutOfStock: cartService.Cart = {
      id: 1,
      totalItems: 2,
      subtotal: 51980000,
      shippingFee: 0,
      discountAmount: 0,
      total: 51980000,
      hasStockIssue: true,
      canCheckout: false,
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
          availableStock: 0,
          subtotal: 51980000,
          hasStockIssue: true,
          stockStatusMessage: "Sản phẩm hiện đã hết hàng",
        },
      ],
    };

    mockedGetCart.mockResolvedValue(mockCartOutOfStock);

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
      expect(screen.getByTestId("cart-stock-alert")).toBeInTheDocument();
      expect(
        screen.getByTestId("item-out-of-stock-chip-1"),
      ).toBeInTheDocument();
      expect(screen.getByTestId("item-out-of-stock-chip-1")).toHaveTextContent(
        "Hết hàng",
      );
      expect(screen.getByTestId("stock-error-msg-1")).toHaveTextContent(
        "Sản phẩm hiện đã hết hàng",
      );
      expect(screen.getByTestId("checkout-btn")).toBeDisabled();
      expect(screen.getByTestId("increase-qty-btn-1")).toBeDisabled();
    });
  });

  it("enables checkout button and removes warning alert after reducing quantity to valid stock", async () => {
    const mockCartExceedStock: cartService.Cart = {
      id: 1,
      totalItems: 4,
      subtotal: 103960000,
      shippingFee: 0,
      discountAmount: 0,
      total: 103960000,
      hasStockIssue: true,
      canCheckout: false,
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
          quantity: 4,
          availableStock: 3,
          subtotal: 103960000,
          hasStockIssue: true,
          stockStatusMessage: "Tồn kho không đủ (chỉ còn 3 sản phẩm)",
        },
      ],
    };

    mockedGetCart.mockResolvedValue(mockCartExceedStock);

    const mockCartResolvedStock: cartService.Cart = {
      id: 1,
      totalItems: 3,
      subtotal: 77970000,
      shippingFee: 0,
      discountAmount: 0,
      total: 77970000,
      hasStockIssue: false,
      canCheckout: true,
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
          quantity: 3,
          availableStock: 3,
          subtotal: 77970000,
          hasStockIssue: false,
          stockStatusMessage: null,
        },
      ],
    };
    mockedUpdateCartItemQuantity.mockResolvedValue(mockCartResolvedStock);

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
      expect(screen.getByTestId("cart-stock-alert")).toBeInTheDocument();
      expect(screen.getByTestId("checkout-btn")).toBeDisabled();
    });

    // Giảm số lượng từ 4 xuống 3
    fireEvent.click(screen.getByTestId("decrease-qty-btn-1"));

    await waitFor(() => {
      expect(mockedUpdateCartItemQuantity).toHaveBeenCalledWith(1, 3);
    });

    await waitFor(() => {
      expect(screen.queryByTestId("cart-stock-alert")).not.toBeInTheDocument();
      expect(
        screen.queryByTestId("item-exceed-stock-chip-1"),
      ).not.toBeInTheDocument();
      expect(screen.getByTestId("checkout-btn")).toBeEnabled();
    });
  });
});

describe("US-07.6: Đồng bộ giỏ hàng tạm khi khách đăng nhập", () => {
  const loggedInAuthValue = {
    user: {
      id: 1,
      email: "user@example.com",
      fullName: "Nguyễn Văn A",
      phone: "0912345678",
      status: "ACTIVE",
      roles: ["CUSTOMER"],
      emailVerified: true,
      createdAt: "2026-09-01T00:00:00Z",
    },
    isAuthenticated: true,
    signIn: vi.fn(),
    signOut: vi.fn(),
    updateUserProfile: vi.fn(),
    clearSession: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetRelatedProducts.mockResolvedValue([]);
    window.localStorage.clear();
  });

  it("gọi API syncCart khi người dùng đăng nhập và cập nhật giỏ hàng", async () => {
    window.localStorage.setItem("techstore.sessionId", "guest_test_123");

    const syncedCart = {
      id: 10,
      totalItems: 3,
      subtotal: 75000000,
      shippingFee: 0,
      discountAmount: 0,
      total: 75000000,
      canCheckout: true,
      items: [
        {
          id: 1,
          variantId: 101,
          productId: 1,
          productName: "iPhone 15 Pro",
          sku: "IP15P-TITAN-128",
          price: 25000000,
          quantity: 3,
          availableStock: 10,
          subtotal: 75000000,
          hasStockIssue: false,
          stockStatusMessage: null,
        },
      ],
    };

    mockedSyncCart.mockResolvedValue({
      cart: syncedCart,
      mergedItemsCount: 2,
      hasStockAdjusted: false,
      message: "Đã đồng bộ 2 sản phẩm vào giỏ hàng thành công",
    });

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={loggedInAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/"]}>
              <StorefrontLayout />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(mockedSyncCart).toHaveBeenCalledWith("guest_test_123");
    });

    await waitFor(() => {
      const badge = screen.getByTestId("header-cart-badge");
      expect(badge).toHaveTextContent("3");
      expect(screen.getByTestId("sync-cart-toast")).toBeInTheDocument();
      expect(screen.getByTestId("sync-cart-toast")).toHaveTextContent("Đã đồng bộ 2 sản phẩm");
    });
  });

  it("hiển thị thông báo điều chỉnh tồn kho khi hasStockAdjusted là true", async () => {
    window.localStorage.setItem("techstore.sessionId", "guest_test_adjusted");

    const syncedCart = {
      id: 10,
      totalItems: 5,
      subtotal: 125000000,
      shippingFee: 0,
      discountAmount: 0,
      total: 125000000,
      canCheckout: true,
      items: [
        {
          id: 1,
          variantId: 101,
          productId: 1,
          productName: "iPhone 15 Pro",
          sku: "IP15P-TITAN-128",
          price: 25000000,
          quantity: 5,
          availableStock: 5,
          subtotal: 125000000,
          hasStockIssue: false,
          stockStatusMessage: null,
        },
      ],
    };

    mockedSyncCart.mockResolvedValue({
      cart: syncedCart,
      mergedItemsCount: 2,
      hasStockAdjusted: true,
      message: "Đã đồng bộ 2 sản phẩm vào giỏ hàng (một số sản phẩm được điều chỉnh theo tồn kho tối đa)",
    });

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={loggedInAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/"]}>
              <StorefrontLayout />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(mockedSyncCart).toHaveBeenCalledWith("guest_test_adjusted");
      expect(screen.getByTestId("sync-cart-toast")).toBeInTheDocument();
      expect(screen.getByTestId("sync-cart-toast")).toHaveTextContent("tồn kho tối đa");
    });
  });

  it("khi chưa đăng nhập, gọi getCart bình thường không gọi syncCart", async () => {
    mockedGetCart.mockResolvedValue({
      id: 99,
      totalItems: 1,
      subtotal: 25000000,
      shippingFee: 0,
      discountAmount: 0,
      total: 25000000,
      canCheckout: true,
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
      expect(mockedGetCart).toHaveBeenCalled();
      expect(mockedSyncCart).not.toHaveBeenCalled();
    });
  });

  it("khi syncCart thất bại, fallback sang refreshCart không làm crash trang", async () => {
    window.localStorage.setItem("techstore.sessionId", "guest_fail_session");
    mockedSyncCart.mockRejectedValue(new Error("Network Error"));
    mockedGetCart.mockResolvedValue({
      id: 5,
      totalItems: 2,
      subtotal: 50000000,
      shippingFee: 0,
      discountAmount: 0,
      total: 50000000,
      canCheckout: true,
      items: [],
    });

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={loggedInAuthValue}>
          <CartProvider>
            <MemoryRouter initialEntries={["/"]}>
              <StorefrontLayout />
            </MemoryRouter>
          </CartProvider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(mockedSyncCart).toHaveBeenCalledWith("guest_fail_session");
      expect(mockedGetCart).toHaveBeenCalled();
    });
  });
});
