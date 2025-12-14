package com.example.kahoot.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerDashboardController implements Initializable {

    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Label statusLabel;
    @FXML private Label clientCountLabel;
    @FXML private Label gameCountLabel;
    @FXML private TextArea logArea;

    private KahootServer server;
    private ScheduledExecutorService updateService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Thiết lập giá trị mặc định
        if (ipField != null && ipField.getText().isEmpty()) {
            ipField.setText("0.0.0.0"); // 0.0.0.0 = bind vào tất cả interfaces
        }
        if (portField != null && portField.getText().isEmpty()) {
            portField.setText("8888");
        }
        addLog("Server Dashboard đã khởi động");
        addLog("Nhấn 'Khởi động Server' để bắt đầu");
    }

    /**
     * Xử lý khi nhấn nút Khởi động Server.
     */
    @FXML
    public void handleStartServerAction(ActionEvent event) {
        try {
            String bindIp = ipField.getText().trim();
            if (bindIp.isEmpty()) {
                addLog("Lỗi: Vui lòng nhập IP address!");
                return;
            }
            
            int port = Integer.parseInt(portField.getText());
            
            if (port < 1024 || port > 65535) {
                addLog("Lỗi: Port phải nằm trong khoảng 1024-65535");
                return;
            }

            // Tạo và khởi động server trong thread riêng
            server = new KahootServer(bindIp, port);
            server.setStatusCallback(message -> addLog(message));
            
            // Chạy server trong background thread
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Platform.runLater(() -> {
                        startButton.setDisable(true);
                        stopButton.setDisable(false);
                        ipField.setDisable(true);
                        portField.setDisable(true);
                        statusLabel.setText("Đang chạy");
                        statusLabel.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 15;");
                        addLog("Đang khởi động server trên " + bindIp + ":" + port + "...");
                    });
                    
                    server.start();
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        addLog("Lỗi khi khởi động server: " + e.getMessage());
                        startButton.setDisable(false);
                        stopButton.setDisable(true);
                        portField.setDisable(false);
                        statusLabel.setText("Lỗi");
                        statusLabel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 15;");
                    });
                }
            });

            // Bắt đầu cập nhật thống kê định kỳ
            startUpdateService();

        } catch (NumberFormatException e) {
            addLog("Lỗi: Port phải là số!");
        }
    }

    /**
     * Xử lý khi nhấn nút Dừng Server.
     */
    @FXML
    public void handleStopServerAction(ActionEvent event) {
        if (server != null) {
            server.stop();
            addLog("Server đã dừng");
            
            startButton.setDisable(false);
            stopButton.setDisable(true);
            ipField.setDisable(false);
            portField.setDisable(false);
            statusLabel.setText("Đã dừng");
            statusLabel.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 15;");
            
            if (updateService != null) {
                updateService.shutdown();
            }
        }
    }

    /**
     * Xử lý khi nhấn nút Xóa Log.
     */
    @FXML
    public void handleClearLogAction(ActionEvent event) {
        logArea.clear();
    }

    /**
     * Bắt đầu service cập nhật thống kê định kỳ.
     */
    private void startUpdateService() {
        updateService = Executors.newScheduledThreadPool(1);
        updateService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (server != null && server.isRunning()) {
                    clientCountLabel.setText(String.valueOf(server.getActiveClientCount()));
                    gameCountLabel.setText(String.valueOf(server.getActiveGameCount()));
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Thêm log vào text area.
     */
    public void addLog(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString();
            logArea.appendText("[" + timestamp + "] " + message + "\n");
            // Auto scroll to bottom
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}

