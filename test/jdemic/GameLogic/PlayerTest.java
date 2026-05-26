package jdemic.GameLogic;

import com.fasterxml.jackson.databind.JsonNode;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.GameLogic.Actions.*;
import jdemic.GameLogic.Actions.Movement.*;
import jdemic.GameLogic.Actions.Other.*;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerTest {

    private Player player;
    private PlayerState mockState;
    private GameClient mockGameClient;
    private Card mockCard;

    @BeforeEach
    void setUp() {
        mockState = mock(PlayerState.class);
        mockGameClient = mock(GameClient.class);
        when(mockState.getPlayerName()).thenReturn("reghin_77");

        mockCard = mock(Card.class, RETURNS_DEEP_STUBS);

        player = new Player(mockState, mockGameClient);
    }

    private JsonNode capturePayloadFromClient() {
        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(mockGameClient, atLeastOnce()).sendPacket(packetCaptor.capture());
        return packetCaptor.getValue().getPayload();
    }

    @Test
    void testStateGettersAndSetters() {
        assertEquals(mockState, player.getState());
        PlayerState newState = mock(PlayerState.class);
        player.updateState(newState);
        assertEquals(newState, player.getState());
    }

    @Test
    void testDriveFerryActionPayload() {
        DriveFerryAction action = mock(DriveFerryAction.class, RETURNS_DEEP_STUBS);
        when(action.getDestination().getName()).thenReturn("Atlanta");

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("DRIVE_FERRY", payload.get("GameAction").asText());
        assertEquals("Atlanta", payload.get("destination").asText());
    }

    @Test
    void testDirectFlightActionPayload() {
        DirectFlightAction action = mock(DirectFlightAction.class, RETURNS_DEEP_STUBS);
        when(action.getDestination().getName()).thenReturn("Paris");
        
        List<Card> hand = new ArrayList<>();
        hand.add(mockCard);
        when(mockState.getHand()).thenReturn(hand);
        when(action.getCardToDiscard()).thenReturn(mockCard);

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("DIRECT_FLIGHT", payload.get("GameAction").asText());
        assertEquals("Paris", payload.get("destination").asText());
        assertEquals(0, payload.get("cardIndex").asInt());
    }

    @Test
    void testCharterFlightActionPayload() {
        CharterFlightAction action = mock(CharterFlightAction.class, RETURNS_DEEP_STUBS);
        when(action.getDestination().getName()).thenReturn("Cairo");
        when(action.getCardToDiscard()).thenReturn(null);

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("CHARTER_FLIGHT", payload.get("GameAction").asText());
        assertEquals(-1, payload.get("cardIndex").asInt());
    }

    @Test
    void testShuttleFlightActionPayload() {
        ShuttleFlightAction action = mock(ShuttleFlightAction.class, RETURNS_DEEP_STUBS);
        when(action.getDestination().getName()).thenReturn("Chicago");

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("SHUTTLE_FLIGHT", payload.get("GameAction").asText());
        assertEquals("Chicago", payload.get("destination").asText());
    }

    @Test
    void testTreatDiseasePayload() {
        TreatDisease action = mock(TreatDisease.class, RETURNS_DEEP_STUBS);
        // Using Mockito to dynamically mock whatever actual Enum type getTargetDisease returns
        when(action.getTargetDisease().name()).thenReturn("RED");

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("TREAT_DISEASE", payload.get("GameAction").asText());
        assertEquals("RED", payload.get("color").asText());
    }

    @Test
    void testShareKnowledgeGivePayload() {
        ShareKnowledge action = mock(ShareKnowledge.class, RETURNS_DEEP_STUBS);
        PlayerState mockReceiver = mock(PlayerState.class);
        
        when(action.getGiver()).thenReturn(mockState); 
        when(action.getReceiver()).thenReturn(mockReceiver);
        when(mockReceiver.getPlayerName()).thenReturn("bob");
        
        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("give", payload.get("direction").asText());
        assertEquals("bob", payload.get("targetPlayer").asText());
    }

    @Test
    void testShareKnowledgeTakePayload() {
        ShareKnowledge action = mock(ShareKnowledge.class, RETURNS_DEEP_STUBS);
        PlayerState mockGiver = mock(PlayerState.class);
        
        when(action.getGiver()).thenReturn(mockGiver); 
        when(action.getReceiver()).thenReturn(mockState);
        when(mockGiver.getPlayerName()).thenReturn("alice");
        
        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("take", payload.get("direction").asText());
        assertEquals("alice", payload.get("targetPlayer").asText());
    }

    @Test
    void testDiscoverCurePayload() {
        DiscoverCure action = mock(DiscoverCure.class, RETURNS_DEEP_STUBS);
        when(action.getTargetColor().name()).thenReturn("BLUE");
        
        List<Card> discards = new ArrayList<>();
        discards.add(mockCard);
        when(action.getCardsToDiscard()).thenReturn(discards);

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("DISCOVER_CURE", payload.get("GameAction").asText());
        assertEquals("BLUE", payload.get("color").asText());
        assertTrue(payload.has("cardIndices"));
    }

    @Test
    void testBuildResearchStationPayload() {
        BuildResearchStation action = mock(BuildResearchStation.class);
        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("BUILD_RESEARCH_STATION", payload.get("GameAction").asText());
    }

    @Test
    void testFirewallActionPayload() {
        FirewallAction action = mock(FirewallAction.class, RETURNS_DEEP_STUBS);
        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("FIREWALL", payload.get("GameAction").asText());
    }

    @Test
    void testSatelliteActionPayload() {
        SatelliteAction action = mock(SatelliteAction.class, RETURNS_DEEP_STUBS);
        when(action.getTargetPawn()).thenReturn("Pawn1");
        when(action.getDestination().getName()).thenReturn("London");

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("SATELLITE", payload.get("GameAction").asText());
        assertEquals("Pawn1", payload.get("targetPlayer").asText());
    }

    @Test
    void testServerActionPayload() {
        ServerAction action = mock(ServerAction.class, RETURNS_DEEP_STUBS);
        when(action.getDestinationCity().getName()).thenReturn("Tokyo");

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("SERVER", payload.get("GameAction").asText());
    }

    @Test
    void testSystemControlActionPayload() {
        SystemControlAction action = mock(SystemControlAction.class, RETURNS_DEEP_STUBS);
        when(action.getInfectionCardToRemove().getCardName()).thenReturn("InfectionCard");

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("SYSTEM_CONTROL", payload.get("GameAction").asText());
    }

    @Test
    void testThreatActionPayload() {
        ThreatAction action = mock(ThreatAction.class, RETURNS_DEEP_STUBS);
        List<Card> reordered = new ArrayList<>();
        reordered.add(mockCard);
        // We cast to raw list here if signature uses a generic wildcard that Mockito stubs roughly
        when((List) action.getRearrangedCards()).thenReturn(reordered);

        player.executeAction(action);

        JsonNode payload = capturePayloadFromClient();
        assertEquals("THREAT", payload.get("GameAction").asText());
        assertTrue(payload.has("infectionCardIndices"));
    }

    @Test
    void testSendWithMissingGameClient() {
        Player headlessPlayer = new Player(mockState, null);
        assertDoesNotThrow(() -> headlessPlayer.executeAction(mock(DriveFerryAction.class, RETURNS_DEEP_STUBS)));
        assertDoesNotThrow(headlessPlayer::sendTestPacket);
        assertDoesNotThrow(headlessPlayer::requestEndTurn);
        assertDoesNotThrow(() -> headlessPlayer.useCard(mockCard));
    }

    @Test
    void testUtilityRequestMethods() {
        player.sendTestPacket();
        player.requestDrawCards();
        player.requestInfectionPhase();

        verify(mockGameClient, times(3)).sendPacket(any(Packet.class));
    }

    @Test
    void testUseCardBranches() {
        Card eventCard = mock(Card.class, RETURNS_DEEP_STUBS);
        when(eventCard.getEventType()).thenReturn(null);
        player.useCard(eventCard);
        
        JsonNode payload1 = capturePayloadFromClient();
        assertEquals("USE_CARD", payload1.get("GameAction").asText());

        Card specialEventCard = mock(Card.class, RETURNS_DEEP_STUBS);
        when(specialEventCard.getEventType().name()).thenReturn("SPECIAL_EVENT");
        player.useCard(specialEventCard);
        
        JsonNode payload2 = capturePayloadFromClient();
        assertEquals("SPECIAL_EVENT", payload2.get("GameAction").asText());
    }
}