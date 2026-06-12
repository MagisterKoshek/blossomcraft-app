package com.blossomcraft.core;

import com.blossomcraft.core.model.Role;
import com.blossomcraft.core.model.User;
import com.blossomcraft.core.net.ApiClient;
import com.blossomcraft.core.net.TokenStore;
import com.blossomcraft.core.service.AdminService;
import com.blossomcraft.core.service.AuthService;
import com.blossomcraft.core.service.GroupService;
import com.blossomcraft.core.service.MessageService;
import com.blossomcraft.core.service.MusicService;
import com.blossomcraft.core.service.ProfileService;
import com.blossomcraft.core.service.ShopService;
import com.blossomcraft.core.service.VideoService;

/**
 * Central entry point wiring together the API client and every service, plus the
 * in-memory session (current user + role). Both the desktop and Android apps
 * construct one {@code BlossomCraft} instance with their platform {@link TokenStore}.
 */
public class BlossomCraft {

    private final ApiClient api;
    private final AuthService auth;
    private final ShopService shop;
    private final MusicService music;
    private final VideoService videos;
    private final MessageService messages;
    private final GroupService groups;
    private final ProfileService profile;
    private final AdminService admin;

    private volatile User currentUser;
    private volatile Role currentRole;

    public BlossomCraft(TokenStore tokenStore) {
        this.api = new ApiClient(tokenStore);
        this.auth = new AuthService(api);
        this.shop = new ShopService(api);
        this.music = new MusicService(api);
        this.videos = new VideoService(api);
        this.messages = new MessageService(api);
        this.groups = new GroupService(api);
        this.profile = new ProfileService(api);
        this.admin = new AdminService(api);
    }

    public ApiClient api() {
        return api;
    }

    public AuthService auth() {
        return auth;
    }

    public ShopService shop() {
        return shop;
    }

    public MusicService music() {
        return music;
    }

    public VideoService videos() {
        return videos;
    }

    public MessageService messages() {
        return messages;
    }

    public GroupService groups() {
        return groups;
    }

    public ProfileService profile() {
        return profile;
    }

    public AdminService admin() {
        return admin;
    }

    // ─── Session state ────────────────────────────────────────────────────
    public User currentUser() {
        return currentUser;
    }

    public Role currentRole() {
        return currentRole;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void setSession(User user) {
        this.currentUser = user;
        this.currentRole = Role.fromUser(user);
    }

    public void clearSession() {
        this.currentUser = null;
        this.currentRole = null;
    }

    public boolean hasPermission(String perm) {
        if (currentRole == null || currentRole.permissions == null) {
            return false;
        }
        return currentRole.permissions.contains("*") || currentRole.permissions.contains(perm);
    }

    /** Restore the session from a stored token (returns the user, or {@code null}). */
    public User restoreSession() {
        User user = auth.restoreSession();
        setSession(user);
        return user;
    }
}
