package jdemic.frontend;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.LobbyScene;
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
 * {@link LobbyScene} entry: title chrome, nickname field, host/join/back buttons,
 * empty-nickname validation, and the host-game transition driven by {@code stage.getScene().setRoot(...)}.
 * <p>
 * Not covered here:
 * <ul>
 *   <li>{@code JOIN BY CODE} and {@code BACK} buttons — both go through {@link jdemic.Scenes.SceneManager#switchScene(String)},
 *       which holds static state and would instantiate sibling scenes; verifying that the buttons exist is sufficient.</li>
 * </ul>
 */
@ExtendWith(ApplicationExtension.class)
class LobbySceneFxTest {

    private LobbyScene lobbyScene;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        lobbyScene = new LobbyScene(stage);
        stage.setScene(new Scene(lobbyScene.getRoot(), 1024, 768));
        stage.show();
    }

    @Test
    void lobbyRootRendersTitleAndNicknamePrompt(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(lobbyScene.getRoot());
        assertFalse(lobbyScene.getRoot().getChildren().isEmpty());
        assertNotNull(robot.lookup(hasText("LOBBY")).query());
        assertNotNull(robot.lookup(hasText("NICKNAME:")).query());
    }

    @Test
    void nicknameFieldHasDefaultValue(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        TextField nicknameField = robot.lookup(".text-field").queryAs(TextField.class);
        assertNotNull(nicknameField);
        assertEquals("Newbie", nicknameField.getText());
    }

    @Test
    void hostJoinAndBackButtonsArePresent(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(buttonByText(robot, "HOST GAME"));
        assertNotNull(buttonByText(robot, "JOIN BY CODE"));
        assertNotNull(buttonByText(robot, "BACK"));
    }

    @Test
    void errorLabelHiddenInitially(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Labeled error = robot.lookup(hasText("ENTER NICKNAME")).queryLabeled();
        assertNotNull(error);
        assertFalse(error.isVisible());
    }

    @Test
    void hostGameWithBlankNicknameRevealsError(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        TextField nicknameField = robot.lookup(".text-field").queryAs(TextField.class);
        robot.interact(() -> nicknameField.setText("   "));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil hostBtn = buttonByText(robot, "HOST GAME");
        robot.clickOn(hostBtn);
        WaitForAsyncUtils.waitForFxEvents();

        Labeled error = robot.lookup(hasText("ENTER NICKNAME")).queryLabeled();
        assertTrue(error.isVisible(), "blank nickname should reveal the error label");
    }

    @Test
    void hostGameWithValidNicknameSwapsSceneRoot(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Node lobbyRoot = lobbyScene.getRoot();
        assertSame(lobbyRoot, stage.getScene().getRoot());

        TextField nicknameField = robot.lookup(".text-field").queryAs(TextField.class);
        robot.interact(() -> nicknameField.setText("Emirhan"));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil hostBtn = buttonByText(robot, "HOST GAME");
        robot.clickOn(hostBtn);
        WaitForAsyncUtils.waitForFxEvents();

        assertNotSame(lobbyRoot, stage.getScene().getRoot(), "HOST GAME should swap the scene root");
        // The new root belongs to HostGameScene — chrome confirms that.
        assertNotNull(robot.lookup(hasText("HOST GAME")).query());
        assertNotNull(robot.lookup(hasText("CODE:")).query());
    }

    private static void assertSame(Object expected, Object actual) {
        assertTrue(expected == actual, "expected same reference");
    }

    static ButtonsUtil buttonByText(FxRobot robot, String text) {
        Labeled lab = robot.lookup(hasText(text)).queryLabeled();
        Node n = lab.getParent();
        while (n != null && !(n instanceof ButtonsUtil)) n = n.getParent();
        assertNotNull(n, "no ButtonsUtil ancestor for label \"" + text + "\"");
        return (ButtonsUtil) n;
    }
}
