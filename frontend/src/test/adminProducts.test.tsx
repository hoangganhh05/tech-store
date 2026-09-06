import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AuthProvider } from "../modules/auth/AuthContext";
import { AdminProductsPage } from "../modules/admin/AdminProductsPage";
import {
  createAdminProduct,
  getAdminProducts,
  updateAdminProduct,
  updateAdminProductStatus,
  type Product,
} from "../services/productService";
import { getAdminBrands, type Brand } from "../services/brandService";
import { getAdminCategories, type Category } from "../services/categoryService";

vi.mock("../services/productService", () => ({
  getAdminProducts: vi.fn(),
  getAdminProductById: vi.fn(),
  createAdminProduct: vi.fn(),
  updateAdminProduct: vi.fn(),
  updateAdminProductStatus: vi.fn(),
}));

vi.mock("../services/brandService", () => ({
  getAdminBrands: vi.fn(),
}));

vi.mock("../services/categoryService", () => ({
  getAdminCategories: vi.fn(),
}));

const mockedGetAdminProducts = vi.mocked(getAdminProducts);
const mockedCreateAdminProduct = vi.mocked(createAdminProduct);
const mockedUpdateAdminProduct = vi.mocked(updateAdminProduct);
const mockedUpdateAdminProductStatus = vi.mocked(updateAdminProductStatus);
const mockedGetAdminBrands = vi.mocked(getAdminBrands);
const mockedGetAdminCategories = vi.mocked(getAdminCategories);

const mockBrands: Brand[] = [
  {
    id: 1,
    name: "Apple",
    logoUrl: "https://example.com/apple.png",
    description: "Apple Inc.",
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 2,
    name: "Samsung",
    logoUrl: "https://example.com/samsung.png",
    description: "Samsung Electronics",
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

const mockCategories: Category[] = [
  {
    id: 10,
    name: "Điện thoại",
    description: "Điện thoại di động",
    parentId: null,
    parentName: null,
    imageUrl: null,
    displayOrder: 0,
    isActive: true,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

const mockProducts: Product[] = [
  {
    id: 100,
    name: "iPhone 16 Pro Max",
    description: "Flagship mới nhất từ Apple",
    brandId: 1,
    brandName: "Apple",
    categoryId: 10,
    categoryName: "Điện thoại",
    status: "DRAFT",
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 101,
    name: "Galaxy S25 Ultra",
    description: "Flagship Samsung",
    brandId: 2,
    brandName: "Samsung",
    categoryId: 10,
    categoryName: "Điện thoại",
    status: "ACTIVE",
    createdAt: "2026-09-02T00:00:00Z",
    updatedAt: "2026-09-02T00:00:00Z",
  },
];

function renderAdminProductsPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider>
        <MemoryRouter>
          <AdminProductsPage />
        </MemoryRouter>
      </AuthProvider>
    </ThemeProvider>,
  );
}

describe("AdminProductsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetAdminProducts.mockResolvedValue(mockProducts);
    mockedGetAdminBrands.mockResolvedValue(mockBrands);
    mockedGetAdminCategories.mockResolvedValue(mockCategories);
  });

  it("renders products table with brand and category data", async () => {
    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
      expect(screen.getByText("Galaxy S25 Ultra")).toBeInTheDocument();
    });

    expect(screen.getAllByText("Apple").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Samsung").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Điện thoại").length).toBeGreaterThan(0);
    expect(screen.getByText("Nháp")).toBeInTheDocument();
    expect(screen.getByText("Đang bán")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /sửa/i })).toHaveLength(2);
    expect(screen.getAllByRole("button", { name: /biến thể/i })).toHaveLength(2);
    expect(screen.getAllByRole("button", { name: /hình ảnh/i })).toHaveLength(2);
  });

  it("filters products using search input", async () => {
    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/tìm kiếm sản phẩm theo tên/i);
    fireEvent.change(searchInput, { target: { value: "Galaxy" } });

    expect(screen.queryByText("iPhone 16 Pro Max")).not.toBeInTheDocument();
    expect(screen.getByText("Galaxy S25 Ultra")).toBeInTheDocument();
  });

  it("displays empty state when there are no products", async () => {
    mockedGetAdminProducts.mockResolvedValue([]);

    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("Chưa có sản phẩm nào")).toBeInTheDocument();
    });

    expect(
      screen.getByRole("button", { name: /thêm sản phẩm đầu tiên/i }),
    ).toBeInTheDocument();
  });

  it("opens create product dialog and submits successfully", async () => {
    mockedCreateAdminProduct.mockResolvedValue({
      id: 102,
      name: "iPhone 16 Plus",
      description: "Màn hình lớn",
      brandId: 1,
      brandName: "Apple",
      categoryId: 10,
      categoryName: "Điện thoại",
      status: "DRAFT",
      createdAt: "2026-09-03T00:00:00Z",
      updatedAt: "2026-09-03T00:00:00Z",
    });

    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: /thêm sản phẩm$/i });
    fireEvent.click(addBtn);

    expect(screen.getByText("Tạo sản phẩm mới")).toBeInTheDocument();

    const nameInput = screen.getByLabelText(/tên sản phẩm \*/i);
    fireEvent.change(nameInput, { target: { value: "iPhone 16 Plus" } });

    const submitBtn = screen.getByRole("button", { name: /tạo sản phẩm$/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedCreateAdminProduct).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "iPhone 16 Plus",
          brandId: 1,
          categoryId: 10,
          status: "DRAFT",
        }),
      );
    });
  });

  it("opens edit product dialog with prefilled data and submits successfully", async () => {
    mockedUpdateAdminProduct.mockResolvedValue({
      id: 100,
      name: "iPhone 16 Pro Max (Updated)",
      description: "Mô tả mới",
      brandId: 1,
      brandName: "Apple",
      categoryId: 10,
      categoryName: "Điện thoại",
      status: "DRAFT",
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-03T00:00:00Z",
    });

    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const editBtns = screen.getAllByRole("button", { name: /sửa/i });
    fireEvent.click(editBtns[0]);

    expect(screen.getByText("Chỉnh sửa thông tin sản phẩm")).toBeInTheDocument();

    const nameInput = screen.getByLabelText(/tên sản phẩm \*/i);
    expect(nameInput).toHaveValue("iPhone 16 Pro Max");

    fireEvent.change(nameInput, { target: { value: "iPhone 16 Pro Max (Updated)" } });

    const submitBtn = screen.getByRole("button", { name: /cập nhật sản phẩm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockedUpdateAdminProduct).toHaveBeenCalledWith(
        100,
        expect.objectContaining({
          name: "iPhone 16 Pro Max (Updated)",
          brandId: 1,
          categoryId: 10,
          status: "DRAFT",
        }),
      );
    });

    await waitFor(() => {
      expect(
        screen.getByText(/Đã cập nhật sản phẩm "iPhone 16 Pro Max \(Updated\)" thành công/i),
      ).toBeInTheDocument();
    });
  });

  it("validates required name field before submitting", async () => {
    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: /thêm sản phẩm$/i });
    fireEvent.click(addBtn);

    const submitBtn = screen.getByRole("button", { name: /tạo sản phẩm$/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText("Tên sản phẩm không được để trống."),
      ).toBeInTheDocument();
    });
    expect(mockedCreateAdminProduct).not.toHaveBeenCalled();
  });

  it("displays error alert when creating product fails", async () => {
    mockedCreateAdminProduct.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          message: "Tên sản phẩm đã tồn tại trong cùng thương hiệu",
        },
      },
    });

    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: /thêm sản phẩm$/i });
    fireEvent.click(addBtn);

    const nameInput = screen.getByLabelText(/tên sản phẩm \*/i);
    fireEvent.change(nameInput, { target: { value: "iPhone 16 Pro Max" } });

    const submitBtn = screen.getByRole("button", { name: /tạo sản phẩm$/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Tên sản phẩm đã tồn tại trong cùng thương hiệu/i),
      ).toBeInTheDocument();
    });
  });

  it("displays error alert when editing product fails", async () => {
    mockedUpdateAdminProduct.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          message: "Tên sản phẩm đã tồn tại trong cùng thương hiệu",
        },
      },
    });

    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const editBtns = screen.getAllByRole("button", { name: /sửa/i });
    fireEvent.click(editBtns[0]);

    const submitBtn = screen.getByRole("button", { name: /cập nhật sản phẩm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Tên sản phẩm đã tồn tại trong cùng thương hiệu/i),
      ).toBeInTheDocument();
    });
  });

  it("toggles product status quickly via status menu to INACTIVE", async () => {
    mockedUpdateAdminProductStatus.mockResolvedValue({
      ...mockProducts[1],
      status: "INACTIVE",
    });

    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("Galaxy S25 Ultra")).toBeInTheDocument();
    });

    const activeChip = screen.getByTestId("status-chip-101");
    fireEvent.click(activeChip);

    const inactiveOption = screen.getByRole("menuitem", {
      name: /ngừng bán \(inactive\)/i,
    });
    fireEvent.click(inactiveOption);

    await waitFor(() => {
      expect(mockedUpdateAdminProductStatus).toHaveBeenCalledWith(101, {
        status: "INACTIVE",
      });
    });

    await waitFor(() => {
      expect(
        screen.getByText(/Đã chuyển trạng thái sản phẩm "Galaxy S25 Ultra" sang "Ngừng bán" thành công/i),
      ).toBeInTheDocument();
    });
  });

  it("displays error alert when toggling product status fails", async () => {
    mockedUpdateAdminProductStatus.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          message: "Sản phẩm chỉ có thể chuyển sang đang bán khi có ít nhất một biến thể hợp lệ",
        },
      },
    });

    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const draftChip = screen.getByTestId("status-chip-100");
    fireEvent.click(draftChip);

    const activeOption = screen.getByRole("menuitem", {
      name: /đang bán \(active\)/i,
    });
    fireEvent.click(activeOption);

    await waitFor(() => {
      expect(
        screen.getByText(/Sản phẩm chỉ có thể chuyển sang đang bán khi có ít nhất một biến thể hợp lệ/i),
      ).toBeInTheDocument();
    });
  });

  it("displays error alert when fetching data fails", async () => {
    mockedGetAdminProducts.mockRejectedValue(new Error("Network error"));

    renderAdminProductsPage();

    await waitFor(() => {
      expect(
        screen.getByText(/Không thể tải danh sách sản phẩm/i),
      ).toBeInTheDocument();
    });
  });
});
