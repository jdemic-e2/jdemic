package jdemic.frontend;

import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.stage.Stage;
import jdemic.Scenes.PlayScene;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * {@link PlayScene} shell: scene graph initializes without crashing.
 * <p>
 * The current {@link PlayScene} implementation does not embed {@link jdemic.ui.gameplay.CardView} or
 * {@link jdemic.ui.gameplay.CardDeckView}; command-hand / deck strip integration is therefore asserted
 * as absent on the default entry screen rather than inventing a fake scene graph.
 */
@ExtendWith(ApplicationExtension.class)
class PlaySceneFxTest {

    private PlayScene playScene;

    @Start
    void start(Stage stage) {
        playScene = new PlayScene(stage);
        stage.setScene(new Scene(playScene.getRoot(), 1024, 768));
        stage.show();
    }

    @Test
    void playSceneRootInitializesWithLobbyChrome(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(playScene.getRoot());
        assertFalse(playScene.getRoot().getChildren().isEmpty());
        assertNotNull(robot.lookup(hasText("LOBBY")).query());
    }

    @Test
    void entryScreenDoesNotContainGameplayHandCardNodes(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(
                playScene.getRoot().lookupAll(".gameplay-card").isEmpty(),
                "PlayScene entry should not mount gameplay hand CardView tiles yet"
        );
    }

    @Test
    void nickPromptVisibleForLobbyFlow(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Labeled nickPrompt = robot.lookup(hasText("NICKNAME:")).queryLabeled();
        assertNotNull(nickPrompt);
    }
}
