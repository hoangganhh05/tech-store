import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { OrderDetailPage } from "../modules/orders/OrderDetailPage";
import { getOrderDetail, type OrderDetail } from "../services/orderService";
import {
  getMyProductReview,
  submitProductReview,
} from "../services/reviewService";

vi.mock("../services/orderService", () => ({
  getOrderDetail: vi.fn(),
  cancelOrder: vi.fn(),
}));

vi.mock("../services/reviewService", () => ({
  getMyProductReview: vi.fn(),
  submitProductReview: vi.fn(),
}));

const mockedGetOrderDetail = vi.mocked(getOrderDetail);
const mockedGetMyProductReview = vi.mocked(getMyProductReview);
const mockedSubmitProductReview = vi.mocked(submitProductReview);

const completedOrder: OrderDetail = {
  id: 15,
  orderNumber: "TS-COMPLETED-15",
  status: "COMPLETED",
  paymentMethod: "COD",
  paymentStatus: "PAID",
  cancellationReason: null,
  subtotal: 20000000,
  discountAmount: 0,
  shippingFee: 0,
  totalAmount: 20000000,
  placedAt: "2026-09-09T10:00:00Z",
  shippingAddress: {
    recipientName: "Trần Thị B",
    recipientPhone: "0912345678",
    line1: "123 Cầu Giấy",
    ward: "Quan Hoa",
    district: "Cầu Giấy",
    province: "Hà Nội",
  },
  items: [
    {
      productId: 101,
      productName: "iPhone 15 Pro",
      sku: "IP15P-BLUE-128",
      variantLabel: "Xanh Titan / 128GB",
      unitPrice: 20000000,
      quantity: 1,
      subtotal: 20000000,
    },
  ],
  statusHistory: [
    { status: "PENDING", changedAt: "2026-09-09T10:00:00Z" },
    { status: "COMPLETED", changedAt: "2026-09-09T14:00:00Z" },
  ],
};

function renderPage(path = "/account/orders/15") {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/account/orders/:id" element={<OrderDetailPage />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-11.1: Order Review Flow", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetOrderDetail.mockResolvedValue(completedOrder);
    mockedGetMyProductReview.mockResolvedValue({
      canReview: true,
      myReview: null,
    });
    mockedSubmitProductReview.mockResolvedValue({
      id: 1,
      productId: 101,
      userId: 1,
      userFullName: "Trần Thị B",
      rating: 5,
      comment: "Sản phẩm rất tốt!",
      status: "APPROVED",
      createdAt: "2026-09-09T15:00:00Z",
      updatedAt: "2026-09-09T15:00:00Z",
    });
  });

  it("does not show review button when order is not COMPLETED", async () => {
    mockedGetOrderDetail.mockResolvedValueOnce({
      ...completedOrder,
      status: "SHIPPING",
    });

    renderPage();

    expect(await screen.findByText("TS-COMPLETED-15")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /đánh giá/i }),
    ).not.toBeInTheDocument();
  });

  it("shows review button for each product when order is COMPLETED", async () => {
    renderPage();

    expect(await screen.findByText("TS-COMPLETED-15")).toBeInTheDocument();
    const reviewBtn = screen.getByRole("button", { name: /đánh giá/i });
    expect(reviewBtn).toBeInTheDocument();
  });

  it("opens dialog, fills review, and submits successfully", async () => {
    renderPage();

    const reviewBtn = await screen.findByRole("button", { name: /đánh giá/i });
    fireEvent.click(reviewBtn);

    const dialog = await screen.findByRole("dialog");
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByText("Đánh giá sản phẩm")).toBeInTheDocument();
    expect(within(dialog).getByText("iPhone 15 Pro")).toBeInTheDocument();
    expect(
      within(dialog).getByText("Phân loại: Xanh Titan / 128GB"),
    ).toBeInTheDocument();

    // Comment field
    const commentInput =
      within(dialog).getByPlaceholderText(/chia sẻ cảm nhận/i);
    fireEvent.change(commentInput, { target: { value: "Sản phẩm rất tốt!" } });

    // Submit button
    const submitBtn = within(dialog).getByRole("button", {
      name: "Gửi đánh giá",
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedSubmitProductReview).toHaveBeenCalledWith(101, {
        rating: 5,
        comment: "Sản phẩm rất tốt!",
      });
    });

    expect(
      await screen.findByText("Gửi đánh giá sản phẩm thành công!"),
    ).toBeInTheDocument();
    await waitFor(
      () => expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
      { timeout: 2000 },
    );
  });

  it("prefills existing review and allows updating", async () => {
    mockedGetMyProductReview.mockResolvedValueOnce({
      canReview: true,
      myReview: {
        id: 2,
        productId: 101,
        userId: 1,
        userFullName: "Trần Thị B",
        rating: 4,
        comment: "Dùng ổn áp",
        status: "APPROVED",
        createdAt: "2026-09-08T10:00:00Z",
        updatedAt: "2026-09-08T10:00:00Z",
      },
    });

    renderPage();

    const reviewBtn = await screen.findByRole("button", { name: /đánh giá/i });
    fireEvent.click(reviewBtn);

    const dialog = await screen.findByRole("dialog");
    expect(
      within(dialog).getByText("Chỉnh sửa đánh giá sản phẩm"),
    ).toBeInTheDocument();
    const commentInput = within(dialog).getByDisplayValue("Dùng ổn áp");
    expect(commentInput).toBeInTheDocument();

    fireEvent.change(commentInput, {
      target: { value: "Dùng rất hài lòng sau vài hôm" },
    });

    const updateBtn = within(dialog).getByRole("button", {
      name: "Cập nhật đánh giá",
    });
    fireEvent.click(updateBtn);

    await waitFor(() => {
      expect(mockedSubmitProductReview).toHaveBeenCalledWith(101, {
        rating: 4,
        comment: "Dùng rất hài lòng sau vài hôm",
      });
    });

    expect(
      await screen.findByText("Cập nhật đánh giá thành công!"),
    ).toBeInTheDocument();
    await waitFor(
      () => expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
      { timeout: 2000 },
    );
  });

  it("shows warning alert when user is not eligible to review", async () => {
    mockedGetMyProductReview.mockResolvedValueOnce({
      canReview: false,
      myReview: null,
    });

    renderPage();

    const reviewBtn = await screen.findByRole("button", { name: /đánh giá/i });
    fireEvent.click(reviewBtn);

    expect(
      await screen.findByText(
        /Chỉ khách hàng có đơn hàng đã hoàn thành chứa sản phẩm này mới được đánh giá/i,
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Gửi đánh giá" }),
    ).not.toBeInTheDocument();
  });
});
