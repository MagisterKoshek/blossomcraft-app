package com.blossomcraft.core.service;

import com.blossomcraft.core.model.User;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.Json;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Profile viewing/editing, user search, avatar upload, and ratings. */
public class ProfileService {

    private final ApiClient api;

    public ProfileService(ApiClient api) {
        this.api = api;
    }

    public List<User> searchUsers(String query) {
        String path = query == null || query.isBlank()
                ? "/users.php"
                : "/users.php?search=" + urlEncode(query);
        return Json.list(api.get(path), "users", User.class);
    }

    public User getUser(String id) {
        return Json.object(api.get("/users.php?id=" + id), "user", User.class);
    }

    public void updateProfile(Map<String, Object> fields) {
        api.put("/profile.php", new LinkedHashMap<>(fields));
    }

    /** Uploads a base64-encoded avatar; returns the stored avatar reference. */
    public String uploadAvatar(String base64Image) {
        JsonObject res = api.post("/upload_avatar.php", Map.of("image", base64Image));
        String url = Json.string(res, "avatarUrl");
        return url != null ? url : Json.string(res, "avatarBase64");
    }

    public JsonObject userRating(String sellerId) {
        return api.get("/rating.php?action=user_rating&seller_id=" + sellerId);
    }

    public int usersCount() {
        return Json.integer(api.get("/profile.php"), "usersCount", 0);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
