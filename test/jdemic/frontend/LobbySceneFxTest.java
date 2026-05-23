package jdemic.frontend;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.LobbyScene;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.Scenes.Settings.SettingsManager;
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
 *   <li>{@code CREATE SERVER} starts real networking on a background thread; the unit-style FX tests cover
 *       the synchronous validation path instead of opening sockets.</li>
 *   <li>{@code BACK} goes through {@link jdemic.Scenes.SceneManager#switchScene(String)}, which holds static state;
 *       verifying that the button exists is sufficient.</li>
 * </ul>
 */
@ExtendWith(ApplicationExtension.class)
class LobbySceneFxTest {

    private LobbyScene lobbyScene;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        SceneManager.init(stage);
        SettingsManager.getInstance().playerNameProperty().set("Player");
        lobbyScene = new LobbyScene(stage);
        stage.setScene(new Scene(lobbyScene.getRoot(), 1024, 768));
        stage.show();
    }

    @Test
    void lobbyRootRendersTitleAndNicknamePrompt(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(lobbyScene.getRoot());
        assertFalse(lobbyScene.getRoot().getChildren().isEmpty());
        assertNotNull(robot.lookup(hasText("HOST")).query());
        assertNotNull(robot.lookup(hasText("NICKNAME:")).query());
    }

    @Test
    void nicknameFieldHasDefaultValue(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        TextField nicknameField = robot.lookup(".text-field").queryAs(TextField.class);
        assertNotNull(nicknameField);
        assertEquals("Player", nicknameField.getText());
    }

    @Test
    void hostJoinAndBackButtonsArePresent(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(buttonByText(robot, "CREATE SERVER"));
        assertNotNull(buttonByText(robot, "JOIN BY IP"));
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
        robot.interact(() -> nicknameField.setText(""));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil hostBtn = buttonByText(robot, "CREATE SERVER");
        robot.interact(() -> hostBtn.fireEvent(mouseClicked()));
        WaitForAsyncUtils.waitForFxEvents();

        Labeled error = robot.lookup(hasText("ENTER NICKNAME")).queryLabeled();
        assertTrue(error.isVisible(), "blank nickname should reveal the error label");
    }

    @Test
    void joinByIpWithValidNicknameSwapsSceneRoot(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Node lobbyRoot = lobbyScene.getRoot();
        assertSame(lobbyRoot, stage.getScene().getRoot());

        TextField nicknameField = robot.lookup(".text-field").queryAs(TextField.class);
        robot.interact(() -> nicknameField.setText("Player"));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil joinBtn = buttonByText(robot, "JOIN BY IP");
        robot.interact(() -> joinBtn.fireEvent(mouseClicked()));
        WaitForAsyncUtils.waitForFxEvents();

        assertNotSame(lobbyRoot, stage.getScene().getRoot(), "JOIN BY IP should swap the scene root");
        assertNotNull(robot.lookup(hasText("ENTER IP ADDRESS")).query());
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

    static MouseEvent mouseClicked() {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0,
                MouseButton.PRIMARY, 1,
                false, false, false, false,
                true, false, false, true, false, false, null
        );
    }
}
