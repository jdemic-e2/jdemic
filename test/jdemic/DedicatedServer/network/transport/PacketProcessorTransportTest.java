package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This test class focuses specifically on the transport / routing responsibilities
 * of PacketProcessor, not on the deep internal correctness of every gameplay rule.
 *
 * In other words, here we verify that PacketProcessor:
 * - routes packets to the correct handler,
 * - rejects invalid or suspicious transport-level input safely,
 * - respects connection identity,
 * - triggers broadcasting only in the correct scenarios,
 * - and handles lobby/game packet flow in a stable and predictable way.
 *
 * The goal is to increase coverage on the networking side of PacketProcessor,
 * especially on the many guard clauses and early-return branches that are easy
 * to miss in normal gameplay tests.
 */
@ExtendWith(MockitoExtension.class)
class PacketProcessorTransportTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ClientHandler clientHandler;

    private GameManager gameManager;
    private PacketProcessor packetProcessor;

    private PlayerState alice;
    private PlayerState bob;

    @BeforeEach
    void setUp() {
        alice = new PlayerState("ALICE");
        bob = new PlayerState("BOB");

        List<PlayerState> players = new ArrayList<>();
        players.add(alice);
        players.add(bob);

        gameManager = new GameManager(players, false);
        gameManager.getState().setGameStarted(false);
        gameManager.getState().setGameOver(false);
        gameManager.getState().setCurrentPlayerIndex(0);
        gameManager.getState().setActionsRemaining(4);

        packetProcessor = new PacketProcessor(gameManager, clientHandler);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * This helper section keeps packet creation short and readable.
     *
     * Because many tests differ only by packet type or small payload details,
     * helper methods make the tests easier to scan and easier to maintain.
     * They also reduce noise, so the intention of each test is clearer.
     */

    private Packet packet(PacketType type, ObjectNode payload) {
        return new Packet(type, payload);
    }

    private Packet gameDataPacket(ObjectNode payload) {
        return new Packet(PacketType.GAME_DATA, payload);
    }

    private ObjectNode baseGameData(String action, String playerId) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("GameAction", action);
        if (playerId != null) {
            payload.put("PlayerID", playerId);
        }
        return payload;
    }

    private Packet connectPacket(String playerName) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerName", playerName);
        return new Packet(PacketType.CONNECT, payload);
    }

    private Packet chatPacket(String message) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("message", message);
        return new Packet(PacketType.LOBBY_CHAT, payload);
    }

    private Packet readyPacket(boolean ready) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("ready", ready);
        return new Packet(PacketType.LOBBY_READY, payload);
    }

    /**
     * A small helper used in tests that need the game to already be in progress.
     * This avoids repeating the same setup lines in multiple tests.
     */
    private void startGame() {
        gameManager.getState().setGameStarted(true);
        gameManager.getState().setGameOver(false);
    }




    private Packet discardPacket(String playerId, Integer cardIndex) {
        ObjectNode payload = baseGameData("DISCARD_CARD", playerId);
        if (cardIndex != null) {
            payload.put("cardIndex", cardIndex);
        }
        return gameDataPacket(payload);
    }
    // -------------------------------------------------------------------------
    // process() routing guards
    // -------------------------------------------------------------------------

    /**
     * These tests verify the very first defensive layer of PacketProcessor.process(...).
     *
     * Before any gameplay or lobby logic happens, PacketProcessor must safely handle:
     * - null packets,
     * - packets with null types,
     * - basic packet types like PING, PONG, and ERROR.
     *
     * These branches are simple, but important for coverage because they validate
     * that the router itself is stable and does not crash on malformed or minimal input.
     */

    @Test
    void processNullPacketDoesNothing() {
        assertDoesNotThrow(() -> packetProcessor.process(null));
        verifyNoInteractions(clientHandler);
    }

    @Test
    void processPacketWithNullTypeDoesNothing() {
        Packet nullTypePacket = mock(Packet.class);
        when(nullTypePacket.getType()).thenReturn(null);

        assertDoesNotThrow(() -> packetProcessor.process(nullTypePacket));
        verifyNoInteractions(clientHandler);
    }

    @Test
    void processPingDoesNotThrow() {
        assertDoesNotThrow(() -> packetProcessor.process(new Packet(PacketType.PING)));
        verifyNoInteractions(clientHandler);
    }

    @Test
    void processPongDoesNotThrow() {
        assertDoesNotThrow(() -> packetProcessor.process(new Packet(PacketType.PONG)));
        verifyNoInteractions(clientHandler);
    }

    @Test
    void processErrorDoesNotThrow() {
        assertDoesNotThrow(() -> packetProcessor.process(new Packet(PacketType.ERROR)));
        verifyNoInteractions(clientHandler);
    }

    // -------------------------------------------------------------------------
    // GAME_DATA transport/guard branches
    // -------------------------------------------------------------------------

    /**
     * This section targets the most important transport-level branches inside
     * handleGameData(...).
     *
     * PacketProcessor performs many checks before it allows gameplay mutations:
     * - whether the sender identity is valid,
     * - whether the player exists,
     * - whether the game has started,
     * - whether it is actually that player's turn,
     * - whether the game is already over,
     * - and whether the requested action can even be built from the payload.
     *
     * These tests are valuable because they verify that bad or suspicious packets
     * are dropped early and safely, which is exactly what strong transport handling
     * should do.
     */


// -------------------------------------------------------------------------
// END_TURN and DISCARD_CARD branches
// -------------------------------------------------------------------------

    /**
     * These tests cover the dedicated GAME_DATA branches that do not go through
     * buildAction(...), namely END_TURN and DISCARD_CARD.
     *
     * They are important because PacketProcessor treats them specially:
     * - END_TURN manually forces actionsRemaining to 0 and advances the turn,
     * - DISCARD_CARD validates cardIndex and delegates to GameManager discard logic.
     *
     * Because these branches bypass the generic action-construction path,
     * they need their own focused tests to ensure they are not missed by coverage.
     */

    @Test
    void endTurnRejectedWhenSenderNotRegistered() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn(null);

        ObjectNode payload = baseGameData("END_TURN", null);
        packetProcessor.process(gameDataPacket(payload));

        assertEquals(4, gameManager.getState().getActionsRemaining());
        assertEquals("ALICE", gameManager.getCurrentPlayer().getPlayerName());
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void endTurnRejectedWhenNotPlayersTurn() {
        startGame();
        gameManager.getState().setCurrentPlayerIndex(0); // ALICE turn
        when(clientHandler.getConnectedPlayerName()).thenReturn("BOB");

        ObjectNode payload = baseGameData("END_TURN", "BOB");
        packetProcessor.process(gameDataPacket(payload));

        assertEquals(4, gameManager.getState().getActionsRemaining());
        assertEquals("ALICE", gameManager.getCurrentPlayer().getPlayerName());
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void endTurnValidAdvancesTurnAndBroadcasts() {
        startGame();
        gameManager.getState().setCurrentPlayerIndex(0); // ALICE turn
        gameManager.getState().setActionsRemaining(3);
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("END_TURN", "ALICE");
        packetProcessor.process(gameDataPacket(payload));

        assertEquals("BOB", gameManager.getCurrentPlayer().getPlayerName());
        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void discardCardRejectedWhenCardIndexMissing() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        Packet packet = discardPacket("ALICE", null);
        packetProcessor.process(packet);

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void discardCardRejectedWhenCardIndexOutOfBounds() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        alice.setIsDiscarding(true);
        alice.getHand().clear();
        alice.addCard(new Card("Only Card", CardType.EVENT, null));

        Packet packet = discardPacket("ALICE", 99);
        packetProcessor.process(packet);

        assertEquals(1, alice.getHand().size());
        assertTrue(alice.getIsDiscarding());
        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void discardCardValidRemovesCardWhenPlayerIsDiscarding() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        alice.getHand().clear();
        for (int i = 0; i < 8; i++) {
            alice.addCard(new Card("Card " + i, CardType.EVENT, null));
        }
        alice.setIsDiscarding(true);
        gameManager.getState().setCurrentPlayerIndex(0); // ALICE turn

        Packet packet = discardPacket("ALICE", 0);
        packetProcessor.process(packet);

        assertEquals(7, alice.getHand().size());
        assertFalse(alice.getIsDiscarding());
        verify(clientHandler).broadcastGameStateToAll();
    }


    @Test
    void testActionBroadcastsImmediately() {
        ObjectNode payload = baseGameData("TEST_ACTION", "ALICE");
        payload.put("message", "hello");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void gameDataRejectedWhenPlayerIdSpoofed() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn("BOB");

        ObjectNode payload = baseGameData("BUILD_RESEARCH_STATION", "ALICE");
        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
        assertEquals(4, gameManager.getState().getActionsRemaining());
    }

    @Test
    void gameDataRejectedWhenPlayerNotFound() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn("GHOST");

        ObjectNode payload = baseGameData("BUILD_RESEARCH_STATION", "GHOST");
        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void gameDataRejectedWhenGameNotStarted() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("BUILD_RESEARCH_STATION", "ALICE");
        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
        assertFalse(gameManager.getState().isGameStarted());
    }

    @Test
    void gameDataRejectedWhenNotPlayersTurn() {
        startGame();
        gameManager.getState().setCurrentPlayerIndex(0); // ALICE turn
        when(clientHandler.getConnectedPlayerName()).thenReturn("BOB");

        ObjectNode payload = baseGameData("BUILD_RESEARCH_STATION", "BOB");
        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
        assertEquals(4, gameManager.getState().getActionsRemaining());
    }

    @Test
    void gameDataRejectedWhenGameOver() {
        startGame();
        gameManager.getState().setGameOver(true);
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("BUILD_RESEARCH_STATION", "ALICE");
        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void gameDataRejectedWhenActionCannotBeBuilt() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("UNKNOWN_ACTION", "ALICE");
        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
        assertEquals(4, gameManager.getState().getActionsRemaining());
    }

    /**
     * Threat preview is a special transport branch because it does not behave
     * like a normal gameplay mutation. Instead of mutating the full game state,
     * it conditionally sends a preview packet back to the requesting client.
     *
     * These tests verify both:
     * - rejection when the player has no valid THREAT event card,
     * - and successful response when the card exists.
     */

    @Test
    void threatPreviewRejectedWithoutThreatCard() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("THREAT_PREVIEW", "ALICE");
        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).sendPacketToClient(any());
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void threatPreviewSendsPreviewPacketWhenThreatCardExists() {
        startGame();
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        Card threatCard = new Card("Threat Scan", CardType.EVENT, null);
        threatCard.setEventType(Card.EventType.THREAT);
        alice.getHand().add(threatCard);

        ObjectNode payload = baseGameData("THREAT_PREVIEW", "ALICE");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
        verify(clientHandler).sendPacketToClient(captor.capture());

        Packet sent = captor.getValue();
        assertNotNull(sent);
        assertEquals(PacketType.GAME_DATA, sent.getType());
        assertEquals("THREAT_PREVIEW", sent.getPayload().get("GameAction").asText());
        assertTrue(sent.getPayload().has("topInfectionCards"));
    }


    // -------------------------------------------------------------------------
    // LOBBY_CHAT
    // -------------------------------------------------------------------------

    /**
     * Lobby chat is transport logic, not gameplay logic, so it deserves separate coverage.
     *
     * Here we verify that PacketProcessor:
     * - ignores empty chat messages,
     * - rejects messages from unregistered clients,
     * - truncates oversized messages safely,
     * - and broadcasts only when a valid chat message was accepted.
     *
     * This is useful both for correctness and for security-related confidence,
     * because the server must decide who is allowed to speak and what payload is acceptable.
     */


    @Test
    void lobbyChatIgnoredWhenMessageEmpty() {


        packetProcessor.process(chatPacket("   "));

        assertTrue(gameManager.getState().getLobbyChatMessages().isEmpty());
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void lobbyChatRejectedWhenClientNotRegistered() {
        when(clientHandler.getConnectedPlayerName()).thenReturn(null);

        packetProcessor.process(chatPacket("hello"));

        assertTrue(gameManager.getState().getLobbyChatMessages().isEmpty());
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void lobbyChatTruncatesLongMessageAndBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        String longMessage = "x".repeat(350);
        packetProcessor.process(chatPacket(longMessage));

        assertEquals(1, gameManager.getState().getLobbyChatMessages().size());
        String stored = gameManager.getState().getLobbyChatMessages().get(0).getMessage();
        assertEquals(300, stored.length());
        verify(clientHandler).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // LOBBY_READY
    // -------------------------------------------------------------------------

    /**
     * The ready-state flow is another good source of transport coverage because it
     * includes several state-dependent branches:
     * - reject if the client is not registered,
     * - reject if the game already started,
     * - reject if the player is missing,
     * - accept and broadcast when the update is valid.
     *
     * These tests verify that PacketProcessor protects the lobby state correctly
     * before the game begins.
     */
// -------------------------------------------------------------------------
// LOBBY_READY countdown behavior
// -------------------------------------------------------------------------

    /**
     * These tests focus specifically on the countdown behavior triggered by lobby
     * ready-state updates.
     *
     * PacketProcessor calls updateLobbyCountdown() after valid ready changes, so
     * this section verifies that:
     * - when all players become ready, countdown starts,
     * - when readiness becomes invalid again, countdown is cancelled.
     *
     * This is useful because it covers transport-triggered state transitions in the lobby
     * without needing to wait for the full scheduled game start.
     */

    @Test
    void lobbyReadyStartsCountdownWhenAllPlayersBecomeReady() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        bob.setReady(true);

        packetProcessor.process(readyPacket(true));

        assertTrue(alice.isReady());
        assertTrue(gameManager.getState().getLobbyCountdownStartedAt() > 0);
        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void lobbyReadyCancelsCountdownWhenAPlayerBecomesUnready() {
        alice.setReady(true);
        bob.setReady(true);
        gameManager.getState().setLobbyCountdownStartedAt(System.currentTimeMillis());

        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        packetProcessor.process(readyPacket(false));

        assertFalse(alice.isReady());
        assertEquals(0, gameManager.getState().getLobbyCountdownStartedAt());
        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void lobbyReadyRejectedWhenClientNotRegistered() {
        when(clientHandler.getConnectedPlayerName()).thenReturn(null);

        packetProcessor.process(readyPacket(true));

        assertFalse(alice.isReady());
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void lobbyReadyRejectedWhenGameAlreadyStarted() {
        gameManager.getState().setGameStarted(true);
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        packetProcessor.process(readyPacket(true));

        assertFalse(alice.isReady());
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void lobbyReadyRejectedWhenPlayerMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("GHOST");

        packetProcessor.process(readyPacket(true));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void lobbyReadyUpdatesPlayerAndBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        packetProcessor.process(readyPacket(true));

        assertTrue(alice.isReady());
        verify(clientHandler).broadcastGameStateToAll();
    }

    

    // -------------------------------------------------------------------------
    // CONNECT edge branches
    // -------------------------------------------------------------------------

    /**
     * Although CONNECT may already be partially covered elsewhere, these tests
     * focus on the specific edge branches that are easy to miss:
     * - blank names,
     * - full lobby,
     * - and attempts to connect after the game already started.
     *
     * These are transport-level validation checks and are important for ensuring
     * that PacketProcessor does not admit invalid session state.
     */

    @Test
    void connectRejectedWhenNameBlank() {
        Packet blankConnect = connectPacket("   ");

        packetProcessor.process(blankConnect);

        assertEquals(2, gameManager.getState().getPlayers().size());
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void connectRejectedWhenLobbyFull() {
        gameManager.getState().addPlayer(new PlayerState("C1"));
        gameManager.getState().addPlayer(new PlayerState("C2"));

        packetProcessor.process(connectPacket("C3"));

        assertEquals(4, gameManager.getState().getPlayers().size());
        assertNull(findPlayer("C3"));
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void connectRejectedWhenGameAlreadyStarted() {
        gameManager.getState().setGameStarted(true);

        packetProcessor.process(connectPacket("CAROL"));

        assertNull(findPlayer("CAROL"));
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // DISCONNECT
    // -------------------------------------------------------------------------

    /**
     * Disconnect handling is transport-critical because it affects both session
     * state and game state consistency.
     *
     * These tests verify:
     * - safe no-op behavior when no player identity is associated with the client,
     * - valid removal and cleanup when a known player disconnects,
     * - and the full DISCONNECT packet path, including closeConnection().
     *
     * This helps cover the branches where PacketProcessor coordinates session
     * cleanup with ClientHandler.
     */

    @Test
    void disconnectCurrentPlayerNoOpWhenNameMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn(null);

        packetProcessor.disconnectCurrentPlayer();

        assertNotNull(findPlayer("ALICE"));
        assertNotNull(findPlayer("BOB"));
        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void disconnectCurrentPlayerRemovesPlayerAndBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        packetProcessor.disconnectCurrentPlayer();

        assertNull(findPlayer("ALICE"));
        assertNotNull(findPlayer("BOB"));
        verify(clientHandler).clearConnectedPlayerName();
        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void handleDisconnectAlsoClosesConnection() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        packetProcessor.process(new Packet(PacketType.DISCONNECT));

        assertNull(findPlayer("ALICE"));
        verify(clientHandler).clearConnectedPlayerName();
        verify(clientHandler).broadcastGameStateToAll();
        verify(clientHandler).closeConnection();
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Small utility lookup used by multiple tests to inspect the current game state
     * without repeating the same loop logic in every method.
     */
    private PlayerState findPlayer(String name) {
        for (PlayerState ps : gameManager.getState().getPlayers()) {
            if (ps.getPlayerName().equals(name)) {
                return ps;
            }
        }
        return null;
    }
}