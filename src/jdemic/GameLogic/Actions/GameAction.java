package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;

public abstract class GameAction {

    public abstract boolean isValid(GameState state);

    public abstract void execute(GameState state);
    
}