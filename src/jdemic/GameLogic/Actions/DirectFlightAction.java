package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
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

    @Override public boolean isValid(GameState state) 
    {
        return false;
    }

    public boolean isValid(PlayerState playerState) 
    {
        // check if the player has the respective city card.
        return playerState.getHand().stream().anyMatch(c -> c.getType() == CardType.CITY && c.getTargetCity() == destination);
    }

    @Override public void execute(GameState state, PlayerState playerState) 
    {
        if(isValid(playerState))
        {
            playerState.getHand().remove(cardToDiscard);
            playerState.setCurrentCity(destination);
        }
    }
}