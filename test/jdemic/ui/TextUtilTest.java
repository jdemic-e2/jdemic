package jdemic.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TextUtilTest {

    // Mandatory setup to initialize JavaFX Toolkit for testing
    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Ignore if the toolkit is already initialized
        }
    }

    @Test
    void shouldCreateStyledLabelCorrectly() {
        // 1. ARRANGE
        String expectedText = "Cyber Crisis";
        String fontName = "hkmodular";
        double sizeRatio = 0.05;
        String colorHex = "#00d9ff";

        // A dummy root pane with a specific width
        Pane dummyRoot = new Pane();
        dummyRoot.setPrefWidth(1000);

        // 2. ACT
        Label resultLabel = TextUtil.createText(expectedText, fontName, sizeRatio, colorHex, dummyRoot);

        // 3. ASSERT
        assertNotNull(resultLabel, "Label should not be null.");
        assertEquals(expectedText, resultLabel.getText(), "Label text does not match.");
        assertEquals(Color.web(colorHex), resultLabel.getTextFill(), "Label color does not match.");
    }
}