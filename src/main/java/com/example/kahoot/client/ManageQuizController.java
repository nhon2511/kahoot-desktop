package com.example.kahoot.client;

import com.example.kahoot.dao.OptionDAO;
import com.example.kahoot.dao.QuestionDAO;
import com.example.kahoot.model.Option;
import com.example.kahoot.model.Question;
import com.example.kahoot.model.Quiz;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ManageQuizController implements Initializable {

    @FXML private Label quizTitleLabel;
    @FXML private ListView<Question> questionsListView;
    @FXML private TextArea questionTextArea;
    @FXML private TextField timeLimitField;
    @FXML private TextField pointValueField;
    @FXML private ListView<Option> optionsListView;
    @FXML private TextField optionTextField;
    @FXML private CheckBox isCorrectCheckBox;
    @FXML private Label messageLabel;

    private Quiz currentQuiz;
    private Question currentQuestion;
    private Option currentOption;
    private QuestionDAO questionDAO;
    private OptionDAO optionDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionDAO = new QuestionDAO();
        optionDAO = new OptionDAO();
        
        // Setup questions list view
        questionsListView.setCellFactory(param -> new ListCell<Question>() {
            @Override
            protected void updateItem(Question question, boolean empty) {
                super.updateItem(question, empty);
                if (empty || question == null) {
                    setText(null);
                } else {
                    setText("Câu " + question.getQuestionOrder() + ": " + 
                            (question.getQuestionText().length() > 50 ? 
                             question.getQuestionText().substring(0, 50) + "..." : 
                             question.getQuestionText()));
                }
            }
        });
        
        questionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadQuestionDetails(newVal);
            }
        });
        
        // Setup options list view
        optionsListView.setCellFactory(param -> new ListCell<Option>() {
            @Override
            protected void updateItem(Option option, boolean empty) {
                super.updateItem(option, empty);
                if (empty || option == null) {
                    setText(null);
                } else {
                    String marker = option.isCorrect() ? "✓ " : "  ";
                    setText(marker + option.getOptionText());
                }
            }
        });
        
        optionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadOptionDetails(newVal);
            }
        });
    }

    /**
     * Thiết lập quiz hiện tại.
     */
    public void setQuiz(Quiz quiz) {
        this.currentQuiz = quiz;
        if (quiz != null) {
            quizTitleLabel.setText("Quản lý Quiz: " + quiz.getTitle());
            loadQuestions();
        }
    }

    /**
     * Load danh sách questions của quiz.
     */
    private void loadQuestions() {
        if (currentQuiz == null) return;

        try {
            List<Question> questions = questionDAO.findByQuizId(currentQuiz.getQuizId());
            questionsListView.getItems().clear();
            questionsListView.getItems().addAll(questions);
        } catch (SQLException e) {
            showMessage("Lỗi khi load câu hỏi: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Load chi tiết question vào form.
     */
    private void loadQuestionDetails(Question question) {
        this.currentQuestion = question;
        questionTextArea.setText(question.getQuestionText());
        timeLimitField.setText(String.valueOf(question.getTimeLimit()));
        pointValueField.setText(String.valueOf(question.getPointValue()));
        
        // Load options của question này
        loadOptions(question.getQuestionId());
    }

    /**
     * Load danh sách options của một question.
     */
    private void loadOptions(int questionId) {
        try {
            List<Option> options = optionDAO.findByQuestionId(questionId);
            optionsListView.getItems().clear();
            optionsListView.getItems().addAll(options);
        } catch (SQLException e) {
            showMessage("Lỗi khi load lựa chọn: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Load chi tiết option vào form.
     */
    private void loadOptionDetails(Option option) {
        this.currentOption = option;
        optionTextField.setText(option.getOptionText());
        isCorrectCheckBox.setSelected(option.isCorrect());
    }

    /**
     * Xử lý khi nhấn nút Thêm Câu hỏi.
     */
    @FXML
    public void handleAddQuestionAction(ActionEvent event) {
        clearQuestionForm();
        currentQuestion = null;
    }

    /**
     * Xử lý khi nhấn nút Lưu Câu hỏi.
     */
    @FXML
    public void handleSaveQuestionAction(ActionEvent event) {
        if (currentQuiz == null) {
            showMessage("Chưa chọn quiz!", true);
            return;
        }

        String questionText = questionTextArea.getText().trim();
        if (questionText.isEmpty()) {
            showMessage("Vui lòng nhập nội dung câu hỏi!", true);
            return;
        }

        try {
            int timeLimit = Integer.parseInt(timeLimitField.getText());
            int pointValue = Integer.parseInt(pointValueField.getText());

            if (currentQuestion == null) {
                // Tạo mới
                int nextOrder = questionsListView.getItems().size() + 1;
                Question newQuestion = new Question(
                        currentQuiz.getQuizId(),
                        questionText,
                        nextOrder,
                        timeLimit,
                        pointValue
                );
                questionDAO.saveQuestion(newQuestion);
                showMessage("Đã thêm câu hỏi thành công!", false);
            } else {
                // Cập nhật
                currentQuestion.setQuestionText(questionText);
                currentQuestion.setTimeLimit(timeLimit);
                currentQuestion.setPointValue(pointValue);
                questionDAO.updateQuestion(currentQuestion);
                showMessage("Đã cập nhật câu hỏi thành công!", false);
            }

            loadQuestions();
            clearQuestionForm();
        } catch (NumberFormatException e) {
            showMessage("Thời gian và điểm số phải là số!", true);
        } catch (SQLException e) {
            showMessage("Lỗi khi lưu câu hỏi: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Xử lý khi nhấn nút Xóa Câu hỏi.
     */
    @FXML
    public void handleDeleteQuestionAction(ActionEvent event) {
        if (currentQuestion == null) {
            showMessage("Chưa chọn câu hỏi để xóa!", true);
            return;
        }

        try {
            questionDAO.deleteQuestion(currentQuestion.getQuestionId());
            showMessage("Đã xóa câu hỏi thành công!", false);
            loadQuestions();
            clearQuestionForm();
        } catch (SQLException e) {
            showMessage("Lỗi khi xóa câu hỏi: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Xử lý khi nhấn nút Thêm Lựa chọn.
     */
    @FXML
    public void handleAddOptionAction(ActionEvent event) {
        if (currentQuestion == null) {
            showMessage("Vui lòng chọn hoặc tạo câu hỏi trước!", true);
            return;
        }
        clearOptionForm();
        currentOption = null;
    }

    /**
     * Xử lý khi nhấn nút Lưu Lựa chọn.
     */
    @FXML
    public void handleSaveOptionAction(ActionEvent event) {
        if (currentQuestion == null) {
            showMessage("Vui lòng chọn hoặc tạo câu hỏi trước!", true);
            return;
        }

        String optionText = optionTextField.getText().trim();
        if (optionText.isEmpty()) {
            showMessage("Vui lòng nhập nội dung lựa chọn!", true);
            return;
        }

        try {
            if (currentOption == null) {
                // Tạo mới
                Option newOption = new Option(
                        currentQuestion.getQuestionId(),
                        optionText,
                        isCorrectCheckBox.isSelected()
                );
                optionDAO.saveOption(newOption);
                showMessage("Đã thêm lựa chọn thành công!", false);
            } else {
                // Cập nhật
                currentOption.setOptionText(optionText);
                currentOption.setCorrect(isCorrectCheckBox.isSelected());
                optionDAO.updateOption(currentOption);
                showMessage("Đã cập nhật lựa chọn thành công!", false);
            }

            loadOptions(currentQuestion.getQuestionId());
            clearOptionForm();
        } catch (SQLException e) {
            showMessage("Lỗi khi lưu lựa chọn: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Xử lý khi nhấn nút Xóa Lựa chọn.
     */
    @FXML
    public void handleDeleteOptionAction(ActionEvent event) {
        if (currentOption == null) {
            showMessage("Chưa chọn lựa chọn để xóa!", true);
            return;
        }

        try {
            optionDAO.deleteOption(currentOption.getOptionId());
            showMessage("Đã xóa lựa chọn thành công!", false);
            loadOptions(currentQuestion.getQuestionId());
            clearOptionForm();
        } catch (SQLException e) {
            showMessage("Lỗi khi xóa lựa chọn: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Xử lý khi nhấn nút Quay lại.
     */
    @FXML
    public void handleBackButtonAction(ActionEvent event) {
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        javafx.stage.Stage stage = (javafx.stage.Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Clear form câu hỏi.
     */
    private void clearQuestionForm() {
        questionTextArea.clear();
        timeLimitField.setText("20");
        pointValueField.setText("1000");
        optionsListView.getItems().clear();
        currentQuestion = null;
    }

    /**
     * Clear form lựa chọn.
     */
    private void clearOptionForm() {
        optionTextField.clear();
        isCorrectCheckBox.setSelected(false);
        currentOption = null;
    }

    /**
     * Hiển thị thông báo.
     */
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}

