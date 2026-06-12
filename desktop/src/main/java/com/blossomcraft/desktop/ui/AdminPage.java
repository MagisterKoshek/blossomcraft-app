package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.model.User;
import com.blossomcraft.desktop.AppContext;
import com.blossomcraft.desktop.Async;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Admin panel: user management (rename, role change, delete) plus quick links to
 * logs/analytics. Visible only to permitted accounts; the backend re-checks.
 */
public class AdminPage implements Page {

    private final AppContext context;
    private final VBox root = new VBox(12);
    private final VBox userBox = new VBox(8);

    public AdminPage(AppContext context) {
        this.context = context;
        Button reload = new Button("Обновить");
        reload.setOnAction(e -> load());
        root.getChildren().addAll(new HBox(12, Ui.h1("Админ-панель"), Ui.grow(), reload), userBox);
        VBox.setVgrow(userBox, Priority.ALWAYS);
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void onShown() {
        load();
    }

    private void load() {
        userBox.getChildren().setAll(Ui.muted("Загрузка..."));
        Async.run(() -> context.bc().admin().allUsers(), this::render,
                err -> userBox.getChildren().setAll(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private void render(List<User> users) {
        userBox.getChildren().clear();
        if (users.isEmpty()) {
            userBox.getChildren().add(Ui.muted("Пользователей нет"));
            return;
        }
        for (User u : users) {
            userBox.getChildren().add(row(u));
        }
    }

    private Node row(User u) {
        Label name = new Label((u.roleEmoji != null ? u.roleEmoji + " " : "") + u.displayName());
        Label meta = Ui.muted(u.email + "   •   " + (u.roleDisplay != null ? u.roleDisplay : u.roleId));

        Button rename = new Button("Имя");
        rename.setOnAction(e -> prompt("Новое имя", u.name, value ->
                Async.run(() -> context.bc().admin().setUsername(u.id, value), this::load, err -> Ui.error(Ui.message(err)))));

        Button setRole = new Button("Роль");
        setRole.setOnAction(e -> prompt("ID роли", u.roleId, value ->
                Async.run(() -> context.bc().admin().setRole(u.id, value), this::load, err -> Ui.error(Ui.message(err)))));

        Button delete = new Button("Удалить");
        delete.setOnAction(e -> Async.run(() -> context.bc().admin().deleteUser(u.id), this::load, err -> Ui.error(Ui.message(err))));

        VBox info = new VBox(2, name, meta);
        HBox row = new HBox(10, info, Ui.grow(), rename, setRole, delete);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("glass-card");
        return row;
    }

    private void prompt(String header, String initial, java.util.function.Consumer<String> onValue) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.setHeaderText(header);
        dialog.showAndWait().ifPresent(value -> {
            if (!value.isBlank()) {
                onValue.accept(value.trim());
            }
        });
    }
}
