package jdemic.DedicatedServer.network.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PacketProcessorConnectTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private GameManager gameManager;
    private PacketProcessor processor;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Build a CONNECT packet with the given playerName payload. */
    private Packet connectPacket(String playerName) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerName", playerName);
        return new Packet(PacketType.CONNECT, payload);
    }

    /** Convenience: count players currently registered in the game state. */
    private int playerCount() {
        return gameManager.getState().getPlayers().size();
    }

    /** Convenience: find a PlayerState by name (null if absent). */
    private PlayerState findPlayer(String name) {
        for (PlayerState ps : gameManager.getState().getPlayers()) {
            if (ps.getPlayerName().equals(name)) return ps;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @BeforeEach
    public void setUp() {
        // Empty lobby, no ClientHandler (broadcast is a no-op)
        gameManager = new GameManager(new ArrayList<>(), false);
        processor   = new PacketProcessor(gameManager, null);
    }

    // =========================================================================
    // Normal CONNECT behaviour (regression guard)
    // =========================================================================

    @Test
    public void testFirstConnectAddsPlayer() {
        processor.process(connectPacket("Alice"));
        assertEquals(1, playerCount());
        assertNotNull(findPlayer("Alice"));
    }

    @Test
    public void testFirstConnectPlayerIsNotReadyByDefault() {
        processor.process(connectPacket("Alice"));
        assertFalse(findPlayer("Alice").isReady());
    }

    @Test
    public void testTwoDistinctPlayersCanConnect() {
        processor.process(connectPacket("Alice"));
        processor.process(connectPacket("Bob"));
        assertEquals(2, playerCount());
        assertNotNull(findPlayer("Alice"));
        assertNotNull(findPlayer("Bob"));
    }

    @Test
    public void testThreeDistinctPlayersCanConnect() {
        processor.process(connectPacket("Alice"));
        processor.process(connectPacket("Bob"));
        processor.process(connectPacket("Carol"));
        assertEquals(3, playerCount());
    }

    // =========================================================================
    // Duplicate name rejection — core requirement
    // =========================================================================

    @Test
    public void testDuplicateConnectDoesNotAddSecondPlayer() {
        processor.process(connectPacket("Alice"));
        processor.process(connectPacket("Alice")); // duplicate
        assertEquals(1, playerCount(),
                "A second CONNECT with the same name must not add a second player");
    }

    @Test
    public void testDuplicateConnectThirdAttemptStillRejected() {
        processor.process(connectPacket("Alice"));
        processor.process(connectPacket("Alice"));
        processor.process(connectPacket("Alice"));
        assertEquals(1, playerCount());
    }

    @Test
    public void testDuplicateConnectDoesNotOverwriteExistingPlayerState() {
        processor.process(connectPacket("Alice"));
        PlayerState original = findPlayer("Alice");

        processor.process(connectPacket("Alice")); // duplicate
        PlayerState afterDuplicate = findPlayer("Alice");

        assertSame(original, afterDuplicate,
                "The original PlayerState must be untouched after a duplicate CONNECT");
    }

    @Test
    public void testDuplicateRejectedWhileOtherPlayersUnaffected() {
        processor.process(connectPacket("Alice"));
        processor.process(connectPacket("Bob"));
        processor.process(connectPacket("Alice")); // duplicate

        assertEquals(2, playerCount());
        assertNotNull(findPlayer("Alice"));
        assertNotNull(findPlayer("Bob"));
    }

    @Test
    public void testDuplicateNameIsCaseSensitive() {
        // "alice" and "Alice" are different names — both should be accepted,
        // because findPlayerState uses equals(), not equalsIgnoreCase().
        processor.process(connectPacket("alice"));
        processor.process(connectPacket("Alice"));
        assertEquals(2, playerCount(),
                "Name comparison is case-sensitive; 'alice' and 'Alice' are different players");
    }

    @Test
    public void testExactDuplicateAfterReadyStateIsStillRejected() {
        processor.process(connectPacket("Alice"));
        // Manually mark Alice as ready to simulate mid-lobby state
        findPlayer("Alice").setReady(true);

        processor.process(connectPacket("Alice")); // duplicate
        assertEquals(1, playerCount());
        // Original ready flag must be preserved
        assertTrue(findPlayer("Alice").isReady(),
                "Duplicate CONNECT must not reset the ready flag of the original player");
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Test
    public void testConnectWithNullPayloadPlayerNameUsesDefault() {
        // Packet with no "playerName" key — processor defaults to "PLAYER"
        ObjectNode emptyPayload = mapper.createObjectNode();
        Packet packet = new Packet(PacketType.CONNECT, emptyPayload);
        processor.process(packet);
        assertEquals(1, playerCount());
        assertNotNull(findPlayer("PLAYER"));
    }

    @Test
    public void testDuplicateDefaultPlayerNameRejected() {
        ObjectNode emptyPayload = mapper.createObjectNode();
        processor.process(new Packet(PacketType.CONNECT, emptyPayload));
        processor.process(new Packet(PacketType.CONNECT, emptyPayload)); // second default "PLAYER"
        assertEquals(1, playerCount());
    }
}