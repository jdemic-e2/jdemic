package jdemic.ui;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for the PanelUtil class.
 */
class PanelUtilTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @Test
    void shouldCreateCyberpunkPanelCorrectly() {
        // 1. ARRANGE
        Pane dummyRoot = new Pane();
        dummyRoot.setPrefWidth(1000.0);
        dummyRoot.setPrefHeight(800.0);

        // 2. ACT
        StackPane resultPanel = PanelUtil.createPanel(
                0.5,        // widthRatio
                0.5,        // heightRatio
                "#00b5d4",  // borderColor
                2.0,        // borderWidth
                10.0,       // borderRadius
                15.0,       // backgroundRadius
                dummyRoot   // root
        );

        // 3. ASSERT
        // We only check if the panel was instantiated successfully.
        // PanelUtil uses styling properties rather than adding child nodes.
        assertNotNull(resultPanel, "Panel object should not be null.");
    }
}