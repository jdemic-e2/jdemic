package jdemic.GameLogic.Actions;
import jdemic.GameLogic.ServerRelatedClasses.GameState;

public interface GameAction {
    public boolean isValid(GameState state);
    public void execute(GameState state);
}
