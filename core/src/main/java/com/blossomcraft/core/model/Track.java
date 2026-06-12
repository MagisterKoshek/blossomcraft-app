package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A music track. Field names mirror the JSON emitted by {@code music.php}. */
public class Track {
    public String id;
    @SerializedName(value = "userId", alternate = {"user_id"})
    public String userId;
    public String title;
    public String artist;

    @SerializedName(value = "audioUrl", alternate = {"audio_url", "fileUrl", "file_url"})
    public String audioUrl;
    @SerializedName(value = "coverUrl", alternate = {"cover_url"})
    public String coverUrl;

    public String status;
    public boolean liked;
    public boolean favorite;
    public int plays;
    public int likes;
    public String createdAt;
    public String rejectReason;
}
