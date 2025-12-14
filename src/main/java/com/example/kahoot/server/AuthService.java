package com.example.kahoot.server;

import com.example.kahoot.dao.UserDAO;
import com.example.kahoot.model.User;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Service xác thực cho server.
 */
public class AuthService {
    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public User authenticate(String username, String plainPassword) {
        try {
            User user = userDAO.findByUsername(username);
            if (user != null && Objects.equals(plainPassword, user.getPasswordHash())) {
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi xác thực: " + e.getMessage());
        }
        return null;
    }

    public User registerUser(String username, String plainPassword, String email) throws Exception {
        String hashedPassword = plainPassword; // Tạm thời, không an toàn

        User newUser = new User(username, hashedPassword, email);

        if (userDAO.findByUsername(username) != null) {
            throw new Exception("Tên đăng nhập đã tồn tại.");
        }

        try {
            userDAO.saveUser(newUser);
            return newUser;
        } catch (SQLException e) {
            throw new Exception("Lỗi CSDL khi đăng ký: " + e.getMessage());
        }
    }
}





