package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.model.User;
import com.blossomcraft.desktop.AppContext;
import com.blossomcraft.desktop.Async;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/** Profile view & editor: name, bio, avatar emoji/color, mirroring the website's dashboard. */
public class ProfilePage implements Page {

    private final AppContext context;
    private final VBox root = new VBox(12);

    public ProfilePage(AppContext context) {
        this.context = context;
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void onShown() {
        User user = context.bc().currentUser();
        root.getChildren().clear();
        if (user == null) {
            root.getChildren().add(Ui.muted("Не авторизован"));
            return;
        }

        Label header = Ui.h1((user.roleEmoji != null ? user.roleEmoji + " " : "") + user.displayName());
        Label role = Ui.muted("Роль: " + (user.roleDisplay != null ? user.roleDisplay : user.roleId));
        Label email = Ui.muted(user.email);

        TextField name = new TextField(user.name);
        name.setPromptText("Имя");
        TextField avatarEmoji = new TextField(user.avatarEmoji);
        avatarEmoji.setPromptText("Эмодзи аватара");
        TextField avatarColor = new TextField(user.avatarColor);
        avatarColor.setPromptText("Цвет аватара (#hex)");
        TextArea bio = new TextArea(user.bio);
        bio.setPromptText("О себе");
        bio.setPrefRowCount(4);
        bio.setWrapText(true);

        Button save = Ui.primaryButton("Сохранить");
        save.setOnAction(e -> {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("name", name.getText().trim());
            fields.put("avatarEmoji", avatarEmoji.getText().trim());
            fields.put("avatarColor", avatarColor.getText().trim());
            fields.put("bio", bio.getText());
            save.setDisable(true);
            Async.run(() -> context.bc().profile().updateProfile(fields), () -> {
                save.setDisable(false);
                user.name = name.getText().trim();
                user.avatarEmoji = avatarEmoji.getText().trim();
                user.avatarColor = avatarColor.getText().trim();
                user.bio = bio.getText();
                Ui.info("Профиль обновлён");
            }, err -> {
                save.setDisable(false);
                Ui.error(Ui.message(err));
            });
        });

        VBox card = Ui.card(header, role, email,
                new Label("Имя"), name,
                new Label("Эмодзи / цвет аватара"), avatarEmoji, avatarColor,
                new Label("О себе"), bio, save);
        card.setMaxWidth(520);
        root.getChildren().add(card);
    }
}
