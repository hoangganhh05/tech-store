import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { appTheme } from "../configs/theme";
import { AdminOrderDetailPage } from "../modules/admin/AdminOrderDetailPage";
import {
  getAdminOrderDetail,
  updateAdminOrderStatus,
  type AdminOrderDetail,
} from "../services/adminOrderService";

vi.mock("../services/adminOrderService", () => ({
  getAdminOrderDetail: vi.fn(),
  updateAdminOrderStatus: vi.fn(),
}));

const mockedGetAdminOrderDetail = vi.mocked(getAdminOrderDetail);
const mockedUpdateAdminOrderStatus = vi.mocked(updateAdminOrderStatus);

const baseOrder: AdminOrderDetail = {
  id: 15,
  orderNumber: "TS-ADMIN-STATUS-15",
  customer: {
    id: 8,
    fullName: "Trần Văn B",
    email: "tranvanb@example.com",
    phone: "0901234567",
  },
  status: "PENDING",
  paymentMethod: "COD",
  paymentStatus: "UNPAID",
  cancellationReason: null,
  internalNote: "Đơn hàng mới",
  subtotal: 500000,
  discountAmount: 0,
  shippingFee: 30000,
  totalAmount: 530000,
  placedAt: "2026-09-09T10:00:00Z",
  shippingAddress: {
    recipientName: "Trần Văn B",
    recipientPhone: "0901234567",
    line1: "100 Cầu Giấy",
    ward: "Quan Hoa",
    district: "Cầu Giấy",
    province: "Hà Nội",
  },
  items: [
    {
      productName: "Tai nghe Bluetooth",
      sku: "EARPHONE-01",
      variantLabel: "Trắng",
      unitPrice: 500000,
      quantity: 1,
      subtotal: 500000,
    },
  ],
  statusHistory: [
    { status: "PENDING", changedAt: "2026-09-09T10:00:00Z", changedBy: null },
  ],
};

function renderOrderDetailPage(orderId = 15) {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={[`/admin/orders/${orderId}`]}>
        <Routes>
          <Route path="/admin/orders/:id" element={<AdminOrderDetailPage />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-09.6: Cập nhật trạng thái đơn hàng (admin)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị dropdown với các trạng thái hợp lệ khi đơn đang ở PENDING (Xác nhận, Huỷ)", async () => {
    mockedGetAdminOrderDetail.mockResolvedValueOnce({
      ...baseOrder,
      status: "PENDING",
    });
    renderOrderDetailPage();

    await screen.findByText("TS-ADMIN-STATUS-15");
    expect(
      screen.getByRole("heading", { name: "Cập nhật trạng thái" }),
    ).toBeInTheDocument();

    // Nút cập nhật trạng thái bị vô hiệu khi chưa chọn trạng thái mới
    const updateBtn = screen.getByRole("button", {
      name: "Cập nhật trạng thái",
    });
    expect(updateBtn).toBeDisabled();

    // Mở dropdown chọn trạng thái
    fireEvent.mouseDown(
      screen.getByRole("combobox", { name: "Trạng thái mới" }),
    );
    expect(
      await screen.findByRole("option", { name: "Xác nhận đơn hàng" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: "Huỷ đơn hàng" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Chuyển sang đang giao" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Hoàn thành đơn hàng" }),
    ).not.toBeInTheDocument();
  });

  it("hiển thị các tuỳ chọn hợp lệ khi đơn đang ở CONFIRMED (Đang giao, Huỷ)", async () => {
    mockedGetAdminOrderDetail.mockResolvedValueOnce({
      ...baseOrder,
      status: "CONFIRMED",
      statusHistory: [
        {
          status: "PENDING",
          changedAt: "2026-09-09T10:00:00Z",
          changedBy: null,
        },
        {
          status: "CONFIRMED",
          changedAt: "2026-09-09T10:15:00Z",
          changedBy: { id: 1, fullName: "Admin QTV" },
        },
      ],
    });
    renderOrderDetailPage();

    await screen.findByText("TS-ADMIN-STATUS-15");
    fireEvent.mouseDown(
      screen.getByRole("combobox", { name: "Trạng thái mới" }),
    );
    expect(
      await screen.findByRole("option", { name: "Chuyển sang đang giao" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: "Huỷ đơn hàng" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Xác nhận đơn hàng" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Hoàn thành đơn hàng" }),
    ).not.toBeInTheDocument();
  });

  it("hiển thị tuỳ chọn hợp lệ khi đơn đang ở SHIPPING (Hoàn thành)", async () => {
    mockedGetAdminOrderDetail.mockResolvedValueOnce({
      ...baseOrder,
      status: "SHIPPING",
      statusHistory: [
        {
          status: "PENDING",
          changedAt: "2026-09-09T10:00:00Z",
          changedBy: null,
        },
        {
          status: "CONFIRMED",
          changedAt: "2026-09-09T10:15:00Z",
          changedBy: { id: 1, fullName: "Admin QTV" },
        },
        {
          status: "SHIPPING",
          changedAt: "2026-09-09T10:30:00Z",
          changedBy: { id: 1, fullName: "Admin QTV" },
        },
      ],
    });
    renderOrderDetailPage();

    await screen.findByText("TS-ADMIN-STATUS-15");
    fireEvent.mouseDown(
      screen.getByRole("combobox", { name: "Trạng thái mới" }),
    );
    expect(
      await screen.findByRole("option", { name: "Hoàn thành đơn hàng" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Huỷ đơn hàng" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Xác nhận đơn hàng" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Chuyển sang đang giao" }),
    ).not.toBeInTheDocument();
  });

  it("hiển thị thông báo khi đơn hàng ở trạng thái cuối (COMPLETED hoặc CANCELLED)", async () => {
    mockedGetAdminOrderDetail.mockResolvedValueOnce({
      ...baseOrder,
      status: "COMPLETED",
      statusHistory: [
        {
          status: "PENDING",
          changedAt: "2026-09-09T10:00:00Z",
          changedBy: null,
        },
        {
          status: "COMPLETED",
          changedAt: "2026-09-09T11:00:00Z",
          changedBy: { id: 1, fullName: "Admin QTV" },
        },
      ],
    });
    renderOrderDetailPage();

    await screen.findByText("TS-ADMIN-STATUS-15");
    expect(
      screen.getByText("Đơn hàng ở trạng thái cuối, không thể cập nhật thêm."),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("combobox", { name: "Trạng thái mới" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Cập nhật trạng thái" }),
    ).not.toBeInTheDocument();
  });

  it("cập nhật trạng thái thành công: gọi API, hiển thị thông báo và cập nhật dòng thời gian kèm người thực hiện", async () => {
    mockedGetAdminOrderDetail.mockResolvedValueOnce({
      ...baseOrder,
      status: "PENDING",
    });
    const updatedOrder: AdminOrderDetail = {
      ...baseOrder,
      status: "CONFIRMED",
      statusHistory: [
        {
          status: "PENDING",
          changedAt: "2026-09-09T10:00:00Z",
          changedBy: null,
        },
        {
          status: "CONFIRMED",
          changedAt: "2026-09-09T10:20:00Z",
          changedBy: { id: 2, fullName: "Quản trị viên kho" },
        },
      ],
    };
    mockedUpdateAdminOrderStatus.mockResolvedValueOnce(updatedOrder);

    renderOrderDetailPage();

    await screen.findByText("TS-ADMIN-STATUS-15");
    fireEvent.mouseDown(
      screen.getByRole("combobox", { name: "Trạng thái mới" }),
    );
    fireEvent.click(
      await screen.findByRole("option", { name: "Xác nhận đơn hàng" }),
    );

    const updateBtn = screen.getByRole("button", {
      name: "Cập nhật trạng thái",
    });
    expect(updateBtn).toBeEnabled();
    fireEvent.click(updateBtn);

    await waitFor(() => {
      expect(mockedUpdateAdminOrderStatus).toHaveBeenCalledWith(
        15,
        "CONFIRMED",
      );
    });

    expect(
      await screen.findByText("Đã cập nhật trạng thái đơn hàng."),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Cập nhật bởi Quản trị viên kho/),
    ).toBeInTheDocument();
  });

  it("hiển thị thông báo lỗi khi API cập nhật trạng thái trả về lỗi", async () => {
    mockedGetAdminOrderDetail.mockResolvedValueOnce({
      ...baseOrder,
      status: "PENDING",
    });
    mockedUpdateAdminOrderStatus.mockRejectedValueOnce(
      Object.assign(new Error("Validation error"), {
        isAxiosError: true,
        response: {
          data: {
            message: "Không thể chuyển trạng thái từ PENDING sang COMPLETED",
          },
        },
      }),
    );

    renderOrderDetailPage();

    await screen.findByText("TS-ADMIN-STATUS-15");
    fireEvent.mouseDown(
      screen.getByRole("combobox", { name: "Trạng thái mới" }),
    );
    fireEvent.click(
      await screen.findByRole("option", { name: "Xác nhận đơn hàng" }),
    );

    fireEvent.click(
      screen.getByRole("button", { name: "Cập nhật trạng thái" }),
    );

    await waitFor(() => {
      expect(mockedUpdateAdminOrderStatus).toHaveBeenCalledWith(
        15,
        "CONFIRMED",
      );
    });

    expect(
      await screen.findByText(
        "Không thể chuyển trạng thái từ PENDING sang COMPLETED",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Cập nhật trạng thái" }),
    ).toBeEnabled();
  });
});
