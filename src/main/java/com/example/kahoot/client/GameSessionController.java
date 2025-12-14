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
    @FXML private javafx.scene.control.TextField serverHostField;
    @FXML private javafx.scene.control.TextField serverPortField;
    @FXML private Button connectButton;
    @FXML private Label connectionStatusLabel;

    private GameSession currentSession;
    private GameSessionDAO sessionDAO;
    private SocketClient socketClient;
    private String pinCode;
    private boolean isFirstQuestion = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            sessionDAO = new GameSessionDAO();
            // Không khởi tạo SocketClient cố định ở đây để tránh hard-coded IP gây lỗi kết nối
            // SocketClient sẽ được tạo khi người dùng nhấn 'Kết nối' hoặc khi startGame() cần kết nối tự động
            socketClient = null;
            if (connectionStatusLabel != null) connectionStatusLabel.setText("Chưa kết nối");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi khởi tạo GameSessionController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Thử kết nối tới server theo host/port được nhập trong UI (non-blocking).
     */
    @FXML
    public void handleConnectAction(ActionEvent event) {
        String host = (serverHostField != null) ? serverHostField.getText().trim() : "localhost";
        String portStr = (serverPortField != null) ? serverPortField.getText().trim() : "8888";
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            updateConnectionStatus("Port không hợp lệ", true);
            return;
        }

        // Disable connect button while attempting
        Platform.runLater(() -> { if (connectButton != null) connectButton.setDisable(true); });
        updateConnectionStatus("Đang kết nối...", false);

        final int fp = port;
        final String fh = host;
        new Thread(() -> {
            try {
                SocketClient sc = new SocketClient(fh, fp);
                if (sc.connect()) {
                    // Successful connection
                    sc.setMessageListener(this::handleServerMessage);
                    // Swap current socket and update UI
                    socketClient = sc;
                    updateConnectionStatus("Đã kết nối: " + fh + ":" + fp, false);
                    // Re-enable connect button
                    Platform.runLater(() -> { if (connectButton != null) connectButton.setDisable(false); });
                } else {
                    updateConnectionStatus("Không thể kết nối đến " + fh + ":" + fp, true);
                    Platform.runLater(() -> { if (connectButton != null) connectButton.setDisable(false); });
                }
            } catch (Exception e) {
                updateConnectionStatus("Lỗi khi kết nối: " + e.getMessage(), true);
                Platform.runLater(() -> { if (connectButton != null) connectButton.setDisable(false); });
            }
        }).start();
    }

    private void updateConnectionStatus(String msg, boolean isError) {
        Platform.runLater(() -> {
            if (connectionStatusLabel != null) connectionStatusLabel.setText(msg);
            if (messageLabel != null && isError) {
                showMessage(msg, true);
            }
            System.out.println("Connection status: " + msg);
        });
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
            case "PLAYER_LEFT":
                if (parts.length >= 2) {
                    try {
                        int playerCount = Integer.parseInt(parts[1]);
                        Platform.runLater(() -> {
                            updatePlayerCount(playerCount);
                            showTemporaryMessage("Một người chơi đã rời phòng. Còn " + playerCount + " người.");
                            System.out.println("✓ PLAYER_LEFT: " + playerCount);
                        });
                    } catch (NumberFormatException e) {
                        System.err.println("✗ Lỗi parse PLAYER_LEFT: " + parts[1]);
                    }
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
                        // Ensure we have a connected SocketClient. Prefer the one set from UI if available
                        if (socketClient == null || !socketClient.isConnected()) {
                            // Try host/port from UI fields first
                            String host = (serverHostField != null && serverHostField.getText() != null && !serverHostField.getText().trim().isEmpty()) ? serverHostField.getText().trim() : "localhost";
                            int port = 8888;
                            try {
                                if (serverPortField != null && serverPortField.getText() != null && !serverPortField.getText().trim().isEmpty()) {
                                    port = Integer.parseInt(serverPortField.getText().trim());
                                }
                            } catch (NumberFormatException ignored) {}

                            // Try connect synchronously but not blocking UI (we are already in background thread)
                            boolean connected = false;
                            String[] tryHosts = new String[] { host, "localhost", "127.0.0.1" };
                            for (String h : tryHosts) {
                                try {
                                    SocketClient sc = new SocketClient(h, port);
                                    System.out.println("🔁 Thử kết nối tới: " + h + ":" + port);
                                    if (sc.connect()) {
                                        socketClient = sc;
                                        socketClient.setMessageListener(this::handleServerMessage);
                                        updateConnectionStatus("Đã kết nối: " + h + ":" + port, false);
                                        connected = true;
                                        break;
                                    }
                                } catch (Exception e) {
                                    System.err.println("✗ Lỗi khi thử kết nối đến " + h + ":" + port + " -> " + e.getMessage());
                                }
                            }

                            if (!connected) {
                                final String attempted = host + ", localhost, 127.0.0.1";
                                Platform.runLater(() -> {
                                    showMessage("Cảnh báo: Không kết nối được với server! Các host đã thử: " + attempted + ".\nKiểm tra server và firewall.", true);
                                });
                                return;
                            }
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

