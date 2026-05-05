package jdemic.GameLogic;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Player {
    
    private PlayerState state;
    private GameClient gameClient; // Class that sends packages to the server
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("PlayerID", playerId);
        payload.put("GameAction", actionName);

        System.out.println("[Player] Executing action: " + actionName + ". Sending to server...");

        if (gameClient != null) {
            gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
        }
    }

    public void sendTestPacket() {
        if (gameClient == null) {
            System.err.println("[Player] Cannot send test packet without a GameClient.");
            return;
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("PlayerID", state.getPlayerName());
        payload.put("GameAction", "TEST_ACTION");
        payload.put("message", "Sample gameplay packet from Player.java");

        System.out.println("[Player] Sending sample test packet to server...");
        gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
    }

    public PlayerState getState() {
        return state;
    }

    public void updateState(PlayerState newState) {
        this.state = newState;
    }
}
