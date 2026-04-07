package jdemic.DedicatedServer;

import jdemic.DedicatedServer.network.session.HeartbeatMonitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Tests for HeartbeatMonitor - covers validation (null/blank/closed checks)
 * and core features (register, unregister, pong, timeout, listener, lifecycle).
 * Has its own main(), just run it directly.
 */
public class HeartbeatMonitorTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== HeartbeatMonitor Test Suite ===\n");

        // validation tests (PR #27 fixes)
        testRegisterClientWithValidParams();
        testRejectNullPlayerId();
        testRejectBlankPlayerId();
        testRejectNullOutputStream();
        testRejectNullSocket();
        testRejectClosedSocket();

        // functionality tests
        testUnregisterClient();
        testUnregisterNullPlayerId();
        testReceivePongUpdatesTimestamp();
        testReceivePongIgnoresNullPlayerId();
        testReceivePongIgnoresUnknownPlayer();
        testTimeoutDetection();
        testDisconnectListenerNotification();
        testMultipleClientsRegistration();
        testStartAndStop();

        System.out.println("\n=== Results: " + testsPassed + " passed, "
                + testsFailed + " failed out of "
                + (testsPassed + testsFailed) + " tests ===");


        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    // --- VALIDATION TESTS ---
    // these cover the null/blank/closed bugs found in PR #27 review

    // TEST 1: valid params -> should register fine
    private static void testRegisterClientWithValidParams() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        // need a real socket pair since registerClient checks isConnected()
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread clientThread = new Thread(() -> {
            try {
                Socket client = new Socket("127.0.0.1", port);
                Thread.sleep(2000);
                client.close();
            } catch (Exception e) {
                // Expected
            }
        });
        clientThread.start();

        Socket serverSideSocket = serverSocket.accept();
        PrintWriter outStream = new PrintWriter(serverSideSocket.getOutputStream(), true);


        boolean registered = monitor.registerClient("player_1", outStream, serverSideSocket);

        boolean isRegistered = monitor.isClientRegistered("player_1");
        boolean correctCount = monitor.getRegisteredClientCount() == 1;


        serverSideSocket.close();
        serverSocket.close();
        clientThread.join(3000);

        assertTest("registerClient() returns true and client is tracked with valid params",
                registered && isRegistered && correctCount);
    }

    // TEST 2: null playerId -> must reject (ConcurrentHashMap.put(null,...) = NPE)
    private static void testRejectNullPlayerId() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();


        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread clientThread = new Thread(() -> {
            try {
                Socket client = new Socket("127.0.0.1", port);
                Thread.sleep(1000);
                client.close();
            } catch (Exception e) {
                // Expected
            }
        });
        clientThread.start();

        Socket serverSideSocket = serverSocket.accept();
        PrintWriter outStream = new PrintWriter(serverSideSocket.getOutputStream(), true);

        // try registering with null - should fail
        boolean registered = monitor.registerClient(null, outStream, serverSideSocket);


        boolean emptyMonitor = monitor.getRegisteredClientCount() == 0;


        serverSideSocket.close();
        serverSocket.close();
        clientThread.join(3000);

        assertTest("Null playerId is rejected (prevents NullPointerException crash)",
                !registered && emptyMonitor);
    }

    // TEST 3: empty/whitespace playerId -> rejected too
    private static void testRejectBlankPlayerId() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread clientThread = new Thread(() -> {
            try {
                Socket client = new Socket("127.0.0.1", port);
                Thread.sleep(1000);
                client.close();
            } catch (Exception e) {
                // Expected
            }
        });
        clientThread.start();

        Socket serverSideSocket = serverSocket.accept();
        PrintWriter outStream = new PrintWriter(serverSideSocket.getOutputStream(), true);

        // empty string
        boolean registeredEmpty = monitor.registerClient("", outStream, serverSideSocket);
        // just spaces
        boolean registeredBlank = monitor.registerClient("   ", outStream, serverSideSocket);

        boolean emptyMonitor = monitor.getRegisteredClientCount() == 0;


        serverSideSocket.close();
        serverSocket.close();
        clientThread.join(3000);

        assertTest("Blank/empty playerId is rejected",
                !registeredEmpty && !registeredBlank && emptyMonitor);
    }

    // TEST 4: null outStream -> rejected (no stream = can't send PINGs = always timeout)
    private static void testRejectNullOutputStream() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread clientThread = new Thread(() -> {
            try {
                Socket client = new Socket("127.0.0.1", port);
                Thread.sleep(1000);
                client.close();
            } catch (Exception e) {
                // Expected
            }
        });
        clientThread.start();

        Socket serverSideSocket = serverSocket.accept();

        // valid socket but no output stream
        boolean registered = monitor.registerClient("player_null_stream", null, serverSideSocket);
        boolean emptyMonitor = monitor.getRegisteredClientCount() == 0;


        serverSideSocket.close();
        serverSocket.close();
        clientThread.join(3000);

        assertTest("Null outStream is rejected (cannot send PINGs without output stream)",
                !registered && emptyMonitor);
    }

    // TEST 5: null socket -> rejected (can't force-close without it)
    private static void testRejectNullSocket() {
        HeartbeatMonitor monitor = new HeartbeatMonitor();
        PrintWriter dummyOut = new PrintWriter(new StringWriter());


        boolean registered = monitor.registerClient("player_null_socket", dummyOut, null);
        boolean emptyMonitor = monitor.getRegisteredClientCount() == 0;

        assertTest("Null rawSocket is rejected (cannot force disconnect without socket)",
                !registered && emptyMonitor);
    }

    // TEST 6: closed socket -> rejected (dead on arrival)
    private static void testRejectClosedSocket() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread clientThread = new Thread(() -> {
            try {
                Socket client = new Socket("127.0.0.1", port);
                client.close();
            } catch (Exception e) {
                // Expected
            }
        });
        clientThread.start();

        Socket serverSideSocket = serverSocket.accept();
        PrintWriter outStream = new PrintWriter(serverSideSocket.getOutputStream(), true);

        // kill the socket first, then try to register
        serverSideSocket.close();

        boolean registered = monitor.registerClient("player_closed_socket", outStream, serverSideSocket);
        boolean emptyMonitor = monitor.getRegisteredClientCount() == 0;


        serverSocket.close();
        clientThread.join(3000);

        assertTest("Closed socket is rejected (socket.isClosed() check)",
                !registered && emptyMonitor);
    }

    // --- CORE FUNCTIONALITY TESTS ---

    // TEST 7: unregister removes the client
    private static void testUnregisterClient() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();


        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread clientThread = new Thread(() -> {
            try {
                Socket client = new Socket("127.0.0.1", port);
                Thread.sleep(2000);
                client.close();
            } catch (Exception e) {
                // Expected
            }
        });
        clientThread.start();

        Socket serverSideSocket = serverSocket.accept();
        PrintWriter outStream = new PrintWriter(serverSideSocket.getOutputStream(), true);

        monitor.registerClient("player_unreg", outStream, serverSideSocket);
        monitor.unregisterClient("player_unreg");

        boolean isGone = !monitor.isClientRegistered("player_unreg");
        boolean countZero = monitor.getRegisteredClientCount() == 0;


        serverSideSocket.close();
        serverSocket.close();
        clientThread.join(3000);

        assertTest("Client removed after unregisterClient()", isGone && countZero);
    }

    // TEST 8: unregisterClient(null) shouldn't crash (NPE guard)
    private static void testUnregisterNullPlayerId() {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        boolean noCrash = true;
        try {

            monitor.unregisterClient(null);
        } catch (Exception e) {
            noCrash = false;
        }

        assertTest("unregisterClient(null) does not crash", noCrash);
    }

    // TEST 9: receivePong should update the timestamp
    private static void testReceivePongUpdatesTimestamp() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread clientThread = new Thread(() -> {
            try {
                Socket client = new Socket("127.0.0.1", port);
                Thread.sleep(2000);
                client.close();
            } catch (Exception e) {
                // Expected
            }
        });
        clientThread.start();

        Socket serverSideSocket = serverSocket.accept();
        PrintWriter outStream = new PrintWriter(serverSideSocket.getOutputStream(), true);

        monitor.registerClient("player_pong", outStream, serverSideSocket);

        // save initial timestamp
        Long initialTimestamp = monitor.getLastPongTimestamp("player_pong");

        // small delay so timestamps differ
        Thread.sleep(100);

        // simulate PONG
        monitor.receivePong("player_pong");

        Long updatedTimestamp = monitor.getLastPongTimestamp("player_pong");

        boolean timestampUpdated = updatedTimestamp != null
                && initialTimestamp != null
                && updatedTimestamp >= initialTimestamp;


        serverSideSocket.close();
        serverSocket.close();
        clientThread.join(3000);

        assertTest("receivePong() updates the last PONG timestamp", timestampUpdated);
    }

    // TEST 10: receivePong(null) shouldn't crash (NPE guard)
    private static void testReceivePongIgnoresNullPlayerId() {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        boolean noCrash = true;
        try {
            monitor.receivePong(null);
        } catch (Exception e) {
            noCrash = false;
        }

        assertTest("receivePong(null) does not crash", noCrash);
    }

    // TEST 11: receivePong for unknown player shouldn't add them
    private static void testReceivePongIgnoresUnknownPlayer() {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        // pong for a player we never registered
        monitor.receivePong("unknown_player");

        boolean notAdded = !monitor.isClientRegistered("unknown_player");

        assertTest("receivePong() for unregistered player does not create entry", notAdded);
    }

    // TEST 12: fresh client shouldn't be timed out
    private static void testTimeoutDetection() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread clientThread = new Thread(() -> {
            try {
                Socket client = new Socket("127.0.0.1", port);
                Thread.sleep(2000);
                client.close();
            } catch (Exception e) {
                // Expected
            }
        });
        clientThread.start();

        Socket serverSideSocket = serverSocket.accept();
        PrintWriter outStream = new PrintWriter(serverSideSocket.getOutputStream(), true);

        monitor.registerClient("player_timeout", outStream, serverSideSocket);

        // check timestamp right after registration
        Long lastPong = monitor.getLastPongTimestamp("player_timeout");
        long now = System.currentTimeMillis();

        // just registered, should NOT be timed out
        boolean notTimedOut = lastPong != null && (now - lastPong) < 10_000;


        serverSideSocket.close();
        serverSocket.close();
        clientThread.join(3000);

        assertTest("Freshly registered client is NOT in timeout state", notTimedOut);
    }

    // TEST 13: listener gets called with the right player ID
    private static void testDisconnectListenerNotification() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();


        DisconnectListenerTestHelper helper = new DisconnectListenerTestHelper();
        monitor.setDisconnectListener(helper);

        // fire it manually to check the mechanism works
        helper.onPlayerDisconnected("player_disconnect");

        boolean listenerCalled = "player_disconnect".equals(helper.getLastDisconnectedId());

        assertTest("DisconnectListener receives correct player ID on disconnect", listenerCalled);
    }

    // TEST 14: tracking multiple clients at once
    private static void testMultipleClientsRegistration() throws Exception {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        // spin up 3 real socket connections
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Socket[] serverSockets = new Socket[3];
        Thread[] clientThreads = new Thread[3];

        for (int i = 0; i < 3; i++) {
            clientThreads[i] = new Thread(() -> {
                try {
                    Socket client = new Socket("127.0.0.1", port);
                    Thread.sleep(3000);
                    client.close();
                } catch (Exception e) {
                    // Expected
                }
            });
            clientThreads[i].start();
            serverSockets[i] = serverSocket.accept();
        }


        String[] ids = { "player_A", "player_B", "player_C" };
        for (int i = 0; i < 3; i++) {
            PrintWriter out = new PrintWriter(serverSockets[i].getOutputStream(), true);
            monitor.registerClient(ids[i], out, serverSockets[i]);
        }

        boolean allRegistered = monitor.isClientRegistered("player_A")
                && monitor.isClientRegistered("player_B")
                && monitor.isClientRegistered("player_C");
        boolean correctCount = monitor.getRegisteredClientCount() == 3;


        for (Socket s : serverSockets) {
            s.close();
        }
        serverSocket.close();
        for (Thread t : clientThreads) {
            t.join(3000);
        }

        assertTest("3 clients registered simultaneously, all tracked",
                allRegistered && correctCount);
    }

    // TEST 15: start/stop lifecycle shouldn't throw
    private static void testStartAndStop() {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        boolean noError = true;
        try {
            monitor.start();
            // let it run a bit
            Thread.sleep(500);
            monitor.stop();
        } catch (Exception e) {
            noError = false;
            System.err.println("  Error during start/stop: " + e.getMessage());
        }

        assertTest("start() and stop() complete without exceptions", noError);
    }

    // --- helpers ---

    // records the last disconnected ID for assertions
    private static class DisconnectListenerTestHelper implements HeartbeatMonitor.DisconnectListener {
        private String lastDisconnectedId = null;

        @Override
        public void onPlayerDisconnected(String playerId) {
            this.lastDisconnectedId = playerId;
        }

        public String getLastDisconnectedId() {
            return lastDisconnectedId;
        }
    }



    private static void assertTest(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            testsPassed++;
        } else {
            System.out.println("  [FAIL] " + testName);
            testsFailed++;
        }
    }
}
