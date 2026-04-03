package jdemic.ui;

import javafx.scene.text.Font;
import java.io.InputStream;

public class FontUtil {

    /**
     * Loads a font from the resources folder.
     * Uses an InputStream for better compatibility with different environments.
     */
    public static Font getFont(String fontName, double size) {
        try {
            // Using getResourceAsStream is more robust than toExternalForm for loading fonts
            InputStream fontStream = FontUtil.class.getResourceAsStream("/fonts/" + fontName + ".otf");

            if (fontStream != null) {
                Font customFont = Font.loadFont(fontStream, size);
                if (customFont != null) {
                    return customFont;
                }
            }

            // Log a warning if the specific font could not be found
            System.err.println("Warning: Could not load custom font: " + fontName + ". Falling back to system font.");

        } catch (Exception e) {
            System.err.println("Error during font loading: " + e.getMessage());
        }

        // Defensive Fallback: Returns a standard system font to prevent a NullPointerException and app crash
        return Font.font("Arial", size);
    }
}