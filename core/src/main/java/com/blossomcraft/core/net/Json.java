package com.blossomcraft.core.net;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared Gson instance. The backend returns mixed naming (camelCase from PHP
 * arrays and snake_case from raw SQL columns), so models declare explicit
 * {@code @SerializedName} aliases where needed; Gson is configured to be lenient.
 */
public final class Json {

    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .serializeNulls()
            .setLenient()
            .create();

    private Json() {
    }

    /**
     * Deserialise an array member of {@code obj} into a typed list. The array is
     * looked up under {@code key} first; if absent it falls back to {@code "data"}
     * — which is where {@link ApiClient} wraps a response that was itself a bare
     * top-level array. This keeps the list from being silently empty when the live
     * server returns {@code [...]} instead of {@code {"key": [...]}}.
     */
    public static <T> List<T> list(JsonObject obj, String key, Class<T> type) {
        List<T> out = new ArrayList<>();
        if (obj == null) {
            return out;
        }
        JsonArray array = null;
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            array = obj.getAsJsonArray(key);
        } else if (obj.has("data") && obj.get("data").isJsonArray()) {
            array = obj.getAsJsonArray("data");
        }
        if (array == null) {
            return out;
        }
        for (JsonElement element : array) {
            out.add(GSON.fromJson(element, type));
        }
        return out;
    }

    /** Deserialise the named object member of {@code obj} into {@code type}. */
    public static <T> T object(JsonObject obj, String key, Class<T> type) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return GSON.fromJson(obj.get(key), type);
    }

    public static String string(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }

    public static int integer(JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public static boolean bool(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return false;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
