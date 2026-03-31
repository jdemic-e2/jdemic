package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;

public class ShuttleFlightAction extends GameAction {
    //Shuttle Flight -> Verifică dacă ambele orașe (curent și destinație) au stații de cercetare.
    
    private CityNode destination;

    public ShuttleFlightAction(CityNode destination) {
        this.destination = destination;
    }

    @Override
    public boolean isValid(GameState state) {
        return false; // TODO
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        // TODO
    }
}