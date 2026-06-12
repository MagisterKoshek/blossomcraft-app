package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.ApiConfig;
import com.blossomcraft.core.model.Video;
import com.blossomcraft.desktop.AppContext;
import com.blossomcraft.desktop.Async;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.util.List;

/** Vertical video feed (TikTok-style) with like and inline playback. */
public class VideosPage implements Page {

    private final AppContext context;
    private final VBox root = new VBox(12);
    private final VBox feed = new VBox(16);
    private MediaPlayer player;

    public VideosPage(AppContext context) {
        this.context = context;
        Button refresh = new Button("Обновить");
        refresh.setOnAction(e -> load());
        HBox header = new HBox(12, Ui.h1("Видео"), Ui.grow(), refresh);
        header.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(header, feed);
        VBox.setVgrow(feed, Priority.ALWAYS);
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
        feed.getChildren().setAll(Ui.muted("Загрузка..."));
        Async.run(() -> context.bc().videos().feed(), this::render,
                err -> feed.getChildren().setAll(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private void render(List<Video> videos) {
        feed.getChildren().clear();
        if (videos.isEmpty()) {
            feed.getChildren().add(Ui.muted("Видео нет"));
            return;
        }
        for (Video v : videos) {
            feed.getChildren().add(card(v));
        }
    }

    private Node card(Video v) {
        Label title = Ui.h2(v.title == null ? "Без названия" : v.title);
        Label author = Ui.muted("@" + (v.userName == null ? v.userId : v.userName)
                + "   👁 " + v.views + "   ♥ " + v.likes);
        Label desc = new Label(v.description == null ? "" : v.description);
        desc.setWrapText(true);

        Button play = new Button("▶ Смотреть");
        Button like = new Button(v.liked ? "♥ Нравится" : "♡ Нравится");
        like.setOnAction(e -> Async.run(() -> context.bc().videos().toggleLike(v.id),
                liked -> {
                    v.liked = liked;
                    like.setText(liked ? "♥ Нравится" : "♡ Нравится");
                },
                err -> Ui.error(Ui.message(err))));

        VBox card = Ui.card(title, author, desc, new HBox(10, play, like));
        card.setMaxWidth(560);

        play.setOnAction(e -> {
            String url = v.videoUrl == null ? null
                    : (v.videoUrl.startsWith("http") ? v.videoUrl : ApiConfig.getBaseUrl() + "/" + v.videoUrl);
            if (url == null) {
                Ui.error("Нет ссылки на видео");
                return;
            }
            try {
                if (player != null) {
                    player.dispose();
                }
                player = new MediaPlayer(new Media(url));
                MediaView view = new MediaView(player);
                view.setFitWidth(520);
                view.setPreserveRatio(true);
                card.getChildren().add(view);
                player.play();
            } catch (RuntimeException ex) {
                Ui.error("Не удалось воспроизвести: " + ex.getMessage());
            }
        });
        return card;
    }
}
