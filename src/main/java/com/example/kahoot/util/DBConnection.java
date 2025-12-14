package com.example.kahoot.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Defaults (used only if environment variables are not provided)
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/kahoot";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = ""; // empty by default

    /**
     * Thiết lập và trả về đối tượng Connection tới CSDL.
     * @return Connection tới cơ sở dữ liệu Kahoot.
     * @throws SQLException nếu kết nối thất bại.
     */
    public static Connection getConnection() throws SQLException {
        // Use environment variables when available to avoid committing secrets
        String url = System.getenv().getOrDefault("DB_URL", DEFAULT_URL);
        String user = System.getenv().getOrDefault("DB_USER", DEFAULT_USER);
        String password = System.getenv().getOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);

        System.out.println("DBConnection: Connecting to: " + url + " as user=" + user);

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("DBConnection: Connection successful");
            return conn;
        } catch (SQLException e) {
            System.err.println("DBConnection: Connection failed: " + e.getMessage());
            System.err.println("DBConnection: SQL State: " + e.getSQLState());
            throw e;
        }
    }
}