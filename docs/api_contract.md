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
