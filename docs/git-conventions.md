# Git workflow and conventions

**Story:** `US-00.1`
**Tasks:** `T-00.1.1`, `T-00.1.3`

Tài liệu này quy định cách làm việc với Git cho dự án TechStore. Mục tiêu là
không push trực tiếp vào các nhánh dùng chung, giữ lịch sử dễ đọc và bảo đảm mọi
thay đổi đều được CI kiểm tra trước khi merge.

## 1. Vai trò của các nhánh

- `main`: phiên bản ổn định có thể phát hành. Chỉ nhận thay đổi qua Pull Request.
- `develop`: nhánh tích hợp cho quá trình phát triển. Feature thông thường mở
  Pull Request vào nhánh này.
- Nhánh công việc phải có phạm vi nhỏ, gắn với một ticket hoặc một mục tiêu sửa
  lỗi rõ ràng.

Không commit hoặc push trực tiếp vào `main` và `develop`.

## 2. Quy ước đặt tên branch

Mẫu chuẩn:

```text
feature/TSM-<ticket>/<mo-ta-ngan>
fix/TSM-<ticket>/<mo-ta-loi>
hotfix/TSM-<ticket>/<mo-ta-loi-khan-cap>
chore/TSM-<ticket>/<mo-ta-cong-viec>
docs/TSM-<ticket>/<mo-ta-tai-lieu>
```

Ý nghĩa:

- `feature/`: tính năng mới, tạo từ `develop`, merge về `develop`.
- `fix/`: sửa lỗi chưa phát hành, tạo từ `develop`, merge về `develop`.
- `hotfix/`: sửa lỗi khẩn cấp trên production, tạo từ `main`, sau đó merge về
  cả `main` và `develop`.
- `chore/`: cấu hình, dependency, CI hoặc bảo trì không làm thay đổi nghiệp vụ.
- `docs/`: chỉ thay đổi tài liệu.

Tên nhánh dùng chữ thường cho phần mô tả, ngăn cách bằng dấu gạch ngang. Ví dụ:

```text
feature/TSM-24/register-account
fix/TSM-31/default-address-validation
hotfix/TSM-80/checkout-total
```

Các nhánh `TSM-<ticket>/<mo-ta>` đã tạo trong EPIC-00 được giữ nguyên để không
làm mất liên kết Pull Request. Từ story tiếp theo sử dụng mẫu chuẩn ở trên.

## 3. Quy trình làm việc

### Feature hoặc fix thông thường

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/TSM-24/register-account
```

Sau khi hoàn thành:

1. Chạy test, lint và build phù hợp với phần thay đổi.
2. Commit theo Conventional Commits.
3. Push nhánh công việc lên `origin`.
4. Tạo Pull Request vào `develop`.
5. Chỉ merge khi các required checks đã thành công và các trao đổi đã được xử lý.

Khi chuẩn bị phát hành, tạo Pull Request từ `develop` vào `main`.

### Hotfix

Hotfix được tạo từ `main`, mở Pull Request vào `main`, sau đó đưa cùng thay đổi
trở lại `develop` để tránh tái xuất hiện lỗi ở bản phát hành sau.

## 4. Conventional Commits

Định dạng:

```text
<type>(<scope>): <mô tả ngắn>
```

Các `type` sử dụng trong dự án:

- `feat`: thêm chức năng.
- `fix`: sửa lỗi.
- `docs`: thay đổi tài liệu.
- `test`: thêm hoặc sửa kiểm thử.
- `refactor`: thay đổi cấu trúc nhưng không đổi hành vi.
- `perf`: cải thiện hiệu năng.
- `build`: thay đổi build hoặc dependency.
- `ci`: thay đổi pipeline.
- `chore`: công việc bảo trì khác.

`scope` nên là module như `auth`, `catalog`, `order`, `backend`, `frontend`,
`database` hoặc `ci`. Mô tả dùng câu mệnh lệnh, ngắn gọn, không thêm commit hash
vào đầu message và không kết thúc bằng dấu chấm.

Ví dụ:

```text
feat(auth): add customer registration endpoint
fix(database): enforce one default address per user
ci(frontend): run lint and tests for pull requests
docs(architecture): record technology decisions
```

Thay đổi không tương thích phải thêm `!` hoặc footer `BREAKING CHANGE:`.

## 5. Quy tắc Pull Request

- Tiêu đề PR dùng định dạng Conventional Commits và chứa ticket khi cần.
- Mô tả phải nêu mục tiêu, thay đổi chính, cách kiểm tra và ảnh hưởng database/API.
- Không đưa secret, file `.env`, artifact build hoặc dependency đã cài vào commit.
- Không merge khi Backend CI hoặc Frontend CI bắt buộc đang fail hoặc chưa chạy.
- Ưu tiên PR nhỏ, chỉ giải quyết một user story hoặc một nhóm task liên quan.

## 6. Bảo vệ nhánh

Repository phải áp dụng cho `main`:

- Require a pull request before merging.
- Require status checks to pass before merging.
- Require branches to be up to date before merging.
- Không cho force-push hoặc xóa nhánh.
- Bắt buộc các check `Backend CI` và `Frontend CI`.

Vì branch protection là cấu hình trên GitHub, nội dung trong repository không thể
tự thay thế quy tắc này. Cách kiểm tra: mở **Settings → Rules → Rulesets** hoặc
**Settings → Branches** và xác nhận quy tắc đang ở trạng thái Active.
