# Hướng dẫn Setup Database

## Bước 1: Tạo Database và Tables
Chạy file `init_database.sql` trong MySQL để tạo database và các bảng:
```sql
source sql/init_database.sql;
```
hoặc copy và paste nội dung vào MySQL Workbench/Command Line.

## Bước 2: Thêm dữ liệu test (Tùy chọn)
Chạy file `insert_test_data.sql` để thêm các user test:
```sql
source sql/insert_test_data.sql;
```

## Tài khoản test
Sau khi chạy `insert_test_data.sql`, bạn có thể đăng nhập với:

| Username | Password | Email |
|----------|----------|-------|
| admin | admin123 | admin@kahoot.com |
| testuser | test123 | test@kahoot.com |
| host1 | host123 | host1@kahoot.com |
| player1 | player123 | player1@kahoot.com |

**Lưu ý:** Mật khẩu đang được lưu dạng plain text (không an toàn). Trong production cần hash mật khẩu bằng BCrypt hoặc thuật toán tương tự.






