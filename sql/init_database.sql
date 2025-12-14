-- Đảm bảo sử dụng InnoDB để hỗ trợ khóa ngoại (FOREIGN KEY)
SET default_storage_engine=InnoDB; 

-- Bắt buộc phải xóa các bảng con trước các bảng cha do ràng buộc khóa ngoại
DROP TABLE IF EXISTS Results;
DROP TABLE IF EXISTS Options;
DROP TABLE IF EXISTS Questions;
DROP TABLE IF EXISTS GameSessions;
DROP TABLE IF EXISTS Quizzes;
DROP TABLE IF EXISTS Users;

-- =========================================================
-- 1. Bảng Users (Người dùng) - Bảng Cha
-- =========================================================
CREATE TABLE Users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE
);

-- =========================================================
-- 2. Bảng Quizzes (Bài trắc nghiệm)
-- =========================================================
CREATE TABLE Quizzes (
    quiz_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    host_id INT NOT NULL,
    access_code VARCHAR(10) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (host_id) REFERENCES Users(user_id) 
        ON DELETE CASCADE -- Nếu User bị xóa, Quiz của họ bị xóa
);

-- =========================================================
-- 3. Bảng Questions (Câu hỏi)
-- =========================================================
CREATE TABLE Questions (
    question_id INT PRIMARY KEY AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    question_text TEXT NOT NULL,
    question_order INT NOT NULL,
    time_limit INT DEFAULT 20, 
    point_value INT DEFAULT 1000,
    
    FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id)
        ON DELETE CASCADE -- Nếu Quiz bị xóa, Question bị xóa
);

-- =========================================================
-- 4. Bảng Options (Các tùy chọn trả lời)
-- =========================================================
CREATE TABLE Options (
    option_id INT PRIMARY KEY AUTO_INCREMENT,
    question_id INT NOT NULL,
    option_text VARCHAR(255) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    
    FOREIGN KEY (question_id) REFERENCES Questions(question_id)
        ON DELETE CASCADE -- Nếu Question bị xóa, Options bị xóa
);

-- =========================================================
-- 5. Bảng GameSessions (Phiên chơi game)
-- =========================================================
CREATE TABLE GameSessions (
    session_id INT PRIMARY KEY AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    pin_code VARCHAR(10) UNIQUE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    
    FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id)
        ON DELETE CASCADE -- Nếu Quiz bị xóa, Game Sessions cũ bị xóa
);

-- =========================================================
-- 6. Bảng Results (Kết quả)
-- =========================================================
CREATE TABLE Results (
    result_id INT PRIMARY KEY AUTO_INCREMENT,
    session_id INT NOT NULL,
    player_name VARCHAR(50) NOT NULL,
    question_id INT NOT NULL,
    selected_option_id INT, 
    points_earned INT DEFAULT 0,
    answer_time INT, 
    
    FOREIGN KEY (session_id) REFERENCES GameSessions(session_id)
        ON DELETE CASCADE, -- Nếu Session bị xóa, Results bị xóa
    FOREIGN KEY (question_id) REFERENCES Questions(question_id),
    -- Lưu ý: selected_option_id có thể là NULL (nếu người chơi không trả lời)
    FOREIGN KEY (selected_option_id) REFERENCES Options(option_id)




