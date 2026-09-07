import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useSearchParams } from "react-router-dom";
import { ThemeProvider } from "@mui/material";
import { appTheme } from "../configs/theme";
import { StorefrontLayout } from "../layouts/StorefrontLayout";
import { AuthProvider } from "../modules/auth/AuthContext";

function DummyProductsPage() {
  const [params] = useSearchParams();
  return (
    <div>
      <h1>Trang sản phẩm</h1>
      <span data-testid="url-q">{params.get("q") || "none"}</span>
    </div>
  );
}

function DummyHomePage() {
  return <h1>Trang chủ</h1>;
}

function renderHeaderSearch(initialRoute = "/") {
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider>
        <MemoryRouter initialEntries={[initialRoute]}>
          <Routes>
            <Route element={<StorefrontLayout />}>
              <Route path="/" element={<DummyHomePage />} />
              <Route path="/products" element={<DummyProductsPage />} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

describe("US-05.3: Storefront Header Search", () => {
  it("renders the search input in the header toolbar", () => {
    renderHeaderSearch("/");

    const searchInput = screen.getByTestId("header-search-input");
    expect(searchInput).toBeInTheDocument();
    expect(searchInput).toHaveAttribute("placeholder", "Tìm sản phẩm...");
    expect(screen.getByTestId("header-search-button")).toBeInTheDocument();
  });

  it("submits search keyword and navigates to /products?q=keyword", async () => {
    renderHeaderSearch("/");

    const searchInput = screen.getByTestId("header-search-input");
    fireEvent.change(searchInput, { target: { value: "iPhone 15" } });

    const searchButton = screen.getByTestId("header-search-button");
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Trang sản phẩm" })).toBeInTheDocument();
      expect(screen.getByTestId("url-q")).toHaveTextContent("iPhone 15");
    });
  });

  it("submits search when pressing Enter key in search input", async () => {
    renderHeaderSearch("/");

    const searchInput = screen.getByTestId("header-search-input");
    fireEvent.change(searchInput, { target: { value: "MacBook" } });
    fireEvent.submit(searchInput.closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Trang sản phẩm" })).toBeInTheDocument();
      expect(screen.getByTestId("url-q")).toHaveTextContent("MacBook");
    });
  });

  it("clears search keyword and navigates back to /products when clear button is clicked", async () => {
    renderHeaderSearch("/products?q=Samsung");

    const searchInput = screen.getByTestId("header-search-input") as HTMLInputElement;
    expect(searchInput.value).toBe("Samsung");

    // Click clear button
    const clearBtn = screen.getByRole("button", { name: /Xoá từ khoá tìm kiếm/i });
    expect(clearBtn).toBeInTheDocument();
    fireEvent.click(clearBtn);

    expect(searchInput.value).toBe("");
    await waitFor(() => {
      expect(screen.getByTestId("url-q")).toHaveTextContent("none");
    });
  });
});
