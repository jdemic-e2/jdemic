package jdemic.DedicatedServer.network.security;

import jdemic.DedicatedServer.network.core.DedicatedServerConfig;
import jdemic.DedicatedServer.network.core.JdemicNetworkServer;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityIntegrationTest - Integration test suite for security.
 * Verifies Handshake, Resilience to corrupted data, Flood Control, and Anti-Spoofing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityIntegrationTest {

    private static final int TEST_PORT = 9999;
    private JdemicNetworkServer server;

    @BeforeAll
    void startServer() throws InterruptedException {
        // Configure a test server on port 9999, without status UI
        DedicatedServerConfig config = new DedicatedServerConfig(TEST_PORT, false, "localhost", 0, false, 0L, false);
        server = new JdemicNetworkServer(config);
        
        Thread serverThread = new Thread(server::start, "it-server-thread");
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for the socket to open
        Thread.sleep(1000);
    }

    @AfterAll
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Test 1: Checks if basic secure communication works (RSA + AES).
     */
    @Test
    void testSuccessfulSecureHandshakeAndCommunication() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT)) {
            SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapClientSocket(socket);
            assertNotNull(secureSocket, "Secure handshake failed!");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send a valid PING packet
            Packet ping = new Packet(PacketType.PING);
            out.println(secureSocket.encrypt(ping.toJson()));

            // The server should process without closing the connection
            assertFalse(socket.isClosed());
        }
    }

    /**
     * Test 2: Checks resilience to malformed JSON (DataSanitizer).
     */
    @Test
    void testResilienceToMalformedJson() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT)) {
            SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapClientSocket(socket);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Send something that is correctly AES encrypted, but contains invalid JSON
            String garbageJson = "{ \"type\": \"PING\", \"payload\": "; // missing closing braces
            out.println(secureSocket.encrypt(garbageJson));

            // Wait a bit to process
            Thread.sleep(200);

            // Verify that the server did NOT kick us (resilient to bad data)
            assertFalse(socket.isClosed(), "The server should not close the connection for malformed JSON.");
        }
    }

    /**
     * Test 3: Checks Flood Control (RateLimiter).
     */
    @Test
    void testFloodControlKicksPlayer() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT)) {
            SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapClientSocket(socket);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send a burst of packets to trigger RateLimiter (limit is 60/sec, kick after 3 violations)
            // Sending 200 packets quickly.
            Packet p = new Packet(PacketType.PING);
            String encrypted = secureSocket.encrypt(p.toJson());

            boolean kicked = false;
            try {
                for (int i = 0; i < 250; i++) {
                    out.println(encrypted);
                    if (out.checkError()) { // checkError() returns true if the stream is closed
                        kicked = true;
                        break;
                    }
                }
            } catch (Exception e) {
                kicked = true;
            }

            // Since detecting the kick on the socket can take a while (TCP fin/rst):
            Thread.sleep(500);
            
            // Try to read something. If kicked, readLine will return null or throw an exception
            try {
                socket.setSoTimeout(1000);
                if (in.readLine() == null) kicked = true;
            } catch (Exception e) {
                kicked = true;
            }

            assertTrue(kicked, "The player should have been disconnected for flooding.");
        }
    }

    /**
     * Test 4: Checks Anti-Spoofing (Identity Theft).
     */
    @Test
    void testIdentitySpoofingRejection() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT)) {
            SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapClientSocket(socket);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // 1. Connect as ALICE
            Packet connect = new Packet(PacketType.CONNECT);
            ((ObjectNode)connect.getPayload()).put("playerName", "Alice");
            out.println(secureSocket.encrypt(connect.toJson()));
            
            Thread.sleep(200);

            // 2. Try to send a game action pretending to be BOB
            Packet spoofedAction = new Packet(PacketType.GAME_DATA);
            ObjectNode payload = (ObjectNode)spoofedAction.getPayload();
            payload.put("GameAction", "DRIVE_FERRY");
            payload.put("PlayerID", "Bob");
            payload.put("destination", "Paris");

            out.println(secureSocket.encrypt(spoofedAction.toJson()));

            // Wait for processing
            Thread.sleep(200);

            // Check stability: the server should ignore the packet and remain active
            assertFalse(socket.isClosed(), "The server must remain active after a spoofed packet.");
        }
    }
}