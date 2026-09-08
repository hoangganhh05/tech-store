import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { appTheme } from "../configs/theme";
import { CheckoutPage } from "../modules/checkout/CheckoutPage";
import { CartContext } from "../modules/cart/CartStore";
import { AuthContext } from "../modules/auth/AuthStore";
import * as userService from "../services/userService";
import type { Cart } from "../services/cartService";
import * as checkoutService from "../services/checkoutService";

const paymentOption: checkoutService.PaymentOption = {
  paymentMethod: "COD", label: "Thanh toán khi nhận hàng (COD)", instructions: "Trả tiền khi nhận hàng.",
};

vi.mock("../services/checkoutService", () => ({
  getPaymentMethods: vi.fn(),
  selectPaymentMethod: vi.fn(),
  getCheckoutReview: vi.fn(),
}));

vi.mock("../services/userService", () => ({
  getMyAddresses: vi.fn(),
  addMyAddress: vi.fn(),
  updateMyAddress: vi.fn(),
  deleteMyAddress: vi.fn(),
  setDefaultAddress: vi.fn(),
}));

const mockedGetMyAddresses = vi.mocked(userService.getMyAddresses);
const mockedAddMyAddress = vi.mocked(userService.addMyAddress);

const mockUser = {
  id: 1,
  email: "customer@example.com",
  fullName: "Nguyễn Văn A",
  phone: "0912345678",
  status: "ACTIVE",
  roles: ["CUSTOMER"],
  emailVerified: true,
  createdAt: "2026-09-01T00:00:00Z",
};

const mockAuthValue = {
  user: mockUser,
  isAuthenticated: true,
  signIn: vi.fn(),
  signOut: vi.fn(),
  updateUserProfile: vi.fn(),
  clearSession: vi.fn(),
};

const mockCart: Cart = {
  id: 10,
  totalItems: 2,
  subtotal: 50000000,
  shippingFee: 0,
  discountAmount: 0,
  total: 50000000,
  canCheckout: true,
  items: [
    {
      id: 1,
      variantId: 101,
      productId: 1,
      productName: "iPhone 15 Pro",
      sku: "IP15P-TITAN-128",
      price: 25000000,
      quantity: 2,
      availableStock: 5,
      subtotal: 50000000,
      hasStockIssue: false,
      stockStatusMessage: null,
    },
  ],
};

const mockCartContextValue = {
  cart: mockCart,
  cartCount: 2,
  loading: false,
  syncNotification: null,
  clearSyncNotification: vi.fn(),
  addToCart: vi.fn(),
  updateQuantity: vi.fn(),
  removeCartItem: vi.fn(),
  refreshCart: vi.fn(),
  syncGuestCart: vi.fn(),
};

const mockAddresses: userService.Address[] = [
  {
    id: 101,
    recipientName: "Nguyễn Văn A (Nhà riêng)",
    phone: "0912345678",
    province: "Hà Nội",
    district: "Cầu Giấy",
    ward: "Dịch Vọng",
    streetAddress: "Số 1 Duy Tân",
    isDefault: false,
    createdAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 102,
    recipientName: "Nguyễn Văn A (Công ty)",
    phone: "0987654321",
    province: "Hà Nội",
    district: "Ba Đình",
    ward: "Kim Mã",
    streetAddress: "Số 10 Liễu Giai",
    isDefault: true,
    createdAt: "2026-09-02T00:00:00Z",
  },
];

describe("US-08.1: Chọn hoặc nhập địa chỉ giao hàng ở bước checkout", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(checkoutService.getPaymentMethods).mockResolvedValue([paymentOption]);
    vi.mocked(checkoutService.selectPaymentMethod).mockResolvedValue(paymentOption);
    vi.mocked(checkoutService.getCheckoutReview).mockResolvedValue({
      cart: mockCart,
      shippingAddress: mockAddresses[1],
      paymentMethod: paymentOption,
      readyToPlaceOrder: true,
    });
  });

  it("hiển thị danh sách địa chỉ đã lưu và tự động chọn địa chỉ mặc định", async () => {
    mockedGetMyAddresses.mockResolvedValue(mockAddresses);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartContext.Provider value={mockCartContextValue}>
            <MemoryRouter initialEntries={["/checkout"]}>
              <CheckoutPage />
            </MemoryRouter>
          </CartContext.Provider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    // Chờ danh sách địa chỉ hiển thị
    await waitFor(() => {
      expect(screen.getByText("1. Địa chỉ giao hàng")).toBeInTheDocument();
      expect(screen.getByText("Nguyễn Văn A (Nhà riêng)")).toBeInTheDocument();
      expect(screen.getByText("Nguyễn Văn A (Công ty)")).toBeInTheDocument();
    });

    // Địa chỉ mặc định (id 102) phải có nhãn "Mặc định" và được chọn tự động
    expect(screen.getByTestId("default-chip-102")).toBeInTheDocument();
    const radio102 = screen
      .getByTestId("address-radio-102")
      .querySelector("input");
    const radio101 = screen
      .getByTestId("address-radio-101")
      .querySelector("input");

    expect(radio102).toBeChecked();
    expect(radio101).not.toBeChecked();

    // Nút tiếp tục phải được kích hoạt
    const continueBtn = screen.getByTestId("continue-to-payment-btn");
    expect(continueBtn).toBeEnabled();

    // Tóm tắt đơn hàng hiển thị chuẩn
    expect(screen.getByTestId("checkout-order-summary")).toBeInTheDocument();
    expect(screen.getByTestId("checkout-total-amount")).toHaveTextContent(
      "50.000.000",
    );
  });

  it("cho phép người dùng chọn địa chỉ khác trong danh sách", async () => {
    mockedGetMyAddresses.mockResolvedValue(mockAddresses);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartContext.Provider value={mockCartContextValue}>
            <MemoryRouter initialEntries={["/checkout"]}>
              <CheckoutPage />
            </MemoryRouter>
          </CartContext.Provider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("Nguyễn Văn A (Nhà riêng)")).toBeInTheDocument();
    });

    // Click chọn địa chỉ id 101
    fireEvent.click(screen.getByTestId("address-card-101"));

    const radio101 = screen
      .getByTestId("address-radio-101")
      .querySelector("input");
    const radio102 = screen
      .getByTestId("address-radio-102")
      .querySelector("input");

    expect(radio101).toBeChecked();
    expect(radio102).not.toBeChecked();
  });

  it("vô hiệu hoá nút tiếp tục khi tài khoản chưa có địa chỉ giao hàng nào", async () => {
    mockedGetMyAddresses.mockResolvedValue([]);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartContext.Provider value={mockCartContextValue}>
            <MemoryRouter initialEntries={["/checkout"]}>
              <CheckoutPage />
            </MemoryRouter>
          </CartContext.Provider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByText(/Bạn chưa có địa chỉ giao hàng nào trong tài khoản/i),
      ).toBeInTheDocument();
    });

    const continueBtn = screen.getByTestId("continue-to-payment-btn");
    expect(continueBtn).toBeDisabled();
    expect(screen.getByTestId("add-first-address-btn")).toBeInTheDocument();
  });

  it("cho phép mở modal thêm địa chỉ mới và tự động chọn địa chỉ mới vừa lưu", async () => {
    mockedGetMyAddresses.mockResolvedValue([mockAddresses[0]]);

    const newCreatedAddress: userService.Address = {
      id: 999,
      recipientName: "Trần Thị Mới",
      phone: "0909999888",
      province: "Đà Nẵng",
      district: "Hải Châu",
      ward: "Bình Hiên",
      streetAddress: "Số 55 Nguyễn Văn Linh",
      isDefault: false,
      createdAt: "2026-09-08T00:00:00Z",
    };
    mockedAddMyAddress.mockResolvedValue(newCreatedAddress);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartContext.Provider value={mockCartContextValue}>
            <MemoryRouter initialEntries={["/checkout"]}>
              <CheckoutPage />
            </MemoryRouter>
          </CartContext.Provider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("Nguyễn Văn A (Nhà riêng)")).toBeInTheDocument();
    });

    // Mở modal thêm địa chỉ mới
    fireEvent.click(screen.getByTestId("add-new-address-btn"));

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: "Thêm địa chỉ mới" }),
      ).toBeInTheDocument();
    });

    // Điền form
    fireEvent.change(screen.getByLabelText(/Người nhận/i), {
      target: { value: "Trần Thị Mới" },
    });
    fireEvent.change(screen.getByLabelText(/Số điện thoại/i), {
      target: { value: "0909999888" },
    });
    fireEvent.change(screen.getByLabelText(/Tỉnh\/Thành phố/i), {
      target: { value: "Đà Nẵng" },
    });
    fireEvent.change(screen.getByLabelText(/Quận\/Huyện/i), {
      target: { value: "Hải Châu" },
    });
    fireEvent.change(screen.getByLabelText(/Phường\/Xã/i), {
      target: { value: "Bình Hiên" },
    });
    fireEvent.change(screen.getByLabelText(/Địa chỉ chi tiết/i), {
      target: { value: "Số 55 Nguyễn Văn Linh" },
    });

    // Submit form
    fireEvent.click(screen.getByRole("button", { name: "Thêm địa chỉ" }));

    await waitFor(() => {
      expect(mockedAddMyAddress).toHaveBeenCalledWith({
        recipientName: "Trần Thị Mới",
        phone: "0909999888",
        province: "Đà Nẵng",
        district: "Hải Châu",
        ward: "Bình Hiên",
        streetAddress: "Số 55 Nguyễn Văn Linh",
      });
    });

    // Địa chỉ mới phải xuất hiện trong danh sách và được tự động chọn
    await waitFor(() => {
      expect(screen.getByText("Trần Thị Mới")).toBeInTheDocument();
      const radio999 = screen
        .getByTestId("address-radio-999")
        .querySelector("input");
      expect(radio999).toBeChecked();
    });
  });

  it("chuyển sang bước tiếp theo khi bấm nút tiếp tục và có thể quay lại", async () => {
    mockedGetMyAddresses.mockResolvedValue(mockAddresses);

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartContext.Provider value={mockCartContextValue}>
            <MemoryRouter initialEntries={["/checkout"]}>
              <CheckoutPage />
            </MemoryRouter>
          </CartContext.Provider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("continue-to-payment-btn")).toBeEnabled();
    });

    // Bấm tiếp tục
    fireEvent.click(screen.getByTestId("continue-to-payment-btn"));

    // Bước 2: Phương thức thanh toán được hiển thị
    await waitFor(() => {
      expect(screen.getByText("2. Phương thức thanh toán")).toBeInTheDocument();
      expect(screen.getByText(/Đã chọn địa chỉ:/i)).toBeInTheDocument();
    });

    fireEvent.click(await screen.findByRole("radio", { name: paymentOption.label }));

    // Bấm quay lại bước chọn địa chỉ
    fireEvent.click(screen.getByTestId("back-to-address-btn"));

    await waitFor(() => {
      expect(screen.getByText("1. Địa chỉ giao hàng")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("continue-to-payment-btn"));
    expect(await screen.findByRole("radio", { name: paymentOption.label })).toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "Tiếp tục xem lại đơn hàng" }));
    expect(await screen.findByTestId("order-review-content")).toBeInTheDocument();
    expect(checkoutService.selectPaymentMethod).toHaveBeenCalledWith("COD");
    fireEvent.click(screen.getAllByRole("button", { name: "Thay đổi" })[1]);
    expect(await screen.findByRole("radio", { name: paymentOption.label })).toBeChecked();
  });

  it("hiển thị cảnh báo và nút về trang sản phẩm khi giỏ hàng trống", async () => {
    mockedGetMyAddresses.mockResolvedValue([]);
    const emptyCartContextValue = {
      ...mockCartContextValue,
      cart: {
        id: 1,
        totalItems: 0,
        subtotal: 0,
        shippingFee: 0,
        discountAmount: 0,
        total: 0,
        items: [],
      },
      cartCount: 0,
    };

    render(
      <ThemeProvider theme={appTheme}>
        <AuthContext.Provider value={mockAuthValue}>
          <CartContext.Provider value={emptyCartContextValue}>
            <MemoryRouter initialEntries={["/checkout"]}>
              <CheckoutPage />
            </MemoryRouter>
          </CartContext.Provider>
        </AuthContext.Provider>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByText("Giỏ hàng của bạn đang trống"),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /Xem danh sách sản phẩm/i }),
      ).toBeInTheDocument();
    });
  });
});
