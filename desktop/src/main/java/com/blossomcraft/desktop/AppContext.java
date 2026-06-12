package com.blossomcraft.desktop;

import com.blossomcraft.core.BlossomCraft;
import com.blossomcraft.core.theme.Theme;

import java.util.prefs.Preferences;

/** Shared application state for the desktop app: the core client and theme preference. */
public class AppContext {

    private final BlossomCraft bc;
    private final Preferences prefs = Preferences.userRoot().node("com/blossomcraft/desktop");

    public AppContext() {
        this.bc = new BlossomCraft(new DesktopTokenStore());
    }

    public BlossomCraft bc() {
        return bc;
    }

    public Theme theme() {
        return Theme.fromId(prefs.get("user_theme", Theme.DARK.id));
    }

    public void setTheme(Theme theme) {
        prefs.put("user_theme", theme.id);
    }

    public String font() {
        return prefs.get("user_font", "'Press Start 2P'");
    }

    public void setFont(String font) {
        prefs.put("user_font", font);
    }
}
