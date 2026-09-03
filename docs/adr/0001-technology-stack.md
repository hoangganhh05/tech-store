# ADR-0001: Technology stack

- **Status:** Accepted
- **Date:** 2026-09-03
- **Story:** `US-00.2`

## Context

TechStore cần một stack dễ phát triển trên Windows, có type safety, hỗ trợ giao
dịch thương mại điện tử, sinh tài liệu API và có thể kiểm tra tự động trên CI.
Nhóm cần ưu tiên công nghệ phổ biến, tài liệu tốt và không tạo thêm hạ tầng vận
hành ở giai đoạn đầu.

## Decision

- Dùng Java 21 và Spring Boot 3.4 cho Backend.
- Dùng Maven Wrapper để build nhất quán ở local và CI.
- Dùng Spring Data JPA/Hibernate và MySQL 8.0+ cho persistence.
- Dùng React 19, TypeScript và Vite cho Frontend SPA.
- Dùng Material UI làm component library và theme foundation.
- Dùng Axios cho HTTP, React Router cho routing.
- Dùng JUnit/Spring Boot Test và Vitest/Testing Library cho kiểm thử.
- Dùng GitHub Actions cho CI trên Pull Request.
- Dùng OpenAPI 3 qua springdoc và Swagger UI để mô tả API.

## Rationale

- Spring Boot cung cấp validation, transaction, security và hệ sinh thái phù hợp
  với hệ thống nghiệp vụ nhiều module.
- Java 21 là bản LTS và được CI hỗ trợ ổn định.
- MySQL phù hợp với dữ liệu quan hệ và transaction của đơn hàng/tồn kho.
- TypeScript giảm lỗi contract phía client; Vite có vòng lặp phát triển nhanh.
- Material UI cung cấp component có accessibility và theme nhất quán.
- Các công cụ test đã tích hợp trực tiếp với framework được chọn.

## Consequences

- Nhóm cần duy trì contract DTO giữa Backend và Frontend.
- ORM không thay thế việc thiết kế index, constraint và transaction cẩn thận.
- SPA cần chiến lược token, route guard và xử lý trạng thái loading/error.
- Bundle Frontend phải được theo dõi và tách chunk khi tính năng tăng.
- Việc đổi database hoặc framework UI là thay đổi lớn và phải có ADR mới.

## Alternatives considered

- Node.js Backend: cùng ngôn ngữ với Frontend nhưng không được chọn vì stack
  Java/Spring phù hợp hơn với mục tiêu học tập và transaction nghiệp vụ.
- PostgreSQL: khả năng mạnh nhưng máy phát triển đã chuẩn hóa trên MySQL.
- Tailwind làm UI foundation: linh hoạt nhưng cần tự xây dựng nhiều primitive và
  quy tắc accessibility hơn Material UI.
