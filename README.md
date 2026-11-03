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

REDIS_HOST=localhost
REDIS_PORT=6379
```

Lưu ý:
- Thay các giá trị mẫu bằng thông tin của môi trường cá nhân.
- `POSTGRES_ADMIN_PASSWORD` là mật khẩu tài khoản quản trị PostgreSQL.
- `DEV_DB_URL` dùng cho database phát triển; `TEST_DB_URL` dùng cho database kiểm thử.
- Tên biến phải khớp với cấu hình trong `application.properties`, `application-dev.properties` và `application-test.properties`.

## 5. Khởi động PostgreSQL và Redis bằng Docker

Từ thư mục gốc dự án, chạy:

```bash
docker compose up -d db redis
```

Kiểm tra container:

```bash
docker compose ps
```

Xem log PostgreSQL:

```bash
docker compose logs -f db redis
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

### Nếu chỉ cần chạy ứng dụng:
### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Nếu cần chạy cả seeder:
### Windows

```powershell
 .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev --spring.main.web-application-type=none --app.seed.enabled=true --app.seed.allow-destructive=true"
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

Dừng container PostgreSQL và Redis:

```bash
docker compose stop db redis
```

Hoặc dừng các dịch vụ trong Compose:

```bash
docker compose down
```

Lệnh `docker compose down` thông thường không xóa named volumes. Không sử dụng `docker compose down -v` nếu chưa xác nhận rằng dữ liệu trong volume có thể bị xóa.

## 10. Quy trình khởi động nhanh

Sau khi đã cấu hình `.env` và chuẩn bị database:

```bash
docker compose up -d db redis
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

## 11. Performance Test bằng k6
Performance test sử dụng PostgreSQL, Redis riêng cho môi trường PERF, Spring Boot profile perf và k6 chạy bằng Docker. Application chạy trực tiếp trên máy local; các service phụ trợ và k6 chạy trong Docker.

### 11.1. Chuẩn bị database PERF

Tạo database bằng DBeaver:

1. Tạo kết nối PostgreSQL bằng tài khoản postgres, sử dụng Host `127.0.0.1`, Port `5432` và mật khẩu `POSTGRES_ADMIN_PASSWORD` trong file `.env`.

2. Kết nối đến database postgres, mở SQL Editor và chạy:

    ```sql
    CREATE DATABASE vouchersystem_perf OWNER voucher_app;
    ```
3. Tạo một kết nối DBeaver mới đến database vouchersystem_perf, sử dụng tài khoản voucher_app và mật khẩu tương ứng với `DB_PASSWORD` trong .env.

Cập nhật các biến môi trường trong file .env ở thư mục gốc:


```dotenv
PERF_DB_URL=jdbc:postgresql://localhost:5432/vouchersystem_perf
PERF_REDIS_HOST=localhost
PERF_REDIS_PORT=6380
```

### 11.2. Khởi động các Docker services

Tại thư mục gốc project, chạy một lệnh:

```powershell
docker compose up -d db redis-perf
```

Lệnh này khởi động PostgreSQL và Redis dành riêng cho performance test. 
Không cần khởi động Redis thông thường hoặc các service monitoring.

### 11.3. Chạy application với profile PERF

Tại thư mục gốc project, chạy:

```powershell
$env:SPRING_PROFILES_ACTIVE = "perf"
.\mvnw.cmd spring-boot:run
```

Application sẽ sử dụng application-perf.properties và tự động chạy Flyway migrations khi khởi động.

Sau khi application khởi động thành công:

Mở DBeaver và kết nối đến database vouchersystem_perf.

Mở file `performance/seed-perf.sql`.

Thực thi toàn bộ script trên database PERF để chuẩn bị dữ liệu cho các bài test.

Lưu ý: seed-perf.sql có thao tác TRUNCATE và reset identity sequences. Chỉ chạy script trên database vouchersystem_perf. Nên chạy lại seed trước mỗi lần benchmark để có dữ liệu ban đầu nhất quán.

### 11.4. Chạy k6 performance tests

Chạy các lệnh bên dưới tại thư mục gốc project trên PowerShell.

Nếu chưa có file `performance/.env`, tạo từ file mẫu trong `performance/.env_example`

1. Smoke test – 5 requests/s trong 20 giây

```powershell
docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm -e MODE=constant -e TARGET_RATE=5 -e DURATION=20s -e CUSTOMER_PREFIX=PERF-SMOKE k6 run /scripts/redemption.js
```

2. Generic redemption – Constant 500 requests/s trong 5 phút

```powershell
docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm -e MODE=constant k6 run /scripts/redemption.js
```

3. Generic redemption – Ramping load

```powershell
docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm -e MODE=ramp k6 run /scripts/redemption.js
```

4. Bulk redemption – 500 requests/s trong 5 phút

```powershell
docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm k6 run /scripts/bulk-redemption.js
```

5. Contention test – Nhiều khách hàng redeem cùng một batch

```powershell
docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm -e TARGET_RATE=100 -e DURATION=1m -e MODE=same_batch k6 run /scripts/contention.js
```

6. Contention test – một khách hàng redeem cùng voucher

```powershell
docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm -e TARGET_RATE=100 -e DURATION=1m -e MODE=same_customer k6 run /scripts/contention.js
```

7. Failure test – Sử dụng voucher không hợp lệ

```powershell
docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm -e MODE=invalid_voucher -e TARGET_RATE=100 -e DURATION=2m k6 run /scripts/failure.js
```

8. Failure test – Không thỏa mãn điều kiện voucher

```powershell
docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm -e MODE=invalid_condition -e TARGET_RATE=100 -e DURATION=2m k6 run /scripts/failure.js
```

11.5. Kết quả và lưu ý

- Kết quả tổng hợp của `bulk-redemption.js` và `redemption.js` được lưu tại `performance/results/`.

- Kết quả chi tiết của các lần chạy cũng được hiển thị trực tiếp trong terminal.

- cần seed lại dữ liệu trước khi chạy lại một bài test vì mỗi voucher chỉ được sử dụng một lần.

### 11.6. Dừng môi trường PERF

Sau khi hoàn tất performance test:

```powershell
docker compose stop db redis-perf
```