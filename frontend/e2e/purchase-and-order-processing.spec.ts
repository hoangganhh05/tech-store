import { expect, test, type Page } from "@playwright/test";

const API_PREFIX = "/api/v1";
const NOW = "2026-09-10T07:00:00.000Z";

const customerUser = {
  id: 101,
  email: "customer.e2e@example.test",
  fullName: "Khách hàng E2E",
  phone: "0900000001",
  status: "ACTIVE",
  roles: ["CUSTOMER"],
  emailVerified: true,
  createdAt: NOW,
};

const adminUser = {
  id: 1,
  email: "admin.e2e@example.test",
  fullName: "Quản trị viên E2E",
  phone: "0900000002",
  status: "ACTIVE",
  roles: ["ADMIN"],
  emailVerified: true,
  createdAt: NOW,
};

const pageResponse = <T>(items: T[]) => ({
  items,
  page: 0,
  size: 10,
  totalElements: items.length,
  totalPages: items.length ? 1 : 0,
  first: true,
  last: true,
});

const apiResponse = <T>(data: T) => ({
  success: true,
  code: "SUCCESS",
  message: "OK",
  data,
  timestamp: NOW,
});

const product = {
  id: 1,
  name: "Điện thoại E2E",
  description: "Sản phẩm phục vụ kiểm thử luồng mua hàng.",
  brandId: 1,
  brandName: "TechStore",
  categoryId: 1,
  categoryName: "Điện thoại",
  status: "ACTIVE",
  minPrice: 10_000_000,
  maxPrice: 10_000_000,
  originalPrice: 11_000_000,
  discountPercent: 9,
  totalStock: 5,
  hasStock: true,
  salesCount: 0,
  rating: 0,
  variants: [
    {
      id: 11,
      productId: 1,
      productName: "Điện thoại E2E",
      sku: "E2E-PHONE-BLACK-128",
      color: "Đen",
      storage: "128GB",
      price: 10_000_000,
      originalPrice: 11_000_000,
      stockQuantity: 5,
      status: "ACTIVE",
      stockStatus: "IN_STOCK",
      createdAt: NOW,
      updatedAt: NOW,
    },
  ],
  availableColors: ["Đen"],
  availableStorages: ["128GB"],
  images: [],
  specifications: [],
  createdAt: NOW,
  updatedAt: NOW,
};

const address = {
  id: 21,
  recipientName: customerUser.fullName,
  phone: customerUser.phone,
  province: "Hải Phòng",
  district: "Lê Chân",
  ward: "Dư Hàng",
  streetAddress: "1 Đường E2E",
  isDefault: true,
  createdAt: NOW,
};

function emptyCart() {
  return {
    id: 61,
    totalItems: 0,
    subtotal: 0,
    shippingFee: 30_000,
    discountAmount: 0,
    total: 30_000,
    hasStockIssue: false,
    canCheckout: false,
    items: [],
  };
}

function cartWithProduct() {
  return {
    id: 61,
    totalItems: 1,
    subtotal: 10_000_000,
    shippingFee: 30_000,
    discountAmount: 0,
    total: 10_030_000,
    hasStockIssue: false,
    canCheckout: true,
    items: [
      {
        id: 91,
        variantId: 11,
        productId: 1,
        productName: product.name,
        sku: "E2E-PHONE-BLACK-128",
        color: "Đen",
        storage: "128GB",
        price: 10_000_000,
        originalPrice: 11_000_000,
        imageUrl: null,
        quantity: 1,
        availableStock: 5,
        subtotal: 10_000_000,
        hasStockIssue: false,
      },
    ],
  };
}

async function fulfillJson(
  route: Parameters<Parameters<Page["route"]>[1]>[0],
  data: unknown,
) {
  await route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(apiResponse(data)),
  });
}

async function seedCustomerSession(page: Page) {
  await page.addInitScript((user) => {
    window.localStorage.setItem("techstore.accessToken", "customer-e2e-token");
    window.localStorage.setItem("techstore.refreshToken", "customer-e2e-refresh");
    window.localStorage.setItem("techstore.authUser", JSON.stringify(user));
    window.localStorage.setItem("techstore.sessionId", "customer-e2e-session");
  }, customerUser);
}

async function mockCustomerPurchaseApi(page: Page) {
  let cart = emptyCart();
  let placedOrderPayload: { addressId: number; paymentMethod: string } | null = null;
  let orderPlaced = false;

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();

    if (method === "GET" && path === `${API_PREFIX}/products/1`) {
      return fulfillJson(route, product);
    }
    if (method === "GET" && path === `${API_PREFIX}/products/1/related`) {
      return fulfillJson(route, []);
    }
    if (method === "GET" && path === `${API_PREFIX}/products/1/reviews`) {
      return fulfillJson(route, {
        averageRating: 0,
        totalReviews: 0,
        reviews: pageResponse([]),
      });
    }
    if (
      method === "GET" &&
      path === `${API_PREFIX}/products/1/variants/11/stock`
    ) {
      return fulfillJson(route, {
        variantId: 11,
        productId: 1,
        sku: "E2E-PHONE-BLACK-128",
        stockQuantity: 5,
        stockStatus: "IN_STOCK",
        isAvailable: true,
      });
    }
    if (method === "GET" && path === `${API_PREFIX}/wishlist`) {
      return fulfillJson(route, pageResponse([]));
    }
    if (method === "POST" && path === `${API_PREFIX}/cart/sync`) {
      return fulfillJson(route, {
        cart,
        mergedItemsCount: 0,
        hasStockAdjusted: false,
        message: "",
      });
    }
    if (method === "GET" && path === `${API_PREFIX}/cart`) {
      return fulfillJson(route, cart);
    }
    if (method === "POST" && path === `${API_PREFIX}/cart/items`) {
      const payload = request.postDataJSON() as { variantId: number; quantity: number };
      if (payload.variantId === 11 && payload.quantity === 1) cart = cartWithProduct();
      return fulfillJson(route, cart);
    }
    if (method === "POST" && path === `${API_PREFIX}/cart/validate`) {
      return fulfillJson(route, { valid: true, issues: [] });
    }
    if (method === "GET" && path === `${API_PREFIX}/users/me/addresses`) {
      return fulfillJson(route, [address]);
    }
    if (method === "GET" && path === `${API_PREFIX}/checkout/payment-methods`) {
      return fulfillJson(route, [
        {
          paymentMethod: "COD",
          label: "Thanh toán khi nhận hàng",
          instructions: "Thanh toán cho nhân viên giao hàng.",
        },
      ]);
    }
    if (method === "POST" && path === `${API_PREFIX}/checkout/payment-method`) {
      return fulfillJson(route, {
        paymentMethod: "COD",
        label: "Thanh toán khi nhận hàng",
        instructions: "Thanh toán cho nhân viên giao hàng.",
      });
    }
    if (method === "POST" && path === `${API_PREFIX}/checkout/review`) {
      return fulfillJson(route, {
        cart,
        shippingAddress: address,
        paymentMethod: {
          paymentMethod: "COD",
          label: "Thanh toán khi nhận hàng",
          instructions: "Thanh toán cho nhân viên giao hàng.",
        },
        readyToPlaceOrder: true,
        voucher: null,
      });
    }
    if (method === "POST" && path === `${API_PREFIX}/orders`) {
      placedOrderPayload = request.postDataJSON() as {
        addressId: number;
        paymentMethod: string;
      };
      orderPlaced = true;
      cart = emptyCart();
      return fulfillJson(route, {
        id: 501,
        orderNumber: "TS-E2E-501",
        status: "PENDING",
        totalAmount: 10_030_000,
        placedAt: NOW,
        estimatedProcessingTime: "1-2 ngày làm việc",
        items: [
          {
            productName: product.name,
            variantLabel: "Đen · 128GB",
            unitPrice: 10_000_000,
            quantity: 1,
            subtotal: 10_000_000,
          },
        ],
      });
    }
    if (method === "GET" && path === `${API_PREFIX}/orders/my-orders`) {
      return fulfillJson(
        route,
        pageResponse(
          orderPlaced
            ? [
                {
                  id: 501,
                  orderNumber: "TS-E2E-501",
                  placedAt: NOW,
                  totalAmount: 10_030_000,
                  status: "PENDING",
                },
              ]
            : [],
        ),
      );
    }

    return fulfillJson(route, null);
  });

  return {
    placedOrderPayload: () => placedOrderPayload,
  };
}

test("khách hàng mua sản phẩm từ chi tiết đến lịch sử đơn hàng", async ({ page }) => {
  const api = await mockCustomerPurchaseApi(page);
  await seedCustomerSession(page);

  await page.goto("/products/1");
  await expect(page.getByTestId("product-title")).toHaveText(product.name);
  await page.getByTestId("color-option-Đen").click();
  await page.getByTestId("storage-option-128GB").click();

  const addToCartResponse = page.waitForResponse(
    (response) =>
      response.url().includes("/api/v1/cart/items") &&
      response.request().method() === "POST",
  );
  await page.getByTestId("add-to-cart-btn").click();
  await addToCartResponse;

  await page.goto("/cart");
  await expect(page.getByTestId("cart-item-91")).toContainText(product.name);
  await page.getByTestId("checkout-btn").click();

  await expect(page.getByTestId("checkout-step-address")).toBeVisible();
  await expect(page.getByTestId("address-card-21")).toContainText(address.streetAddress);
  await page.getByTestId("continue-to-payment-btn").click();

  await expect(page.getByTestId("checkout-step-payment")).toBeVisible();
  await page.getByLabel("Thanh toán khi nhận hàng").check();
  await page.getByRole("button", { name: "Tiếp tục xem lại đơn hàng" }).click();

  await expect(page.getByTestId("order-review-content")).toBeVisible();
  const placeOrderResponse = page.waitForResponse(
    (response) =>
      response.url().includes("/api/v1/orders") &&
      response.request().method() === "POST",
  );
  await page.getByTestId("place-order-btn").click();
  await placeOrderResponse;

  await expect(page.getByTestId("order-confirmation")).toBeVisible();
  await expect(page.getByTestId("order-number")).toHaveText("TS-E2E-501");
  expect(api.placedOrderPayload()).toEqual({ addressId: 21, paymentMethod: "COD" });

  await page.goto("/account/orders");
  await expect(page.getByRole("table", { name: "Lịch sử đơn hàng" })).toContainText(
    "TS-E2E-501",
  );
});

test("quản trị viên xác nhận, giao, hoàn thành đơn và kiểm tra tồn kho", async ({
  page,
}) => {
  let inventoryQueries = 0;
  const statusUpdates: string[] = [];
  let currentStatus = "PENDING";
  const statusHistory = [
    { status: "PENDING", changedAt: NOW, changedBy: null },
  ];

  const orderDetail = () => ({
    id: 501,
    orderNumber: "TS-E2E-501",
    customer: {
      id: customerUser.id,
      fullName: customerUser.fullName,
      email: customerUser.email,
      phone: customerUser.phone,
    },
    status: currentStatus,
    paymentMethod: "COD",
    paymentStatus: "UNPAID",
    cancellationReason: null,
    internalNote: null,
    subtotal: 10_000_000,
    discountAmount: 0,
    shippingFee: 30_000,
    totalAmount: 10_030_000,
    placedAt: NOW,
    shippingAddress: {
      recipientName: customerUser.fullName,
      recipientPhone: customerUser.phone,
      line1: address.streetAddress,
      ward: address.ward,
      district: address.district,
      province: address.province,
    },
    items: [
      {
        productName: product.name,
        sku: "E2E-PHONE-BLACK-128",
        variantLabel: "Đen · 128GB",
        unitPrice: 10_000_000,
        quantity: 1,
        subtotal: 10_000_000,
      },
    ],
    statusHistory,
  });

  const inventoryItem = {
    id: 11,
    variantId: 11,
    sku: "E2E-PHONE-BLACK-128",
    productId: 1,
    productName: product.name,
    categoryName: "Điện thoại",
    brandName: "TechStore",
    color: "Đen",
    storage: "128GB",
    price: 10_000_000,
    // Five units before the purchase, four units after it. Status changes do not deduct stock again.
    quantityOnHand: 4,
    quantityReserved: 0,
    availableQuantity: 4,
    lowStockThreshold: 2,
    stockStatus: "IN_STOCK",
    updatedAt: NOW,
  };

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();

    if (method === "POST" && path === `${API_PREFIX}/auth/admin/login`) {
      return fulfillJson(route, {
        accessToken: "admin-e2e-token",
        refreshToken: "admin-e2e-refresh",
        tokenType: "Bearer",
        accessTokenExpiresAt: "2026-09-11T07:00:00.000Z",
        refreshTokenExpiresAt: "2026-09-17T07:00:00.000Z",
        user: adminUser,
      });
    }
    if (method === "GET" && path === `${API_PREFIX}/admin/dashboard`) {
      return fulfillJson(route, {
        period: "DAY",
        fromDate: "2026-09-10",
        toDate: "2026-09-10",
        totalRevenue: 0,
        totalOrders: 0,
        ordersByStatus: [],
        topSellingProducts: [],
        revenueTrend: [],
      });
    }
    if (method === "GET" && path === `${API_PREFIX}/admin/notifications`) {
      return fulfillJson(route, { ...pageResponse([]), unreadCount: 0 });
    }
    if (method === "GET" && path === `${API_PREFIX}/admin/orders/501`) {
      return fulfillJson(route, orderDetail());
    }
    if (method === "PATCH" && path === `${API_PREFIX}/admin/orders/501/status`) {
      const payload = request.postDataJSON() as { status: string };
      currentStatus = payload.status;
      statusUpdates.push(payload.status);
      statusHistory.push({
        status: payload.status,
        changedAt: NOW,
        changedBy: { id: adminUser.id, fullName: adminUser.fullName },
      });
      return fulfillJson(route, orderDetail());
    }
    if (method === "GET" && path === `${API_PREFIX}/admin/categories`) {
      return fulfillJson(route, []);
    }
    if (method === "GET" && path === `${API_PREFIX}/admin/inventory/summary`) {
      return fulfillJson(route, {
        totalVariants: 1,
        inStockCount: 1,
        lowStockCount: 0,
        outOfStockCount: 0,
      });
    }
    if (method === "GET" && path === `${API_PREFIX}/admin/inventory`) {
      inventoryQueries += 1;
      return fulfillJson(route, pageResponse([inventoryItem]));
    }
    if (method === "GET" && path === `${API_PREFIX}/cart`) {
      return fulfillJson(route, emptyCart());
    }

    return fulfillJson(route, null);
  });

  await page.goto("/admin/login");
  await page.getByLabel("Email quản trị viên").fill(adminUser.email);
  await page.getByLabel("Mật khẩu").fill("admin-password");
  await page.getByRole("button", { name: "Đăng nhập Quản trị" }).click();
  await page.waitForURL("**/admin");

  await page.goto("/admin/orders/501");
  await expect(page.getByRole("heading", { name: "Chi tiết đơn hàng" })).toBeVisible();

  const updateStatus = async (option: string, expectedLabel: string) => {
    await page.getByLabel("Trạng thái mới").click();
    await page.getByRole("option", { name: option }).click();
    const response = page.waitForResponse(
      (candidate) =>
        candidate.url().includes("/api/v1/admin/orders/501/status") &&
        candidate.request().method() === "PATCH",
    );
    await page.getByRole("button", { name: "Cập nhật trạng thái" }).click();
    await response;
    await expect(page.getByText("Đã cập nhật trạng thái đơn hàng.")).toBeVisible();
    await expect(page.getByText(expectedLabel, { exact: true }).first()).toBeVisible();
  };

  await updateStatus("Xác nhận đơn hàng", "Đã xác nhận");
  await updateStatus("Chuyển sang đang giao", "Đang giao");
  await updateStatus("Hoàn thành đơn hàng", "Hoàn thành");
  expect(statusUpdates).toEqual(["CONFIRMED", "SHIPPING", "COMPLETED"]);

  await page.goto("/admin/inventory");
  const inventoryRow = page
    .getByRole("row")
    .filter({ hasText: "E2E-PHONE-BLACK-128" });
  await expect(inventoryRow).toContainText("4");
  expect(inventoryQueries).toBeGreaterThan(0);
});
