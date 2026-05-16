package jdemic.ui;

import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for the GlowUtil class.
 */
class GlowUtilTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @Test
    void shouldApplyGlowEffectCorrectly() {
        // 1. ARRANGE
        Pane dummyNode = new Pane();
        String glowColorHex = "#ff2d2d";
        double inputRadius = 15.0;

        // According to the GlowUtil logic, the actual applied radius is transformed to 6.0
        double expectedRadius = 6.0;

        // 2. ACT
        GlowUtil.applyGlow(dummyNode, glowColorHex, inputRadius);

        // 3. ASSERT
        assertNotNull(dummyNode.getEffect(), "Effect should be applied, but it is null.");
        assertTrue(dummyNode.getEffect() instanceof DropShadow, "Applied effect is not a DropShadow.");

        DropShadow shadow = (DropShadow) dummyNode.getEffect();
        assertEquals(Color.web(glowColorHex), shadow.getColor(), "Glow color does not match.");

        // Assert against the actual generated radius
        assertEquals(expectedRadius, shadow.getRadius(), "Glow radius does not match the generated output.");
    }
}