package com.blossomcraft.core.model;

/** Response shape for login/register/me/google_auth endpoints. */
public class AuthResponse {
    public boolean success;
    public String token;
    public User user;
    public String error;
}
