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
import jdemic.GameLogic.ServerRelatedClasses.LobbyChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private ClientHandler clientHandler;
    private static final int GAME_START_DELAY_SECONDS = 10;
    private static final ScheduledExecutorService gameStartScheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> gameStartTask;

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

        if (packet.getType() == PacketType.PING) {
            handlePing(packet);
        } else if (packet.getType() == PacketType.PONG) {
            handlePong(packet);
        } else if (packet.getType() == PacketType.GAME_DATA) {
            handleGameData(packet);
        } else if (packet.getType() == PacketType.CONNECT) {
            handleConnect(packet);
        } else if (packet.getType() == PacketType.LOBBY_CHAT) {
            handleLobbyChat(packet);
        } else if (packet.getType() == PacketType.LOBBY_READY) {
            handleLobbyReady(packet);
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
        String gameAction = payload.has("GameAction") ? payload.get("GameAction").asText() : "";
        if ("TEST_ACTION".equals(gameAction)) {
            String playerId = payload.has("PlayerID") ? payload.get("PlayerID").asText() : "UNKNOWN";
            String message = payload.has("message") ? payload.get("message").asText() : "";
            System.out.println("[PacketProcessor] TEST_ACTION received from " + playerId + ": " + message);
            broadcastGameState();
            return;
        }

        String playerId = payload.has("PlayerID") ? payload.get("PlayerID").asText()
                : clientHandler != null ? clientHandler.getConnectedPlayerName() : "UNKNOWN";
        if (playerId == null || playerId.isBlank()) {
            System.err.println("[PacketProcessor] Missing PlayerID for GAME_DATA packet.");
            return;
        }

        // Find PlayerState by playerId
        PlayerState playerState = null;
        for (PlayerState ps : gameManager.getState().getPlayers()) {
            if (ps.getPlayerName().equalsIgnoreCase(playerId)) {
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
            case "MOVE":
            case "BUILD":
            case "SHARE":
            case "FLY":
                consumeGenericGameplayAction(playerState, gameAction);
                broadcastGameState();
                return;
            default:
                System.err.println("[PacketProcessor] Unknown action: " + gameAction);
                return;
        }

        // Perform action
        gameManager.performAction(player, action);

        System.out.println("[PacketProcessor] Action performed: " + gameAction + " for player " + playerId);
        broadcastGameState();
    }

    private void consumeGenericGameplayAction(PlayerState playerState, String gameAction) {
        int actionsRemaining = gameManager.getState().getActionsRemaining();
        if (actionsRemaining <= 0) {
            System.out.println("[PacketProcessor] Ignored " + gameAction + " because no actions remain.");
            return;
        }

        gameManager.getState().setActionsRemaining(actionsRemaining - 1);
        System.out.println("[PacketProcessor] Generic gameplay action consumed: " + gameAction
                + " for player " + playerState.getPlayerName());
    }

    private void handleConnect(Packet packet) {
        System.out.println("[PacketProcessor] Received CONNECT packet: " + packet);
        
        try {
            JsonNode payload = packet.getPayload();
            String playerName = payload.has("playerName") ? payload.get("playerName").asText() : "PLAYER";
            
            // Register this player with the game manager
            PlayerState newPlayer = new PlayerState(playerName);
            newPlayer.setReady(false);
            gameManager.getState().addPlayer(newPlayer);
            updateLobbyCountdown();
            
            // Store player name in client handler so we know who this is
            if (clientHandler != null) {
                clientHandler.setConnectedPlayerName(playerName);
            }
            
            System.out.println("[PacketProcessor] Player registered: " + playerName);
            System.out.println("[PacketProcessor] Total players: " + gameManager.getState().getPlayers().size());
            
            // Broadcast updated game state to ALL connected clients
            broadcastGameState();
        } catch (Exception e) {
            System.err.println("[PacketProcessor] Error handling CONNECT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLobbyChat(Packet packet) {
        System.out.println("[PacketProcessor] Received LOBBY_CHAT packet: " + packet);

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
                playerName = payload.has("playerName") ? payload.get("playerName").asText() : "PLAYER";
            }

            gameManager.getState().addLobbyChatMessage(
                    new LobbyChatMessage(playerName, message, System.currentTimeMillis())
            );
            broadcastGameState();
        } catch (Exception e) {
            System.err.println("[PacketProcessor] Error handling LOBBY_CHAT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLobbyReady(Packet packet) {
        System.out.println("[PacketProcessor] Received LOBBY_READY packet: " + packet);

        try {
            JsonNode payload = packet.getPayload();
            boolean ready = payload.has("ready") && payload.get("ready").asBoolean();
            String playerName = clientHandler != null ? clientHandler.getConnectedPlayerName() : null;

            if (playerName == null || playerName.isBlank()) {
                System.err.println("[PacketProcessor] Ready update ignored because client is not registered.");
                return;
            }

            PlayerState playerState = findPlayerState(playerName);
            if (playerState == null) {
                System.err.println("[PacketProcessor] Ready update ignored. Player not found: " + playerName);
                return;
            }

            playerState.setReady(ready);
            updateLobbyCountdown();
            broadcastGameState();
        } catch (Exception e) {
            System.err.println("[PacketProcessor] Error handling LOBBY_READY: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDisconnect(Packet packet) {
        System.out.println("[PacketProcessor] Received DISCONNECT packet: " + packet);
        disconnectCurrentPlayer();
        if (clientHandler != null) {
            clientHandler.closeConnection();
        }
    }

    private void handleError(Packet packet) {
        System.err.println("[PacketProcessor] Received ERROR packet: " + packet);
    }

    private PlayerState findPlayerState(String playerName) {
        for (PlayerState ps : gameManager.getState().getPlayers()) {
            if (ps.getPlayerName().equals(playerName)) {
                return ps;
            }
        }
        return null;
    }

    public void disconnectCurrentPlayer() {
        if (gameManager == null || clientHandler == null) {
            return;
        }

        String playerName = clientHandler.getConnectedPlayerName();
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        boolean removed = gameManager.getState().getPlayers()
                .removeIf(player -> playerName.equals(player.getPlayerName()));

        if (removed) {
            System.out.println("[PacketProcessor] Player disconnected: " + playerName);
            updateLobbyCountdown();
            clientHandler.clearConnectedPlayerName();
            broadcastGameState();
        }
    }

    private void broadcastGameState() {
        if (clientHandler != null) {
            clientHandler.broadcastGameStateToAll();
        }
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
                    if (!areAllPlayersReady() || gameManager.getState().isGameStarted()) {
                        gameManager.getState().setLobbyCountdownStartedAt(0);
                        broadcastGameState();
                        return;
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
        if (gameManager.getState().getPlayers().isEmpty()) {
            return false;
        }

        for (PlayerState player : gameManager.getState().getPlayers()) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }
}
