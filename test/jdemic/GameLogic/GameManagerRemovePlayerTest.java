package jdemic.GameLogic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class GameManagerRemovePlayerTest {

    // Players are created fresh for each test
    private PlayerState p1;
    private PlayerState p2;
    private PlayerState p3;

    @BeforeEach
    public void setUp() {
        p1 = new PlayerState("P1");
        p2 = new PlayerState("P2");
        p3 = new PlayerState("P3");
    }

    // -------------------------------------------------------------------------
    // Helper — create a GameManager with the given players, no initial hands
    // -------------------------------------------------------------------------
    private GameManager makeGame(PlayerState... players) {
        List<PlayerState> list = new ArrayList<>();
        for (PlayerState p : players) list.add(p);
        return new GameManager(list, false);
    }

    // =========================================================================
    // The exact scenario described in the bug report
    // =========================================================================

    /**
     * [P1, P2], currentPlayerIndex == 1 (P2's turn).
     * P1 disconnects. List becomes [P2].
     * currentPlayerIndex must become 0, getCurrentPlayer() must return P2.
     */
    @Test
    public void testRemovePlayerBeforeCurrentIndexDecrementsIndex() {
        GameManager gm = makeGame(p1, p2);
        gm.getState().setCurrentPlayerIndex(1); // P2's turn

        gm.removePlayer(p1);

        assertEquals(0, gm.getState().getCurrentPlayerIndex(),
                "Index must be decremented when a player before the current one is removed");
        assertEquals(p2, gm.getCurrentPlayer(),
                "getCurrentPlayer() must return P2 after P1 is removed");
    }

    // =========================================================================
    // Removing the current player
    // =========================================================================

    /**
     * [P1, P2, P3], currentPlayerIndex == 1 (P2's turn).
     * P2 (the current player) disconnects. List becomes [P1, P3].
     * Index 1 is still valid and now points to P3.
     */
    @Test
    public void testRemoveCurrentPlayerIndexPointsToNextPlayer() {
        GameManager gm = makeGame(p1, p2, p3);
        gm.getState().setCurrentPlayerIndex(1);

        gm.removePlayer(p2);

        List<PlayerState> remaining = gm.getState().getPlayers();
        assertEquals(2, remaining.size());
        // Index should still be valid
        int idx = gm.getState().getCurrentPlayerIndex();
        assertTrue(idx >= 0 && idx < remaining.size(),
                "currentPlayerIndex must be within bounds after removing the current player");
        assertDoesNotThrow(gm::getCurrentPlayer,
                "getCurrentPlayer() must not throw after the current player is removed");
    }

    /**
     * [P1, P2], currentPlayerIndex == 1 (P2's turn).
     * P2 disconnects. List becomes [P1].
     * Index must be clamped to 0 (the only remaining player).
     */
    @Test
    public void testRemoveLastPlayerClampsIndex() {
        GameManager gm = makeGame(p1, p2);
        gm.getState().setCurrentPlayerIndex(1);

        gm.removePlayer(p2);

        assertEquals(1, gm.getState().getPlayers().size());
        assertEquals(0, gm.getState().getCurrentPlayerIndex(),
                "Index must clamp to 0 when the last-indexed player is removed");
        assertEquals(p1, gm.getCurrentPlayer());
    }

    // =========================================================================
    // Removing a player after the current index
    // =========================================================================

    /**
     * [P1, P2, P3], currentPlayerIndex == 0 (P1's turn).
     * P3 (index 2) disconnects. Index must stay at 0 and still point to P1.
     */
    @Test
    public void testRemovePlayerAfterCurrentIndexDoesNotChangeIndex() {
        GameManager gm = makeGame(p1, p2, p3);
        gm.getState().setCurrentPlayerIndex(0);

        gm.removePlayer(p3);

        assertEquals(0, gm.getState().getCurrentPlayerIndex(),
                "Index must not change when a player after it is removed");
        assertEquals(p1, gm.getCurrentPlayer());
    }

    /**
     * [P1, P2, P3], currentPlayerIndex == 1 (P2's turn).
     * P3 disconnects. Index stays at 1 and still points to P2.
     */
    @Test
    public void testRemovePlayerAfterCurrentIndexKeepsCurrentPlayer() {
        GameManager gm = makeGame(p1, p2, p3);
        gm.getState().setCurrentPlayerIndex(1);

        gm.removePlayer(p3);

        assertEquals(1, gm.getState().getCurrentPlayerIndex());
        assertEquals(p2, gm.getCurrentPlayer());
    }

    // =========================================================================
    // All players removed
    // =========================================================================

    @Test
    public void testRemoveOnlyPlayerResetsIndexToZero() {
        GameManager gm = makeGame(p1);
        gm.getState().setCurrentPlayerIndex(0);

        gm.removePlayer(p1);

        assertEquals(0, gm.getState().getCurrentPlayerIndex(),
                "Index must be 0 when all players are removed");
        assertTrue(gm.getState().getPlayers().isEmpty());
        assertNull(gm.getCurrentPlayer(),
                "getCurrentPlayer() must return null when no players remain");
    }

    @Test
    public void testRemoveAllPlayersOneByOneNeverThrows() {
        GameManager gm = makeGame(p1, p2, p3);
        gm.getState().setCurrentPlayerIndex(2);

        assertDoesNotThrow(() -> {
            gm.removePlayer(p3); // removes current, index clamped
            gm.removePlayer(p1);
            gm.removePlayer(p2);
        });

        assertTrue(gm.getState().getPlayers().isEmpty());
        assertNull(gm.getCurrentPlayer());
    }

    // =========================================================================
    // getCurrentPlayer() never throws regardless of removal order
    // =========================================================================

    @Test
    public void testGetCurrentPlayerNeverThrowsAfterAnyRemoval() {
        // Exhaustively test all (player, currentIndex) combos for a 3-player game
        PlayerState[] all = {p1, p2, p3};
        for (int startIdx = 0; startIdx < 3; startIdx++) {
            for (PlayerState toRemove : all) {
                GameManager gm = makeGame(p1, p2, p3);
                gm.getState().setCurrentPlayerIndex(startIdx);
                gm.removePlayer(toRemove);

                final GameManager gmFinal = gm;
                assertDoesNotThrow(gmFinal::getCurrentPlayer,
                        "getCurrentPlayer() must not throw after removing " + toRemove.getPlayerName()
                        + " when startIndex was " + startIdx);

                int idx = gm.getState().getCurrentPlayerIndex();
                int size = gm.getState().getPlayers().size();
                if (size > 0) {
                    assertTrue(idx >= 0 && idx < size,
                            "Index " + idx + " out of bounds for size " + size);
                }
            }
        }
    }

    // =========================================================================
    // Removing a player not in the list is a no-op
    // =========================================================================

    @Test
    public void testRemoveUnknownPlayerIsNoOp() {
        GameManager gm = makeGame(p1, p2);
        gm.getState().setCurrentPlayerIndex(1);

        PlayerState stranger = new PlayerState("Stranger");
        gm.removePlayer(stranger);

        assertEquals(2, gm.getState().getPlayers().size(),
                "Player count must be unchanged when removing an unknown player");
        assertEquals(1, gm.getState().getCurrentPlayerIndex(),
                "currentPlayerIndex must be unchanged when removing an unknown player");
        assertEquals(p2, gm.getCurrentPlayer());
    }
}