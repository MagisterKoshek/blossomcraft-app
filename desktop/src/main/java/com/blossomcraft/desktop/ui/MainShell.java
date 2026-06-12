package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.model.User;
import com.blossomcraft.desktop.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The authenticated app shell: a left sidebar with the same sections as the
 * website (Shop, Music, Videos, Messages, Profile, Admin, Settings) and a
 * swappable content area on the right.
 */
public class MainShell {

    private final AppContext context;
    private final BorderPane root = new BorderPane();
    private final VBox sidebar = new VBox();
    private final List<Button> navButtons = new ArrayList<>();

    public MainShell(AppContext context, Runnable onLogout) {
        this.context = context;
        root.getStyleClass().add("app-root");

        sidebar.getStyleClass().add("sidebar");
        buildSidebar(onLogout);
        root.setLeft(sidebar);

        // Default landing page = Shop, mirroring the website's home route.
        select(0, () -> new ShopPage(context));
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }

    private void buildSidebar(Runnable onLogout) {
        Label brand = Ui.h1("🌸 BlossomCraft");
        brand.getStyleClass().add("neon");
        sidebar.getChildren().add(brand);

        User user = context.bc().currentUser();
        if (user != null) {
            Label who = new Label((user.roleEmoji != null ? user.roleEmoji + " " : "") + user.displayName());
            who.getStyleClass().add("muted");
            sidebar.getChildren().add(who);
        }

        sidebar.getChildren().add(spacer(8));

        addNav("🛒 Магазин", 0, () -> new ShopPage(context));
        addNav("🎵 Музыка", 1, () -> new MusicPage(context));
        addNav("🎬 Видео", 2, () -> new VideosPage(context));
        addNav("💬 Сообщения", 3, () -> new MessagesPage(context));
        addNav("👤 Профиль", 4, () -> new ProfilePage(context));

        if (context.bc().hasPermission("manage_users") || context.bc().hasPermission("*")) {
            addNav("⚙️ Админ", 5, () -> new AdminPage(context));
        }
        addNav("🎨 Настройки", 6, () -> new SettingsPage(context, this::reload));

        sidebar.getChildren().add(Ui.grow());

        Button logout = new Button("Выйти");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> onLogout.run());
        sidebar.getChildren().add(logout);
    }

    private void addNav(String label, int index, Supplier<Page> pageFactory) {
        Button button = new Button(label);
        button.getStyleClass().add("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(e -> select(index, pageFactory));
        navButtons.add(button);
        sidebar.getChildren().add(button);
    }

    private void select(int index, Supplier<Page> pageFactory) {
        for (int i = 0; i < navButtons.size(); i++) {
            Button b = navButtons.get(i);
            b.getStyleClass().remove("nav-item-active");
            if (i == index) {
                b.getStyleClass().add("nav-item-active");
            }
        }
        Page page = pageFactory.get();
        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(page.getView());
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("content");
        root.setCenter(scroller);
        page.onShown();
    }

    private void reload() {
        sidebar.getChildren().clear();
        navButtons.clear();
        // The logout handler is re-bound via a no-op fallback; the shell is
        // typically rebuilt on theme change by re-selecting the settings page.
        buildSidebar(() -> { });
        select(6, () -> new SettingsPage(context, this::reload));
    }

    private Region spacer(double height) {
        Region r = new Region();
        r.setMinHeight(height);
        return r;
    }
}
