package com.example.kahoot.client;

import com.example.kahoot.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel; // Để hiển thị thông báo lỗi

    private final AuthService authService = new AuthService();

    /**
     * Xử lý khi người dùng nhấn nút Đăng nhập.
     */
    @FXML
    public void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Clear previous message
        messageLabel.setText("Đang kiểm tra...");
        messageLabel.setStyle("-fx-text-fill: blue;");

        try {
            User user = authService.authenticate(username, password);

            if (user != null) {
                messageLabel.setText("Đăng nhập thành công!");
                messageLabel.setStyle("-fx-text-fill: green;");
                
                try {
                    // Chuyển sang màn hình host dashboard
                    loadHostDashboard(user, event);
                } catch (IOException e) {
                    messageLabel.setText("Lỗi khi chuyển màn hình: " + e.getMessage());
                    messageLabel.setStyle("-fx-text-fill: red;");
                    e.printStackTrace();
                }
            } else {
                messageLabel.setText("Đăng nhập thất bại. Kiểm tra lại tên/mật khẩu hoặc kết nối database.");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (Exception e) {
            messageLabel.setText("Lỗi: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    /**
     * Xử lý khi người dùng nhấn link Đăng ký.
     */
    @FXML
    public void handleRegisterLinkAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/register.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 400, 450);
            stage.setTitle("Kahoot Desktop - Đăng ký");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            messageLabel.setText("Lỗi khi chuyển màn hình: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    /**
     * Xử lý khi người dùng nhấn nút Tham gia Game (không cần đăng nhập).
     */
    @FXML
    public void handleJoinGameAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/player.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 500, 400);
            stage.setTitle("Kahoot Desktop - Tham gia Game");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            messageLabel.setText("Lỗi khi mở màn hình tham gia game: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    /**
     * Tải và hiển thị màn hình Host Dashboard.
     */
    private void loadHostDashboard(User user, ActionEvent event) throws IOException {
        try {
            System.out.println("Đang load host_dashboard.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/host_dashboard.fxml"));
            
            if (loader.getLocation() == null) {
                throw new IOException("Không tìm thấy file host_dashboard.fxml");
            }
            
            Parent root = loader.load();
            System.out.println("Đã load FXML thành công");
            
            // Truyền thông tin user sang controller của dashboard
            HostDashboardController controller = loader.getController();
            if (controller == null) {
                throw new IOException("Không thể lấy controller từ FXML");
            }
            
            System.out.println("Đang set user cho controller...");
            controller.setCurrentUser(user);
            System.out.println("Đã set user thành công");
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Kahoot Desktop - Host Dashboard");
            stage.setScene(scene);
            stage.show();
            System.out.println("Đã hiển thị dashboard");
        } catch (Exception e) {
            System.err.println("Lỗi chi tiết khi load dashboard: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Lỗi khi tải màn hình dashboard: " + e.getMessage(), e);
        }
    }
}