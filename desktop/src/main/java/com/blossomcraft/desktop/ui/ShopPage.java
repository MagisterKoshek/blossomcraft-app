package com.blossomcraft.desktop.ui;

import com.blossomcraft.core.model.Product;
import com.blossomcraft.desktop.AppContext;
import com.blossomcraft.desktop.Async;
import com.blossomcraft.core.service.ShopService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Shop / products page: browse listings with search, mirroring the website's home grid. */
public class ShopPage implements Page {

    private final AppContext context;
    private final VBox root = new VBox(16);
    private final FlowPane grid = new FlowPane(16, 16);
    private final TextField search = new TextField();
    private List<Product> products = List.of();

    public ShopPage(AppContext context) {
        this.context = context;

        search.setPromptText("Поиск товаров...");
        HBox.setHgrow(search, Priority.ALWAYS);
        search.textProperty().addListener((o, a, b) -> render());

        Button refresh = new Button("Обновить");
        refresh.setOnAction(e -> load());

        HBox header = new HBox(12, Ui.h1("Магазин"), Ui.grow(), search, refresh);
        header.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(header, grid);
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
        grid.getChildren().setAll(Ui.muted("Загрузка..."));
        ShopService shop = context.bc().shop();
        Async.run(() -> shop.listProducts(0), list -> {
            products = list;
            render();
        }, err -> grid.getChildren().setAll(Ui.muted("Ошибка: " + Ui.message(err))));
    }

    private void render() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        grid.getChildren().clear();
        boolean any = false;
        for (Product p : products) {
            if (!query.isEmpty() && (p.name == null || !p.name.toLowerCase().contains(query))) {
                continue;
            }
            grid.getChildren().add(card(p));
            any = true;
        }
        if (!any) {
            grid.getChildren().add(Ui.muted("Товары не найдены"));
        }
    }

    private Node card(Product p) {
        Label name = Ui.h2(p.name == null ? "—" : p.name);
        Label price = new Label(p.price == null ? "" : p.price + " ₽");
        price.getStyleClass().add("neon");
        Label seller = Ui.muted("Продавец: " + (p.sellerName == null ? "?" : p.sellerName));
        Label desc = Ui.muted(p.description == null ? "" : p.description);
        desc.setWrapText(true);
        VBox card = Ui.card(name, price, seller, desc);
        card.setPrefWidth(240);
        return card;
    }
}
