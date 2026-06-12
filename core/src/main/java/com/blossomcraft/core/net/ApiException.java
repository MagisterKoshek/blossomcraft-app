package com.blossomcraft.core.net;

/**
 * Thrown when the API returns a non-2xx response (other than 401, which the
 * client treats as a session expiry and surfaces via {@link AuthExpiredException}).
 *
 * <p>The backend reports errors as {@code {"error": "message"}} — that message is
 * captured in {@link #getMessage()} when present.</p>
 */
public class ApiException extends RuntimeException {

    private final int statusCode;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
