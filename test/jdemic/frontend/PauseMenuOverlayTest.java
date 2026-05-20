package jdemic.frontend;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.PauseMenuOverlay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Frontend tests for {@link PauseMenuOverlay} — overlay lifecycle and panel navigation only.
 */
class PauseMenuOverlayTest {

    private static final String LABEL_PAUSED = "PAUSED";
    private static final String LABEL_RESUME = "RESUME";
    private static final String LABEL_SETTINGS = "SETTINGS";
    private static final String LABEL_TUTORIAL = "TUTORIAL";
    private static final String LABEL_DISCONNECT = "DISCONNECT";
    private static final String LABEL_YES = "YES";
    private static final String LABEL_NO = "NO";
    private static final String MSG_DISCONNECT_CONFIRM = "DISCONNECT AND RETURN TO MAIN MENU?";
    private static final String LABEL_IN_GAME_SETTINGS = "IN-GAME SETTINGS";
    private static final String LABEL_GAME_RULES = "GAME RULES";

    private StackPane root;
    private AtomicBoolean disconnectCalled;
    private PauseMenuOverlay overlay;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyInitialized) {
            // Expected when multiple test classes share one JVM JavaFX toolkit.
            assertNotNull(alreadyInitialized.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        runOnFxThread(() -> {
            root = new StackPane();
            root.setPrefSize(1024, 768);
            disconnectCalled = new AtomicBoolean(false);
            overlay = new PauseMenuOverlay(root, () -> disconnectCalled.set(true));
        });
    }

    @Test
    void constructsWithRootAndCallback() throws Exception {
        runOnFxThread(() -> {
            assertNotNull(overlay);
            assertFalse(overlay.isVisible());
            assertEquals(0, root.getChildren().size());
        });
    }

    @Test
    void showAddsOverlayToRoot() throws Exception {
        runOnFxThread(() -> {
            int before = root.getChildren().size();
            overlay.show();

            assertTrue(overlay.isVisible());
            assertEquals(before + 1, root.getChildren().size());
            assertTrue(hasVisibleText(root, LABEL_PAUSED));
        });
    }

    @Test
    void hideRemovesOverlayFromRoot() throws Exception {
        runOnFxThread(() -> {
            int initialChildCount = root.getChildren().size();
            overlay.show();
            assertTrue(overlay.isVisible());
            assertTrue(overlay.blocksMouseInput());

            overlay.hide();

            assertFalse(overlay.isVisible());
            assertFalse(overlay.blocksMouseInput());
            assertEquals(initialChildCount, root.getChildren().size());
            assertFalse(root.getChildren().contains(findOverlayRoot()));
        });
    }

    @Test
    void hideDoesNotBlockMouseInputOnGameplayRoot() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger clickCount = new AtomicInteger();
            StackPane gameplayLayer = new StackPane();
            gameplayLayer.setPrefSize(400, 300);
            gameplayLayer.setOnMouseClicked(e -> clickCount.incrementAndGet());
            root.getChildren().add(gameplayLayer);

            int baselineChildren = root.getChildren().size();
            overlay.show();
            overlay.hide();

            assertEquals(baselineChildren, root.getChildren().size());
            assertFalse(overlay.blocksMouseInput());
            gameplayLayer.fireEvent(mouseClicked());
            assertEquals(1, clickCount.get(), "Gameplay layer should receive clicks after pause hide");
        });
    }

    @Test
    void hideDismissesDisconnectConfirmationSoInputIsNotBlocked() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, LABEL_DISCONNECT);
            assertTrue(overlay.blocksMouseInput());

            overlay.hide();

            assertFalse(overlay.blocksMouseInput());
            assertFalse(hasVisibleText(root, MSG_DISCONNECT_CONFIRM));
        });
    }

    @Test
    void resumeClosesOverlay() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, LABEL_RESUME);

            assertFalse(overlay.isVisible());
            assertTrue(root.getChildren().isEmpty());
        });
    }

    @Test
    void hideInvokesOnOverlayHiddenCallback() throws Exception {
        AtomicInteger hiddenCount = new AtomicInteger();
        runOnFxThread(() -> {
            PauseMenuOverlay withCallback = new PauseMenuOverlay(
                    root,
                    () -> {},
                    hiddenCount::incrementAndGet
            );
            withCallback.show();
            withCallback.hide();
            assertEquals(1, hiddenCount.get());
        });
    }

    @Test
    void settingsOpensInGamePanelWithoutLeavingRoot() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            int childCountAfterShow = root.getChildren().size();

            clickButton(root, LABEL_SETTINGS);

            assertTrue(overlay.isVisible());
            assertEquals(childCountAfterShow, root.getChildren().size());
            assertFalse(hasVisibleText(root, LABEL_PAUSED));
            assertTrue(hasVisibleText(root, LABEL_IN_GAME_SETTINGS));
        });
    }

    @Test
    void tutorialOpensRulesPanelWithoutLeavingRoot() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            int childCountAfterShow = root.getChildren().size();

            clickButton(root, LABEL_TUTORIAL);

            assertTrue(overlay.isVisible());
            assertEquals(childCountAfterShow, root.getChildren().size());
            assertFalse(hasVisibleText(root, LABEL_PAUSED));
            assertTrue(hasVisibleText(root, LABEL_GAME_RULES));
        });
    }

    @Test
    void disconnectShowsConfirmationBeforeCallback() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, LABEL_DISCONNECT);

            assertFalse(disconnectCalled.get());
            assertTrue(hasVisibleText(root, MSG_DISCONNECT_CONFIRM));
            assertTrue(root.getChildren().size() >= 2, "Pause overlay and confirmation should both be on root");
        });
    }

    @Test
    void disconnectYesInvokesCallbackAndClosesPauseOverlay() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, LABEL_DISCONNECT);
            clickButton(root, LABEL_YES);

            assertTrue(disconnectCalled.get());
            assertFalse(overlay.isVisible());
        });
    }

    @Test
    void toggleFromSettingsReturnsToMainPanelWithoutHiding() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, LABEL_SETTINGS);
            assertTrue(hasVisibleText(root, LABEL_IN_GAME_SETTINGS));

            overlay.toggle();

            assertTrue(overlay.isVisible());
            assertTrue(hasVisibleText(root, LABEL_PAUSED));
            assertFalse(hasVisibleText(root, LABEL_IN_GAME_SETTINGS));
        });
    }

    @Test
    void toggleWhenHiddenShowsOverlay() throws Exception {
        runOnFxThread(() -> {
            overlay.toggle();
            assertTrue(overlay.isVisible());
            assertTrue(hasVisibleText(root, LABEL_PAUSED));
        });
    }

    @Test
    void toggleWhenVisibleOnMainPanelHidesOverlay() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            overlay.toggle();
            assertFalse(overlay.isVisible());
        });
    }

    @Test
    void disconnectYesWithNullCallbackStillClosesOverlay() throws Exception {
        runOnFxThread(() -> {
            PauseMenuOverlay noCallbackOverlay = new PauseMenuOverlay(root, null);
            noCallbackOverlay.show();
            clickButton(root, LABEL_DISCONNECT);
            clickButton(root, LABEL_YES);
            assertFalse(noCallbackOverlay.isVisible());
        });
    }

    @Test
    void disconnectNoDismissesConfirmationWithoutCallback() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, LABEL_DISCONNECT);
            int withConfirm = root.getChildren().size();

            clickButton(root, LABEL_NO);

            assertFalse(disconnectCalled.get());
            assertTrue(overlay.isVisible());
            assertTrue(withConfirm > root.getChildren().size(), "Confirmation should be removed from root");
        });
    }

    private static void clickButton(Parent searchRoot, String labelText) {
        ButtonsUtil button = findButton(searchRoot, labelText);
        assertNotNull(button, "Expected button labeled: " + labelText);
        button.fireEvent(mouseClicked());
    }

    private StackPane findOverlayRoot() {
        for (Node node : root.getChildren()) {
            if (node instanceof StackPane stackPane
                    && flatten(stackPane).stream().anyMatch(n -> n instanceof Labeled labeled && LABEL_PAUSED.equals(labeled.getText()))) {
                return stackPane;
            }
        }
        return null;
    }

    private static ButtonsUtil findButton(Parent searchRoot, String labelText) {
        for (Node node : flatten(searchRoot)) {
            if (node instanceof ButtonsUtil button && buttonHasLabel(button, labelText)) {
                return button;
            }
        }
        return null;
    }

    private static boolean buttonHasLabel(ButtonsUtil button, String labelText) {
        for (Node child : button.getChildrenUnmodifiable()) {
            if (child instanceof Labeled labeled && labelText.equals(labeled.getText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasVisibleText(Parent searchRoot, String text) {
        return flatten(searchRoot).stream()
                .filter(Node::isVisible)
                .filter(Labeled.class::isInstance)
                .map(n -> ((Labeled) n).getText())
                .anyMatch(text::equals);
    }

    private static List<Node> flatten(Node node) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(node);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                nodes.addAll(flatten(child));
            }
        }
        return nodes;
    }

    private static MouseEvent mouseClicked() {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0,
                MouseButton.PRIMARY, 1,
                false, false, false, false,
                true, false, false, true, false, false, null
        );
    }

    private void runOnFxThread(Runnable action) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX action timed out");
        if (error.get() != null) {
            if (error.get() instanceof Exception ex) {
                throw ex;
            }
            throw new AssertionError(error.get());
        }
    }
}
