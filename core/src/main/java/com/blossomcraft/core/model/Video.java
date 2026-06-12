package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A short-form video from {@code videos.php} (TikTok-style feed). */
public class Video {
    public String id;
    @SerializedName(value = "userId", alternate = {"user_id"})
    public String userId;
    @SerializedName(value = "userName", alternate = {"user_name", "name"})
    public String userName;
    public String title;
    public String description;
    @SerializedName(value = "videoUrl", alternate = {"video_url"})
    public String videoUrl;
    @SerializedName(value = "thumbUrl", alternate = {"thumb_url"})
    public String thumbUrl;
    public int views;
    public int likes;
    public boolean liked;
    public Integer comments;
    public String createdAt;
}
