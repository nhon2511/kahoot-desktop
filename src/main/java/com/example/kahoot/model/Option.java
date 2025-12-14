package com.example.kahoot.model;

public class Option {
    private int optionId;
    private int questionId;
    private String optionText;
    private boolean isCorrect;

    // Constructor để TẠO MỚI Option
    public Option(int questionId, String optionText, boolean isCorrect) {
        this.questionId = questionId;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
    }

    // Constructor để TẢI Option TỪ DB
    public Option(int optionId, int questionId, String optionText, boolean isCorrect) {
        this.optionId = optionId;
        this.questionId = questionId;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
    }

    // Getters
    public int getOptionId() { return optionId; }
    public int getQuestionId() { return questionId; }
    public String getOptionText() { return optionText; }
    public boolean isCorrect() { return isCorrect; }

    // Setters
    public void setOptionId(int optionId) { this.optionId = optionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}





