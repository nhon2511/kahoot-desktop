package com.example.kahoot.dao;

import com.example.kahoot.model.User;
import com.example.kahoot.util.DBConnection;
import java.sql.*;

public class UserDAO {

    // SQL Queries
    private static final String FIND_BY_USERNAME =
            "SELECT user_id, username, password_hash, email FROM Users WHERE username = ?";
    private static final String INSERT_USER =
            "INSERT INTO Users (username, password_hash, email) VALUES (?, ?, ?)";

    // Phương thức 1: Đăng ký User mới
    public void saveUser(User user) throws SQLException {
        // Dùng try-with-resources để tự động đóng Connection và PreparedStatement
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            // Lấy ID tự động tạo ra từ CSDL và cập nhật lại cho đối tượng User
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
    }

    // Phương thức 2: Tìm kiếm User theo Username (để Đăng nhập)
    public User findByUsername(String username) throws SQLException {
        System.out.println("UserDAO: Đang kết nối database...");
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("UserDAO: Kết nối database thành công!");
            System.out.println("UserDAO: Database name: " + conn.getCatalog());
            
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_USERNAME)) {
                ps.setString(1, username);
                System.out.println("UserDAO: Đang thực thi query: " + FIND_BY_USERNAME + " với username = " + username);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        User user = new User(
                                rs.getInt("user_id"),
                                rs.getString("username"),
                                rs.getString("password_hash"),
                                rs.getString("email")
                        );
                        System.out.println("UserDAO: Tìm thấy user - ID: " + user.getUserId() + ", Username: " + user.getUsername());
                        return user;
                    } else {
                        System.out.println("UserDAO: Không tìm thấy user với username: " + username);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO: Lỗi SQL: " + e.getMessage());
            System.err.println("UserDAO: SQL State: " + e.getSQLState());
            System.err.println("UserDAO: Error Code: " + e.getErrorCode());
            throw e;
        }
        return null;
    }
}