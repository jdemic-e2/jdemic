package jdemic.ui;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class CardDeckViewTest {

    private SimpleDoubleProperty sceneWidth;
    private SimpleDoubleProperty sceneHeight;
    private CardDeckView deckView;

    @Start
    void start(Stage stage) {
        // Ensures Toolkit initialization
    }

    @BeforeEach
    void setUp() {
        sceneWidth = new SimpleDoubleProperty(1024);
        sceneHeight = new SimpleDoubleProperty(768);
        deckView = new CardDeckView(sceneWidth, sceneHeight);
    }

    @Test
    void testInitialDeckIsEmpty() {
        ObservableList<Card> cards = deckView.getCards();
        assertTrue(cards.isEmpty());
    }

    @Test
    void testSetCardsPopulatesDeck() {
        Card card1 = createMockCard("City A", CardType.CITY);
        Card card2 = createMockCard("Infection B", CardType.INFECTION);

        deckView.setCards(List.of(card1, card2));

        assertEquals(2, deckView.getCards().size());
        assertEquals("City A", deckView.getCards().get(0).getCardName());
    }

    @Test
    void testSetCardsHandlesNullGracefully() {
        deckView.setCards(null);
        assertTrue(deckView.getCards().isEmpty());
    }

    private Card createMockCard(String name, CardType type) {
        Card card = Mockito.mock(Card.class);
        when(card.getCardName()).thenReturn(name);
        when(card.getType()).thenReturn(type);
        when(card.getEffectDescription()).thenReturn("Description for " + name);
        return card;
    }
}