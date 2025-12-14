package com.example.kahoot.util;

import javafx.scene.Scene;

public final class UIUtils {

    private UIUtils() {
        // utility
    }

    /**
     * Apply the global stylesheet `/css/style.css` to the given Scene if available.
     */
    public static void applyGlobalStyles(Scene scene) {
        if (scene == null) return;
        try {
            java.net.URL cssUrl = UIUtils.class.getResource("/css/style.css");
            if (cssUrl != null) {
                String s = cssUrl.toExternalForm();
                if (!scene.getStylesheets().contains(s)) {
                    scene.getStylesheets().add(s);
                }
            } else {
                System.err.println("⚠ Không tìm thấy stylesheet /css/style.css (UIUtils)");
            }
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi thêm stylesheet (UIUtils): " + e.getMessage());
        }
    }
}
