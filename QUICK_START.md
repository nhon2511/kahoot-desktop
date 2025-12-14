# 🚀 Quick Start Guide

## Cách chạy nhanh nhất

### Option 1: Dùng Script Batch (Windows) ⚡

**Chạy cả Server và Client:**
```bash
quick-start.bat
```
Chọn option 3 để chạy cả hai

**Hoặc chạy riêng:**
- `start-server.bat` - Chỉ chạy Server
- `start-client.bat` - Chỉ chạy Client  
- `start-all.bat` - Chạy cả hai (tự động mở 2 cửa sổ)

### Option 2: Dùng Maven Commands

**Chạy Server:**
```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

**Chạy Client:**
```bash
mvn clean compile javafx:run
```

### Option 3: Maven Profile (Sau khi compile)

**Chạy Server:**
```bash
mvn compile exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

**Chạy Client:**
```bash
mvn compile javafx:run
```

## Lưu ý

1. **Luôn chạy Server trước** Client
2. **Database phải đã setup** (chạy `sql/init_database.sql`)
3. **MySQL phải đang chạy**

## Tài khoản test

- Username: `admin`
- Password: `admin123`

## Troubleshooting

Nếu gặp lỗi, xem file `HUONG_DAN_CHAY.md` để biết chi tiết.






