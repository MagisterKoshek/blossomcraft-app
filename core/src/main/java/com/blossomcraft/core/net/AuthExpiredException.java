package com.blossomcraft.core.net;

/**
 * Raised when the API responds with HTTP 401. Mirrors the website behaviour of
 * clearing the stored token and bouncing the user back to the login screen.
 */
public class AuthExpiredException extends ApiException {
    public AuthExpiredException() {
        super(401, "Session expired");
    }
}
