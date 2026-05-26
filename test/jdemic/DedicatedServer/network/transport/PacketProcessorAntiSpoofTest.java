package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PacketProcessorAntiSpoofTest {

    private PacketProcessor packetProcessor;
    private GameManager gameManager;
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ClientHandler mockClientHandler;

    @BeforeEach
    public void setUp() {
        gameManager = new GameManager(new ArrayList<>());
        PlayerState alice = new PlayerState("ALICE");
        PlayerState bob = new PlayerState("BOB");
        gameManager.getState().addPlayer(alice);
        gameManager.getState().addPlayer(bob);
        CityNode atlanta = gameManager.getState().getMap().getCity("Atlanta");
        alice.setCurrentCity(atlanta);
        bob.setCurrentCity(atlanta);

        // Alice's turn with 4 actions remaining
        gameManager.getState().setCurrentPlayerIndex(0);
        gameManager.getState().setActionsRemaining(4);
        gameManager.getState().setGameStarted(true);

        packetProcessor = new PacketProcessor(gameManager, mockClientHandler);
    }

    private ObjectNode driveFerryPayload() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("GameAction", "DRIVE_FERRY");
        payload.put("destination", "Chicago");
        return payload;
    }

    /**
     * Bob connects and sends a GAME_DATA packet claiming to be Alice.
     * The server must reject it: no state mutation, no broadcast.
     */
    @Test
    public void testRejectSpoofedIdentity() {
        // This connection belongs to BOB
        when(mockClientHandler.getConnectedPlayerName()).thenReturn("BOB");

        // Bob's packet falsely declares PlayerID = "ALICE"
        ObjectNode payload = driveFerryPayload();
        payload.put("PlayerID", "ALICE");

        Packet packet = new Packet(PacketType.GAME_DATA, payload);
        packetProcessor.process(packet);

        // If the spoof had succeeded the action counter would have dropped to 3.
        assertEquals(4, gameManager.getState().getActionsRemaining(),
                "Security failure: server accepted a spoofed PlayerID.");

        // No state broadcast should occur on a rejected packet.
        verify(mockClientHandler, never()).broadcastGameStateToAll();
    }

    /**
     * Alice connects and sends a GAME_DATA packet with her own PlayerID.
     * The server must accept it and decrement the action counter.
     */
    @Test
    public void testLegitimateActionAccepted() {
        // This connection belongs to ALICE
        when(mockClientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        // Alice sends her own PlayerID — no spoofing
        ObjectNode payload = driveFerryPayload();
        payload.put("PlayerID", "ALICE");

        Packet packet = new Packet(PacketType.GAME_DATA, payload);
        packetProcessor.process(packet);

        // Action counter must have been consumed (4 → 3)
        assertEquals(3, gameManager.getState().getActionsRemaining(),
                "Legitimate action was incorrectly rejected.");

        // A broadcast must have been sent after a valid action
        verify(mockClientHandler, atLeastOnce()).broadcastGameStateToAll();
    }

    /**
     * A client that never sent a CONNECT packet (name is null) must be
     * silently rejected even without a PlayerID field.
     */
    @Test
    public void testRejectUnregisteredClient() {
        // No CONNECT was received; name is null
        when(mockClientHandler.getConnectedPlayerName()).thenReturn(null);

        ObjectNode payload = driveFerryPayload();

        Packet packet = new Packet(PacketType.GAME_DATA, payload);
        packetProcessor.process(packet);

        // State must be unchanged
        assertEquals(4, gameManager.getState().getActionsRemaining(),
                "Unregistered client should not be able to perform actions.");

        verify(mockClientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    public void testRejectUnregisteredClientWithClaimedPlayerId() {
        when(mockClientHandler.getConnectedPlayerName()).thenReturn(null);

        ObjectNode payload = driveFerryPayload();
        payload.put("PlayerID", "ALICE");

        packetProcessor.process(new Packet(PacketType.GAME_DATA, payload));

        assertEquals(4, gameManager.getState().getActionsRemaining(),
                "Unregistered clients must not be able to authorize actions with payload PlayerID.");
        verify(mockClientHandler, never()).broadcastGameStateToAll();
    }

    /**
     * A packet with a blank PlayerID that does not match the connected
     * client's name must also be rejected.
     */
    @Test
    public void testRejectBlankPlayerIdMismatch() {
        when(mockClientHandler.getConnectedPlayerName()).thenReturn("BOB");

        // PlayerID present but empty — not equal to "BOB"
        ObjectNode payload = driveFerryPayload();
        payload.put("PlayerID", "");

        Packet packet = new Packet(PacketType.GAME_DATA, payload);
        packetProcessor.process(packet);

        assertEquals(4, gameManager.getState().getActionsRemaining(),
                "Packet with mismatched blank PlayerID should be rejected.");

        verify(mockClientHandler, never()).broadcastGameStateToAll();
    }

    /**
     * When the payload omits PlayerID entirely the server must fall back to
     * the connection's registered name (no rejection).
     */
    @Test
    public void testAcceptWhenPlayerIdOmitted() {
        when(mockClientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        // No PlayerID field at all — server should default to connected name
        ObjectNode payload = driveFerryPayload();

        Packet packet = new Packet(PacketType.GAME_DATA, payload);
        packetProcessor.process(packet);

        assertEquals(3, gameManager.getState().getActionsRemaining(),
                "Omitting PlayerID should default to the connection identity and succeed.");

        verify(mockClientHandler, atLeastOnce()).broadcastGameStateToAll();
    }

    @Test
    public void testRejectActionBeforeGameStarts() {
        gameManager.getState().setGameStarted(false);
        when(mockClientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = driveFerryPayload();
        payload.put("PlayerID", "ALICE");

        packetProcessor.process(new Packet(PacketType.GAME_DATA, payload));

        assertEquals(4, gameManager.getState().getActionsRemaining());
        verify(mockClientHandler, never()).broadcastGameStateToAll();
    }
}
