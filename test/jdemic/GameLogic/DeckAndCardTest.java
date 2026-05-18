package jdemic.GameLogic;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckAndCardTest {

    @Test
    void shouldCreateDeckAndDrawTwoCardsIntoPlayerHand() {
        GameManager manager = newManager();
        Deck deck = manager.getState().getCardDeck();
        PlayerState playerState = manager.getCurrentPlayer();
        int startingCards = deck.getRemainingCardsCount();

        deck.drawHand(playerState);

        assertEquals(2, playerState.getHand().size());
        assertEquals(startingCards - 2, deck.getRemainingCardsCount());
        assertFalse(deck.isEmpty());
    }

    @Test
    void shouldDescribeCardEffectsByType() {
        CityNode atlanta = new CityNode("Atlanta", DiseaseColor.BLUE, 0.25f, 0.39f);
        Card cityCard = new Card("Atlanta", CardType.CITY, atlanta);
        Card infectionCard = new Card("Atlanta", CardType.INFECTION, atlanta);
        Card epidemicCard = new Card("System Breach", CardType.EPIDEMIC, null);
        Card eventCard = new Card("Satellite Override", CardType.EVENT, null);
        eventCard.setEventType(Card.EventType.SATELLITE);

        assertEquals("You can use this card for any city-related action in Atlanta", cityCard.getEffectDescription());
        assertEquals("Use this card to infect Atlanta", infectionCard.getEffectDescription());
        assertEquals("Start an epidemic in the next location.", epidemicCard.getEffectDescription());
        assertEquals("Move any 1 pawn to any city on the board.", eventCard.getEffectDescription());
        assertSame(atlanta, cityCard.getTargetCity());
        assertEquals(CardType.CITY, cityCard.getType());
    }

    @Test
    void playerDiscardShouldRemoveCardAndUseDeckReference() {
        GameManager manager = newManager();
        PlayerState playerState = manager.getCurrentPlayer();
        CityNode atlanta = manager.getState().getMap().getCity("Atlanta");
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        playerState.addCard(atlantaCard);

        Card discardedCard = playerState.getHand().remove(0);
        manager.getState().getCardDeck().discard(discardedCard);

        assertTrue(playerState.getHand().isEmpty());
    }

    private GameManager newManager() {
        return new GameManager(List.of(new PlayerState("Ruben")));
    }
}
