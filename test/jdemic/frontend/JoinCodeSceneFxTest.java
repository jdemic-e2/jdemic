package jdemic.frontend;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.JoinCodeScene;
import jdemic.ui.ButtonsUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * {@link JoinCodeScene}: chrome, code field placeholder, validation flow (invalid → error label;
 * the hard-coded valid code {@code "1234"} → swap to {@link jdemic.Scenes.Lobby.WaitingRoomScene}).
 * <p>
 * The {@code CANCEL} button calls {@link jdemic.Scenes.SceneManager#switchScene(String)} so we only assert it exists.
 */
@ExtendWith(ApplicationExtension.class)
class JoinCodeSceneFxTest {

    private JoinCodeScene joinCodeScene;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        joinCodeScene = new JoinCodeScene(stage);
        stage.setScene(new Scene(joinCodeScene.getRoot(), 1024, 768));
        stage.show();
    }

    @Test
    void chromeAndPromptRender(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(joinCodeScene.getRoot());
        assertFalse(joinCodeScene.getRoot().getChildren().isEmpty());
        assertNotNull(robot.lookup(hasText("LOBBY")).query());
        assertNotNull(robot.lookup(hasText("ENTER ACCESS CODE")).query());
    }

    @Test
    void codeFieldShowsPlaceholderAndStartsEmpty(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        TextField codeField = robot.lookup(".text-field").queryAs(TextField.class);
        assertNotNull(codeField);
        assertTrue(codeField.getText() == null || codeField.getText().isEmpty());
        assertEquals("XXXX-YYYY", codeField.getPromptText());
    }

    @Test
    void joinAndCancelButtonsArePresent(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(LobbySceneFxTest.buttonByText(robot, "JOIN"));
        assertNotNull(LobbySceneFxTest.buttonByText(robot, "CANCEL"));
    }

    @Test
    void errorLabelHiddenInitially(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Labeled err = robot.lookup(hasText("ERROR: INVALID CODE")).queryLabeled();
        assertNotNull(err);
        assertFalse(err.isVisible());
    }

    @Test
    void invalidCodeRevealsErrorLabelAndKeepsScene(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Node originalRoot = stage.getScene().getRoot();

        TextField codeField = robot.lookup(".text-field").queryAs(TextField.class);
        robot.interact(() -> codeField.setText("ZZZZ-9999"));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil joinBtn = LobbySceneFxTest.buttonByText(robot, "JOIN");
        robot.clickOn(joinBtn);
        WaitForAsyncUtils.waitForFxEvents();

        Labeled err = robot.lookup(hasText("ERROR: INVALID CODE")).queryLabeled();
        assertTrue(err.isVisible(), "invalid code should reveal the error label");
        // Scene root must NOT change on invalid input.
        assertEquals(originalRoot, stage.getScene().getRoot());
    }

    @Test
    void validCodeAdvancesToWaitingRoom(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Node originalRoot = stage.getScene().getRoot();

        TextField codeField = robot.lookup(".text-field").queryAs(TextField.class);
        robot.interact(() -> codeField.setText("1234"));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil joinBtn = LobbySceneFxTest.buttonByText(robot, "JOIN");
        robot.clickOn(joinBtn);
        WaitForAsyncUtils.waitForFxEvents();

        assertNotSame(originalRoot, stage.getScene().getRoot());
        assertNotNull(robot.lookup(hasText("WAITING ROOM")).query());
    }
}
