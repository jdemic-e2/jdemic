package jdemic.GameLogic;

import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Actions.Movement.DriveFerryAction;
import jdemic.GameLogic.Actions.Movement.DirectFlightAction;
import jdemic.GameLogic.Actions.Movement.CharterFlightAction;
import jdemic.GameLogic.Actions.Movement.ShuttleFlightAction;
import jdemic.GameLogic.Actions.FirewallAction;
import jdemic.GameLogic.Actions.SatelliteAction;
import jdemic.GameLogic.Actions.ServerAction;
import jdemic.GameLogic.Actions.SystemControlAction;
import jdemic.GameLogic.Actions.ThreatAction;
import jdemic.GameLogic.Actions.Other.BuildResearchStation;
import jdemic.GameLogic.Actions.Other.DiscoverCure;
import jdemic.GameLogic.Actions.Other.ShareKnowledge;
import jdemic.GameLogic.Actions.Other.TreatDisease;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
        String actionName = toPacketAction(action);
        String playerId = state.getPlayerName();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("PlayerID", playerId);
        payload.put("GameAction", actionName);

        if (action instanceof DriveFerryAction) {
            DriveFerryAction moveAction = (DriveFerryAction) action;
            payload.put("destination", moveAction.getDestination().getName());
        }
        else if (action instanceof DirectFlightAction) {
            DirectFlightAction moveAction = (DirectFlightAction) action;
            payload.put("destination", moveAction.getDestination().getName());
            payload.put("cardIndex", getCardIndex(moveAction.getCardToDiscard()));
        } 
        else if (action instanceof CharterFlightAction) {
            CharterFlightAction moveAction = (CharterFlightAction) action;
            payload.put("destination", moveAction.getDestination().getName());
            payload.put("cardIndex", getCardIndex(moveAction.getCardToDiscard()));
        } 
        else if (action instanceof ShuttleFlightAction) {
            ShuttleFlightAction moveAction = (ShuttleFlightAction) action;
            payload.put("destination", moveAction.getDestination().getName());
        } else if (action instanceof TreatDisease treatDisease) {
            payload.put("color", treatDisease.getTargetDisease().name());
        } else if (action instanceof ShareKnowledge shareKnowledge) {
            boolean currentPlayerGives = shareKnowledge.getGiver().getPlayerName().equals(state.getPlayerName());
            PlayerState target = currentPlayerGives ? shareKnowledge.getReceiver() : shareKnowledge.getGiver();
            payload.put("cardIndex", currentPlayerGives
                    ? getCardIndex(shareKnowledge.getCard())
                    : target.getHand().indexOf(shareKnowledge.getCard()));
            payload.put("targetPlayer", target.getPlayerName());
            payload.put("direction", currentPlayerGives ? "give" : "take");
        } else if (action instanceof DiscoverCure discoverCure) {
            payload.put("color", discoverCure.getTargetColor().name());
            ArrayNode indices = payload.putArray("cardIndices");
            for (Card card : discoverCure.getCardsToDiscard()) {
                indices.add(getCardIndex(card));
            }
        } else if (action instanceof BuildResearchStation) {
            // No extra payload is needed; the server validates the current city and required card.
        } else if (action instanceof FirewallAction firewallAction) {
            payload.put("cardIndex", getCardIndex(firewallAction.getCardToDiscard()));
        } else if (action instanceof SatelliteAction satelliteAction) {
            payload.put("cardIndex", getCardIndex(satelliteAction.getCardToDiscard()));
            payload.put("targetPlayer", satelliteAction.getTargetPawn());
            payload.put("destination", satelliteAction.getDestination().getName());
        } else if (action instanceof ServerAction serverAction) {
            payload.put("cardIndex", getCardIndex(serverAction.getCardToDiscard()));
            payload.put("destination", serverAction.getDestinationCity().getName());
        } else if (action instanceof SystemControlAction systemControlAction) {
            payload.put("cardIndex", getCardIndex(systemControlAction.getCardToDiscard()));
            payload.put("infectionCardName", systemControlAction.getInfectionCardToRemove().getCardName());
        } else if (action instanceof ThreatAction threatAction) {
            payload.put("cardIndex", getCardIndex(threatAction.getCardToDiscard()));
            ArrayNode indices = payload.putArray("infectionCardIndices");
            for (int i = 0; i < threatAction.getRearrangedCards().size(); i++) {
                indices.add(i);
            }
        }

        System.out.println("[Player] Executing action: " + actionName + ". Sending to server...");

        if (gameClient != null) {
            gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
        }
        else {
            System.err.println("[Player] Cannot send packet without a GameClient.");
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

    public void requestDrawCards() {
        requestEndTurn();
    }

    public void requestInfectionPhase() {
        requestEndTurn();
    }

    public void useCard(Card card) {
        if (gameClient == null) {
            System.err.println("[Player] Cannot send UseCard packet - gameClient is null!");
            return;
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("PlayerID", state.getPlayerName());
        payload.put("GameAction", card.getEventType() == null ? "USE_CARD" : card.getEventType().name());
        payload.put("cardIndex", getCardIndex(card));

        System.out.println("[Player] Playing card: " + card.getCardName() + "...");
        gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
    }

    public PlayerState getState() {
        return state;
    }

    public void updateState(PlayerState newState) {
        this.state = newState;
    }

    public void requestEndTurn() {
        if (gameClient == null) {
            System.err.println("[Player] Cannot send END_TURN packet, gameClient is null!");
            return;
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("PlayerID", state.getPlayerName());
        payload.put("GameAction", "END_TURN");
        gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
    }

    private String toPacketAction(GameAction action) {
        if (action instanceof DriveFerryAction) return "DRIVE_FERRY";
        if (action instanceof DirectFlightAction) return "DIRECT_FLIGHT";
        if (action instanceof CharterFlightAction) return "CHARTER_FLIGHT";
        if (action instanceof ShuttleFlightAction) return "SHUTTLE_FLIGHT";
        if (action instanceof TreatDisease) return "TREAT_DISEASE";
        if (action instanceof ShareKnowledge) return "SHARE_KNOWLEDGE";
        if (action instanceof DiscoverCure) return "DISCOVER_CURE";
        if (action instanceof BuildResearchStation) return "BUILD_RESEARCH_STATION";
        if (action instanceof FirewallAction) return "FIREWALL";
        if (action instanceof SatelliteAction) return "SATELLITE";
        if (action instanceof ServerAction) return "SERVER";
        if (action instanceof SystemControlAction) return "SYSTEM_CONTROL";
        if (action instanceof ThreatAction) return "THREAT";
        return action.getClass().getSimpleName();
    }

    private int getCardIndex(Card card) {
        return card == null ? -1 : state.getHand().indexOf(card);
    }
}
