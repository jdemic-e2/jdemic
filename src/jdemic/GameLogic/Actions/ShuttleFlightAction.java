package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;

public class ShuttleFlightAction extends GameAction {
    
    private CityNode destination;

    public ShuttleFlightAction(CityNode destination) 
    {
        this.destination = destination;
    }

    @Override public boolean isValid(GameState state) 
    {
        return false;
    }

    public boolean isValid(PlayerState playerState) 
    {
        // ambele orase trebuie sa aiba centre de cercetare
        CityNode currentCity = playerState.getPlayerCurrentCity();
        return currentCity.hasResearchStation() && destination.hasResearchStation();
    }

    @Override public void execute(GameState state, PlayerState playerState) 
    {
        if(isValid(playerState))
        {
            playerState.setCurrentCity(destination);
        }
    }
}