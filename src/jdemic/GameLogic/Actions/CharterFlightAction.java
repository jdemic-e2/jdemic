package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;

public class CharterFlightAction extends GameAction {
    //Charter Flight -> Verifică dacă ai în mână cartea orașului în care te afli.
    
    private CityNode destination;
    private Card cardToDiscard;

    public CharterFlightAction(CityNode destination, Card cardToDiscard) {
        this.destination = destination;
        this.cardToDiscard = cardToDiscard;
    }

    @Override
    public boolean isValid(GameState state) {
        return false; // TODO
    }

    @Override
    public void execute(GameState state) {
        // TODO
    }
}