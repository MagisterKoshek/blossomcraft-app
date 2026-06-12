package com.blossomcraft.core;

/**
 * Runtime configuration for the BlossomCraft API client.
 *
 * <p>The native apps reuse the same PHP backend that powers the website
 * (the {@code /api/*.php} endpoints). The base URL must point at the host that
 * serves those endpoints, e.g. {@code https://your-host/api}.</p>
 *
 * <p>Resolution order for the default base URL:</p>
 * <ol>
 *   <li>System property {@code blossomcraft.api.base}</li>
 *   <li>Environment variable {@code BLOSSOMCRAFT_API_BASE}</li>
 *   <li>The compiled-in {@link #FALLBACK_BASE_URL}</li>
 * </ol>
 *
 * <p>Each platform launcher (desktop / Android) may also override the base URL
 * explicitly via {@link #setBaseUrl(String)} (for example from a settings screen).</p>
 */
public final class ApiConfig {

    /** Pre-set to the live BlossomCraft site; override at runtime (see class docs) or via in-app settings. */
    public static final String FALLBACK_BASE_URL = "https://bc-shop.duckdns.org/api";

    private static volatile String baseUrl = resolveDefaultBaseUrl();

    private ApiConfig() {
    }

    private static String resolveDefaultBaseUrl() {
        String prop = System.getProperty("blossomcraft.api.base");
        if (prop != null && !prop.isBlank()) {
            return normalize(prop);
        }
        String env = System.getenv("BLOSSOMCRAFT_API_BASE");
        if (env != null && !env.isBlank()) {
            return normalize(env);
        }
        return normalize(FALLBACK_BASE_URL);
    }

    /** Current API base URL without a trailing slash (e.g. {@code https://host/api}). */
    public static String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Website root URL — the live site that the native apps embed in a web view.
     * Derived from the API base by dropping a trailing {@code /api} segment,
     * e.g. {@code https://host/api} -> {@code https://host}.
     */
    public static String getSiteUrl() {
        String b = baseUrl;
        if (b.endsWith("/api")) {
            return b.substring(0, b.length() - "/api".length());
        }
        return b;
    }

    /** Override the API base URL at runtime (e.g. from an in-app settings screen). */
    public static void setBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Base URL must not be blank");
        }
        baseUrl = normalize(value);
    }

    /**
     * Build a full endpoint URL from a path such as {@code /login.php} or
     * {@code /music.php?action=public}.
     */
    public static String url(String path) {
        if (path == null || path.isEmpty()) {
            return baseUrl;
        }
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private static String normalize(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
