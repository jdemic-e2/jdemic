package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;

public class DirectFlightAction extends GameAction {
    //Direct Flight -> Verifică dacă ai în mână cartea orașului destinație.

    private CityNode destination;
    private Card cardToDiscard;

    public DirectFlightAction(CityNode destination, Card cardToDiscard) {
        this.destination = destination;
        this.cardToDiscard = cardToDiscard;
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