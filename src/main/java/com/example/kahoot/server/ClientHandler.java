package com.example.kahoot.server;

import com.example.kahoot.dao.*;
import com.example.kahoot.model.*;
import java.io.*;
import java.net.Socket;
import java.sql.SQLException;

/**
 * Xử lý kết nối từ một client.
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private KahootServer server;
    private volatile boolean isConnected; // volatile để đảm bảo thread-safe
    private String clientType; // "HOST" hoặc "PLAYER"
    private int sessionId;

    public ClientHandler(Socket socket, KahootServer server) {
        this.socket = socket;
        this.server = server;
        this.isConnected = true;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String clientInfo = "Client " + socket.getRemoteSocketAddress();
            System.out.println("✓ Đã khởi tạo handler cho " + clientInfo);
            System.out.println("  Đang chờ message từ client...");

            String message;
            while (isConnected && (message = reader.readLine()) != null) {
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("📨 Nhận được message từ " + clientInfo + ":");
                System.out.println("   " + message);
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                handleMessage(message);
            }
        } catch (IOException e) {
            if (isConnected) {
                System.err.println("✗ Lỗi khi xử lý client " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
            }
        } finally {
            closeConnection();
        }
    }

    /**
     * Xử lý message từ client.
     * Format: COMMAND|param1|param2|...
     */
    private void handleMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length == 0) return;

        String command = parts[0];

        try {
            switch (command) {
                case "LOGIN":
                    handleLogin(parts);
                    break;
                case "REGISTER":
                    handleRegister(parts);
                    break;
                case "CREATE_QUIZ":
                    handleCreateQuiz(parts);
                    break;
                case "GET_QUIZZES":
                    handleGetQuizzes(parts);
                    break;
                case "JOIN_GAME":
                    handleJoinGame(parts);
                    break;
                case "START_GAME":
                    handleStartGame(parts);
                    break;
                case "GET_QUESTION":
                    handleGetQuestion(parts);
                    break;
                case "SUBMIT_ANSWER":
                    handleSubmitAnswer(parts);
                    break;
                case "END_GAME":
                    handleEndGame(parts);
                    break;
                case "NEXT_QUESTION":
                    handleNextQuestion(parts);
                    break;
                case "SHOW_RESULTS":
                    handleShowResults(parts);
                    break;
                case "START_QUESTION":
                    handleStartQuestion(parts);
                    break;
                case "LEAVE_GAME":
                    handleLeaveGame(parts);
                    break;
                default:
                    sendResponse("ERROR|Unknown command: " + command);
            }
        } catch (Exception e) {
            sendResponse("ERROR|" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            sendResponse("ERROR|Invalid login format");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        try {
            AuthService authService = new AuthService();
            User user = authService.authenticate(username, password);

            if (user != null) {
                this.clientType = "HOST";
                sendResponse("LOGIN_SUCCESS|" + user.getUserId() + "|" + user.getUsername());
            } else {
                sendResponse("LOGIN_FAILED|Invalid credentials");
            }
        } catch (Exception e) {
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleRegister(String[] parts) {
        if (parts.length < 4) {
            sendResponse("ERROR|Invalid register format");
            return;
        }

        String username = parts[1];
        String password = parts[2];
        String email = parts[3];

        try {
            AuthService authService = new AuthService();
            User user = authService.registerUser(username, password, email);
            sendResponse("REGISTER_SUCCESS|" + user.getUserId() + "|" + user.getUsername());
        } catch (Exception e) {
            sendResponse("REGISTER_FAILED|" + e.getMessage());
        }
    }

    private void handleCreateQuiz(String[] parts) {
        if (parts.length < 4) {
            sendResponse("ERROR|Invalid create quiz format");
            return;
        }

        int hostId = Integer.parseInt(parts[1]);
        String title = parts[2];
        String accessCode = parts[3];

        try {
            QuizDAO quizDAO = new QuizDAO();
            Quiz quiz = new Quiz(title, hostId, accessCode);
            quizDAO.saveQuiz(quiz);
            sendResponse("QUIZ_CREATED|" + quiz.getQuizId() + "|" + quiz.getTitle());
        } catch (SQLException e) {
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleGetQuizzes(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid get quizzes format");
            return;
        }

        int hostId = Integer.parseInt(parts[1]);

        try {
            QuizDAO quizDAO = new QuizDAO();
            java.util.List<Quiz> quizzes = quizDAO.findByHostId(hostId);
            
            StringBuilder response = new StringBuilder("QUIZZES|");
            for (Quiz quiz : quizzes) {
                response.append(quiz.getQuizId()).append(",")
                       .append(quiz.getTitle()).append(",")
                       .append(quiz.getAccessCode()).append(";");
            }
            sendResponse(response.toString());
        } catch (SQLException e) {
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleJoinGame(String[] parts) {
        if (parts.length < 3) {
            sendResponse("ERROR|Invalid join game format");
            return;
        }

        String pinCode = parts[1];
        String playerName = parts[2];
        
        System.out.println("🎮 Player muốn tham gia game:");
        System.out.println("   PIN Code: " + pinCode);
        System.out.println("   Player Name: " + playerName);

        try {
            GameSessionDAO sessionDAO = new GameSessionDAO();
            GameSession session = sessionDAO.findByPinCode(pinCode);

            if (session == null) {
                System.out.println("✗ PIN code không hợp lệ: " + pinCode);
                sendResponse("JOIN_FAILED|Invalid PIN code");
                return;
            }

            this.clientType = "PLAYER";
            this.sessionId = session.getSessionId();
            
            // Đăng ký client vào server
            // Register client locally
            server.registerClientToGame(sessionId, pinCode, this);

            // If the game has already been started on the server, add player immediately.
            GameSessionHandler gameHandler = server.getGameSession(pinCode);
            if (gameHandler != null) {
                gameHandler.addPlayer(this, playerName);
                System.out.println("✓ Player '" + playerName + "' đã tham gia game với PIN: " + pinCode);
                sendResponse("JOIN_SUCCESS|" + session.getQuizId() + "|" + playerName);
            } else {
                // Game not started yet — add to pending list so when host starts it they'll be added.
                server.addPendingPlayer(pinCode, this, playerName);
                System.out.println("ℹ Player '" + playerName + "' đã được thêm vào danh sách chờ cho PIN: " + pinCode);
                // Still respond with JOIN_SUCCESS so client can enter waiting screen
                sendResponse("JOIN_SUCCESS|" + session.getQuizId() + "|" + playerName);
            }
        } catch (SQLException e) {
            System.err.println("✗ Lỗi SQL khi xử lý JOIN_GAME: " + e.getMessage());
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleStartGame(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid start game format");
            return;
        }

        String pinCode = parts[1];
        
        System.out.println("🚀 Host muốn bắt đầu game với PIN: " + pinCode);

        try {
            GameSessionDAO sessionDAO = new GameSessionDAO();
            GameSession session = sessionDAO.findByPinCode(pinCode);

            if (session != null) {
                System.out.println("✓ Tìm thấy session:");
                System.out.println("  Session ID: " + session.getSessionId());
                System.out.println("  Quiz ID: " + session.getQuizId());
                System.out.println("  PIN Code: " + session.getPinCode());
                
                try {
                    GameSessionHandler gameHandler = new GameSessionHandler(session, server);
                    
                    // Kiểm tra xem có câu hỏi không
                    if (gameHandler.getTotalQuestions() == 0) {
                        System.err.println("✗ Quiz không có câu hỏi nào!");
                        sendResponse("ERROR|Quiz không có câu hỏi nào. Vui lòng thêm câu hỏi trước khi bắt đầu game.");
                        return;
                    }
                    
                    server.registerGameSession(pinCode, gameHandler);
                    this.clientType = "HOST";
                    this.sessionId = session.getSessionId();
                    server.registerClientToGame(sessionId, pinCode, this);
                    
                    // Thiết lập host trong game handler
                    gameHandler.setHost(this);
                    
                    System.out.println("✓ Game session đã được khởi động với PIN: " + pinCode);
                    System.out.println("  Số câu hỏi: " + gameHandler.getTotalQuestions());
                    sendResponse("GAME_STARTED|" + pinCode);
                } catch (Exception e) {
                    System.err.println("✗ Lỗi khi tạo GameSessionHandler: " + e.getMessage());
                    e.printStackTrace();
                    sendResponse("ERROR|Lỗi khi khởi động game: " + e.getMessage());
                }
            } else {
                System.out.println("✗ Không tìm thấy session với PIN: " + pinCode);
                sendResponse("ERROR|Session not found");
            }
        } catch (SQLException e) {
            System.err.println("✗ Lỗi SQL khi xử lý START_GAME: " + e.getMessage());
            e.printStackTrace();
            sendResponse("ERROR|" + e.getMessage());
        } catch (Exception e) {
            System.err.println("✗ Lỗi không xác định khi xử lý START_GAME: " + e.getMessage());
            e.printStackTrace();
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleGetQuestion(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid get question format");
            return;
        }

        String pinCode = parts[1];
        
        try {
            GameSessionHandler gameHandler = server.getGameSession(pinCode);
            if (gameHandler != null) {
                // GameHandler sẽ xử lý việc gửi câu hỏi
                gameHandler.sendNextQuestion(this);
            } else {
                sendResponse("ERROR|Game session not found");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi xử lý GET_QUESTION: " + e.getMessage());
            sendResponse("ERROR|" + e.getMessage());
        }
    }
    
    private void handleSubmitAnswer(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid submit answer format");
            return;
        }

        int optionId = Integer.parseInt(parts[1]);
        
        System.out.println("📝 Player gửi đáp án: Option ID = " + optionId);
        
        try {
            // Tìm game session của player này
            GameSessionHandler gameHandler = findGameHandlerForPlayer();
            if (gameHandler != null) {
                gameHandler.submitAnswer(this, optionId);
                sendResponse("ANSWER_ACCEPTED");
            } else {
                sendResponse("ERROR|Game session not found");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi xử lý SUBMIT_ANSWER: " + e.getMessage());
            sendResponse("ERROR|" + e.getMessage());
        }
    }
    
    /**
     * Tìm GameSessionHandler cho player này.
     */
    private GameSessionHandler findGameHandlerForPlayer() {
        // Tìm trong tất cả active games
        for (String pinCode : server.getActiveGames().keySet()) {
            GameSessionHandler handler = server.getGameSession(pinCode);
            if (handler != null && handler.getPlayers().contains(this)) {
                return handler;
            }
        }
        return null;
    }

    private void handleStartQuestion(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid start question format");
            return;
        }
        String pinCode = parts[1];

        System.out.println("🔄 START_QUESTION received for PIN: " + pinCode);

        try {
            GameSessionHandler gameHandler = server.getGameSession(pinCode);
            if (gameHandler != null) {
                gameHandler.startGame();
                sendResponse("START_QUESTION_OK");
                System.out.println("✓ Game started for PIN: " + pinCode);
            } else {
                System.err.println("✗ START_QUESTION: Game session not found for PIN: " + pinCode);
                sendResponse("ERROR|Game session not found");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi xử lý START_QUESTION: " + e.getMessage());
            e.printStackTrace();
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleLeaveGame(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid leave game format");
            return;
        }

        String pinCode = parts[1];
        System.out.println("↩ Player requests to leave game: " + pinCode);

        try {
            GameSessionHandler gameHandler = server.getGameSession(pinCode);
            if (gameHandler != null) {
                gameHandler.removePlayer(this);
                sendResponse("LEFT_GAME|" + pinCode);
            } else {
                sendResponse("ERROR|Game session not found");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi xử lý LEAVE_GAME: " + e.getMessage());
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleNextQuestion(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid next question format");
            return;
        }

        String pinCode = parts[1];

        try {
            GameSessionHandler gameHandler = server.getGameSession(pinCode);
            if (gameHandler != null) {
                gameHandler.nextQuestion();
                sendResponse("NEXT_QUESTION_OK");
            } else {
                sendResponse("ERROR|Game session not found");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi xử lý NEXT_QUESTION: " + e.getMessage());
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleShowResults(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid show results format");
            return;
        }

        String pinCode = parts[1];

        try {
            GameSessionHandler gameHandler = server.getGameSession(pinCode);
            if (gameHandler != null) {
                gameHandler.showResults();
                sendResponse("SHOW_RESULTS_OK");
            } else {
                sendResponse("ERROR|Game session not found");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi xử lý SHOW_RESULTS: " + e.getMessage());
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    private void handleEndGame(String[] parts) {
        if (parts.length < 2) {
            sendResponse("ERROR|Invalid end game format");
            return;
        }

        String pinCode = parts[1];

        try {
            GameSessionHandler gameHandler = server.getGameSession(pinCode);
            if (gameHandler != null) {
                gameHandler.endGame();
                server.unregisterGameSession(pinCode);
                // Acknowledge the host's request to end the game with a confirmation response.
                sendResponse("END_GAME_OK");
            } else {
                sendResponse("ERROR|Game session not found");
            }
        } catch (Exception e) {
            sendResponse("ERROR|" + e.getMessage());
        }
    }

    /**
     * Gửi response về client.
     */
    public synchronized void sendResponse(String response) {
        if (writer != null && isConnected) {
            try {
                writer.println(response);
                writer.flush(); // Đảm bảo message được gửi ngay, tránh buffer
                // Chỉ log khi cần debug (comment out để giảm I/O)
                // System.out.println("📤 Gửi response đến client " + socket.getRemoteSocketAddress() + ":");
                // System.out.println("   " + response);
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi gửi response đến client " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
                // Đánh dấu connection bị lỗi
                isConnected = false;
            }
        } else {
            if (writer == null) {
                System.err.println("✗ Không thể gửi response: writer is null");
            }
            if (!isConnected) {
                System.err.println("✗ Không thể gửi response: connection đã đóng");
            }
        }
    }

    /**
     * Kiểm tra xem connection còn hoạt động không.
     */
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
    
    /**
     * Đóng kết nối.
     */
    private void closeConnection() {
        isConnected = false;
        try {
            String clientInfo = socket != null ? socket.getRemoteSocketAddress().toString() : "Unknown";
            System.out.println("✗ Client đã ngắt kết nối: " + clientInfo);
            
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
            
            // Nếu client đang nằm trong một game session, loại bỏ họ khỏi session
            try {
                GameSessionHandler gh = findGameHandlerForPlayer();
                if (gh != null) {
                    gh.removePlayer(this);
                }
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi loại bỏ player khỏi session trong closeConnection: " + e.getMessage());
            }

            if (sessionId > 0) {
                server.unregisterClient(sessionId);
            }
        } catch (IOException e) {
            System.err.println("✗ Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
}




