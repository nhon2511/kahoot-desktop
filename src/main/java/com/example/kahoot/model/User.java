package com.example.kahoot.model;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String email;

    // 1. Constructor để TẠO MỚI User (ID sẽ được DB tự động tạo)
    public User(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    // 2. Constructor để TẢI User TỪ DB (đã có ID)
    public User(int userId, String username, String passwordHash, String email) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    // Getters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }

    // Setters (Chỉ cần thiết cho các thuộc tính có thể thay đổi sau này)
    public void setUserId(int userId) { this.userId = userId; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    // ...
}