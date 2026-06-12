package com.blossomcraft.core.theme;

/**
 * The four visual themes offered by the website, shared so both native apps
 * expose the same options and persist the same identifiers.
 *
 * <ul>
 *   <li>{@link #DARK} — default senku near-black + blue accent.</li>
 *   <li>{@link #LIGHT} — clean white interface + blue accent.</li>
 *   <li>{@link #MIRROR} — translucent purple glassmorphism.</li>
 *   <li>{@link #GRAY_MIRROR} — neutral-gray glassmorphism.</li>
 * </ul>
 */
public enum Theme {
    DARK("dark", "Тёмная"),
    LIGHT("light", "Светлая"),
    MIRROR("mirror", "Зеркальная"),
    GRAY_MIRROR("gray-mirror", "Серо-зеркальная");

    /** Stable id matching the website's {@code user_theme} value. */
    public final String id;
    /** Russian display label, matching the site. */
    public final String label;

    Theme(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static Theme fromId(String id) {
        if (id != null) {
            for (Theme theme : values()) {
                if (theme.id.equals(id)) {
                    return theme;
                }
            }
        }
        return DARK;
    }
}
