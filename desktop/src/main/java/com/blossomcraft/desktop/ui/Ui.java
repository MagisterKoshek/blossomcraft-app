package com.blossomcraft.desktop.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Small reusable JavaFX UI helpers shared across pages. */
public final class Ui {

    private Ui() {
    }

    public static Label h1(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("h1");
        return label;
    }

    public static Label h2(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("h2");
        return label;
    }

    public static Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    public static Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("btn-primary");
        return button;
    }

    public static VBox card(javafx.scene.Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add("glass-card");
        return box;
    }

    public static Region grow() {
        Region region = new Region();
        VBox.setVgrow(region, javafx.scene.layout.Priority.ALWAYS);
        return region;
    }

    public static StackPane centered(javafx.scene.Node node) {
        StackPane pane = new StackPane(node);
        StackPane.setAlignment(node, Pos.CENTER);
        return pane;
    }

    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Ошибка");
        alert.showAndWait();
    }

    public static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static String message(Throwable t) {
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
