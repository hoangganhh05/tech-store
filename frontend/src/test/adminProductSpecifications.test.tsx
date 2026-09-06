import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { appTheme } from "../configs/theme";
import { AdminProductSpecificationsDialog } from "../modules/admin/AdminProductSpecificationsDialog";
import {
  createProductSpecification,
  deleteProductSpecification,
  getProductSpecifications,
  updateProductSpecification,
  type Product,
  type ProductSpecification,
} from "../services/productService";

vi.mock("../services/productService", () => ({
  getProductSpecifications: vi.fn(),
  createProductSpecification: vi.fn(),
  updateProductSpecification: vi.fn(),
  deleteProductSpecification: vi.fn(),
}));

const mockedGetProductSpecifications = vi.mocked(getProductSpecifications);
const mockedCreateProductSpecification = vi.mocked(createProductSpecification);
const mockedUpdateProductSpecification = vi.mocked(updateProductSpecification);
const mockedDeleteProductSpecification = vi.mocked(deleteProductSpecification);

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

const mockSpecifications: ProductSpecification[] = [
  {
    id: 101,
    productId: 1,
    specKey: "Màn hình",
    specValue: "6.9 inch OLED Super Retina XDR",
    displayOrder: 1,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 102,
    productId: 1,
    specKey: "CPU",
    specValue: "Apple A18 Pro 6 nhân",
    displayOrder: 2,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

describe("AdminProductSpecificationsDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetProductSpecifications.mockResolvedValue(mockSpecifications);
  });

  const renderDialog = (open = true, onSpecsChanged = vi.fn(), onClose = vi.fn()) => {
    return render(
      <ThemeProvider theme={appTheme}>
        <AdminProductSpecificationsDialog
          open={open}
          product={mockProduct}
          onClose={onClose}
          onSpecificationsChanged={onSpecsChanged}
        />
      </ThemeProvider>,
    );
  };

  it("renders specifications table with order, key, and value", async () => {
    renderDialog();

    await waitFor(() => {
      expect(screen.getByText("Thông số kỹ thuật sản phẩm")).toBeInTheDocument();
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
      expect(screen.getByText("Màn hình")).toBeInTheDocument();
      expect(screen.getByText("6.9 inch OLED Super Retina XDR")).toBeInTheDocument();
      expect(screen.getByText("CPU")).toBeInTheDocument();
      expect(screen.getByText("Apple A18 Pro 6 nhân")).toBeInTheDocument();
    });
  });

  it("renders empty state when product has no specifications", async () => {
    mockedGetProductSpecifications.mockResolvedValue([]);
    renderDialog();

    await waitFor(() => {
      expect(screen.getByText("Chưa có thông số kỹ thuật nào")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /thêm thông số đầu tiên/i })).toBeInTheDocument();
    });
  });

  it("opens create specification form, fills template chip, and creates spec successfully", async () => {
    mockedCreateProductSpecification.mockResolvedValue({
      id: 103,
      productId: 1,
      specKey: "RAM",
      specValue: "8 GB",
      displayOrder: 3,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    });

    const onSpecsChanged = vi.fn();
    renderDialog(true, onSpecsChanged);

    await waitFor(() => {
      expect(screen.getByText("Màn hình")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: "Thêm thông số" });
    fireEvent.click(addBtn);

    expect(screen.getByText("Thêm thông số kỹ thuật")).toBeInTheDocument();

    // Click quick suggestion chip for "RAM"
    const ramChip = screen.getByRole("button", { name: "RAM" });
    fireEvent.click(ramChip);

    const keyInput = screen.getByLabelText(/tên thông số kỹ thuật \*/i);
    expect(keyInput).toHaveValue("RAM");

    const valueInput = screen.getByLabelText(/giá trị thông số \*/i);
    fireEvent.change(valueInput, { target: { value: "8 GB" } });

    const submitBtn = screen.getByRole("button", { name: "Thêm thông số" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedCreateProductSpecification).toHaveBeenCalledWith(1, {
        specKey: "RAM",
        specValue: "8 GB",
        displayOrder: 3,
      });
    });

    await waitFor(() => {
      expect(screen.getByText(/Đã thêm thông số "RAM" thành công/i)).toBeInTheDocument();
    });
  });

  it("validates required fields on client side before submitting", async () => {
    renderDialog();

    await waitFor(() => {
      expect(screen.getByText("Màn hình")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: "Thêm thông số" });
    fireEvent.click(addBtn);

    const submitBtn = screen.getByRole("button", { name: "Thêm thông số" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText("Tên thông số kỹ thuật không được để trống.")).toBeInTheDocument();
      expect(screen.getByText("Giá trị thông số kỹ thuật không được để trống.")).toBeInTheDocument();
    });
    expect(mockedCreateProductSpecification).not.toHaveBeenCalled();
  });

  it("opens edit specification form and updates successfully", async () => {
    mockedUpdateProductSpecification.mockResolvedValue({
      id: 101,
      productId: 1,
      specKey: "Màn hình",
      specValue: "6.9 inch OLED ProMotion 120Hz",
      displayOrder: 1,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    });

    renderDialog();

    await waitFor(() => {
      expect(screen.getByText("Màn hình")).toBeInTheDocument();
    });

    const editBtn = screen.getByRole("button", { name: /sửa thông số màn hình/i });
    fireEvent.click(editBtn);

    expect(screen.getByText("Chỉnh sửa thông số kỹ thuật")).toBeInTheDocument();

    const valueInput = screen.getByLabelText(/giá trị thông số \*/i);
    fireEvent.change(valueInput, { target: { value: "6.9 inch OLED ProMotion 120Hz" } });

    const submitBtn = screen.getByRole("button", { name: "Lưu thay đổi" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedUpdateProductSpecification).toHaveBeenCalledWith(1, 101, {
        specKey: "Màn hình",
        specValue: "6.9 inch OLED ProMotion 120Hz",
        displayOrder: 1,
      });
    });

    await waitFor(() => {
      expect(screen.getByText(/Đã cập nhật thông số "Màn hình" thành công/i)).toBeInTheDocument();
    });
  });

  it("deletes a specification after confirming in dialog", async () => {
    mockedDeleteProductSpecification.mockResolvedValue();

    renderDialog();

    await waitFor(() => {
      expect(screen.getByText("Màn hình")).toBeInTheDocument();
    });

    const deleteBtn = screen.getByRole("button", { name: /xoá thông số màn hình/i });
    fireEvent.click(deleteBtn);

    expect(screen.getByRole("heading", { name: "Xác nhận xoá thông số" })).toBeInTheDocument();
    expect(screen.getByText(/Bạn có chắc chắn muốn xoá thông số/i)).toBeInTheDocument();

    const confirmBtn = screen.getByRole("button", { name: "Xác nhận xoá" });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(mockedDeleteProductSpecification).toHaveBeenCalledWith(1, 101);
    });

    await waitFor(() => {
      expect(screen.getByText(/Đã xoá thông số "Màn hình" thành công/i)).toBeInTheDocument();
    });
  });

  it("displays error alert when creating duplicate specification key fails", async () => {
    mockedCreateProductSpecification.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          code: "SPECIFICATION_KEY_DUPLICATE",
          message: "Thông số kỹ thuật 'CPU' đã tồn tại cho sản phẩm này",
        },
      },
    });

    renderDialog();

    await waitFor(() => {
      expect(screen.getByText("Màn hình")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: "Thêm thông số" });
    fireEvent.click(addBtn);

    const keyInput = screen.getByLabelText(/tên thông số kỹ thuật \*/i);
    fireEvent.change(keyInput, { target: { value: "CPU" } });

    const valueInput = screen.getByLabelText(/giá trị thông số \*/i);
    fireEvent.change(valueInput, { target: { value: "A18 Pro" } });

    const submitBtn = screen.getByRole("button", { name: "Thêm thông số" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Thông số kỹ thuật 'CPU' đã tồn tại cho sản phẩm này/i),
      ).toBeInTheDocument();
    });
  });
});
