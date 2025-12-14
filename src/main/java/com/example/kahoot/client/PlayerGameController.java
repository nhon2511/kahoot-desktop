package com.example.kahoot.client;

import com.example.kahoot.model.Option;
import com.example.kahoot.model.Question;
import com.example.kahoot.util.SocketClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Controller cho màn hình chơi game của player.
 */
public class PlayerGameController implements Initializable {

    @FXML private Label playerNameLabel;
    @FXML private Label scoreLabel;
    @FXML private Label timerLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionTextLabel;
    @FXML private Label statusLabel;
    @FXML private GridPane optionsGrid;
    @FXML private VBox waitingBox;
    @FXML private javafx.scene.control.ListView<String> leaderboardListView;

    private SocketClient socketClient;
    private String playerName;
    private int currentScore = 0;
    private int currentQuestionNumber = 0;
    private int totalQuestions = 0;
    private Timer timer;
    private int timeRemaining = 0;
    private boolean hasAnswered = false;
    private int selectedOptionId = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // SocketClient sẽ được set từ PlayerController
        // Đảm bảo leaderboard được hiển thị
        Platform.runLater(() -> {
            if (leaderboardListView != null) {
                leaderboardListView.getItems().clear();
                leaderboardListView.getItems().add("Đang chờ dữ liệu xếp hạng...");
                leaderboardListView.setVisible(true);
                System.out.println("✓ Leaderboard ListView đã được khởi tạo");
            } else {
                System.err.println("✗ leaderboardListView là null trong initialize!");
            }
        });
    }

    /**
     * Thiết lập thông tin player và socket client.
     */
    public void setup(String playerName, SocketClient socketClient) {
        this.playerName = playerName;
        this.socketClient = socketClient;
        
        if (playerNameLabel != null) {
            playerNameLabel.setText("Player: " + playerName);
        }
        
        updateScore(0);
        
        // Đăng ký listener để nhận message từ server
        if (socketClient != null) {
            socketClient.setMessageListener(this::handleServerMessage);
        }
    }
    
    /**
     * Xử lý message từ server (được gọi từ SocketClient listener).
     */
    private void handleServerMessage(String message) {
        if (message == null) {
            System.err.println("⚠ Nhận được message null từ server");
            return;
        }
        
        System.out.println("📨 Player nhận message: " + message);
        
        String[] parts = message.split("\\|");
        if (parts.length == 0) {
            System.err.println("⚠ Message không có parts: " + message);
            return;
        }
        
        String command = parts[0];
        System.out.println("  Command: " + command + ", Parts count: " + parts.length);
        
        switch (command) {
            case "QUESTION":
                handleQuestionMessage(parts);
                break;
            case "ANSWER_RESULT":
                handleAnswerResult(parts);
                break;
            case "SHOW_RESULTS":
                System.out.println("  → Xử lý SHOW_RESULTS với " + parts.length + " parts");
                handleShowResults(parts);
                break;
            case "GAME_ENDED":
                System.out.println("  → Xử lý GAME_ENDED với " + parts.length + " parts");
                handleGameEnded(parts);
                break;
            case "PLAYER_JOINED":
                // Có thể hiển thị số lượng player
                break;
            default:
                System.out.println("  ⚠ Command không xử lý: " + command);
        }
    }
    
    /**
     * Xử lý message QUESTION từ server.
     */
    private void handleQuestionMessage(String[] parts) {
        if (parts.length < 7) return;
        
        try {
            int questionId = Integer.parseInt(parts[1]);
            String questionText = parts[2];
            try {
                questionText = URLDecoder.decode(questionText == null ? "" : questionText, StandardCharsets.UTF_8.name());
            } catch (Exception ex) {
                System.err.println("⚠ Không thể decode questionText: " + ex.getMessage());
            }
            int timeLimit = Integer.parseInt(parts[3]);
            int pointValue = Integer.parseInt(parts[4]);
            int questionNumber = Integer.parseInt(parts[5]);
            int totalQuestions = Integer.parseInt(parts[6]);
            System.out.println("📨 Parsed QUESTION: id=" + questionId + ", text='" + questionText + "', timeLimit=" + timeLimit + ", pointValue=" + pointValue + ", questionNumber=" + questionNumber + ", totalQuestions=" + totalQuestions + ", rawParts=" + parts.length);
            
            Question question = new Question(questionId, 0, questionText, questionNumber, timeLimit, pointValue);
            
            // Parse options
            List<Option> options = new ArrayList<>();
            for (int i = 7; i < parts.length; i += 2) {
                if (i + 1 < parts.length) {
                    int optionId = Integer.parseInt(parts[i]);
                    String optionText = parts[i + 1];
                    try {
                        optionText = URLDecoder.decode(optionText == null ? "" : optionText, StandardCharsets.UTF_8.name());
                    } catch (Exception ex) {
                        System.err.println("⚠ Không thể decode optionText: " + ex.getMessage());
                    }
                    System.out.println("   → Option parsed: id=" + optionId + ", text='" + optionText + "'");
                    options.add(new Option(optionId, questionId, optionText, false));
                }
            }
            
            displayQuestion(question, options, questionNumber, totalQuestions);
        } catch (Exception e) {
            System.err.println("Lỗi khi parse question message: " + e.getMessage());
        }
    }
    
    /**
     * Xử lý kết quả đáp án.
     */
    private void handleAnswerResult(String[] parts) {
        if (parts.length < 4) return;
        
        boolean isCorrect = Boolean.parseBoolean(parts[1]);
        int pointsEarned = Integer.parseInt(parts[2]);
        int totalScore = Integer.parseInt(parts[3]);
        double answerTime = parts.length >= 5 ? Double.parseDouble(parts[4]) : 0.0;

        // Show result (feedback to user) and set score to server's authoritative total
        showResult(isCorrect, pointsEarned, answerTime);
        setScore(totalScore);
    }
    
    /**
     * Xử lý khi server gửi kết quả và leaderboard.
     */
    private void handleShowResults(String[] parts) {
        if (parts.length < 3) {
            System.err.println("✗ SHOW_RESULTS không đủ tham số: " + parts.length);
            return;
        }
        
        try {
            int correctOptionId = Integer.parseInt(parts[1]);
            String leaderboardData = parts.length >= 3 ? parts[2] : "";

            // Decode leaderboard (server URL-encodes it to avoid '|' conflicts)
            try {
                leaderboardData = URLDecoder.decode(leaderboardData, StandardCharsets.UTF_8.name());
            } catch (Exception ex) {
                System.err.println("⚠ Không thể decode leaderboard: " + ex.getMessage());
            }

            System.out.println("📊 Nhận SHOW_RESULTS: correctOptionId=" + correctOptionId + ", leaderboard=" + leaderboardData);

            Platform.runLater(() -> {
                // Highlight đáp án đúng
                highlightCorrectAnswer(correctOptionId);
                
                // Hiển thị leaderboard (luôn hiển thị, kể cả nếu rỗng)
                if (leaderboardData != null && !leaderboardData.trim().isEmpty()) {
                    displayLeaderboard(leaderboardData);
                } else {
                    System.err.println("⚠ Leaderboard data rỗng trong SHOW_RESULTS");
                    if (leaderboardListView != null) {
                        leaderboardListView.getItems().clear();
                        leaderboardListView.getItems().add("Đang cập nhật xếp hạng...");
                    }
                }
                
                // Đảm bảo leaderboard được hiển thị
                if (leaderboardListView != null) {
                    leaderboardListView.setVisible(true);
                }
                
                statusLabel.setText("Chờ host chuyển sang câu hỏi tiếp theo...");
                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            });
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi xử lý SHOW_RESULTS: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Highlight đáp án đúng.
     */
    private void highlightCorrectAnswer(int correctOptionId) {
        optionsGrid.getChildren().forEach(node -> {
            if (node instanceof Button) {
                Button button = (Button) node;
                // Lấy optionId từ userData hoặc tag
                Object userData = button.getUserData();
                if (userData != null && userData instanceof Integer) {
                    int optionId = (Integer) userData;
                    if (optionId == correctOptionId) {
                        button.setStyle("-fx-font-size: 16px; -fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }
    
    /**
     * Hiển thị leaderboard.
     */
    private void displayLeaderboard(String leaderboardData) {
        if (leaderboardListView == null) {
            System.err.println("⚠ leaderboardListView là null!");
            return;
        }
        
        Platform.runLater(() -> {
            leaderboardListView.getItems().clear();
            
            if (leaderboardData == null || leaderboardData.trim().isEmpty()) {
                System.out.println("⚠ Leaderboard data rỗng!");
                leaderboardListView.getItems().add("Chưa có dữ liệu xếp hạng");
                return;
            }
            
            System.out.println("📊 Hiển thị leaderboard: " + leaderboardData);
            
            String[] entries = leaderboardData.split(";");
            int count = 0;
            for (String entry : entries) {
                if (entry == null || entry.trim().isEmpty()) continue;
                
                String[] parts = entry.split("\\|");
                if (parts.length >= 3) {
                    try {
                        int rank = Integer.parseInt(parts[0].trim());
                        String name = parts[1].trim();
                        int score = Integer.parseInt(parts[2].trim());
                        
                        // Format đẹp hơn với emoji cho top 3
                        String displayText;
                        if (rank == 1) {
                            displayText = "🥇 " + rank + ". " + name + " - " + score + " điểm";
                        } else if (rank == 2) {
                            displayText = "🥈 " + rank + ". " + name + " - " + score + " điểm";
                        } else if (rank == 3) {
                            displayText = "🥉 " + rank + ". " + name + " - " + score + " điểm";
                        } else {
                            displayText = rank + ". " + name + " - " + score + " điểm";
                        }
                        
                        leaderboardListView.getItems().add(displayText);
                        count++;
                    } catch (NumberFormatException e) {
                        System.err.println("✗ Lỗi parse leaderboard entry: " + entry + " - " + e.getMessage());
                    }
                } else {
                    System.err.println("⚠ Entry không đúng format: " + entry + " (parts: " + parts.length + ")");
                }
            }
            
            System.out.println("✓ Đã hiển thị " + count + " người chơi trong leaderboard");
            
            // Nếu không có dữ liệu, hiển thị thông báo
            if (count == 0) {
                leaderboardListView.getItems().add("Chưa có dữ liệu xếp hạng");
            }
        });
    }
    
    /**
     * Xử lý khi game kết thúc.
     */
    private void handleGameEnded(String[] parts) {
        System.out.println("🎮 Xử lý GAME_ENDED với " + parts.length + " parts");
        for (int i = 0; i < parts.length; i++) {
            System.out.println("  Part[" + i + "]: " + parts[i]);
        }
        
        int finalScore = 0;
        int rank = 1;
        String finalLeaderboard = "";
        
        try {
            if (parts.length >= 2) {
                finalScore = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 3) {
                rank = Integer.parseInt(parts[2]);
            }
            if (parts.length >= 4) {
                finalLeaderboard = parts[3];
                // Try to decode final leaderboard (may be URL encoded)
                try {
                    finalLeaderboard = URLDecoder.decode(finalLeaderboard, StandardCharsets.UTF_8.name());
                } catch (Exception ex) {
                    System.err.println("⚠ Không thể decode final leaderboard: " + ex.getMessage());
                }

                System.out.println("📊 Final leaderboard từ GAME_ENDED: " + finalLeaderboard);
            } else {
                System.err.println("⚠ GAME_ENDED không có leaderboard data (parts.length=" + parts.length + ")");
            }
        } catch (NumberFormatException e) {
            System.err.println("✗ Lỗi parse GAME_ENDED: " + e.getMessage());
            e.printStackTrace();
        }
        
        showFinalResults(finalScore, rank, finalLeaderboard);
    }

    /**
     * Hiển thị câu hỏi và các đáp án.
     */
    public void displayQuestion(Question question, List<Option> options, int questionNumber, int totalQuestions) {
        Platform.runLater(() -> {
            this.currentQuestionNumber = questionNumber;
            this.totalQuestions = totalQuestions;
            this.hasAnswered = false;
            this.selectedOptionId = -1;
            
            // Hiển thị số câu hỏi
            questionNumberLabel.setText("Câu hỏi " + questionNumber + "/" + totalQuestions);
            
            // Hiển thị câu hỏi
            questionTextLabel.setText(question.getQuestionText());
            
            // Xóa các đáp án cũ
            optionsGrid.getChildren().clear();
            
            // Hiển thị các đáp án
            int row = 0;
            int col = 0;
            for (Option option : options) {
                Button optionButton = createOptionButton(option);
                optionsGrid.add(optionButton, col, row);
                
                col++;
                if (col >= 2) {
                    col = 0;
                    row++;
                }
            }
            
            // Bắt đầu timer
            startTimer(question.getTimeLimit());
            
            // Ẩn màn hình chờ
            waitingBox.setVisible(false);
            statusLabel.setText("Chọn đáp án của bạn!");
        });
    }

    /**
     * Tạo button cho một đáp án.
     */
    private Button createOptionButton(Option option) {
        Button button = new Button(option.getOptionText());
        button.setPrefWidth(280);
        button.setPrefHeight(80);
        button.setStyle("-fx-font-size: 16px; -fx-background-color: #7B2CBF; -fx-text-fill: white; -fx-background-radius: 10;");
        button.setUserData(option.getOptionId()); // Lưu optionId để highlight sau
        
        button.setOnAction(e -> {
            if (!hasAnswered) {
                selectOption(option.getOptionId(), button);
            }
        });
        
        return button;
    }

    /**
     * Xử lý khi player chọn đáp án.
     */
    private void selectOption(int optionId, Button button) {
        if (hasAnswered) return;
        
        hasAnswered = true;
        selectedOptionId = optionId;
        
        // Highlight button được chọn
        button.setStyle("-fx-font-size: 16px; -fx-background-color: #ffc107; -fx-text-fill: #333; -fx-background-radius: 10; -fx-font-weight: bold;");
        
        // Disable tất cả buttons
        optionsGrid.getChildren().forEach(node -> {
            if (node instanceof Button) {
                ((Button) node).setDisable(true);
            }
        });
        
        // Gửi đáp án đến server
        if (socketClient != null && socketClient.isConnected()) {
            String message = "SUBMIT_ANSWER|" + selectedOptionId;
            socketClient.sendMessageAsync(message);
            statusLabel.setText("Đã gửi đáp án! Chờ kết quả...");
        }
    }

    /**
     * Bắt đầu đếm ngược timer.
     */
    private void startTimer(int seconds) {
        if (timer != null) {
            timer.cancel();
        }
        
        timeRemaining = seconds;
        updateTimerDisplay();
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    timeRemaining--;
                    if (timeRemaining <= 0) {
                        timer.cancel();
                        timeUp();
                    } else {
                        updateTimerDisplay();
                    }
                });
            }
        }, 1000, 1000);
    }

    /**
     * Cập nhật hiển thị timer.
     */
    private void updateTimerDisplay() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        
        // Đổi màu khi sắp hết thời gian
        if (timeRemaining <= 10) {
            timerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #dc3545; -fx-background-color: #ffebee; -fx-background-radius: 10; -fx-padding: 10 20;");
        } else {
            timerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #28a745; -fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10 20;");
        }
    }

    /**
     * Xử lý khi hết thời gian.
     */
    private void timeUp() {
        if (!hasAnswered) {
            statusLabel.setText("⏰ Hết thời gian! Bạn chưa trả lời.");
            // Disable tất cả buttons
            optionsGrid.getChildren().forEach(node -> {
                if (node instanceof Button) {
                    ((Button) node).setDisable(true);
                }
            });
        }
    }

    /**
     * Cập nhật điểm số.
     */
    public void updateScore(int points) {
        currentScore += points;
        Platform.runLater(() -> {
            scoreLabel.setText("Điểm: " + currentScore);
        });
    }

    /**
     * Đặt điểm số (thay vì cộng thêm) theo giá trị từ server
     */
    public void setScore(int score) {
        this.currentScore = score;
        Platform.runLater(() -> {
            scoreLabel.setText("Điểm: " + currentScore);
        });
    }

    /**
     * Hiển thị kết quả sau khi trả lời.
     */
    public void showResult(boolean isCorrect, int pointsEarned, double answerTime) {
        Platform.runLater(() -> {
            if (isCorrect) {
                String timeStr = String.format("%.1f", answerTime);
                statusLabel.setText("✓ Đúng! Bạn nhận được " + pointsEarned + " điểm! (Thời gian: " + timeStr + "s)");
                statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #28a745; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("✗ Sai! Chờ host hiển thị đáp án đúng...");
                statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #dc3545; -fx-font-weight: bold;");
            }
        });
    }

    /**
     * Hiển thị màn hình chờ.
     */
    public void showWaitingScreen() {
        Platform.runLater(() -> {
            waitingBox.setVisible(true);
            statusLabel.setText("Chờ câu hỏi tiếp theo...");
        });
    }

    /**
     * Hiển thị kết quả cuối cùng.
     */
    public void showFinalResults(int finalScore, int rank, String finalLeaderboard) {
        Platform.runLater(() -> {
            questionTextLabel.setText("🎉 Game kết thúc!");
            questionTextLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #7B2CBF;");
            
            // Hiển thị kết quả của player
            String rankEmoji = "";
            if (rank == 1) rankEmoji = "🥇";
            else if (rank == 2) rankEmoji = "🥈";
            else if (rank == 3) rankEmoji = "🥉";
            
            statusLabel.setText(rankEmoji + " Điểm của bạn: " + finalScore + " | Xếp hạng: #" + rank);
            statusLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #333; -fx-font-weight: bold;");
            
            optionsGrid.getChildren().clear();
            waitingBox.setVisible(false);
            
            // Hiển thị leaderboard cuối cùng (luôn hiển thị, kể cả nếu rỗng)
            System.out.println("📊 Hiển thị leaderboard cuối cùng: " + finalLeaderboard);
            if (finalLeaderboard != null && !finalLeaderboard.trim().isEmpty()) {
                displayLeaderboard(finalLeaderboard);
            } else {
                System.err.println("⚠ Final leaderboard rỗng!");
                if (leaderboardListView != null) {
                    leaderboardListView.getItems().clear();
                    leaderboardListView.getItems().add("Không có dữ liệu xếp hạng");
                }
            }
            
            // Đảm bảo leaderboard được hiển thị
            if (leaderboardListView != null) {
                leaderboardListView.setVisible(true);
            }
        });
    }
    
    /**
     * Overload method cho backward compatibility.
     */
    public void showFinalResults(int finalScore, int rank) {
        showFinalResults(finalScore, rank, null);
    }
}


