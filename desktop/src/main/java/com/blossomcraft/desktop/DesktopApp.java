package com.blossomcraft.desktop;

import com.blossomcraft.core.ApiConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * Desktop (PC) entry point for the BlossomCraft app.
 *
 * <p>The desktop app embeds the live BlossomCraft website inside a JavaFX
 * {@link WebView}, so the interface and every feature match the site exactly —
 * it <em>is</em> the site, running in a real application window. The site URL is
 * derived from the configured API base ({@link ApiConfig#getSiteUrl()}).</p>
 *
 * <p>Run with:
 * {@code ./gradlew :desktop:run -Pblossomcraft.api.base=https://your-host/api}</p>
 */
public class DesktopApp extends Application {

    /** A standard desktop Chrome user agent (helps site compatibility and sign-in). */
    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("BlossomCraft");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent(DESKTOP_USER_AGENT);
        engine.load(ApiConfig.getSiteUrl());

        StackPane root = new StackPane(webView);
        Scene scene = new Scene(root, 1280, 820);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
