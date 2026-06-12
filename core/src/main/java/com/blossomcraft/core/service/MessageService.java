package com.blossomcraft.core.service;

import com.blossomcraft.core.model.Message;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.Json;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Direct messages against {@code messages.php}. */
public class MessageService {

    private final ApiClient api;

    public MessageService(ApiClient api) {
        this.api = api;
    }

    /** Fetches all DMs newer than {@code lastId} (pass 0 for the full history). */
    public List<Message> fetchMessages(long lastId) {
        return Json.list(api.get("/messages.php?last_id=" + lastId), "messages", Message.class);
    }

    public String send(String toId, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("toId", toId);
        body.put("text", text);
        JsonObject res = api.post("/messages.php", body);
        return Json.string(res, "id");
    }

    public void markRead(String fromId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fromId", fromId);
        api.put("/messages.php", body);
    }

    public void edit(String id, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("text", text);
        api.put("/messages.php", body);
    }

    public void deleteMessage(String id) {
        api.delete("/messages.php", Map.of("id", id));
    }

    /** Clears a conversation. {@code scope} is {@code "me"} or {@code "both"}. */
    public void clearChat(String chatId, String scope) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chatId", chatId);
        body.put("scope", scope);
        api.delete("/messages.php", body);
    }
}
