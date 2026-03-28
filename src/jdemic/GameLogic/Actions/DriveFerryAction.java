package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.CityNode;

public class DriveFerryAction extends GameAction {
    //Drive / Ferry -> Verifică dacă orașele sunt vecine.
    
    private CityNode destination;

    public DriveFerryAction(CityNode destination) {
        this.destination = destination;
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