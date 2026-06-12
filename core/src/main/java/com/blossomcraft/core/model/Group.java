package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/**
 * A Telegram-style group or channel from {@code groups.php}.
 * {@code type} is either {@code "group"} or {@code "channel"}.
 */
public class Group {
    public String id;
    public String name;
    public String description;
    public String type;
    public String avatar;

    @SerializedName(value = "ownerId", alternate = {"owner_id"})
    public String ownerId;
    @SerializedName(value = "inviteCode", alternate = {"invite_code"})
    public String inviteCode;

    /** Current user's role within this group: owner / admin / member. */
    public String role;
    @SerializedName(value = "memberCount", alternate = {"member_count", "members"})
    public Integer memberCount;

    public String preview;
    public Integer unread;
    public String lastMessageAt;

    public boolean isChannel() {
        return "channel".equalsIgnoreCase(type);
    }

    public boolean canPost(String userRole) {
        if (!isChannel()) {
            return true;
        }
        return "owner".equals(userRole) || "admin".equals(userRole);
    }
}
