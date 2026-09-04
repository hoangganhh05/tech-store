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
