package jdemic.DedicatedServer.network.transport;

import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Player;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.Actions.Movement.CharterFlightAction;
import jdemic.GameLogic.Actions.Movement.DirectFlightAction;
import jdemic.GameLogic.Actions.Movement.DriveFerryAction;
import jdemic.GameLogic.Actions.Movement.ShuttleFlightAction;
import jdemic.GameLogic.Actions.Other.BuildResearchStation;
import jdemic.GameLogic.Actions.Other.DiscoverCure;
import jdemic.GameLogic.Actions.Other.ShareKnowledge;
import jdemic.GameLogic.Actions.Other.TreatDisease;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.DiseaseColor;
import jdemic.GameLogic.ServerRelatedClasses.LobbyChatMessage;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * PacketProcessor handles packets that have already passed validation and
 * converts them into the appropriate server-side actions. It checks the
 * packet type and routes each packet to the matching handler method, keeping
 * protocol logic separate from raw socket communication.
 */
public class PacketProcessor {

    private GameManager gameManager;
    private ClientHandler clientHandler;
    private static final int GAME_START_DELAY_SECONDS = 10;
    private static final int MAX_PLAYERS = 4;
    private static final ScheduledExecutorService gameStartScheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> gameStartTask;

    public PacketProcessor() {
        this(new GameManager(new ArrayList<>()), null);
    }

    public PacketProcessor(GameManager gameManager, ClientHandler clientHandler) {
        this.gameManager = gameManager;
        this.clientHandler = clientHandler;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main dispatcher
    // ─────────────────────────────────────────────────────────────────────────

    public void process(Packet packet) {
        if (packet == null) {
            System.err.println("[PacketProcessor] Cannot process a null packet.");
            return;
        }

        switch (packet.getType()) {
            case PING:          handlePing(packet);         break;
            case PONG:          handlePong(packet);         break;
            case GAME_DATA:     handleGameData(packet);     break;
            case CONNECT:       handleConnect(packet);      break;
            case LOBBY_CHAT:    handleLobbyChat(packet);    break;
            case LOBBY_READY:   handleLobbyReady(packet);   break;
            case DISCONNECT:    handleDisconnect(packet);   break;
            case ERROR:         handleError(packet);        break;
            case END_TURN:      handleEndTurn(packet);      break;
            default:
                System.err.println("[PacketProcessor] Unsupported packet type: " + packet.getType());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heartbeat
    // ─────────────────────────────────────────────────────────────────────────

    private void handlePing(Packet packet) {
        System.out.println("[PacketProcessor] Received PING.");
        // Pong is sent back via the client handler if needed — nothing to do here
    }

    private void handlePong(Packet packet) {
        System.out.println("[PacketProcessor] Received PONG.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Game data — action routing
    // ─────────────────────────────────────────────────────────────────────────

    private void handleGameData(Packet packet) {
        System.out.println("[PacketProcessor] Received GAME_DATA packet.");
        JsonNode payload = packet.getPayload();

        // ── TEST_ACTION (dev/debug only) ──────────────────────────────────────
        String gameAction = payload.has("GameAction") ? payload.get("GameAction").asText() : "";
        if ("TEST_ACTION".equals(gameAction)) {
            String playerId = payload.has("PlayerID") ? payload.get("PlayerID").asText() : "UNKNOWN";
            String message  = payload.has("message")  ? payload.get("message").asText()  : "";
            System.out.println("[PacketProcessor] TEST_ACTION from " + playerId + ": " + message);
            broadcastGameState();
            return;
        }

        // ── Resolve player ────────────────────────────────────────────────────
        String playerId = resolvePlayerId(payload);
        if (playerId == null) {
            System.err.println("[PacketProcessor] Missing PlayerID in GAME_DATA packet.");
            return;
        }

        PlayerState playerState = findPlayerState(playerId);
        if (playerState == null) {
            System.err.println("[PacketProcessor] Player not found: " + playerId);
            return;
        }

        // ── Turn enforcement ──────────────────────────────────────────────────
        // Only the current player may perform game actions.
        if (!isCurrentPlayer(playerState)) {
            System.err.println("[PacketProcessor] Out-of-turn action ignored from: " + playerId);
            return;
        }

        // ── Game-over guard ───────────────────────────────────────────────────
        if (gameManager.isGameOver()) {
            System.err.println("[PacketProcessor] Game is over; action ignored.");
            return;
        }

        // ── Route to action builder ───────────────────────────────────────────
        Player player = new Player(playerState, null);
        GameAction action = buildAction(gameAction, payload, playerState);

        if (action == null) {
            // buildAction already logged the reason
            return;
        }

        gameManager.performAction(player, action);
        System.out.println("[PacketProcessor] Action performed: " + gameAction + " by " + playerId);

        // ── Auto end-of-turn ──────────────────────────────────────────────────
        // When the last action is spent, run the full end-of-turn sequence:
        // draw player cards (epidemic fires inside drawHand if triggered),
        // then infect cities, then advance to the next player.
        if (gameManager.getState().getActionsRemaining() == 0) {
            System.out.println("[PacketProcessor] All actions spent — ending turn for " + playerId);
            gameManager.nextTurn();
        }

        broadcastGameState();
    }

    /**
     * Resolves the PlayerID from the payload, falling back to the name stored
     * in the ClientHandler for this connection.
     */
    private String resolvePlayerId(JsonNode payload) {
        if (payload.has("PlayerID") && !payload.get("PlayerID").asText().isBlank()) {
            return payload.get("PlayerID").asText();
        }
        if (clientHandler != null && clientHandler.getConnectedPlayerName() != null) {
            return clientHandler.getConnectedPlayerName();
        }
        return null;
    }

    /**
     * Returns true only if the given player state belongs to whoever's turn it
     * currently is.
     */
    private boolean isCurrentPlayer(PlayerState playerState) {
        PlayerState current = gameManager.getCurrentPlayer();
        return current != null && current.getPlayerName().equals(playerState.getPlayerName());
    }

    /**
     * Builds the correct GameAction from the action name and payload.
     * Returns null (and logs the reason) if construction fails.
     */
    private GameAction buildAction(String gameAction, JsonNode payload, PlayerState playerState) {
        switch (gameAction) {

            // ── Movement ──────────────────────────────────────────────────────

            case "DRIVE_FERRY": {
                CityNode dest = resolveDestination(payload, "destination");
                if (dest == null) return null;
                return new DriveFerryAction(dest);
            }

            case "DIRECT_FLIGHT": {
                CityNode dest = resolveDestination(payload, "destination");
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                if (dest == null || card == null) return null;
                return new DirectFlightAction(dest, card);
            }

            case "CHARTER_FLIGHT": {
                CityNode dest = resolveDestination(payload, "destination");
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                if (dest == null || card == null) return null;
                return new CharterFlightAction(dest, card);
            }

            case "SHUTTLE_FLIGHT": {
                CityNode dest = resolveDestination(payload, "destination");
                if (dest == null) return null;
                return new ShuttleFlightAction(dest);
            }

            // ── Other actions ─────────────────────────────────────────────────

            case "BUILD_RESEARCH_STATION": {
                return new BuildResearchStation();
            }

            case "TREAT_DISEASE": {
                DiseaseColor color = resolveColor(payload, "color");
                if (color == null) return null;
                return new TreatDisease(color);
            }

            case "SHARE_KNOWLEDGE": {
                // Payload must include: "cardIndex" (int), "targetPlayer" (string), "direction" ("give"|"take")
                Card card = resolveCardByIndex(payload, "cardIndex", playerState);
                if (card == null) return null;

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
                // "give": current player hands card to target; "take": current player takes from target
                PlayerState giver    = "give".equalsIgnoreCase(direction) ? playerState : targetState;
                PlayerState receiver = "give".equalsIgnoreCase(direction) ? targetState : playerState;

                // Re-resolve card from the giver's actual hand when direction is "take"
                Card giverCard = card;
                if ("take".equalsIgnoreCase(direction)) {
                    giverCard = resolveCardByIndex(payload, "cardIndex", targetState);
                    if (giverCard == null) return null;
                }

                return new ShareKnowledge(giverCard, giver, receiver);
            }

            case "DISCOVER_CURE": {
                DiseaseColor color = resolveColor(payload, "color");
                if (color == null) return null;

                // Payload must include "cardIndices": array of int indices into playerState.getHand()
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

            default:
                System.err.println("[PacketProcessor] Unknown GameAction: " + gameAction);
                return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lobby handlers
    // ─────────────────────────────────────────────────────────────────────────

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

            // FIX: reject duplicate names
            if (findPlayerState(playerName) != null) {
                System.err.println("[PacketProcessor] CONNECT rejected: name already taken: " + playerName);
                return;
            }

            // FIX: enforce Pandemic's 4-player maximum
            if (gameManager.getState().getPlayers().size() >= MAX_PLAYERS) {
                System.err.println("[PacketProcessor] CONNECT rejected: lobby is full (" + MAX_PLAYERS + " players max).");
                return;
            }

            // FIX: reject joins once the game has already started
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

            System.out.println("[PacketProcessor] Player registered: " + playerName
                    + " | Total: " + gameManager.getState().getPlayers().size());

            broadcastGameState();
        } catch (Exception e) {
            System.err.println("[PacketProcessor] Error handling CONNECT: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void handleLobbyChat(Packet packet) {
        System.out.println("[PacketProcessor] Received LOBBY_CHAT packet.");

        try {
            JsonNode payload = packet.getPayload();
            String message = payload.has("message") ? payload.get("message").asText().trim() : "";
            if (message.isEmpty()) return;
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
            // FIX: ignore ready toggles once the game has started
            if (gameManager.getState().isGameStarted()) {
                System.err.println("[PacketProcessor] LOBBY_READY ignored: game already started.");
                return;
            }

            JsonNode payload = packet.getPayload();
            boolean ready = payload.has("ready") && payload.get("ready").asBoolean();

            String playerName = clientHandler != null ? clientHandler.getConnectedPlayerName() : null;
            if (playerName == null || playerName.isBlank()) {
                System.err.println("[PacketProcessor] LOBBY_READY ignored: client not registered.");
                return;
            }

            PlayerState playerState = findPlayerState(playerName);
            if (playerState == null) {
                System.err.println("[PacketProcessor] LOBBY_READY ignored: player not found: " + playerName);
                return;
            }

            playerState.setReady(ready);
            updateLobbyCountdown();
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

    /**
     * Handles an explicit END_TURN packet from the client.
     * The player may still have actions remaining but chooses to skip them.
     * Runs the full end-of-turn sequence: draw cards → infect cities → next player.
     */
    private void handleEndTurn(Packet packet) {
        System.out.println("[PacketProcessor] Received END_TURN packet.");

        if (gameManager.isGameOver()) {
            System.err.println("[PacketProcessor] END_TURN ignored: game is already over.");
            return;
        }

        if (!gameManager.getState().isGameStarted()) {
            System.err.println("[PacketProcessor] END_TURN ignored: game has not started.");
            return;
        }

        String playerName = clientHandler != null ? clientHandler.getConnectedPlayerName() : null;
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

        // Force actions to 0 so nextTurn() doesn't guard against them
        gameManager.getState().setActionsRemaining(0);
        gameManager.nextTurn();
        broadcastGameState();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers — resolution utilities
    // ─────────────────────────────────────────────────────────────────────────

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

    //CASE SENSITIVE !!!!!!
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

    // ─────────────────────────────────────────────────────────────────────────
    // Disconnect / broadcast / lobby countdown
    // ─────────────────────────────────────────────────────────────────────────

    public void disconnectCurrentPlayer() {
        if (gameManager == null || clientHandler == null) return;

        String playerName = clientHandler.getConnectedPlayerName();
        if (playerName == null || playerName.isBlank()) return;
        else {
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
        boolean hasPlayers = !gameManager.getState().getPlayers().isEmpty();
        boolean allReady = hasPlayers;
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
        synchronized (PacketProcessor.class) {
            cancelGameStart();
            gameStartTask = gameStartScheduler.schedule(() -> {
                synchronized (PacketProcessor.class) {
                    synchronized (gameManager.getStateLock()) {
                        if (!areAllPlayersReady() || gameManager.getState().isGameStarted()) {
                            gameManager.getState().setLobbyCountdownStartedAt(0);
                        } else {
                            gameManager.startGame();
                            System.out.println("[PacketProcessor] Lobby countdown finished. Game started.");
                        }
                    }
                    gameManager.startGame();
                    System.out.println("[PacketProcessor] Lobby countdown finished. Game started.");
                    broadcastGameState();
                }
            }, GAME_START_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void cancelGameStart() {
        synchronized (PacketProcessor.class) {
            if (gameStartTask != null && !gameStartTask.isDone()) {
                gameStartTask.cancel(false);
            }
            gameStartTask = null;
        }
    }

    private boolean areAllPlayersReady() {
        if (gameManager.getState().getPlayers().isEmpty()) return false;
        for (PlayerState player : gameManager.getState().getPlayers()) {
            if (!player.isReady()) return false;
        }
        return true;
    }
}