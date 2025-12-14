package com.example.kahoot.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Main class để chạy server với giao diện JavaFX.
 */
public class ServerMain extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerMain.class.getResource("/views/server_dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 600);
        
        stage.setTitle("Kahoot Server - Dashboard");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            // Đóng server khi đóng cửa sổ
            ServerDashboardController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.handleStopServerAction(null);
            }
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

