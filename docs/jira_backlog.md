# TechStore implementation backlog

## EPIC-00 — Project initialization and preparation

### US-00.1 — Initialize Git repository and conventions

Status: **Done on `TSM-17/setup-git-repository`**

- [x] `T-00.1.1` Create the repository and protect `main` with Pull Request and
  required CI rules.
- [x] `T-00.1.2` Document the project, setup steps, and directory structure in
  `README.md`.
- [x] `T-00.1.3` Document Git flow, branch naming, Pull Request rules, and
  Conventional Commits.

Deliverables:

- `README.md`
- `.gitignore`
- `docs/git-conventions.md`
- GitHub branch protection for `main`

### US-00.2 — Define system architecture and technology stack

Status: **Done on `TSM-18/design-system-architecture`**

- [x] `T-00.2.1` Document the high-level Client, API, Database, email, storage,
  and external-service architecture.
- [x] `T-00.2.2` Select and document the Backend, Frontend, Database, build, and
  test stack.
- [x] `T-00.2.3` Record the main technology and architecture decisions as ADRs.

Deliverables:

- `docs/ARCHITECTURE.md`
- `docs/adr/README.md`
- `docs/adr/0001-technology-stack.md`
- `docs/adr/0002-layered-modular-monolith.md`

### US-00.3 — Complete database design (ERD)

Status: **Done on `TSM-19/design-database-erd`**

- [x] `T-00.3.1` List entities, attributes and business relationships.
- [x] `T-00.3.2` Produce the detailed ERD and document 3NF decisions.
- [x] `T-00.3.3` Provide the MySQL 8.0.16+ initialization schema and basic seed data.

Deliverables:

- `docs/DATABASE_DESIGN.md`
- `docs/database_schema.sql`

### US-00.4 — Initialize the layered Backend skeleton

Status: **Done on `TSM-20/init-backend-skeleton`**

- [x] `T-00.4.1` Initialize Spring Boot, MySQL datasource and dev/test/prod profiles.
- [x] `T-00.4.2` Establish controller/service/repository/entity/dto/mapper layers.
- [x] `T-00.4.3` Add centralized exception handling and the standard `ApiResponse` envelope.
- [x] `T-00.4.4` Integrate OpenAPI and Swagger UI.

### US-00.5 — Initialize the Frontend skeleton

Status: **Done on `TSM-21/init-frontend-skeleton`**

- [x] `T-00.5.1` Initialize React, TypeScript and Vite with development and production scripts.
- [x] `T-00.5.2` Establish storefront and admin layouts with the main application routes.
- [x] `T-00.5.3` Add a shared Axios client with token and 401/403 interceptors.
- [x] `T-00.5.4` Integrate Material UI and a shared responsive theme.

Deliverables:

- Storefront routes: home, product list/detail, cart, checkout and login.
- Admin routes: dashboard, products and orders.
- Frontend type-check, routing tests, HTTP client tests and production build.

### US-00.6 — Set up the basic CI pipeline

Status: **Done on `TSM-22/setup-ci-pipeline`**

- [x] `T-00.6.1` Build and test the Backend in GitHub Actions.
- [x] `T-00.6.2` Lint, test and build the Frontend in GitHub Actions.
- [x] `T-00.6.3` Require `Backend CI` and `Frontend CI` in the remote `main`
  branch protection rule after the workflows have run once.

Deliverables:

- `.github/workflows/backend-ci.yml`
- `.github/workflows/frontend-ci.yml`
- `docs/CI.md`

### US-00.7 — Design wireframes for the main screens

Status: **Done on `TSM-23/design-main-screen-wireframes`**

- [x] `T-00.7.1` Wireframe the main storefront and account screens.
- [x] `T-00.7.2` Wireframe the dashboard, product, order, and inventory admin
  screens.
- [x] `T-00.7.3` Document the end-to-end purchase and admin navigation flows.

Deliverables:

- `docs/wireframes/storefront.svg`
- `docs/wireframes/admin.svg`
- `docs/wireframes/README.md`

## EPIC-01 — Authentication and account management

### US-01.1 — Register an account with email and password

Status: **Done on `TSM-24/register-account`**

- [x] `T-01.1.1` Add User/Role entities, registration DTO, BCrypt password
  encoding, and `POST /api/v1/auth/register`.
- [x] `T-01.1.2` Validate input, reject duplicate emails, assign the CUSTOMER
  role, and return standardized errors.
- [x] `T-01.1.3` Build the registration form with client-side field validation.
- [x] `T-01.1.4` Integrate the registration API, loading/error states, and
  redirect to login after success.
- [x] `T-01.1.5` Add service, controller, integration, and Frontend form tests.

Deliverables:

- `backend/src/main/java/com/techstore/controller/AuthController.java`
- `backend/src/main/java/com/techstore/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/techstore/entity/User.java`
- `backend/src/main/java/com/techstore/entity/Role.java`
- `frontend/src/modules/auth/RegisterPage.tsx`
- `docs/api_contract.md`

### US-01.2 — Log in with email and password

Status: **Done on `TSM-25/login`**

- [x] `T-01.2.1` Add `POST /api/v1/auth/login` and generate signed access and
  refresh JWTs with safe user information.
- [x] `T-01.2.2` Validate login input, keep incorrect-email and
  incorrect-password responses identical, and block locked accounts.
- [x] `T-01.2.3` Build the login form and persist authenticated user state in
  the Frontend Auth Context.
- [x] `T-01.2.4` Integrate login API calls, loading/error states, token storage,
  and automatic Bearer token attachment through the shared HTTP client.
- [x] `T-01.2.5` Add service, controller, integration, HTTP client, and
  Frontend login tests.

Deliverables:

- `backend/src/main/java/com/techstore/security/JwtTokenIssuer.java`
- `backend/src/main/java/com/techstore/controller/AuthController.java`
- `frontend/src/modules/auth/LoginPage.tsx`
- `frontend/src/modules/auth/AuthContext.tsx`
- `docs/api_contract.md`

### US-01.3 — Log out safely

Status: **Done on `TSM-26/logout`**

- [x] `T-01.3.1` Add `POST /api/v1/auth/logout`, persist the signed refresh-token
  identifier at login, and mark it revoked during logout without storing the raw token.
- [x] `T-01.3.2` Clear Frontend authentication state and tokens even if the
  logout request fails, then redirect the user to a public route.
- [x] `T-01.3.3` Show a logout action in logged-in storefront and admin layouts,
  and protect the checkout and admin routes.
- [x] `T-01.3.4` Add service, controller, integration, HTTP client, session,
  and protected-route tests.

Deliverables:

- `backend/src/main/java/com/techstore/entity/RefreshToken.java`
- `backend/src/main/java/com/techstore/controller/AuthController.java`
- `frontend/src/modules/auth/RequireAuth.tsx`
- `frontend/src/modules/auth/AuthContext.tsx`
- `docs/migrations/V20260904_01__add_refresh_tokens.sql`
- `docs/api_contract.md`

### US-01.4 — Reset password by email

Status: **Done on `TSM-27/reset-password`**

- [x] `T-01.4.1` Add `POST /api/v1/auth/forgot-password`; generate a random,
  expiring reset token for an existing account and persist only its hash.
- [x] `T-01.4.2` Send the reset link by SMTP, configure its TTL and Frontend
  base URL through environment variables, and return the same response for
  known and unknown emails.
- [x] `T-01.4.3` Add `POST /api/v1/auth/reset-password`; atomically reject
  invalid, expired, or used links; encode the new password; and revoke active
  refresh-token sessions.
- [x] `T-01.4.4` Build Frontend forgot-password and reset-password forms with
  validation, generic reset-link errors, loading states, and login redirects.
- [x] `T-01.4.5` Add service, controller, integration, and Frontend form tests
  for known/unknown email handling, expiry, one-time use, and old-password
  invalidation.

Deliverables:

- `backend/src/main/java/com/techstore/entity/PasswordResetToken.java`
- `backend/src/main/java/com/techstore/service/impl/PasswordResetServiceImpl.java`
- `backend/src/main/java/com/techstore/infrastructure/mail/SmtpPasswordResetEmailSender.java`
- `frontend/src/modules/auth/ForgotPasswordPage.tsx`
- `frontend/src/modules/auth/ResetPasswordPage.tsx`
- `docs/migrations/V20260905_02__add_password_reset_tokens.sql`
- `docs/api_contract.md`

### US-01.5 — View and update personal profile

Status: **Done on `TSM-28/view-update-profile`**

- [x] `T-01.5.1` Add authenticated `GET /api/v1/users/me` and return full name,
  email, phone, optional date of birth, and update timestamp without sensitive data.
- [x] `T-01.5.2` Add `PUT /api/v1/users/me`, validate editable fields, preserve
  the account email, and reject missing, invalid, or non-access Bearer tokens.
- [x] `T-01.5.3` Add the protected Frontend profile route with initial loading,
  retry, field validation, API error, submit loading, and success states.
- [x] `T-01.5.4` Keep email read-only and synchronize updated full name and phone
  into the active Frontend session.
- [x] `T-01.5.5` Add service, controller, integration, and Frontend tests for
  viewing, updating, validation, authentication, and email immutability.

Deliverables:

- `backend/src/main/java/com/techstore/controller/UserController.java`
- `backend/src/main/java/com/techstore/service/impl/UserProfileServiceImpl.java`
- `backend/src/main/java/com/techstore/security/AccessTokenAuthenticator.java`
- `frontend/src/modules/profile/ProfilePage.tsx`
- `frontend/src/services/userService.ts`
- `docs/migrations/V20260905_03__add_user_date_of_birth.sql`
- `docs/api_contract.md`

### US-01.6 — Change password

Status: **Done on `TSM-29/change-password` (pending commit and Pull Request)**

- [x] `T-01.6.1` Add authenticated `PUT /api/v1/users/me/password`, verify the
  current password, BCrypt-encode the new password, and revoke active refresh sessions.
- [x] `T-01.6.2` Validate required values, enforce the 8–72 character policy,
  reject mismatched confirmation and password reuse, and return standardized errors.
- [x] `T-01.6.3` Add the change-password form to the protected account page.
- [x] `T-01.6.4` Integrate loading and field/server errors, clear the old local
  session on success, and redirect to login with a confirmation message.
- [x] `T-01.6.5` Add service, controller, integration, and Frontend tests for
  successful changes, wrong current passwords, reuse, validation, and authentication.

Deliverables:

- `backend/src/main/java/com/techstore/controller/UserController.java`
- `backend/src/main/java/com/techstore/service/impl/AccountPasswordServiceImpl.java`
- `backend/src/main/java/com/techstore/dto/request/ChangePasswordRequest.java`
- `frontend/src/modules/profile/ChangePasswordForm.tsx`
- `frontend/src/services/userService.ts`
- `docs/api_contract.md`
