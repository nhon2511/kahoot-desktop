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
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();

    /**
     * Xử lý khi người dùng nhấn nút Đăng ký.
     */
    @FXML
    public void handleRegisterButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();

        // Validation
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.isEmpty()) {
            messageLabel.setText("Vui lòng điền đầy đủ thông tin!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (username.length() < 3) {
            messageLabel.setText("Tên đăng nhập phải có ít nhất 3 ký tự!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (password.length() < 3) {
            messageLabel.setText("Mật khẩu phải có ít nhất 3 ký tự!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Mật khẩu xác nhận không khớp!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            messageLabel.setText("Email không hợp lệ!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Clear previous message
        messageLabel.setText("Đang đăng ký...");
        messageLabel.setStyle("-fx-text-fill: blue;");

        try {
            User newUser = authService.registerUser(username, password, email);
            
            if (newUser != null) {
                messageLabel.setText("Đăng ký thành công! Đang chuyển đến trang đăng nhập...");
                messageLabel.setStyle("-fx-text-fill: green;");
                
                // Chờ 1.5 giây rồi chuyển về trang đăng nhập
                PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                pause.setOnFinished(e -> loadLoginPage(event));
                pause.play();
            }
        } catch (Exception e) {
            messageLabel.setText("Lỗi đăng ký: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    /**
     * Xử lý khi người dùng nhấn link Đăng nhập.
     */
    @FXML
    public void handleLoginLinkAction(ActionEvent event) {
        loadLoginPage(event);
    }

    /**
     * Tải và hiển thị màn hình đăng nhập.
     */
    private void loadLoginPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 400, 350);
            stage.setTitle("Kahoot Desktop - Đăng nhập");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            messageLabel.setText("Lỗi khi chuyển màn hình: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }
}

