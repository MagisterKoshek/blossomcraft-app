package com.blossomcraft.android;

import android.app.Application;

import com.blossomcraft.core.ApiConfig;
import com.blossomcraft.core.BlossomCraft;

/**
 * Android {@link Application} that owns the single shared {@link BlossomCraft}
 * core instance. The API base URL is taken from the {@code api_base_url} string
 * resource (set at build time via the {@code blossomcraft.api.base} property).
 */
public class BlossomApp extends Application {

    private BlossomCraft bc;

    @Override
    public void onCreate() {
        super.onCreate();
        ApiConfig.setBaseUrl(getString(R.string.api_base_url));
        bc = new BlossomCraft(new AndroidTokenStore(this));
    }

    public BlossomCraft bc() {
        return bc;
    }

    public static BlossomCraft core(android.content.Context context) {
        return ((BlossomApp) context.getApplicationContext()).bc();
    }
}
