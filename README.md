# VoucherSystemBackend

Backend quản lý voucher và cấu hình Rule Engine, được xây dựng bằng Spring Boot, PostgreSQL, Flyway và Redis.

## 1. Công nghệ sử dụng

- Java và Spring Boot
- Maven
- PostgreSQL
- Docker Compose
- Flyway – quản lý database migrations
- Hibernate/JPA – ORM và kiểm tra schema
- Redis – hỗ trợ các chức năng liên quan đến rate limiting, distribution lock và caching
- JUnit và Spring Boot Test

## 2. Yêu cầu môi trường

Trước khi chạy dự án, hãy cài đặt:

- JDK tương thích với phiên bản Java được khai báo trong `pom.xml`
- Docker Desktop
- Git
- IntelliJ IDEA
- DBeaver hoặc công cụ quản lý PostgreSQL khác (tùy chọn)

Kiểm tra môi trường:

```bash
java -version
docker --version
docker compose version
```

## 3. Clone dự án

```bash
git clone https://github.com/JustAnother-12/VoucherSystemBackend.git
```

## 4. Cấu hình biến môi trường

Tạo file `.env` tại thư mục gốc, cùng cấp với `pom.xml` và `compose.yaml`.

Hãy dùng `.env_example` làm mẫu và điền các giá trị phù hợp.

Ví dụ các biến môi trường:

```dotenv
JWT_SECRET=<run openssl rand -base64 64 to get secrets>
JWT_ACCESS_EXPIRATION_HOURS=2
JWT_REFRESH_EXPIRATION_DAYS=7
POSTGRES_ADMIN_PASSWORD=<YOUR_ADMIN_PASSWORD>
POSTGRES_PORT=5432

DEV_DB_URL=jdbc:postgresql://localhost:5432/vouchersystem_dev
TEST_DB_URL=jdbc:postgresql://localhost:5432/vouchersystem_test
DB_USERNAME=voucher_app
DB_PASSWORD=<YOUR_APP_PASSWORD>
```

Lưu ý:
- Thay các giá trị mẫu bằng thông tin của môi trường cá nhân.
- `POSTGRES_ADMIN_PASSWORD` là mật khẩu tài khoản quản trị PostgreSQL.
- `DEV_DB_URL` dùng cho database phát triển; `TEST_DB_URL` dùng cho database kiểm thử.
- Tên biến phải khớp với cấu hình trong `application.properties`, `application-dev.properties` và `application-test.properties`.

## 5. Khởi động PostgreSQL bằng Docker

Từ thư mục gốc dự án, chạy:

```bash
docker compose up -d db
```

Kiểm tra container:

```bash
docker compose ps
```

Xem log PostgreSQL:

```bash
docker compose logs -f db
```

Kiểm tra database đã được tạo:

```bash
docker compose exec db psql -U postgres -d postgres -c "\l"
```

Khi PostgreSQL đã sẵn sàng, ứng dụng có thể kết nối đến database qua port được publish trên máy host.

### Kiểm tra database

Có thể kết nối bằng DBeaver hoặc một PostgreSQL client khác với thông tin tương ứng trong `.env`.

Thông tin mặc định dự kiến:

| Thuộc tính | Giá trị              |
|---|----------------------|
| Host | `127.0.0.1`          |
| Port | `5432`               |
| Database phát triển | `vouchersystem_dev`  |
| Database kiểm thử | `vouchersystem_test` |
| Username ứng dụng | `voucher_app`        |
| Password | đang mặc định là `123` |

Nếu `POSTGRES_PORT` được đổi, hãy sử dụng port thực tế đã publish trong Docker Compose.

## 6. Chạy ứng dụng Spring Boot

### Windows

Khởi chạy với profile phát triển (mặc định trong `application.properties`):

```powershell
.\mvnw.cmd spring-boot:run
```

## 7. Chạy kiểm thử

Đảm bảo PostgreSQL đang chạy và database kiểm thử đã được tạo, sau đó chạy:

### Windows

```powershell
.\mvnw.cmd test
```

Các test tích hợp cần sử dụng cấu hình test và database kiểm thử riêng, không dùng nhầm `vouchersystem_dev`.

## 8. Quản lý database migrations

Flyway quản lý lịch sử migration trong bảng `flyway_schema_history`.

Quy tắc đề xuất:

- Migration đã chạy thành công trên môi trường dùng chung không nên bị sửa nội dung.
- Tạo migration phiên bản mới khi cần thay đổi schema.
- Kiểm tra migration trên database phát triển và database kiểm thử.
- Không tắt schema validation chỉ để bỏ qua lỗi thiếu bảng hoặc thiếu cột.

Ví dụ cấu trúc:

```text
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_condition_type.sql
└── V3__update_reward_configuration.sql
```

Tên migration thực tế cần phản ánh thay đổi tương ứng trong dự án.

## 9. Dừng dịch vụ

Dừng container PostgreSQL:

```bash
docker compose stop db
```

Hoặc dừng các dịch vụ trong Compose:

```bash
docker compose down
```

Lệnh `docker compose down` thông thường không xóa named volumes. Không sử dụng `docker compose down -v` nếu chưa xác nhận rằng dữ liệu trong volume có thể bị xóa.

## 10. Quy trình khởi động nhanh

Sau khi đã cấu hình `.env` và chuẩn bị database:

```bash
docker compose up -d db
```

Chạy Spring Boot trên Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Chạy test:

```powershell
.\mvnw.cmd test
```

Dùng DBeaver hoặc công cụ tương đương để kiểm tra dữ liệu PostgreSQL.
