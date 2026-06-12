package com.blossomcraft.core.service;

import com.blossomcraft.core.model.Group;
import com.blossomcraft.core.model.GroupMember;
import com.blossomcraft.core.model.GroupMessage;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.Json;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Telegram-style groups & channels: {@code groups.php} for management and
 * {@code group_messages.php} for the conversation timeline.
 */
public class GroupService {

    private final ApiClient api;

    public GroupService(ApiClient api) {
        this.api = api;
    }

    public List<Group> myGroups() {
        return Json.list(api.get("/groups.php"), "groups", Group.class);
    }

    public List<GroupMember> members(String groupId) {
        return Json.list(api.get("/groups.php?action=members&groupId=" + groupId), "members", GroupMember.class);
    }

    /** Creates a group or channel; {@code type} is {@code "group"} or {@code "channel"}. */
    public JsonObject create(String name, String type) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", type);
        return api.post("/groups.php", body);
    }

    public void join(String groupId) {
        action("join", groupId, null);
    }

    public void leave(String groupId) {
        action("leave", groupId, null);
    }

    public void delete(String groupId) {
        action("delete", groupId, null);
    }

    public void kick(String groupId, String userId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "kick");
        body.put("groupId", groupId);
        body.put("userId", userId);
        api.put("/groups.php", body);
    }

    public void setRole(String groupId, String userId, String role) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "set_role");
        body.put("groupId", groupId);
        body.put("userId", userId);
        body.put("role", role);
        api.put("/groups.php", body);
    }

    public void update(String groupId, String name, String description, String avatar) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "update");
        body.put("groupId", groupId);
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (avatar != null) body.put("avatar", avatar);
        api.put("/groups.php", body);
    }

    public String regenerateCode(String groupId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "regenerate_code");
        body.put("groupId", groupId);
        JsonObject res = api.put("/groups.php", body);
        return Json.string(res, "inviteCode");
    }

    private void action(String action, String groupId, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", action);
        body.put("groupId", groupId);
        if (extra != null) {
            body.putAll(extra);
        }
        api.put("/groups.php", body);
    }

    // ─── group_messages.php ───────────────────────────────────────────────
    public List<GroupMessage> messages(String groupId, long lastId) {
        return Json.list(
                api.get("/group_messages.php?group_id=" + groupId + "&last_id=" + lastId),
                "messages", GroupMessage.class);
    }

    public String sendMessage(String groupId, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("groupId", groupId);
        body.put("text", text);
        JsonObject res = api.post("/group_messages.php", body);
        return Json.string(res, "id");
    }
}
