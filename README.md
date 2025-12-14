# Kahoot Desktop

This repository contains a Java/JavaFX desktop client and server for a Kahoot-like quiz application.

Quick start
1. Build and run tests:
```powershell
mvn test
```

2. Run the server (example):
```powershell
# from repository root
mvn -Pserver exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

3. Run the client (JavaFX):
```powershell
mvn javafx:run -Dmain.class=com.example.kahoot.Main
```

Database configuration
- The application reads DB configuration from environment variables:
  - `DB_URL` (e.g., `jdbc:mysql://localhost:3306/kahoot`)
  - `DB_USER`
  - `DB_PASSWORD`

If not set, it falls back to defaults in the code (useful for local dev).

Security
- Do NOT commit secrets. Use environment variables for DB credentials and other secrets.

Contribution
- Open a PR, let CI run tests, and request a review before merging into `main`.
# Kahoot Desktop Application

Ứng dụng Kahoot Desktop sử dụng mô hình Client-Server với giao thức TCP.

## 🚀 Quick Start - CỰC NHANH!

### ⚡ Cách nhanh nhất (1 click):

**Double-click `run.bat`** → Tự động chạy cả Server và Client!

### Hoặc chạy riêng:

- `run-server.bat` - Chỉ Server (1 click)
- `run-client.bat` - Chỉ Client (1 click)

### Các script khác:

- `quick-start.bat` - Menu chọn
- `start-all.bat` - Chạy cả hai (cửa sổ riêng)

## 📋 Yêu cầu

- Java JDK 17+ (hoặc JDK 25)
- Maven
- MySQL đã cài đặt và đang chạy

## 🗄️ Setup Database

```sql
source sql/init_database.sql;
source sql/insert_test_data.sql;  -- Tùy chọn
```

## 💻 Chạy bằng Terminal

### Terminal 1 - Server:
```bash
cd d:\workspace\kahoot-desktop
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

### Terminal 2 - Client:
```bash
cd d:\workspace\kahoot-desktop
mvn javafx:run
```

**Lưu ý:** 
- Sau khi chạy Client, bạn có thể:
  - **Đăng nhập** để làm Host (tạo quiz, quản lý game)
  - **Nhấn "Tham gia Game"** để tham gia game bằng mã PIN (không cần đăng nhập)

**Xem chi tiết:** `HUONG_DAN_TERMINAL.md` hoặc `TERMINAL_QUICK.txt`

## 📖 Tài liệu

- `TERMINAL_QUICK.txt` - Hướng dẫn terminal ngắn gọn
- `HUONG_DAN_TERMINAL.md` - Hướng dẫn terminal chi tiết
- `QUICK_START.md` - Hướng dẫn nhanh
- `HUONG_DAN_CHAY.md` - Hướng dẫn chi tiết
- `SERVER_README.md` - Tài liệu về Server
- `TROUBLESHOOTING.md` - Xử lý lỗi

## 🏗️ Cấu trúc Project

```
kahoot-desktop/
├── src/main/java/com/example/kahoot/
│   ├── client/          # Client controllers
│   ├── server/          # Server code
│   ├── dao/             # Data Access Objects
│   ├── model/           # Data models
│   └── util/            # Utilities
├── src/main/resources/
│   └── views/           # FXML files
├── sql/                 # Database scripts
└── *.bat                # Launch scripts
```

## 🎮 Tính năng

- ✅ Đăng nhập/Đăng ký
- ✅ Tạo và quản lý Quiz
- ✅ Thêm/Sửa/Xóa Questions và Options
- ✅ Bắt đầu Game Session với PIN code
- ✅ Player tham gia game bằng mã PIN (không cần đăng nhập)
- ✅ TCP Server với giao diện
- ✅ Game play với timer và tính điểm
- ✅ Hiển thị kết quả và leaderboard

## 📝 License

Educational project

# kahoot-desktop
