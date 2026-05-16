package jdemic.ui.GameplayUI;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test that verifies the outbreak icon resource exists and can be loaded
 * using the exact path expected by OutbreakManager.
 */
class OutbreakManagerResourceTest {

    @Test
    void shouldLoadOutbreakIconResource() {
        URL iconUrl = OutbreakManager.class.getResource("/icons/outbreakicon.png");

        assertNotNull(iconUrl,
                "Outbreak icon resource should exist at /icons/outbreakicon.png");

        assertDoesNotThrow(iconUrl::toExternalForm,
                "Outbreak icon URL should be convertible to external form");
    }
}