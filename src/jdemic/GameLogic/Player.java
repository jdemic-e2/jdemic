package jdemic.GameLogic;

import java.util.Set;

import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class Player {
    
    private PlayerState playerState;
    private Set<GameAction> actions;

    public Player(PlayerState state) {
        this.playerState = state;
    }
    
    public void endTurn() {}

    public void drawCards(Deck deck) {}

    public void executeAction(GameAction action) {}

    public PlayerState getState() {
        return null;
    }

    public void discardCard(Card c) {}

    public void syncStateFromServer(PlayerState newState) {}
}