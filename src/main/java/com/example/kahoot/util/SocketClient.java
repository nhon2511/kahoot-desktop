package com.example.kahoot.util;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Client Socket để kết nối với server.
 */
public class SocketClient {
    private String serverHost;
    private int serverPort;
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean isConnected;
    private Thread listenerThread;
    private Consumer<String> messageListener;
    private BlockingQueue<String> responseQueue; // Queue cho response đồng bộ
    private boolean waitingForResponse; // Flag để biết đang đợi response

    /**
     * Constructor mặc định (localhost:8888).
     */
    public SocketClient() {
        this("localhost", 8888);
    }

    /**
     * Constructor với IP và PORT tùy chỉnh.
     */
    public SocketClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.isConnected = false;
        this.responseQueue = new LinkedBlockingQueue<>();
        this.waitingForResponse = false;
    }

    /**
     * Kết nối đến server.
     */
    public boolean connect() {
        return connect(this.serverHost, this.serverPort);
    }

    /**
     * Kết nối đến server với IP và PORT cụ thể.
     */
    public boolean connect(String host, int port) {
        try {
            this.serverHost = host;
            this.serverPort = port;
            
            System.out.println("🔄 Đang kết nối đến server " + host + ":" + port + "...");
            
            // Tạo socket với timeout để tránh lag
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 5000); // 5 giây timeout
            socket.setSoTimeout(30000); // 30 giây timeout cho read operations
            
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;
            System.out.println("✓ Đã kết nối đến server: " + host + ":" + port);
            
            // Bắt đầu listener thread
            startListener();
            
            return true;
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("✗ Timeout khi kết nối đến server " + host + ":" + port + " (quá 5 giây)");
            isConnected = false;
            return false;
        } catch (java.net.ConnectException e) {
            System.err.println("✗ Không thể kết nối đến server " + host + ":" + port);
            System.err.println("  Lỗi: " + e.getMessage());
            System.err.println("  Kiểm tra:");
            System.err.println("  1. Server đang chạy trên " + host + ":" + port);
            System.err.println("  2. Firewall không chặn port " + port);
            System.err.println("  3. IP " + host + " đúng và có thể truy cập được");
            System.err.println("  4. Test kết nối: telnet " + host + " " + port);
            isConnected = false;
            return false;
        } catch (IOException e) {
            System.err.println("✗ Lỗi khi kết nối đến server " + host + ":" + port + ": " + e.getMessage());
            System.err.println("  Kiểm tra:");
            System.err.println("  - Server đang chạy trên " + host + ":" + port);
            System.err.println("  - Firewall không chặn kết nối");
            System.err.println("  - Network có thể truy cập được");
            e.printStackTrace();
            isConnected = false;
            return false;
        }
    }
    
    /**
     * Bắt đầu thread lắng nghe message từ server.
     */
    private void startListener() {
        if (listenerThread != null && listenerThread.isAlive()) {
            return;
        }
        
        listenerThread = new Thread(() -> {
            try {
                String message;
                while (isConnected && socket != null && !socket.isClosed() && reader != null) {
                    message = reader.readLine();
                    if (message == null) {
                        // Server đã đóng kết nối
                        break;
                    }
                    
                    System.out.println("Nhận được message từ server: " + message);
                    
                    // Nếu đang đợi response đồng bộ, chỉ đưa vào queue nếu là response hợp lệ
                    // Bỏ qua các message broadcast như PLAYER_JOINED, QUESTION, etc.
                    if (waitingForResponse) {
                        // Chỉ nhận các response hợp lệ (không phải broadcast message)
                        if (isValidResponse(message)) {
                            try {
                                responseQueue.put(message);
                                waitingForResponse = false;
                                System.out.println("✓ Đã nhận response hợp lệ: " + message);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        } else {
                            // Message không phải response, gửi đến listener nếu có
                            System.out.println("⚠ Bỏ qua message broadcast khi đợi response: " + message);
                            if (messageListener != null) {
                                try {
                                    messageListener.accept(message);
                                } catch (Exception e) {
                                    System.err.println("Lỗi trong message listener: " + e.getMessage());
                                }
                            }
                        }
                    } else {
                        // Gọi listener nếu có (cho message bất đồng bộ)
                        if (messageListener != null) {
                            try {
                                messageListener.accept(message);
                            } catch (Exception e) {
                                System.err.println("Lỗi trong message listener: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (isConnected) {
                    System.err.println("Lỗi khi đọc message từ server: " + e.getMessage());
                }
            } finally {
                isConnected = false;
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Kiểm tra xem message có phải là response hợp lệ không.
     * Các message broadcast như PLAYER_JOINED, QUESTION, etc. không phải response.
     */
    private boolean isValidResponse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String command = message.split("\\|")[0];
        
        // Các command hợp lệ cho response đồng bộ
        return command.equals("JOIN_SUCCESS") ||
               command.equals("JOIN_FAILED") ||
               command.equals("LOGIN_SUCCESS") ||
               command.equals("LOGIN_FAILED") ||
               command.equals("REGISTER_SUCCESS") ||
               command.equals("REGISTER_FAILED") ||
               command.equals("QUIZ_CREATED") ||
               command.equals("QUIZZES") ||
               command.equals("GAME_STARTED") ||
               command.equals("START_QUESTION_OK") ||
               command.equals("NEXT_QUESTION_OK") ||
               command.equals("SHOW_RESULTS_OK") ||
               command.equals("GAME_ENDED") ||
               command.equals("ANSWER_ACCEPTED") ||
               command.equals("ERROR") ||
               command.startsWith("ERROR");
    }
    
    /**
     * Đăng ký listener để nhận message từ server.
     */
    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }
    
    public String getServerHost() {
        return serverHost;
    }
    
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Gửi message đến server (không đợi response).
     * Response sẽ được nhận qua messageListener.
     */
    public void sendMessageAsync(String message) {
        if (!isConnected || writer == null) {
            System.err.println("Không thể gửi message: Chưa kết nối");
            return;
        }

        try {
            writer.println(message);
            System.out.println("Đã gửi message (async): " + message);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi message: " + e.getMessage());
        }
    }
    
    /**
     * Gửi message đến server và đợi response (cho các command cần response ngay).
     */
    public String sendMessage(String message) {
        if (!isConnected || writer == null) {
            System.err.println("✗ Không thể gửi message: Chưa kết nối hoặc writer null");
            return "ERROR|Not connected to server";
        }

        try {
            waitingForResponse = true;
            responseQueue.clear(); // Xóa queue cũ
            
            writer.println(message);
            writer.flush(); // Đảm bảo message được gửi ngay
            System.out.println("✓ Đã gửi message (sync): " + message);
            
            // Đợi response từ queue với timeout 30 giây (tăng từ 10 để tránh timeout)
            try {
                String response = responseQueue.poll(30, java.util.concurrent.TimeUnit.SECONDS);
                waitingForResponse = false;
                
                if (response == null) {
                    System.err.println("✗ Timeout: Không nhận được response sau 30 giây");
                    return "ERROR|Timeout waiting for response";
                }
                
                System.out.println("✓ Nhận được response: " + response);
                return response;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                waitingForResponse = false;
                System.err.println("✗ Interrupted khi đợi response");
                return "ERROR|Interrupted waiting for response";
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi gửi message: " + e.getMessage());
            e.printStackTrace();
            waitingForResponse = false;
            return "ERROR|" + e.getMessage();
        }
    }

    /**
     * Đóng kết nối.
     */
    public void disconnect() {
        isConnected = false;
        messageListener = null;
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            System.out.println("Đã ngắt kết nối với server");
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
}




