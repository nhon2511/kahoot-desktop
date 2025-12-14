package com.example.kahoot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL; // Cần import thêm

public class Main extends Application {

    /**
     * Phương thức bắt đầu của JavaFX.
     * @param stage Cửa sổ chính của ứng dụng.
     */
    @Override
    public void start(Stage stage) {

        // 1. Khai báo URL để kiểm tra và sử dụng
        URL fxmlUrl = null;

        try {
            // Tải file FXML cho màn hình đăng nhập
            // Đảm bảo file login.fxml nằm trong: src/main/resources/views/
            fxmlUrl = Main.class.getResource("/views/login.fxml");

            // 2. Thêm kiểm tra URL (quan trọng)
            if (fxmlUrl == null) {
                // In ra thông báo lỗi rõ ràng hơn, bao gồm thư mục resource
                System.err.println("✗ KHÔNG TÌM THẤY file FXML! Vui lòng kiểm tra:");
                System.err.println("   1. Đường dẫn tuyệt đối: /views/login.fxml");
                System.err.println("   2. File có nằm trong thư mục 'src/main/resources/views/' không?");
                // Nếu không tìm thấy, chúng ta không thể tiếp tục, nên thoát.
                return;
            }

            // Khởi tạo FXMLLoader với URL đã kiểm tra
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);

            // 3. Tạo Scene (Root của Scene là Parent)
            Scene scene = new Scene(fxmlLoader.load(), 400, 300);

            stage.setTitle("Kahoot Desktop - Đăng nhập"); // Tiêu đề cửa sổ
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            // Lỗi khi tải hoặc phân tích cú pháp FXML (ví dụ: cú pháp FXML bị lỗi)
            System.err.println("✗ Lỗi I/O (load FXML): " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Ngoại lệ này có thể xảy ra nếu fxmlLoader.load() gọi mà không có URL.
            // Nhưng đã được xử lý ở bước 2, tuy nhiên giữ lại để an toàn.
            System.err.println("✗ Lỗi NullPointerException. Có thể do URL FXML bị null.");
            e.printStackTrace();
        } catch (Exception e) {
            // Bắt các lỗi khác (ví dụ: lỗi khởi tạo Controller)
            System.err.println("✗ Lỗi không mong đợi trong start(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Phương thức main để khởi chạy ứng dụng.
     * @param args Các đối số dòng lệnh.
     */
    public static void main(String[] args) {
        // Khởi chạy vòng đời JavaFX
        launch(args);
    }
}