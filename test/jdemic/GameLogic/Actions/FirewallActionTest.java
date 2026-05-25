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

class FirewallActionTest {

    private GameState state;
    private PlayerState playerState;
    private Deck deck;
    private Card cardToDiscard;
    private List<Card> hand;

    @BeforeEach
    void setUp() {
        state = mock(GameState.class);
        playerState = mock(PlayerState.class);
        deck = mock(Deck.class);
        cardToDiscard = mock(Card.class);
        hand = new ArrayList<>();

        when(state.getCardDeck()).thenReturn(deck);
        when(playerState.getHand()).thenReturn(hand);
    }

    @Test
    void testGetters() {
        FirewallAction action = new FirewallAction(cardToDiscard);
        assertEquals(cardToDiscard, action.getCardToDiscard());
    }

    @Test
    void testIsValid_NullChecks() {
        FirewallAction action;

        action = new FirewallAction(cardToDiscard);
        assertFalse(action.isValid(state, null));

        action = new FirewallAction(null);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_CardNotInHand() {
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.FIREWALL);

        FirewallAction action = new FirewallAction(cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongCardType() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.CITY);

        FirewallAction action = new FirewallAction(cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongEventType() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.CONTROL);

        FirewallAction action = new FirewallAction(cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_Success() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.FIREWALL);

        FirewallAction action = new FirewallAction(cardToDiscard);
        assertTrue(action.isValid(state, playerState));
    }

    @Test
    void testExecute_Valid() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.FIREWALL);

        FirewallAction action = new FirewallAction(cardToDiscard);
        action.execute(state, playerState);

        assertFalse(hand.contains(cardToDiscard));
        verify(deck).discard(cardToDiscard);
        verify(state).setSkipInfection(true);
    }

    @Test
    void testExecute_Invalid() {
        FirewallAction action = new FirewallAction(cardToDiscard);
        action.execute(state, playerState);

        verify(deck, never()).discard(any());
        verify(state, never()).setSkipInfection(anyBoolean());
    }
}