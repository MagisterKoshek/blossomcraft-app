package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.model.Group;
import com.blossomcraft.core.model.GroupMember;
import com.blossomcraft.core.model.GroupMessage;
import com.blossomcraft.core.model.Message;
import com.blossomcraft.core.model.User;
import com.blossomcraft.desktop.AppContext;
import com.blossomcraft.desktop.Async;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Messaging hub mirroring the website: a "Direct" tab for DMs and a
 * "Groups & Channels" tab for the Telegram-style groups/channels. Channels are
 * post-restricted to owners/admins, matching the backend behaviour.
 */
public class MessagesPage implements Page {

    private final AppContext context;
    private final TabPane root = new TabPane();

    // Direct messages state
    private final ListView<User> peopleList = new ListView<>();
    private final VBox dmThread = new VBox(8);
    private final TextField dmInput = new TextField();
    private User activePeer;

    // Groups state
    private final ListView<Group> groupList = new ListView<>();
    private final VBox groupThread = new VBox(8);
    private final TextField groupInput = new TextField();
    private Group activeGroup;

    public MessagesPage(AppContext context) {
        this.context = context;
        root.getTabs().addAll(directTab(), groupsTab());
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void onShown() {
        loadPeople();
        loadGroups();
    }

    // ─── Direct messages ──────────────────────────────────────────────────
    private Tab directTab() {
        peopleList.setPrefWidth(240);
        peopleList.setCellFactory(v -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : (u.roleEmoji != null ? u.roleEmoji + " " : "") + u.displayName());
            }
        });
        peopleList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            activePeer = b;
            if (b != null) {
                loadDmThread(b);
            }
        });

        ScrollPane threadScroll = new ScrollPane(dmThread);
        threadScroll.setFitToWidth(true);
        VBox.setVgrow(threadScroll, Priority.ALWAYS);

        dmInput.setPromptText("Сообщение...");
        HBox.setHgrow(dmInput, Priority.ALWAYS);
        Button send = Ui.primaryButton("Отправить");
        send.setOnAction(e -> sendDm());
        dmInput.setOnAction(e -> sendDm());

        VBox right = new VBox(8, threadScroll, new HBox(8, dmInput, send));
        VBox.setVgrow(threadScroll, Priority.ALWAYS);

        HBox content = new HBox(12, peopleList, right);
        HBox.setHgrow(right, Priority.ALWAYS);
        content.setPadding(new javafx.geometry.Insets(8));

        Tab tab = new Tab("Личные");
        tab.setClosable(false);
        tab.setContent(content);
        return tab;
    }

    private void loadPeople() {
        Async.run(() -> context.bc().profile().searchUsers(null),
                list -> peopleList.getItems().setAll(list),
                err -> peopleList.setPlaceholder(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private void loadDmThread(User peer) {
        dmThread.getChildren().setAll(Ui.muted("Загрузка..."));
        Async.run(() -> context.bc().messages().fetchMessages(0), all -> {
            dmThread.getChildren().clear();
            String me = context.bc().currentUser() != null ? context.bc().currentUser().id : "";
            for (Message m : all) {
                boolean involves = peer.id.equals(m.fromId) || peer.id.equals(m.toId);
                if (!involves) {
                    continue;
                }
                boolean mine = me.equals(m.fromId);
                dmThread.getChildren().add(bubble(m.text, mine, m.fromName));
            }
            if (dmThread.getChildren().isEmpty()) {
                dmThread.getChildren().add(Ui.muted("Нет сообщений"));
            }
        }, err -> dmThread.getChildren().setAll(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private void sendDm() {
        if (activePeer == null || dmInput.getText().isBlank()) {
            return;
        }
        String text = dmInput.getText().trim();
        dmInput.clear();
        Async.run(() -> context.bc().messages().send(activePeer.id, text),
                id -> loadDmThread(activePeer),
                err -> Ui.error(Ui.message(err)));
    }

    // ─── Groups & channels ────────────────────────────────────────────────
    private Tab groupsTab() {
        groupList.setPrefWidth(240);
        groupList.setCellFactory(v -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Group g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty || g == null ? null
                        : (g.isChannel() ? "📢 " : "👥 ") + g.name
                        + (g.unread != null && g.unread > 0 ? "  (" + g.unread + ")" : ""));
            }
        });
        groupList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            activeGroup = b;
            if (b != null) {
                loadGroupThread(b);
            }
        });

        Button create = new Button("＋ Создать");
        create.setOnAction(e -> createGroup());
        Button settings = new Button("⚙ Настройки");
        settings.setOnAction(e -> openGroupSettings());

        VBox left = new VBox(8, new HBox(8, create, settings), groupList);
        VBox.setVgrow(groupList, Priority.ALWAYS);

        ScrollPane threadScroll = new ScrollPane(groupThread);
        threadScroll.setFitToWidth(true);

        groupInput.setPromptText("Сообщение...");
        HBox.setHgrow(groupInput, Priority.ALWAYS);
        Button send = Ui.primaryButton("Отправить");
        send.setOnAction(e -> sendGroup());
        groupInput.setOnAction(e -> sendGroup());

        VBox right = new VBox(8, threadScroll, new HBox(8, groupInput, send));
        VBox.setVgrow(threadScroll, Priority.ALWAYS);

        HBox content = new HBox(12, left, right);
        HBox.setHgrow(right, Priority.ALWAYS);
        content.setPadding(new javafx.geometry.Insets(8));

        Tab tab = new Tab("Группы и каналы");
        tab.setClosable(false);
        tab.setContent(content);
        return tab;
    }

    private void loadGroups() {
        Async.run(() -> context.bc().groups().myGroups(),
                list -> groupList.getItems().setAll(list),
                err -> groupList.setPlaceholder(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private void loadGroupThread(Group g) {
        groupThread.getChildren().setAll(Ui.muted("Загрузка..."));
        Async.run(() -> context.bc().groups().messages(g.id, 0), msgs -> {
            groupThread.getChildren().clear();
            String me = context.bc().currentUser() != null ? context.bc().currentUser().id : "";
            for (GroupMessage m : msgs) {
                boolean mine = me.equals(m.userId);
                groupThread.getChildren().add(bubble(m.text, mine, m.userName));
            }
            if (groupThread.getChildren().isEmpty()) {
                groupThread.getChildren().add(Ui.muted("Нет сообщений"));
            }
            boolean canPost = g.canPost(g.role);
            groupInput.setDisable(!canPost);
            groupInput.setPromptText(canPost ? "Сообщение..." : "Только администраторы могут писать в канал");
        }, err -> groupThread.getChildren().setAll(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private void sendGroup() {
        if (activeGroup == null || groupInput.getText().isBlank()) {
            return;
        }
        String text = groupInput.getText().trim();
        groupInput.clear();
        Async.run(() -> context.bc().groups().sendMessage(activeGroup.id, text),
                id -> loadGroupThread(activeGroup),
                err -> Ui.error(Ui.message(err)));
    }

    // ─── Group / channel settings ─────────────────────────────────────────
    private void openGroupSettings() {
        Group g = activeGroup;
        if (g == null) {
            Ui.info("Сначала выберите группу или канал слева.");
            return;
        }
        boolean isOwner = "owner".equals(g.role);
        boolean canManage = isOwner || "admin".equals(g.role);
        if (!canManage) {
            Ui.info("Управление доступно только владельцу или администратору.");
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (root.getScene() != null) {
            stage.initOwner(root.getScene().getWindow());
        }
        stage.setTitle("Настройки: " + g.name);

        Label title = Ui.h2((g.isChannel() ? "📢 " : "👥 ") + g.name);

        TextField nameField = new TextField(g.name);
        TextArea descArea = new TextArea(g.description == null ? "" : g.description);
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        Button save = Ui.primaryButton("Сохранить");
        save.setOnAction(e -> Async.run(
                () -> context.bc().groups().update(g.id, nameField.getText().trim(), descArea.getText().trim(), null),
                () -> {
                    g.name = nameField.getText().trim();
                    g.description = descArea.getText().trim();
                    loadGroups();
                    Ui.info("Сохранено");
                },
                err -> Ui.error(Ui.message(err))));

        Label codeLabel = new Label(g.inviteCode == null ? "—" : g.inviteCode);
        Button regen = new Button("Обновить код");
        regen.setOnAction(e -> Async.run(
                () -> context.bc().groups().regenerateCode(g.id),
                code -> {
                    g.inviteCode = code;
                    codeLabel.setText(code == null ? "—" : code);
                },
                err -> Ui.error(Ui.message(err))));
        HBox codeRow = new HBox(10, new Label("Код приглашения:"), codeLabel, Ui.grow(), regen);
        codeRow.setAlignment(Pos.CENTER_LEFT);

        VBox membersBox = new VBox(8);

        HBox actions = new HBox(10);
        if (isOwner) {
            Button delete = new Button("Удалить " + (g.isChannel() ? "канал" : "группу"));
            delete.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Удалить безвозвратно? Это действие нельзя отменить.",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText(null);
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        Async.run(() -> context.bc().groups().delete(g.id),
                                () -> {
                                    stage.close();
                                    activeGroup = null;
                                    groupThread.getChildren().clear();
                                    loadGroups();
                                },
                                err -> Ui.error(Ui.message(err)));
                    }
                });
            });
            actions.getChildren().add(delete);
        }

        VBox content = new VBox(14,
                title,
                new Label("Название"), nameField,
                new Label("Описание"), descArea,
                save,
                new Separator(),
                codeRow,
                new Separator(),
                Ui.h2("Участники"), membersBox,
                new Separator(),
                actions);
        content.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        Scene scene = new Scene(scroll, 480, 660);
        if (root.getScene() != null) {
            scene.getStylesheets().addAll(root.getScene().getStylesheets());
            if (root.getScene().getRoot() != null) {
                content.getStyleClass().addAll(root.getScene().getRoot().getStyleClass());
            }
        }
        stage.setScene(scene);
        loadMembers(g, membersBox, isOwner);
        stage.show();
    }

    private void loadMembers(Group g, VBox box, boolean isOwner) {
        box.getChildren().setAll(Ui.muted("Загрузка..."));
        Async.run(() -> context.bc().groups().members(g.id), list -> {
            box.getChildren().clear();
            if (list.isEmpty()) {
                box.getChildren().add(Ui.muted("Нет участников"));
                return;
            }
            for (GroupMember m : list) {
                box.getChildren().add(memberRow(g, m, isOwner, box));
            }
        }, err -> box.getChildren().setAll(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private Node memberRow(Group g, GroupMember m, boolean isOwner, VBox box) {
        boolean memberIsOwner = "owner".equals(m.role);
        boolean memberIsAdmin = "admin".equals(m.role);
        String badge = memberIsOwner ? "👑" : memberIsAdmin ? "⭐" : "•";

        Label name = new Label(badge + " " + (m.userName != null ? m.userName : m.userId));
        Label roleLabel = Ui.muted(m.role == null ? "member" : m.role);
        HBox row = new HBox(10, new VBox(2, name, roleLabel), Ui.grow());
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("glass-card");

        // Role changes are owner-only (the backend enforces this too).
        if (isOwner && !memberIsOwner) {
            if (memberIsAdmin) {
                Button demote = new Button("Снять админа");
                demote.setOnAction(e -> Async.run(
                        () -> context.bc().groups().setRole(g.id, m.userId, "member"),
                        () -> loadMembers(g, box, isOwner),
                        err -> Ui.error(Ui.message(err))));
                row.getChildren().add(demote);
            } else {
                Button promote = new Button("Сделать админом");
                promote.setOnAction(e -> Async.run(
                        () -> context.bc().groups().setRole(g.id, m.userId, "admin"),
                        () -> loadMembers(g, box, isOwner),
                        err -> Ui.error(Ui.message(err))));
                row.getChildren().add(promote);
            }
        }

        // Owner can remove anyone but the owner; an admin can remove plain members only.
        boolean canKick = !memberIsOwner && (isOwner || !memberIsAdmin);
        if (canKick) {
            Button kick = new Button("Удалить");
            kick.setOnAction(e -> Async.run(
                    () -> context.bc().groups().kick(g.id, m.userId),
                    () -> loadMembers(g, box, isOwner),
                    err -> Ui.error(Ui.message(err))));
            row.getChildren().add(kick);
        }
        return row;
    }

    private void createGroup() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setHeaderText("Название");
        nameDialog.setContentText("Название группы/канала:");
        nameDialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) {
                return;
            }
            ChoiceDialog<String> typeDialog = new ChoiceDialog<>("group", List.of("group", "channel"));
            typeDialog.setHeaderText("Тип");
            typeDialog.setContentText("group = группа, channel = канал");
            typeDialog.showAndWait().ifPresent(type ->
                    Async.run(() -> context.bc().groups().create(name.trim(), type),
                            res -> loadGroups(),
                            err -> Ui.error(Ui.message(err))));
        });
    }

    private Node bubble(String text, boolean mine, String author) {
        Label content = new Label(text == null ? "" : text);
        content.setWrapText(true);
        content.getStyleClass().add(mine ? "bubble-out" : "bubble-in");
        VBox box = new VBox(2);
        if (!mine && author != null) {
            Label a = Ui.muted(author);
            box.getChildren().add(a);
        }
        box.getChildren().add(content);
        box.setMaxWidth(420);
        HBox wrapper = new HBox(box);
        wrapper.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return wrapper;
    }
}
