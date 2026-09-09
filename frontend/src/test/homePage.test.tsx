import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { HomePage } from "../modules/home/HomePage";
import {
  getStorefrontHomeData,
  type StorefrontHomeData,
  type StorefrontProduct,
} from "../services/storefrontService";

vi.mock("../services/storefrontService", () => ({
  getStorefrontHomeData: vi.fn(),
  getFeaturedProducts: vi.fn(),
  getNewArrivals: vi.fn(),
  getOnSaleProducts: vi.fn(),
}));

const mockedGetStorefrontHomeData = vi.mocked(getStorefrontHomeData);

const mockProduct1: StorefrontProduct = {
  id: 1,
  name: "iPhone 15 Pro Max",
  description: "Flagship Apple",
  brandId: 1,
  brandName: "Apple",
  categoryId: 1,
  categoryName: "Điện thoại",
  thumbnailUrl: "https://example.com/ip15pm.jpg",
  minPrice: 29990000,
  maxPrice: 34990000,
  originalPrice: 34990000,
  discountPercent: 14,
  totalStock: 25,
  hasStock: true,
  salesCount: 150,
  rating: 5.0,
  createdAt: "2026-09-01T00:00:00Z",
};

const mockProduct2: StorefrontProduct = {
  id: 2,
  name: "Samsung Galaxy S24 Ultra",
  description: "Flagship Samsung",
  brandId: 2,
  brandName: "Samsung",
  categoryId: 1,
  categoryName: "Điện thoại",
  thumbnailUrl: "https://example.com/s24u.jpg",
  minPrice: 27990000,
  maxPrice: 27990000,
  originalPrice: null,
  discountPercent: 0,
  totalStock: 0,
  hasStock: false,
  salesCount: 80,
  rating: 4.9,
  createdAt: "2026-09-02T00:00:00Z",
};

const mockProductNew: StorefrontProduct = {
  id: 3,
  name: "MacBook Pro M3 Max",
  description: "Laptop đỉnh cao",
  brandId: 1,
  brandName: "Apple",
  categoryId: 2,
  categoryName: "Laptop",
  thumbnailUrl: null,
  minPrice: 49990000,
  maxPrice: 49990000,
  originalPrice: null,
  discountPercent: 0,
  totalStock: 10,
  hasStock: true,
  salesCount: 12,
  rating: 5.0,
  createdAt: "2026-09-05T00:00:00Z",
};

const mockHomeData: StorefrontHomeData = {
  featuredProducts: [mockProduct1, mockProduct2],
  newArrivals: [mockProductNew],
  onSaleProducts: [mockProduct1],
  featuredCategories: [
    {
      id: 1,
      name: "Điện thoại",
      description: "Smartphone",
      parentId: null,
      parentName: null,
      imageUrl: "https://example.com/phone.png",
      displayOrder: 1,
      isActive: true,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
    {
      id: 2,
      name: "Laptop",
      description: "Máy tính xách tay",
      parentId: null,
      parentName: null,
      imageUrl: null,
      displayOrder: 2,
      isActive: true,
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    },
  ],
};

function renderHomePage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-05.1: HomePage Storefront Component", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders loading skeletons and hero banner initially", () => {
    mockedGetStorefrontHomeData.mockReturnValue(new Promise(() => {}));
    renderHomePage();

    expect(
      screen.getByRole("heading", { name: "Đăng Tùng Mobile" }),
    ).toBeInTheDocument();
    expect(screen.getByText(/Khám phá sản phẩm/i)).toBeInTheDocument();
  });

  it("renders all storefront sections successfully with real data", async () => {
    mockedGetStorefrontHomeData.mockResolvedValue(mockHomeData);
    renderHomePage();

    // 1. Featured categories
    await waitFor(() => {
      expect(screen.getByText("Danh mục nổi bật")).toBeInTheDocument();
    });
    expect(screen.getByText("Điện thoại")).toBeInTheDocument();
    expect(screen.getByText("Laptop")).toBeInTheDocument();

    // 2. On-sale / Flash sale section
    expect(screen.getByText("Săn Sale Giá Sốc")).toBeInTheDocument();
    expect(screen.getAllByText("-14%").length).toBeGreaterThan(0);

    // 3. Featured products section
    expect(screen.getByText("Sản phẩm nổi bật")).toBeInTheDocument();
    expect(screen.getAllByText("iPhone 15 Pro Max").length).toBeGreaterThan(0);
    expect(screen.getByText("Samsung Galaxy S24 Ultra")).toBeInTheDocument();

    // 4. Out of stock badge
    expect(screen.getByText("Tạm hết hàng")).toBeInTheDocument();

    // 5. New arrivals section
    expect(screen.getByText("Sản phẩm mới về")).toBeInTheDocument();
    expect(screen.getByText("MacBook Pro M3 Max")).toBeInTheDocument();

    // Store commitments remain hidden until their terms are configured.
    expect(screen.queryByText("100% Chính Hãng")).not.toBeInTheDocument();
    expect(screen.queryByText("Giao Hàng Toàn Quốc")).not.toBeInTheDocument();
    expect(screen.queryByText("Đổi Trả Trong 30 Ngày")).not.toBeInTheDocument();
    expect(screen.queryByText("Hỗ Trợ Tận Tâm 24/7")).not.toBeInTheDocument();
  });

  it("renders error alert and retries when clicking retry button", async () => {
    mockedGetStorefrontHomeData.mockRejectedValueOnce(
      new Error("Mạng không ổn định"),
    );
    renderHomePage();

    await waitFor(() => {
      expect(screen.getByText("Mạng không ổn định")).toBeInTheDocument();
    });

    const retryBtn = screen.getByRole("button", { name: /Thử lại/i });
    expect(retryBtn).toBeInTheDocument();

    mockedGetStorefrontHomeData.mockResolvedValueOnce(mockHomeData);
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.queryByText("Mạng không ổn định")).not.toBeInTheDocument();
      expect(screen.getByText("Sản phẩm nổi bật")).toBeInTheDocument();
    });
  });

  it("handles empty product sections gracefully without crashing", async () => {
    mockedGetStorefrontHomeData.mockResolvedValue({
      featuredProducts: [],
      newArrivals: [],
      onSaleProducts: [],
      featuredCategories: [],
    });

    renderHomePage();

    await waitFor(() => {
      expect(
        screen.getByText("Chưa có sản phẩm nổi bật nào."),
      ).toBeInTheDocument();
      expect(screen.getByText("Chưa có sản phẩm mới nào.")).toBeInTheDocument();
    });

    // Flash sale should be hidden when empty
    expect(screen.queryByText("Săn Sale Giá Sốc")).not.toBeInTheDocument();
  });
});
