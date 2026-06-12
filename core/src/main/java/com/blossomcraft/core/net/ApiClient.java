package com.blossomcraft.core.net;

import com.blossomcraft.core.ApiConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Thin HTTP client built on {@link java.net.HttpURLConnection} that mirrors the
 * website's {@code request()} wrapper: it attaches the bearer token, sends/receives
 * JSON, clears the token on 401, and surfaces backend {@code {"error": ...}} messages.
 *
 * <p>{@code HttpURLConnection} is used in place of {@code java.net.http.HttpClient}
 * because the latter does not exist on Android (it is not backported by core-library
 * desugaring) — every call would throw {@code NoClassDefFoundError}. {@code
 * HttpURLConnection} is available on both desktop and Android.</p>
 *
 * <p>All calls are blocking; callers should run them off the UI thread.</p>
 */
public class ApiClient {

    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private final TokenStore tokenStore;

    public ApiClient(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public TokenStore tokenStore() {
        return tokenStore;
    }

    // ─── Public verbs ───────────────────────────────────────────────────────

    public JsonObject get(String path) {
        return send("GET", path, null);
    }

    public JsonObject post(String path, Object body) {
        return send("POST", path, body);
    }

    public JsonObject put(String path, Object body) {
        return send("PUT", path, body);
    }

    public JsonObject delete(String path, Object body) {
        return send("DELETE", path, body);
    }

    /** Typed GET that deserialises the full response body into {@code type}. */
    public <T> T getAs(String path, Type type) {
        JsonObject obj = get(path);
        return Json.GSON.fromJson(obj, type);
    }

    /** Multipart upload (used for music/video/avatar uploads that send files). */
    public JsonObject postMultipart(String path, Map<String, String> fields,
                                    Multipart.FilePart... files) {
        Multipart multipart = new Multipart();
        if (fields != null) {
            fields.forEach(multipart::addField);
        }
        if (files != null) {
            for (Multipart.FilePart file : files) {
                multipart.addFile(file);
            }
        }
        return execute("POST", path,
                "multipart/form-data; boundary=" + multipart.boundary(),
                multipart.build());
    }

    // ─── Internals ──────────────────────────────────────────────────────────

    private JsonObject send(String method, String path, Object body) {
        byte[] payload = body == null ? null
                : Json.GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        return execute(method, path, "application/json", payload);
    }

    private JsonObject execute(String method, String path, String contentType, byte[] payload) {
        HttpURLConnection conn = null;
        int status;
        String responseBody;
        try {
            URL url = new URL(ApiConfig.url(path));
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(false);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Accept", "application/json");
            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType);
            }

            String token = tokenStore.getToken();
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (payload != null) {
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode((long) payload.length);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(payload);
                }
            }

            status = conn.getResponseCode();
            responseBody = readBody(conn, status);
        } catch (IOException e) {
            throw new ApiException(0, "Network error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return handle(status, responseBody);
    }

    private JsonObject handle(int status, String body) {
        if (status == 401) {
            tokenStore.clearToken();
            throw new AuthExpiredException();
        }

        JsonObject json = tryParse(body);

        if (status < 200 || status >= 300) {
            String message = json != null && json.has("error") && json.get("error").isJsonPrimitive()
                    ? json.get("error").getAsString()
                    : "Request failed (HTTP " + status + ")";
            throw new ApiException(status, message);
        }

        // On success a body that does not parse as JSON is a real failure (e.g. a
        // shared-hosting PHP warning printed before the JSON). Surface it rather
        // than silently returning an empty object → blank screens.
        if (json == null) {
            throw new ApiException(status,
                    "Invalid JSON response (HTTP " + status + "): " + snippet(body));
        }
        return json;
    }

    private static String readBody(HttpURLConnection conn, int status) throws IOException {
        InputStream stream = (status >= 200 && status < 400)
                ? conn.getInputStream()
                : conn.getErrorStream();
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Parses {@code body} into a {@link JsonObject}. Bare top-level arrays or
     * primitives are wrapped under {@code {"data": ...}} (matching the website's
     * handling of endpoints that return a raw array). A blank body yields an empty
     * object; only a genuine parse failure returns {@code null}.
     */
    private static JsonObject tryParse(String body) {
        if (body == null || body.isBlank()) {
            return new JsonObject();
        }
        try {
            JsonElement element = JsonParser.parseString(body);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            JsonObject wrapper = new JsonObject();
            wrapper.add("data", element);
            return wrapper;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String snippet(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed;
    }
}
