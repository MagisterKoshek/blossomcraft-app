package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A notification from {@code notifications.php}. */
public class Notification {
    public String id;
    public String type;
    public String title;
    public String text;
    public String link;
    @SerializedName(value = "isRead", alternate = {"is_read", "read"})
    public Integer isRead;
    public String createdAt;
}
