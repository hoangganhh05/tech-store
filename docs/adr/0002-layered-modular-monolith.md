# ADR-0002: Layered modular monolith

- **Status:** Accepted
- **Date:** 2026-09-03
- **Story:** `US-00.2`

## Context

TechStore có nhiều domain nhưng được phát triển bởi một nhóm nhỏ. Microservices
sẽ làm tăng chi phí deployment, quan sát hệ thống, consistency và xử lý lỗi phân
tán trước khi có nhu cầu scale độc lập thực tế.

## Decision

Xây Backend dưới dạng modular monolith và tổ chức theo các layer
controller–service–repository–entity, kết hợp DTO và mapper tại ranh giới API.

Quy tắc phụ thuộc:

- Controller chỉ điều phối HTTP và gọi service.
- Service sở hữu use case, transaction và business rule.
- Repository chỉ xử lý persistence.
- Entity không được trả trực tiếp qua API.
- Tích hợp email, storage và payment nằm sau interface trong infrastructure.
- Module không truy cập repository nội bộ của module khác nếu có thể sử dụng
  service/use case công khai.

## Consequences

- Deployment và transaction ban đầu đơn giản.
- Có thể refactor package theo domain mà không đổi topology triển khai.
- Cần review để tránh service lớn hoặc phụ thuộc vòng giữa module.
- Nếu một module cần scale/triển khai độc lập, có thể tách sau khi đã xác định rõ
  boundary và contract; quyết định đó phải có ADR mới.

## Alternatives considered

- Microservices: chưa chọn vì chi phí vận hành và consistency lớn hơn lợi ích ở
  quy mô hiện tại.
- Một package phân lớp duy nhất không có boundary module: đơn giản lúc đầu nhưng
  khó kiểm soát coupling khi thêm catalog, inventory, order và voucher.
