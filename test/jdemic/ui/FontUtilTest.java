package jdemic.ui;

import javafx.application.Platform;
import javafx.scene.text.Font;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontUtilTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Ignore if the toolkit is already initialized
        }
    }

    @Test
    void shouldLoadCustomFontsCorrectly() {
        // Test loading one of the frequently used cyberpunk fonts
        double targetSize = 20.0;
        Font hkModularFont = FontUtil.getFont("hkmodular", targetSize);

        // Verify if the font is loaded successfully
        assertNotNull(hkModularFont, "hkmodular font failed to load (returned null).");
        assertTrue(hkModularFont.getSize() == targetSize, "Font size does not match the target value.");
    }
}