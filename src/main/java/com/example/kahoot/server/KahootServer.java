package com.example.kahoot.server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP Server cho ứng dụng Kahoot.
 * Lắng nghe kết nối từ client và xử lý các request.
 */
public class KahootServer {
    private static final int DEFAULT_PORT = 8888;
    private static final String DEFAULT_HOST = "0.0.0.0"; // Bind vào tất cả interfaces
    private int port;
    private String bindAddress;
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private boolean isRunning;
    
    // Lưu trữ các client đang kết nối: sessionId -> ClientHandler
    private ConcurrentHashMap<Integer, ClientHandler> activeClients;
    
    // Lưu trữ các game session đang hoạt động: pinCode -> GameSessionHandler
    private ConcurrentHashMap<String, GameSessionHandler> activeGames;
    // Lưu trữ các player đang chờ host bắt đầu game: pinCode -> list of pending players
    private ConcurrentHashMap<String, java.util.List<PendingPlayer>> pendingPlayers;
    
    // Callback để cập nhật UI
    private ServerStatusCallback statusCallback;

    public KahootServer() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }
    
    public KahootServer(int port) {
        this(DEFAULT_HOST, port);
    }
    
    public KahootServer(String bindAddress, int port) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.activeClients = new ConcurrentHashMap<>();
        this.activeGames = new ConcurrentHashMap<>();
        this.pendingPlayers = new ConcurrentHashMap<>();
    }
    
    public void setStatusCallback(ServerStatusCallback callback) {
        this.statusCallback = callback;
    }

    /**
     * Add a player who joined before the host started the game.
     */
    public void addPendingPlayer(String pinCode, ClientHandler client, String playerName) {
        pendingPlayers.compute(pinCode, (k, list) -> {
            if (list == null) list = new java.util.ArrayList<>();
            list.add(new PendingPlayer(client, playerName));
            return list;
        });
        System.out.println("✓ Added pending player '" + playerName + "' for PIN: " + pinCode);
    }

    /**
     * Khởi động server.
     */
    public void start() {
        try {
            // Nếu bindAddress là "0.0.0.0" hoặc rỗng, bind vào tất cả interfaces
            ServerSocket serverSocketToUse;
            if (bindAddress == null || bindAddress.isEmpty() || bindAddress.equals("0.0.0.0")) {
                // Bind vào tất cả interfaces để có thể nhận kết nối từ mọi IP
                serverSocketToUse = new ServerSocket(port, 50);
                String localIP = getLocalIPAddress();
                String message = "Kahoot Server đã khởi động trên tất cả interfaces (0.0.0.0:" + port + ")";
                System.out.println(message);
                System.out.println("  Local IP: " + localIP);
                System.out.println("  Clients có thể kết nối qua: " + localIP + ":" + port);
                log(message);
                log("  Local IP: " + localIP);
                log("  Clients có thể kết nối qua: " + localIP + ":" + port);
            } else {
                // Bind vào IP cụ thể
                InetAddress bindAddr = InetAddress.getByName(bindAddress);
                serverSocketToUse = new ServerSocket(port, 50, bindAddr);
                String message = "Kahoot Server đã khởi động trên " + bindAddress + ":" + port;
                System.out.println(message);
                log(message);
            }
            
            this.serverSocket = serverSocketToUse;
            isRunning = true;
            log("Đang chờ kết nối từ client...");

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                String connectMsg = "✓ Client đã kết nối từ: " + clientSocket.getRemoteSocketAddress() + 
                                   " (Local: " + clientSocket.getLocalSocketAddress() + ")";
                System.out.println(connectMsg);
                log(connectMsg);
                
                // Xử lý mỗi client trong một thread riêng
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clientThreadPool.execute(clientHandler);
                
                log("Tổng số client đang kết nối: " + (activeClients.size() + 1));
            }
        } catch (IOException e) {
            if (isRunning) {
                String errorMsg = "Lỗi khi khởi động server: " + e.getMessage();
                System.err.println(errorMsg);
                log(errorMsg);
                e.printStackTrace();
            }
        }
    }
    
    private void log(String message) {
        if (statusCallback != null) {
            statusCallback.onLog(message);
        }
    }

    /**
     * Dừng server.
     */
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clientThreadPool.shutdown();
            System.out.println("Server đã dừng.");
        } catch (IOException e) {
            System.err.println("Lỗi khi dừng server: " + e.getMessage());
        }
    }

    /**
     * Đăng ký client vào một game session.
     * Lưu ý: addPlayer() phải được gọi riêng với playerName.
     */
    public void registerClientToGame(int sessionId, String pinCode, ClientHandler clientHandler) {
        activeClients.put(sessionId, clientHandler);
        // addPlayer() sẽ được gọi riêng trong handleJoinGame hoặc handleStartGame
    }

    /**
     * Đăng ký game session mới.
     */
    public void registerGameSession(String pinCode, GameSessionHandler gameHandler) {
        activeGames.put(pinCode, gameHandler);
        System.out.println("Game session đã đăng ký với PIN: " + pinCode);
        // Nếu có player chờ trước đó, thêm họ vào game handler
        java.util.List<PendingPlayer> pending = pendingPlayers.remove(pinCode);
        if (pending != null && !pending.isEmpty()) {
            System.out.println("✓ Found " + pending.size() + " pending players for PIN: " + pinCode + ", adding to session...");
            for (PendingPlayer pp : pending) {
                try {
                    gameHandler.addPlayer(pp.client, pp.playerName);
                } catch (Exception e) {
                    System.err.println("✗ Lỗi khi thêm pending player " + pp.playerName + " vào game: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Hủy đăng ký game session.
     */
    public void unregisterGameSession(String pinCode) {
        activeGames.remove(pinCode);
        System.out.println("Game session đã hủy đăng ký với PIN: " + pinCode);
    }

    /**
     * Lấy game session handler theo PIN code.
     */
    public GameSessionHandler getGameSession(String pinCode) {
        return activeGames.get(pinCode);
    }
    
    /**
     * Lấy tất cả active games (để ClientHandler có thể tìm game của mình).
     */
    public ConcurrentHashMap<String, GameSessionHandler> getActiveGames() {
        return activeGames;
    }

    /**
     * Hủy đăng ký client.
     */
    public void unregisterClient(int sessionId) {
        activeClients.remove(sessionId);
        log("Client đã ngắt kết nối. Tổng số client: " + activeClients.size());
    }
    
    /**
     * Lấy số lượng client đang kết nối.
     */
    public int getActiveClientCount() {
        return activeClients.size();
    }
    
    /**
     * Lấy số lượng game session đang hoạt động.
     */
    public int getActiveGameCount() {
        return activeGames.size();
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getBindAddress() {
        return bindAddress;
    }
    
    /**
     * Lấy địa chỉ IP local của máy.
     */
    private String getLocalIPAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                java.util.Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể lấy IP local: " + e.getMessage());
        }
        
        // Fallback: thử lấy IP từ localhost
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    public static void main(String[] args) {
        KahootServer server = new KahootServer();
        server.start();
    }
    
    /**
     * Interface để callback status và log.
     */
    public interface ServerStatusCallback {
        void onLog(String message);
    }

    /**
     * Small holder for pending players.
     */
    private static class PendingPlayer {
        public final ClientHandler client;
        public final String playerName;

        public PendingPlayer(ClientHandler client, String playerName) {
            this.client = client;
            this.playerName = playerName;
        }
    }
}

