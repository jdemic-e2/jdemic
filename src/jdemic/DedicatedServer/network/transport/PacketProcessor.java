package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.DiseaseColor;
import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.Player;
import jdemic.GameLogic.Actions.FirewallAction;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.Actions.SatelliteAction;
import jdemic.GameLogic.Actions.ServerAction;
import jdemic.GameLogic.Actions.SystemControlAction;
import jdemic.GameLogic.Actions.ThreatAction;
import jdemic.GameLogic.Actions.Movement.CharterFlightAction;
import jdemic.GameLogic.Actions.Movement.DirectFlightAction;
import jdemic.GameLogic.Actions.Movement.DriveFerryAction;
import jdemic.GameLogic.Actions.Movement.ShuttleFlightAction;
import jdemic.GameLogic.Actions.Other.BuildResearchStation;
import jdemic.GameLogic.Actions.Other.DiscoverCure;
import jdemic.GameLogic.Actions.Other.ShareKnowledge;
import jdemic.GameLogic.Actions.Other.TreatDisease;
import jdemic.GameLogic.ServerRelatedClasses.LobbyChatMessage;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts validated network packets into game mutations.
 */
public class PacketProcessor {

    private static final int GAME_START_DELAY_SECONDS = 10;
    private static final int MIN_PLAYERS_TO_START = 2;
    private static final int MAX_PLAYERS = 4;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = Logger.getLogger(PacketProcessor.class.getName());

    private final GameManager gameManager;
    private final ClientHandler clientHandler;
    private final ScheduledExecutorService gameStartScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "jdemic-lobby-countdown");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> gameStartTask;

    public PacketProcessor() {
        this(new GameManager(new ArrayList<>()), null);
    }

    public PacketProcessor(GameManager gameManager, ClientHandler clientHandler) {
        this.gameManager = gameManager;
        this.clientHandler = clientHandler;
    }

    public void process(Packet packet) {
        if (packet == null) {
            System.err.println("[PacketProcessor] Cannot process a null packet.");
            return;
        }
        if (packet.getType() == null) {
            System.err.println("[PacketProcessor] Unsupported packet type: null");
            return;
        }

        System.out.println("[PacketProcessor] Routing packet: " + packet);

        switch (packet.getType()) {
            case PING: handlePing(packet); break;
            case PONG: handlePong(packet); break;
            case GAME_DATA: handleGameData(packet); break;
            case CONNECT: handleConnect(packet); break;
            case LOBBY_CHAT: handleLobbyChat(packet); break;
            case LOBBY_READY: handleLobbyReady(packet); break;
            case DISCONNECT: handleDisconnect(packet); break;
            case ERROR: handleError(packet); break;
            default: System.err.println("[PacketProcessor] Unsupported packet type: " + packet.getType());
        }
    }

    private void handlePing(Packet packet) {
        System.out.println("[PacketProcessor] Received PING packet.");
    }

    private void handlePong(Packet packet) {
        System.out.println("[PacketProcessor] Received PONG packet.");
    }

    private void handleGameData(Packet packet) {
        System.out.println("[PacketProcessor] Received GAME_DATA packet.");
        JsonNode payload = packet.getPayload();
        String gameAction = payload.has("GameAction") ? payload.get("GameAction").asText() : "";

        if ("TEST_ACTION".equals(gameAction)) {
            String playerId = payload.has("PlayerID") ? payload.get("PlayerID").asText() : "UNKNOWN";
            String message = payload.has("message") ? payload.get("message").asText() : "";
            System.out.println("[PacketProcessor] TEST_ACTION from " + playerId + ": " + message);
            broadcastGameState();
            return;
        }

        boolean shouldBroadcast = false;
        synchronized (gameManager.getStateLock()) {
            String playerId = resolvePlayerId(payload);
            if (playerId == null) {
                System.err.println("[PacketProcessor] Missing or spoofed PlayerID in GAME_DATA packet.");
                return;
            }

            PlayerState playerState = findPlayerState(playerId);
            if (playerState == null) {
                System.err.println("[PacketProcessor] Player not found: " + playerId);
                return;
            }

            if (!gameManager.getState().isGameStarted()) {
                System.err.println("[PacketProcessor] GAME_DATA ignored: game has not started.");
                return;
            }

            if (!isCurrentPlayer(playerState)) {
                System.err.println("[PacketProcessor] Out-of-turn action ignored from: " + playerId);
                return;
            }

            if (gameManager.isGameOver()) {
                System.err.println("[PacketProcessor] Game is over; action ignored.");
                return;
            }

            if ("END_TURN".equals(gameAction)) {
                handleEndTurn(payload);
                shouldBroadcast = true;
            } else if ("DISCARD_CARD".equals(gameAction)) {
                handleDiscardCard(payload, playerState);
                shouldBroadcast = true;
            } else if ("THREAT_PREVIEW".equals(gameAction)) {
                sendThreatPreview(playerState, payload);
            } else {
                Player player = new Player(playerState, null);
                GameAction action = buildAction(gameAction, payload, playerState);
                if (action == null) {
                    System.err.println("[PacketProcessor] Action ignored because it could not be built: " + gameAction);
                    return;
                }

                gameManager.performAction(player, action);
                System.out.println("[PacketProcessor] Action performed: " + gameAction + " by " + playerId);
                shouldBroadcast = true;
            }
        }

        if (shouldBroadcast) {
            broadcastGameState();
        }
    }

    private String resolvePlayerId(JsonNode payload) {
        if (clientHandler != null && clientHandler.getConnectedPlayerName() != null) {
            String connectedName = clientHandler.getConnectedPlayerName();
            if (payload.has("PlayerID")) {
                String claimedName = payload.get("PlayerID").asText();
                if (claimedName.isBlank() || !claimedName.equalsIgnoreCase(connectedName)) {
                    System.err.println("[PacketProcessor] PlayerID spoof rejected. Connected="
                            + connectedName + ", claimed=" + claimedName);
                    return null;
                }
            }
            return connectedName;
        }
        if (payload.has("PlayerID") && !payload.get("PlayerID").asText().isBlank()) {
            return payload.get("PlayerID").asText();
        }
        return null;
    }

    private boolean isCurrentPlayer(PlayerState playerState) {
        PlayerState current = gameManager.getCurrentPlayer();
        return current != null && current.getPlayerName().equals(playerState.getPlayerName());
    }

    private GameAction buildAction(String gameAction, JsonNode payload, PlayerState playerState) {
        switch (gameAction) {
            case "DRIVE_FERRY": {
                CityNode dest = resolveDestination(payload, "destination");
                return dest == null ? null : new DriveFerryAction(dest);
            }
            case "DIRECT_FLIGHT": {
                CityNode dest = resolveDestination(payload, "destination");
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                return dest == null || card == null ? null : new DirectFlightAction(dest, card);
            }
            case "CHARTER_FLIGHT": {
                CityNode dest = resolveDestination(payload, "destination");
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                return dest == null || card == null ? null : new CharterFlightAction(dest, card);
            }
            case "SHUTTLE_FLIGHT": {
                CityNode dest = resolveDestination(payload, "destination");
                return dest == null ? null : new ShuttleFlightAction(dest);
            }
            case "BUILD_RESEARCH_STATION":
                return new BuildResearchStation();
            case "TREAT_DISEASE": {
                DiseaseColor color = resolveColor(payload, "color");
                return color == null ? null : new TreatDisease(color);
            }
            case "SHARE_KNOWLEDGE":
                return buildShareKnowledgeAction(payload, playerState);
            case "DISCOVER_CURE":
                return buildDiscoverCureAction(payload, playerState);
            case "FIREWALL": {
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                return card == null ? null : new FirewallAction(card);
            }
            case "SATELLITE": {
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                CityNode dest = resolveDestination(payload, "destination");
                String targetPlayer = payload.has("targetPlayer") ? payload.get("targetPlayer").asText() : null;
                return card == null || dest == null || targetPlayer == null || targetPlayer.isBlank()
                        ? null
                        : new SatelliteAction(targetPlayer, dest, card);
            }
            case "SERVER": {
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                CityNode dest = resolveDestination(payload, "destination");
                return card == null || dest == null ? null : new ServerAction(dest, card);
            }
            case "SYSTEM_CONTROL":
            case "CONTROL": {
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                Card infectionCard = resolveInfectionDiscardByIndex(payload, "infectionDiscardIndex");
                return card == null || infectionCard == null ? null : new SystemControlAction(card, infectionCard);
            }
            case "THREAT": {
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                List<Card> reorderedCards = resolveTopInfectionCards(payload, "infectionCardIndices");
                return card == null || reorderedCards == null ? null : new ThreatAction(card, reorderedCards);
            }
            default:
                System.err.println("[PacketProcessor] Unknown GameAction: " + gameAction);
                return null;
        }
    }

    private GameAction buildShareKnowledgeAction(JsonNode payload, PlayerState playerState) {
        String targetName = payload.has("targetPlayer") ? payload.get("targetPlayer").asText() : null;
        if (targetName == null || targetName.isBlank()) {
            System.err.println("[PacketProcessor] SHARE_KNOWLEDGE missing targetPlayer.");
            return null;
        }

        PlayerState targetState = findPlayerState(targetName);
        if (targetState == null) {
            System.err.println("[PacketProcessor] SHARE_KNOWLEDGE target not found: " + targetName);
            return null;
        }

        String direction = payload.has("direction") ? payload.get("direction").asText() : "give";
        PlayerState giver = "give".equalsIgnoreCase(direction) ? playerState : targetState;
        PlayerState receiver = "give".equalsIgnoreCase(direction) ? targetState : playerState;
        Card giverCard = resolveCardByIndex(payload, "cardIndex", giver);
        return giverCard == null ? null : new ShareKnowledge(giverCard, giver, receiver);
    }

    private GameAction buildDiscoverCureAction(JsonNode payload, PlayerState playerState) {
        DiseaseColor color = resolveColor(payload, "color");
        if (color == null) {
            return null;
        }
        if (!payload.has("cardIndices") || !payload.get("cardIndices").isArray()) {
            System.err.println("[PacketProcessor] DISCOVER_CURE missing cardIndices array.");
            return null;
        }

        List<Card> hand = playerState.getHand();
        List<Card> chosenCards = new ArrayList<>();
        for (JsonNode indexNode : payload.get("cardIndices")) {
            int idx = indexNode.asInt(-1);
            if (idx < 0 || idx >= hand.size()) {
                System.err.println("[PacketProcessor] DISCOVER_CURE invalid card index: " + idx);
                return null;
            }
            Card chosen = hand.get(idx);
            if (chosenCards.contains(chosen)) {
                System.err.println("[PacketProcessor] DISCOVER_CURE duplicate card index: " + idx);
                return null;
            }
            chosenCards.add(chosen);
        }

        return new DiscoverCure(color, chosenCards);
    }

    private void handleConnect(Packet packet) {
        System.out.println("[PacketProcessor] Received CONNECT packet.");

        try {
            JsonNode payload = packet.getPayload();
            String playerName = payload.has("playerName")
                    ? payload.get("playerName").asText().trim()
                    : "PLAYER";

            if (playerName.isBlank()) {
                System.err.println("[PacketProcessor] CONNECT rejected: empty player name.");
                return;
            }

            synchronized (gameManager.getStateLock()) {
                if (findPlayerState(playerName) != null) {
                    System.err.println("[PacketProcessor] CONNECT rejected: name already taken: " + playerName);
                    return;
                }
                if (gameManager.getState().getPlayers().size() >= MAX_PLAYERS) {
                    System.err.println("[PacketProcessor] CONNECT rejected: lobby is full (" + MAX_PLAYERS + " players max).");
                    return;
                }
                if (gameManager.getState().isGameStarted()) {
                    System.err.println("[PacketProcessor] CONNECT rejected: game already in progress.");
                    return;
                }

                PlayerState newPlayer = new PlayerState(playerName);
                newPlayer.setReady(false);
                gameManager.getState().addPlayer(newPlayer);
                updateLobbyCountdown();

                if (clientHandler != null) {
                    clientHandler.setConnectedPlayerName(playerName);
                }
            }

            System.out.println("[PacketProcessor] Player registered: " + playerName
                    + " | Total: " + gameManager.getState().getPlayers().size());
            broadcastGameState();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error handling CONNECT packet.", e);
        }
    }

    private void handleLobbyChat(Packet packet) {
        System.out.println("[PacketProcessor] Received LOBBY_CHAT packet.");

        try {
            JsonNode payload = packet.getPayload();
            String message = payload.has("message") ? payload.get("message").asText().trim() : "";
            if (message.isEmpty()) {
                return;
            }
            if (message.length() > 300) {
                message = message.substring(0, 300);
            }

            String playerName = clientHandler != null ? clientHandler.getConnectedPlayerName() : null;
            if (playerName == null || playerName.isBlank()) {
                System.err.println("[PacketProcessor] Lobby chat rejected because client is not registered.");
                return;
            }

            synchronized (gameManager.getStateLock()) {
                gameManager.getState().addLobbyChatMessage(
                        new LobbyChatMessage(playerName, message, System.currentTimeMillis())
                );
            }

            broadcastGameState();
        } catch (Exception e) {
            System.err.println("[PacketProcessor] Error handling LOBBY_CHAT: " + e.getMessage());
        }
    }

    private void handleLobbyReady(Packet packet) {
        System.out.println("[PacketProcessor] Received LOBBY_READY packet.");

        try {
            JsonNode payload = packet.getPayload();
            boolean ready = payload.has("ready") && payload.get("ready").asBoolean();

            String playerName = clientHandler != null ? clientHandler.getConnectedPlayerName() : null;
            if (playerName == null || playerName.isBlank()) {
                System.err.println("[PacketProcessor] LOBBY_READY ignored: client not registered.");
                return;
            }

            synchronized (gameManager.getStateLock()) {
                if (gameManager.getState().isGameStarted()) {
                    System.err.println("[PacketProcessor] LOBBY_READY ignored: game already started.");
                    return;
                }

                PlayerState playerState = findPlayerState(playerName);
                if (playerState == null) {
                    System.err.println("[PacketProcessor] LOBBY_READY ignored: player not found: " + playerName);
                    return;
                }

                playerState.setReady(ready);
                updateLobbyCountdown();
            }

            broadcastGameState();
        } catch (Exception e) {
            System.err.println("[PacketProcessor] Error handling LOBBY_READY: " + e.getMessage());
        }
    }

    private void handleDisconnect(Packet packet) {
        System.out.println("[PacketProcessor] Received DISCONNECT packet.");
        disconnectCurrentPlayer();
        if (clientHandler != null) {
            clientHandler.closeConnection();
        }
    }

    private void handleError(Packet packet) {
        System.err.println("[PacketProcessor] Received ERROR packet: " + packet);
    }

    private void handleDiscardCard(JsonNode payload, PlayerState playerState) {
        if (!payload.has("cardIndex") || !payload.get("cardIndex").isInt()) {
            System.err.println("[PacketProcessor] DISCARD_CARD missing cardIndex.");
            return;
        }
        gameManager.discardCurrentPlayerCard(playerState, payload.get("cardIndex").asInt());
    }

    private void handleEndTurn(JsonNode payload) {
        System.out.println("[PacketProcessor] Received END_TURN packet.");

        String playerName = resolvePlayerId(payload);
        if (playerName == null || playerName.isBlank()) {
            System.err.println("[PacketProcessor] END_TURN ignored: sender is not registered.");
            return;
        }

        PlayerState playerState = findPlayerState(playerName);
        if (playerState == null || !isCurrentPlayer(playerState)) {
            System.err.println("[PacketProcessor] END_TURN ignored: not this player's turn (" + playerName + ").");
            return;
        }

        System.out.println("[PacketProcessor] Player " + playerName + " ended their turn early ("
                + gameManager.getState().getActionsRemaining() + " action(s) remaining).");

        gameManager.getState().setActionsRemaining(0);
        gameManager.nextTurn();
    }

    private CityNode resolveDestination(JsonNode payload, String field) {
        if (!payload.has(field) || !payload.get(field).isTextual()) {
            System.err.println("[PacketProcessor] Missing or non-text field: " + field);
            return null;
        }
        String name = payload.get(field).asText();
        CityNode city = gameManager.getState().getMap().getCity(name);
        if (city == null) {
            System.err.println("[PacketProcessor] Unknown city: " + name);
        }
        return city;
    }

    private Card resolveCardByIndex(JsonNode payload, String field, PlayerState playerState) {
        if (!payload.has(field) || !payload.get(field).isInt()) {
            System.err.println("[PacketProcessor] Missing or non-integer field: " + field);
            return null;
        }
        int idx = payload.get(field).asInt();
        List<Card> hand = playerState.getHand();
        if (idx < 0 || idx >= hand.size()) {
            System.err.println("[PacketProcessor] Card index out of bounds: " + idx
                    + " (hand size " + hand.size() + ")");
            return null;
        }
        return hand.get(idx);
    }

    private DiseaseColor resolveColor(JsonNode payload, String field) {
        if (!payload.has(field) || !payload.get(field).isTextual()) {
            System.err.println("[PacketProcessor] Missing or non-text color field: " + field);
            return null;
        }
        try {
            return DiseaseColor.valueOf(payload.get(field).asText().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[PacketProcessor] Unknown disease color: " + payload.get(field).asText());
            return null;
        }
    }

    private Card resolveInfectionDiscardByIndex(JsonNode payload, String field) {
        List<Card> discardPile = gameManager.getState().getCardDeck().getInfectionDiscardPile();

        if (payload.has(field) && payload.get(field).isInt()) {
            int idx = payload.get(field).asInt();
            if (idx < 0 || idx >= discardPile.size()) {
                System.err.println("[PacketProcessor] Infection discard index out of bounds: " + idx);
                return null;
            }
            return discardPile.get(idx);
        }

        if (payload.has("infectionCardName") && payload.get("infectionCardName").isTextual()) {
            String cardName = payload.get("infectionCardName").asText();
            for (Card card : discardPile) {
                if (card.getCardName().equals(cardName)) {
                    return card;
                }
            }
            System.err.println("[PacketProcessor] Infection discard card not found: " + cardName);
            return null;
        }

        System.err.println("[PacketProcessor] Missing infection discard selector: " + field + " or infectionCardName.");
        return null;
    }

    private List<Card> resolveTopInfectionCards(JsonNode payload, String field) {
        if (!payload.has(field) || !payload.get(field).isArray()) {
            System.err.println("[PacketProcessor] THREAT missing infectionCardIndices array.");
            return null;
        }

        List<Card> topCards = gameManager.getState().getCardDeck().getTopInfectionCards(6);
        List<Card> reordered = new ArrayList<>();
        for (JsonNode indexNode : payload.get(field)) {
            int idx = indexNode.asInt(-1);
            if (idx < 0 || idx >= topCards.size()) {
                System.err.println("[PacketProcessor] THREAT invalid top infection card index: " + idx);
                return null;
            }
            Card selected = topCards.get(idx);
            if (reordered.contains(selected)) {
                System.err.println("[PacketProcessor] THREAT duplicate infection card index: " + idx);
                return null;
            }
            reordered.add(selected);
        }
        return reordered;
    }

    private void sendThreatPreview(PlayerState playerState, JsonNode payload) {
        if (clientHandler == null) {
            return;
        }
        if (!playerHasThreatCard(playerState, payload)) {
            System.err.println("[PacketProcessor] THREAT_PREVIEW rejected: player has no selected Threat Scan card.");
            return;
        }

        ObjectNode response = OBJECT_MAPPER.createObjectNode();
        response.put("GameAction", "THREAT_PREVIEW");
        ArrayNode cards = response.putArray("topInfectionCards");
        List<Card> topCards = gameManager.getState().getCardDeck().getTopInfectionCards(6);

        for (int i = 0; i < topCards.size(); i++) {
            Card card = topCards.get(i);
            ObjectNode cardNode = cards.addObject();
            cardNode.put("index", i);
            cardNode.put("cardName", card.getCardName());
            if (card.getTargetCity() != null) {
                cardNode.put("cityName", card.getTargetCity().getName());
                cardNode.put("color", card.getTargetCity().getNativeColor().name());
            }
        }

        clientHandler.sendPacketToClient(new Packet(PacketType.GAME_DATA, response));
    }

    private boolean playerHasThreatCard(PlayerState playerState, JsonNode payload) {
        if (payload.has("cardIndex") && payload.get("cardIndex").isInt()) {
            return isThreatCard(resolveCardByIndex(payload, "cardIndex", playerState));
        }

        for (Card card : playerState.getHand()) {
            if (isThreatCard(card)) {
                return true;
            }
        }
        return false;
    }

    private boolean isThreatCard(Card card) {
        return card != null
                && card.getType() == CardType.EVENT
                && card.getEventType() == Card.EventType.THREAT;
    }

    private PlayerState findPlayerState(String playerName) {
        for (PlayerState ps : gameManager.getState().getPlayers()) {
            if (ps.getPlayerName().equalsIgnoreCase(playerName)) {
                return ps;
            }
        }
        return null;
    }

    private void broadcastGameState() {
        if (clientHandler != null) {
            clientHandler.broadcastGameStateToAll();
        }
    }

    public void disconnectCurrentPlayer() {
        if (gameManager == null || clientHandler == null) {
            return;
        }

        String playerName = clientHandler.getConnectedPlayerName();
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        synchronized (gameManager.getStateLock()) {
            PlayerState playerToRemove = findPlayerState(playerName);
            if (playerToRemove == null) {
                return;
            }

            gameManager.removePlayer(playerToRemove);
            System.out.println("[PacketProcessor] Player disconnected: " + playerName);
            updateLobbyCountdown();
            clientHandler.clearConnectedPlayerName();
        }

        broadcastGameState();
    }

    private void updateLobbyCountdown() {
        boolean enoughPlayers = gameManager.getState().getPlayers().size() >= MIN_PLAYERS_TO_START;
        boolean allReady = enoughPlayers;
        for (PlayerState player : gameManager.getState().getPlayers()) {
            if (!player.isReady()) {
                allReady = false;
                break;
            }
        }

        if (allReady && gameManager.getState().getLobbyCountdownStartedAt() == 0) {
            gameManager.getState().setLobbyCountdownStartedAt(System.currentTimeMillis());
            scheduleGameStart();
        } else if (!allReady) {
            gameManager.getState().setLobbyCountdownStartedAt(0);
            cancelGameStart();
        }
    }

    private void scheduleGameStart() {
        synchronized (this) {
            cancelGameStart();
            gameStartTask = gameStartScheduler.schedule(() -> {
                synchronized (gameManager.getStateLock()) {
                    if (!areAllPlayersReady() || gameManager.getState().isGameStarted()) {
                        gameManager.getState().setLobbyCountdownStartedAt(0);
                    } else {
                        gameManager.startGame();
                        System.out.println("[PacketProcessor] Lobby countdown finished. Game started.");
                    }
                }
                broadcastGameState();
            }, GAME_START_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void cancelGameStart() {
        synchronized (this) {
            if (gameStartTask != null && !gameStartTask.isDone()) {
                gameStartTask.cancel(false);
            }
            gameStartTask = null;
        }
    }

    private boolean areAllPlayersReady() {
        if (gameManager.getState().getPlayers().size() < MIN_PLAYERS_TO_START) {
            return false;
        }
        for (PlayerState player : gameManager.getState().getPlayers()) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }

    public void shutdown() {
        cancelGameStart();
        gameStartScheduler.shutdownNow();
    }
}
