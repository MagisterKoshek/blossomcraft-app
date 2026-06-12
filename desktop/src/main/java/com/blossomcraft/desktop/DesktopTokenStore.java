package com.blossomcraft.desktop;

import com.blossomcraft.core.net.TokenStore;

import java.util.prefs.Preferences;

/** Persists the auth token in the OS user preferences (desktop equivalent of localStorage). */
public class DesktopTokenStore implements TokenStore {

    private static final String KEY = "auth_token";
    private final Preferences prefs = Preferences.userRoot().node("com/blossomcraft/desktop");

    @Override
    public String getToken() {
        String token = prefs.get(KEY, null);
        return token == null || token.isBlank() ? null : token;
    }

    @Override
    public void setToken(String token) {
        if (token == null || token.isBlank()) {
            clearToken();
        } else {
            prefs.put(KEY, token);
        }
    }

    @Override
    public void clearToken() {
        prefs.remove(KEY);
    }
}
