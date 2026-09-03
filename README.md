# TechStore

Website thương mại điện tử bán điện thoại và phụ kiện công nghệ.

## Backend

Yêu cầu: Java 21+ và MySQL 8.0.16+ (khuyến nghị MySQL 8.4).

1. Khởi động MySQL và tạo schema bằng `docs/database_schema.sql` theo hướng dẫn
   trong `docs/DATABASE_DESIGN.md`.
2. Mở PowerShell tại thư mục gốc và cung cấp thông tin kết nối cho phiên terminal
   hiện tại. Thay giá trị mật khẩu bằng mật khẩu MySQL trên máy của bạn:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_URL = "jdbc:mysql://localhost:3306/techstore?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-mysql-password>"
```

File `.env.example` chỉ là danh sách biến mẫu; Spring Boot không tự động đọc file
`.env`. Không commit mật khẩu thật vào repository.

3. Chạy ứng dụng:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Các địa chỉ phát triển:

- API health: `http://localhost:8080/api/v1/health`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Chạy test:

```powershell
cd backend
.\mvnw.cmd test
```

Các profile được tách tại `application-dev.yml`, `application-test.yml` và `application-prod.yml`. Profile mặc định là `dev`; production bắt buộc cung cấp `DB_URL`, `DB_USERNAME` và `DB_PASSWORD` qua biến môi trường.

## Frontend

Yêu cầu: Node.js 20.19+ và npm 10+.

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

Ứng dụng chạy tại `http://localhost:5173`. Biến `VITE_API_BASE_URL` trong `.env` xác định địa chỉ Backend API.

Các lệnh kiểm tra:

```powershell
npm run typecheck
npm run lint
npm test
npm run build
```

## Continuous Integration

Pull requests targeting `develop` or `main` automatically run the Backend and
Frontend CI checks. See `docs/CI.md` for the checks, test reports, and required
branch-protection settings.

## UI/UX wireframes

The main storefront and administration flows are documented in
`docs/wireframes/README.md`, including full-resolution SVG wireframes and the
end-to-end purchase navigation flow.
