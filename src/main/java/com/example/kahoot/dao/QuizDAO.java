package com.example.kahoot.dao;

import com.example.kahoot.model.Quiz;
import com.example.kahoot.util.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuizDAO {

    // SQL Queries
    private static final String INSERT_QUIZ =
            "INSERT INTO Quizzes (title, host_id, access_code) VALUES (?, ?, ?)";
    private static final String FIND_BY_HOST_ID =
            "SELECT quiz_id, title, host_id, access_code, created_at FROM Quizzes WHERE host_id = ? ORDER BY created_at DESC";
    private static final String FIND_BY_QUIZ_ID =
            "SELECT quiz_id, title, host_id, access_code, created_at FROM Quizzes WHERE quiz_id = ?";
    private static final String DELETE_QUIZ =
            "DELETE FROM Quizzes WHERE quiz_id = ? AND host_id = ?";
    private static final String UPDATE_QUIZ =
            "UPDATE Quizzes SET title = ?, access_code = ? WHERE quiz_id = ? AND host_id = ?";

    /**
     * Lưu Quiz mới vào database.
     */
    public void saveQuiz(Quiz quiz) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_QUIZ, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, quiz.getTitle());
            ps.setInt(2, quiz.getHostId());
            ps.setString(3, quiz.getAccessCode());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating quiz failed, no rows affected.");
            }

            // Lấy ID tự động tạo ra từ CSDL
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    quiz.setQuizId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating quiz failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Tìm tất cả Quiz của một Host.
     */
    public List<Quiz> findByHostId(int hostId) throws SQLException {
        System.out.println("🔍 QuizDAO.findByHostId() được gọi với hostId: " + hostId);
        List<Quiz> quizzes = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                throw new SQLException("Không thể kết nối đến database!");
            }
            System.out.println("✓ Đã kết nối database thành công");
            
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_HOST_ID)) {
                System.out.println("📝 SQL Query: " + FIND_BY_HOST_ID);
                ps.setInt(1, hostId);
                System.out.println("📝 Parameter hostId: " + hostId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("✓ Đã thực thi query");
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        Timestamp timestamp = rs.getTimestamp("created_at");
                        LocalDateTime createdAt = timestamp != null ? timestamp.toLocalDateTime() : null;
                        
                        Quiz quiz = new Quiz(
                                rs.getInt("quiz_id"),
                                rs.getString("title"),
                                rs.getInt("host_id"),
                                rs.getString("access_code"),
                                createdAt
                        );
                        quizzes.add(quiz);
                        System.out.println("  ✓ Đã load quiz: ID=" + quiz.getQuizId() + ", Title=" + quiz.getTitle() + ", HostID=" + quiz.getHostId());
                    }
                    System.out.println("✓ Tổng cộng tìm thấy " + count + " quiz");
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ SQLException trong findByHostId: " + e.getMessage());
            System.err.println("  SQL State: " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            throw e;
        }
        
        return quizzes;
    }

    /**
     * Tìm Quiz theo ID.
     */
    public Quiz findByQuizId(int quizId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_QUIZ_ID)) {
            
            ps.setInt(1, quizId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = timestamp != null ? timestamp.toLocalDateTime() : null;
                    
                    return new Quiz(
                            rs.getInt("quiz_id"),
                            rs.getString("title"),
                            rs.getInt("host_id"),
                            rs.getString("access_code"),
                            createdAt
                    );
                }
            }
        }
        
        return null;
    }

    /**
     * Xóa Quiz (chỉ host của quiz mới có thể xóa).
     */
    public boolean deleteQuiz(int quizId, int hostId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_QUIZ)) {
            
            ps.setInt(1, quizId);
            ps.setInt(2, hostId);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Cập nhật thông tin Quiz.
     */
    public boolean updateQuiz(Quiz quiz) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_QUIZ)) {
            
            ps.setString(1, quiz.getTitle());
            ps.setString(2, quiz.getAccessCode());
            ps.setInt(3, quiz.getQuizId());
            ps.setInt(4, quiz.getHostId());
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Tạo mã truy cập ngẫu nhiên cho Quiz.
     */
    public static String generateAccessCode() {
        // Tạo mã 6 chữ số ngẫu nhiên
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}





