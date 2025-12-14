package com.example.kahoot.dao;

import com.example.kahoot.model.Option;
import com.example.kahoot.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OptionDAO {

    private static final String INSERT_OPTION =
            "INSERT INTO Options (question_id, option_text, is_correct) VALUES (?, ?, ?)";
    private static final String FIND_BY_QUESTION_ID =
            "SELECT option_id, question_id, option_text, is_correct FROM Options WHERE question_id = ?";
    private static final String DELETE_OPTION =
            "DELETE FROM Options WHERE option_id = ?";
    private static final String DELETE_BY_QUESTION_ID =
            "DELETE FROM Options WHERE question_id = ?";
    private static final String UPDATE_OPTION =
            "UPDATE Options SET option_text = ?, is_correct = ? WHERE option_id = ?";

    /**
     * Lưu Option mới vào database.
     */
    public void saveOption(Option option) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_OPTION, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, option.getQuestionId());
            ps.setString(2, option.getOptionText());
            ps.setBoolean(3, option.isCorrect());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating option failed, no rows affected.");
            }

            // Lấy ID tự động tạo ra từ CSDL
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    option.setOptionId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating option failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Tìm tất cả Option của một Question.
     */
    public List<Option> findByQuestionId(int questionId) throws SQLException {
        List<Option> options = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_QUESTION_ID)) {
            
            ps.setInt(1, questionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Option option = new Option(
                            rs.getInt("option_id"),
                            rs.getInt("question_id"),
                            rs.getString("option_text"),
                            rs.getBoolean("is_correct")
                    );
                    options.add(option);
                }
            }
        }
        
        return options;
    }

    /**
     * Xóa Option.
     */
    public boolean deleteOption(int optionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_OPTION)) {
            
            ps.setInt(1, optionId);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Xóa tất cả Option của một Question.
     */
    public boolean deleteByQuestionId(int questionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_QUESTION_ID)) {
            
            ps.setInt(1, questionId);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows >= 0; // Có thể không có option nào để xóa
        }
    }

    /**
     * Cập nhật Option.
     */
    public boolean updateOption(Option option) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_OPTION)) {
            
            ps.setString(1, option.getOptionText());
            ps.setBoolean(2, option.isCorrect());
            ps.setInt(3, option.getOptionId());
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }
}





