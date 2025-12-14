# Hướng dẫn chạy bằng Terminal/Command Line

## 📋 Yêu cầu trước khi chạy

1. **MySQL đang chạy**
2. **Database đã setup** (chạy `sql/init_database.sql`)
3. **Maven đã cài đặt** và có trong PATH

## 🚀 Cách chạy

### Bước 1: Mở Terminal

**Windows:**
- Nhấn `Win + R`, gõ `cmd` hoặc `powershell`
- Hoặc tìm "Command Prompt" hoặc "PowerShell" trong Start Menu

**Hoặc trong VS Code/IDE:**
- Nhấn `` Ctrl + ` `` để mở terminal tích hợp

### Bước 2: Di chuyển đến thư mục project

```bash
cd d:\workspace\kahoot-desktop
```

### Bước 3: Chạy Server (Terminal 1)

**Nếu dùng Command Prompt (cmd):**
```bash
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

**Nếu dùng PowerShell:**
```powershell
mvn exec:java "-Dexec.mainClass=com.example.kahoot.server.ServerMain"
```

**Hoặc dùng script PowerShell:**
```powershell
.\run-server.ps1
```

**Kết quả:**
- Server Dashboard sẽ hiển thị
- Nhấn nút **"Khởi động Server"** trong giao diện

### Bước 4: Chạy Client (Terminal 2)

Mở **terminal thứ hai** (giữ terminal 1 đang chạy) và chạy:

```bash
mvn javafx:run
```

**Hoặc:**
```bash
mvn exec:java -Dexec.mainClass="com.example.kahoot.Main"
```

**Kết quả:**
- Client UI sẽ hiển thị
- Có thể đăng nhập ngay

## 📝 Các lệnh hữu ích

### Compile project
```bash
mvn clean compile
```

### Chạy Server (sau khi compile)

**Command Prompt:**
```bash
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

**PowerShell:**
```powershell
mvn exec:java "-Dexec.mainClass=com.example.kahoot.server.ServerMain"
```

### Chạy Client (sau khi compile)
```bash
mvn javafx:run
```

### Chạy cả hai cùng lúc (Windows PowerShell)
```powershell
# Cách 1: Dùng script
.\run.ps1

# Cách 2: Chạy thủ công
# Terminal 1
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd d:\workspace\kahoot-desktop; mvn exec:java '-Dexec.mainClass=com.example.kahoot.server.ServerMain'"

# Đợi 4 giây
Start-Sleep -Seconds 4

# Terminal 2
mvn javafx:run
```

### Xem log chi tiết
```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain" -X
```

## 🔧 Troubleshooting

### Lỗi: "mvn: command not found"
**Giải pháp:** Maven chưa được cài đặt hoặc chưa có trong PATH
```bash
# Kiểm tra Maven
mvn -version

# Nếu không có, cài đặt Maven hoặc thêm vào PATH
```

### Lỗi: "Port already in use"
**Giải pháp:** Port 8888 đang được sử dụng
```bash
# Windows: Tìm process đang dùng port
netstat -ano | findstr :8888

# Kill process (thay PID bằng số từ lệnh trên)
taskkill /PID <PID> /F
```

### Lỗi: "Cannot connect to database"
**Giải pháp:** Kiểm tra MySQL đang chạy
```bash
# Windows: Kiểm tra MySQL service
sc query MySQL80

# Hoặc kiểm tra trong Services (services.msc)
```

### Lỗi: "ClassNotFoundException"
**Giải pháp:** Cần compile lại
```bash
mvn clean compile
```

## 💡 Tips

### Chạy nhanh hơn (bỏ qua test)

**Command Prompt:**
```bash
mvn clean compile -DskipTests exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

**PowerShell:**
```powershell
mvn clean compile -DskipTests exec:java "-Dexec.mainClass=com.example.kahoot.server.ServerMain"
```

### Chạy với log đầy đủ
```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain" -e
```

### Xem dependency tree
```bash
mvn dependency:tree
```

## 📋 Tóm tắt nhanh

**Command Prompt (cmd):**
```bash
# Terminal 1 - Server
cd d:\workspace\kahoot-desktop
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"

# Terminal 2 - Client
cd d:\workspace\kahoot-desktop
mvn javafx:run
```

**PowerShell:**
```powershell
# Terminal 1 - Server
cd d:\workspace\kahoot-desktop
mvn exec:java "-Dexec.mainClass=com.example.kahoot.server.ServerMain"

# Terminal 2 - Client
cd d:\workspace\kahoot-desktop
mvn javafx:run
```

**Hoặc dùng script PowerShell (dễ nhất):**
```powershell
.\run-server.ps1    # Terminal 1
.\run-client.ps1    # Terminal 2
# Hoặc
.\run.ps1           # Chạy cả hai tự động
```

## 🎯 Workflow đề xuất

1. **Lần đầu tiên:**
   ```bash
   mvn clean compile
   ```

2. **Mỗi lần chạy:**
   - Terminal 1: `mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"`
   - Terminal 2: `mvn javafx:run`

3. **Khi có thay đổi code:**
   ```bash
   mvn clean compile
   ```
   Rồi chạy lại như bước 2

