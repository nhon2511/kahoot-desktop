package com.example.kahoot.model;

import java.time.LocalDateTime;

public class GameSession {
    private int sessionId;
    private int quizId;
    private String pinCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Constructor để TẠO MỚI GameSession
    public GameSession(int quizId, String pinCode) {
        this.quizId = quizId;
        this.pinCode = pinCode;
        this.startTime = LocalDateTime.now();
    }

    // Constructor để TẢI GameSession TỪ DB
    public GameSession(int sessionId, int quizId, String pinCode, LocalDateTime startTime, LocalDateTime endTime) {
        this.sessionId = sessionId;
        this.quizId = quizId;
        this.pinCode = pinCode;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters
    public int getSessionId() { return sessionId; }
    public int getQuizId() { return quizId; }
    public String getPinCode() { return pinCode; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    // Setters
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}





