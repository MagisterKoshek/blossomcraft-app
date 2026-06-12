package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A member entry from {@code groups.php?action=members}. */
public class GroupMember {
    @SerializedName(value = "userId", alternate = {"user_id", "id"})
    public String userId;
    @SerializedName(value = "userName", alternate = {"user_name", "name"})
    public String userName;
    public String role;
    public String avatarEmoji;
    public String avatarColor;
}
