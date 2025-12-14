# Hướng dẫn chạy Kahoot Server

## Kiến trúc Client-Server

Ứng dụng Kahoot Desktop sử dụng mô hình **Client-Server** với giao thức **TCP**:

- **Server**: Xử lý tất cả logic nghiệp vụ, kết nối database, quản lý game sessions
- **Client**: Giao diện người dùng (JavaFX), kết nối đến server qua TCP socket

## Cách chạy

### 1. Chạy Server

Server cần được chạy trước khi client có thể kết nối.

**Cách 1: Chạy ServerMain riêng biệt**
```bash
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

**Cách 2: Tạo JAR và chạy**
```bash
mvn clean package
java -cp target/kahoot-desktop-1.0-SNAPSHOT.jar com.example.kahoot.server.ServerMain
```

Server sẽ chạy trên port **8888** (mặc định).

### 2. Chạy Client

Sau khi server đã chạy, khởi động client:

```bash
mvn javafx:run
```

Hoặc:
```bash
mvn exec:java -Dexec.mainClass="com.example.kahoot.Main"
```

## Protocol giao tiếp

Client và Server giao tiếp qua TCP socket với format message:

```
COMMAND|param1|param2|...
```

### Các lệnh hỗ trợ:

#### Authentication
- `LOGIN|username|password` - Đăng nhập
- `REGISTER|username|password|email` - Đăng ký

#### Quiz Management
- `CREATE_QUIZ|hostId|title|accessCode` - Tạo quiz mới
- `GET_QUIZZES|hostId` - Lấy danh sách quiz của host

#### Game Session
- `START_GAME|pinCode` - Bắt đầu game session
- `JOIN_GAME|pinCode|playerName` - Player tham gia game
- `END_GAME|pinCode` - Kết thúc game

#### Game Play (TODO)
- `GET_QUESTION|sessionId|questionOrder` - Lấy câu hỏi
- `SUBMIT_ANSWER|sessionId|questionId|optionId` - Gửi câu trả lời

### Response Format:

- `SUCCESS|data1|data2|...` - Thành công
- `ERROR|error message` - Lỗi
- `COMMAND_RESULT|data` - Kết quả của lệnh cụ thể

## Cấu trúc Server

```
src/main/java/com/example/kahoot/server/
├── KahootServer.java          # Server chính, lắng nghe kết nối
├── ClientHandler.java         # Xử lý từng client connection
├── GameSessionHandler.java    # Quản lý một game session
├── AuthService.java           # Xác thực user
└── ServerMain.java            # Main class để chạy server
```

## Cấu trúc Client Socket

```
src/main/java/com/example/kahoot/util/
└── SocketClient.java          # Client socket để kết nối server
```

## Lưu ý

1. **Server phải chạy trước**: Client không thể kết nối nếu server chưa khởi động
2. **Port mặc định**: 8888 (có thể thay đổi trong `KahootServer.java`)
3. **Database**: Server cần kết nối được đến MySQL database
4. **Multi-threading**: Server sử dụng thread pool để xử lý nhiều client đồng thời

## Troubleshooting

### Server không khởi động được
- Kiểm tra port 8888 có bị chiếm dụng không
- Kiểm tra database connection trong `DBConnection.java`

### Client không kết nối được server
- Đảm bảo server đã chạy
- Kiểm tra `SERVER_HOST` và `SERVER_PORT` trong `SocketClient.java`
- Kiểm tra firewall có chặn kết nối không

### Lỗi khi gửi/nhận message
- Kiểm tra format message đúng protocol
- Xem log trong console để debug






