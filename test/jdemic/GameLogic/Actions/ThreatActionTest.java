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

class ThreatActionTest {

    private GameState state;
    private PlayerState playerState;
    private Deck deck;
    private Card cardToDiscard;
    private List<Card> hand;
    private List<Card> rearrangedCards;

    @BeforeEach
    void setUp() {
        state = mock(GameState.class);
        playerState = mock(PlayerState.class);
        deck = mock(Deck.class);
        cardToDiscard = mock(Card.class);
        
        hand = new ArrayList<>();
        rearrangedCards = new ArrayList<>();

        when(state.getCardDeck()).thenReturn(deck);
        when(playerState.getHand()).thenReturn(hand);
    }

    @Test
    void testGetters() {
        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertEquals(cardToDiscard, action.getCardToDiscard());
        assertEquals(rearrangedCards, action.getRearrangedCards());
    }

    @Test
    void testIsValid_NullChecks() {
        ThreatAction action;

        action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertFalse(action.isValid(state, null));

        action = new ThreatAction(null, rearrangedCards);
        assertFalse(action.isValid(state, playerState));

        action = new ThreatAction(cardToDiscard, null);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_RearrangedCardsTooLarge() {
        for (int i = 0; i < 7; i++) {
            rearrangedCards.add(mock(Card.class));
        }
        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_CardNotInHand() {
        Card topCard = mock(Card.class);
        rearrangedCards.add(topCard);
        
        when(deck.getTopInfectionCards(1)).thenReturn(List.of(topCard));
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.THREAT);

        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongCardType() {
        hand.add(cardToDiscard);
        Card topCard = mock(Card.class);
        rearrangedCards.add(topCard);
        
        when(deck.getTopInfectionCards(1)).thenReturn(List.of(topCard));
        when(cardToDiscard.getType()).thenReturn(CardType.CITY);

        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongEventType() {
        hand.add(cardToDiscard);
        Card topCard = mock(Card.class);
        rearrangedCards.add(topCard);
        
        when(deck.getTopInfectionCards(1)).thenReturn(List.of(topCard));
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.FIREWALL);

        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_SizeMismatch() {
        hand.add(cardToDiscard);
        rearrangedCards.add(mock(Card.class));
        
        when(deck.getTopInfectionCards(1)).thenReturn(new ArrayList<>());

        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_ContentMismatch() {
        hand.add(cardToDiscard);
        Card realTopCard = mock(Card.class);
        Card userSuppliedCard = mock(Card.class);
        rearrangedCards.add(userSuppliedCard);
        
        when(deck.getTopInfectionCards(1)).thenReturn(List.of(realTopCard));
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.THREAT);

        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_Success() {
        hand.add(cardToDiscard);
        Card topCard = mock(Card.class);
        rearrangedCards.add(topCard);
        
        when(deck.getTopInfectionCards(1)).thenReturn(List.of(topCard));
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.THREAT);

        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        assertTrue(action.isValid(state, playerState));
    }

    @Test
    void testExecute_Valid() {
        hand.add(cardToDiscard);
        Card topCard = mock(Card.class);
        rearrangedCards.add(topCard);
        
        when(deck.getTopInfectionCards(1)).thenReturn(List.of(topCard));
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.THREAT);

        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        action.execute(state, playerState);

        assertFalse(hand.contains(cardToDiscard));
        verify(deck).discard(cardToDiscard);
        verify(deck).reorderTopInfectionCards(rearrangedCards);
    }

    @Test
    void testExecute_Invalid() {
        ThreatAction action = new ThreatAction(cardToDiscard, rearrangedCards);
        action.execute(state, playerState);

        verify(deck, never()).discard(any());
        verify(deck, never()).reorderTopInfectionCards(any());
    }
}