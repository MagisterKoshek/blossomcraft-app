package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A user playlist from {@code playlists.php}. */
public class Playlist {
    public String id;
    public String name;
    public String description;
    @SerializedName(value = "isPublic", alternate = {"is_public"})
    public boolean isPublic;
    @SerializedName(value = "userId", alternate = {"user_id"})
    public String userId;
    public Integer trackCount;
    public String createdAt;
}
