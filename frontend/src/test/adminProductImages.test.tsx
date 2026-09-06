import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { AdminProductImagesDialog } from "../modules/admin/AdminProductImagesDialog";
import { AdminProductsPage } from "../modules/admin/AdminProductsPage";
import {
  deleteProductImage,
  getAdminProducts,
  getProductImages,
  getProductVariants,
  setPrimaryProductImage,
  updateProductImage,
  uploadProductImage,
  type Product,
  type ProductImage,
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
  getProductImages: vi.fn(),
  uploadProductImage: vi.fn(),
  setPrimaryProductImage: vi.fn(),
  updateProductImage: vi.fn(),
  deleteProductImage: vi.fn(),
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
const mockedGetProductImages = vi.mocked(getProductImages);
const mockedGetProductVariants = vi.mocked(getProductVariants);
const mockedUploadProductImage = vi.mocked(uploadProductImage);
const mockedSetPrimaryProductImage = vi.mocked(setPrimaryProductImage);
const mockedUpdateProductImage = vi.mocked(updateProductImage);
const mockedDeleteProductImage = vi.mocked(deleteProductImage);

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
];

const mockImages: ProductImage[] = [
  {
    id: 201,
    productId: 1,
    variantId: null,
    variantSku: null,
    variantColor: null,
    imageUrl: "/uploads/products/front.jpg",
    isPrimary: true,
    displayOrder: 0,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
  {
    id: 202,
    productId: 1,
    variantId: 101,
    variantSku: "IP16PM-256-DESERT",
    variantColor: "Sa mạc titan",
    imageUrl: "/uploads/products/desert.jpg",
    isPrimary: false,
    displayOrder: 1,
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
  },
];

function renderImagesDialog(props?: {
  open?: boolean;
  product?: Product | null;
  onClose?: () => void;
  onImagesChanged?: () => void;
}) {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <AdminProductImagesDialog
          open={props?.open ?? true}
          product={props?.product !== undefined ? props.product : mockProduct}
          onClose={props?.onClose ?? vi.fn()}
          onImagesChanged={props?.onImagesChanged}
        />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

function renderProductsPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <AdminProductsPage />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("AdminProductImagesDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetProductImages.mockResolvedValue(mockImages);
    mockedGetProductVariants.mockResolvedValue(mockVariants);
  });

  test("Hiển thị tiêu đề, tên sản phẩm và khu vực tải ảnh", async () => {
    renderImagesDialog();

    expect(screen.getByText("Quản lý hình ảnh")).toBeInTheDocument();
    expect(screen.getByText(/iPhone 16 Pro Max/i)).toBeInTheDocument();
    expect(
      screen.getByText(/Kéo thả nhiều ảnh vào đây hoặc nhấp để tải lên/i),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(mockedGetProductImages).toHaveBeenCalledWith(1);
      expect(mockedGetProductVariants).toHaveBeenCalledWith(1);
    });
  });

  test("Hiển thị danh sách ảnh và huy hiệu ảnh đại diện, biến thể", async () => {
    renderImagesDialog();

    await waitFor(() => {
      expect(screen.getByText("Ảnh đại diện")).toBeInTheDocument();
      expect(screen.getByText("Sa mạc titan")).toBeInTheDocument();
      expect(screen.getByText("Đặt đại diện")).toBeInTheDocument();
    });
  });

  test("Kiểm tra validate phía client: định dạng file không hợp lệ", async () => {
    renderImagesDialog();

    const fileInput = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;
    expect(fileInput).not.toBeNull();

    const invalidFile = new File(["dummy content"], "document.pdf", {
      type: "application/pdf",
    });

    fireEvent.change(fileInput, { target: { files: [invalidFile] } });

    await waitFor(() => {
      expect(
        screen.getByText(
          /không đúng định dạng\. Chỉ hỗ trợ JPG, JPEG, PNG, WEBP/i,
        ),
      ).toBeInTheDocument();
    });
    expect(mockedUploadProductImage).not.toHaveBeenCalled();
  });

  test("Kiểm tra validate phía client: dung lượng file vượt quá 5MB", async () => {
    renderImagesDialog();

    const fileInput = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;

    const largeFile = new File(["dummy"], "big-photo.jpg", {
      type: "image/jpeg",
    });
    Object.defineProperty(largeFile, "size", { value: 6 * 1024 * 1024 });

    fireEvent.change(fileInput, { target: { files: [largeFile] } });

    await waitFor(() => {
      expect(
        screen.getByText(/dung lượng quá lớn.*Giới hạn tối đa là 5MB/i),
      ).toBeInTheDocument();
    });
    expect(mockedUploadProductImage).not.toHaveBeenCalled();
  });

  test("Tải lên ảnh hợp lệ thành công", async () => {
    mockedUploadProductImage.mockResolvedValue({
      id: 203,
      productId: 1,
      variantId: null,
      imageUrl: "/uploads/products/new.png",
      isPrimary: false,
      displayOrder: 2,
      createdAt: "2026-09-02T00:00:00Z",
      updatedAt: "2026-09-02T00:00:00Z",
    });

    renderImagesDialog();

    const fileInput = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;

    const validFile = new File(["valid image content"], "photo.png", {
      type: "image/png",
    });

    fireEvent.change(fileInput, { target: { files: [validFile] } });

    await waitFor(() => {
      expect(mockedUploadProductImage).toHaveBeenCalledWith(
        1,
        validFile,
        null,
        false,
      );
    });

    await waitFor(() => {
      expect(
        screen.getByText(/Đã tải lên thành công 1 hình ảnh/i),
      ).toBeInTheDocument();
    });
  });

  test("Đặt ảnh làm ảnh đại diện", async () => {
    mockedSetPrimaryProductImage.mockResolvedValue({
      ...mockImages[1],
      isPrimary: true,
    });

    renderImagesDialog();

    await waitFor(() => {
      expect(screen.getByText("Đặt đại diện")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Đặt đại diện"));

    await waitFor(() => {
      expect(mockedSetPrimaryProductImage).toHaveBeenCalledWith(1, 202);
      expect(
        screen.getByText(/Đã thiết lập ảnh đại diện mới cho sản phẩm/i),
      ).toBeInTheDocument();
    });
  });

  test("Cập nhật gắn biến thể cho hình ảnh", async () => {
    mockedUpdateProductImage.mockResolvedValue({
      ...mockImages[0],
      variantId: 101,
    });

    renderImagesDialog();

    await waitFor(() => {
      expect(
        screen.getByTestId("image-variant-select-201"),
      ).toBeInTheDocument();
    });

    const variantSelect = screen.getByTestId("image-variant-select-201");
    fireEvent.change(variantSelect, { target: { value: "101" } });

    await waitFor(() => {
      expect(mockedUpdateProductImage).toHaveBeenCalledWith(1, 201, {
        variantId: 101,
      });
      expect(
        screen.getByText(/Cập nhật gắn biến thể cho ảnh thành công/i),
      ).toBeInTheDocument();
    });
  });

  test("Xoá ảnh thành công sau khi xác nhận", async () => {
    mockedDeleteProductImage.mockResolvedValue();

    renderImagesDialog();

    await waitFor(() => {
      expect(screen.getAllByLabelText("Xoá ảnh này")).toHaveLength(2);
    });

    const deleteButtons = screen.getAllByLabelText("Xoá ảnh này");
    fireEvent.click(deleteButtons[1]); // Delete second image

    await waitFor(() => {
      expect(screen.getByText("Xác nhận xoá hình ảnh")).toBeInTheDocument();
    });

    const confirmButton = screen.getByRole("button", { name: "Xoá ảnh" });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockedDeleteProductImage).toHaveBeenCalledWith(1, 202);
      expect(
        screen.getByText(/Đã xoá hình ảnh thành công khỏi hệ thống/i),
      ).toBeInTheDocument();
    });
  });

  test("Mở dialog quản lý hình ảnh từ AdminProductsPage", async () => {
    mockedGetAdminProducts.mockResolvedValue([mockProduct]);
    mockedGetAdminBrands.mockResolvedValue([]);
    mockedGetAdminCategories.mockResolvedValue([]);

    renderProductsPage();

    await waitFor(() => {
      expect(screen.getByText("iPhone 16 Pro Max")).toBeInTheDocument();
    });

    const imageBtn = screen.getByRole("button", { name: /Hình ảnh/i });
    expect(imageBtn).toBeInTheDocument();

    fireEvent.click(imageBtn);

    await waitFor(() => {
      expect(screen.getByText("Quản lý hình ảnh")).toBeInTheDocument();
      expect(mockedGetProductImages).toHaveBeenCalledWith(1);
    });
  });
});
