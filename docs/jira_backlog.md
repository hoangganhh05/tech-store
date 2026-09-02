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
