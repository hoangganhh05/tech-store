# TechStore API contract

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
