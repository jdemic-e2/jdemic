package jdemic.DedicatedServer.network.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jdemic.DedicatedServer.network.session.HeartbeatMonitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strategy: reflection
 *   - Access lastPongTimestamps directly to backdate the timestamp by 11s
 *   - Invoke heartbeatCycle() directly, bypassing the scheduler entirely
 *   - Capture the DisconnectListener callback via a lambda AtomicReference
 */
@DisplayName("HeartbeatMonitor Tests")
public class HeartbeatMonitorMissingPongTest {

    private HeartbeatMonitor monitor;

    // A real loopback socket pair so the socket is genuinely open/connected
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter clientOut;

    @BeforeEach
    void setUp() throws Exception {
        monitor = new HeartbeatMonitor();

        // Open a real loopback socket — HeartbeatMonitor validates isClosed() / isConnected()
        serverSocket = new ServerSocket(0); // OS picks a free port
        clientSocket = new Socket("localhost", serverSocket.getLocalPort());
        clientOut = new PrintWriter(new StringWriter()); // we aint need no output from ping cuh
    }

    @AfterEach
    void tearDown() throws Exception {
        monitor.stop();
        if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
    }

    // -------------------------------------------------------------------------
    // Helper: backdate lastPongTimestamps for a player via reflection
    // -------------------------------------------------------------------------
    private void backdateLastPong(String playerId, long msAgo) throws Exception {
        Field field = HeartbeatMonitor.class.getDeclaredField("lastPongTimestamps");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Long> timestamps =
                (ConcurrentHashMap<String, Long>) field.get(monitor);
        timestamps.put(playerId, System.currentTimeMillis() - msAgo);
    }

    // -------------------------------------------------------------------------
    // Helper: invoke the private heartbeatCycle() directly via reflection
    // -------------------------------------------------------------------------
    private void triggerHeartbeatCycle() throws Exception {
        Method method = HeartbeatMonitor.class.getDeclaredMethod("heartbeatCycle");
        method.setAccessible(true);
        method.invoke(monitor);
    }

    // =========================================================================
    // CORE TEST — the one from the task description
    // =========================================================================

    @Test
    @DisplayName("After 10s without PONG, disconnect listener should be called with the correct playerId")
    void testTimeout_callsDisconnectListener_afterTenSeconds() throws Exception {
        String playerId = "Player_test01";

        // AtomicReference captures which playerId the listener received
        AtomicReference<String> disconnectedId = new AtomicReference<>(null);

        monitor.setDisconnectListener(id -> disconnectedId.set(id));
        monitor.registerClient(playerId, clientOut, clientSocket);

        // Simulate 11 seconds without a PONG (just over the 10s threshold)
        backdateLastPong(playerId, 11_000);

        // Fire the heartbeat cycle directly — no real waiting
        triggerHeartbeatCycle();

        // The disconnect listener must have been called with the correct playerId
        assertEquals(playerId, disconnectedId.get(),
                "DisconnectListener should have been called with the timed-out playerId");
    }

    // =========================================================================
    // SUPPORTING TESTS
    // =========================================================================

    @Test
    @DisplayName("After timeout, client should be unregistered from the monitor")
    void testTimeout_clientIsUnregistered_afterDisconnect() throws Exception {
        String playerId = "Player_test02";

        monitor.setDisconnectListener(id -> {}); // no-op listener
        monitor.registerClient(playerId, clientOut, clientSocket);

        assertTrue(monitor.isClientRegistered(playerId),
                "Client should be registered before the timeout");

        backdateLastPong(playerId, 11_000);
        triggerHeartbeatCycle();

        assertFalse(monitor.isClientRegistered(playerId),
                "Client should be unregistered after the timeout disconnect");
    }

    @Test
    @DisplayName("After timeout, the client socket should be closed")
    void testTimeout_socketIsClosed_afterDisconnect() throws Exception {
        String playerId = "Player_test03";

        monitor.setDisconnectListener(id -> {});
        monitor.registerClient(playerId, clientOut, clientSocket);

        backdateLastPong(playerId, 11_000);
        triggerHeartbeatCycle();

        assertTrue(clientSocket.isClosed(),
                "The client socket should be closed after a timeout disconnect");
    }

    @Test
    @DisplayName("Client with recent PONG (under 10s) should NOT be disconnected")
    void testNoTimeout_clientWithRecentPong_isNotDisconnected() throws Exception {
        String playerId = "Player_test04";

        AtomicReference<String> disconnectedId = new AtomicReference<>(null);

        monitor.setDisconnectListener(id -> disconnectedId.set(id));
        monitor.registerClient(playerId, clientOut, clientSocket);

        // Only 5 seconds ago — well within the 10s threshold
        backdateLastPong(playerId, 5_000);
        triggerHeartbeatCycle();

        assertNull(disconnectedId.get(),
                "DisconnectListener should NOT be called for a client within the timeout window");
        assertTrue(monitor.isClientRegistered(playerId),
                "Client should still be registered when within the timeout window");
    }

    @Test
    @DisplayName("receivePong() should reset the timestamp and prevent a timeout")
    void testReceivePong_preventsTimeout() throws Exception {
        String playerId = "Player_test05";

        AtomicReference<String> disconnectedId = new AtomicReference<>(null);

        monitor.setDisconnectListener(id -> disconnectedId.set(id));
        monitor.registerClient(playerId, clientOut, clientSocket);

        // First backdate it — would normally trigger disconnect
        backdateLastPong(playerId, 11_000);

        // Client sends PONG — resets the timestamp to now
        monitor.receivePong(playerId);

        triggerHeartbeatCycle();

        assertNull(disconnectedId.get(),
                "DisconnectListener should NOT be called after a PONG resets the timestamp");
        assertTrue(monitor.isClientRegistered(playerId),
                "Client should still be registered after sending a PONG");
    }
}
