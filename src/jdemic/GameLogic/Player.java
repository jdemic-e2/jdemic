package jdemic.GameLogic;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class Player {
    
    private PlayerState playerState;
    public Deck deckReference;

    public Player(PlayerState state) {
        this.playerState = state;
    }

    // Players can have maximum 7 cards. If player has 6/7 cards, enter discard mode. In discard mode, select cards to discard until you have 5 cards or below. Then you can draw.
    public void drawCards(Deck deck) {
        while(playerState.getHand().size() > 5){
            if(playerState.getIsDiscarding() == false){
                playerState.setIsDiscarding(true);
            }
        }
        playerState.setIsDiscarding(false);
        deck.drawHand(this.playerState);
        
    }

    public PlayerState getState() {
        return this.playerState;
    }

    // make a new object to add to the discard pile, then remove the card from the player deck.
    public void discardCard(int index) {
        Card c = playerState.getCard(index);
        playerState.removeCard(index);
        deckReference.discard(c);
    }

    public void syncStateFromServer(PlayerState newState) {
        this.playerState = newState;
    }
}