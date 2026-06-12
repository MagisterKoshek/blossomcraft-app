package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.model.AuthResponse;
import com.blossomcraft.desktop.AppContext;
import com.blossomcraft.desktop.Async;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Login & registration screen, including Google sign-in. Mirrors the website's
 * /login and /register flows against the same endpoints.
 */
public class AuthScreen {

    private final AppContext context;
    private final Runnable onAuthenticated;
    private final StackPane root = new StackPane();

    public AuthScreen(AppContext context, Runnable onAuthenticated) {
        this.context = context;
        this.onAuthenticated = onAuthenticated;
        root.getStyleClass().addAll("app-root", "content");
        showLogin();
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }

    private void showLogin() {
        TextField email = new TextField();
        email.setPromptText("Email");
        PasswordField password = new PasswordField();
        password.setPromptText("Пароль");

        Button submit = Ui.primaryButton("Войти");
        submit.setMaxWidth(Double.MAX_VALUE);

        Hyperlink toRegister = new Hyperlink("Нет аккаунта? Зарегистрироваться");
        toRegister.setOnAction(e -> showRegister());

        Button google = new Button("Войти через Google");
        google.setMaxWidth(Double.MAX_VALUE);

        submit.setOnAction(e -> {
            submit.setDisable(true);
            Async.run(
                    () -> context.bc().auth().login(email.getText().trim(), password.getText()),
                    res -> handleAuth(res, submit),
                    err -> {
                        submit.setDisable(false);
                        Ui.error(Ui.message(err));
                    });
        });

        google.setOnAction(e -> promptGoogleToken());

        VBox card = Ui.card(
                Ui.h1("BlossomCraft"),
                Ui.muted("Вход в аккаунт"),
                email, password, submit, google, toRegister);
        card.setMaxWidth(360);
        card.setAlignment(Pos.CENTER_LEFT);
        setCard(card);
    }

    private void showRegister() {
        TextField name = new TextField();
        name.setPromptText("Имя");
        TextField email = new TextField();
        email.setPromptText("Email");
        PasswordField password = new PasswordField();
        password.setPromptText("Пароль");

        Button submit = Ui.primaryButton("Создать аккаунт");
        submit.setMaxWidth(Double.MAX_VALUE);
        Hyperlink toLogin = new Hyperlink("Уже есть аккаунт? Войти");
        toLogin.setOnAction(e -> showLogin());

        submit.setOnAction(e -> {
            submit.setDisable(true);
            Async.run(
                    () -> context.bc().auth().register(
                            name.getText().trim(), email.getText().trim(), password.getText()),
                    res -> handleAuth(res, submit),
                    err -> {
                        submit.setDisable(false);
                        Ui.error(Ui.message(err));
                    });
        });

        VBox card = Ui.card(
                Ui.h1("Регистрация"),
                Ui.muted("Создайте аккаунт BlossomCraft"),
                name, email, password, submit, toLogin);
        card.setMaxWidth(360);
        card.setAlignment(Pos.CENTER_LEFT);
        setCard(card);
    }

    /**
     * Google sign-in: the access token must be obtained from Google's OAuth flow.
     * On desktop, paste a token obtained via the browser consent screen (the
     * README documents how to configure the client); the token is exchanged with
     * google_auth.php exactly like the website does.
     */
    private void promptGoogleToken() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setHeaderText("Google access token");
        dialog.setContentText("Вставьте access token:");
        dialog.showAndWait().ifPresent(token -> {
            if (token.isBlank()) {
                return;
            }
            Async.run(
                    () -> context.bc().auth().googleAuth("login", token.trim()),
                    res -> handleAuth(res, null),
                    err -> Ui.error(Ui.message(err)));
        });
    }

    private void handleAuth(AuthResponse res, Button submit) {
        if (res != null && res.user != null) {
            context.bc().setSession(res.user);
            onAuthenticated.run();
        } else {
            if (submit != null) {
                submit.setDisable(false);
            }
            Ui.error(res != null && res.error != null ? res.error : "Не удалось войти");
        }
    }

    private void setCard(VBox card) {
        HBox.setHgrow(card, Priority.NEVER);
        root.getChildren().setAll(card);
        StackPane.setMargin(card, new Insets(40));
        StackPane.setAlignment(card, Pos.CENTER);
    }
}
