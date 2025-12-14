package com.example.kahoot.dao;

import com.example.kahoot.model.Question;
import com.example.kahoot.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {

    private static final String INSERT_QUESTION =
            "INSERT INTO Questions (quiz_id, question_text, question_order, time_limit, point_value) VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_BY_QUIZ_ID =
            "SELECT question_id, quiz_id, question_text, question_order, time_limit, point_value FROM Questions WHERE quiz_id = ? ORDER BY question_order";
    private static final String FIND_BY_QUESTION_ID =
            "SELECT question_id, quiz_id, question_text, question_order, time_limit, point_value FROM Questions WHERE question_id = ?";
    private static final String DELETE_QUESTION =
            "DELETE FROM Questions WHERE question_id = ?";
    private static final String UPDATE_QUESTION =
            "UPDATE Questions SET question_text = ?, question_order = ?, time_limit = ?, point_value = ? WHERE question_id = ?";

    /**
     * Lưu Question mới vào database.
     */
    public void saveQuestion(Question question) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_QUESTION, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, question.getQuizId());
            ps.setString(2, question.getQuestionText());
            ps.setInt(3, question.getQuestionOrder());
            ps.setInt(4, question.getTimeLimit());
            ps.setInt(5, question.getPointValue());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating question failed, no rows affected.");
            }

            // Lấy ID tự động tạo ra từ CSDL
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    question.setQuestionId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating question failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Tìm tất cả Question của một Quiz.
     */
    public List<Question> findByQuizId(int quizId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_QUIZ_ID)) {
            
            ps.setInt(1, quizId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question question = new Question(
                            rs.getInt("question_id"),
                            rs.getInt("quiz_id"),
                            rs.getString("question_text"),
                            rs.getInt("question_order"),
                            rs.getInt("time_limit"),
                            rs.getInt("point_value")
                    );
                    questions.add(question);
                }
            }
        }
        
        return questions;
    }

    /**
     * Tìm Question theo ID.
     */
    public Question findByQuestionId(int questionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_QUESTION_ID)) {
            
            ps.setInt(1, questionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Question(
                            rs.getInt("question_id"),
                            rs.getInt("quiz_id"),
                            rs.getString("question_text"),
                            rs.getInt("question_order"),
                            rs.getInt("time_limit"),
                            rs.getInt("point_value")
                    );
                }
            }
        }
        
        return null;
    }

    /**
     * Xóa Question.
     */
    public boolean deleteQuestion(int questionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_QUESTION)) {
            
            ps.setInt(1, questionId);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Cập nhật Question.
     */
    public boolean updateQuestion(Question question) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_QUESTION)) {
            
            ps.setString(1, question.getQuestionText());
            ps.setInt(2, question.getQuestionOrder());
            ps.setInt(3, question.getTimeLimit());
            ps.setInt(4, question.getPointValue());
            ps.setInt(5, question.getQuestionId());
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }
}





