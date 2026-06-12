package com.blossomcraft.desktop;

/**
 * Plain (non-JavaFX) entry point used when packaging a self-contained native
 * installer with {@code jpackage}.
 *
 * <p>Launching a class that extends {@link javafx.application.Application}
 * directly from the classpath can fail with "JavaFX runtime components are
 * missing"; delegating through this wrapper avoids that. Gradle's
 * {@code application} plugin keeps using {@link DesktopApp} for
 * {@code :desktop:run}, so this only matters for {@code jpackage}.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        DesktopApp.main(args);
    }
}
