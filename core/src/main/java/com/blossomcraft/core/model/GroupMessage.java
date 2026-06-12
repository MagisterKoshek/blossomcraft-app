package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A message within a group/channel from {@code group_messages.php}. */
public class GroupMessage {
    public String id;
    @SerializedName(value = "groupId", alternate = {"group_id"})
    public String groupId;
    @SerializedName(value = "userId", alternate = {"user_id"})
    public String userId;
    @SerializedName(value = "userName", alternate = {"user_name", "name"})
    public String userName;
    public String text;
    public String attachment;

    @SerializedName("roleDisplay")
    public String roleDisplay;
    @SerializedName("roleBgColor")
    public String roleBgColor;
    @SerializedName("roleEmoji")
    public String roleEmoji;
    public String avatarEmoji;
    public String avatarColor;
    public String createdAt;
}
