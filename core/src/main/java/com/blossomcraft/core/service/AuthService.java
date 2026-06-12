package com.blossomcraft.core.service;

import com.blossomcraft.core.model.AuthResponse;
import com.blossomcraft.core.model.User;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.AuthExpiredException;
import com.blossomcraft.core.net.Json;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authentication against the existing PHP endpoints: email/password login &
 * registration, Google sign-in, session restore ({@code me.php}), logout, and
 * SMTP-based password reset ({@code change_password.php}).
 */
public class AuthService {

    private final ApiClient api;

    public AuthService(ApiClient api) {
        this.api = api;
    }

    private AuthResponse handleAuth(JsonObject obj) {
        AuthResponse res = Json.GSON.fromJson(obj, AuthResponse.class);
        if (res != null && res.token != null && !res.token.isBlank()) {
            api.tokenStore().setToken(res.token);
        }
        return res;
    }

    public AuthResponse login(String email, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        return handleAuth(api.post("/login.php", body));
    }

    public AuthResponse register(String name, String email, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        return handleAuth(api.post("/register.php", body));
    }

    /**
     * Google sign-in / linking. The website posts {@code {action, accessToken}}
     * to {@code google_auth.php}. {@code action} is typically {@code "login"} or
     * {@code "link"}; {@code accessToken} is the Google OAuth access token obtained
     * by the platform (Google Sign-In SDK on Android, browser flow on desktop).
     */
    public AuthResponse googleAuth(String action, String accessToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", action == null ? "login" : action);
        body.put("accessToken", accessToken);
        return handleAuth(api.post("/google_auth.php", body));
    }

    /** Restores the session from the stored token. Returns {@code null} when logged out. */
    public User restoreSession() {
        if (api.tokenStore().getToken() == null || api.tokenStore().getToken().isBlank()) {
            return null;
        }
        try {
            JsonObject obj = api.get("/me.php");
            if (obj.has("token") && obj.get("token").isJsonPrimitive()) {
                api.tokenStore().setToken(obj.get("token").getAsString());
            }
            return Json.object(obj, "user", User.class);
        } catch (AuthExpiredException e) {
            return null;
        }
    }

    public void logout() {
        try {
            api.get("/logout.php");
        } catch (RuntimeException ignored) {
            // The token is cleared regardless of network outcome.
        }
        api.tokenStore().clearToken();
    }

    /** Request a password-reset code be e-mailed to the account. */
    public JsonObject requestPasswordReset() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "request");
        return api.post("/change_password.php", body);
    }

    /** Confirm a password reset with the e-mailed code. */
    public JsonObject confirmPasswordReset(String code, String newPassword) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", "confirm");
        body.put("code", code);
        body.put("newPassword", newPassword);
        return api.post("/change_password.php", body);
    }
}
