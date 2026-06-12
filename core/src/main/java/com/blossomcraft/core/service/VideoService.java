package com.blossomcraft.core.service;

import com.blossomcraft.core.model.Comment;
import com.blossomcraft.core.model.Video;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.Json;
import com.blossomcraft.core.net.Multipart;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Short-form video feed operations against {@code videos.php}. */
public class VideoService {

    private final ApiClient api;

    public VideoService(ApiClient api) {
        this.api = api;
    }

    public List<Video> feed() {
        return Json.list(api.get("/videos.php?action=feed"), "videos", Video.class);
    }

    public List<Video> myVideos() {
        return Json.list(api.get("/videos.php?action=my"), "videos", Video.class);
    }

    public List<Comment> comments(String videoId) {
        return Json.list(api.get("/videos.php?action=comments&videoId=" + videoId), "comments", Comment.class);
    }

    public void comment(String videoId, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "comment");
        body.put("videoId", videoId);
        body.put("text", text);
        api.post("/videos.php", body);
    }

    /** Toggles a like; returns the new liked state reported by the server. */
    public boolean toggleLike(String videoId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "like");
        body.put("videoId", videoId);
        JsonObject res = api.put("/videos.php", body);
        return Json.bool(res, "liked");
    }

    public void toggleHidden(String videoId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "hide");
        body.put("videoId", videoId);
        api.put("/videos.php", body);
    }

    /**
     * Single-shot upload (small files). For large files the website uploads in
     * chunks via {@code action=upload_chunk} then {@code action=finish}; use
     * {@link #uploadChunk} / {@link #finishUpload} for that flow.
     */
    public JsonObject upload(String title, String description, Multipart.FilePart video) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", title);
        fields.put("description", description);
        return api.postMultipart("/videos.php", fields, video);
    }

    public JsonObject uploadChunk(String uploadId, int index, Multipart.FilePart chunk) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("action", "upload_chunk");
        fields.put("uploadId", uploadId);
        fields.put("index", String.valueOf(index));
        return api.postMultipart("/videos.php", fields, chunk);
    }

    public JsonObject finishUpload(String uploadId, String title, String description) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("action", "finish");
        fields.put("uploadId", uploadId);
        fields.put("title", title);
        fields.put("description", description);
        return api.postMultipart("/videos.php", fields);
    }
}
