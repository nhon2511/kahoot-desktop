package com.example.kahoot.client;

import com.example.kahoot.dao.QuizDAO;
import com.example.kahoot.model.Quiz;
import com.example.kahoot.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class HostDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private ListView<Quiz> quizListView;
    @FXML private Button createQuizButton;
    @FXML private Button manageQuizButton;
    @FXML private Button startGameButton;
    @FXML private Button logoutButton;

    private User currentUser;
    private QuizDAO quizDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        quizDAO = new QuizDAO();
        // User sẽ được set từ LoginController
    }

    /**
     * Thiết lập user hiện tại và load danh sách quiz.
     */
    public void setCurrentUser(User user) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("📋 HostDashboardController.setCurrentUser() được gọi");
        System.out.println("  User: " + (user != null ? user.getUsername() : "null"));
        System.out.println("  User ID: " + (user != null ? user.getUserId() : "null"));
        System.out.println("═══════════════════════════════════════════════════════");
        
        this.currentUser = user;
        if (user != null) {
            // Kiểm tra null để tránh lỗi khi FXML chưa được inject
            if (welcomeLabel != null) {
                welcomeLabel.setText("Xin chào, " + user.getUsername() + "!");
                System.out.println("✓ Đã set welcomeLabel");
            } else {
                System.err.println("⚠ welcomeLabel là null!");
            }
            
            if (quizListView == null) {
                System.err.println("⚠ quizListView là null! Không thể load quiz.");
            } else {
                System.out.println("✓ quizListView đã được khởi tạo, bắt đầu load quiz...");
                loadQuizzes();
            }
        } else {
            System.err.println("⚠ User là null, không thể load quiz!");
        }
    }

    /**
     * Load danh sách quiz của host.
     */
    private void loadQuizzes() {
        if (currentUser == null) {
            System.err.println("⚠ loadQuizzes: currentUser là null!");
            return;
        }

        System.out.println("🔄 Đang load quiz cho user ID: " + currentUser.getUserId() + " (username: " + currentUser.getUsername() + ")");

        try {
            List<Quiz> quizzes = quizDAO.findByHostId(currentUser.getUserId());
            System.out.println("✓ Tìm thấy " + quizzes.size() + " quiz cho user ID: " + currentUser.getUserId());
            
            if (quizzes.isEmpty()) {
                System.out.println("⚠ Không có quiz nào cho user này. Có thể user chưa tạo quiz nào.");
            } else {
                for (Quiz q : quizzes) {
                    System.out.println("  - Quiz ID: " + q.getQuizId() + ", Title: " + q.getTitle() + ", Access Code: " + q.getAccessCode());
                }
            }
            
            quizListView.getItems().clear();
            quizListView.getItems().addAll(quizzes);
            
            // Custom cell factory để hiển thị đẹp hơn
            quizListView.setCellFactory(param -> new javafx.scene.control.ListCell<Quiz>() {
                @Override
                protected void updateItem(Quiz quiz, boolean empty) {
                    super.updateItem(quiz, empty);
                    if (empty || quiz == null) {
                        setText(null);
                    } else {
                        setText(quiz.getTitle() + " (Mã: " + quiz.getAccessCode() + ")");
                    }
                }
            });
            
            System.out.println("✓ Đã load " + quizListView.getItems().size() + " quiz vào ListView");
        } catch (SQLException e) {
            System.err.println("✗ Lỗi SQL khi load quiz: " + e.getMessage());
            e.printStackTrace();
            
            // Hiển thị lỗi cho user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể tải danh sách quiz");
            alert.setContentText("Lỗi: " + e.getMessage() + "\n\nVui lòng kiểm tra kết nối database.");
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("✗ Lỗi không mong đợi khi load quiz: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Lỗi không mong đợi");
            alert.setContentText("Lỗi: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Xử lý khi nhấn nút Tạo Quiz Mới.
     */
    @FXML
    public void handleCreateQuizButtonAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/create_quiz.fxml"));
            Parent root = loader.load();
            
            // Truyền user và controller này sang CreateQuizController
            CreateQuizController controller = loader.getController();
            if (controller != null) {
                controller.setHostUser(currentUser);
                controller.setDashboardController(this);
            }
            
            // Tạo cửa sổ mới thay vì thay đổi scene hiện tại
            Stage newStage = new Stage();
            Scene scene = new Scene(root, 600, 400);
            newStage.setTitle("Tạo Quiz Mới");
            newStage.setScene(scene);
            newStage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            newStage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi mở màn hình tạo quiz: " + e.getMessage());
            e.printStackTrace();
            // Hiển thị thông báo lỗi cho user
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể mở màn hình tạo quiz");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Xử lý khi nhấn nút Quản lý Quiz.
     */
    @FXML
    public void handleManageQuizButtonAction(ActionEvent event) {
        Quiz selectedQuiz = quizListView.getSelectionModel().getSelectedItem();
        if (selectedQuiz == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cảnh báo");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một quiz để quản lý!");
            alert.showAndWait();
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/manage_quiz.fxml"));
            Parent root = loader.load();
            
            ManageQuizController controller = loader.getController();
            if (controller != null) {
                controller.setQuiz(selectedQuiz);
            }
            
            // Tạo cửa sổ mới
            Stage newStage = new Stage();
            Scene scene = new Scene(root, 900, 700);
            newStage.setTitle("Quản lý Quiz: " + selectedQuiz.getTitle());
            newStage.setScene(scene);
            newStage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            newStage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi mở màn hình quản lý quiz: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể mở màn hình quản lý quiz");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Xử lý khi nhấn nút Bắt đầu Game.
     */
    @FXML
    public void handleStartGameButtonAction(ActionEvent event) {
        Quiz selectedQuiz = quizListView.getSelectionModel().getSelectedItem();
        if (selectedQuiz == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cảnh báo");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một quiz để bắt đầu game!");
            alert.showAndWait();
            return;
        }
        
        // Kiểm tra quiz có câu hỏi chưa
        try {
            com.example.kahoot.dao.QuestionDAO questionDAO = new com.example.kahoot.dao.QuestionDAO();
            java.util.List<com.example.kahoot.model.Question> questions = questionDAO.findByQuizId(selectedQuiz.getQuizId());
            
            if (questions.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cảnh báo");
                alert.setHeaderText(null);
                alert.setContentText("Quiz này chưa có câu hỏi nào! Vui lòng thêm câu hỏi trước khi bắt đầu game.");
                alert.showAndWait();
                return;
            }
            
            // Mở màn hình game session
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/views/game_session.fxml"));
                javafx.scene.Parent root = loader.load();
                
                GameSessionController controller = loader.getController();
                if (controller != null) {
                    controller.startGame(selectedQuiz);
                }
                
                // Tạo cửa sổ mới
                javafx.stage.Stage newStage = new javafx.stage.Stage();
                javafx.scene.Scene scene = new javafx.scene.Scene(root, 600, 400);
                newStage.setTitle("Game Session - " + selectedQuiz.getTitle());
                newStage.setScene(scene);
                newStage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
                newStage.show();
            } catch (java.io.IOException e) {
                System.err.println("Lỗi khi mở màn hình game session: " + e.getMessage());
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Không thể mở màn hình game session");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi kiểm tra quiz: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể kiểm tra quiz");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Xử lý khi nhấn nút Đăng xuất.
     */
    @FXML
    public void handleLogoutButtonAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 400, 350);
            stage.setTitle("Kahoot Desktop - Đăng nhập");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi đăng xuất: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refresh danh sách quiz (được gọi từ CreateQuizController sau khi tạo quiz mới).
     */
    public void refreshQuizList() {
        loadQuizzes();
    }
}

