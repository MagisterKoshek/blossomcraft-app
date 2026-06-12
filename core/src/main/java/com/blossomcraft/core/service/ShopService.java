package com.blossomcraft.core.service;

import com.blossomcraft.core.model.Product;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.Json;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shop / products operations against {@code products.php}. */
public class ShopService {

    private final ApiClient api;

    public ShopService(ApiClient api) {
        this.api = api;
    }

    public List<Product> listProducts(long lastId) {
        return Json.list(api.get("/products.php?last_id=" + lastId), "products", Product.class);
    }

    public List<Product> listMyProducts() {
        return Json.list(api.get("/products.php?my=1"), "products", Product.class);
    }

    public String createProduct(String name, double price, String description,
                                String category, String images, String videoUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("price", price);
        body.put("description", description);
        body.put("category", category);
        body.put("images", images);
        body.put("videoUrl", videoUrl);
        JsonObject res = api.post("/products.php", body);
        return Json.string(res, "id");
    }

    public void updateProduct(String id, Map<String, Object> fields) {
        Map<String, Object> body = new LinkedHashMap<>(fields);
        body.put("id", id);
        api.put("/products.php", body);
    }

    public void setStatus(String id, String status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("status", status);
        api.put("/products.php", body);
    }

    public void deleteProduct(String id) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        api.delete("/products.php", body);
    }
}
