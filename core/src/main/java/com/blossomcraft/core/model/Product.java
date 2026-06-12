package com.blossomcraft.core.model;

import com.google.gson.annotations.SerializedName;

/** A shop listing. The backend returns {@code SELECT p.*} so field names mirror columns. */
public class Product {
    public String id;
    public String name;
    public String description;
    public Double price;
    public String category;

    /** JSON-encoded array of image URLs/base64 strings, as stored by the site. */
    public String images;
    public String videoUrl;
    public String status;

    @SerializedName(value = "sellerId", alternate = {"seller_id", "userId", "user_id"})
    public String sellerId;
    @SerializedName(value = "sellerName", alternate = {"seller_name"})
    public String sellerName;
    @SerializedName("sellerStatus")
    public String sellerStatus;

    public String createdAt;
}
