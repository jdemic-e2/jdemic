package jdemic.GameLogic;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTurnDrawTest {

    @Test
    void dealsInitialHandsBasedOnPlayerCount() {
        PlayerState first = new PlayerState("P1");
        PlayerState second = new PlayerState("P2");
        PlayerState third = new PlayerState("P3");
        GameManager manager = new GameManager(List.of(first, second, third));

        assertEquals(3, first.getHand().size());
        assertEquals(3, second.getHand().size());
        assertEquals(3, third.getHand().size());
        assertFalse(manager.isGameOver());
    }

    @Test
    void drawsTwoCardsThenAdvancesWhenHandStaysWithinLimit() {
        PlayerState first = new PlayerState("P1");
        PlayerState second = new PlayerState("P2");
        GameManager manager = new GameManager(List.of(first, second));

        assertEquals(4, first.getHand().size());
        assertEquals(4, second.getHand().size());

        spendFourActions(manager, first);

        assertEquals(6, first.getHand().size());
        assertFalse(first.getIsDiscarding());
        assertEquals(second, manager.getState().getCurrentPlayer());
        assertEquals(4, manager.getState().getActionsRemaining());
    }

    @Test
    void waitsForDiscardWhenDrawPushesHandAboveSeven() {
        PlayerState first = new PlayerState("P1");
        PlayerState second = new PlayerState("P2");
        GameManager manager = new GameManager(List.of(first, second));

        first.getHand().clear();
        for (int i = 0; i < 7; i++) {
            first.addCard(new Card("Card " + i, CardType.EVENT, null));
        }

        spendFourActions(manager, first);

        assertEquals(first, manager.getState().getCurrentPlayer());
        assertEquals(9, first.getHand().size());
        assertTrue(first.getIsDiscarding());
        assertEquals(0, manager.getState().getActionsRemaining());

        manager.discardCurrentPlayerCard(first, 0);
        assertEquals(first, manager.getState().getCurrentPlayer());
        assertEquals(8, first.getHand().size());
        assertTrue(first.getIsDiscarding());

        manager.discardCurrentPlayerCard(first, 0);
        assertEquals(7, first.getHand().size());
        assertFalse(first.getIsDiscarding());
        assertEquals(second, manager.getState().getCurrentPlayer());
        assertEquals(4, manager.getState().getActionsRemaining());
    }

    private void spendFourActions(GameManager manager, PlayerState player) {
        for (int i = 0; i < 4; i++) {
            manager.consumeAction(player);
        }
    }
}
