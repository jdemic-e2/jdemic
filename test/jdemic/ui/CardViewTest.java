package jdemic.ui;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.stage.Stage;
import javafx.util.Duration;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class CardViewTest {

    private Card mockCard;
    private SimpleDoubleProperty widthProp;
    private SimpleDoubleProperty heightProp;
    private SimpleDoubleProperty translateYProp;
    private Duration duration;
    private CardView cardView;

    @Start
    void start(Stage stage) {
        // Needs to be empty, but ensures JavaFX toolkit initializes
    }

    @BeforeEach
    void setUp() {
        mockCard = Mockito.mock(Card.class);
        when(mockCard.getCardName()).thenReturn("Test Card");
        when(mockCard.getType()).thenReturn(CardType.CITY);
        when(mockCard.getEffectDescription()).thenReturn("Sample Description");

        widthProp = new SimpleDoubleProperty(170);
        heightProp = new SimpleDoubleProperty(245);
        translateYProp = new SimpleDoubleProperty(-22);
        duration = Duration.millis(50);

        cardView = new CardView(mockCard, widthProp, heightProp, translateYProp, duration);
    }

    @Test
    void testInitializationAndBindings() {
        assertEquals(mockCard, cardView.getCard());
        assertEquals(170, cardView.getPrefWidth());
        assertEquals(245, cardView.getPrefHeight());
        assertFalse(cardView.isSelected());
    }

    @Test
    void testSelectionStateAndAnimationTrigger() {
        cardView.setSelected(true);
        assertTrue(cardView.isSelected());

        cardView.setSelected(false);
        assertFalse(cardView.isSelected());
    }

    @Test
    void testClickTriggersOnSelectedConsumer(FxRobot robot) {
        AtomicBoolean consumed = new AtomicBoolean(false);
        cardView.setOnSelected(cv -> consumed.set(true));

        robot.interact(() -> cardView.getOnMouseClicked().handle(null));
        assertTrue(consumed.get());
    }

    @Test
    void testNullCardThrowsException() {
        assertThrows(NullPointerException.class, () -> 
            new CardView(null, widthProp, heightProp, translateYProp, duration)
        );
    }
}