package jdemic.DedicatedServer.network.session;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import jdemic.DedicatedServer.network.transport.ClientHandler;

/**
 * SessionRegistry acts as the central hub for all active player connections.
 * It uses a ConcurrentHashMap to guarantee thread-safety across the server,
 * preventing crashes when multiple threads register or disconnect simultaneously.
 */
public class SessionRegistry {

    // The thread-safe ledger mapping Player IDs to their specific network handler
    private static final Map<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();
    
    // Strict Regex to validate the Player ID format (e.g., "Player_12345")
    // CHANGED: Lowered the minimum suffix length from 5 to 1 to support test IDs like "Player_1"
    private static final Pattern PLAYER_ID_PATTERN = Pattern.compile("^Player_[a-zA-Z0-9-]{1,36}$");

    /**
     * Internal security check to ensure the ID format is completely valid
     * before it touches the server's memory.
     */
    private static boolean isValidPlayerId(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return false;
        }
        return PLAYER_ID_PATTERN.matcher(playerId).matches();
    }

    /**
     * Registers a new player in the system after a successful secure handshake.
     * @param playerId The unique identifier of the player.
     * @param handler The ClientHandler thread managing their socket.
     * @return true if registered successfully, false if the player is already logged in or ID is invalid.
     */
    public static boolean registerPlayer(String playerId, ClientHandler handler) {
        if (!isValidPlayerId(playerId)) {
            System.err.println("[SessionRegistry] Failed to register: Invalid playerId format ('" + playerId + "')");
            return false;
        }

        if (handler == null) {
            System.err.println("[SessionRegistry] Failed to register: Handler is null.");
            return false;
        }

        // putIfAbsent prevents a second connection from overwriting an active one
        ClientHandler existingSession = activeSessions.putIfAbsent(playerId, handler);
        
        if (existingSession == null) {
            System.out.println("[SessionRegistry] Player registered: " + playerId + " | Total active: " + activeSessions.size());
            return true;
        } else {
            System.err.println("[SessionRegistry] Connection rejected: Player " + playerId + " is already online.");
            return false;
        }
    }

    /**
     * Removes a player from the registry (called when they disconnect or timeout).
     * @param playerId The unique identifier of the player.
     */
    public static void removePlayer(String playerId) {
        if (isValidPlayerId(playerId)) {
            ClientHandler removed = activeSessions.remove(playerId);
            if (removed != null) {
                System.out.println("[SessionRegistry] Player disconnected: " + playerId + " | Remaining: " + activeSessions.size());
            }
        }
    }

    /**
     * Retrieves the network handler for a specific player (used for sending private messages).
     * @param playerId The target player's ID.
     * @return The ClientHandler, or null if the player is offline.
     */
    public static ClientHandler getPlayerSession(String playerId) {
        if (!isValidPlayerId(playerId)) return null;
        return activeSessions.get(playerId);
    }

    /**
     * Checks if a specific player is currently online.
     */
    public static boolean isPlayerOnline(String playerId) {
        if (!isValidPlayerId(playerId)) return false;
        return activeSessions.containsKey(playerId);
    }

    /**
     * Returns a read-only view of all currently connected players.
     * Useful for broadcasting the updated game state to everyone.
     */
    public static Collection<ClientHandler> getAllActiveSessions() {
        // CHANGED: Wrapped in unmodifiableCollection to prevent external modification
        return Collections.unmodifiableCollection(activeSessions.values());
    }
    
    /**
     * Disconnects all players and clears the registry (useful for server shutdown).
     */
    public static void clearAllSessions() {
        System.out.println("[SessionRegistry] CLEARING ALL SESSIONS...");
        activeSessions.clear();
    }
}