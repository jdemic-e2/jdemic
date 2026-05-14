package jdemic.frontend;

import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.PandemicMapGraph;
import jdemic.ui.gameplay.CardView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CardView} gameplay tile: layout, labels, hover-driven scale.
 * <p>
 * Not covered here (no stable API / too toolkit-timed):
 * <ul>
 *   <li>Tooltip popup text visibility — {@link javafx.scene.control.Tooltip} activation is asynchronous
 *        and environment-dependent; we only rely on {@code Tooltip.install} not throwing during construction.</li>
 *   <li>Selected / active-card state — current {@link CardView} has no selection model or pseudo-class hook.</li>
 * </ul>
 */
@ExtendWith(ApplicationExtension.class)
class CardViewFxTest {

    private CardView cardView;
    private StackPane scaleRoot;

    @Start
    void start(Stage stage) {
        scaleRoot = new StackPane();
        scaleRoot.setPrefSize(960, 720);
        PandemicMapGraph map = new PandemicMapGraph();
        Card card = new Card("Osaka", CardType.CITY, map.getCity("Osaka"));
        cardView = new CardView(card, scaleRoot);
        cardView.setId("card-under-test");
        scaleRoot.getChildren().add(cardView);
        stage.setScene(new Scene(scaleRoot, 960, 720));
        stage.show();
    }

    @Test
    void cardViewBuildsExpectedLabelsAndStyleClass(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(cardView.getStyleClass().contains("gameplay-card"));
        assertFalse(cardView.getChildren().isEmpty());
        VBox inner = (VBox) cardView.getChildren().get(0);
        Label typeLabel = (Label) inner.getChildren().get(0);
        Label nameLabel = (Label) inner.getChildren().get(1);
        assertEquals("CITY", typeLabel.getText());
        assertEquals("Osaka", nameLabel.getText());
    }

    @Test
    void responsiveBindingsTrackScaleRootSize(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> {
            scaleRoot.setPrefWidth(640);
            scaleRoot.setPrefHeight(480);
        });
        WaitForAsyncUtils.waitForFxEvents();
        VBox inner = (VBox) cardView.getChildren().get(0);
        assertTrue(inner.getPrefWidth() > 0);
        assertTrue(inner.getPrefHeight() > 0);
    }

    @Test
    void hoverDriveScalesInnerPastUnityWithoutThrowing(FxRobot robot) throws Exception {
        WaitForAsyncUtils.waitForFxEvents();
        VBox inner = (VBox) cardView.getChildren().get(0);
        // Stabilize: another test or default cursor position may have left the card hovered.
        robot.moveTo(new Point2D(20, 20));
        waitUntil(() -> inner.getScaleX() <= 1.01, 3000);
        assertEquals(1.0, inner.getScaleX(), 0.02);
        assertEquals(1.0, inner.getScaleY(), 0.02);

        Point2D scenePoint = cardView.localToScene(cardView.getWidth() / 2, cardView.getHeight() / 2);
        assertDoesNotThrow(() -> robot.moveTo(scenePoint));

        waitUntil(() -> inner.getScaleX() >= 1.04, 3000);

        Point2D away = new Point2D(scenePoint.getX() + 400, scenePoint.getY() + 300);
        robot.moveTo(away);
        waitUntil(() -> inner.getScaleX() <= 1.01, 3000);
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        assertTrue(condition.getAsBoolean(), "condition not met within " + timeoutMs + "ms");
    }
}
