package com.blossomcraft.core.service;

import com.blossomcraft.core.model.User;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.Json;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin operations: user/role management, bans/mutes, logs, analytics, and the
 * command console. Available only to accounts with the relevant permissions —
 * the backend enforces this; the UI hides these screens for other users.
 */
public class AdminService {

    private final ApiClient api;

    public AdminService(ApiClient api) {
        this.api = api;
    }

    public List<User> allUsers() {
        return Json.list(api.get("/users.php"), "users", User.class);
    }

    public void setRole(String userId, String roleId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", userId);
        body.put("roleId", roleId);
        api.put("/users.php", body);
    }

    public void setUsername(String userId, String username) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", userId);
        body.put("username", username);
        api.put("/users.php", body);
    }

    public void deleteUser(String userId) {
        api.delete("/users.php", Map.of("userId", userId));
    }

    public JsonObject roles() {
        return api.get("/roles.php");
    }

    public JsonObject bans() {
        return api.get("/bans.php");
    }

    public void ban(String userId, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("reason", reason);
        api.post("/bans.php", body);
    }

    public void unban(String userId) {
        api.delete("/bans.php", Map.of("userId", userId));
    }

    public JsonObject logs() {
        return api.get("/logs.php");
    }

    public JsonObject analytics() {
        return api.get("/analytics.php");
    }

    /** Mass/admin actions, e.g. {@code clear_logs}, {@code reset_stats}. */
    public JsonObject adminAction(String action) {
        return api.post("/admin_actions.php", Map.of("action", action));
    }
}
