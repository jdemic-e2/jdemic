package jdemic.GameLogic.Actions.Movement;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;

public class DirectFlightAction extends GameAction {

    private CityNode destination;
    private Card cardToDiscard;

    public DirectFlightAction(CityNode destination, Card cardToDiscard) 
    {
        this.destination = destination;
        this.cardToDiscard = cardToDiscard;
    }

    public CityNode getDestination() {
        return this.destination;
    }
    
    public boolean isValid(GameState state, PlayerState playerState)
    {
        return cardToDiscard != null
                && playerState.getHand().contains(cardToDiscard)
                && cardToDiscard.getType() == CardType.CITY
                && cardToDiscard.getTargetCity() == destination;
    }

    @Override public void execute(GameState state, PlayerState playerState) 
    {
        if(isValid(state, playerState))
        {
            playerState.getHand().remove(cardToDiscard);
            playerState.setCurrentCity(destination);
        }
    }
}