package com.blossomcraft.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.blossomcraft.core.net.TokenStore;

/** Persists the auth token in {@link SharedPreferences} (Android equivalent of localStorage). */
public class AndroidTokenStore implements TokenStore {

    private static final String FILE = "blossomcraft";
    private static final String KEY = "auth_token";
    private final SharedPreferences prefs;

    public AndroidTokenStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Override
    public String getToken() {
        String token = prefs.getString(KEY, null);
        return token == null || token.isEmpty() ? null : token;
    }

    @Override
    public void setToken(String token) {
        if (token == null || token.isEmpty()) {
            clearToken();
        } else {
            prefs.edit().putString(KEY, token).apply();
        }
    }

    @Override
    public void clearToken() {
        prefs.edit().remove(KEY).apply();
    }
}
