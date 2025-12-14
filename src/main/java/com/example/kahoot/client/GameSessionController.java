package com.example.kahoot.client;

import com.example.kahoot.dao.GameSessionDAO;
import com.example.kahoot.model.GameSession;
import com.example.kahoot.model.Quiz;
import com.example.kahoot.util.SocketClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class GameSessionController implements Initializable {

    @FXML private Label pinCodeLabel;
    @FXML private Label quizTitleLabel;
    @FXML private Label messageLabel;
    @FXML private Label playerCountLabel;
    @FXML private Button startQuestionButton;
    @FXML private Button showResultsButton;
    @FXML private javafx.scene.control.ListView<String> playerListView;
    @FXML private javafx.scene.control.ListView<String> notificationListView;

    private GameSession currentSession;
    private GameSessionDAO sessionDAO;
    private SocketClient socketClient;
    private String pinCode;
    private boolean isFirstQuestion = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            sessionDAO = new GameSessionDAO();
            // Kết nối đến server trên máy thật (192.168.1.102:8888)
            socketClient = new SocketClient("192.168.1.102", 8888);
            // Kết nối đến server (không bắt buộc ngay, có thể kết nối sau)
            // Không kết nối ngay trong initialize để tránh block UI
            // Sẽ kết nối khi startGame() được gọi
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi khởi tạo GameSessionController: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Xử lý message từ server (được gọi từ SocketClient listener).
     */
    private void handleServerMessage(String message) {
        if (message == null) return;
        
        String[] parts = message.split("\\|");
        if (parts.length == 0) return;
        
        String command = parts[0];
        
        switch (command) {
            case "PLAYER_JOINED":
                if (parts.length >= 2) {
                    try {
                        int playerCount = Integer.parseInt(parts[1]);
                        Platform.runLater(() -> {
                            updatePlayerCount(playerCount);
                            System.out.println("✓ Cập nhật số người chơi: " + playerCount);
                        });
                    } catch (NumberFormatException e) {
                        System.err.println("✗ Lỗi parse player count: " + parts[1]);
                    }
                }
                break;
            case "PLAYER_LIST":
                if (parts.length >= 2) {
                    String encoded = parts[1];
                    try {
                        String decoded = java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8.name());
                        String[] names = decoded.split(";");
                        Platform.runLater(() -> {
                            if (playerListView != null) {
                                playerListView.getItems().clear();
                                int count = 0;
                                for (String n : names) {
                                    if (n != null && !n.trim().isEmpty()) {
                                        playerListView.getItems().add(n);
                                        count++;
                                    }
                                }
                                updatePlayerCount(count);
                                System.out.println("✓ Cập nhật PLAYER_LIST: " + count + " entries");
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("✗ Lỗi decode PLAYER_LIST: " + e.getMessage());
                    }
                }
                break;
            case "NOTIFICATION":
                if (parts.length >= 2) {
                    String note = parts[1];
                    Platform.runLater(() -> {
                        if (notificationListView != null) {
                            notificationListView.getItems().add(0, note);
                        }
                        // Also briefly show in messageLabel
                        showTemporaryMessage(note);
                        System.out.println("🔔 Thông báo: " + note);
                    });
                }
                break;
            case "GAME_ENDED":
                // Host may receive final leaderboard
                if (parts.length >= 4) {
                    String encodedLeaderboard = parts[3];
                    try {
                        String decoded = java.net.URLDecoder.decode(encodedLeaderboard, java.nio.charset.StandardCharsets.UTF_8.name());
                        Platform.runLater(() -> {
                            // Show final leaderboard in message area and notification list
                            showMessage("Game ended. Final leaderboard updated.", false);
                            if (notificationListView != null) notificationListView.getItems().add(0, "Final leaderboard received");

                            // Parse leaderboard entries and add to player list for quick view
                            if (playerListView != null) {
                                playerListView.getItems().clear();
                                String[] entries = decoded.split(";");
                                for (String entry : entries) {
                                    if (entry == null || entry.trim().isEmpty()) continue;
                                    playerListView.getItems().add(entry);
                                }
                            }

                            System.out.println("📊 Host nhận final leaderboard: " + decoded);
                        });
                    } catch (Exception e) {
                        System.err.println("✗ Lỗi decode GAME_ENDED leaderboard: " + e.getMessage());
                    }
                }
                break;
        }
    }

    private void showTemporaryMessage(String msg) {
        if (messageLabel == null) return;
        messageLabel.setText(msg);
        new Thread(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                messageLabel.setText("");
            });
        }).start();
    }

    /**
     * Thiết lập quiz và tạo game session.
     */
    public void startGame(Quiz quiz) {
        if (quiz == null) {
            showMessage("Lỗi: Quiz không hợp lệ!", true);
            return;
        }

        // Hiển thị thông báo đang xử lý
        showMessage("Đang khởi tạo game session...", false);
        
        // Chạy các thao tác blocking trong background thread để tránh lag UI
        new Thread(() -> {
            try {
                // Tạo PIN code
                pinCode = GameSessionDAO.generatePinCode();
                System.out.println("✓ Đã tạo PIN code: " + pinCode);
                
                // Tạo game session
                GameSession session = new GameSession(quiz.getQuizId(), pinCode);
                System.out.println("✓ Đã tạo GameSession object với quizId: " + quiz.getQuizId());
                
                // Lưu vào database
                System.out.println("🔄 Đang lưu game session vào database...");
                try {
                    sessionDAO.saveSession(session);
                    System.out.println("✓ Đã lưu game session vào database thành công! Session ID: " + session.getSessionId());
                } catch (SQLException dbException) {
                    System.err.println("✗ Lỗi khi lưu vào database:");
                    System.err.println("  Message: " + dbException.getMessage());
                    System.err.println("  SQL State: " + dbException.getSQLState());
                    System.err.println("  Error Code: " + dbException.getErrorCode());
                    dbException.printStackTrace();
                    Platform.runLater(() -> {
                        showMessage("Lỗi kết nối database: " + dbException.getMessage() + 
                                   "\nKiểm tra:\n- MySQL đang chạy\n- Database 'kahoot' đã được tạo\n- Bảng GameSessions tồn tại", true);
                    });
                    return;
                }
                
                this.currentSession = session;

                // Cập nhật UI trên JavaFX thread
                Platform.runLater(() -> {
                    pinCodeLabel.setText(pinCode);
                    quizTitleLabel.setText("Quiz: " + quiz.getTitle());
                });
                
                // Kết nối đến server nếu chưa kết nối
                        if (socketClient == null) {
                            socketClient = new SocketClient("192.168.1.102", 8888);
                        }

                        if (!socketClient.isConnected()) {
                            System.out.println("🔄 Đang kết nối đến server (thử): " + socketClient.getServerHost() + ":" + socketClient.getServerPort());

                            // Thử kết nối tới host mặc định; nếu thất bại, thử localhost và 127.0.0.1
                            String[] tryHosts = new String[] { socketClient.getServerHost(), "localhost", "127.0.0.1" };
                            boolean connected = false;
                            for (String h : tryHosts) {
                                if (h == null) continue;
                                SocketClient sc = new SocketClient(h, socketClient.getServerPort());
                                System.out.println("🔁 Thử kết nối tới: " + h + ":" + sc.getServerPort());
                                if (sc.connect()) {
                                    socketClient = sc; // swap to working client
                                        socketClient.setMessageListener(this::handleServerMessage);
                                    System.out.println("✓ Đã kết nối thành công đến: " + h + ":" + sc.getServerPort());
                                    connected = true;
                                    break;
                                } else {
                                    System.err.println("✗ Không thể kết nối tới: " + h);
                                }
                            }

                            if (!connected) {
                                final String attempted = String.join(", ", tryHosts);
                                Platform.runLater(() -> {
                                    showMessage("Cảnh báo: Không kết nối được với server! Các host đã thử: " + attempted + ".\n" +
                                              "Kiểm tra:\n- Server đang chạy và Firewall không chặn\n- Địa chỉ IP/Port đúng", true);
                                });
                                return;
                            }
                        }
                    // Ensure we always have a listener set for incoming messages
                    if (socketClient != null && socketClient.isConnected()) {
                        socketClient.setMessageListener(this::handleServerMessage);
                    }
                
                // Gửi START_GAME message đến server
                String message = "START_GAME|" + pinCode;
                System.out.println("🔄 Đang gửi START_GAME message: " + message);
                String response = socketClient.sendMessage(message);
                System.out.println("✓ Server response: " + response);
                
                // Cập nhật UI dựa trên response
                Platform.runLater(() -> {
                    if (response != null && response.startsWith("GAME_STARTED")) {
                        showMessage("Game đã được tạo! Người chơi có thể tham gia bằng mã PIN này. Nhấn 'Bắt đầu câu hỏi' để bắt đầu game.", false);
                        updatePlayerCount(0);
                        if (startQuestionButton != null) {
                            startQuestionButton.setDisable(false);
                            startQuestionButton.setText("Bắt đầu câu hỏi");
                        }
                        if (showResultsButton != null) {
                            showResultsButton.setDisable(true);
                        }
                    } else {
                        showMessage("Lỗi khi khởi động game trên server: " + response, true);
                    }
                });
            } catch (Exception e) {
                System.err.println("✗ Lỗi không mong đợi khi tạo game session:");
                System.err.println("  Type: " + e.getClass().getName());
                System.err.println("  Message: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showMessage("Lỗi khi tạo game session: " + e.getMessage(), true);
                });
            }
        }).start();
    }
    
    /**
     * Cập nhật số lượng player.
     */
    public void updatePlayerCount(int count) {
        if (playerCountLabel != null) {
            playerCountLabel.setText("Số người chơi: " + count);
        }
    }

    /**
     * Xử lý khi nhấn nút Kết thúc Game.
     */
    @FXML
    public void handleEndGameAction(ActionEvent event) {
        if (currentSession == null) {
            showMessage("Không có game session đang chạy!", true);
            return;
        }

        try {
            // Gửi END_GAME message đến server
            if (socketClient != null && socketClient.isConnected() && pinCode != null) {
                String message = "END_GAME|" + pinCode;
                String response = socketClient.sendMessage(message);
                System.out.println("Server response: " + response);
            }
            
            sessionDAO.endSession(currentSession.getSessionId());
            showMessage("Game đã kết thúc!", false);
            
            // Disable nút kết thúc
            Button endButton = (Button) event.getSource();
            endButton.setDisable(true);
            
            // Đóng kết nối
            if (socketClient != null) {
                socketClient.disconnect();
            }
        } catch (SQLException e) {
            showMessage("Lỗi khi kết thúc game: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Xử lý khi nhấn nút Đóng.
     */
    @FXML
    public void handleCloseAction(ActionEvent event) {
        // Đóng kết nối socket trước khi đóng cửa sổ
        if (socketClient != null) {
            socketClient.disconnect();
        }
        
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        javafx.stage.Stage stage = (javafx.stage.Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Xử lý khi nhấn nút Bắt đầu câu hỏi.
     */
    @FXML
    public void handleStartQuestionAction(ActionEvent event) {
        if (pinCode == null) {
            showMessage("Lỗi: Chưa có game session!", true);
            return;
        }

        if (socketClient != null && socketClient.isConnected()) {
            if (isFirstQuestion) {
                // Câu hỏi đầu tiên - gọi startGame() trên server
                String message = "START_QUESTION|" + pinCode;
                String response = socketClient.sendMessage(message);
                System.out.println("Server response: " + response);
                
                if (response != null && response.startsWith("START_QUESTION_OK")) {
                    showMessage("Đã bắt đầu câu hỏi đầu tiên!", false);
                    isFirstQuestion = false;
                    if (startQuestionButton != null) {
                        startQuestionButton.setText("Câu hỏi tiếp theo");
                        startQuestionButton.setDisable(true);
                    }
                    if (showResultsButton != null) {
                        showResultsButton.setDisable(false);
                    }
                } else {
                    showMessage("Lỗi: " + response, true);
                }
            } else {
                // Câu hỏi tiếp theo
                String message = "NEXT_QUESTION|" + pinCode;
                String response = socketClient.sendMessage(message);
                System.out.println("Server response: " + response);
                
                if (response != null && response.startsWith("NEXT_QUESTION_OK")) {
                    showMessage("Đã chuyển sang câu hỏi tiếp theo!", false);
                    if (showResultsButton != null) {
                        showResultsButton.setDisable(false);
                    }
                    if (startQuestionButton != null) {
                        startQuestionButton.setDisable(true);
                    }
                } else {
                    showMessage("Lỗi: " + response, true);
                }
            }
        } else {
            showMessage("Không kết nối được với server!", true);
        }
    }

    /**
     * Xử lý khi nhấn nút Hiển thị kết quả.
     */
    @FXML
    public void handleShowResultsAction(ActionEvent event) {
        if (pinCode == null) {
            showMessage("Lỗi: Chưa có game session!", true);
            return;
        }

        if (socketClient != null && socketClient.isConnected()) {
            String message = "SHOW_RESULTS|" + pinCode;
            String response = socketClient.sendMessage(message);
            System.out.println("Server response: " + response);
            
            if (response != null && response.startsWith("SHOW_RESULTS_OK")) {
                showMessage("Đã hiển thị kết quả và leaderboard!", false);
                if (startQuestionButton != null) {
                    startQuestionButton.setDisable(false);
                }
                if (showResultsButton != null) {
                    showResultsButton.setDisable(true);
                }
            } else {
                showMessage("Lỗi: " + response, true);
            }
        } else {
            showMessage("Không kết nối được với server!", true);
        }
    }

    /**
     * Hiển thị thông báo.
     */
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}

