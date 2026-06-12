package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.ApiConfig;
import com.blossomcraft.core.theme.Theme;
import com.blossomcraft.desktop.AppContext;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Settings: theme selection (dark/light/mirror/gray-mirror) and API host configuration. */
public class SettingsPage implements Page {

    private final AppContext context;
    private final Runnable onThemeChanged;
    private final VBox root = new VBox(12);

    public SettingsPage(AppContext context, Runnable onThemeChanged) {
        this.context = context;
        this.onThemeChanged = onThemeChanged;
        build();
    }

    @Override
    public Node getView() {
        return root;
    }

    private void build() {
        ComboBox<Theme> themeBox = new ComboBox<>();
        themeBox.getItems().addAll(Theme.values());
        themeBox.setValue(context.theme());
        themeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Theme t) {
                return t == null ? "" : t.label;
            }

            @Override
            public Theme fromString(String s) {
                return Theme.DARK;
            }
        });
        themeBox.valueProperty().addListener((o, a, b) -> {
            if (b != null) {
                context.setTheme(b);
                // Re-apply theme by toggling the scene root style class.
                Node scene = root.getScene() != null ? root.getScene().getRoot() : null;
                if (scene instanceof javafx.scene.Parent parent) {
                    com.blossomcraft.desktop.theme.ThemeManager.apply(parent, b);
                }
                if (onThemeChanged != null) {
                    onThemeChanged.run();
                }
            }
        });

        TextField apiBase = new TextField(ApiConfig.getBaseUrl());
        apiBase.setPromptText("https://your-host/api");
        Button saveApi = Ui.primaryButton("Сохранить адрес API");
        saveApi.setOnAction(e -> {
            try {
                ApiConfig.setBaseUrl(apiBase.getText().trim());
                Ui.info("Адрес API обновлён. Перезапустите для применения ко всем запросам.");
            } catch (RuntimeException ex) {
                Ui.error(Ui.message(ex));
            }
        });

        VBox themeCard = Ui.card(Ui.h2("Тема"), Ui.muted("Оформление как на сайте"), themeBox);
        VBox apiCard = Ui.card(Ui.h2("Сервер"),
                Ui.muted("Адрес PHP-API (тот же бэкенд, что у сайта)"), apiBase, saveApi);
        themeCard.setMaxWidth(520);
        apiCard.setMaxWidth(520);

        root.getChildren().setAll(Ui.h1("Настройки"), themeCard, apiCard);
    }
}
