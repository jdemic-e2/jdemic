package jdemic.GameLogic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GameClientTest {

    private GameClient gameClient;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        gameClient = new GameClient();
    }

    @AfterEach
    void tearDown() {
        gameClient.disconnect();
    }

    @Test
    void testListenerManagement() {
        // Test add null listener
        gameClient.addPlayerUpdateListener(null);

        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        GameClient.PlayerUpdateListener listener = gameState -> listenerCalled.set(true);

        // Test add and immediate callback if state exists (state is null right now, should not call)
        gameClient.addPlayerUpdateListener(listener);
        assertFalse(listenerCalled.get());

        // Remove listener
        gameClient.removePlayerUpdateListener(listener);

        // Clear listeners
        gameClient.clearPlayerUpdateListeners();
    }

    @Test
    void testConnectToServerSuccessAndDisconnection() throws Exception {
        SecureSocket mockSecureSocket = mock(SecureSocket.class);
        InputStream mockInputStream = new ByteArrayInputStream("".getBytes());
        OutputStream mockOutputStream = new ByteArrayOutputStream();

        try (org.mockito.MockedConstruction<Socket> mockedSocketConstruction = mockConstruction(Socket.class, 
                (mockSocket, context) -> {
                    when(mockSocket.getInputStream()).thenReturn(mockInputStream);
                    when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);
                });
             MockedStatic<SecureConnectionManager> mockedSecurity = mockStatic(SecureConnectionManager.class)) {

            mockedSecurity.when(() -> SecureConnectionManager.wrapClientSocket(any()))
                    .thenReturn(mockSecureSocket);
            
            when(mockSecureSocket.getRawSocket()).thenAnswer(inv -> {
                if (!mockedSocketConstruction.constructed().isEmpty()) {
                    return mockedSocketConstruction.constructed().get(0);
                }
                return null;
            });

            boolean connected = gameClient.connectToServer("localhost", 9000);
            assertTrue(connected);
            assertTrue(gameClient.isConnected());

            gameClient.disconnectFromLobby();
            assertFalse(gameClient.isConnected());
        }
    }

    @Test
    void testConnectToServerSecurityFailure() {
        try (MockedStatic<SecureConnectionManager> mockedSecurity = mockStatic(SecureConnectionManager.class)) {
            mockedSecurity.when(() -> SecureConnectionManager.wrapClientSocket(any())).thenReturn(null);

            boolean connected = gameClient.connectToServer("localhost", 9000);
            assertFalse(connected);
            assertFalse(gameClient.isConnected());
        }
    }

    @Test
    void testConnectToServerThrowsException() {
        try (MockedStatic<SecureConnectionManager> mockedSecurity = mockStatic(SecureConnectionManager.class)) {
            mockedSecurity.when(() -> SecureConnectionManager.wrapClientSocket(any())).thenThrow(new RuntimeException("Socket error"));

            boolean connected = gameClient.connectToServer("localhost", 9000);
            assertFalse(connected);
        }
    }

    @Test
    void testSendPacketWhenNotConnected() {
        // Structural guard checks logs and returns gracefully when secureSocket / out is null
        gameClient.sendPacket("{\"test\":\"data\"}");
        gameClient.sendPacket(new Packet(PacketType.PONG));
        assertFalse(gameClient.isConnected());
    }

    @Test
    void testReceivePacketNullStreams() {
        assertNull(gameClient.receivePacket());
    }

    @Test
    void testIncomingDataRoutingAndPacketParsing() throws Exception {
        // Construct raw Packet format string payload
        ObjectNode payload = mapper.createObjectNode().put("key", "value");
        Packet packetObj = new Packet(PacketType.GAME_DATA, payload);
        String validPacketJson = mapper.writeValueAsString(packetObj);

        AtomicBoolean listenerTriggered = new AtomicBoolean(false);
        gameClient.addPlayerUpdateListener(gameState -> {
            assertNotNull(gameState);
            listenerTriggered.set(true);
        });

        // Use reflection or standard mock data ingestion path to verify handleIncomingData structures
        // Since handleIncomingData is private, we pass strings to simulate packet reception routing paths
        java.lang.reflect.Method handleDataMethod = GameClient.class.getDeclaredMethod("handleIncomingData", String.class);
        handleDataMethod.setAccessible(true);

        // 1. Packet parsing block trigger (has type & payload)
        handleDataMethod.invoke(gameClient, validPacketJson);
        assertTrue(listenerTriggered.get());

        // 2. Direct Game State raw JSON block fallback trigger
        listenerTriggered.set(false);
        String plainJson = "{\"status\":\"running\"}";
        handleDataMethod.invoke(gameClient, plainJson);
        assertTrue(listenerTriggered.get());

        // 3. Invalid JSON data structural safety catch branch
        assertDoesNotThrow(() -> handleDataMethod.invoke(gameClient, "{broken-json"));
    }

    @Test
    void testHandleIncomingPacketBranches() throws Exception {
        java.lang.reflect.Method handlePacketMethod = GameClient.class.getDeclaredMethod("handleIncomingPacket", Packet.class);
        handlePacketMethod.setAccessible(true);

        // Branch 1: PONG packet type
        Packet pongPacket = new Packet(PacketType.PONG);
        assertDoesNotThrow(() -> handlePacketMethod.invoke(gameClient, pongPacket));

        // Branch 2: ERROR packet type
        Packet errorPacket = new Packet(PacketType.ERROR, mapper.createObjectNode().put("msg", "fail"));
        assertDoesNotThrow(() -> handlePacketMethod.invoke(gameClient, errorPacket));

        // Branch 3: DEFAULT structural catch fallback
        Packet disconnectPacket = new Packet(PacketType.DISCONNECT);
        assertDoesNotThrow(() -> handlePacketMethod.invoke(gameClient, disconnectPacket));
    }

    @Test
    void testDisconnectAllFromLobbyEmpty() {
        assertDoesNotThrow(GameClient::disconnectAllFromLobby);
    }
}