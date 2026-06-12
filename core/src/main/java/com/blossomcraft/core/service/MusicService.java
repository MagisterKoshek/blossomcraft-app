package com.blossomcraft.core.service;

import com.blossomcraft.core.model.Playlist;
import com.blossomcraft.core.model.Track;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.Json;
import com.blossomcraft.core.net.Multipart;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Music platform operations: tracks, likes, favorites, plays, playlists. */
public class MusicService {

    private final ApiClient api;

    public MusicService(ApiClient api) {
        this.api = api;
    }

    // ─── Tracks ───────────────────────────────────────────────────────────
    public List<Track> publicTracks() {
        return Json.list(api.get("/music.php?action=public"), "tracks", Track.class);
    }

    public List<Track> myTracks() {
        return Json.list(api.get("/music.php?action=mine"), "tracks", Track.class);
    }

    public List<Track> pendingTracks() {
        return Json.list(api.get("/music.php?action=pending"), "tracks", Track.class);
    }

    public List<Track> allTracks() {
        return Json.list(api.get("/music.php?action=all"), "tracks", Track.class);
    }

    public List<Track> artistTracks(String userId) {
        return Json.list(api.get("/music.php?action=artist&user_id=" + userId), "tracks", Track.class);
    }

    public JsonObject uploadTrack(String title, String artist, Multipart.FilePart audio,
                                  Multipart.FilePart cover) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", title);
        fields.put("artist", artist);
        if (cover != null) {
            return api.postMultipart("/music.php", fields, audio, cover);
        }
        return api.postMultipart("/music.php", fields, audio);
    }

    public void deleteTrack(String id) {
        api.delete("/music.php", Map.of("id", id));
    }

    public void moderateTrack(String id, String status, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("status", status);
        body.put("reason", reason);
        api.put("/music.php", body);
    }

    // ─── Likes / favorites / plays ────────────────────────────────────────
    public void like(String trackId) {
        api.post("/music_likes.php", Map.of("trackId", trackId));
    }

    public void unlike(String trackId) {
        api.delete("/music_likes.php", Map.of("trackId", trackId));
    }

    public List<Track> likedTracks() {
        return Json.list(api.get("/music_likes.php?action=mine"), "tracks", Track.class);
    }

    public List<Track> topTracks(int limit) {
        return Json.list(api.get("/music_likes.php?action=top&limit=" + limit), "tracks", Track.class);
    }

    public void favorite(String trackId) {
        api.post("/music_favorites.php", Map.of("trackId", trackId));
    }

    public void unfavorite(String trackId) {
        api.delete("/music_favorites.php", Map.of("trackId", trackId));
    }

    public List<Track> favoriteTracks() {
        return Json.list(api.get("/music_favorites.php?action=mine"), "tracks", Track.class);
    }

    public void recordPlay(String trackId) {
        try {
            api.post("/music_plays.php", Map.of("trackId", trackId));
        } catch (RuntimeException ignored) {
            // Play counting is best-effort.
        }
    }

    // ─── Playlists ────────────────────────────────────────────────────────
    public List<Playlist> myPlaylists() {
        return Json.list(api.get("/playlists.php?action=mine"), "playlists", Playlist.class);
    }

    public List<Playlist> publicPlaylists() {
        return Json.list(api.get("/playlists.php?action=public"), "playlists", Playlist.class);
    }

    public List<Track> playlistTracks(String id) {
        return Json.list(api.get("/playlists.php?action=tracks&id=" + id), "tracks", Track.class);
    }

    public JsonObject createPlaylist(String name, String description, boolean isPublic) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("isPublic", isPublic);
        return api.post("/playlists.php", body);
    }

    public void addTrackToPlaylist(String playlistId, String trackId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "add_track");
        body.put("playlistId", playlistId);
        body.put("trackId", trackId);
        api.put("/playlists.php", body);
    }

    public void removeTrackFromPlaylist(String playlistId, String trackId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "remove_track");
        body.put("playlistId", playlistId);
        body.put("trackId", trackId);
        api.put("/playlists.php", body);
    }

    public void deletePlaylist(String id) {
        api.delete("/playlists.php", Map.of("id", id));
    }
}
