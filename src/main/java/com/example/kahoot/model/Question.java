package com.example.kahoot.model;

public class Question {
    private int questionId;
    private int quizId;
    private String questionText;
    private int questionOrder;
    private int timeLimit;  // Thời gian trả lời (giây)
    private int pointValue; // Điểm số

    // Constructor để TẠO MỚI Question
    public Question(int quizId, String questionText, int questionOrder, int timeLimit, int pointValue) {
        this.quizId = quizId;
        this.questionText = questionText;
        this.questionOrder = questionOrder;
        this.timeLimit = timeLimit;
        this.pointValue = pointValue;
    }

    // Constructor để TẢI Question TỪ DB
    public Question(int questionId, int quizId, String questionText, int questionOrder, int timeLimit, int pointValue) {
        this.questionId = questionId;
        this.quizId = quizId;
        this.questionText = questionText;
        this.questionOrder = questionOrder;
        this.timeLimit = timeLimit;
        this.pointValue = pointValue;
    }

    // Getters
    public int getQuestionId() { return questionId; }
    public int getQuizId() { return quizId; }
    public String getQuestionText() { return questionText; }
    public int getQuestionOrder() { return questionOrder; }
    public int getTimeLimit() { return timeLimit; }
    public int getPointValue() { return pointValue; }

    // Setters
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setQuestionOrder(int questionOrder) { this.questionOrder = questionOrder; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    public void setPointValue(int pointValue) { this.pointValue = pointValue; }
}





