package com.example.kahoot.server;

import com.example.kahoot.dao.QuestionDAO;
import com.example.kahoot.dao.OptionDAO;
import com.example.kahoot.model.Question;
import com.example.kahoot.model.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý một game session cụ thể.
 * Quản lý các player tham gia và luồng game.
 */
public class GameSessionHandler {
    private com.example.kahoot.model.GameSession session;
    private KahootServer server;
    private List<ClientHandler> players;
    private ClientHandler host; // Lưu host connection để gửi message
    private boolean isActive;
    
    private List<Question> questions;
    private int currentQuestionIndex = -1;
    private Question currentQuestion;
    private Map<ClientHandler, Integer> playerAnswers; // Player -> OptionId
    private Map<ClientHandler, Integer> playerScores; // Player -> Score
    private Map<ClientHandler, Long> answerTimes; // Player -> Thời gian trả lời (ms)
    private Map<ClientHandler, String> playerNames; // Player -> Tên player
    
    private GameState gameState;
    private long questionStartTime; // Thời gian bắt đầu câu hỏi
    private Timer questionTimer; // Timer để tự động hiển thị kết quả khi hết thời gian
    private ExecutorService executorService; // Thread pool để xử lý các task không blocking

    public GameSessionHandler(com.example.kahoot.model.GameSession session, KahootServer server) {
        try {
            this.session = session;
            this.server = server;
            this.players = new ArrayList<>();
            this.isActive = true;
            this.playerAnswers = new HashMap<>();
            this.playerScores = new HashMap<>();
            this.answerTimes = new HashMap<>();
            this.playerNames = new HashMap<>();
            this.gameState = GameState.WAITING;
            this.executorService = Executors.newFixedThreadPool(2); // Thread pool nhỏ để xử lý async tasks
            
            System.out.println("✓ Đang khởi tạo GameSessionHandler...");
            System.out.println("  Session ID: " + (session != null ? session.getSessionId() : "NULL"));
            System.out.println("  Quiz ID: " + (session != null ? session.getQuizId() : "NULL"));
            System.out.println("  Game State: " + gameState);
            
            // Load questions từ database
            loadQuestions();
            
            if (questions == null || questions.isEmpty()) {
                System.err.println("⚠ Cảnh báo: Quiz không có câu hỏi nào!");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi khởi tạo GameSessionHandler: " + e.getMessage());
            e.printStackTrace();
            questions = new ArrayList<>();
        }
    }
    
    /**
     * Load tất cả câu hỏi của quiz.
     */
    private void loadQuestions() {
        try {
            if (session == null) {
                System.err.println("✗ Session is null!");
                questions = new ArrayList<>();
                return;
            }
            
            QuestionDAO questionDAO = new QuestionDAO();
            questions = questionDAO.findByQuizId(session.getQuizId());
            System.out.println("✓ Đã load " + (questions != null ? questions.size() : 0) + " câu hỏi cho game session");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi load questions: " + e.getMessage());
            e.printStackTrace();
            questions = new ArrayList<>();
        }
    }

    /**
     * Thêm player vào game.
     */
    public synchronized void addPlayer(ClientHandler player, String playerName) {
        players.add(player);
        playerScores.put(player, 0);
        playerNames.put(player, playerName);
        int playerCount = players.size();
        System.out.println("✓ Player '" + playerName + "' đã tham gia game. Tổng số player: " + playerCount);
        
        // Gửi PLAYER_JOINED đến tất cả players
        broadcastToAll("PLAYER_JOINED|" + playerCount);
        
        // Gửi PLAYER_JOINED đến host (nếu có)
        if (host != null) {
            try {
                host.sendResponse("PLAYER_JOINED|" + playerCount);
                System.out.println("✓ Đã gửi PLAYER_JOINED đến host: " + playerCount);
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi gửi PLAYER_JOINED đến host: " + e.getMessage());
            }
        }

        // Gửi player list tới host để host có thể hiển thị danh sách người chơi
        if (host != null) {
            try {
                // Build encoded list of player names separated by semicolon
                StringBuilder sb = new StringBuilder();
                for (String name : playerNames.values()) {
                    sb.append(name.replace(";", "\uFF1B")).append(";");
                }
                String encodedList = java.net.URLEncoder.encode(sb.toString(), java.nio.charset.StandardCharsets.UTF_8.name());
                host.sendResponse("PLAYER_LIST|" + encodedList);
                System.out.println("✓ Đã gửi PLAYER_LIST đến host: " + sb.toString());
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi gửi PLAYER_LIST đến host: " + e.getMessage());
            }
        }

        // Gửi thông báo tới host
        if (host != null) {
            try {
                host.sendResponse("NOTIFICATION|Player '" + playerName + "' joined the game");
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi gửi NOTIFICATION đến host: " + e.getMessage());
            }
        }

        // Gửi thông báo (notification) tới tất cả players để họ thấy ai vừa vào
        try {
            broadcastToAll("NOTIFICATION|Player '" + playerName + "' joined the game");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi broadcast NOTIFICATION join: " + e.getMessage());
        }
        
        System.out.println("  Game PIN: " + session.getPinCode());
        System.out.println("  Số lượng player hiện tại: " + playerCount);
        System.out.println("  Game State: " + gameState);
    }
    
    /**
     * Thiết lập host cho game session này.
     */
    public void setHost(ClientHandler host) {
        this.host = host;
        System.out.println("✓ Đã thiết lập host cho game session: " + session.getPinCode());
        // Khi host được set, gửi cho host trạng thái hiện tại (số lượng player hiện tại)
        try {
            if (host != null) {
                int playerCount = players.size();
                host.sendResponse("PLAYER_JOINED|" + playerCount);
                System.out.println("✓ Đã gửi initial PLAYER_JOINED đến host: " + playerCount);
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi gửi initial PLAYER_JOINED đến host: " + e.getMessage());
        }
    }
    
    /**
     * Gửi câu hỏi tiếp theo đến player.
     */
    public void sendNextQuestion(ClientHandler player) {
        if (currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) {
            player.sendResponse("ERROR|No more questions");
            return;
        }
        
        currentQuestion = questions.get(currentQuestionIndex);
        
        try {
            OptionDAO optionDAO = new OptionDAO();
            List<Option> options = optionDAO.findByQuestionId(currentQuestion.getQuestionId());
            
            // Tạo response với format: QUESTION|questionId|questionText|timeLimit|pointValue|option1Id|option1Text|option2Id|option2Text|...
            String qText = currentQuestion.getQuestionText();
            try {
                qText = URLEncoder.encode(qText == null ? "" : qText, StandardCharsets.UTF_8.name());
            } catch (Exception ex) {
                System.err.println("⚠ Không thể encode question text: " + ex.getMessage());
            }

            StringBuilder response = new StringBuilder("QUESTION|");
            response.append(currentQuestion.getQuestionId()).append("|");
            response.append(qText).append("|");
            response.append(currentQuestion.getTimeLimit()).append("|");
            response.append(currentQuestion.getPointValue()).append("|");
            response.append(currentQuestion.getQuestionOrder()).append("|");
            response.append(questions.size()).append("|");
            
            for (Option option : options) {
                String optText = option.getOptionText();
                try {
                    optText = URLEncoder.encode(optText == null ? "" : optText, StandardCharsets.UTF_8.name());
                } catch (Exception ex) {
                    System.err.println("⚠ Không thể encode option text: " + ex.getMessage());
                }

                response.append(option.getOptionId()).append("|");
                response.append(optText).append("|");
            }
            
            player.sendResponse(response.toString());
            System.out.println("✓ Đã gửi câu hỏi " + (currentQuestionIndex + 1) + " đến player");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi gửi câu hỏi: " + e.getMessage());
            player.sendResponse("ERROR|" + e.getMessage());
        }
    }
    
    /**
     * Gửi câu hỏi hiện tại đến tất cả players.
     */
    public void broadcastQuestion() {
        if (questions == null || questions.isEmpty()) {
            System.err.println("✗ Không có câu hỏi để gửi!");
            broadcastToAll("ERROR|Không có câu hỏi");
            return;
        }
        
        if (currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) {
            System.err.println("✗ Index câu hỏi không hợp lệ: " + currentQuestionIndex + " / " + questions.size());
            broadcastToAll("ERROR|No more questions");
            return;
        }
        
        currentQuestion = questions.get(currentQuestionIndex);
        if (currentQuestion == null) {
            System.err.println("✗ Câu hỏi tại index " + currentQuestionIndex + " là null!");
            broadcastToAll("ERROR|Question is null");
            return;
        }
        
        // Reset cho câu hỏi mới
        playerAnswers.clear();
        answerTimes.clear();
        gameState = GameState.QUESTION;
        questionStartTime = System.currentTimeMillis();
        
        // Hủy timer cũ nếu có
        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
        }
        
        // Tạo timer mới để tự động hiển thị kết quả khi hết thời gian
        // Sử dụng daemon thread để tránh block application
        int timeLimitSeconds = currentQuestion.getTimeLimit();
        questionTimer = new Timer(true); // Daemon thread
        questionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Chạy showResults() trên thread pool để không block timer thread
                executorService.execute(() -> {
                    try {
                        // Kiểm tra gameState trước khi hiển thị kết quả
                        if (gameState == GameState.QUESTION) {
                            System.out.println("⏰ Hết thời gian cho câu hỏi " + (currentQuestionIndex + 1) + " - Tự động hiển thị kết quả");
                            showResults();
                        }
                    } catch (Exception e) {
                        System.err.println("✗ Lỗi trong timer task: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }, timeLimitSeconds * 1000L); // Chuyển sang milliseconds
        
        try {
            OptionDAO optionDAO = new OptionDAO();
            List<Option> options = optionDAO.findByQuestionId(currentQuestion.getQuestionId());
            
            if (options == null || options.isEmpty()) {
                System.err.println("✗ Câu hỏi không có đáp án!");
                broadcastToAll("ERROR|Question has no options");
                return;
            }
            
            // Tạo response (encode text fields để tránh xung đột với ký tự phân tách '|')
            String qText = currentQuestion.getQuestionText();
            try {
                qText = URLEncoder.encode(qText == null ? "" : qText, StandardCharsets.UTF_8.name());
            } catch (Exception ex) {
                System.err.println("⚠ Không thể encode question text: " + ex.getMessage());
            }

            StringBuilder response = new StringBuilder("QUESTION|");
            response.append(currentQuestion.getQuestionId()).append("|");
            response.append(qText).append("|");
            response.append(currentQuestion.getTimeLimit()).append("|");
            response.append(currentQuestion.getPointValue()).append("|");
            response.append(currentQuestion.getQuestionOrder()).append("|");
            response.append(questions.size()).append("|");

            for (Option option : options) {
                String optText = option.getOptionText();
                try {
                    optText = URLEncoder.encode(optText == null ? "" : optText, StandardCharsets.UTF_8.name());
                } catch (Exception ex) {
                    System.err.println("⚠ Không thể encode option text: " + ex.getMessage());
                }

                response.append(option.getOptionId()).append("|");
                response.append(optText).append("|");
            }
            
            String fullMsg = response.toString();
            System.out.println("📤 Broadcast QUESTION message: " + fullMsg);
            broadcastToAll(fullMsg);
            System.out.println("✓ Đã gửi câu hỏi " + (currentQuestionIndex + 1) + "/" + questions.size() + " đến tất cả players");
            System.out.println("  Game State: " + gameState);
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi broadcast câu hỏi: " + e.getMessage());
            e.printStackTrace();
            broadcastToAll("ERROR|" + e.getMessage());
        }
    }
    
    /**
     * Xử lý đáp án từ player.
     */
    public void submitAnswer(ClientHandler player, int optionId) {
        if (currentQuestion == null) {
            System.err.println("✗ Không có câu hỏi hiện tại!");
            player.sendResponse("ERROR|No current question");
            return;
        }
        
        if (gameState != GameState.QUESTION) {
            System.err.println("✗ Không thể gửi đáp án: Game state = " + gameState);
            player.sendResponse("ERROR|Cannot submit answer in current state");
            return;
        }
        
        // Lưu đáp án và thời gian trả lời
        long answerTime = System.currentTimeMillis() - questionStartTime;
        playerAnswers.put(player, optionId);
        answerTimes.put(player, answerTime);
        
        // Kiểm tra đáp án đúng
        try {
            OptionDAO optionDAO = new OptionDAO();
            Option selectedOption = null;
            List<Option> options = optionDAO.findByQuestionId(currentQuestion.getQuestionId());
            
            if (options == null || options.isEmpty()) {
                System.err.println("✗ Câu hỏi không có đáp án!");
                player.sendResponse("ERROR|Question has no options");
                return;
            }
            
            for (Option opt : options) {
                if (opt.getOptionId() == optionId) {
                    selectedOption = opt;
                    break;
                }
            }
            
            if (selectedOption != null && selectedOption.isCorrect()) {
                // Đáp án đúng - tính điểm theo thời gian
                int basePoints = currentQuestion.getPointValue();
                int timeLimit = currentQuestion.getTimeLimit() * 1000; // Chuyển sang milliseconds
                int pointsEarned = calculateTimeBasedScore(basePoints, answerTime, timeLimit);
                
                int currentScore = playerScores.getOrDefault(player, 0);
                int newScore = currentScore + pointsEarned;
                playerScores.put(player, newScore);
                
                String playerName = playerNames.getOrDefault(player, "Unknown");
                System.out.println("✓ Player '" + playerName + "' trả lời đúng! Thời gian: " + (answerTime/1000.0) + "s, Điểm: " + pointsEarned + "/" + basePoints + ", Tổng: " + newScore);
                
                player.sendResponse("ANSWER_RESULT|true|" + pointsEarned + "|" + newScore + "|" + (answerTime/1000.0));
            } else {
                // Đáp án sai
                player.sendResponse("ANSWER_RESULT|false|0|" + playerScores.getOrDefault(player, 0) + "|0");
                String playerName = playerNames.getOrDefault(player, "Unknown");
                System.out.println("✗ Player '" + playerName + "' trả lời sai");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi xử lý đáp án: " + e.getMessage());
            e.printStackTrace();
            player.sendResponse("ERROR|" + e.getMessage());
        }
    }
    
    /**
     * Tính điểm dựa trên thời gian trả lời.
     * Càng nhanh càng nhiều điểm.
     */
    private int calculateTimeBasedScore(int basePoints, long answerTimeMs, int timeLimitMs) {
        if (answerTimeMs <= 0) {
            return basePoints;
        }
        
        // Tính phần trăm thời gian còn lại
        double timeRatio = Math.max(0, 1.0 - (answerTimeMs / (double) timeLimitMs));
        
        // Điểm tối đa khi trả lời ngay (100%), tối thiểu 50% khi hết thời gian
        double scoreRatio = 0.5 + (timeRatio * 0.5);
        
        return (int) Math.round(basePoints * scoreRatio);
    }
    
    /**
     * Chuyển sang câu hỏi tiếp theo.
     */
    public void nextQuestion() {
        // Kiểm tra state một cách thread-safe
        synchronized (this) {
            System.out.println("🔁 nextQuestion() called. currentQuestionIndex=" + currentQuestionIndex + ", total=" + (questions != null ? questions.size() : 0) + ", gameState=" + gameState);
            if (gameState == GameState.QUESTION) {
                // Host pressed next while question still open - force show results first
                System.out.println("ℹ Host yêu cầu chuyển câu hỏi trong khi question còn đang mở - gọi showResults() trước");
                try {
                    showResults();
                } catch (Exception e) {
                    System.err.println("⚠ Lỗi khi gọi showResults trước khi nextQuestion: " + e.getMessage());
                }
            } else if (gameState != GameState.RESULT && gameState != GameState.WAITING) {
                System.err.println("✗ Không thể chuyển câu hỏi: Game state = " + gameState + " - yêu cầu state RESULT hoặc WAITING");
                return;
            }

            currentQuestionIndex++;
            System.out.println("🔁 nextQuestion() -> new currentQuestionIndex=" + currentQuestionIndex);
        }
        
        if (currentQuestionIndex < questions.size()) {
            // Chạy broadcastQuestion trên thread pool để không block
            executorService.execute(() -> {
                try {
                    broadcastQuestion();
                } catch (Exception e) {
                    System.err.println("✗ Lỗi khi broadcast câu hỏi: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            // Game kết thúc
            endGame();
        }
    }
    
    /**
     * Hiển thị kết quả và leaderboard sau mỗi câu hỏi.
     * Sử dụng synchronized để tránh race condition khi timer và host cùng gọi.
     */
    public void showResults() {
        // Kiểm tra và thay đổi state một cách thread-safe
        synchronized (this) {
            if (gameState != GameState.QUESTION) {
                System.err.println("✗ Không thể hiển thị kết quả: Game state = " + gameState);
                return;
            }
            
            // Hủy timer nếu đang chạy (trường hợp host nhấn nút trước khi hết thời gian)
            if (questionTimer != null) {
                try {
                    questionTimer.cancel();
                } catch (Exception e) {
                    System.err.println("⚠ Lỗi khi hủy timer: " + e.getMessage());
                }
                questionTimer = null;
            }
            
            // Thay đổi state trước khi làm các thao tác khác
            gameState = GameState.RESULT;
        }
        
        System.out.println("📊 Hiển thị kết quả câu hỏi " + (currentQuestionIndex + 1));
        
        // Tìm đáp án đúng và tạo leaderboard (nhanh, không block)
        int correctOptionId = -1;
        try {
            OptionDAO optionDAO = new OptionDAO();
            List<Option> options = optionDAO.findByQuestionId(currentQuestion.getQuestionId());
            if (options != null) {
                for (Option opt : options) {
                    if (opt.isCorrect()) {
                        correctOptionId = opt.getOptionId();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi tìm đáp án đúng: " + e.getMessage());
        }
        
        // Tạo leaderboard
        String leaderboard = buildLeaderboard();
        
        // Lưu vào final variables để dùng trong lambda
        final int finalCorrectOptionId = correctOptionId;
        final String finalLeaderboard = leaderboard;
        
        // Gửi kết quả đến tất cả players trên thread pool (không block)
        executorService.execute(() -> {
            try {
                // Encode leaderboard to avoid conflicts with outer '|' separator
                String encodedLeaderboard;
                try {
                    encodedLeaderboard = URLEncoder.encode(finalLeaderboard, StandardCharsets.UTF_8.name());
                } catch (Exception ex) {
                    System.err.println("⚠ Không thể encode leaderboard: " + ex.getMessage());
                    encodedLeaderboard = finalLeaderboard;
                }

                String message = "SHOW_RESULTS|" + finalCorrectOptionId + "|" + encodedLeaderboard;
                System.out.println("📤 Server gửi SHOW_RESULTS: " + message);
                System.out.println("  Leaderboard length: " + finalLeaderboard.length());
                broadcastToAll(message);
                System.out.println("  Game State: " + gameState);
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi broadcast kết quả: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Xây dựng leaderboard string.
     */
    private String buildLeaderboard() {
        synchronized (this) {
            System.out.println("📊 Building leaderboard...");
            System.out.println("  Total players: " + players.size());
            System.out.println("  Player scores: " + playerScores.size());
            System.out.println("  Player names: " + playerNames.size());
            
            // Sắp xếp players theo điểm số (giảm dần)
            List<ClientHandler> sortedPlayers = players.stream()
                .sorted((p1, p2) -> {
                    int score1 = playerScores.getOrDefault(p1, 0);
                    int score2 = playerScores.getOrDefault(p2, 0);
                    return Integer.compare(score2, score1);
                })
                .collect(Collectors.toList());
            
            System.out.println("  Sorted players count: " + sortedPlayers.size());
            
            StringBuilder leaderboard = new StringBuilder();
            int rank = 1;
            for (ClientHandler player : sortedPlayers) {
                String name = playerNames.getOrDefault(player, "Unknown");
                int score = playerScores.getOrDefault(player, 0);
                String entry = rank + "|" + name + "|" + score + ";";
                leaderboard.append(entry);
                System.out.println("  Entry " + rank + ": " + name + " - " + score + " điểm");
                rank++;
            }
            
            String result = leaderboard.toString();
            System.out.println("📊 Leaderboard result: " + result);
            return result;
        }
    }
    
    /**
     * Bắt đầu game (gửi câu hỏi đầu tiên).
     */
    public void startGame() {
        if (questions == null || questions.isEmpty()) {
            System.err.println("✗ Không thể bắt đầu game: Không có câu hỏi!");
            broadcastToAll("ERROR|Không có câu hỏi trong quiz");
            return;
        }
        
        if (gameState != GameState.WAITING) {
            System.err.println("✗ Không thể bắt đầu game: Game state = " + gameState);
            return;
        }
        
        currentQuestionIndex = 0;
        System.out.println("🎮 Bắt đầu game với câu hỏi đầu tiên");
        broadcastQuestion();
    }

    /**
     * Gửi message đến tất cả players.
     * Sử dụng copy của list để tránh concurrent modification.
     */
    public void broadcastToAll(String message) {
        // Tạo copy của players list để tránh concurrent modification
        List<ClientHandler> playersCopy = new ArrayList<>(players);
        
        // Gửi message đến từng player, bỏ qua nếu có lỗi
        // Không block nếu một player bị lỗi
        for (ClientHandler player : playersCopy) {
            try {
                if (player != null && player.isConnected()) {
                    player.sendResponse(message);
                }
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi gửi message đến player: " + e.getMessage());
                // Không throw exception để không block các players khác
            }
        }
    }

    /**
     * Kết thúc game.
     */
    public void endGame() {
        // Hủy timer nếu đang chạy
        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
        }
        
        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdown();
        }
        
        isActive = false;
        gameState = GameState.FINISHED;
        
        // Tạo leaderboard cuối cùng
        String finalLeaderboard = buildLeaderboard();
        
        // Gửi kết quả cuối cùng đến từng player
        System.out.println("📤 Gửi GAME_ENDED đến " + players.size() + " players");
        for (ClientHandler player : players) {
            int finalScore = playerScores.getOrDefault(player, 0);
            int rank = calculateRank(player);
            String encodedLeaderboard;
            try {
                encodedLeaderboard = URLEncoder.encode(finalLeaderboard, StandardCharsets.UTF_8.name());
            } catch (Exception ex) {
                System.err.println("⚠ Không thể encode final leaderboard: " + ex.getMessage());
                encodedLeaderboard = finalLeaderboard;
            }

            String message = "GAME_ENDED|" + finalScore + "|" + rank + "|" + encodedLeaderboard;
            System.out.println("  → Gửi đến player: " + playerNames.getOrDefault(player, "Unknown") + " - " + message);
            player.sendResponse(message);
        }

        // Gửi GAME_ENDED đến host nếu host không nằm trong players
        if (host != null && !players.contains(host)) {
            try {
                System.out.println("  → Gửi GAME_ENDED đến host: " + host);
                // For host, finalScore/rank are not applicable (use totals from leaderboard)
                String encodedLeaderboard;
                try {
                    encodedLeaderboard = URLEncoder.encode(finalLeaderboard, StandardCharsets.UTF_8.name());
                } catch (Exception ex) {
                    System.err.println("⚠ Không thể encode final leaderboard for host: " + ex.getMessage());
                    encodedLeaderboard = finalLeaderboard;
                }
                host.sendResponse("GAME_ENDED|0|0|" + encodedLeaderboard);
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi gửi GAME_ENDED đến host: " + e.getMessage());
            }
        }
        
        System.out.println("Game session đã kết thúc: " + session.getPinCode());
        System.out.println("  Game State: " + gameState);
        // Đăng ký dọn dẹp session trên server để tránh session bị để lại
        try {
            server.unregisterGameSession(session.getPinCode());
            System.out.println("✓ Đã unregister session trên server: " + session.getPinCode());
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi unregister session từ endGame: " + e.getMessage());
        }

        // Thông báo tới tất cả (nếu vẫn có kết nối)
        try {
            broadcastToAll("NOTIFICATION|Game has ended. Thank you for playing!");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi broadcast game ended notification: " + e.getMessage());
        }
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Tính xếp hạng của player.
     */
    private int calculateRank(ClientHandler player) {
        int playerScore = playerScores.getOrDefault(player, 0);
        int rank = 1;
        
        for (ClientHandler p : players) {
            if (playerScores.getOrDefault(p, 0) > playerScore) {
                rank++;
            }
        }
        
        return rank;
    }
    
    public Question getCurrentQuestion() {
        return currentQuestion;
    }
    
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }
    
    public int getTotalQuestions() {
        return questions != null ? questions.size() : 0;
    }

    public com.example.kahoot.model.GameSession getSession() {
        return session;
    }

    public List<ClientHandler> getPlayers() {
        return players;
    }

    /**
     * Loại bỏ player khỏi session (khi player rời phòng hoặc mất kết nối).
     */
    public synchronized void removePlayer(ClientHandler player) {
        if (player == null) return;

        String name = playerNames.getOrDefault(player, "Unknown");
        boolean removed = players.remove(player);
        playerScores.remove(player);
        playerNames.remove(player);
        playerAnswers.remove(player);
        answerTimes.remove(player);

        if (removed) {
            int count = players.size();
            System.out.println("✓ Player '" + name + "' đã rời phòng. Số người còn lại: " + count);

            // Thông báo tới tất cả players
            broadcastToAll("PLAYER_LEFT|" + count);
            broadcastToAll("NOTIFICATION|Player '" + name + "' left the game");

            // Cập nhật player list cho host nếu có
            if (host != null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (String n : playerNames.values()) {
                        sb.append(n.replace(";", "")).append(";");
                    }
                    String encoded = java.net.URLEncoder.encode(sb.toString(), java.nio.charset.StandardCharsets.UTF_8.name());
                    host.sendResponse("PLAYER_LIST|" + encoded);
                } catch (Exception e) {
                    System.err.println("✗ Lỗi khi gửi PLAYER_LIST đến host sau khi rời phòng: " + e.getMessage());
                }
                try {
                    host.sendResponse("NOTIFICATION|Player '" + name + "' left the game");
                } catch (Exception e) {
                    System.err.println("✗ Lỗi khi gửi NOTIFICATION rời phòng tới host: " + e.getMessage());
                }
            }

            // Nếu không còn players và game đã kết thúc, có thể dọn dẹp
            if (players.isEmpty() && gameState == GameState.FINISHED) {
                System.out.println("ℹ Không còn người chơi và game đã kết thúc, dọn dẹp session: " + session.getPinCode());
                try {
                    server.unregisterGameSession(session.getPinCode());
                } catch (Exception e) {
                    System.err.println("✗ Lỗi khi unregister session: " + e.getMessage());
                }
            }
        }
    }

    public boolean isActive() {
        return isActive;
    }
}




