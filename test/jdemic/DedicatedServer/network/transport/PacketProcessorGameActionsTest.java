package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests for game action routing through PacketProcessor.buildAction().
 *
 * Each action type (DRIVE_FERRY, DIRECT_FLIGHT, etc.) has its own validation
 * path inside PacketProcessor. These tests verify that valid payloads are
 * accepted and that missing or invalid fields are rejected safely without
 * crashing the server.
 */
@ExtendWith(MockitoExtension.class)
class PacketProcessorGameActionsTest {

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
        gameManager.getState().setGameStarted(true);
        gameManager.getState().setGameOver(false);
        gameManager.getState().setCurrentPlayerIndex(0);
        gameManager.getState().setActionsRemaining(4);

        packetProcessor = new PacketProcessor(gameManager, clientHandler);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    private void setAliceOnCity(String cityName) {
        CityNode city = gameManager.getState().getMap().getCity(cityName);
        if (city != null) {
            alice.setCurrentCity(city);
        }
    }

    private Card addCityCardToAlice(String cityName) {
        CityNode city = gameManager.getState().getMap().getCity(cityName);
        Card card = new Card(cityName, CardType.CITY, city);
        alice.addCard(card);
        return card;
    }

    private Card addEventCardToAlice(String cardName, Card.EventType eventType) {
        Card card = new Card(cardName, CardType.EVENT, null);
        card.setEventType(eventType);
        alice.addCard(card);
        return card;
    }

    // -------------------------------------------------------------------------
    // DRIVE_FERRY — requires: destination (adjacent city)
    // -------------------------------------------------------------------------

    @Test
    void driveFerryValidDestinationBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        setAliceOnCity("Atlanta");

        ObjectNode payload = baseGameData("DRIVE_FERRY", "ALICE");
        payload.put("destination", "Chicago");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void driveFerryRejectedWhenDestinationMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("DRIVE_FERRY", "ALICE");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void driveFerryRejectedWhenDestinationUnknown() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("DRIVE_FERRY", "ALICE");
        payload.put("destination", "Atlantis");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // SHUTTLE_FLIGHT — requires: destination
    // -------------------------------------------------------------------------

    @Test
    void shuttleFlightValidBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        setAliceOnCity("Atlanta"); // has research station by default
        gameManager.getState().getMap().getCity("Paris").addResearchStation();

        ObjectNode payload = baseGameData("SHUTTLE_FLIGHT", "ALICE");
        payload.put("destination", "Paris");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void shuttleFlightRejectedWhenDestinationMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("SHUTTLE_FLIGHT", "ALICE");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void shuttleFlightRejectedWhenDestinationUnknown() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("SHUTTLE_FLIGHT", "ALICE");
        payload.put("destination", "Narnia");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // TREAT_DISEASE — requires: color
    // -------------------------------------------------------------------------

    @Test
    void treatDiseaseValidColorBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        setAliceOnCity("Atlanta");
        // Add disease cubes to Atlanta to make it a fully valid action execution
        CityNode atlanta = gameManager.getState().getMap().getCity("Atlanta");
        gameManager.getState().getDiseaseManager().addInfectionCubes(atlanta, 1);

        ObjectNode payload = baseGameData("TREAT_DISEASE", "ALICE");
        payload.put("color", "BLUE");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void treatDiseaseRejectedWhenColorMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("TREAT_DISEASE", "ALICE");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void treatDiseaseRejectedWhenColorInvalid() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("TREAT_DISEASE", "ALICE");
        payload.put("color", "PURPLE");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void treatDiseaseAcceptsCaseInsensitiveColor() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        setAliceOnCity("Atlanta");
        CityNode atlanta = gameManager.getState().getMap().getCity("Atlanta");
        gameManager.getState().getDiseaseManager().addInfectionCubes(atlanta, 1);

        ObjectNode payload = baseGameData("TREAT_DISEASE", "ALICE");
        payload.put("color", "blue");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // DIRECT_FLIGHT — requires: destination + cardIndex
    // -------------------------------------------------------------------------

    @Test
    void directFlightValidWithCardAndDestinationBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        setAliceOnCity("Atlanta");
        addCityCardToAlice("Paris");

        ObjectNode payload = baseGameData("DIRECT_FLIGHT", "ALICE");
        payload.put("destination", "Paris");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void directFlightRejectedWhenDestinationMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Paris");

        ObjectNode payload = baseGameData("DIRECT_FLIGHT", "ALICE");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void directFlightRejectedWhenCardIndexMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("DIRECT_FLIGHT", "ALICE");
        payload.put("destination", "Paris");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void directFlightRejectedWhenCardIndexOutOfBounds() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Paris");

        ObjectNode payload = baseGameData("DIRECT_FLIGHT", "ALICE");
        payload.put("destination", "Paris");
        payload.put("cardIndex", 99);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // CHARTER_FLIGHT — requires: destination + cardIndex
    // -------------------------------------------------------------------------

    @Test
    void charterFlightValidBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        setAliceOnCity("Atlanta");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("CHARTER_FLIGHT", "ALICE");
        payload.put("destination", "Paris");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void charterFlightRejectedWhenCardIndexMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("CHARTER_FLIGHT", "ALICE");
        payload.put("destination", "Tokyo");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void charterFlightRejectedWhenDestinationMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("CHARTER_FLIGHT", "ALICE");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // BUILD_RESEARCH_STATION — requires: none
    // -------------------------------------------------------------------------

    @Test
    void buildResearchStationValidBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        setAliceOnCity("Paris"); // Atlanta has one already, Paris does not

        ObjectNode payload = baseGameData("BUILD_RESEARCH_STATION", "ALICE");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // FIREWALL — requires: cardIndex
    // -------------------------------------------------------------------------

    @Test
    void firewallValidBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Firewall Card", Card.EventType.FIREWALL);

        ObjectNode payload = baseGameData("FIREWALL", "ALICE");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void firewallRejectedWhenCardIndexMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("FIREWALL", "ALICE");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void firewallRejectedWhenCardIndexOutOfBounds() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("FIREWALL", "ALICE");
        payload.put("cardIndex", 50);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // SERVER action — requires: cardIndex + destination
    // -------------------------------------------------------------------------

    @Test
    void serverActionValidBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Server Card", Card.EventType.SERVER);

        ObjectNode payload = baseGameData("SERVER", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("destination", "Paris");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void serverActionRejectedWhenCardIndexMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("SERVER", "ALICE");
        payload.put("destination", "Paris");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void serverActionRejectedWhenDestinationMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Paris");

        ObjectNode payload = baseGameData("SERVER", "ALICE");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // SHARE_KNOWLEDGE — requires: targetPlayer + cardIndex + direction
    // -------------------------------------------------------------------------

    @Test
    void shareKnowledgeRejectedWhenTargetPlayerMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("SHARE_KNOWLEDGE", "ALICE");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void shareKnowledgeRejectedWhenTargetPlayerNotFound() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("SHARE_KNOWLEDGE", "ALICE");
        payload.put("targetPlayer", "GHOST");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void shareKnowledgeRejectedWhenCardIndexMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("SHARE_KNOWLEDGE", "ALICE");
        payload.put("targetPlayer", "BOB");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void shareKnowledgeGiveDirectionBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        CityNode atlanta = gameManager.getState().getMap().getCity("Atlanta");
        alice.setCurrentCity(atlanta);
        bob.setCurrentCity(atlanta);
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("SHARE_KNOWLEDGE", "ALICE");
        payload.put("targetPlayer", "BOB");
        payload.put("direction", "give");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void shareKnowledgeTakeDirectionBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        CityNode atlanta = gameManager.getState().getMap().getCity("Atlanta");
        alice.setCurrentCity(atlanta);
        bob.setCurrentCity(atlanta);
        Card card = new Card("Atlanta", CardType.CITY, atlanta);
        bob.addCard(card);

        ObjectNode payload = baseGameData("SHARE_KNOWLEDGE", "ALICE");
        payload.put("targetPlayer", "BOB");
        payload.put("direction", "take");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // DISCOVER_CURE — requires: color + cardIndices array
    // -------------------------------------------------------------------------

    @Test
    void discoverCureValidBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        setAliceOnCity("Atlanta"); // Atlanta has research station by default
        addCityCardToAlice("Atlanta");
        addCityCardToAlice("Chicago");
        addCityCardToAlice("Montreal");
        addCityCardToAlice("New York");
        addCityCardToAlice("Washington");

        ObjectNode payload = baseGameData("DISCOVER_CURE", "ALICE");
        payload.put("color", "BLUE");
        ArrayNode indices = mapper.createArrayNode();
        indices.add(0).add(1).add(2).add(3).add(4);
        payload.set("cardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void discoverCureRejectedWhenColorMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("DISCOVER_CURE", "ALICE");
        ArrayNode indices = mapper.createArrayNode();
        indices.add(0).add(1).add(2).add(3).add(4);
        payload.set("cardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void discoverCureRejectedWhenColorInvalid() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("DISCOVER_CURE", "ALICE");
        payload.put("color", "PURPLE");
        ArrayNode indices = mapper.createArrayNode();
        indices.add(0).add(1).add(2).add(3).add(4);
        payload.set("cardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void discoverCureRejectedWhenCardIndicesArrayMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("DISCOVER_CURE", "ALICE");
        payload.put("color", "BLUE");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void discoverCureRejectedWhenCardIndicesIsNotArray() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("DISCOVER_CURE", "ALICE");
        payload.put("color", "BLUE");
        payload.put("cardIndices", 42);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void discoverCureRejectedWhenCardIndexOutOfBounds() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");
        addCityCardToAlice("Chicago");

        ObjectNode payload = baseGameData("DISCOVER_CURE", "ALICE");
        payload.put("color", "BLUE");
        ArrayNode indices = mapper.createArrayNode();
        indices.add(0).add(1).add(99);
        payload.set("cardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void discoverCureRejectedWhenDuplicateCardIndex() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");
        addCityCardToAlice("Chicago");
        addCityCardToAlice("Montreal");

        ObjectNode payload = baseGameData("DISCOVER_CURE", "ALICE");
        payload.put("color", "BLUE");
        ArrayNode indices = mapper.createArrayNode();
        indices.add(0).add(1).add(0);
        payload.set("cardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void discoverCureRejectedWhenNegativeCardIndex() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("DISCOVER_CURE", "ALICE");
        payload.put("color", "BLUE");
        ArrayNode indices = mapper.createArrayNode();
        indices.add(-1).add(0);
        payload.set("cardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // SATELLITE — requires: cardIndex + destination + targetPlayer
    // -------------------------------------------------------------------------

    @Test
    void satelliteValidBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Satellite Card", Card.EventType.SATELLITE);

        ObjectNode payload = baseGameData("SATELLITE", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("destination", "Paris");
        payload.put("targetPlayer", "BOB");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void satelliteRejectedWhenCardIndexMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("SATELLITE", "ALICE");
        payload.put("destination", "Paris");
        payload.put("targetPlayer", "BOB");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void satelliteRejectedWhenDestinationMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("SATELLITE", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("targetPlayer", "BOB");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void satelliteRejectedWhenTargetPlayerMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("SATELLITE", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("destination", "Paris");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void satelliteRejectedWhenTargetPlayerBlank() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("SATELLITE", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("destination", "Paris");
        payload.put("targetPlayer", "   ");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void satelliteRejectedWhenCardIndexOutOfBounds() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("SATELLITE", "ALICE");
        payload.put("cardIndex", 50);
        payload.put("destination", "Paris");
        payload.put("targetPlayer", "BOB");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void satelliteRejectedWhenDestinationUnknown() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addCityCardToAlice("Atlanta");

        ObjectNode payload = baseGameData("SATELLITE", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("destination", "Atlantis");
        payload.put("targetPlayer", "BOB");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // CONTROL / SYSTEM_CONTROL — requires: cardIndex + infectionDiscardIndex/infectionCardName
    // -------------------------------------------------------------------------

    @Test
    void controlActionValidWithDiscardIndexBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Control Event Card", Card.EventType.CONTROL);

        Card discarded = new Card("Paris", CardType.INFECTION, gameManager.getState().getMap().getCity("Paris"));
        gameManager.getState().getCardDeck().getInfectionDiscardPile().add(discarded);

        ObjectNode payload = baseGameData("CONTROL", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("infectionDiscardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void controlActionValidWithCardNameBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Control Event Card", Card.EventType.CONTROL);

        Card discarded = new Card("Paris", CardType.INFECTION, gameManager.getState().getMap().getCity("Paris"));
        gameManager.getState().getCardDeck().getInfectionDiscardPile().add(discarded);

        ObjectNode payload = baseGameData("SYSTEM_CONTROL", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("infectionCardName", "Paris");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void controlActionRejectedWhenCardIndexMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");

        ObjectNode payload = baseGameData("CONTROL", "ALICE");
        payload.put("infectionDiscardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void controlActionRejectedWhenDiscardIndexOutOfBounds() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Control Event Card", Card.EventType.CONTROL);

        ObjectNode payload = baseGameData("CONTROL", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("infectionDiscardIndex", 99);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void controlActionRejectedWhenCardNameNotFound() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Control Event Card", Card.EventType.CONTROL);

        ObjectNode payload = baseGameData("CONTROL", "ALICE");
        payload.put("cardIndex", 0);
        payload.put("infectionCardName", "Atlantis");

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    // -------------------------------------------------------------------------
    // THREAT — requires: cardIndex + infectionCardIndices array
    // -------------------------------------------------------------------------

    @Test
    void threatActionValidBroadcasts() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Threat Scan Card", Card.EventType.THREAT);

        // Populate top infection cards
        for (int i = 0; i < 6; i++) {
            Card c = new Card("Infect City " + i, CardType.INFECTION, null);
            gameManager.getState().getCardDeck().getInfectionCards().add(c);
        }

        ObjectNode payload = baseGameData("THREAT", "ALICE");
        payload.put("cardIndex", 0);
        ArrayNode indices = mapper.createArrayNode();
        indices.add(0).add(1).add(2).add(3).add(4).add(5);
        payload.set("infectionCardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler).broadcastGameStateToAll();
    }

    @Test
    void threatActionRejectedWhenIndicesMissing() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Threat Scan Card", Card.EventType.THREAT);

        ObjectNode payload = baseGameData("THREAT", "ALICE");
        payload.put("cardIndex", 0);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void threatActionRejectedWhenIndexOutOfBounds() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Threat Scan Card", Card.EventType.THREAT);

        for (int i = 0; i < 2; i++) {
            Card c = new Card("Infect City " + i, CardType.INFECTION, null);
            gameManager.getState().getCardDeck().getInfectionCards().add(c);
        }

        ObjectNode payload = baseGameData("THREAT", "ALICE");
        payload.put("cardIndex", 0);
        ArrayNode indices = mapper.createArrayNode();
        indices.add(0).add(99);
        payload.set("infectionCardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }

    @Test
    void threatActionRejectedWhenDuplicateIndex() {
        when(clientHandler.getConnectedPlayerName()).thenReturn("ALICE");
        addEventCardToAlice("Threat Scan Card", Card.EventType.THREAT);

        for (int i = 0; i < 2; i++) {
            Card c = new Card("Infect City " + i, CardType.INFECTION, null);
            gameManager.getState().getCardDeck().getInfectionCards().add(c);
        }

        ObjectNode payload = baseGameData("THREAT", "ALICE");
        payload.put("cardIndex", 0);
        ArrayNode indices = mapper.createArrayNode();
        indices.add(0).add(0);
        payload.set("infectionCardIndices", indices);

        packetProcessor.process(gameDataPacket(payload));

        verify(clientHandler, never()).broadcastGameStateToAll();
    }
}
