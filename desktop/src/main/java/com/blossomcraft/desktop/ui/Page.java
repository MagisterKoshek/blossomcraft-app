package com.blossomcraft.desktop.ui;

import javafx.scene.Node;

/** A content page hosted by the {@link MainShell}. */
public interface Page {

    /** The page's root node, placed into the shell's content area. */
    Node getView();

    /** Called after the page is attached; load data here. */
    default void onShown() {
    }
}
