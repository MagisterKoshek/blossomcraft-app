package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A video comment from {@code videos.php?action=comments}. */
public class Comment {
    public String id;
    @SerializedName(value = "userId", alternate = {"user_id"})
    public String userId;
    @SerializedName(value = "userName", alternate = {"user_name", "name"})
    public String userName;
    public String text;
    public String createdAt;
    public String avatarEmoji;
    public String avatarColor;
}
