import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ThemeProvider } from "@mui/material";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { appTheme } from "../configs/theme";
import { ProductCard } from "../components/common/ProductCard";
import { WishlistProvider } from "../modules/wishlist/WishlistContext";
import { WishlistPage } from "../modules/wishlist/WishlistPage";
import {
  addToWishlist,
  getWishlist,
  removeFromWishlist,
  type WishlistPage as WishlistPageData,
} from "../services/wishlistService";
import type { StorefrontProduct } from "../services/storefrontService";

vi.mock("../services/wishlistService", () => ({
  addToWishlist: vi.fn(),
  getWishlist: vi.fn(),
  removeFromWishlist: vi.fn(),
}));

vi.mock("../hooks/useAuth", () => ({
  useAuth: () => ({
    user: {
      id: 10,
      email: "customer@example.com",
      fullName: "Customer",
      phone: "0900000000",
      status: "ACTIVE",
      roles: ["CUSTOMER"],
      emailVerified: true,
      createdAt: "2026-09-01T00:00:00Z",
    },
    isAuthenticated: true,
    signIn: vi.fn(),
    updateUserProfile: vi.fn(),
    signOut: vi.fn(),
    clearSession: vi.fn(),
  }),
}));

const mockedGetWishlist = vi.mocked(getWishlist);
const mockedAddToWishlist = vi.mocked(addToWishlist);
const mockedRemoveFromWishlist = vi.mocked(removeFromWishlist);

const product: StorefrontProduct = {
  id: 101,
  name: "iPhone yêu thích",
  description: "Điện thoại",
  brandId: 1,
  brandName: "Apple",
  categoryId: 1,
  categoryName: "Điện thoại",
  thumbnailUrl: null,
  minPrice: 20000000,
  maxPrice: 22000000,
  originalPrice: null,
  discountPercent: 0,
  totalStock: 4,
  hasStock: true,
  salesCount: 12,
  rating: 5,
  createdAt: "2026-09-01T00:00:00Z",
};

const wishlistResponse: WishlistPageData = {
  items: [product],
  page: 0,
  size: 12,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
};

function renderWishlistPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter initialEntries={["/account/wishlist"]}>
        <WishlistProvider>
          <WishlistPage />
        </WishlistProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe("US-11.3: Wishlist", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetWishlist.mockResolvedValue(wishlistResponse);
    mockedAddToWishlist.mockResolvedValue({ productId: product.id, favorite: true });
    mockedRemoveFromWishlist.mockResolvedValue({ productId: product.id, favorite: false });
  });

  it("renders the authenticated user's wishlist and removes an item", async () => {
    renderWishlistPage();

    expect(await screen.findByText("Sản phẩm yêu thích")).toBeInTheDocument();
    expect(await screen.findByText(product.name)).toBeInTheDocument();
    const favoriteButton = await screen.findByRole("button", {
      name: "Xoá khỏi yêu thích",
    });

    fireEvent.click(favoriteButton);

    await waitFor(() => {
      expect(mockedRemoveFromWishlist).toHaveBeenCalledWith(product.id);
    });
    expect(screen.getAllByText("Đã xoá sản phẩm khỏi danh sách yêu thích.").length).toBeGreaterThan(0);
  });

  it("shows a retryable error when the wishlist API fails", async () => {
    mockedGetWishlist.mockRejectedValue(new Error("Mạng không ổn định"));
    renderWishlistPage();

    expect(await screen.findByText("Mạng không ổn định")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
  });

  it("adds a product and updates the heart state", async () => {
    mockedGetWishlist.mockResolvedValue({ ...wishlistResponse, items: [], totalElements: 0, totalPages: 0 });
    render(
      <ThemeProvider theme={appTheme}>
        <MemoryRouter initialEntries={["/products"]}>
          <WishlistProvider>
            <ProductCard product={product} />
          </WishlistProvider>
        </MemoryRouter>
      </ThemeProvider>,
    );

    const favoriteButton = await screen.findByRole("button", {
      name: "Thêm vào yêu thích",
    });
    fireEvent.click(favoriteButton);

    await waitFor(() => {
      expect(mockedAddToWishlist).toHaveBeenCalledWith(product.id);
      expect(screen.getByRole("button", { name: "Xoá khỏi yêu thích" })).toBeInTheDocument();
    });
  });

  it("renders the flash-sale price together with the original price", async () => {
    const saleProduct: StorefrontProduct = {
      ...product,
      minPrice: 800000,
      maxPrice: 800000,
      originalPrice: 1000000,
      discountPercent: 20,
    };
    mockedGetWishlist.mockResolvedValue({ ...wishlistResponse, items: [], totalElements: 0, totalPages: 0 });
    render(
      <ThemeProvider theme={appTheme}>
        <MemoryRouter initialEntries={["/products"]}>
          <WishlistProvider>
            <ProductCard product={saleProduct} />
          </WishlistProvider>
        </MemoryRouter>
      </ThemeProvider>,
    );

    expect(await screen.findByText(/800\.000/)).toBeInTheDocument();
    expect(screen.getByText(/1\.000\.000/)).toBeInTheDocument();
    expect(screen.getByText("-20%")).toBeInTheDocument();
  });

  it("redirects guests to login when they click the favorite icon", async () => {
    render(
      <ThemeProvider theme={appTheme}>
        <MemoryRouter initialEntries={["/products"]}>
          <Routes>
            <Route path="/products" element={<ProductCard product={product} />} />
            <Route path="/login" element={<div>Trang đăng nhập</div>} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Thêm vào yêu thích" }));
    expect(await screen.findByText("Trang đăng nhập")).toBeInTheDocument();
  });
});
