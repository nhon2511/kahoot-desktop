package com.example.kahoot.client;

import com.example.kahoot.util.SocketClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller cho màn hình player tham gia game.
 */
public class PlayerController implements Initializable {

    @FXML private TextField serverIpField;
    @FXML private TextField serverPortField;
    @FXML private TextField pinCodeField;
    @FXML private TextField playerNameField;
    @FXML private Label messageLabel;
    @FXML private Button joinButton;

    private SocketClient socketClient;
    private String playerName;
    private String joinedPin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Không tự động kết nối, chờ user nhập IP và PORT
        showMessage("Nhập thông tin server, mã PIN và tên để tham gia!", false);
    }

    /**
     * Xử lý khi nhấn nút Tham gia Game.
     */
    @FXML
    public void handleJoinGameAction(ActionEvent event) {
        String serverIp = serverIpField.getText().trim();
        String serverPortStr = serverPortField.getText().trim();
        String pinCode = pinCodeField.getText().trim();
        String playerName = playerNameField.getText().trim();

        if (serverIp.isEmpty()) {
            showMessage("Vui lòng nhập IP server!", true);
            return;
        }

        if (serverPortStr.isEmpty()) {
            showMessage("Vui lòng nhập Port server!", true);
            return;
        }

        int serverPort;
        try {
            serverPort = Integer.parseInt(serverPortStr);
        } catch (NumberFormatException e) {
            showMessage("Port không hợp lệ!", true);
            return;
        }

        if (pinCode.isEmpty()) {
            showMessage("Vui lòng nhập mã PIN!", true);
            return;
        }

        if (playerName.isEmpty()) {
            showMessage("Vui lòng nhập tên của bạn!", true);
            return;
        }

        // Tạo socket client với IP và PORT
        socketClient = new SocketClient(serverIp, serverPort);
        
        // Kết nối đến server
        showMessage("Đang kết nối đến server...", false);
        if (!socketClient.connect()) {
            showMessage("Không thể kết nối đến server " + serverIp + ":" + serverPort + "!", true);
            return;
        }

        // Lưu playerName vào field
        this.playerName = playerName;
        
        // Gửi JOIN_GAME message đến server
        String message = "JOIN_GAME|" + pinCode + "|" + playerName;
        System.out.println("Gửi JOIN_GAME: " + message);
        
        try {
            String rawResponse = socketClient.sendMessage(message);
            
            // Log chi tiết response
            System.out.println("═══════════════════════════════════════════");
            System.out.println("📥 Server response nhận được:");
            System.out.println("   Raw: " + rawResponse);
            System.out.println("   Length: " + (rawResponse != null ? rawResponse.length() : 0));
            System.out.println("   Starts with JOIN_SUCCESS: " + (rawResponse != null && rawResponse.startsWith("JOIN_SUCCESS")));
            System.out.println("   Starts with JOIN_FAILED: " + (rawResponse != null && rawResponse.startsWith("JOIN_FAILED")));
            System.out.println("   Starts with ERROR: " + (rawResponse != null && rawResponse.startsWith("ERROR")));
            System.out.println("═══════════════════════════════════════════");

            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                Platform.runLater(() -> {
                    showMessage("Lỗi: Không nhận được phản hồi từ server. Kiểm tra server đã chạy chưa!", true);
                });
                return;
            }

            // Trim response để loại bỏ whitespace
            final String response = rawResponse.trim();

            if (response.startsWith("JOIN_SUCCESS")) {
                String[] parts = response.split("\\|");
                System.out.println("✓ JOIN_SUCCESS! Parts length: " + parts.length);
                System.out.println("   Parts: " + java.util.Arrays.toString(parts));
                
                if (parts.length >= 3) {
                    System.out.println("═══════════════════════════════════════════");
                    System.out.println("🎮 Bắt đầu mở màn hình chơi game...");
                    System.out.println("═══════════════════════════════════════════");
                    
                    // Lưu PIN đã join
                    this.joinedPin = pinCode;

                    // Hiển thị thông báo trước
                    showMessage("Đã tham gia game thành công! Đang chuyển đến màn hình chơi game...", false);
                    
                    // Đợi một chút để thông báo hiển thị
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Mở màn hình chơi game (đảm bảo chạy trên JavaFX thread)
                    Platform.runLater(() -> {
                            try {
                                System.out.println("🔄 Platform.runLater: Bắt đầu openGameScreen()");
                                openGameScreen();
                                System.out.println("✓ Platform.runLater: openGameScreen() hoàn thành");
                            } catch (Exception e) {
                            System.err.println("✗ Lỗi khi mở màn hình chơi game: " + e.getMessage());
                            e.printStackTrace();
                            showMessage("Lỗi khi mở màn hình chơi game: " + e.getMessage(), true);
                        }
                    });
                } else {
                    showMessage("Lỗi: Response không đúng format. Server response: " + response + " (Parts: " + parts.length + ")", true);
                }
            } else if (response.startsWith("JOIN_FAILED")) {
                String errorMsg = response.contains("|") ? response.split("\\|")[1] : "Không thể tham gia game";
                System.out.println("✗ JOIN_FAILED: " + errorMsg);
                Platform.runLater(() -> {
                    showMessage("Lỗi: " + errorMsg, true);
                });
            } else if (response.startsWith("ERROR")) {
                String errorMsg = response.contains("|") ? response.split("\\|")[1] : response;
                System.out.println("✗ ERROR từ server: " + errorMsg);
                Platform.runLater(() -> {
                    showMessage("Lỗi server: " + errorMsg, true);
                });
            } else {
                // Log toàn bộ response để debug
                System.err.println("⚠ Response không xác định:");
                System.err.println("   Full response: [" + response + "]");
                System.err.println("   First 50 chars: " + (response.length() > 50 ? response.substring(0, 50) + "..." : response));
                
                final String displayResponse = response.length() > 100 ? response.substring(0, 100) + "..." : response;
                Platform.runLater(() -> {
                    showMessage("Lỗi: Response không xác định từ server. Xem console để biết chi tiết.\nResponse: " + displayResponse, true);
                });
            }
        } catch (Exception e) {
            System.err.println("✗ Exception khi gửi/nhận message: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                showMessage("Lỗi kết nối: " + e.getMessage(), true);
            });
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
     * Mở màn hình chơi game.
     */
    private void openGameScreen() throws Exception {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("📂 Bắt đầu load player_game.fxml...");
        System.out.println("   Player name: " + playerName);
        System.out.println("   SocketClient: " + (socketClient != null ? "OK" : "NULL"));
        System.out.println("   Is connected: " + (socketClient != null && socketClient.isConnected()));
        System.out.println("═══════════════════════════════════════════");
        
        // Kiểm tra socket client
        if (socketClient == null) {
            throw new Exception("Socket client là null!");
        }
        
        if (!socketClient.isConnected()) {
            throw new Exception("Socket client chưa kết nối hoặc đã bị ngắt!");
        }
        
        // Kiểm tra file FXML có tồn tại không
        java.net.URL fxmlUrl = getClass().getResource("/views/player_game.fxml");
        if (fxmlUrl == null) {
            System.err.println("✗ Không tìm thấy file player_game.fxml!");
            System.err.println("   Đang tìm trong: " + getClass().getResource("/views/"));
            throw new IOException("Không tìm thấy file player_game.fxml trong resources!");
        }
        
        System.out.println("✓ Tìm thấy FXML file: " + fxmlUrl);
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        System.out.println("✓ FXMLLoader created");
        
        Parent root;
        try {
            root = loader.load();
            System.out.println("✓ FXML loaded successfully");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi load FXML: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Lỗi khi load FXML: " + e.getMessage(), e);
        }
        
        PlayerGameController gameController = loader.getController();
        System.out.println("   Controller: " + (gameController != null ? "OK" : "NULL"));
        
        if (gameController == null) {
            throw new Exception("Không thể khởi tạo PlayerGameController! Kiểm tra fx:controller trong player_game.fxml");
        }
        
        try {
            gameController.setup(playerName, socketClient, this.joinedPin);
            System.out.println("✓ Controller setup completed");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi setup controller: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Lỗi khi setup controller: " + e.getMessage(), e);
        }
        
        // Lấy stage từ button
        javafx.scene.Node source = joinButton;
        if (source == null || source.getScene() == null) {
            throw new Exception("Không thể lấy Scene từ joinButton!");
        }
        
        Stage stage = (Stage) source.getScene().getWindow();
        if (stage == null) {
            throw new Exception("Không thể lấy Stage từ scene!");
        }
        
        System.out.println("✓ Stage retrieved: " + stage);
        
        try {
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Kahoot - " + playerName);
            stage.setScene(scene);
            stage.show();
            stage.toFront(); // Đưa cửa sổ lên trước
            System.out.println("═══════════════════════════════════════════");
            System.out.println("✅ Màn hình chơi game đã được hiển thị!");
            System.out.println("═══════════════════════════════════════════");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi hiển thị scene: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Lỗi khi hiển thị scene: " + e.getMessage(), e);
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


