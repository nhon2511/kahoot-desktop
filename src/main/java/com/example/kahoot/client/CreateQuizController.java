package com.example.kahoot.client;

import com.example.kahoot.dao.QuizDAO;
import com.example.kahoot.model.Quiz;
import com.example.kahoot.model.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.concurrent.Task;
import javafx.application.Platform;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CreateQuizController implements Initializable {

    @FXML private TextField titleField;
    @FXML private TextField accessCodeField;
    @FXML private Label messageLabel;

    private User hostUser;
    private HostDashboardController dashboardController;
    private QuizDAO quizDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        quizDAO = new QuizDAO();
        // Tự động tạo mã truy cập khi mở màn hình (sau khi FXML đã được inject)
        if (accessCodeField != null) {
            generateAccessCode();
        }
    }

    /**
     * Thiết lập user host.
     */
    public void setHostUser(User user) {
        this.hostUser = user;
        // Nếu field đã được inject, tạo mã ngay
        if (accessCodeField != null) {
            generateAccessCode();
        }
    }

    /**
     * Thiết lập dashboard controller để refresh danh sách sau khi tạo quiz.
     */
    public void setDashboardController(HostDashboardController controller) {
        this.dashboardController = controller;
    }

    /**
     * Xử lý khi nhấn nút Tạo mã mới.
     */
    @FXML
    public void handleGenerateCodeAction(ActionEvent event) {
        generateAccessCode();
    }

    /**
     * Tạo mã truy cập ngẫu nhiên.
     */
    private void generateAccessCode() {
        if (accessCodeField != null) {
            String code = QuizDAO.generateAccessCode();
            accessCodeField.setText(code);
        }
    }

    /**
     * Xử lý khi nhấn nút Tạo Quiz.
     */
    @FXML
    public void handleCreateButtonAction(ActionEvent event) {
        String title = titleField.getText().trim();
        String accessCode = accessCodeField.getText().trim();

        // Validation
        if (title.isEmpty()) {
            messageLabel.setText("Vui lòng nhập tiêu đề quiz!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (title.length() < 3) {
            messageLabel.setText("Tiêu đề phải có ít nhất 3 ký tự!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (accessCode.isEmpty()) {
            messageLabel.setText("Vui lòng tạo mã truy cập!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (accessCode.length() != 6 || !accessCode.matches("\\d+")) {
            messageLabel.setText("Mã truy cập phải là 6 chữ số!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Check host user is set
        if (hostUser == null) {
            messageLabel.setText("Không có host user. Vui lòng đăng nhập lại.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Prepare quiz to save
        Quiz newQuiz = new Quiz(title, hostUser.getUserId(), accessCode);

        // Inform user and run DB operation on background thread to avoid blocking UI
        messageLabel.setText("Đang tạo quiz...");
        messageLabel.setStyle("-fx-text-fill: blue;");

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                quizDAO.saveQuiz(newQuiz);
                return null;
            }
        };

        saveTask.setOnSucceeded(ev -> {
            messageLabel.setText("Tạo quiz thành công!");
            messageLabel.setStyle("-fx-text-fill: green;");

            // Refresh danh sách quiz trong dashboard
            if (dashboardController != null) {
                dashboardController.refreshQuizList();
            }

            // Đóng cửa sổ sau 1 giây
            Platform.runLater(() -> {
                javafx.util.Duration delay = javafx.util.Duration.seconds(1);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(delay);
                pause.setOnFinished(e -> {
                    javafx.scene.Node source = (javafx.scene.Node) event.getSource();
                    javafx.stage.Stage stage = (javafx.stage.Stage) source.getScene().getWindow();
                    stage.close();
                });
                pause.play();
            });
        });

        saveTask.setOnFailed(ev -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null ? ex.getMessage() : "Lỗi không xác định";
            Platform.runLater(() -> {
                messageLabel.setText("Lỗi khi tạo quiz: " + msg);
                messageLabel.setStyle("-fx-text-fill: red;");
            });
            if (ex != null) ex.printStackTrace();
        });

        Thread t = new Thread(saveTask);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Xử lý khi nhấn nút Hủy.
     */
    @FXML
    public void handleCancelButtonAction(ActionEvent event) {
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        javafx.stage.Stage stage = (javafx.stage.Stage) source.getScene().getWindow();
        stage.close();
    }
}

