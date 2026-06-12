package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A direct message from {@code messages.php}. */
public class Message {
    public String id;
    public String fromId;
    public String toId;
    public String text;
    public String attachment;
    public String attachmentType;

    @SerializedName("fromName")
    public String fromName;
    @SerializedName("fromRoleDisplay")
    public String fromRoleDisplay;
    @SerializedName("fromRoleBgColor")
    public String fromRoleBgColor;
    @SerializedName("fromRoleTextColor")
    public String fromRoleTextColor;
    @SerializedName("fromRoleEmoji")
    public String fromRoleEmoji;

    @SerializedName(value = "isRead", alternate = {"is_read", "read"})
    public Integer isRead;
    public String createdAt;
}
