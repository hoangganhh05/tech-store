import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it, vi } from "vitest";
import { OrderReviewStep } from "../modules/checkout/OrderReviewStep";
import { httpClient } from "../services/httpClient";
import type { CheckoutReview, PaymentOption } from "../services/checkoutService";

const payment: PaymentOption = { paymentMethod: "COD", label: "Thanh toán khi nhận hàng (COD)", instructions: "Trả tiền khi nhận hàng." };
const review: CheckoutReview = {
  shippingAddress: { id: 5, recipientName: "Nguyễn Văn A", phone: "0912345678", province: "Hà Nội", district: "Cầu Giấy", ward: "Dịch Vọng", streetAddress: "1 Duy Tân", isDefault: true, createdAt: "2026-09-08T00:00:00Z" },
  paymentMethod: payment,
  readyToPlaceOrder: true,
  cart: { id: 10, totalItems: 2, subtotal: 50000000, shippingFee: 30000, discountAmount: 100000, total: 49930000, canCheckout: true, items: [
    { id: 1, variantId: 101, productId: 1, productName: "iPhone 15 Pro", sku: "IP15P", color: "Titan tự nhiên", storage: "256GB", price: 25000000, quantity: 2, availableStock: 3, subtotal: 50000000 },
  ] },
};
let mock: MockAdapter;
afterEach(() => mock.restore());

describe("US-08.3 order review", () => {
  it("loads and displays products, variant, address, payment and every total", async () => {
    mock = new MockAdapter(httpClient);
    mock.onPost("/checkout/review", { addressId: 5, paymentMethod: "COD" }).reply(200, { data: review });
    render(<OrderReviewStep addressId={5} paymentOption={payment} onEditAddress={vi.fn()} onEditPayment={vi.fn()} onPlaced={vi.fn()} />);
    expect(screen.getByText("Đang kiểm tra đơn hàng...")).toBeInTheDocument();
    expect(await screen.findByText("iPhone 15 Pro")).toBeInTheDocument();
    expect(screen.getByText("Titan tự nhiên · 256GB")).toBeInTheDocument();
    expect(screen.getByText(/1 Duy Tân, Dịch Vọng, Cầu Giấy, Hà Nội/)).toBeInTheDocument();
    expect(screen.getByText(payment.label)).toBeInTheDocument();
    expect(screen.getByTestId("review-totals")).toHaveTextContent("Tạm tính");
    expect(screen.getByTestId("review-totals")).toHaveTextContent("Phí vận chuyển");
    expect(screen.getByTestId("review-totals")).toHaveTextContent("Giảm giá");
    expect(screen.getByTestId("review-totals")).toHaveTextContent("Tổng cộng");
    expect(screen.getByTestId("place-order-btn")).toBeEnabled();
  });

  it("allows editing both address and payment", async () => {
    mock = new MockAdapter(httpClient);
    mock.onPost("/checkout/review").reply(200, { data: review });
    const editAddress = vi.fn(); const editPayment = vi.fn();
    render(<OrderReviewStep addressId={5} paymentOption={payment} onEditAddress={editAddress} onEditPayment={editPayment} onPlaced={vi.fn()} />);
    const buttons = await screen.findAllByRole("button", { name: "Thay đổi" });
    fireEvent.click(buttons[0]); fireEvent.click(buttons[1]);
    expect(editAddress).toHaveBeenCalledOnce(); expect(editPayment).toHaveBeenCalledOnce();
  });

  it("disables placing an invalid order and shows the stock warning", async () => {
    mock = new MockAdapter(httpClient);
    mock.onPost("/checkout/review").reply(200, { data: { ...review, readyToPlaceOrder: false } });
    render(<OrderReviewStep addressId={5} paymentOption={payment} onEditAddress={vi.fn()} onEditPayment={vi.fn()} onPlaced={vi.fn()} />);
    expect(await screen.findByText(/không còn đủ tồn kho/)).toBeInTheDocument();
    expect(screen.getByTestId("place-order-btn")).toBeDisabled();
  });

  it("shows an error and retries the review request", async () => {
    mock = new MockAdapter(httpClient);
    mock.onPost("/checkout/review").replyOnce(500).onPost("/checkout/review").reply(200, { data: review });
    render(<OrderReviewStep addressId={5} paymentOption={payment} onEditAddress={vi.fn()} onEditPayment={vi.fn()} onPlaced={vi.fn()} />);
    fireEvent.click(await screen.findByRole("button", { name: "Thử lại" }));
    await waitFor(() => expect(screen.getByTestId("order-review-content")).toBeInTheDocument());
    expect(mock.history.post).toHaveLength(2);
  });

  it("places a valid order and returns its order number", async () => {
    mock = new MockAdapter(httpClient);
    mock.onPost("/checkout/review").reply(200, { data: review });
    mock.onPost("/orders", { addressId: 5, paymentMethod: "COD" }).reply(200, {
      data: { id: 55, orderNumber: "TS-ABC123", status: "PENDING", totalAmount: 49930000, placedAt: "2026-09-08T00:00:00Z" },
    });
    const onPlaced = vi.fn();
    render(<OrderReviewStep addressId={5} paymentOption={payment} onEditAddress={vi.fn()} onEditPayment={vi.fn()} onPlaced={onPlaced} />);
    fireEvent.click(await screen.findByTestId("place-order-btn"));
    await waitFor(() => expect(onPlaced).toHaveBeenCalledWith("TS-ABC123"));
  });
});
