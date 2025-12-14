# Hướng dẫn chạy Kahoot Desktop

## Yêu cầu hệ thống

1. **Java**: JDK 17 trở lên (hoặc JDK 25 như trong project)
2. **Maven**: Đã cài đặt và cấu hình
3. **MySQL**: Đã cài đặt và đang chạy
4. **Database**: Đã tạo database `kahoot` và chạy script SQL

## Bước 1: Setup Database

1. Mở MySQL (Command Line hoặc MySQL Workbench)

2. Chạy script tạo database:
   ```sql
   source sql/init_database.sql;
   ```
   Hoặc copy nội dung file và paste vào MySQL

3. (Tùy chọn) Chạy script thêm dữ liệu test:
   ```sql
   source sql/insert_test_data.sql;
   ```

## Bước 2: Chạy Server

### Cách 1: Chạy Server với giao diện (Khuyến nghị)

Mở terminal/PowerShell thứ nhất và chạy:

```bash
cd d:\workspace\kahoot-desktop
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

Hoặc nếu đã compile:
```bash
mvn javafx:run -Djavafx.main.class=com.example.kahoot.server.ServerMain
```

**Giao diện Server sẽ hiển thị:**
- Nhập port (mặc định: 8888)
- Nhấn nút "Khởi động Server"
- Xem log và thống kê real-time

### Cách 2: Chạy Server không giao diện (Console)

```bash
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.KahootServer"
```

## Bước 3: Chạy Client

Sau khi server đã khởi động, mở terminal/PowerShell thứ hai và chạy:

```bash
cd d:\workspace\kahoot-desktop
mvn javafx:run
```

Hoặc:
```bash
mvn exec:java -Dexec.mainClass="com.example.kahoot.Main"
```

**Giao diện Client sẽ hiển thị:**
- Màn hình đăng nhập
- Có thể đăng nhập hoặc đăng ký tài khoản mới

## Bước 4: Sử dụng ứng dụng

### Với tài khoản test (nếu đã chạy insert_test_data.sql):
- Username: `admin`, Password: `admin123`
- Username: `testuser`, Password: `test123`

### Quy trình sử dụng:

1. **Đăng nhập** vào hệ thống
2. **Tạo Quiz mới** từ Host Dashboard
3. **Quản lý Quiz**: Thêm câu hỏi và các lựa chọn
4. **Bắt đầu Game**: Tạo game session và lấy PIN code
5. (Sắp tới) **Player tham gia** bằng PIN code

## Kiểm tra kết nối

### Kiểm tra Server đang chạy:
- Xem trong giao diện Server Dashboard
- Hoặc kiểm tra log: "Kahoot Server đã khởi động trên port 8888"

### Kiểm tra Client kết nối được Server:
- Xem log trong Server Dashboard khi client kết nối
- Hoặc thử đăng nhập từ client

## Troubleshooting

### Lỗi: Port đã được sử dụng
- Đổi port trong Server Dashboard (ví dụ: 8889)
- Hoặc đóng ứng dụng đang dùng port 8888

### Lỗi: Không kết nối được database
- Kiểm tra MySQL đang chạy
- Kiểm tra thông tin trong `DBConnection.java`
- Xem file `TROUBLESHOOTING.md`

### Lỗi: Không tìm thấy FXML file
- Đảm bảo đã compile: `mvn clean compile`
- Kiểm tra file FXML có trong `src/main/resources/views/`

### Server không khởi động được
- Kiểm tra port có bị chiếm dụng không
- Kiểm tra log trong console để xem lỗi cụ thể

## Cấu trúc chạy

```
Terminal 1: Server
└── mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
    └── Server Dashboard hiển thị
        └── Nhấn "Khởi động Server"

Terminal 2: Client  
└── mvn javafx:run
    └── Client UI hiển thị
        └── Đăng nhập và sử dụng
```

## Lưu ý

- **Luôn chạy Server trước** khi chạy Client
- Server và Client có thể chạy trên cùng máy (localhost) hoặc khác máy
- Nếu chạy khác máy, cần cập nhật `SERVER_HOST` trong `SocketClient.java`

## Tắt ứng dụng

1. Đóng Client trước
2. Nhấn "Dừng Server" trong Server Dashboard
3. Hoặc đóng cửa sổ Server Dashboard






