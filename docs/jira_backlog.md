# TechStore implementation backlog

## EPIC-00 — Project initialization and preparation

### US-00.3 — Complete database design (ERD)

Status: **Done on `TSM-19/design-database-erd`**

- [x] `T-00.3.1` List entities, attributes and business relationships.
- [x] `T-00.3.2` Produce the detailed ERD and document 3NF decisions.
- [x] `T-00.3.3` Provide the MySQL 5.7+ initialization schema and basic seed data.

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

Status: **In progress on `TSM-22/setup-ci-pipeline`**

- [x] `T-00.6.1` Build and test the Backend in GitHub Actions.
- [x] `T-00.6.2` Lint, test and build the Frontend in GitHub Actions.
- [ ] `T-00.6.3` Require `Backend CI` and `Frontend CI` in the remote `main`
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
