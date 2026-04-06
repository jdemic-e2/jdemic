package jdemic.DedicatedServer.network.session;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HeartbeatMonitor - Zombie Connection Detector
 *
 * Runs as an asynchronous background service that monitors all active
 * client connections registered in the SessionRegistry.
 *
 * Mechanism:
 * - Every 3 seconds, sends a PING packet to all connected clients
 *   through their respective ClientHandler output streams.
 * - Waits for a PONG response (which arrives back via PacketProcessor).
 * - If no PONG is received within 10 seconds (Timeout), the connection
 *   is considered dead ("zombie") and is forcefully closed.
 * - Upon timeout, the backend is notified that the player has left the match.
 *
 * Integration with other modules:
 * - The playerId parameter used in registerClient() corresponds to
 *   ClientHandler.getPlayerId() (e.g. "Player_a3f2b").
 * - On timeout, the DisconnectListener should call
 *   SessionRegistry.removePlayer(playerId) to keep the registry in sync.
 * - ClientHandler should call registerClient() in its run() method,
 *   right after SessionRegistry.registerPlayer() succeeds.
 */
public class HeartbeatMonitor {

    // Interval between PING signals (in seconds)
    private static final int PING_INTERVAL_SECONDS = 3;

    // Maximum time to wait for a PONG before declaring a connection dead (in ms)
    private static final long TIMEOUT_MS = 10_000;

    // The PING packet string sent to clients
    private static final String PING_PACKET = "PING";

    // Maps a player identifier (e.g. socket address or player ID) to the
    // timestamp of the last received PONG response
    private final ConcurrentHashMap<String, Long> lastPongTimestamps;

    // Maps a player identifier to their output stream (for sending PINGs)
    private final ConcurrentHashMap<String, PrintWriter> clientOutputStreams;

    // Maps a player identifier to their raw socket (for forced disconnection)
    private final ConcurrentHashMap<String, Socket> clientSockets;

    // Background scheduler for periodic PING tasks
    private final ScheduledExecutorService scheduler;

    // Listener interface for notifying the backend about disconnections
    private DisconnectListener disconnectListener;

    /**
     * Callback interface used to notify the game backend
     * when a player is disconnected due to heartbeat timeout.
     */
    public interface DisconnectListener {
        void onPlayerDisconnected(String playerId);
    }

    /**
     * Creates a new HeartbeatMonitor instance.
     * Does not start monitoring until start() is called.
     */
    public HeartbeatMonitor() {
        this.lastPongTimestamps = new ConcurrentHashMap<>();
        this.clientOutputStreams = new ConcurrentHashMap<>();
        this.clientSockets = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HeartbeatMonitor-Thread");
            t.setDaemon(true); // Won't prevent JVM shutdown
            return t;
        });
    }

    /**
     * Sets the listener that will be notified when a player
     * is forcefully disconnected due to heartbeat timeout.
     *
     * @param listener the callback to invoke on disconnect
     */
    public void setDisconnectListener(DisconnectListener listener) {
        this.disconnectListener = listener;
    }

    /**
     * Registers a new client for heartbeat monitoring.
     * Called after the connection has been secured and the client
     * has been added to the SessionRegistry.
     *
     * @param playerId   unique identifier for the player (e.g. socket address)
     * @param outStream  the PrintWriter to the client's output stream
     * @param rawSocket  the client's raw Socket for forced close
     */
    public void registerClient(String playerId, PrintWriter outStream, Socket rawSocket) {
        lastPongTimestamps.put(playerId, System.currentTimeMillis());
        if (outStream != null) {
            clientOutputStreams.put(playerId, outStream);
        }
        if (rawSocket != null) {
            clientSockets.put(playerId, rawSocket);
        }
        System.out.println("[HeartbeatMonitor] Registered client: " + playerId);
    }

    /**
     * Removes a client from heartbeat monitoring.
     * Called when a client disconnects normally or is removed from SessionRegistry.
     *
     * @param playerId the identifier of the player to unregister
     */
    public void unregisterClient(String playerId) {
        lastPongTimestamps.remove(playerId);
        clientOutputStreams.remove(playerId);
        clientSockets.remove(playerId);
        System.out.println("[HeartbeatMonitor] Unregistered client: " + playerId);
    }

    /**
     * Called by the PacketProcessor when a PONG response is received
     * from a client. Updates the timestamp of the last valid heartbeat.
     *
     * @param playerId the identifier of the client who responded
     */
    public void receivePong(String playerId) {
        if (lastPongTimestamps.containsKey(playerId)) {
            lastPongTimestamps.put(playerId, System.currentTimeMillis());
        }
    }

    /**
     * Starts the heartbeat monitoring service.
     * Schedules a periodic task every PING_INTERVAL_SECONDS that:
     * 1. Sends PING to all registered clients
     * 2. Checks for timed-out clients and disconnects them
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::heartbeatCycle,
                PING_INTERVAL_SECONDS,
                PING_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
        System.out.println("[HeartbeatMonitor] Started. PING interval: "
                + PING_INTERVAL_SECONDS + "s, Timeout: " + TIMEOUT_MS + "ms");
    }

    /**
     * Stops the heartbeat monitoring service gracefully.
     * Waits up to 5 seconds for pending tasks to complete.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[HeartbeatMonitor] Stopped.");
    }

    /**
     * The core heartbeat cycle executed periodically.
     * Sends PINGs and checks for timeouts.
     */
    private void heartbeatCycle() {
        long now = System.currentTimeMillis();

        // Iterate over all registered clients
        for (Map.Entry<String, Long> entry : lastPongTimestamps.entrySet()) {
            String playerId = entry.getKey();
            long lastPong = entry.getValue();

            // Step 1: Send a PING to the client
            PrintWriter out = clientOutputStreams.get(playerId);
            if (out != null && !out.checkError()) {
                out.println(PING_PACKET);
                out.flush();
            }

            // Step 2: Check if the client has timed out
            long timeSinceLastPong = now - lastPong;
            if (timeSinceLastPong > TIMEOUT_MS) {
                System.out.println("[HeartbeatMonitor] TIMEOUT for client: " + playerId
                        + " (no PONG for " + timeSinceLastPong + "ms). Forcing disconnect.");
                forceDisconnect(playerId);
            }
        }
    }

    /**
     * Forcefully closes a client's connection and notifies the backend.
     *
     * Integration with SessionRegistry:
     * The DisconnectListener callback is designed so that the server wiring code
     * can call SessionRegistry.removePlayer(playerId) when a timeout occurs.
     *
     * Example wiring (done in JdemicNetworkServer or main server setup):
     *   heartbeatMonitor.setDisconnectListener(playerId -> {
     *       SessionRegistry.removePlayer(playerId);
     *       System.out.println("Player " + playerId + " removed after heartbeat timeout.");
     *   });
     *
     * @param playerId the identifier of the timed-out player
     */
    private void forceDisconnect(String playerId) {
        // Close the socket
        Socket socket = clientSockets.get(playerId);
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (Exception e) {
                System.err.println("[HeartbeatMonitor] Error closing socket for: " + playerId);
            }
        }

        // Remove from heartbeat monitoring maps
        unregisterClient(playerId);

        // Notify the game backend (triggers SessionRegistry.removePlayer via listener)
        if (disconnectListener != null) {
            disconnectListener.onPlayerDisconnected(playerId);
        }
    }

    // --- Getters for testing and monitoring ---

    public int getRegisteredClientCount() {
        return lastPongTimestamps.size();
    }

    public boolean isClientRegistered(String playerId) {
        return lastPongTimestamps.containsKey(playerId);
    }

    public Long getLastPongTimestamp(String playerId) {
        return lastPongTimestamps.get(playerId);
    }
}
