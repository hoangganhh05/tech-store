# Đăng Tùng Mobile API contract

All application endpoints use the `/api/v1` base path and return the common
`ApiResponse` envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "...",
  "data": {},
  "timestamp": "2026-09-04T00:00:00Z"
}
```

## Register a customer account

`POST /api/v1/auth/register`

Request body:

```json
{
  "fullName": "Nguyen Van A",
  "email": "customer@example.com",
  "phone": "0901234567",
  "password": "strong-password",
  "confirmPassword": "strong-password"
}
```

Validation rules:

- `fullName`, `email`, `phone`, `password`, and `confirmPassword` are required.
- `email` must be a valid address and is normalized to lowercase.
- `password` must contain 8–72 characters.
- `phone` accepts 7–20 digits and common separators (`+`, spaces, `.`, `-`,
  parentheses).
- `confirmPassword` must match `password`.

Successful response: `201 Created`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "fullName": "Nguyen Van A",
    "phone": "0901234567",
    "status": "ACTIVE",
    "roles": ["CUSTOMER"],
    "emailVerified": false,
    "createdAt": "2026-09-04T00:00:00Z"
  },
  "timestamp": "2026-09-04T00:00:00Z"
}
```

Error responses:

- `400 Bad Request`, code `VALIDATION_ERROR`: field-specific validation
  messages are joined in `message` (for example, `email: ...; password: ...`).
- `409 Conflict`, code `EMAIL_ALREADY_EXISTS`: the email is already registered.

The password is encoded with BCrypt before persistence and is never included in
the response. New accounts receive the `CUSTOMER` role and are redirected to
the login screen by the Frontend after a successful registration.

## Login with email and password

`POST /api/v1/auth/login`

Request body:

```json
{
  "email": "customer@example.com",
  "password": "strong-password"
}
```

Successful response: `200 OK`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "<signed-jwt-access-token>",
    "refreshToken": "<signed-jwt-refresh-token>",
    "tokenType": "Bearer",
    "accessTokenExpiresAt": "2026-09-04T00:15:00Z",
    "refreshTokenExpiresAt": "2026-09-11T00:00:00Z",
    "user": {
      "id": 1,
      "email": "customer@example.com",
      "fullName": "Nguyen Van A",
      "phone": "0901234567",
      "status": "ACTIVE",
      "roles": ["CUSTOMER"],
      "emailVerified": false,
      "createdAt": "2026-09-04T00:00:00Z"
    }
  },
  "timestamp": "2026-09-04T00:00:00Z"
}
```

The two tokens are signed JWTs. Access tokens expire after 15 minutes and
refresh tokens after 7 days by default; configure `JWT_SECRET`,
`JWT_ACCESS_TOKEN_TTL`, and `JWT_REFRESH_TOKEN_TTL` in the runtime environment.
`JWT_SECRET` must contain at least 32 characters and must never be committed.

Error responses:

- `400 Bad Request`, code `VALIDATION_ERROR`: missing or malformed fields.
- `401 Unauthorized`, code `INVALID_CREDENTIALS`: the exact same generic
  message is returned for an unknown email and an incorrect password.
- `423 Locked`, code `ACCOUNT_LOCKED`: correct credentials were supplied for a
  locked account.

After a successful login, the Frontend stores the returned token pair and
attaches the access token as `Authorization: Bearer <token>` to subsequent API
requests.

## Log out and revoke a refresh token

`POST /api/v1/auth/logout`

Request body:

```json
{
  "refreshToken": "<signed-jwt-refresh-token>"
}
```

Successful response: `200 OK`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Đăng xuất thành công",
  "data": null,
  "timestamp": "2026-09-04T00:00:00Z"
}
```

At login, the server stores only the refresh token's signed JWT identifier
(`jti`). Logout marks that identifier as revoked, so a future refresh-token
endpoint can reject the session without storing the raw token. Repeating logout
with the same valid refresh token is safe and returns success.

Error responses:

- `400 Bad Request`, code `VALIDATION_ERROR`: `refreshToken` is missing or blank.
- `401 Unauthorized`, code `INVALID_REFRESH_TOKEN`: the token is malformed,
  expired, signed with a different key, is an access token, or is not known to
  the server.

The Frontend clears its local access token, refresh token, and authenticated
user state even when the logout request cannot reach the server; it then sends
the user to a public route.

## Request a password-reset link

`POST /api/v1/auth/forgot-password`

Request body:

```json
{
  "email": "customer@example.com"
}
```

`email` is required, must be a valid address, and is normalized to lowercase.

Successful response: `200 OK`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Nếu email này thuộc về một tài khoản, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.",
  "data": null,
  "timestamp": "2026-09-05T00:00:00Z"
}
```

The response is identical whether the email exists or not, so this endpoint
does not disclose registered email addresses. For an existing account, the
server invalidates older unused reset links, stores only a hash of a newly
generated random token, and sends a link to
`PASSWORD_RESET_FRONTEND_URL/reset-password?token=...`. The link lifetime is
configured by `PASSWORD_RESET_TOKEN_TTL` (30 minutes by default).

In the `prod` profile, `MAIL_HOST`, `MAIL_FROM`, and
`PASSWORD_RESET_FRONTEND_URL` are required at startup so a deployment cannot
silently claim to send reset links without SMTP and a public Frontend URL.

Error response:

- `400 Bad Request`, code `VALIDATION_ERROR`: `email` is missing or malformed.

## Reset a password with a link

`POST /api/v1/auth/reset-password`

Request body:

```json
{
  "token": "<random-reset-token-from-email>",
  "password": "new-strong-password",
  "confirmPassword": "new-strong-password"
}
```

Validation rules:

- `token`, `password`, and `confirmPassword` are required.
- `password` must contain 8–72 characters and match `confirmPassword`.
- The password must differ from the current password.

Successful response: `200 OK`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Đặt lại mật khẩu thành công",
  "data": null,
  "timestamp": "2026-09-05T00:00:00Z"
}
```

On success, the server BCrypt-encodes the new password, consumes the link so it
cannot be reused, invalidates the account's other unused links, and revokes all
of the user's active refresh-token sessions. The previous password can no
longer authenticate.

Error responses:

- `400 Bad Request`, code `VALIDATION_ERROR`: required fields are missing, the
  password is outside the allowed length, or the confirmation differs.
- `400 Bad Request`, code `INVALID_PASSWORD_RESET_TOKEN`: the link token is
  invalid, expired, or already used. These cases intentionally share one
  message.
- `400 Bad Request`, code `PASSWORD_MUST_BE_DIFFERENT`: the submitted password
  matches the current password.

## View the authenticated user's profile

`GET /api/v1/users/me`

Header: `Authorization: Bearer <access-token>`

Successful response: `200 OK`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request completed successfully",
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "fullName": "Nguyen Van A",
    "phone": "0901234567",
    "dateOfBirth": "2000-05-20",
    "updatedAt": "2026-09-05T08:00:00Z"
  },
  "timestamp": "2026-09-05T08:00:00Z"
}
```

`dateOfBirth` is `null` when it has not been provided. Password data is never
returned.

## Update the authenticated user's profile

`PUT /api/v1/users/me`

Header: `Authorization: Bearer <access-token>`

Request body:

```json
{
  "fullName": "Nguyen Van B",
  "phone": "0987654321",
  "dateOfBirth": "2000-05-20"
}
```

`fullName` and `phone` are required. `dateOfBirth` is optional but, when
present, must be in the past. Email is intentionally absent from the request
contract and cannot be changed by this endpoint. A successful request returns
the same profile shape as `GET /users/me` and the message
`Cập nhật thông tin cá nhân thành công`.

Error responses for both profile endpoints:

- `401 Unauthorized`, code `INVALID_ACCESS_TOKEN`: the Bearer token is missing,
  malformed, expired, not signed by this server, or is a refresh token.
- `400 Bad Request`, code `VALIDATION_ERROR`: editable fields are invalid; the
  message identifies each invalid field.
- `423 Locked`, code `ACCOUNT_LOCKED`, or `403 Forbidden`, code
  `ACCOUNT_DISABLED`: the account is no longer allowed to use authenticated
  features.

## Change the authenticated user's password

`PUT /api/v1/users/me/password`

Header: `Authorization: Bearer <access-token>`

Request body:

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-strong-password",
  "confirmPassword": "new-strong-password"
}
```

The current password must be correct. The new password must contain 8–72
characters, match `confirmPassword`, and differ from the current password.

Successful response: `200 OK`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Đổi mật khẩu thành công",
  "data": null,
  "timestamp": "2026-09-05T08:00:00Z"
}
```

On success, the server BCrypt-encodes the new password and revokes every active
refresh-token session for the account. The Frontend clears the current local
session and sends the user to login again.

Error responses:

- `401 Unauthorized`, code `INVALID_ACCESS_TOKEN`: the Bearer access token is
  missing, invalid, or expired.
- `400 Bad Request`, code `VALIDATION_ERROR`: required fields are missing, the
  new password is outside the allowed length, or confirmation does not match.
- `400 Bad Request`, code `INVALID_CURRENT_PASSWORD`: the current password is
  incorrect.
- `400 Bad Request`, code `PASSWORD_MUST_BE_DIFFERENT`: the new password is the
  same as the current password.
- `423 Locked`, code `ACCOUNT_LOCKED`, or `403 Forbidden`, code
  `ACCOUNT_DISABLED`: the account cannot use authenticated features.

## Checkout payment methods (US-08.2)

Both endpoints require a valid Bearer access token with the CUSTOMER role.

### GET /api/v1/checkout/payment-methods

Returns the standard `ApiResponse` envelope with `data` containing options:
`{ "paymentMethod": "COD", "label": "...", "instructions": "..." }`.
Supported values: `COD`, `BANK_TRANSFER`, `ONLINE`. Transfer and online
instructions explicitly describe a simulation; no payment gateway is contacted.

### POST /api/v1/checkout/payment-method

Request:

```json
{ "paymentMethod": "BANK_TRANSFER" }
```

Returns 200 with the selected option in `data`. This endpoint validates a
checkout selection; it does not create an order, persist a draft or mark a
payment as paid. The frontend retains the selection while navigating checkout.
The order submission in US-08.4 must pass the selected enum to the Order
constructor; `orders.payment_method` persists its string value and is NOT NULL.

Errors: 400 `VALIDATION_ERROR` for missing/null/unknown/numeric method or malformed
JSON; 401 `INVALID_ACCESS_TOKEN` for missing/invalid/expired authentication;
403 `ACCESS_DENIED` for a session without CUSTOMER role.

## Place order (US-08.4)

`POST /api/v1/orders` requires a CUSTOMER access token and accepts `addressId`
and the selected `paymentMethod`. The server reads the authenticated user's
cart, creates an Order plus immutable item/address snapshots, locks inventory
rows in deterministic variant order, deducts stock, and clears CartItems in one
transaction. Any inventory failure rolls back the order, stock and cart changes.

The success response contains `id`, `orderNumber`, `status`, `totalAmount` and
`placedAt`; the frontend navigates to the order confirmation page. The endpoint
returns 400 for invalid input or insufficient stock, 401 for missing/invalid
authentication, 403 for a non-CUSTOMER role, and 404 for an unknown/foreign
address or empty cart.

## Order confirmation (US-08.5)

The place-order success response also contains `estimatedProcessingTime` and
the immutable `items` summary used by the confirmation page. After the order
transaction commits, the backend publishes an event and sends an email with
the order code, placed time, item summary, total and expected processing time
asynchronously. SMTP errors are logged and do not change the successful order
response.

## Checkout order review (US-08.3)

### POST /api/v1/checkout/review

Requires a Bearer access token with the CUSTOMER role. The server reloads the
authenticated customer's current cart and verifies that the selected address
belongs to that customer.

Request:

```json
{
  "addressId": 12,
  "paymentMethod": "COD"
}
```

The standard response envelope contains `cart`, `shippingAddress`,
`paymentMethod`, and `readyToPlaceOrder`. The cart includes the product and
variant lines, quantities, prices, subtotal, shipping fee, discount and total.
`readyToPlaceOrder` is true only when the cart is non-empty and has no current
stock issue. This endpoint does not create an order or deduct inventory.

Errors: 400 `VALIDATION_ERROR` for a missing/non-positive address or missing/
unknown payment method; 401 `INVALID_ACCESS_TOKEN`; 403 `ACCESS_DENIED`; 404
`ADDRESS_NOT_FOUND` for an unknown or another customer's address; 404
`CART_NOT_FOUND` for an empty cart.

## Apply voucher (US-08.6)

`POST /api/v1/checkout/voucher` requires a CUSTOMER token and accepts
`{ "code": "SAVE10" }`. The server validates the code against the current
cart and returns the discount and recalculated total. Checkout review and
place-order also accept an optional `voucherCode`; place-order validates the
voucher again while locking its usage row so a stale review cannot apply an
expired or exhausted code.

Invalid, expired, minimum-order and usage-limit cases return a descriptive
`VOUCHER_NOT_FOUND`, `VOUCHER_NOT_ELIGIBLE` or `VOUCHER_USAGE_LIMIT_REACHED`
error. The discount never exceeds the product subtotal.
