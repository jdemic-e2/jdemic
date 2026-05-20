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

    private StackPane root;
    private AtomicBoolean disconnectCalled;
    private PauseMenuOverlay overlay;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized (e.g. another test class)
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
            assertTrue(hasVisibleText(root, "PAUSED"));
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
            clickButton(root, "DISCONNECT");
            assertTrue(overlay.blocksMouseInput());

            overlay.hide();

            assertFalse(overlay.blocksMouseInput());
            assertFalse(hasVisibleText(root, "DISCONNECT AND RETURN TO MAIN MENU?"));
        });
    }

    @Test
    void resumeClosesOverlay() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, "RESUME");

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

            clickButton(root, "SETTINGS");

            assertTrue(overlay.isVisible());
            assertEquals(childCountAfterShow, root.getChildren().size());
            assertFalse(hasVisibleText(root, "PAUSED"));
            assertTrue(hasVisibleText(root, "IN-GAME SETTINGS"));
        });
    }

    @Test
    void tutorialOpensRulesPanelWithoutLeavingRoot() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            int childCountAfterShow = root.getChildren().size();

            clickButton(root, "TUTORIAL");

            assertTrue(overlay.isVisible());
            assertEquals(childCountAfterShow, root.getChildren().size());
            assertFalse(hasVisibleText(root, "PAUSED"));
            assertTrue(hasVisibleText(root, "GAME RULES"));
        });
    }

    @Test
    void disconnectShowsConfirmationBeforeCallback() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, "DISCONNECT");

            assertFalse(disconnectCalled.get());
            assertTrue(hasVisibleText(root, "DISCONNECT AND RETURN TO MAIN MENU?"));
            assertTrue(root.getChildren().size() >= 2, "Pause overlay and confirmation should both be on root");
        });
    }

    @Test
    void disconnectYesInvokesCallbackAndClosesPauseOverlay() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, "DISCONNECT");
            clickButton(root, "YES");

            assertTrue(disconnectCalled.get());
            assertFalse(overlay.isVisible());
        });
    }

    @Test
    void disconnectNoDismissesConfirmationWithoutCallback() throws Exception {
        runOnFxThread(() -> {
            overlay.show();
            clickButton(root, "DISCONNECT");
            int withConfirm = root.getChildren().size();

            clickButton(root, "NO");

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
                    && flatten(stackPane).stream().anyMatch(n -> n instanceof Labeled labeled && "PAUSED".equals(labeled.getText()))) {
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
                .filter(n -> n instanceof Labeled)
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
