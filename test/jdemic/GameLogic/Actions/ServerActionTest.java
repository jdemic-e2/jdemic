package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.Deck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerActionTest {

    private GameState state;
    private PlayerState playerState;
    private Deck deck;
    private Card cardToDiscard;
    private CityNode destinationCity;
    private List<Card> hand;

    @BeforeEach
    void setUp() {
        state = mock(GameState.class);
        playerState = mock(PlayerState.class);
        deck = mock(Deck.class);
        cardToDiscard = mock(Card.class);
        destinationCity = mock(CityNode.class);
        hand = new ArrayList<>();

        when(state.getCardDeck()).thenReturn(deck);
        when(playerState.getHand()).thenReturn(hand);
    }

    @Test
    void testGetters() {
        ServerAction action = new ServerAction(destinationCity, cardToDiscard);
        assertEquals(destinationCity, action.getDestinationCity());
        assertEquals(cardToDiscard, action.getCardToDiscard());
    }

    @Test
    void testIsValid_NullChecks() {
        ServerAction action;

        action = new ServerAction(destinationCity, cardToDiscard);
        assertFalse(action.isValid(state, null));

        action = new ServerAction(destinationCity, null);
        assertFalse(action.isValid(state, playerState));

        action = new ServerAction(null, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_CardNotInHand() {
        when(destinationCity.hasResearchStation()).thenReturn(false);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.SERVER);

        ServerAction action = new ServerAction(destinationCity, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongCardType() {
        hand.add(cardToDiscard);
        when(destinationCity.hasResearchStation()).thenReturn(false);
        when(cardToDiscard.getType()).thenReturn(CardType.CITY);

        ServerAction action = new ServerAction(destinationCity, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongEventType() {
        hand.add(cardToDiscard);
        when(destinationCity.hasResearchStation()).thenReturn(false);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.CONTROL);

        ServerAction action = new ServerAction(destinationCity, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_AlreadyHasResearchStation() {
        hand.add(cardToDiscard);
        when(destinationCity.hasResearchStation()).thenReturn(true);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.SERVER);

        ServerAction action = new ServerAction(destinationCity, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_Success() {
        hand.add(cardToDiscard);
        when(destinationCity.hasResearchStation()).thenReturn(false);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.SERVER);

        ServerAction action = new ServerAction(destinationCity, cardToDiscard);
        assertTrue(action.isValid(state, playerState));
    }

    @Test
    void testExecute_Valid() {
        hand.add(cardToDiscard);
        when(destinationCity.hasResearchStation()).thenReturn(false);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.SERVER);

        ServerAction action = new ServerAction(destinationCity, cardToDiscard);
        action.execute(state, playerState);

        assertFalse(hand.contains(cardToDiscard));
        verify(deck).discard(cardToDiscard);
        verify(destinationCity).addResearchStation();
    }

    @Test
    void testExecute_Invalid() {
        ServerAction action = new ServerAction(destinationCity, cardToDiscard);
        action.execute(state, playerState);

        verify(deck, never()).discard(any());
        verify(destinationCity, never()).addResearchStation();
    }
}