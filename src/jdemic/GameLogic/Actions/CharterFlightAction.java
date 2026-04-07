package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;

public class CharterFlightAction extends GameAction {
    //Charter Flight -> Verify if you have the required city.
    
    private CityNode destination;
    private Card cardToDiscard;

    public CharterFlightAction(CityNode destination, Card cardToDiscard) {
        this.destination = destination;
        this.cardToDiscard = cardToDiscard;
    }

    public boolean isValid(GameState gameState, PlayerState playerState) 
    {
        // Player must have the card matching their CURRENT city
        CityNode currentCity = playerState.getPlayerCurrentCity();
        return playerState.getHand().stream()
            .anyMatch(c -> c.getType() == CardType.CITY && c.getTargetCity() == currentCity);
    }

    @Override 
    public void execute(GameState state, PlayerState playerState) 
    {
        if(isValid(state, playerState))
        {
            playerState.getHand().remove(cardToDiscard);
            playerState.setCurrentCity(destination);
        }
    }
}