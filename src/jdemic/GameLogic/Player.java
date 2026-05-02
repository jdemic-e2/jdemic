package jdemic.GameLogic;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Actions.GameAction;

public class Player {
    
    private PlayerState state;
    private GameClient gameClient; // Class that sends packages to the server

    public Player(PlayerState state, GameClient gameClient) {
        this.state = state;
        this.gameClient = gameClient;
    }

    /**
     * Method called by GUI
     */
    public void executeAction(GameAction action) {
        // Action name, ex: "DriveAction", "ShuttleFlightAction"
        String actionName = action.getClass().getSimpleName();
        String playerId = state.getPlayerName();

        String jsonPacket = String.format(
            "{\"type\":\"GAME_DATA\", \"playerId\":\"%s\", \"payload\":\"{\\\"action\\\":\\\"%s\\\"}\"}",
            playerId, 
            actionName
        );

        System.out.println("[Player] Executing action: " + actionName + ". Sending to server...");

        if (gameClient != null) {
            gameClient.sendPacket(jsonPacket);
        }
    }

    public PlayerState getState() {
        return state;
    }

    public void updateState(PlayerState newState) {
        this.state = newState;
    }
}