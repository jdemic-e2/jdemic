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

    public boolean isValid(GameState state, PlayerState playerState) 
    {
        // both cities must have research stations.
        CityNode currentCity = playerState.getPlayerCurrentCity();
        return currentCity.hasResearchStation() && destination.hasResearchStation();
    }

    @Override public void execute(GameState state, PlayerState playerState) 
    {
        if(isValid(state, playerState))
        {
            playerState.setCurrentCity(destination);
        }
    }
}