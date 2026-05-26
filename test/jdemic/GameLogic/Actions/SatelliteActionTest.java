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

class SatelliteActionTest {

    private GameState state;
    private PlayerState playerState;
    private Deck deck;
    private Card cardToDiscard;
    private CityNode destination;
    private List<Card> hand;
    private List<PlayerState> playerList;

    @BeforeEach
    void setUp() {
        state = mock(GameState.class);
        playerState = mock(PlayerState.class);
        deck = mock(Deck.class);
        cardToDiscard = mock(Card.class);
        destination = mock(CityNode.class);
        
        hand = new ArrayList<>();
        playerList = new ArrayList<>();

        when(state.getCardDeck()).thenReturn(deck);
        when(playerState.getHand()).thenReturn(hand);
        when(state.getPlayers()).thenReturn(playerList);
    }

    @Test
    void testGetters() {
        SatelliteAction action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        assertEquals("Pawn1", action.getTargetPawn());
        assertEquals(destination, action.getDestination());
        assertEquals(cardToDiscard, action.getCardToDiscard());
    }

    @Test
    void testIsValid_NullChecks() {
        SatelliteAction action;

        action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        assertFalse(action.isValid(state, null));

        action = new SatelliteAction("Pawn1", destination, null);
        assertFalse(action.isValid(state, playerState));

        action = new SatelliteAction("Pawn1", null, cardToDiscard);
        assertFalse(action.isValid(state, playerState));

        action = new SatelliteAction(null, destination, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_CardNotInHand() {
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.SATELLITE);

        SatelliteAction action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongCardType() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.CITY);

        SatelliteAction action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_WrongEventType() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.CONTROL);

        SatelliteAction action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        assertFalse(action.isValid(state, playerState));
    }

    @Test
    void testIsValid_Success() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.SATELLITE);

        SatelliteAction action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        assertTrue(action.isValid(state, playerState));
    }

    @Test
    void testExecute_ValidAndFindsTargetPawn() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.SATELLITE);

        PlayerState targetPlayer = mock(PlayerState.class);
        when(targetPlayer.getPlayerName()).thenReturn("Pawn1");
        
        PlayerState otherPlayer = mock(PlayerState.class);
        when(otherPlayer.getPlayerName()).thenReturn("Pawn2");

        playerList.add(otherPlayer);
        playerList.add(targetPlayer);

        SatelliteAction action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        action.execute(state, playerState);

        assertFalse(hand.contains(cardToDiscard));
        verify(deck).discard(cardToDiscard);
        verify(targetPlayer).setCurrentCity(destination);
        verify(otherPlayer, never()).setCurrentCity(any());
    }

    @Test
    void testExecute_ValidButTargetPawnNotFound() {
        hand.add(cardToDiscard);
        when(cardToDiscard.getType()).thenReturn(CardType.EVENT);
        when(cardToDiscard.getEventType()).thenReturn(Card.EventType.SATELLITE);

        PlayerState otherPlayer = mock(PlayerState.class);
        when(otherPlayer.getPlayerName()).thenReturn("Pawn2");
        playerList.add(otherPlayer);

        SatelliteAction action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        action.execute(state, playerState);

        assertFalse(hand.contains(cardToDiscard));
        verify(deck).discard(cardToDiscard);
        verify(otherPlayer, never()).setCurrentCity(any());
    }

    @Test
    void testExecute_Invalid() {
        SatelliteAction action = new SatelliteAction("Pawn1", destination, cardToDiscard);
        action.execute(state, playerState);

        verify(deck, never()).discard(any());
    }
}