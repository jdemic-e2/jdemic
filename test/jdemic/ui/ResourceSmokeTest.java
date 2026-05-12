package jdemic.ui;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test that verifies all resource paths used by getResource().toExternalForm()
 * across the UI codebase actually exist on the classpath.
 *
 * Resolves Review Issue #19: Resource smoke test for all getResource paths.
 */
class ResourceSmokeTest {

    // --- Backgrounds ---

    @Test
    void backgroundPngExists() {
        assertNotNull(load("/background.png"), "/background.png");
    }

    @Test
    void backgroundMapPngExists() {
        assertNotNull(load("/backgroundMap.png"), "/backgroundMap.png");
    }

    @Test
    void bgGamePngExists() {
        assertNotNull(load("/bgGame.png"), "/bgGame.png");
    }

    // --- UI Elements ---

    @Test
    void glowTitleFrameExists() {
        assertNotNull(load("/elements/glowTitleFrame.png"), "/elements/glowTitleFrame.png");
    }

    // --- Icons ---

    @Test
    void outbreakIconExists() {
        assertNotNull(load("/icons/outbreakicon.png"), "/icons/outbreakicon.png");
    }

    @Test
    void curedIconExists() {
        assertNotNull(load("/icons/curedIcon.png"), "/icons/curedIcon.png");
    }

    @Test
    void virusIconConfickerExists() {
        assertNotNull(load("/icons/IconVirusConficker.png"), "/icons/IconVirusConficker.png");
    }

    @Test
    void virusIconWannaCryExists() {
        assertNotNull(load("/icons/IconVirusWannaCry.png"), "/icons/IconVirusWannaCry.png");
    }

    @Test
    void virusIconStuxnetExists() {
        assertNotNull(load("/icons/IconVirusStuxnet.png"), "/icons/IconVirusStuxnet.png");
    }

    @Test
    void virusIconZeusExists() {
        assertNotNull(load("/icons/IconVirusZeus.png"), "/icons/IconVirusZeus.png");
    }

    // --- Card Versos ---

    @Test
    void cityCardsVersoExists() {
        assertNotNull(load("/cityCards/cityCardsVerso.png"), "/cityCards/cityCardsVerso.png");
    }

    @Test
    void epidemicCardsVersoExists() {
        assertNotNull(load("/epidemicCards/epidemicCardsVerso.png"), "/epidemicCards/epidemicCardsVerso.png");
    }

    // --- Epidemic Card ---

    @Test
    void systemBreachCardExists() {
        assertNotNull(load("/epidemicCard/SystemBreach.png"), "/epidemicCard/SystemBreach.png");
    }

    // --- Helper ---

    private URL load(String path) {
        return ResourceSmokeTest.class.getResource(path);
    }
}
