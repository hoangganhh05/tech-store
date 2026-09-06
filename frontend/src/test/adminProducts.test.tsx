import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AuthProvider } from "../modules/auth/AuthContext";
import { AdminProductsPage } from "../modules/admin/AdminProductsPage";
import {
  createAdminProduct,
  getAdminProducts,
  type Product,
} from "../services/productService";
import { getAdminBrands, type Brand } from "../services/brandService";
import { getAdminCategories, type Category } from "../services/categoryService";

vi.mock("../services/productService", () => ({
  getAdminProducts: vi.fn(),
  getAdminProductById: vi.fn(),
  createAdminProduct: vi.fn(),
}));

vi.mock("../services/brandService", () => ({
  getAdminBrands: vi.fn(),
}));

vi.mock("../services/categoryService", () => ({
  getAdminCategories: vi.fn(),
}));

const mockedGetAdminProducts = vi.mocked(getAdminProducts);
const mockedCreateAdminProduct = vi.mocked(createAdminProduct);
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

  it("renders products table with items, brands, categories, and status chips", async () => {
    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
      expect(screen.getByText("Galaxy S25 Ultra")).toBeInTheDocument();
    });

    expect(screen.getByText("Apple")).toBeInTheDocument();
    expect(screen.getByText("Samsung")).toBeInTheDocument();
    expect(screen.getByText("Nháp")).toBeInTheDocument();
    expect(screen.getByText("Đang bán")).toBeInTheDocument();
  });

  it("filters products list when typing search keyword", async () => {
    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/tìm kiếm sản phẩm/i);
    fireEvent.change(searchInput, { target: { value: "Galaxy" } });

    expect(screen.queryByText("iPhone 16 Pro Max")).not.toBeInTheDocument();
    expect(screen.getByText("Galaxy S25 Ultra")).toBeInTheDocument();
  });

  it("renders empty state when there are no products", async () => {
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

    const addBtn = screen.getByRole("button", { name: /thêm sản phẩm/i });
    fireEvent.click(addBtn);

    expect(screen.getByText("Tạo sản phẩm mới")).toBeInTheDocument();

    const nameInput = screen.getByLabelText(/tên sản phẩm \*/i);
    fireEvent.change(nameInput, { target: { value: "iPhone 16 Plus" } });

    const submitBtn = screen.getByRole("button", { name: /tạo sản phẩm/i });
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

  it("validates required name field before submitting", async () => {
    renderAdminProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const addBtn = screen.getByRole("button", { name: /thêm sản phẩm/i });
    fireEvent.click(addBtn);

    const submitBtn = screen.getByRole("button", { name: /tạo sản phẩm/i });
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

    const addBtn = screen.getByRole("button", { name: /thêm sản phẩm/i });
    fireEvent.click(addBtn);

    const nameInput = screen.getByLabelText(/tên sản phẩm \*/i);
    fireEvent.change(nameInput, { target: { value: "iPhone 16 Pro Max" } });

    const submitBtn = screen.getByRole("button", { name: /tạo sản phẩm/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Tên sản phẩm đã tồn tại trong cùng thương hiệu/i),
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
