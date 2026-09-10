import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AdminPromotionsPage } from "../modules/admin/AdminPromotionsPage";
import { getAdminCategories } from "../services/categoryService";
import { getAdminProducts, getProductVariants } from "../services/productService";
import { getAdminPromotions, type Promotion } from "../services/promotionService";

vi.mock("../services/promotionService", () => ({
  getAdminPromotions: vi.fn(),
  createAdminPromotion: vi.fn(),
  updateAdminPromotion: vi.fn(),
  deleteAdminPromotion: vi.fn(),
}));
vi.mock("../services/productService", () => ({
  getAdminProducts: vi.fn(),
  getProductVariants: vi.fn(),
}));
vi.mock("../services/categoryService", () => ({
  getAdminCategories: vi.fn(),
}));

const mockedGetAdminPromotions = vi.mocked(getAdminPromotions);
const mockedGetAdminProducts = vi.mocked(getAdminProducts);
const mockedGetProductVariants = vi.mocked(getProductVariants);
const mockedGetAdminCategories = vi.mocked(getAdminCategories);

const promotion: Promotion = {
  id: 1,
  name: "Flash sale tháng 9",
  targetType: "PRODUCT",
  productId: 10,
  productName: "Điện thoại Demo",
  variantId: null,
  variantSku: null,
  variantProductId: null,
  categoryId: null,
  categoryName: null,
  discountPercent: 20,
  startsAt: "2026-09-01T00:00:00Z",
  endsAt: "2026-09-30T23:59:59Z",
  active: true,
  createdAt: "2026-08-30T00:00:00Z",
  updatedAt: "2026-08-30T00:00:00Z",
};

describe("US-12.3: AdminPromotionsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetAdminPromotions.mockResolvedValue({ items: [promotion], page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true });
    mockedGetAdminProducts.mockResolvedValue([{ id: 10, name: "Điện thoại Demo", status: "ACTIVE" } as never]);
    mockedGetAdminCategories.mockResolvedValue([{ id: 20, name: "Điện thoại", displayOrder: 1, isActive: true, createdAt: "2026-08-01T00:00:00Z", updatedAt: "2026-08-01T00:00:00Z" }]);
    mockedGetProductVariants.mockResolvedValue([]);
  });

  it("loads promotions and shows active status and target", async () => {
    render(<AdminPromotionsPage />);

    expect(await screen.findByText("Flash sale tháng 9")).toBeInTheDocument();
    expect(screen.getByText("Đang diễn ra")).toBeInTheDocument();
    expect(screen.getByText("Điện thoại Demo")).toBeInTheDocument();
    expect(mockedGetAdminPromotions).toHaveBeenCalledWith(0, 10);
  });

  it("loads the matching category selector when category scope is selected", async () => {
    render(<AdminPromotionsPage />);
    await screen.findByText("Flash sale tháng 9");
    fireEvent.click(screen.getByRole("button", { name: "Tạo chương trình" }));
    fireEvent.change(screen.getByRole("combobox", { name: "Phạm vi áp dụng" }), { target: { value: "CATEGORY" } });

    expect(await screen.findByRole("combobox", { name: "Chọn danh mục" })).toBeInTheDocument();
    expect(screen.queryByRole("combobox", { name: "Chọn sản phẩm" })).not.toBeInTheDocument();
  });

  it("shows API timestamps in local datetime inputs when editing", async () => {
    const timezoneSpy = vi.spyOn(Date.prototype, "getTimezoneOffset").mockReturnValue(-420);

    render(<AdminPromotionsPage />);
    await screen.findByText("Flash sale tháng 9");

    try {
      fireEvent.click(screen.getByRole("button", { name: "Sửa Flash sale tháng 9" }));

      expect(screen.getByDisplayValue("2026-09-01T07:00")).toBeInTheDocument();
      expect(screen.getByDisplayValue("2026-10-01T06:59")).toBeInTheDocument();
    } finally {
      timezoneSpy.mockRestore();
    }
  });
});
