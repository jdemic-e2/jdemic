package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.Deck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemControlActionTest {

    private GameState state;
    private PlayerState playerState;
    private Deck deck;
    private Card cardToDiscard;
    private Card infectionCardToRemove;
    private List<Card> hand;
    private List<Card> discardPile;

    @BeforeEach
    void setUp() {
        state = mock(GameState.class);
        playerState = mock(PlayerState.class);
        deck = mock(Deck.class);
        cardToDiscard = mock(Card.class);
        infectionCardToRemove = mock(Card.class);
        
        hand = new ArrayList<>();
        discardPile = new ArrayList<>();

        when(state.getCardDeck()).thenReturn(deck);
        when(playerState.getHand()).thenReturn(hand);
        when(deck.getInfectionDiscardPile()).thenReturn(discardPile);
    }

    @Test
    void testGetters() {
        SystemControlAction action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        assertEquals(cardToDiscard, action.getCardToDiscard());
        assertEquals(infectionCardToRemove, action.getInfectionCardToRemove());
    }

    @Test
    void testIsValid_NullChecks() {
        SystemControlAction action;

        action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        assertFalse(action.isValid(state, null));

        action = new SystemControlAction(null, infectionCardToRemove);
        assertFalse(action.isValid(state, playerState));

        action = new SystemControlAction(cardToDiscard, null);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_CardNotInHand() {
        discardPile.add(infectionCardToRemove);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.CONTROL);

        SystemControlAction action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_InfectionCardNotInDiscard() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.CONTROL);

        SystemControlAction action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongCardType() {
        hand.add(cardToDiscard);
        discardPile.add(infectionCardToRemove);
        when(cardToDiscard.getType()).thenReturn(CardType.CITY);

        SystemControlAction action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongEventType() {
        hand.add(cardToDiscard);
        discardPile.add(infectionCardToRemove);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.FIREWALL);

        SystemControlAction action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_Success() {
        hand.add(cardToDiscard);
        discardPile.add(infectionCardToRemove);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.CONTROL);

        SystemControlAction action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        assertTrue(action.isValid(state, playerState));
    }

    @Test
    void testExecute_Valid() {
        hand.add(cardToDiscard);
        discardPile.add(infectionCardToRemove);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.CONTROL);

        SystemControlAction action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        action.execute(state, playerState);

        assertFalse(hand.contains(cardToDiscard));
        verify(deck).discard(cardToDiscard);
        verify(deck).removeInfectionCardFromDiscard(infectionCardToRemove);
    }

    @Test
    void testExecute_Invalid() {
        SystemControlAction action = new SystemControlAction(cardToDiscard, infectionCardToRemove);
        action.execute(state, playerState);

        verify(deck, never()).discard(any());
        verify(deck, never()).removeInfectionCardFromDiscard(any());
    }
}