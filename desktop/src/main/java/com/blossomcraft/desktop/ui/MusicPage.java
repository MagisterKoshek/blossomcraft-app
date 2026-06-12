package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.model.Track;
import com.blossomcraft.core.ApiConfig;
import com.blossomcraft.desktop.AppContext;
import com.blossomcraft.desktop.Async;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.List;
import java.util.function.Supplier;

/**
 * Music platform: tabs for All / Top / Favorites (mirroring the website) plus a
 * persistent bottom player. Likes and plays are recorded against the API.
 */
public class MusicPage implements Page {

    private final AppContext context;
    private final VBox root = new VBox(12);
    private final VBox listBox = new VBox(8);
    private final Label nowPlaying = Ui.muted("Ничего не играет");
    private MediaPlayer player;

    public MusicPage(AppContext context) {
        this.context = context;

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                tab("Все треки", () -> context.bc().music().publicTracks()),
                tab("Топ", () -> context.bc().music().topTracks(20)),
                tab("Избранное", () -> context.bc().music().favoriteTracks()),
                tab("Мои", () -> context.bc().music().myTracks()));
        tabs.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null && b.getUserData() instanceof Supplier) {
                @SuppressWarnings("unchecked")
                Supplier<List<Track>> sup = (Supplier<List<Track>>) b.getUserData();
                loadTracks(sup);
            }
        });

        HBox playerBar = new HBox(12, new Label("▶"), nowPlaying);
        playerBar.setAlignment(Pos.CENTER_LEFT);
        playerBar.getStyleClass().add("glass-card");

        root.getChildren().addAll(Ui.h1("Музыка"), tabs, listBox, Ui.grow(), playerBar);
        VBox.setVgrow(listBox, Priority.ALWAYS);
    }

    private Tab tab(String title, Supplier<List<Track>> loader) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setUserData(loader);
        return tab;
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void onShown() {
        loadTracks(() -> context.bc().music().publicTracks());
    }

    private void loadTracks(Supplier<List<Track>> loader) {
        listBox.getChildren().setAll(Ui.muted("Загрузка..."));
        Async.run(loader::get, this::render,
                err -> listBox.getChildren().setAll(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private void render(List<Track> tracks) {
        listBox.getChildren().clear();
        if (tracks.isEmpty()) {
            listBox.getChildren().add(Ui.muted("Треков нет"));
            return;
        }
        for (Track t : tracks) {
            listBox.getChildren().add(row(t));
        }
    }

    private Node row(Track t) {
        Label title = new Label((t.title == null ? "—" : t.title) + "  •  " + (t.artist == null ? "" : t.artist));
        Label meta = Ui.muted("▶ " + t.plays + "   ♥ " + t.likes);

        Button play = new Button("▶");
        play.setOnAction(e -> play(t));

        Button like = new Button(t.liked ? "♥" : "♡");
        like.setOnAction(e -> toggleLike(t, like));

        HBox row = new HBox(12, title, Ui.grow(), meta, like, play);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("glass-card");
        return row;
    }

    private void toggleLike(Track t, Button like) {
        boolean willLike = !t.liked;
        Async.run(() -> {
            if (willLike) {
                context.bc().music().like(t.id);
            } else {
                context.bc().music().unlike(t.id);
            }
        }, () -> {
            t.liked = willLike;
            like.setText(willLike ? "♥" : "♡");
        }, err -> Ui.error(Ui.message(err)));
    }

    private void play(Track t) {
        if (t.audioUrl == null || t.audioUrl.isBlank()) {
            Ui.error("Нет аудио-ссылки");
            return;
        }
        try {
            if (player != null) {
                player.dispose();
            }
            String url = t.audioUrl.startsWith("http") ? t.audioUrl : ApiConfig.getBaseUrl() + "/" + t.audioUrl;
            player = new MediaPlayer(new Media(url));
            player.play();
            nowPlaying.setText("Играет: " + t.title + " — " + t.artist);
            context.bc().music().recordPlay(t.id);
        } catch (RuntimeException e) {
            Ui.error("Не удалось воспроизвести: " + e.getMessage());
        }
    }
}
