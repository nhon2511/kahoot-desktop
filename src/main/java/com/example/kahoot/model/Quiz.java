package com.example.kahoot.model;

import java.time.LocalDateTime;

public class Quiz {
    private int quizId;
    private String title;
    private int hostId;         // Khóa ngoại: Host tạo Quiz này
    private String accessCode;  // Mã truy cập Quiz (có thể dùng trong GameSessions)
    private LocalDateTime createdAt;

    // Constructor để TẠO MỚI Quiz (ID sẽ được DB tự động tạo)
    public Quiz(String title, int hostId, String accessCode) {
        this.title = title;
        this.hostId = hostId;
        this.accessCode = accessCode;
        this.createdAt = LocalDateTime.now();
    }

    // Constructor để TẢI Quiz TỪ DB (đã có ID và createdAt)
    public Quiz(int quizId, String title, int hostId, String accessCode, LocalDateTime createdAt) {
        this.quizId = quizId;
        this.title = title;
        this.hostId = hostId;
        this.accessCode = accessCode;
        this.createdAt = createdAt;
    }

    // Getters
    public int getQuizId() { return quizId; }
    public String getTitle() { return title; }
    public int getHostId() { return hostId; }
    public String getAccessCode() { return accessCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setQuizId(int quizId) { this.quizId = quizId; }
    public void setTitle(String title) { this.title = title; }
    public void setHostId(int hostId) { this.hostId = hostId; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}