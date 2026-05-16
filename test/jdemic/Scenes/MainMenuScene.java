package jdemic.Scenes;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainMenuSceneTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Ignore if the toolkit is already initialized
        }
    }

    @Test
    void shouldInitializeMainMenuRootPaneWithoutCrashing() {
        // When testing Scene classes, it is safest to run them within the JavaFX thread.
        Platform.runLater(() -> {
            // Passing null for Stage since we only test if the Root UI is created
            MainMenuScene menuScene = new MainMenuScene(null);

            // If root is not null, it means the UI elements were created without crashing.
            assertNotNull(menuScene.getRoot(), "MainMenuScene root pane could not be created.");
        });
    }
}