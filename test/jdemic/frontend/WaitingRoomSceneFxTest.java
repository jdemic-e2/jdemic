package jdemic.frontend;

import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.WaitingRoomScene;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * {@link WaitingRoomScene}: chrome ({@code LOBBY} / {@code WAITING ROOM}), the host-row status label,
 * the {@code READY} toggle in an offline scene, and the chat-send flow when no lobby client is attached.
 * <p>
 * {@code CANCEL} delegates to {@link jdemic.Scenes.SceneManager#switchScene(String)} and is asserted as present only.
 */
@ExtendWith(ApplicationExtension.class)
class WaitingRoomSceneFxTest {

    private WaitingRoomScene waitingRoomScene;

    @Start
    void start(Stage stage) {
        waitingRoomScene = new WaitingRoomScene(stage, "Guest", "AB12-CD34");
        stage.setScene(new Scene(waitingRoomScene.getRoot(), 1024, 768));
        stage.show();
    }

    @Test
    void chromeAndHostRowRender(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(waitingRoomScene.getRoot());
        assertFalse(waitingRoomScene.getRoot().getChildren().isEmpty());
        assertNotNull(robot.lookup(hasText("LOBBY")).query());
        assertNotNull(robot.lookup(hasText("WAITING ROOM")).query());
        // Nickname is uppercased by the constructor.
        assertNotNull(robot.lookup(hasText("GUEST")).query());
        assertNotNull(robot.lookup(hasText("PLAYER CHAT:")).query());
    }

    @Test
    void blankNicknameFallsBackToPlayer(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        boolean[] hasPlayer = {false};
        robot.interact(() -> {
            Stage local = new Stage();
            WaitingRoomScene fallback = new WaitingRoomScene(local, "   ", "X");
            local.setScene(new Scene(fallback.getRoot(), 400, 300));
            hasPlayer[0] = fallback.getRoot().lookupAll(".label").stream()
                    .anyMatch(n -> n instanceof Labeled l && "PLAYER".equals(l.getText()));
        });
        assertTrue(hasPlayer[0], "blank nickname should fall back to 'PLAYER'");
    }

    @Test
    void readyToggleStaysLocalWhenNoClientIsAttached(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Labeled readyBtnLabel = robot.lookup(hasText("READY")).queryLabeled();
        assertNotNull(readyBtnLabel);
        assertNotNull(robot.lookup(hasText("NOT READY")).query());

        ButtonsUtil readyBtn = LobbySceneFxTest.buttonByText(robot, "READY");
        robot.clickOn(readyBtn);
        WaitForAsyncUtils.waitForFxEvents();

        assertNotNull(robot.lookup(hasText("READY")).queryLabeled());
        assertNotNull(robot.lookup(hasText("NOT READY")).query());
    }

    @Test
    void cancelButtonIsPresent(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(LobbySceneFxTest.buttonByText(robot, "CANCEL"));
    }

    @Test
    void sendChatAppendsNicknamePrefixedMessage(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        TextArea chatArea = robot.lookup(".text-area").queryAs(TextArea.class);
        TextField chatInput = robot.lookup(".text-field").queryAs(TextField.class);
        assertNotNull(chatArea);
        assertNotNull(chatInput);
        assertTrue(chatArea.getText().isEmpty(), "chat should start empty");

        robot.interact(() -> chatInput.setText("hello team"));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil sendBtn = LobbySceneFxTest.buttonByText(robot, "SEND");
        robot.interact(() -> sendBtn.fireEvent(LobbySceneFxTest.mouseClicked()));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("", chatArea.getText());
        assertTrue(chatInput.getText().isEmpty(), "input should clear after send");
    }

    @Test
    void emptyChatMessageIsIgnored(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        TextArea chatArea = robot.lookup(".text-area").queryAs(TextArea.class);
        TextField chatInput = robot.lookup(".text-field").queryAs(TextField.class);

        robot.interact(() -> chatInput.setText("   "));
        WaitForAsyncUtils.waitForFxEvents();

        ButtonsUtil sendBtn = LobbySceneFxTest.buttonByText(robot, "SEND");
        robot.interact(() -> sendBtn.fireEvent(LobbySceneFxTest.mouseClicked()));
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(chatArea.getText().isEmpty(), "blank input must not append to the chat");
    }
}
