package jdemic.frontend;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jdemic.ui.gameplay.CardDeckView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CardDeckView}: draw-pile count display and size bindings on a scale root.
 * <p>
 * This control is a {@link javafx.scene.layout.VBox}, not a {@link javafx.scene.control.ScrollPane};
 * scroll behaviour is therefore not applicable here.
 */
@ExtendWith(ApplicationExtension.class)
class CardDeckViewFxTest {

    private CardDeckView deckView;
    private StackPane scaleRoot;

    @Start
    void start(Stage stage) {
        scaleRoot = new StackPane();
        scaleRoot.setPrefSize(900, 700);
        deckView = new CardDeckView(42, scaleRoot);
        deckView.setId("deck-under-test");
        scaleRoot.getChildren().add(deckView);
        stage.setScene(new Scene(scaleRoot, 900, 700));
        stage.show();
    }

    @Test
    void initialCountAndTitleRender(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        Label title = (Label) deckView.getChildren().get(0);
        Label count = (Label) deckView.getChildren().get(1);
        assertEquals("PLAYER DECK", title.getText());
        assertEquals("42", count.getText());
    }

    @Test
    void setRemainingCountUpdatesCountLabel(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> deckView.setRemainingCount(7));
        WaitForAsyncUtils.waitForFxEvents();
        Label count = (Label) deckView.getChildren().get(1);
        assertEquals("7", count.getText());
    }

    @Test
    void widthBindingsProduceNonTrivialSizeAfterLayout(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> {
            scaleRoot.setPrefWidth(1200);
            scaleRoot.setPrefHeight(800);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(deckView.getWidth() > 0, "deck should participate in layout");
        assertTrue(deckView.getPrefWidth() > 0 || deckView.minWidthProperty().isBound());
    }
}
