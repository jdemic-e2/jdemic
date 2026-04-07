package jdemic.DedicatedServer.network.session;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitors client connections and detects zombies (dead/unresponsive ones).
 *
 * Sends PING every 3s, expects PONG back. If no PONG within 10s,
 * force-closes the connection and notifies the backend.
 *
 * playerId = ClientHandler.getPlayerId() (e.g. "Player_a3f2b").
 * On timeout, DisconnectListener calls SessionRegistry.removePlayer().
 */
public class HeartbeatMonitor {

    // how often we ping (seconds)
    private static final int PING_INTERVAL_SECONDS = 3;

    // if no pong after this many ms, connection is dead
    private static final long TIMEOUT_MS = 10_000;

    private static final String PING_PACKET = "PING";

    // playerId -> when we last got a PONG from them
    private final ConcurrentHashMap<String, Long> lastPongTimestamps;

    // playerId -> output stream (we send PINGs through this)
    private final ConcurrentHashMap<String, PrintWriter> clientOutputStreams;

    // playerId -> raw socket (need this to force-close on timeout)
    private final ConcurrentHashMap<String, Socket> clientSockets;

    private final ScheduledExecutorService scheduler;

    private DisconnectListener disconnectListener;

    // callback for when we kick someone due to timeout
    public interface DisconnectListener {
        void onPlayerDisconnected(String playerId);
    }


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


    public void setDisconnectListener(DisconnectListener listener) {
        this.disconnectListener = listener;
    }

    /**
     * Registers a client for monitoring. Call this after SessionRegistry.registerPlayer().
     * All 3 params must be non-null and socket must be open - ConcurrentHashMap
     * throws NPE on null keys/values so we validate everything upfront.
     *
     * @return true if registered, false if something was invalid
     */
    public boolean registerClient(String playerId, PrintWriter outStream, Socket rawSocket) {
        // null key = NPE in ConcurrentHashMap
        if (playerId == null || playerId.isBlank()) {
            System.err.println("[HeartbeatMonitor] Registration rejected: playerId is null or blank.");
            return false;
        }

        // no stream = can't send PINGs
        if (outStream == null) {
            System.err.println("[HeartbeatMonitor] Registration rejected for " + playerId
                    + ": outStream is null. Cannot send PING without output stream.");
            return false;
        }

        // no socket = can't force-close on timeout
        if (rawSocket == null) {
            System.err.println("[HeartbeatMonitor] Registration rejected for " + playerId
                    + ": rawSocket is null. Cannot monitor a client without a socket.");
            return false;
        }

        // already dead socket, don't bother
        if (rawSocket.isClosed() || !rawSocket.isConnected()) {
            System.err.println("[HeartbeatMonitor] Registration rejected for " + playerId
                    + ": socket is closed or not connected.");
            return false;
        }

        // all good, add to maps
        lastPongTimestamps.put(playerId, System.currentTimeMillis());
        clientOutputStreams.put(playerId, outStream);
        clientSockets.put(playerId, rawSocket);
        System.out.println("[HeartbeatMonitor] Registered client: " + playerId);
        return true;
    }

    /**
     * Removes a client from monitoring (normal disconnect or SessionRegistry removal).
     */
    public void unregisterClient(String playerId) {
        // ConcurrentHashMap.remove(null) throws NPE
        if (playerId == null) {
            System.err.println("[HeartbeatMonitor] Cannot unregister: playerId is null.");
            return;
        }
        lastPongTimestamps.remove(playerId);
        clientOutputStreams.remove(playerId);
        clientSockets.remove(playerId);
        System.out.println("[HeartbeatMonitor] Unregistered client: " + playerId);
    }

    /**
     * Called by PacketProcessor when we get a PONG back.
     */
    public void receivePong(String playerId) {
        // containsKey(null) throws NPE
        if (playerId == null) {
            return;
        }
        if (lastPongTimestamps.containsKey(playerId)) {
            lastPongTimestamps.put(playerId, System.currentTimeMillis());
        }
    }

    /**
     * Starts the background loop (ping everyone, check for timeouts).
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
     * Shuts down the scheduler, waits up to 5s for it to finish.
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

    // runs every PING_INTERVAL_SECONDS - sends PINGs and kicks timed-out clients
    private void heartbeatCycle() {
        long now = System.currentTimeMillis();


        for (Map.Entry<String, Long> entry : lastPongTimestamps.entrySet()) {
            String playerId = entry.getKey();
            long lastPong = entry.getValue();

            // send PING
            PrintWriter out = clientOutputStreams.get(playerId);
            if (out != null && !out.checkError()) {
                out.println(PING_PACKET);
                out.flush();
            }

            // check if they timed out
            long timeSinceLastPong = now - lastPong;
            if (timeSinceLastPong > TIMEOUT_MS) {
                System.out.println("[HeartbeatMonitor] TIMEOUT for client: " + playerId
                        + " (no PONG for " + timeSinceLastPong + "ms). Forcing disconnect.");
                forceDisconnect(playerId);
            }
        }
    }

    // force-closes a timed out client: close socket -> unregister -> notify listener
    private void forceDisconnect(String playerId) {

        Socket socket = clientSockets.get(playerId);
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (Exception e) {
                System.err.println("[HeartbeatMonitor] Error closing socket for: " + playerId);
            }
        }


        unregisterClient(playerId);

        // tell the backend (they should call SessionRegistry.removePlayer)
        if (disconnectListener != null) {
            disconnectListener.onPlayerDisconnected(playerId);
        }
    }

    // --- getters (mostly for tests) ---

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
