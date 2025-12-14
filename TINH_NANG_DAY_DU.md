# 🎮 Ứng dụng Kahoot Desktop - Tính năng đầy đủ

## ✅ Các tính năng đã hoàn thành

### 🖥️ Server

#### 1. Tạo phòng chơi có mã PIN
- ✅ Server tạo game session với PIN code duy nhất
- ✅ PIN code được lưu vào database
- ✅ Server quản lý nhiều game session đồng thời

#### 2. Quản lý nhiều client kết nối (Multithreading)
- ✅ Server sử dụng `ExecutorService` để xử lý nhiều client
- ✅ Mỗi client có `ClientHandler` riêng trong thread riêng
- ✅ Server log chi tiết khi client kết nối/ngắt kết nối

#### 3. Gửi câu hỏi cho tất cả client
- ✅ Server broadcast câu hỏi đến tất cả players trong game session
- ✅ Format: `QUESTION|questionId|questionText|timeLimit|pointValue|questionNumber|totalQuestions|option1Id|option1Text|...`

#### 4. Nhận đáp án, tính điểm theo thời gian
- ✅ Server nhận đáp án từ player với `SUBMIT_ANSWER|optionId`
- ✅ Tính điểm dựa trên thời gian trả lời:
  - Trả lời ngay: 100% điểm
  - Trả lời giữa chừng: 50-100% điểm (tỷ lệ với thời gian còn lại)
  - Trả lời cuối: 50% điểm
- ✅ Lưu thời gian trả lời của mỗi player

#### 5. Gửi bảng xếp hạng sau mỗi câu
- ✅ Server tạo leaderboard sau mỗi câu hỏi
- ✅ Sắp xếp players theo điểm số (giảm dần)
- ✅ Format: `SHOW_RESULTS|correctOptionId|rank1|name1|score1;rank2|name2|score2;...`

#### 6. Quản lý trạng thái phòng
- ✅ **WAITING**: Đang chờ players tham gia
- ✅ **QUESTION**: Đang hiển thị câu hỏi, players đang trả lời
- ✅ **RESULT**: Đang hiển thị kết quả và leaderboard
- ✅ **FINISHED**: Game đã kết thúc

### 💻 Client

#### 1. Nhập IP, PORT, tên, mã PIN để vào phòng
- ✅ Giao diện player có các field:
  - IP Server (mặc định: localhost)
  - Port Server (mặc định: 8888)
  - Mã PIN
  - Tên player
- ✅ Client kết nối đến server với IP và PORT tùy chỉnh

#### 2. Nhận câu hỏi từ server, hiển thị GUI, có timer
- ✅ Client nhận message `QUESTION` từ server
- ✅ Hiển thị câu hỏi và các đáp án dạng button
- ✅ Timer đếm ngược theo `timeLimit` của câu hỏi
- ✅ Timer đổi màu khi sắp hết thời gian (≤10s)

#### 3. Gửi 1 đáp án/câu
- ✅ Player chỉ có thể chọn 1 đáp án
- ✅ Sau khi chọn, tất cả buttons bị disable
- ✅ Gửi `SUBMIT_ANSWER|optionId` đến server

#### 4. Nhận kết quả + leaderboard
- ✅ Nhận `ANSWER_RESULT|isCorrect|pointsEarned|totalScore|answerTime`
- ✅ Hiển thị kết quả (đúng/sai) và điểm nhận được
- ✅ Nhận `SHOW_RESULTS|correctOptionId|leaderboard`
- ✅ Highlight đáp án đúng
- ✅ Hiển thị leaderboard trong ListView

#### 5. Host điều khiển game flow
- ✅ Host có các nút:
  - **"Bắt đầu câu hỏi"**: Gửi câu hỏi đầu tiên hoặc câu tiếp theo
  - **"Hiển thị kết quả"**: Hiển thị kết quả và leaderboard
  - **"Kết thúc Game"**: Kết thúc game session

## 📋 Protocol Messages

### Client → Server

#### Authentication
- `LOGIN|username|password`
- `REGISTER|username|password|email`

#### Game Session
- `START_GAME|pinCode` - Tạo game session
- `JOIN_GAME|pinCode|playerName` - Player tham gia
- `START_QUESTION|pinCode` - Host bắt đầu câu hỏi đầu tiên
- `NEXT_QUESTION|pinCode` - Host chuyển sang câu hỏi tiếp theo
- `SHOW_RESULTS|pinCode` - Host hiển thị kết quả
- `SUBMIT_ANSWER|optionId` - Player gửi đáp án
- `END_GAME|pinCode` - Host kết thúc game

### Server → Client

#### Game Flow
- `QUESTION|questionId|questionText|timeLimit|pointValue|questionNumber|totalQuestions|option1Id|option1Text|option2Id|option2Text|...`
- `ANSWER_RESULT|isCorrect|pointsEarned|totalScore|answerTime`
- `SHOW_RESULTS|correctOptionId|rank1|name1|score1;rank2|name2|score2;...`
- `GAME_ENDED|finalScore|rank|finalLeaderboard`
- `PLAYER_JOINED|playerCount`
- `JOIN_SUCCESS|quizId|playerName`
- `JOIN_FAILED|errorMessage`

## 🎯 Luồng hoạt động

### 1. Host tạo game
```
Host: Chọn quiz → Bắt đầu Game
Server: Tạo GameSession → Trả về PIN code
State: WAITING
```

### 2. Players tham gia
```
Player: Nhập IP, PORT, PIN, Tên → Tham gia Game
Server: Kiểm tra PIN → Thêm player vào GameSession
State: WAITING (vẫn chờ)
```

### 3. Host bắt đầu câu hỏi
```
Host: Nhấn "Bắt đầu câu hỏi"
Server: Gửi QUESTION đến tất cả players
State: WAITING → QUESTION
```

### 4. Players trả lời
```
Player: Chọn đáp án → Gửi SUBMIT_ANSWER
Server: Tính điểm theo thời gian → Gửi ANSWER_RESULT
State: QUESTION (vẫn đang trả lời)
```

### 5. Host hiển thị kết quả
```
Host: Nhấn "Hiển thị kết quả"
Server: Tạo leaderboard → Gửi SHOW_RESULTS
State: QUESTION → RESULT
```

### 6. Host chuyển câu hỏi tiếp theo
```
Host: Nhấn "Câu hỏi tiếp theo"
Server: Gửi QUESTION mới
State: RESULT → QUESTION
```

### 7. Game kết thúc
```
Host: Nhấn "Kết thúc Game" hoặc hết câu hỏi
Server: Gửi GAME_ENDED với leaderboard cuối cùng
State: RESULT → FINISHED
```

## 🔧 Cấu trúc Code

### Server
```
src/main/java/com/example/kahoot/server/
├── KahootServer.java          # Server chính, multithreading
├── ClientHandler.java         # Xử lý từng client
├── GameSessionHandler.java    # Quản lý game session, trạng thái
├── GameState.java             # Enum: WAITING, QUESTION, RESULT, FINISHED
└── ServerMain.java            # Main class
```

### Client
```
src/main/java/com/example/kahoot/client/
├── PlayerController.java      # Màn hình tham gia (IP, PORT, PIN, Tên)
├── PlayerGameController.java  # Màn hình chơi game (câu hỏi, timer, leaderboard)
└── GameSessionController.java # Màn hình host (điều khiển game flow)
```

### Utilities
```
src/main/java/com/example/kahoot/util/
└── SocketClient.java          # Client socket với IP/PORT tùy chỉnh
```

## 🚀 Cách sử dụng

### 1. Chạy Server
```powershell
.\run-server.ps1
```

### 2. Chạy Client - Host
```powershell
.\run-client.ps1
```
- Đăng nhập
- Chọn quiz → Bắt đầu Game
- Nhấn "Bắt đầu câu hỏi" để bắt đầu

### 3. Chạy Client - Player
```powershell
.\run-client.ps1
```
- Mở màn hình player (có thể thêm nút trong UI)
- Nhập:
  - IP Server: `localhost` (hoặc IP của server)
  - Port: `8888`
  - Mã PIN: (từ host)
  - Tên: (tên của bạn)
- Nhấn "Tham gia Game"

## 📊 Tính điểm

Công thức tính điểm:
```
timeRatio = 1.0 - (answerTime / timeLimit)
scoreRatio = 0.5 + (timeRatio * 0.5)
pointsEarned = basePoints * scoreRatio
```

- Trả lời ngay (0s): 100% điểm
- Trả lời giữa chừng: 50-100% điểm
- Trả lời cuối (hết thời gian): 50% điểm

## 🎨 Giao diện

### Player Join Screen
- IP Server field
- Port Server field
- PIN Code field
- Player Name field
- Join Button

### Player Game Screen
- Header: Tên player, Điểm, Timer
- Câu hỏi và đáp án (4 buttons)
- Status message
- Leaderboard ListView

### Host Game Screen
- PIN Code hiển thị
- Số lượng players
- Nút "Bắt đầu câu hỏi"
- Nút "Hiển thị kết quả"
- Nút "Kết thúc Game"

## ✅ Checklist tính năng

- [x] Server tạo phòng với PIN
- [x] Multithreading cho nhiều client
- [x] Client nhập IP, PORT, tên, PIN
- [x] Nhận và hiển thị câu hỏi với timer
- [x] Gửi 1 đáp án/câu
- [x] Tính điểm theo thời gian
- [x] Gửi leaderboard sau mỗi câu
- [x] Quản lý trạng thái: WAITING → QUESTION → RESULT → FINISHED
- [x] Host điều khiển game flow



