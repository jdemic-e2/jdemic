package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;

public class DriveFerryAction extends GameAction {

    private CityNode destination;

    public DriveFerryAction(CityNode destination) 
    {
        this.destination = destination;
    }

    public boolean isValid(GameState state, PlayerState playerState) 
    {
        CityNode currentCity = playerState.getPlayerCurrentCity();
        return currentCity.getConnectedCities().contains(destination);
    }

    @Override public void execute(GameState state, PlayerState playerState) 
    {
        if(isValid(state, playerState))
        {
            playerState.setCurrentCity(destination);
        }
    }
}