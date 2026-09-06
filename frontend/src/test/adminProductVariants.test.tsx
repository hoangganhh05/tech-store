import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AdminProductVariantsDialog } from "../modules/admin/AdminProductVariantsDialog";
import { AdminProductsPage } from "../modules/admin/AdminProductsPage";
import {
  createProductVariant,
  deleteProductVariant,
  getAdminProducts,
  getProductVariants,
  updateProductVariant,
  type Product,
  type ProductVariant,
} from "../services/productService";
import { getAdminBrands } from "../services/brandService";
import { getAdminCategories } from "../services/categoryService";

vi.mock("../services/productService", () => ({
  getAdminProducts: vi.fn(),
  getAdminProductById: vi.fn(),
  createAdminProduct: vi.fn(),
  getProductVariants: vi.fn(),
  getProductVariantById: vi.fn(),
  createProductVariant: vi.fn(),
  updateProductVariant: vi.fn(),
  deleteProductVariant: vi.fn(),
}));

vi.mock("../services/brandService", () => ({
  getAdminBrands: vi.fn(),
}));

vi.mock("../services/categoryService", () => ({
  getAdminCategories: vi.fn(),
}));

const mockedGetAdminProducts = vi.mocked(getAdminProducts);
const mockedGetAdminBrands = vi.mocked(getAdminBrands);
const mockedGetAdminCategories = vi.mocked(getAdminCategories);
const mockedGetProductVariants = vi.mocked(getProductVariants);
const mockedCreateProductVariant = vi.mocked(createProductVariant);
const mockedUpdateProductVariant = vi.mocked(updateProductVariant);
const mockedDeleteProductVariant = vi.mocked(deleteProductVariant);

const mockProduct: Product = {
  id: 1,
  name: "iPhone 16 Pro Max",
  description: "Flagship mới",
  brandId: 10,
  brandName: "Apple",
  categoryId: 20,
  categoryName: "Điện thoại",
  status: "DRAFT",
  createdAt: "2026-09-01T00:00:00Z",
  updatedAt: "2026-09-01T00:00:00Z",
};

const mockVariants: ProductVariant[] = [
  {
    id: 101,
    productId: 1,
    productName: "iPhone 16 Pro Max",
    sku: "IP16PM-256-DESERT",
    color: "Sa mạc titan",
    storage: "256GB",
    price: 28990000,
    originalPrice: 31990000,
    stockQuantity: 50,
    status: "ACTIVE",
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 102,
    productId: 1,
    productName: "iPhone 16 Pro Max",
    sku: "IP16PM-512-BLACK",
    color: "Đen không gian",
    storage: "512GB",
    price: 34990000,
    originalPrice: null,
    stockQuantity: 20,
    status: "INACTIVE",
    createdAt: "2026-09-02T00:00:00Z",
    updatedAt: "2026-09-02T00:00:00Z",
  },
];

function renderVariantsDialog(props?: {
  open?: boolean;
  product?: Product | null;
  onClose?: () => void;
  onVariantsChanged?: () => void;
}) {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <AdminProductVariantsDialog
          open={props?.open ?? true}
          product={props?.product ?? mockProduct}
          onClose={props?.onClose ?? vi.fn()}
          onVariantsChanged={props?.onVariantsChanged}
        />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("AdminProductVariantsDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetProductVariants.mockResolvedValue(mockVariants);
    mockedGetAdminProducts.mockResolvedValue([mockProduct]);
    mockedGetAdminBrands.mockResolvedValue([
      {
        id: 10,
        name: "Apple",
        logoUrl: null,
        description: null,
        createdAt: "2026-09-01T00:00:00Z",
        updatedAt: "2026-09-01T00:00:00Z",
      },
    ]);
    mockedGetAdminCategories.mockResolvedValue([
      {
        id: 20,
        name: "Điện thoại",
        description: null,
        parentId: null,
        parentName: null,
        imageUrl: null,
        displayOrder: 0,
        isActive: true,
        createdAt: "2026-09-01T00:00:00Z",
        updatedAt: "2026-09-01T00:00:00Z",
      },
    ]);
  });

  it("renders variants dialog with table of variants and formatted prices", async () => {
    renderVariantsDialog();

    await waitFor(() => {
      expect(screen.getByText("IP16PM-256-DESERT")).toBeInTheDocument();
      expect(screen.getByText("IP16PM-512-BLACK")).toBeInTheDocument();
    });

    expect(screen.getByText("Sa mạc titan")).toBeInTheDocument();
    expect(screen.getByText("Đen không gian")).toBeInTheDocument();
    expect(screen.getByText("256GB")).toBeInTheDocument();
    expect(screen.getByText("512GB")).toBeInTheDocument();
    expect(screen.getByText("Đang bán")).toBeInTheDocument();
    expect(screen.getByText("Ngừng bán")).toBeInTheDocument();
  });

  it("renders empty state when product has no variants", async () => {
    mockedGetProductVariants.mockResolvedValue([]);

    renderVariantsDialog();

    await waitFor(() => {
      expect(
        screen.getByText("Chưa có biến thể nào cho sản phẩm này"),
      ).toBeInTheDocument();
    });

    expect(
      screen.getByRole("button", { name: /thêm biến thể đầu tiên/i }),
    ).toBeInTheDocument();
  });

  it("opens add variant form, validates, and creates variant successfully", async () => {
    const onVariantsChanged = vi.fn();
    mockedCreateProductVariant.mockResolvedValue({
      id: 103,
      productId: 1,
      productName: "iPhone 16 Pro Max",
      sku: "IP16PM-1TB-NATURAL",
      color: "Titan tự nhiên",
      storage: "1TB",
      price: 40990000,
      originalPrice: 42990000,
      stockQuantity: 10,
      status: "ACTIVE",
      createdAt: "2026-09-03T00:00:00Z",
      updatedAt: "2026-09-03T00:00:00Z",
    });

    renderVariantsDialog({ onVariantsChanged });

    await waitFor(() => {
      expect(screen.getByText("IP16PM-256-DESERT")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: /^thêm biến thể$/i });
    fireEvent.click(addBtn);

    expect(screen.getByText("Thêm biến thể mới")).toBeInTheDocument();

    const skuInput = screen.getByLabelText(/mã sku \*/i);
    const colorInput = screen.getByLabelText(/màu sắc/i);
    const storageInput = screen.getByLabelText(/dung lượng \/ phiên bản/i);
    const priceInput = screen.getByLabelText(/giá bán \(vnđ\) \*/i);
    const originalPriceInput = screen.getByLabelText(/giá gốc \(vnđ\)/i);
    const stockInput = screen.getByLabelText(/số lượng tồn kho/i);

    fireEvent.change(skuInput, { target: { value: "ip16pm-1tb-natural" } });
    fireEvent.change(colorInput, { target: { value: "Titan tự nhiên" } });
    fireEvent.change(storageInput, { target: { value: "1TB" } });
    fireEvent.change(priceInput, { target: { value: "40990000" } });
    fireEvent.change(originalPriceInput, { target: { value: "42990000" } });
    fireEvent.change(stockInput, { target: { value: "10" } });

    const submitBtn = screen.getByRole("button", { name: /^tạo biến thể$/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedCreateProductVariant).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          sku: "IP16PM-1TB-NATURAL",
          color: "Titan tự nhiên",
          storage: "1TB",
          price: 40990000,
          originalPrice: 42990000,
          stockQuantity: 10,
          status: "ACTIVE",
        }),
      );
    });

    expect(onVariantsChanged).toHaveBeenCalled();
  });

  it("validates client-side rules: SKU required, price required, originalPrice >= price", async () => {
    renderVariantsDialog();

    await waitFor(() => {
      expect(screen.getByText("IP16PM-256-DESERT")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: /^thêm biến thể$/i });
    fireEvent.click(addBtn);

    const priceInput = screen.getByLabelText(/giá bán \(vnđ\) \*/i);
    const originalPriceInput = screen.getByLabelText(/giá gốc \(vnđ\)/i);
    const submitBtn = screen.getByRole("button", { name: /^tạo biến thể$/i });

    // Empty SKU and invalid price
    fireEvent.change(priceInput, { target: { value: "20000000" } });
    fireEvent.change(originalPriceInput, { target: { value: "15000000" } }); // originalPrice < price
    fireEvent.click(submitBtn);

    expect(screen.getByText("Mã SKU không được để trống.")).toBeInTheDocument();
    expect(
      screen.getByText("Giá gốc phải lớn hơn hoặc bằng giá bán."),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(
        screen.getByText("Mã SKU không được để trống."),
      ).toBeInTheDocument();
      expect(
        screen.getByText("Giá gốc phải lớn hơn hoặc bằng giá bán."),
      ).toBeInTheDocument();
    });

    expect(mockedCreateProductVariant).not.toHaveBeenCalled();
  });

  it("opens edit variant form and updates successfully", async () => {
    mockedUpdateProductVariant.mockResolvedValue({
      ...mockVariants[0],
      price: 27990000,
    });

    renderVariantsDialog();

    await waitFor(() => {
      expect(screen.getByText("IP16PM-256-DESERT")).toBeInTheDocument();
    });

    const editBtn = screen.getByRole("button", {
      name: /chỉnh sửa biến thể ip16pm-256-desert/i,
    });
    fireEvent.click(editBtn);

    expect(
      screen.getByText("Chỉnh sửa biến thể: IP16PM-256-DESERT"),
    ).toBeInTheDocument();

    const priceInput = screen.getByLabelText(/giá bán \(vnđ\) \*/i);
    fireEvent.change(priceInput, { target: { value: "27990000" } });

    const submitBtn = screen.getByRole("button", { name: /lưu thay đổi/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedUpdateProductVariant).toHaveBeenCalledWith(
        1,
        101,
        expect.objectContaining({
          price: 27990000,
        }),
      );
    });
  });

  it("deletes a variant with confirmation dialog", async () => {
    mockedDeleteProductVariant.mockResolvedValue();

    renderVariantsDialog();

    await waitFor(() => {
      expect(screen.getByText("IP16PM-256-DESERT")).toBeInTheDocument();
    });

    const deleteBtn = screen.getByRole("button", {
      name: /xoá biến thể ip16pm-256-desert/i,
    });
    fireEvent.click(deleteBtn);

    expect(screen.getByText("Xác nhận xoá biến thể")).toBeInTheDocument();

    const confirmBtn = screen.getByRole("button", { name: /xác nhận xoá/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(mockedDeleteProductVariant).toHaveBeenCalledWith(1, 101);
    });
  });

  it("shows error alert when deleting variant has orders", async () => {
    mockedDeleteProductVariant.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          code: "VARIANT_HAS_ORDERS",
          message:
            "Không thể xoá biến thể đã phát sinh đơn hàng, vui lòng chuyển trạng thái sang ngừng bán",
        },
      },
    });

    renderVariantsDialog();

    await waitFor(() => {
      expect(screen.getByText("IP16PM-256-DESERT")).toBeInTheDocument();
    });

    const deleteBtn = screen.getByRole("button", {
      name: /xoá biến thể ip16pm-256-desert/i,
    });
    fireEvent.click(deleteBtn);

    const confirmBtn = screen.getByRole("button", { name: /xác nhận xoá/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(
        screen.getByText(
          /Không thể xoá biến thể đã phát sinh đơn hàng, vui lòng chuyển trạng thái sang ngừng bán/i,
        ),
      ).toBeInTheDocument();
    });
  });

  it("opens variant dialog when clicking Biến thể button on AdminProductsPage", async () => {
    render(
      <ThemeProvider theme={appTheme}>
        <MemoryRouter>
          <AdminProductsPage />
        </MemoryRouter>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const variantBtn = screen.getByRole("button", { name: /biến thể/i });
    fireEvent.click(variantBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/biến thể sản phẩm: iphone 16 pro max/i),
      ).toBeInTheDocument();
    });
  });
});
