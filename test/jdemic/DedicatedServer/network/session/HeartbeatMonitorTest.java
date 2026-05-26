package jdemic.DedicatedServer.network.session;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbeatMonitorTest {

    @Test
    void shouldRejectInvalidClientRegistrationInputs() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        try (ConnectedSockets sockets = openConnectedSockets()) {
            PrintWriter writer = new PrintWriter(new StringWriter());

            assertFalse(monitor.registerClient(null, writer, sockets.serverSide()));
            assertFalse(monitor.registerClient("   ", writer, sockets.serverSide()));
            assertFalse(monitor.registerClient("player-1", null, sockets.serverSide()));
            assertFalse(monitor.registerClient("player-1", writer, null));
            assertFalse(monitor.registerClient("player-1", writer, new Socket()));
            assertEquals(0, monitor.getRegisteredClientCount());
        } finally {
            monitor.stop();
        }
    }

    @Test
    void duplicateRegistrationShouldKeepOneSessionEntry() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        try (ConnectedSockets first = openConnectedSockets();
             ConnectedSockets second = openConnectedSockets()) {
            assertTrue(monitor.registerClient("player-1", new PrintWriter(first.clientSide().getOutputStream(), true), first.serverSide()));
            Long firstTimestamp = monitor.getLastPongTimestamp("player-1");

            Thread.sleep(5);

            assertTrue(monitor.registerClient("player-1", new PrintWriter(second.clientSide().getOutputStream(), true), second.serverSide()));

            assertEquals(1, monitor.getRegisteredClientCount());
            assertTrue(monitor.isClientRegistered("player-1"));
            assertTrue(monitor.getLastPongTimestamp("player-1") >= firstTimestamp);
        } finally {
            monitor.stop();
        }
    }

    @Test
    void unregisterShouldCleanUpClientStateAndHandleNullSafely() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        try (ConnectedSockets sockets = openConnectedSockets()) {
            assertTrue(monitor.registerClient("player-1", new PrintWriter(sockets.clientSide().getOutputStream(), true), sockets.serverSide()));

            monitor.unregisterClient("player-1");

            assertEquals(0, monitor.getRegisteredClientCount());
            assertFalse(monitor.isClientRegistered("player-1"));
            assertDoesNotThrow(() -> monitor.unregisterClient(null));
        } finally {
            monitor.stop();
        }
    }

    @Test
    void receivePongShouldRefreshKnownClientTimestampOnly() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        try (ConnectedSockets sockets = openConnectedSockets()) {
            assertTrue(monitor.registerClient("player-1", new PrintWriter(sockets.clientSide().getOutputStream(), true), sockets.serverSide()));
            Long beforePong = monitor.getLastPongTimestamp("player-1");
            assertNotNull(beforePong);

            Thread.sleep(5);
            monitor.receivePong("player-1");
            monitor.receivePong("unknown-player");
            monitor.receivePong(null);

            assertTrue(monitor.getLastPongTimestamp("player-1") >= beforePong);
            assertEquals(1, monitor.getRegisteredClientCount());
        } finally {
            monitor.stop();
        }
    }

    @Test
    void startAndStopShouldCompleteWithoutThrowing() {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        assertDoesNotThrow(() -> {
            monitor.start();
            Thread.sleep(50);
            monitor.stop();
        });
    }

    private ConnectedSockets openConnectedSockets() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<Socket> acceptedSocket = executor.submit(listener::accept);
                Socket clientSide = new Socket("localhost", listener.getLocalPort());
                Socket serverSide = acceptedSocket.get(2, TimeUnit.SECONDS);
                return new ConnectedSockets(clientSide, serverSide);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private record ConnectedSockets(Socket clientSide, Socket serverSide) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            clientSide.close();
            serverSide.close();
        }
    }
}
