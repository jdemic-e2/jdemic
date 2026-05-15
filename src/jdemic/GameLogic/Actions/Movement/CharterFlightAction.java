package jdemic.GameLogic.Actions.Movement;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Actions.GameAction;
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

    public CityNode getDestination() {
        return this.destination;
    }
    
    public boolean isValid(GameState gameState, PlayerState playerState)
    {
        CityNode currentCity = playerState.getPlayerCurrentCity();
        return cardToDiscard != null
                && playerState.getHand().contains(cardToDiscard)
                && cardToDiscard.getType() == CardType.CITY
                && cardToDiscard.getTargetCity() == currentCity;
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