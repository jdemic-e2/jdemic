package jdemic.GameLogic;

import java.util.Set;
import java.util.List;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class Player {

    private PlayerState playerState;
    private Set<GameAction> actions;

    public Player(PlayerState state) {
        this.playerState = state;
    }

    // Fix the existing getState to return the actual field
    public PlayerState getState() {
        return this.playerState;
    }

    // Existing methods (keep as is or implement)
    public void endTurn() {}
    public void drawCards(Deck deck) {}
    public void executeAction(GameAction action) {}
    public void discardCard(Card c) {}
    public void syncStateFromServer(PlayerState newState) {}
}