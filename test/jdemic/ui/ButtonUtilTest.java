package jdemic.ui;

import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for the ButtonsUtil class.
 * This test verifies that the button is initialized correctly with 12 parameters.
 */
class ButtonsUtilTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @Test
    void shouldCreateButtonSuccessfully() {
        // 1. ARRANGE: Create a dummy root container as a StackPane
        // Your ButtonsUtil constructor specifically requires a StackPane root
        StackPane dummyRoot = new StackPane();
        dummyRoot.setPrefWidth(1000.0);
        dummyRoot.setPrefHeight(800.0);

        // 2. ACT: Instantiate using the EXACT 12 parameters found in ButtonsUtil.java
        ButtonsUtil testButton = new ButtonsUtil(
                "PLAY",          // 1. text (String)
                "#ffffff",       // 2. textColor (String)
                "black",         // 3. backgroundColor (String)
                "#00d9ff",       // 4. borderColor (String)
                "#00d9ff",       // 5. glowColor (String)
                2.0,             // 6. borderWidth (double)
                10.0,            // 7. cornerRadius (double)
                15.0,            // 8. glowRadius (double)
                0.2,             // 9. widthRatio (double)
                0.1,             // 10. heightRatio (double)
                0.05,            // 11. fontRatio (double)
                dummyRoot        // 12. root (StackPane)
        );

        // 3. ASSERT: Confirm the button object exists and has internal UI components
        assertNotNull(testButton, "ButtonsUtil object should not be null.");
        assertFalse(testButton.getChildren().isEmpty(), "Button should contain child elements (Rectangle and Label).");
    }
}