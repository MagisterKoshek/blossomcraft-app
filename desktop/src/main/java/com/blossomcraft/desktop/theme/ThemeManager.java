package com.blossomcraft.desktop.theme;

import com.blossomcraft.core.theme.Theme;
import javafx.scene.Scene;
import javafx.scene.Parent;

/**
 * Applies one of the four BlossomCraft themes to a JavaFX scene by toggling a
 * root style-class. The actual colors live in {@code /css/app.css}, mirroring the
 * website's {@code index.css} theme blocks (dark / light / mirror / gray-mirror).
 */
public final class ThemeManager {

    private static final String[] THEME_CLASSES = {
            "theme-dark", "theme-light", "theme-mirror", "theme-gray-mirror"
    };

    private ThemeManager() {
    }

    public static void install(Scene scene) {
        scene.getStylesheets().add(
                ThemeManager.class.getResource("/css/app.css").toExternalForm());
    }

    public static void apply(Parent root, Theme theme) {
        root.getStyleClass().removeAll(THEME_CLASSES);
        root.getStyleClass().add(styleClass(theme));
    }

    private static String styleClass(Theme theme) {
        switch (theme) {
            case LIGHT:
                return "theme-light";
            case MIRROR:
                return "theme-mirror";
            case GRAY_MIRROR:
                return "theme-gray-mirror";
            case DARK:
            default:
                return "theme-dark";
        }
    }
}
