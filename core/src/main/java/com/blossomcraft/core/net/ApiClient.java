package com.blossomcraft.core.net;

import com.blossomcraft.core.ApiConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Thin HTTP client around {@link java.net.http.HttpClient} that mirrors the
 * website's {@code request()} wrapper: it attaches the bearer token, sends/receives
 * JSON, clears the token on 401, and surfaces backend {@code {"error": ...}} messages.
 *
 * <p>All calls are blocking; callers should run them off the UI thread.</p>
 */
public class ApiClient {

    private final HttpClient http;
    private final TokenStore tokenStore;

    public ApiClient(TokenStore tokenStore) {
        this(tokenStore, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public ApiClient(TokenStore tokenStore, HttpClient http) {
        this.tokenStore = tokenStore;
        this.http = http;
    }

    public TokenStore tokenStore() {
        return tokenStore;
    }

    // ─── Public verbs ───────────────────────────────────────────────────────

    public JsonObject get(String path) {
        return send("GET", path, null, null);
    }

    public JsonObject post(String path, Object body) {
        return send("POST", path, body, null);
    }

    public JsonObject put(String path, Object body) {
        return send("PUT", path, body, null);
    }

    public JsonObject delete(String path, Object body) {
        return send("DELETE", path, body, null);
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
        HttpRequest.Builder builder = baseRequest(path)
                .header("Content-Type", "multipart/form-data; boundary=" + multipart.boundary())
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart.build()));
        return execute(builder.build());
    }

    // ─── Internals ──────────────────────────────────────────────────────────

    private JsonObject send(String method, String path, Object body, Void ignored) {
        HttpRequest.Builder builder = baseRequest(path).header("Content-Type", "application/json");
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(Json.GSON.toJson(body), StandardCharsets.UTF_8);
        builder.method(method, publisher);
        return execute(builder.build());
    }

    private HttpRequest.Builder baseRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.url(path)))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json");
        String token = tokenStore.getToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private JsonObject execute(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ApiException(0, "Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Request interrupted");
        }

        int status = response.statusCode();
        if (status == 401) {
            tokenStore.clearToken();
            throw new AuthExpiredException();
        }

        JsonObject json = parse(response.body());
        if (status < 200 || status >= 300) {
            String message = json != null && json.has("error") && json.get("error").isJsonPrimitive()
                    ? json.get("error").getAsString()
                    : "Request failed (HTTP " + status + ")";
            throw new ApiException(status, message);
        }
        return json == null ? new JsonObject() : json;
    }

    private JsonObject parse(String body) {
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
}
