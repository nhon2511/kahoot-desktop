package com.example.kahoot.server;

/**
 * Trạng thái của game session.
 */
public enum GameState {
    WAITING,    // Đang chờ players tham gia
    QUESTION,   // Đang hiển thị câu hỏi
    RESULT,     // Đang hiển thị kết quả
    FINISHED    // Game đã kết thúc
}


