package jdemic.GameLogic;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class Player {
    
    public PlayerState playerState;

    public Player(PlayerState state) {
        this.playerState = state;
    }
    

    // Metoda pentru schimbarea locatiei, fara validare
    public boolean move(CityNode destination) {
        return false;
    }

    public void useCard(Card c) {}
    public void discardCard(Card c) {}

    public void syncStateFromServer(PlayerState newState) {}
}