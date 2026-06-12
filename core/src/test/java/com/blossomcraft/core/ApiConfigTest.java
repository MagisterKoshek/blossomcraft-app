package com.blossomcraft.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies API base URL resolution and normalization. */
class ApiConfigTest {

    @Test
    void setBaseUrlTrimsTrailingSlash() {
        ApiConfig.setBaseUrl("https://example.test/api/");
        assertEquals("https://example.test/api", ApiConfig.getBaseUrl());
    }

    @Test
    void setBaseUrlKeepsCleanUrl() {
        ApiConfig.setBaseUrl("https://example.test/api");
        assertEquals("https://example.test/api", ApiConfig.getBaseUrl());
    }

    @Test
    void rejectsBlankBaseUrl() {
        assertThrows(IllegalArgumentException.class, () -> ApiConfig.setBaseUrl("  "));
    }
}
