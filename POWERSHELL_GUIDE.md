# Hướng dẫn chạy bằng PowerShell

## ⚠️ Lưu ý quan trọng

PowerShell xử lý tham số khác với Command Prompt. Cần dùng dấu ngoặc kép đúng cách!

## 🚀 Cách chạy nhanh nhất

### Option 1: Dùng script PowerShell (Khuyến nghị)

```powershell
.\run.ps1
```

Tự động chạy cả Server và Client!

### Option 2: Chạy riêng

```powershell
# Terminal 1 - Server
.\run-server.ps1

# Terminal 2 - Client
.\run-client.ps1
```

## 📝 Lệnh thủ công

### Chạy Server

```powershell
mvn exec:java "-Dexec.mainClass=com.example.kahoot.server.ServerMain"
```

**Lưu ý:** Phải dùng dấu ngoặc kép **ngoài** tham số `-Dexec.mainClass`

### Chạy Client

```powershell
mvn javafx:run
```

## 🔧 Nếu gặp lỗi "Execution Policy"

PowerShell có thể chặn script. Chạy lệnh này một lần:

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

Hoặc chạy trực tiếp lệnh Maven thay vì script.

## ✅ So sánh Command Prompt vs PowerShell

| Command Prompt | PowerShell |
|----------------|------------|
| `mvn exec:java -Dexec.mainClass="..."` | `mvn exec:java "-Dexec.mainClass=..."` |
| Dấu ngoặc kép bên trong | Dấu ngoặc kép bên ngoài |

## 💡 Tips

### Kiểm tra PowerShell version
```powershell
$PSVersionTable.PSVersion
```

### Chạy với log chi tiết
```powershell
mvn exec:java "-Dexec.mainClass=com.example.kahoot.server.ServerMain" -X
```

### Compile trước khi chạy
```powershell
mvn clean compile
mvn exec:java "-Dexec.mainClass=com.example.kahoot.server.ServerMain"
```






