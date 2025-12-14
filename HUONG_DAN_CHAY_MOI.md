# 🚀 Hướng dẫn chạy ứng dụng Kahoot Desktop

## ⚡ Cách nhanh nhất

### Bước 1: Chạy Server (Terminal 1)
Mở PowerShell hoặc Terminal và chạy:
```powershell
cd d:\workspace\kahoot-desktop
.\run-server.ps1
```

Hoặc dùng Maven trực tiếp:
```powershell
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"
```

Bạn sẽ thấy:
```
Kahoot Server đã khởi động trên port 8888
Đang chờ kết nối từ client...
```

### Bước 2: Chạy Client - Host (Terminal 2)
Mở PowerShell hoặc Terminal mới và chạy:
```powershell
cd d:\workspace\kahoot-desktop
.\run-client.ps1
```

Hoặc:
```powershell
mvn javafx:run
```

### Bước 3: Đăng nhập và bắt đầu game
1. Đăng nhập với tài khoản host (ví dụ: `admin` / `admin123`)
2. Chọn một quiz và nhấn **"Bắt đầu Game"**
3. Màn hình sẽ hiển thị **PIN code** để player tham gia
4. Server sẽ log: `🚀 Host muốn bắt đầu game với PIN: [PIN_CODE]`

### Bước 4: Chạy Client - Player (Terminal 3)
Để test player tham gia game, bạn cần mở thêm một cửa sổ client:

**Cách 1: Dùng code để mở màn hình player**
- Thêm nút trong HostDashboard để mở player.fxml

**Cách 2: Chạy client thứ 2 và tự mở màn hình player**
```powershell
# Chạy client thứ 2
mvn javafx:run
```

Sau đó trong code, bạn có thể thêm nút để mở màn hình player.

## 📋 Luồng hoạt động

### Host (Người tổ chức):
1. ✅ Đăng nhập → Server nhận `LOGIN` command
2. ✅ Chọn quiz và bắt đầu game → Server nhận `START_GAME|PIN_CODE`
3. ✅ Server đăng ký game session và log thông tin
4. ✅ Màn hình hiển thị PIN code và số lượng player

### Player (Người chơi):
1. ✅ Mở màn hình player (player.fxml)
2. ✅ Nhập PIN code và tên
3. ✅ Nhấn "Tham gia Game" → Server nhận `JOIN_GAME|PIN_CODE|PLAYER_NAME`
4. ✅ Server kiểm tra PIN và thêm player vào game session
5. ✅ Server log: `✓ Player '[Tên]' đã tham gia game với PIN: [PIN]`
6. ✅ Player nhận `JOIN_SUCCESS` response

## 🔍 Kiểm tra Server Logs

Khi chạy đúng, bạn sẽ thấy trên server console:

```
✓ Client đã kết nối từ: /127.0.0.1:xxxxx
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📨 Nhận được message từ Client /127.0.0.1:xxxxx:
   START_GAME|123456
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚀 Host muốn bắt đầu game với PIN: 123456
✓ Game session đã được khởi động với PIN: 123456
📤 Gửi response đến client: GAME_STARTED|123456

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📨 Nhận được message từ Client /127.0.0.1:yyyyy:
   JOIN_GAME|123456|Player1
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎮 Player muốn tham gia game:
   PIN Code: 123456
   Player Name: Player1
✓ Player 'Player1' đã tham gia game với PIN: 123456
✓ Player đã tham gia game. Tổng số player: 1
```

## 🛠️ Troubleshooting

### Server không nhận được message từ client?
- ✅ Kiểm tra server đã chạy chưa (port 8888)
- ✅ Kiểm tra client đã kết nối thành công chưa
- ✅ Xem console của client có log "Đã kết nối đến server" không

### Player không thể tham gia?
- ✅ Kiểm tra host đã bắt đầu game chưa (phải có `START_GAME` trước)
- ✅ Kiểm tra PIN code đúng chưa
- ✅ Xem server log để biết lỗi cụ thể

### Lỗi kết nối?
- ✅ Đảm bảo MySQL đang chạy
- ✅ Database đã được khởi tạo (chạy `sql/init_database.sql`)
- ✅ Port 8888 không bị chiếm bởi ứng dụng khác

## 📝 Lưu ý

1. **Luôn chạy Server trước** khi chạy Client
2. **Database phải đã setup** trước khi chạy
3. Để test player, bạn có thể:
   - Chạy nhiều instance của client (mở nhiều cửa sổ)
   - Hoặc thêm nút trong HostDashboard để mở player.fxml

## 🎯 Test nhanh

1. Chạy server: `.\run-server.ps1`
2. Chạy client 1 (host): `.\run-client.ps1` → Đăng nhập → Bắt đầu game
3. Chạy client 2 (player): `.\run-client.ps1` → Mở màn hình player → Nhập PIN và tên
4. Xem server log để thấy player đã tham gia!



