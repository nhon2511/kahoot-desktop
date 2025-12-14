# Hướng dẫn xử lý lỗi đăng nhập

## Các bước kiểm tra

### 1. Kiểm tra Database đã được tạo chưa
Chạy trong MySQL:
```sql
SHOW DATABASES;
```
Phải thấy database `kahoot` trong danh sách.

### 2. Kiểm tra bảng Users đã tồn tại chưa
```sql
USE kahoot;
SHOW TABLES;
```
Phải thấy bảng `Users`.

### 3. Kiểm tra dữ liệu user đã được thêm chưa
```sql
USE kahoot;
SELECT * FROM Users;
```
Phải thấy ít nhất 1 user trong bảng.

### 4. Kiểm tra kết nối database
Trong file `DBConnection.java`, đảm bảo:
- URL: `jdbc:mysql://localhost:3306/kahoot`
- USER: `root` (hoặc user MySQL của bạn)
- PASSWORD: `""` (mật khẩu MySQL của bạn, có thể là rỗng)

### 5. Kiểm tra MySQL đang chạy
- Windows: Kiểm tra trong Services hoặc Task Manager
- Hoặc chạy: `mysql -u root -p`

### 6. Xem log khi chạy ứng dụng
Khi chạy ứng dụng, xem console output để thấy:
- `DBConnection: Đang kết nối tới...`
- `DBConnection: Kết nối thành công!` hoặc lỗi cụ thể
- `UserDAO: Tìm thấy user...` hoặc `Không tìm thấy user...`
- `Password nhập vào: [...]` và `Password trong DB: [...]`

## Các lỗi thường gặp

### Lỗi: "No suitable driver found"
- **Nguyên nhân**: MySQL Connector chưa được load
- **Giải pháp**: Đảm bảo dependency trong `pom.xml` đúng và đã chạy `mvn clean install`

### Lỗi: "Access denied for user"
- **Nguyên nhân**: Username/password MySQL sai
- **Giải pháp**: Kiểm tra và cập nhật trong `DBConnection.java`

### Lỗi: "Unknown database 'kahoot'"
- **Nguyên nhân**: Database chưa được tạo
- **Giải pháp**: Chạy script `sql/init_database.sql`

### Lỗi: "Table 'kahoot.Users' doesn't exist"
- **Nguyên nhân**: Bảng Users chưa được tạo
- **Giải pháp**: Chạy script `sql/init_database.sql`

### Đăng nhập thất bại nhưng không có lỗi
- **Nguyên nhân**: User chưa được thêm vào database hoặc password không khớp
- **Giải pháp**: 
  1. Chạy script `sql/insert_test_data.sql`
  2. Kiểm tra log để xem password có khớp không
  3. Đảm bảo password trong DB là plain text (ví dụ: "admin123")

## Tài khoản test mặc định
Sau khi chạy `insert_test_data.sql`:
- Username: `admin`, Password: `admin123`
- Username: `testuser`, Password: `test123`






