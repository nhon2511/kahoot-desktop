package com.example.kahoot.dao;

import com.example.kahoot.model.GameSession;
import com.example.kahoot.util.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;

public class GameSessionDAO {

    private static final String INSERT_SESSION =
            "INSERT INTO GameSessions (quiz_id, pin_code, start_time) VALUES (?, ?, ?)";
    private static final String FIND_BY_PIN_CODE =
            "SELECT session_id, quiz_id, pin_code, start_time, end_time FROM GameSessions WHERE pin_code = ? AND end_time IS NULL";
    private static final String FIND_BY_QUIZ_ID =
            "SELECT session_id, quiz_id, pin_code, start_time, end_time FROM GameSessions WHERE quiz_id = ? ORDER BY start_time DESC";
    private static final String UPDATE_END_TIME =
            "UPDATE GameSessions SET end_time = ? WHERE session_id = ?";

    /**
     * Lưu GameSession mới vào database.
     */
    public void saveSession(GameSession session) throws SQLException {
        System.out.println("GameSessionDAO.saveSession: Bắt đầu lưu session");
        System.out.println("  Quiz ID: " + session.getQuizId());
        System.out.println("  PIN Code: " + session.getPinCode());
        System.out.println("  Start Time: " + session.getStartTime());
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SESSION, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, session.getQuizId());
            ps.setString(2, session.getPinCode());
            ps.setTimestamp(3, Timestamp.valueOf(session.getStartTime()));

            System.out.println("GameSessionDAO: Đang thực thi INSERT...");
            int affectedRows = ps.executeUpdate();
            System.out.println("GameSessionDAO: Affected rows: " + affectedRows);

            if (affectedRows == 0) {
                throw new SQLException("Creating game session failed, no rows affected.");
            }

            // Lấy ID tự động tạo ra từ CSDL
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int sessionId = generatedKeys.getInt(1);
                    session.setSessionId(sessionId);
                    System.out.println("GameSessionDAO: Đã lấy được Session ID: " + sessionId);
                } else {
                    throw new SQLException("Creating game session failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println("GameSessionDAO.saveSession: SQLException xảy ra!");
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  SQL State: " + e.getSQLState());
            System.err.println("  Message: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Tìm GameSession đang hoạt động theo PIN code.
     */
    public GameSession findByPinCode(String pinCode) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_PIN_CODE)) {
            
            ps.setString(1, pinCode);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp startTimestamp = rs.getTimestamp("start_time");
                    Timestamp endTimestamp = rs.getTimestamp("end_time");
                    
                    return new GameSession(
                            rs.getInt("session_id"),
                            rs.getInt("quiz_id"),
                            rs.getString("pin_code"),
                            startTimestamp != null ? startTimestamp.toLocalDateTime() : null,
                            endTimestamp != null ? endTimestamp.toLocalDateTime() : null
                    );
                }
            }
        }
        
        return null;
    }

    /**
     * Tìm tất cả GameSession của một Quiz.
     */
    public java.util.List<GameSession> findByQuizId(int quizId) throws SQLException {
        java.util.List<GameSession> sessions = new java.util.ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_QUIZ_ID)) {
            
            ps.setInt(1, quizId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp startTimestamp = rs.getTimestamp("start_time");
                    Timestamp endTimestamp = rs.getTimestamp("end_time");
                    
                    GameSession session = new GameSession(
                            rs.getInt("session_id"),
                            rs.getInt("quiz_id"),
                            rs.getString("pin_code"),
                            startTimestamp != null ? startTimestamp.toLocalDateTime() : null,
                            endTimestamp != null ? endTimestamp.toLocalDateTime() : null
                    );
                    sessions.add(session);
                }
            }
        }
        
        return sessions;
    }

    /**
     * Kết thúc GameSession (set end_time).
     */
    public boolean endSession(int sessionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_END_TIME)) {
            
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, sessionId);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Tạo mã PIN ngẫu nhiên cho game session.
     */
    public static String generatePinCode() {
        // Tạo mã 6 chữ số ngẫu nhiên
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}





