package jdemic.frontend;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.JoinCodeScene;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.Scenes.Settings.SettingsManager;
import jdemic.ui.ButtonsUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        SceneManager.init(stage);
        SettingsManager.getInstance().playerNameProperty().set("Player");
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
        assertNotNull(robot.lookup(hasText("ENTER IP ADDRESS")).query());
    }

    @Test
    void codeFieldShowsPlaceholderAndStartsEmpty(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        TextField codeField = codeField(robot);
        assertNotNull(codeField);
        assertTrue(codeField.getText() == null || codeField.getText().isEmpty());
        assertEquals("192.168.1.100", codeField.getPromptText());
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
        long visibleIpPromptCount = robot.lookup(hasText("ENTER IP ADDRESS")).queryAll().stream()
                .filter(Node::isVisible)
                .count();
        assertEquals(1, visibleIpPromptCount, "only the input prompt should be visible initially");
    }

    @Test
    void emptyIpRevealsErrorLabelAndKeepsScene(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Node originalRoot = stage.getScene().getRoot();

        TextField codeField = codeField(robot);
        robot.interact(() -> codeField.setText("   "));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil joinBtn = LobbySceneFxTest.buttonByText(robot, "JOIN");
        robot.interact(() -> joinBtn.fireEvent(LobbySceneFxTest.mouseClicked()));
        WaitForAsyncUtils.waitForFxEvents();

        long visibleIpPromptCount = robot.lookup(hasText("ENTER IP ADDRESS")).queryAll().stream()
                .filter(Node::isVisible)
                .count();
        assertTrue(visibleIpPromptCount >= 2, "empty IP should reveal the validation error label");
        assertEquals(originalRoot, stage.getScene().getRoot());
    }

    @Test
    void blankNicknameRevealsErrorBeforeNetworking(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Node originalRoot = stage.getScene().getRoot();

        List<TextField> fields = textFields(robot);
        TextField nicknameField = fields.get(0);
        TextField codeField = fields.get(1);
        robot.interact(() -> {
            nicknameField.setText("");
            codeField.setText("127.0.0.1");
        });
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil joinBtn = LobbySceneFxTest.buttonByText(robot, "JOIN");
        robot.interact(() -> joinBtn.fireEvent(LobbySceneFxTest.mouseClicked()));
        WaitForAsyncUtils.waitForFxEvents();

        assertNotNull(robot.lookup(hasText("ENTER NICKNAME")).queryLabeled());
        assertEquals(originalRoot, stage.getScene().getRoot());
    }

    private static TextField codeField(FxRobot robot) {
        return textFields(robot).get(1);
    }

    private static List<TextField> textFields(FxRobot robot) {
        return new ArrayList<>(robot.lookup(".text-field").queryAll()).stream()
                .map(TextField.class::cast)
                .toList();
    }
}
