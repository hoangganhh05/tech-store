# TechStore

Website thương mại điện tử bán điện thoại và phụ kiện công nghệ.

## Backend

Yêu cầu: Java 21+ và MySQL 5.7+.

1. Tạo schema bằng `docs/database_schema.sql`.
2. Sao chép `.env.example` và cung cấp thông tin kết nối qua biến môi trường.
3. Chạy ứng dụng:

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

Các địa chỉ phát triển:

- API health: `http://localhost:8080/api/v1/health`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Chạy test:

```powershell
cd backend
./mvnw.cmd test
```

Các profile được tách tại `application-dev.yml`, `application-test.yml` và `application-prod.yml`. Profile mặc định là `dev`; production bắt buộc cung cấp `DB_URL`, `DB_USERNAME` và `DB_PASSWORD` qua biến môi trường.
