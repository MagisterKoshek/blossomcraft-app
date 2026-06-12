package com.blossomcraft.core.net;

/**
 * Persists the bearer token used to authenticate API requests.
 *
 * <p>This mirrors how the website keeps {@code auth_token} in {@code localStorage}.
 * Each platform supplies its own implementation:</p>
 * <ul>
 *   <li>Desktop — {@code java.util.prefs.Preferences}</li>
 *   <li>Android — {@code SharedPreferences}</li>
 * </ul>
 */
public interface TokenStore {

    /** Returns the saved token, or {@code null}/empty when not logged in. */
    String getToken();

    /** Saves the token after a successful login/register. */
    void setToken(String token);

    /** Clears the token (logout, or after a 401 response). */
    void clearToken();

    /** A volatile in-memory store, useful for tests and headless usage. */
    static TokenStore inMemory() {
        return new TokenStore() {
            private volatile String token;

            @Override
            public String getToken() {
                return token;
            }

            @Override
            public void setToken(String value) {
                this.token = value;
            }

            @Override
            public void clearToken() {
                this.token = null;
            }
        };
    }
}
