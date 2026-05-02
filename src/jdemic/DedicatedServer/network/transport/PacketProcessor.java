package jdemic.DedicatedServer.network.transport;

import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Player;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.Actions.DriveFerryAction; // for example
import jdemic.GameLogic.Actions.CharterFlightAction;
import jdemic.GameLogic.Actions.DirectFlightAction;
import jdemic.GameLogic.Actions.ShuttleFlightAction;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * PacketProcessor handles packets that have already passed validation and
 * converts them into the appropriate server-side actions. It checks the
 * packet type and routes each packet to the matching handler method, keeping
 * protocol logic separate from raw socket communication. This makes the
 * networking flow easier to maintain, extend, and integrate with other
 * modules such as session management, heartbeat handling, and game logic.
 */

public class PacketProcessor {

    private GameManager gameManager;

    public PacketProcessor(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void process(Packet packet) {
        if (packet == null) {
            System.err.println("[PacketProcessor] Cannot process a null packet.");
            return;
        }

        if (packet.getType() == PacketType.PING) {
            handlePing(packet);
        } else if (packet.getType() == PacketType.PONG) {
            handlePong(packet);
        } else if (packet.getType() == PacketType.GAME_DATA) {
            handleGameData(packet);
        } else if (packet.getType() == PacketType.CONNECT) {
            handleConnect(packet);
        } else if (packet.getType() == PacketType.DISCONNECT) {
            handleDisconnect(packet);
        } else if (packet.getType() == PacketType.ERROR) {
            handleError(packet);
        } else {
            System.err.println("[PacketProcessor] Unsupported packet type: " + packet.getType());
        }
    }

    private void handlePing(Packet packet) {
        System.out.println("[PacketProcessor] Received PING packet: " + packet);
    }

    private void handlePong(Packet packet) {
        System.out.println("[PacketProcessor] Received PONG packet: " + packet);
    }

    // here we do our code
    private void handleGameData(Packet packet) {
        System.out.println("[PacketProcessor] Received GAME_DATA packet: ");
        JsonNode payload = packet.getPayload();//local
        String playerId = payload.get("PlayerID").asText();
        String gameAction = payload.get("GameAction").asText();

        // Find PlayerState by playerId
        PlayerState playerState = null;
        for (PlayerState ps : gameManager.getState().getPlayers()) {
            if (ps.getPlayerName().equals(playerId)) {
                playerState = ps;
                break;
            }
        }

        if (playerState == null) {
            System.err.println("[PacketProcessor] Player not found: " + playerId);
            return;
        }

        // Create Player object (assuming Player has a constructor with PlayerState and GameClient, but GameClient is null for server)
        Player player = new Player(playerState, null); // GameClient is null on server

        // Create action based on gameAction string
        GameAction action = null;
        switch (gameAction) {
            case "DRIVE_FERRY":
                if (payload.has("destination") && payload.get("destination").isTextual()) {
                    String destDrive = payload.get("destination").asText();
                    CityNode cityDrive = gameManager.getState().getMap().getCity(destDrive);
                    if (cityDrive != null) {
                        action = new DriveFerryAction(cityDrive);
                    } else {
                        System.err.println("[PacketProcessor] Invalid destination: " + destDrive);
                        return;
                    }
                } else {
                    System.err.println("[PacketProcessor] Missing or invalid destination for DRIVE_FERRY");
                    return;
                }
                break;
            case "CHARTER_FLIGHT":
                if (payload.has("destination") && payload.get("destination").isTextual() &&
                    payload.has("cardIndex") && payload.get("cardIndex").isInt()) {
                    String destCharter = payload.get("destination").asText();
                    CityNode cityCharter = gameManager.getState().getMap().getCity(destCharter);
                    int cardIndexCharter = payload.get("cardIndex").asInt();
                    if (cityCharter != null && cardIndexCharter >= 0 && cardIndexCharter < playerState.getHand().size()) {
                        Card cardCharter = playerState.getHand().get(cardIndexCharter);
                        action = new CharterFlightAction(cityCharter, cardCharter);
                    } else {
                        System.err.println("[PacketProcessor] Invalid parameters for CHARTER_FLIGHT");
                        return;
                    }
                } else {
                    System.err.println("[PacketProcessor] Missing or invalid parameters for CHARTER_FLIGHT");
                    return;
                }
                break;
            case "DIRECT_FLIGHT":
                if (payload.has("destination") && payload.get("destination").isTextual() &&
                    payload.has("cardIndex") && payload.get("cardIndex").isInt()) {
                    String destDirect = payload.get("destination").asText();
                    CityNode cityDirect = gameManager.getState().getMap().getCity(destDirect);
                    int cardIndexDirect = payload.get("cardIndex").asInt();
                    if (cityDirect != null && cardIndexDirect >= 0 && cardIndexDirect < playerState.getHand().size()) {
                        Card cardDirect = playerState.getHand().get(cardIndexDirect);
                        action = new DirectFlightAction(cityDirect, cardDirect);
                    } else {
                        System.err.println("[PacketProcessor] Invalid parameters for DIRECT_FLIGHT");
                        return;
                    }
                } else {
                    System.err.println("[PacketProcessor] Missing or invalid parameters for DIRECT_FLIGHT");
                    return;
                }
                break;
            case "SHUTTLE_FLIGHT":
                if (payload.has("destination") && payload.get("destination").isTextual()) {
                    String destShuttle = payload.get("destination").asText();
                    CityNode cityShuttle = gameManager.getState().getMap().getCity(destShuttle);
                    if (cityShuttle != null) {
                        action = new ShuttleFlightAction(cityShuttle);
                    } else {
                        System.err.println("[PacketProcessor] Invalid destination: " + destShuttle);
                        return;
                    }
                } else {
                    System.err.println("[PacketProcessor] Missing or invalid destination for SHUTTLE_FLIGHT");
                    return;
                }
                break;
            default:
                System.err.println("[PacketProcessor] Unknown action: " + gameAction);
                return;
        }

        // Perform action
        gameManager.performAction(player, action);

        System.out.println("[PacketProcessor] Action performed: " + gameAction + " for player " + playerId);
    }

    private void handleConnect(Packet packet) {
        System.out.println("[PacketProcessor] Received CONNECT packet: " + packet);
    }

    private void handleDisconnect(Packet packet) {
        System.out.println("[PacketProcessor] Received DISCONNECT packet: " + packet);
    }

    private void handleError(Packet packet) {
        System.err.println("[PacketProcessor] Received ERROR packet: " + packet);
    }
}