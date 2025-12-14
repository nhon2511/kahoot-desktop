package com.example.kahoot.client;

import com.example.kahoot.dao.UserDAO;
import com.example.kahoot.model.User;
import java.sql.SQLException;
// Cần thêm thư viện bảo mật để hash mật khẩu, ví dụ: BCrypt (thêm vào pom.xml sau)
// Tạm thời dùng hàm kiểm tra đơn giản
import java.util.Objects;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Xác thực người dùng (đăng nhập).
     * @param username Tên đăng nhập.
     * @param plainPassword Mật khẩu chưa mã hóa người dùng nhập.
     * @return Đối tượng User nếu đăng nhập thành công, nếu không thì null.
     */
    public User authenticate(String username, String plainPassword) {
        try {
            System.out.println("Đang tìm user: " + username);
            User user = userDAO.findByUsername(username);

            if (user != null) {
                System.out.println("Tìm thấy user: " + user.getUsername());
                System.out.println("Password nhập vào: [" + plainPassword + "]");
                System.out.println("Password trong DB: [" + user.getPasswordHash() + "]");
                
                // TRONG THỰC TẾ: Sử dụng BCrypt.checkpw(plainPassword, user.getPasswordHash())
                // Tạm thời: Chỉ so sánh chuỗi (GIẢI PHÁP TẠM THỜI VÀ KHÔNG AN TOÀN)
                if (Objects.equals(plainPassword, user.getPasswordHash())) {
                    System.out.println("Mật khẩu khớp! Đăng nhập thành công.");
                    return user; // Đăng nhập thành công
                } else {
                    System.out.println("Mật khẩu KHÔNG khớp!");
                }
            } else {
                System.out.println("Không tìm thấy user với username: " + username);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi xác thực: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi xác thực: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Đăng nhập thất bại
    }

    /**
     * Đăng ký người dùng mới.
     * @param username Tên đăng nhập.
     * @param plainPassword Mật khẩu chưa mã hóa.
     * @param email Email.
     * @return User mới đã được tạo nếu thành công.
     */
    public User registerUser(String username, String plainPassword, String email) throws Exception {
        // TRONG THỰC TẾ: Phải HASH mật khẩu trước khi lưu vào DB
        // String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        String hashedPassword = plainPassword; // Tạm thời, không an toàn

        User newUser = new User(username, hashedPassword, email);

        // Kiểm tra logic nghiệp vụ: Tên đăng nhập đã tồn tại chưa?
        if (userDAO.findByUsername(username) != null) {
            throw new Exception("Tên đăng nhập đã tồn tại.");
        }

        try {
            userDAO.saveUser(newUser); // Lưu vào DB
            return newUser;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Lỗi CSDL khi đăng ký: " + e.getMessage());
        }
    }
}