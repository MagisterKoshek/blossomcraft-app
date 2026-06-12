package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A BlossomCraft account, mirroring the website's {@code User} interface. */
public class User {
    public String id;
    public String name;
    public String email;
    public String roleId;

    @SerializedName("role_display")
    public String roleDisplay;
    @SerializedName("role_bg_color")
    public String roleBgColor;
    @SerializedName("role_text_color")
    public String roleTextColor;
    @SerializedName("role_emoji")
    public String roleEmoji;

    public String[] permissions;
    public String createdAt;
    public String avatarEmoji;
    public String avatarColor;
    public String avatarBase64;
    public String avatarUrl;
    public String bio;
    public String status;
    public String lastSeen;

    @SerializedName("email_verified")
    public Integer emailVerified;
    public Double rating;
    @SerializedName("rating_count")
    public Integer ratingCount;

    public boolean hasPermission(String perm) {
        if (permissions == null) {
            return false;
        }
        for (String p : permissions) {
            if ("*".equals(p) || p.equals(perm)) {
                return true;
            }
        }
        return false;
    }

    public String displayName() {
        return name != null ? name : (email != null ? email : "User");
    }
}
