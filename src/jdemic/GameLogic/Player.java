package jdemic.GameLogic;

import java.util.Set;

import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class Player {
    
    private PlayerState playerState;
    private Set<GameAction> actions;
    public Deck deckReference;

    public Player(PlayerState state) {
        this.playerState = state;
    }
    
    public void endTurn() {}

    public void drawCards(Deck deck) {
        while(playerState.getHand().size() > 5){
            if(playerState.getIsDiscarding() == false){
                playerState.setIsDiscarding(true);
            }
        }
        playerState.setIsDiscarding(false);
        deck.drawHand(this.playerState);
        
    }

    public void executeAction(GameAction action) {}

    public PlayerState getState() {
        return this.playerState;
    }

    public void discardCard(int index) {
        Card c = playerState.getCard(index);
        playerState.removeCard(index);
        deckReference.discard(c);
    }

    public void syncStateFromServer(PlayerState newState) {
        this.playerState = newState;
    }
}