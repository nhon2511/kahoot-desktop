-- Script thêm dữ liệu test cho Kahoot Desktop
-- Chạy script này sau khi đã chạy init_database.sql

USE kahoot;

-- Thêm một số user test
-- Lưu ý: Mật khẩu đang được lưu dạng plain text (tạm thời, không an toàn)
-- Trong thực tế cần hash mật khẩu bằng BCrypt hoặc thuật toán tương tự

INSERT INTO Users (username, password_hash, email) VALUES 
('admin', 'admin123', 'admin@kahoot.com'),
('testuser', 'test123', 'test@kahoot.com'),
('host1', 'host123', 'host1@kahoot.com'),
('player1', 'player123', 'player1@kahoot.com');

-- Kiểm tra dữ liệu đã được thêm
SELECT * FROM Users;





