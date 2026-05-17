package jdemic.frontend;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.input.Clipboard;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.HostGameScene;
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
 * {@link HostGameScene}: header chrome ({@code LOBBY} / {@code HOST GAME}), generated host code label,
 * COPY-to-clipboard, and the HOST button transition to the waiting room via {@code stage.getScene().setRoot}.
 * <p>
 * The {@code BACK} button delegates to {@link jdemic.Scenes.SceneManager} (static state) so we only assert it exists.
 */
@ExtendWith(ApplicationExtension.class)
class HostGameSceneFxTest {

    private HostGameScene hostGameScene;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        hostGameScene = new HostGameScene(stage, "Emirhan");
        stage.setScene(new Scene(hostGameScene.getRoot(), 1024, 768));
        stage.show();
    }

    @Test
    void headerChromeRenders(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(hostGameScene.getRoot());
        assertFalse(hostGameScene.getRoot().getChildren().isEmpty());
        assertNotNull(robot.lookup(hasText("LOBBY")).query());
        assertNotNull(robot.lookup(hasText("HOST GAME")).query());
        assertNotNull(robot.lookup(hasText("CODE:")).query());
    }

    @Test
    void hostCodeIsNonBlankUppercaseAlphanumericWithDash(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        String code = findHostCode(robot);
        assertNotNull(code, "host code label not found");
        assertFalse(code.isBlank());
        // Allowed alphabet: A-H, J-N, P-Z, 2-9, plus dash. No lowercase, no I/O/0/1.
        assertTrue(code.matches("[A-HJ-NP-Z2-9-]+"), "unexpected code charset: " + code);
        // generateHostCode produces 8 chars plus a dash after the 4th — 9 total.
        assertEquals(9, code.length(), "host code should be 8 chars with one dash separator");
        assertEquals('-', code.charAt(4));
    }

    @Test
    void copyAndHostAndBackButtonsArePresent(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(LobbySceneFxTest.buttonByText(robot, "COPY"));
        assertNotNull(LobbySceneFxTest.buttonByText(robot, "HOST"));
        assertNotNull(LobbySceneFxTest.buttonByText(robot, "BACK"));
    }

    @Test
    void copyButtonPushesHostCodeToSystemClipboard(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        String code = findHostCode(robot);

        ButtonsUtil copyBtn = LobbySceneFxTest.buttonByText(robot, "COPY");
        robot.clickOn(copyBtn);
        WaitForAsyncUtils.waitForFxEvents();

        final String[] clipboardText = new String[1];
        robot.interact(() -> clipboardText[0] = Clipboard.getSystemClipboard().getString());
        assertEquals(code, clipboardText[0]);
    }

    @Test
    void hostButtonAdvancesToWaitingRoom(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Node hostGameRoot = hostGameScene.getRoot();
        ButtonsUtil hostBtn = LobbySceneFxTest.buttonByText(robot, "HOST");

        robot.clickOn(hostBtn);
        WaitForAsyncUtils.waitForFxEvents();

        assertNotSame(hostGameRoot, stage.getScene().getRoot());
        assertNotNull(robot.lookup(hasText("WAITING ROOM")).query(),
                "HOST should transition to WaitingRoomScene chrome");
    }

    private static String findHostCode(FxRobot robot) {
        // Code value Label is the only one matching the charset; collect by predicate.
        for (Node n : robot.lookup(".label").queryAll()) {
            if (n instanceof Labeled l) {
                String t = l.getText();
                if (t != null && t.matches("[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}")) {
                    return t;
                }
            }
        }
        return null;
    }
}
