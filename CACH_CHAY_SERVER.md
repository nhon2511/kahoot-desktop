# 🔧 Cách chạy Server - Sửa lỗi Maven

## ⚠️ Lỗi thường gặp

Nếu bạn gặp lỗi:
```
[ERROR] Unknown lifecycle phase ".mainClass=com.example.kahoot.server.ServerMain"
```

## ✅ Giải pháp

### Cách 1: Dùng script đã sửa (Khuyến nghị)
```powershell
.\run-server.ps1
```

Script này đã được sửa để dùng cấu hình mặc định trong `pom.xml`.

### Cách 2: Chạy trực tiếp với Maven
```powershell
mvn exec:java
```

Vì `pom.xml` đã có cấu hình `mainClass` mặc định, không cần tham số `-Dexec.mainClass`.

### Cách 3: Nếu muốn chỉ định rõ ràng
```powershell
mvn exec:java -Dexec.mainClass=com.example.kahoot.server.ServerMain
```

**Lưu ý:** Trong PowerShell, không dùng dấu ngoặc kép quanh `-Dexec.mainClass=...`

### Cách 4: Dùng profile server
```powershell
mvn clean compile
mvn exec:java -Pserver
```

## 🎯 Test nhanh

1. Mở PowerShell
2. Chạy: `cd d:\workspace\kahoot-desktop`
3. Chạy: `.\run-server.ps1`
4. Nếu vẫn lỗi, thử: `mvn exec:java`

## 📝 Lưu ý

- Script `run-server.ps1` đã được cập nhật
- Nếu vẫn gặp lỗi, hãy chạy `mvn clean compile` trước
- Đảm bảo Maven đã được cài đặt đúng



