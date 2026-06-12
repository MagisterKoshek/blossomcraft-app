package com.blossomcraft.core.theme;

/**
 * Color tokens for each {@link Theme}, ported from the website's CSS custom
 * properties in {@code index.css}. Values are hex strings so both JavaFX and
 * Android can consume them directly.
 *
 * <p>The website stores HSL triplets; the constants here are the resolved hex
 * equivalents of those tokens plus the brand purple ({@code #a855f7}).</p>
 */
public final class ThemePalette {

    public static final String BRAND_PURPLE = "#a855f7";
    public static final String BRAND_PURPLE_DEEP = "#9333ea";
    public static final String BRAND_PURPLE_LIGHT = "#c084fc";

    public final String background;
    public final String foreground;
    public final String card;
    public final String border;
    public final String primary;
    public final String muted;
    /** Whether surfaces should be rendered translucent (mirror / gray-mirror). */
    public final boolean glass;

    private ThemePalette(String background, String foreground, String card, String border,
                         String primary, String muted, boolean glass) {
        this.background = background;
        this.foreground = foreground;
        this.card = card;
        this.border = border;
        this.primary = primary;
        this.muted = muted;
        this.glass = glass;
    }

    public static ThemePalette of(Theme theme) {
        switch (theme) {
            case LIGHT:
                return new ThemePalette("#f5f3ff", "#1a1a2e", "#ffffff", "#d8d2e8",
                        BRAND_PURPLE_DEEP, "#f0eef7", false);
            case MIRROR:
                return new ThemePalette("#0f041e", "#ece4f5", "#1a0b2e", "#3a2350",
                        BRAND_PURPLE, "#19102a", true);
            case GRAY_MIRROR:
                return new ThemePalette("#141418", "#e9e9ee", "#1d1d22", "#3a3a42",
                        "#b4b4be", "#202024", true);
            case DARK:
            default:
                return new ThemePalette("#07010f", "#e9def5", "#120a1f", "#2a1d3a",
                        BRAND_PURPLE, "#150c24", false);
        }
    }

    private ThemePalette() {
        throw new AssertionError();
    }
}
