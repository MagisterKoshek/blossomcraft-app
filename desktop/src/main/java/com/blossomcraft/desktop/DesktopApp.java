package com.blossomcraft.desktop;

import com.blossomcraft.core.model.User;
import com.blossomcraft.desktop.theme.ThemeManager;
import com.blossomcraft.desktop.ui.AuthScreen;
import com.blossomcraft.desktop.ui.MainShell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Desktop (PC) entry point for the BlossomCraft native app.
 *
 * <p>On launch it tries to restore the session from a stored token. If a valid
 * session exists it opens the {@link MainShell}; otherwise it shows the
 * {@link AuthScreen}. Theme selection and routing live in those components.</p>
 *
 * <p>Run with:
 * {@code ./gradlew :desktop:run -Pblossomcraft.api.base=https://your-host/api}</p>
 */
public class DesktopApp extends Application {

    private final AppContext context = new AppContext();
    private Stage stage;
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.setTitle("BlossomCraft");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);

        StackPane bootstrap = new StackPane(new Label("Загрузка..."));
        bootstrap.getStyleClass().addAll("app-root", "content");
        scene = new Scene(bootstrap, 1180, 760);
        ThemeManager.install(scene);
        applyTheme(bootstrap);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Restore session in the background, then route to the right screen.
        Async.run(
                context.bc()::restoreSession,
                this::route,
                err -> route(null));
    }

    private void route(User user) {
        if (user != null) {
            showMain();
        } else {
            showAuth();
        }
    }

    public void showAuth() {
        AuthScreen auth = new AuthScreen(context, this::showMain);
        setRoot(auth.getRoot());
    }

    public void showMain() {
        MainShell shell = new MainShell(context, this::onLogout);
        setRoot(shell.getRoot());
    }

    private void onLogout() {
        Async.run(context.bc().auth()::logout, () -> {
            context.bc().clearSession();
            showAuth();
        }, err -> {
            context.bc().clearSession();
            showAuth();
        });
    }

    private void setRoot(javafx.scene.Parent root) {
        applyTheme(root);
        scene.setRoot(root);
    }

    private void applyTheme(javafx.scene.Parent root) {
        ThemeManager.apply(root, context.theme());
    }

    @Override
    public void stop() {
        Async.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
