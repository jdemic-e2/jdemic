package jdemic.GameLogic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.JsonNode;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.GameLogic.Actions.CharterFlightAction;
import jdemic.GameLogic.Actions.DirectFlightAction;
import jdemic.GameLogic.Actions.DriveFerryAction;
import jdemic.GameLogic.Actions.ShuttleFlightAction;
import jdemic.GameLogic.Card.EventType;
import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.LobbyChatMessage;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class GameLogicTest {

    // -------------------------------------------------------------------------
    // Shared fixtures — rebuilt before every test
    // -------------------------------------------------------------------------

    private CityNode atlanta;
    private CityNode chicago;
    private CityNode miami;
    private CityNode london;

    private PlayerState playerInAtlanta;
    private PlayerState playerInChicago;

    @BeforeEach
    public void setUp() {
        atlanta = new CityNode("Atlanta", DiseaseColor.BLUE,   0.25f, 0.39f);
        chicago = new CityNode("Chicago", DiseaseColor.BLUE,   0.24f, 0.30f);
        miami   = new CityNode("Miami",   DiseaseColor.YELLOW, 0.26f, 0.45f);
        london  = new CityNode("London",  DiseaseColor.BLUE,   0.48f, 0.25f);

        atlanta.addConnection(chicago);
        chicago.addConnection(miami);

        playerInAtlanta = new PlayerState("Alice");
        playerInAtlanta.setCurrentCity(atlanta);
        playerInChicago = new PlayerState("Bob");
        playerInChicago.setCurrentCity(chicago);
    }

    @AfterEach
    public void tearDown() {
        atlanta = chicago = miami = london = null;
        playerInAtlanta = playerInChicago = null;
    }

    // =========================================================================
    // Helper — build a single-player GameManager with a fresh PlayerState
    // =========================================================================

    private GameManager singlePlayerGame(PlayerState ps) {
        List<PlayerState> players = new ArrayList<>();
        players.add(ps);
        return new GameManager(players);
    }

    @SuppressWarnings("unchecked")
    private void replacePlayerDeck(Deck deck, List<Card> cards) throws Exception {
        Field playerCardsField = Deck.class.getDeclaredField("playerCards");
        playerCardsField.setAccessible(true);
        List<Card> playerCards = (List<Card>) playerCardsField.get(deck);
        playerCards.clear();
        playerCards.addAll(cards);
    }

    private void replaceGameStatePlayers(GameState state, List<PlayerState> players) throws Exception {
        Field playersField = GameState.class.getDeclaredField("playerArray");
        playersField.setAccessible(true);
        playersField.set(state, players);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private SecureSocket newSecureSocket() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey key = keyGenerator.generateKey();
        return new SecureSocket(null, key);
    }

    private void invokeHandleIncomingData(GameClient client, String data) throws Exception {
        Method method = GameClient.class.getDeclaredMethod("handleIncomingData", String.class);
        method.setAccessible(true);
        method.invoke(client, data);
    }

    private void invokeHandleIncomingPacket(GameClient client, Packet packet) throws Exception {
        Method method = GameClient.class.getDeclaredMethod("handleIncomingPacket", Packet.class);
        method.setAccessible(true);
        method.invoke(client, packet);
    }

    // =========================================================================
    // CardType
    // =========================================================================

    @Test
    public void testCardTypeValuesExist() {
        assertEquals(4, CardType.values().length);
        assertNotNull(CardType.valueOf("EVENT"));
        assertNotNull(CardType.valueOf("CITY"));
        assertNotNull(CardType.valueOf("INFECTION"));
        assertNotNull(CardType.valueOf("EPIDEMIC"));
    }

    // =========================================================================
    // Card
    // =========================================================================

    @Test
    public void testCityCardCreation() {
        Card card = new Card("Atlanta", CardType.CITY, atlanta);
        assertEquals("Atlanta", card.getCardName());
        assertEquals(CardType.CITY, card.getType());
        assertEquals(atlanta, card.getTargetCity());
    }

    @Test
    public void testNullTargetCityAllowedForEventCards() {
        Card card = new Card("Firewall Lockdown", CardType.EVENT, null);
        assertNull(card.getTargetCity());
    }

    @Test
    public void testInfectionCardEffectDescriptionContainsCityName() {
        Card card = new Card("Atlanta", CardType.INFECTION, atlanta);
        assertTrue(card.getEffectDescription().contains("Atlanta"));
    }

    @Test
    public void testEpidemicCardEffectDescriptionNotEmpty() {
        Card card = new Card("System Breach", CardType.EPIDEMIC, null);
        String desc = card.getEffectDescription();
        assertNotNull(desc);
        assertFalse(desc.isEmpty());
    }

    @Test
    public void testCityCardEffectDescriptionContainsCityName() {
        Card card = new Card("Chicago", CardType.CITY, chicago);
        assertTrue(card.getEffectDescription().contains("Chicago"));
    }

    @Test
    public void testEventCardSatelliteDescription() {
        Card card = new Card("Satellite Override", CardType.EVENT, null);
        card.setEventType(EventType.SATELLITE);
        assertTrue(card.getEffectDescription().toLowerCase().contains("pawn"));
    }

    @Test
    public void testEventCardServerDescription() {
        Card card = new Card("Server Deployment", CardType.EVENT, null);
        card.setEventType(EventType.SERVER);
        assertTrue(card.getEffectDescription().contains("Research Station"));
    }

    @Test
    public void testEventCardThreatDescription() {
        Card card = new Card("Threat Scan", CardType.EVENT, null);
        card.setEventType(EventType.THREAT);
        assertTrue(card.getEffectDescription().contains("Infection Deck"));
    }

    @Test
    public void testEventCardFirewallDescription() {
        Card card = new Card("Firewall Lockdown", CardType.EVENT, null);
        card.setEventType(EventType.FIREWALL);
        assertTrue(card.getEffectDescription().contains("Infect Cities"));
    }

    @Test
    public void testEventCardControlDescription() {
        Card card = new Card("System Control", CardType.EVENT, null);
        card.setEventType(EventType.CONTROL);
        assertTrue(card.getEffectDescription().contains("Infection Discard Pile"));
    }

    @Test
    public void testEventTypeEnumValues() {
        assertEquals(5, EventType.values().length);
        assertNotNull(EventType.valueOf("FIREWALL"));
        assertNotNull(EventType.valueOf("SATELLITE"));
        assertNotNull(EventType.valueOf("SERVER"));
        assertNotNull(EventType.valueOf("CONTROL"));
        assertNotNull(EventType.valueOf("THREAT"));
    }

    // =========================================================================
    // CityNode
    // =========================================================================

    @Test
    public void testCityNodeInitialization() {
        assertEquals("Atlanta", atlanta.getName());
        assertEquals(DiseaseColor.BLUE, atlanta.getNativeColor());
        assertEquals(0.25f, atlanta.getRenderX(), 0.001f);
        assertEquals(0.39f, atlanta.getRenderY(), 0.001f);
        assertFalse(atlanta.hasResearchStation());
    }

    @Test
    public void testCityNodeInitialDiseaseCubesAreZero() {
        for (DiseaseColor color : DiseaseColor.values()) {
            assertEquals(0, atlanta.getCubeCount(color),
                    "Initial cubes for " + color + " should be 0");
        }
    }

    @Test
    public void testConnectedCitiesInitiallyEmpty() {
        assertTrue(london.getConnectedCities().isEmpty());
    }

    @Test
    public void testClickEventPrintsConnectedCities() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            atlanta.clickEvent();
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        assertTrue(output.contains("--- Atlanta ---"));
        assertTrue(output.contains("Chicago"));
    }

    @Test
    public void testClickEventPrintsNoNeighboursForIsolatedCity() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            london.clickEvent();
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(out.toString().contains("London has no neighbours."));
    }

    @Test
    public void testAddDiseaseCubeNormal() {
        assertTrue(atlanta.addDiseaseCube(DiseaseColor.BLUE, 2));
        assertEquals(2, atlanta.getCubeCount(DiseaseColor.BLUE));
    }

    @Test
    public void testAddDiseaseCubeExactlyThree() {
        assertTrue(atlanta.addDiseaseCube(DiseaseColor.BLACK, 3));
        assertEquals(3, atlanta.getCubeCount(DiseaseColor.BLACK));
    }

    @Test
    public void testAddDiseaseCubeOverflowReturnsFalseAndCapsAtThree() {
        assertFalse(atlanta.addDiseaseCube(DiseaseColor.BLUE, 4));
        assertEquals(3, atlanta.getCubeCount(DiseaseColor.BLUE));
    }

    @Test
    public void testAddDiseaseCubeAtLimitReturnsFalse() {
        atlanta.addDiseaseCube(DiseaseColor.RED, 3);
        assertFalse(atlanta.addDiseaseCube(DiseaseColor.RED, 1));
        assertEquals(3, atlanta.getCubeCount(DiseaseColor.RED));
    }

    @Test
    public void testRemoveDiseaseCubes() {
        atlanta.addDiseaseCube(DiseaseColor.YELLOW, 3);
        atlanta.removeDiseaseCubes(DiseaseColor.YELLOW, 2);
        assertEquals(1, atlanta.getCubeCount(DiseaseColor.YELLOW));
    }

    @Test
    public void testRemoveAllDiseaseCubes() {
        atlanta.addDiseaseCube(DiseaseColor.RED, 3);
        atlanta.removeDiseaseCubes(DiseaseColor.RED, 3);
        assertEquals(0, atlanta.getCubeCount(DiseaseColor.RED));
    }

    @Test
    public void testRemoveDiseaseCubesBelowZeroFloorsAtZero() {
        atlanta.addDiseaseCube(DiseaseColor.BLACK, 1);
        atlanta.removeDiseaseCubes(DiseaseColor.BLACK, 5);
        assertEquals(0, atlanta.getCubeCount(DiseaseColor.BLACK));
    }

    @Test
    public void testAddResearchStation() {
        assertFalse(atlanta.hasResearchStation());
        atlanta.addResearchStation();
        assertTrue(atlanta.hasResearchStation());
    }

    @Test
    public void testRemoveResearchStation() {
        atlanta.addResearchStation();
        atlanta.removeResearchStation();
        assertFalse(atlanta.hasResearchStation());
    }

    @Test
    public void testAddConnectionIsBidirectional() {
        assertTrue(atlanta.getConnectedCities().contains(chicago));
        assertTrue(chicago.getConnectedCities().contains(atlanta));
    }

    @Test
    public void testAddConnectionNoDuplicates() {
        atlanta.addConnection(chicago);
        long count = atlanta.getConnectedCities().stream()
                .filter(c -> c.equals(chicago)).count();
        assertEquals(1, count);
    }

    @Test
    public void testAddConnectionMultiple() {
        london.addConnectionMultiple(new ArrayList<>(List.of(chicago, miami)));
        assertTrue(london.getConnectedCities().contains(chicago));
        assertTrue(london.getConnectedCities().contains(miami));
    }

    @Test
    public void testDiseaseColorEnumValues() {
        assertEquals(4, DiseaseColor.values().length);
        assertNotNull(DiseaseColor.valueOf("BLUE"));
        assertNotNull(DiseaseColor.valueOf("YELLOW"));
        assertNotNull(DiseaseColor.valueOf("BLACK"));
        assertNotNull(DiseaseColor.valueOf("RED"));
    }

    // =========================================================================
    // PandemicMapGraph
    // =========================================================================

    @Test
    public void testMapGraphCreatesAllFortyEightCities() {
        PandemicMapGraph map = new PandemicMapGraph();
        assertEquals(48, map.getCityList().size());
    }

    @Test
    public void testMapGraphGetCityByName() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode city = map.getCity("Atlanta");
        assertNotNull(city);
        assertEquals("Atlanta", city.getName());
    }

    @Test
    public void testMapGraphGetUnknownCityReturnsNull() {
        assertNull(new PandemicMapGraph().getCity("Narnia"));
    }

    @Test
    public void testAtlantaConnections() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode atl = map.getCity("Atlanta");
        assertTrue(atl.getConnectedCities().contains(map.getCity("Chicago")));
        assertTrue(atl.getConnectedCities().contains(map.getCity("Miami")));
        assertTrue(atl.getConnectedCities().contains(map.getCity("Washington")));
    }

    @Test
    public void testConnectionsAreBidirectional() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode tokyo = map.getCity("Tokyo");
        CityNode osaka = map.getCity("Osaka");
        assertTrue(tokyo.getConnectedCities().contains(osaka));
        assertTrue(osaka.getConnectedCities().contains(tokyo));
    }

    @Test
    public void testSanFranciscoConnectsToTokyoWrapAround() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode sf    = map.getCity("San Francisco");
        CityNode tokyo = map.getCity("Tokyo");
        assertTrue(sf.getConnectedCities().contains(tokyo));
        assertTrue(tokyo.getConnectedCities().contains(sf));
    }

    @Test
    public void testCityNativeColors() {
        PandemicMapGraph map = new PandemicMapGraph();
        assertEquals(DiseaseColor.BLUE,   map.getCity("London").getNativeColor());
        assertEquals(DiseaseColor.YELLOW, map.getCity("Lagos").getNativeColor());
        assertEquals(DiseaseColor.BLACK,  map.getCity("Cairo").getNativeColor());
        assertEquals(DiseaseColor.RED,    map.getCity("Tokyo").getNativeColor());
    }

    @Test
    public void testAllBlueCitiesAreBlue() {
        PandemicMapGraph map = new PandemicMapGraph();
        for (String name : new String[]{"San Francisco", "Chicago", "Montreal",
                "New York", "Washington", "Atlanta", "London",
                "Madrid", "Paris", "Essen", "Milan", "St. Petersburg"}) {
            assertEquals(DiseaseColor.BLUE, map.getCity(name).getNativeColor(),
                    name + " should be BLUE");
        }
    }

    @Test
    public void testAllRedCitiesAreRed() {
        PandemicMapGraph map = new PandemicMapGraph();
        for (String name : new String[]{"Beijing", "Seoul", "Tokyo", "Shanghai",
                "Hong Kong", "Taipei", "Osaka", "Bangkok",
                "Ho Chi Minh City", "Manila", "Jakarta", "Sydney"}) {
            assertEquals(DiseaseColor.RED, map.getCity(name).getNativeColor(),
                    name + " should be RED");
        }
    }

    @Test
    public void testNoCityHasResearchStationBeforeSetup() {
        long count = new PandemicMapGraph().getCityList().stream()
                .filter(CityNode::hasResearchStation).count();
        assertEquals(0, count);
    }

    // =========================================================================
    // DiseaseManager
    // =========================================================================

    @Test
    public void testDiseaseManagerInitialOutbreakScore() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertEquals(0, gm.getState().getDiseaseManager().getOutbreakScore());
    }

    @Test
    public void testDiseaseManagerInitialCubesLeft() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertEquals(96, gm.getState().getDiseaseManager().getInfectionCubesLeft());
    }

    @Test
    public void testDiseaseManagerNotCuredInitially() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        DiseaseManager dm = gm.getState().getDiseaseManager();
        assertFalse(dm.isCured(DiseaseColor.BLUE));
        assertFalse(dm.isCured(DiseaseColor.YELLOW));
        assertFalse(dm.isCured(DiseaseColor.BLACK));
        assertFalse(dm.isCured(DiseaseColor.RED));
    }

    @Test
    public void testDiseaseManagerDiscoverCureNoException() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.getState().getDiseaseManager().discoverCure(DiseaseColor.BLUE);
    }

    @Test
    public void testDiseaseManagerDiscoverCureSetsEveryColor() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        DiseaseManager dm = gm.getState().getDiseaseManager();

        dm.discoverCure(DiseaseColor.BLUE);
        dm.discoverCure(DiseaseColor.YELLOW);
        dm.discoverCure(DiseaseColor.BLACK);
        dm.discoverCure(DiseaseColor.RED);

        assertTrue(dm.isCured(DiseaseColor.BLUE));
        assertTrue(dm.isCured(DiseaseColor.YELLOW));
        assertTrue(dm.isCured(DiseaseColor.BLACK));
        assertTrue(dm.isCured(DiseaseColor.RED));
        assertTrue(dm.areAllCured());
    }

    @Test
    public void testDiseaseManagerAddRemoveCubesNoException() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        DiseaseManager dm = gm.getState().getDiseaseManager();
        CityNode city = gm.getState().getMap().getCity("Atlanta");
        dm.addInfectionCubes(city, 2);
        dm.removeInfectionCubes(city, 1);
    }

    @Test
    public void testDiseaseManagerIncreaseOutbreakScoreNoException() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.getState().getDiseaseManager().increaseOutbreakScore();
    }

    @Test
    public void testDiseaseManagerOutbreakIncreasesWhenCityOverflows() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        DiseaseManager dm = gm.getState().getDiseaseManager();
        CityNode city = gm.getState().getMap().getCity("Atlanta");

        dm.addInfectionCubes(city, 3);
        dm.addInfectionCubes(city, 1);

        assertEquals(1, dm.getOutbreakScore());
        assertEquals(3, city.getCubeCount(DiseaseColor.BLUE));
    }

    @Test
    public void testDiseaseManagerCubesFloorAtZeroAndTriggerLoseCheck() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        DiseaseManager dm = gm.getState().getDiseaseManager();
        CityNode city = gm.getState().getMap().getCity("Atlanta");

        dm.addInfectionCubes(city, 200);

        assertEquals(0, dm.getInfectionCubesLeft());
    }

    @Test
    public void testDiseaseManagerRemoveCubesCapsSupplyAtNinetySix() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        DiseaseManager dm = gm.getState().getDiseaseManager();
        CityNode city = gm.getState().getMap().getCity("Atlanta");

        dm.removeInfectionCubes(city, 5);

        assertEquals(96, dm.getInfectionCubesLeft());
    }

    // =========================================================================
    // PlayerRoles
    // =========================================================================

    @Test
    public void testPlayerRolesAllSevenPresent() {
        assertEquals(7, PlayerRoles.values().length);
        assertNotNull(PlayerRoles.valueOf("CONTINGENCY_PLANNER"));
        assertNotNull(PlayerRoles.valueOf("DISPATCHER"));
        assertNotNull(PlayerRoles.valueOf("MEDIC"));
        assertNotNull(PlayerRoles.valueOf("OPERATIONS_EXPERT"));
        assertNotNull(PlayerRoles.valueOf("QUARANTINE_SPECIALIST"));
        assertNotNull(PlayerRoles.valueOf("RESEARCHER"));
        assertNotNull(PlayerRoles.valueOf("SCIENTIST"));
    }

    @Test
    public void testPlayerRolesOrdinalsAreUnique() {
        PlayerRoles[] roles = PlayerRoles.values();
        for (int i = 0; i < roles.length; i++) {
            for (int j = i + 1; j < roles.length; j++) {
                assertNotEquals(roles[i].ordinal(), roles[j].ordinal());
            }
        }
    }

    // =========================================================================
    // PlayerState
    // =========================================================================

    @Test
    public void testPlayerStateInitialization() {
        assertEquals("Alice", playerInAtlanta.getPlayerName());
        assertEquals(atlanta, playerInAtlanta.getPlayerCurrentCity());
        assertFalse(playerInAtlanta.getIsDiscarding());
        assertTrue(playerInAtlanta.getHand().isEmpty());
    }

    @Test
    public void testPlayerStateRoleNullByDefault() {
        assertNull(playerInAtlanta.getPlayerRole());
    }

    @Test
    public void testPlayerStateReadyAndPlayerReference() {
        Player player = new Player(playerInAtlanta, null);

        playerInAtlanta.setReady(true);
        playerInAtlanta.setPlayer(player);

        assertTrue(playerInAtlanta.isReady());
        assertEquals(player, playerInAtlanta.getPlayer());
    }

    @Test
    public void testPlayerStateSetCurrentCity() {
        playerInAtlanta.setCurrentCity(chicago);
        assertEquals(chicago, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testPlayerStateAddCard() {
        Card card = new Card("Atlanta", CardType.CITY, atlanta);
        playerInAtlanta.addCard(card);
        assertEquals(1, playerInAtlanta.getHand().size());
        assertEquals(card, playerInAtlanta.getCard(0));
    }

    @Test
    public void testPlayerStateAddMultipleCards() {
        playerInAtlanta.addCard(new Card("Atlanta", CardType.CITY, atlanta));
        playerInAtlanta.addCard(new Card("Chicago", CardType.CITY, chicago));
        assertEquals(2, playerInAtlanta.getHand().size());
    }

    @Test
    public void testPlayerStateGetCardByIndex() {
        Card c1 = new Card("Atlanta", CardType.CITY, atlanta);
        Card c2 = new Card("Chicago", CardType.CITY, chicago);
        playerInAtlanta.addCard(c1);
        playerInAtlanta.addCard(c2);
        assertEquals(c1, playerInAtlanta.getCard(0));
        assertEquals(c2, playerInAtlanta.getCard(1));
    }

    @Test
    public void testPlayerStateRemoveCardByIndex() {
        Card c1 = new Card("Atlanta", CardType.CITY, atlanta);
        Card c2 = new Card("Chicago", CardType.CITY, chicago);
        playerInAtlanta.addCard(c1);
        playerInAtlanta.addCard(c2);
        playerInAtlanta.removeCard(0);
        assertEquals(1, playerInAtlanta.getHand().size());
        assertEquals(c2, playerInAtlanta.getCard(0));
    }

    @Test
    public void testPlayerStateRemoveLastCard() {
        Card card = new Card("Atlanta", CardType.CITY, atlanta);
        playerInAtlanta.addCard(card);
        playerInAtlanta.removeCard(0);
        assertTrue(playerInAtlanta.getHand().isEmpty());
    }

    @Test
    public void testPlayerStateSetIsDiscarding() {
        playerInAtlanta.setIsDiscarding(true);
        assertTrue(playerInAtlanta.getIsDiscarding());
        playerInAtlanta.setIsDiscarding(false);
        assertFalse(playerInAtlanta.getIsDiscarding());
    }

    @Test
    public void testPlayerStateHandsAreIndependent() {
        playerInAtlanta.addCard(new Card("Atlanta", CardType.CITY, atlanta));
        assertTrue(playerInChicago.getHand().isEmpty(),
                "Bob's hand should still be empty");
    }

    // =========================================================================
    // GameState
    // =========================================================================

    @Test
    public void testGameStateDefaultValues() {
        GameState state = new GameState();
        assertFalse(state.isGameOver());
        assertFalse(state.isGameWon());
        assertEquals(0, state.getCurrentPlayerIndex());
        assertEquals(0, state.getActionsRemaining());
        assertEquals(0, state.getInfectionRate());
        assertEquals(0, state.getEpidemicCount());
        assertTrue(state.getPlayers().isEmpty());
    }

    @Test
    public void testGameStateSetGameOver() {
        GameState state = new GameState();
        state.setGameOver(true);
        assertTrue(state.isGameOver());
        state.setGameOver(false);
        assertFalse(state.isGameOver());
    }

    @Test
    public void testGameStateSetGameWon() {
        GameState state = new GameState();
        state.setGameWon(true);
        assertTrue(state.isGameWon());
    }

    @Test
    public void testGameStateSetCurrentPlayerIndex() {
        GameState state = new GameState();
        state.setCurrentPlayerIndex(2);
        assertEquals(2, state.getCurrentPlayerIndex());
    }

    @Test
    public void testGameStateSetActionsRemaining() {
        GameState state = new GameState();
        state.setActionsRemaining(4);
        assertEquals(4, state.getActionsRemaining());
    }

    @Test
    public void testGameStateSetInfectionRate() {
        GameState state = new GameState();
        state.setInfectionRate(3);
        assertEquals(3, state.getInfectionRate());
    }

    @Test
    public void testGameStateSetEpidemicCount() {
        GameState state = new GameState();
        state.setEpidemicCount(2);
        assertEquals(2, state.getEpidemicCount());
    }

    @Test
    public void testGameStateAddPlayerState() {
        GameState state = new GameState();
        state.addPlayer(playerInAtlanta);
        assertEquals(1, state.getPlayers().size());
        assertEquals(playerInAtlanta, state.getPlayers().get(0));
    }

    @Test
    public void testGameStateLobbyChatAndCountdown() {
        GameState state = new GameState();
        LobbyChatMessage message = new LobbyChatMessage("Alice", "Ready", 99L);

        state.addLobbyChatMessage(message);
        state.setLobbyCountdownStartedAt(1234L);

        assertEquals(1, state.getLobbyChatMessages().size());
        assertEquals(message, state.getLobbyChatMessages().get(0));
        assertEquals(1234L, state.getLobbyCountdownStartedAt());
    }

    @Test
    public void testGameStateGameStartedFlag() {
        GameState state = new GameState();

        state.setGameStarted(true);

        assertTrue(state.isGameStarted());
    }

    @Test
    public void testGameStateCurrentPlayerReturnsNullWhenEmpty() {
        GameState state = new GameState();

        assertNull(state.getCurrentPlayer());
        assertFalse(state.isPlayerTurn(playerInAtlanta));
    }

    @Test
    public void testGameStateCurrentPlayerReturnsNullWhenPlayerListIsNull() throws Exception {
        GameState state = new GameState();
        replaceGameStatePlayers(state, null);

        assertNull(state.getCurrentPlayer());
    }

    @Test
    public void testGameStateCurrentPlayerAndTurnCheck() {
        GameState state = new GameState();
        state.addPlayer(playerInAtlanta);

        assertEquals(playerInAtlanta, state.getCurrentPlayer());
        assertTrue(state.isPlayerTurn(new PlayerState("alice")));
        assertFalse(state.isPlayerTurn(playerInChicago));
    }

    @Test
    public void testLobbyChatMessageDefaultConstructorAndSetters() {
        LobbyChatMessage message = new LobbyChatMessage();

        message.setPlayerName("Alice");
        message.setMessage("Ready");
        message.setTimestamp(99L);

        assertEquals("Alice", message.getPlayerName());
        assertEquals("Ready", message.getMessage());
        assertEquals(99L, message.getTimestamp());
    }

    @Test
    public void testLobbyChatMessageConstructorSetsFields() {
        LobbyChatMessage message = new LobbyChatMessage("Bob", "Starting", 123L);

        assertEquals("Bob", message.getPlayerName());
        assertEquals("Starting", message.getMessage());
        assertEquals(123L, message.getTimestamp());
    }

    @Test
    public void testGameStateAddMultiplePlayerStates() {
        GameState state = new GameState();
        state.addPlayer(playerInAtlanta);
        state.addPlayer(playerInChicago);
        assertEquals(2, state.getPlayers().size());
    }

    @Test
    public void testGameStateRemovePlayerState() {
        GameState state = new GameState();
        state.addPlayer(playerInAtlanta);
        state.addPlayer(playerInChicago);
        state.removePlayer(playerInAtlanta);
        assertEquals(1, state.getPlayers().size());
        assertFalse(state.getPlayers().contains(playerInAtlanta));
    }

    @Test
    public void testGameStateSetMap() {
        GameState state = new GameState();
        PandemicMapGraph map = new PandemicMapGraph();
        state.setMap(map);
        assertEquals(map, state.getMap());
    }

    @Test
    public void testGameStateSetDiseaseManager() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        DiseaseManager dm = gm.getState().getDiseaseManager();
        assertNotNull(dm);
        assertEquals(dm, gm.getState().getDiseaseManager());
    }

    // =========================================================================
    // Player
    // =========================================================================

    @Test
    public void testPlayerInitialization() {
        Player player = new Player(playerInAtlanta, null);
        assertEquals(playerInAtlanta, player.getState());
    }

    @Test
    public void testPlayerUpdateState() {
        Player player = new Player(playerInAtlanta, null);
        player.updateState(playerInChicago);
        assertEquals(playerInChicago, player.getState());
    }

    @Test
    public void testPlayerExecuteActionWithoutClientDoesNotThrow() {
        Player player = new Player(playerInAtlanta, null);

        assertDoesNotThrow(() -> player.executeAction(new DriveFerryAction(chicago)));
    }

    @Test
    public void testPlayerSendTestPacketWithoutClientDoesNotThrow() {
        Player player = new Player(playerInAtlanta, null);

        assertDoesNotThrow(player::sendTestPacket);
    }

    @Test
    public void testPlayerExecuteActionSendsPacketThroughClient() {
        GameClient client = mock(GameClient.class);
        Player player = new Player(playerInAtlanta, client);

        player.executeAction(new DriveFerryAction(chicago));

        verify(client).sendPacket(any(Packet.class));
    }

    @Test
    public void testPlayerSendTestPacketSendsPacketThroughClient() {
        GameClient client = mock(GameClient.class);
        Player player = new Player(playerInAtlanta, client);

        player.sendTestPacket();

        verify(client).sendPacket(any(Packet.class));
    }

    // =========================================================================
    // GameClient
    // =========================================================================

    @Test
    public void testGameClientAddNullListenerDoesNothing() {
        GameClient client = new GameClient();

        assertDoesNotThrow(() -> client.addPlayerUpdateListener(null));
    }

    @Test
    public void testGameClientIncomingDataNotifiesListeners() throws Exception {
        GameClient client = new GameClient();
        AtomicInteger updates = new AtomicInteger();
        AtomicReference<JsonNode> latest = new AtomicReference<>();

        client.addPlayerUpdateListener(gameState -> {
            updates.incrementAndGet();
            latest.set(gameState);
        });

        invokeHandleIncomingData(client, "{\"players\":[{\"playerName\":\"Alice\"}]}");

        assertEquals(1, updates.get());
        assertEquals("Alice", latest.get().get("players").get(0).get("playerName").asText());
    }

    @Test
    public void testGameClientLateListenerReceivesLatestGameState() throws Exception {
        GameClient client = new GameClient();
        AtomicReference<JsonNode> latest = new AtomicReference<>();

        invokeHandleIncomingData(client, "{\"turn\":2}");
        client.addPlayerUpdateListener(latest::set);

        assertEquals(2, latest.get().get("turn").asInt());
    }

    @Test
    public void testGameClientRemoveListenerStopsNotifications() throws Exception {
        GameClient client = new GameClient();
        AtomicInteger updates = new AtomicInteger();
        GameClient.PlayerUpdateListener listener = gameState -> updates.incrementAndGet();
        client.addPlayerUpdateListener(listener);
        client.removePlayerUpdateListener(listener);

        invokeHandleIncomingData(client, "{\"turn\":3}");

        assertEquals(0, updates.get());
    }

    @Test
    public void testGameClientInvalidIncomingDataDoesNotNotifyListeners() throws Exception {
        GameClient client = new GameClient();
        AtomicInteger updates = new AtomicInteger();
        client.addPlayerUpdateListener(gameState -> updates.incrementAndGet());

        invokeHandleIncomingData(client, "{not-json");

        assertEquals(0, updates.get());
    }

    @Test
    public void testGameClientSendPacketStringWithoutConnectionDoesNotThrow() {
        GameClient client = new GameClient();

        assertDoesNotThrow(() -> client.sendPacket("{\"type\":\"PING\"}"));
    }

    @Test
    public void testGameClientSendPacketObjectWithoutConnectionDoesNotThrow() {
        GameClient client = new GameClient();

        assertDoesNotThrow(() -> client.sendPacket(new Packet(PacketType.PING)));
    }

    @Test
    public void testGameClientSendPacketEncryptsJsonWhenConnected() throws Exception {
        GameClient client = new GameClient();
        SecureSocket secureSocket = newSecureSocket();
        StringWriter output = new StringWriter();
        setField(client, "secureSocket", secureSocket);
        setField(client, "out", new PrintWriter(output, true));

        client.sendPacket("{\"type\":\"PING\"}");

        String encrypted = output.toString().trim();
        assertFalse(encrypted.isEmpty());
        assertEquals("{\"type\":\"PING\"}", secureSocket.decrypt(encrypted));
    }

    @Test
    public void testGameClientSendPacketObjectSerializesAndEncryptsPacket() throws Exception {
        GameClient client = new GameClient();
        SecureSocket secureSocket = newSecureSocket();
        StringWriter output = new StringWriter();
        setField(client, "secureSocket", secureSocket);
        setField(client, "out", new PrintWriter(output, true));

        client.sendPacket(new Packet(PacketType.PING));

        Packet packet = Packet.fromJson(secureSocket.decrypt(output.toString().trim()));
        assertEquals(PacketType.PING, packet.getType());
    }

    @Test
    public void testGameClientReceivePacketReturnsNullWhenDisconnected() {
        GameClient client = new GameClient();

        assertNull(client.receivePacket());
    }

    @Test
    public void testGameClientReceivePacketDecryptsIncomingLine() throws Exception {
        GameClient client = new GameClient();
        SecureSocket secureSocket = newSecureSocket();
        String encrypted = secureSocket.encrypt("{\"type\":\"PONG\"}");
        setField(client, "secureSocket", secureSocket);
        setField(client, "in", new BufferedReader(new StringReader(encrypted + System.lineSeparator())));

        assertEquals("{\"type\":\"PONG\"}", client.receivePacket());
    }

    @Test
    public void testGameClientReceivePacketReturnsNullForInvalidEncryptedLine() throws Exception {
        GameClient client = new GameClient();
        setField(client, "secureSocket", newSecureSocket());
        setField(client, "in", new BufferedReader(new StringReader("not-encrypted" + System.lineSeparator())));

        assertNull(client.receivePacket());
    }

    @Test
    public void testGameClientDisconnectWithoutConnectionDoesNotThrow() {
        GameClient client = new GameClient();

        assertDoesNotThrow(client::disconnect);
    }

    @Test
    public void testGameClientDisconnectFromLobbySendsDisconnectWhenConnected() throws Exception {
        GameClient client = new GameClient();
        SecureSocket secureSocket = newSecureSocket();
        StringWriter output = new StringWriter();
        setField(client, "secureSocket", secureSocket);
        setField(client, "out", new PrintWriter(output, true));
        setField(client, "isConnected", true);

        client.disconnectFromLobby();

        Packet packet = Packet.fromJson(secureSocket.decrypt(output.toString().trim()));
        assertEquals(PacketType.DISCONNECT, packet.getType());
    }

    @Test
    public void testGameClientHandleIncomingPacketKnownTypesDoNotThrow() {
        GameClient client = new GameClient();

        assertDoesNotThrow(() -> invokeHandleIncomingPacket(client, new Packet(PacketType.GAME_DATA)));
        assertDoesNotThrow(() -> invokeHandleIncomingPacket(client, new Packet(PacketType.PONG)));
        assertDoesNotThrow(() -> invokeHandleIncomingPacket(client, new Packet(PacketType.ERROR)));
    }

    @Test
    public void testGameClientHandleIncomingPacketDefaultTypeDoesNotThrow() {
        GameClient client = new GameClient();

        assertDoesNotThrow(() -> invokeHandleIncomingPacket(client, new Packet(PacketType.PING)));
    }

    // =========================================================================
    // Deck
    // =========================================================================

    @Test
    public void testDeckInitialCountIsPositive() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertTrue(gm.getState().getCardDeck().getRemainingCardsCount() > 0);
    }

    @Test
    public void testDeckIsNotEmptyInitially() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertFalse(gm.getState().getCardDeck().isEmpty());
    }

    @Test
    public void testDeckTypesEnumValues() {
        assertEquals(3, Deck.DeckTypes.values().length);
        assertNotNull(Deck.DeckTypes.valueOf("PLAYER"));
        assertNotNull(Deck.DeckTypes.valueOf("INFECTION"));
        assertNotNull(Deck.DeckTypes.valueOf("DISCARD"));
    }

    @Test
    public void testDeckDrawHandReducesCountByTwo() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        Deck deck = gm.getState().getCardDeck();
        int before = deck.getRemainingCardsCount();
        deck.drawHand(playerInAtlanta);
        assertEquals(before - 2, deck.getRemainingCardsCount());
    }

    @Test
    public void testDeckDiscardInfectionCardNoException() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        Card infCard = new Card("London", CardType.INFECTION,
                gm.getState().getMap().getCity("London"));
        gm.getState().getCardDeck().discard(infCard);
    }

    @Test
    public void testDeckDiscardCityCardNoException() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        Card cityCard = new Card("London", CardType.CITY,
                gm.getState().getMap().getCity("London"));
        gm.getState().getCardDeck().discard(cityCard);
    }

    @Test
    public void testDeckDrawInitialHandSkipsEpidemicCards() throws Exception {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        Deck deck = gm.getState().getCardDeck();
        Card epidemic = new Card("System Breach", CardType.EPIDEMIC, null);
        Card city = new Card("Atlanta", CardType.CITY, gm.getState().getMap().getCity("Atlanta"));
        replacePlayerDeck(deck, new ArrayList<>(List.of(epidemic, city)));

        deck.drawInitialHand(playerInAtlanta, 1);

        assertEquals(city, playerInAtlanta.getCard(0));
        assertEquals(1, deck.getRemainingCardsCount());
    }

    @Test
    public void testDeckInitialDrawEndsGameWhenOnlyEpidemicsRemain() throws Exception {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        Deck deck = gm.getState().getCardDeck();
        replacePlayerDeck(deck, new ArrayList<>(List.of(
                new Card("System Breach", CardType.EPIDEMIC, null))));

        deck.drawInitialHand(playerInAtlanta, 1);

        assertTrue(gm.isGameOver());
        assertFalse(gm.isGameWon());
        assertTrue(playerInAtlanta.getHand().isEmpty());
    }

    @Test
    public void testDeckDrawHandEndsGameWhenDeckCannotSupplyTwoCards() throws Exception {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        Deck deck = gm.getState().getCardDeck();
        replacePlayerDeck(deck, new ArrayList<>(List.of(
                new Card("Atlanta", CardType.CITY, gm.getState().getMap().getCity("Atlanta")))));

        deck.drawHand(playerInAtlanta);

        assertTrue(gm.isGameOver());
        assertFalse(gm.isGameWon());
        assertEquals(1, playerInAtlanta.getHand().size());
    }

    @Test
    public void testDeckShuffleDoesNotChangeCardCount() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        Deck deck = gm.getState().getCardDeck();
        List<Card> cards = new ArrayList<>(List.of(
                new Card("Atlanta", CardType.CITY, atlanta),
                new Card("Chicago", CardType.CITY, chicago)));

        deck.shuffle(cards);

        assertEquals(2, cards.size());
    }

    // =========================================================================
    // GameManager
    // =========================================================================

    @Test
    public void testGameManagerNotOverOrWonAtStart() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertFalse(gm.isGameOver());
        assertFalse(gm.isGameWon());
    }

    @Test
    public void testGameManagerStartGameSetsPlayersReadyFalseAndDealsHands() {
        List<PlayerState> players = new ArrayList<>(List.of(
                new PlayerState("Alice"),
                new PlayerState("Bob"),
                new PlayerState("Cara"),
                new PlayerState("Dan")));
        players.forEach(player -> player.setReady(true));
        GameManager gm = new GameManager(players, false);

        gm.startGame();

        assertTrue(gm.getState().isGameStarted());
        assertEquals(0, gm.getState().getCurrentPlayerIndex());
        assertEquals(4, gm.getState().getActionsRemaining());
        assertEquals(0, gm.getState().getLobbyCountdownStartedAt());
        for (PlayerState player : players) {
            assertFalse(player.isReady());
            assertNotNull(player.getPlayer());
            assertEquals(gm.getState().getMap().getCity("Atlanta"), player.getPlayerCurrentCity());
            assertEquals(2, player.getHand().size());
        }
    }

    @Test
    public void testGameManagerStartGameReturnsWhenAlreadyStarted() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        gm.getState().setGameStarted(true);

        gm.startGame();

        assertEquals(0, playerInAtlanta.getHand().size());
    }

    @Test
    public void testAtlantaHasResearchStationAfterSetup() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertTrue(gm.getState().getMap().getCity("Atlanta").hasResearchStation());
    }

    @Test
    public void testGameManagerGetCurrentPlayer() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertEquals(playerInAtlanta, gm.getCurrentPlayer());
    }

    @Test
    public void testGameManagerInitialInfectionRateIsTwo() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertEquals(2, gm.getInfectionRate());
    }

    @Test
    public void testGameManagerInfectionRateIncreasesToThree() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.increaseInfectionRate();
        gm.increaseInfectionRate();
        gm.increaseInfectionRate();
        assertEquals(3, gm.getInfectionRate());
    }

    @Test
    public void testGameManagerInfectionRateCapsAtFour() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        for (int i = 0; i < 20; i++) gm.increaseInfectionRate();
        assertEquals(4, gm.getInfectionRate());
    }

    @Test
    public void testCheckWinConditionDoesNotTriggerInitially() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.checkWinCondition();
        assertFalse(gm.isGameOver());
    }

    @Test
    public void testCheckWinConditionTriggersWhenAllDiseasesCured() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        DiseaseManager dm = gm.getState().getDiseaseManager();
        dm.discoverCure(DiseaseColor.BLUE);
        dm.discoverCure(DiseaseColor.YELLOW);
        dm.discoverCure(DiseaseColor.BLACK);
        dm.discoverCure(DiseaseColor.RED);

        gm.checkWinCondition();

        assertTrue(gm.isGameOver());
        assertTrue(gm.isGameWon());
    }

    @Test
    public void testCheckLoseConditionDoesNotTriggerInitially() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.checkLoseCondition();
        assertFalse(gm.isGameOver());
    }

    @Test
    public void testCheckLoseConditionTriggersAtEightOutbreaks() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        for (int i = 0; i < 8; i++) {
            gm.getState().getDiseaseManager().increaseOutbreakScore();
        }

        gm.checkLoseCondition();

        assertTrue(gm.isGameOver());
        assertFalse(gm.isGameWon());
    }

    @Test
    public void testNextTurnAdvancesPlayerIndex() {
        PlayerState ps2 = new PlayerState("Bob");
        ps2.setCurrentCity(chicago);
        List<PlayerState> players = new ArrayList<>();
        players.add(playerInAtlanta);
        players.add(ps2);
        GameManager gm = new GameManager(players);

        assertEquals(playerInAtlanta, gm.getCurrentPlayer());
        gm.nextTurn();
        assertEquals(ps2, gm.getCurrentPlayer());
    }

    @Test
    public void testNextTurnWrapsAroundToFirstPlayer() {
        PlayerState ps2 = new PlayerState("Bob");
        ps2.setCurrentCity(chicago);
        List<PlayerState> players = new ArrayList<>();
        players.add(playerInAtlanta);
        players.add(ps2);
        GameManager gm = new GameManager(players);

        gm.nextTurn();
        gm.nextTurn();
        assertEquals(playerInAtlanta, gm.getCurrentPlayer());
    }

    @Test
    public void testPerformActionIsNoOpWhenGameOver() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.getState().setGameOver(true);
        Player player = new Player(playerInAtlanta, null);
        gm.performAction(player, new DriveFerryAction(chicago)); // must not throw
    }

    @Test
    public void testPerformActionMovesCurrentPlayerAndConsumesAction() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        CityNode chicagoOnMap = gm.getState().getMap().getCity("Chicago");
        Player player = new Player(playerInAtlanta, null);

        gm.performAction(player, new DriveFerryAction(chicagoOnMap));

        assertEquals(chicagoOnMap, playerInAtlanta.getPlayerCurrentCity());
        assertEquals(3, gm.getState().getActionsRemaining());
    }

    @Test
    public void testPerformActionDoesNothingForInvalidAction() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        CityNode tokyo = gm.getState().getMap().getCity("Tokyo");
        Player player = new Player(playerInAtlanta, null);

        gm.performAction(player, new DriveFerryAction(tokyo));

        assertEquals(gm.getState().getMap().getCity("Atlanta"), playerInAtlanta.getPlayerCurrentCity());
        assertEquals(4, gm.getState().getActionsRemaining());
    }

    @Test
    public void testPerformActionDoesNothingForNonCurrentPlayer() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta, playerInChicago)), false);
        Player nonCurrentPlayer = new Player(playerInChicago, null);
        CityNode miamiOnMap = gm.getState().getMap().getCity("Miami");

        gm.performAction(nonCurrentPlayer, new DriveFerryAction(miamiOnMap));

        assertEquals(gm.getState().getMap().getCity("Atlanta"), playerInChicago.getPlayerCurrentCity());
        assertEquals(4, gm.getState().getActionsRemaining());
    }

    @Test
    public void testPerformActionDoesNothingWhenActionsAreGone() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        CityNode chicagoOnMap = gm.getState().getMap().getCity("Chicago");
        gm.getState().setActionsRemaining(0);

        gm.performAction(new Player(playerInAtlanta, null), new DriveFerryAction(chicagoOnMap));

        assertEquals(gm.getState().getMap().getCity("Atlanta"), playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testPerformActionDoesNothingWhileCurrentPlayerDiscards() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        CityNode chicagoOnMap = gm.getState().getMap().getCity("Chicago");
        playerInAtlanta.setIsDiscarding(true);

        gm.performAction(new Player(playerInAtlanta, null), new DriveFerryAction(chicagoOnMap));

        assertEquals(gm.getState().getMap().getCity("Atlanta"), playerInAtlanta.getPlayerCurrentCity());
        assertEquals(4, gm.getState().getActionsRemaining());
    }

    @Test
    public void testPerformActionAutoAdvancesWhenLastActionConsumed() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta, playerInChicago)), false);
        CityNode chicagoOnMap = gm.getState().getMap().getCity("Chicago");
        gm.getState().setActionsRemaining(1);

        gm.performAction(new Player(playerInAtlanta, null), new DriveFerryAction(chicagoOnMap));

        assertEquals(playerInChicago, gm.getCurrentPlayer());
        assertEquals(4, gm.getState().getActionsRemaining());
    }

    @Test
    public void testConsumeActionDecrementsForCurrentPlayer() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);

        gm.consumeAction(playerInAtlanta);

        assertEquals(3, gm.getState().getActionsRemaining());
    }

    @Test
    public void testConsumeActionDoesNothingForNonCurrentPlayer() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta, playerInChicago)), false);

        gm.consumeAction(playerInChicago);

        assertEquals(4, gm.getState().getActionsRemaining());
    }

    @Test
    public void testConsumeActionAdvancesOnLastAction() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta, playerInChicago)), false);
        gm.getState().setActionsRemaining(1);

        gm.consumeAction(playerInAtlanta);

        assertEquals(playerInChicago, gm.getCurrentPlayer());
        assertEquals(4, gm.getState().getActionsRemaining());
    }

    @Test
    public void testNextTurnClearsDiscardingWhenHandIsAtLimit() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta, playerInChicago)), false);
        playerInAtlanta.setIsDiscarding(true);

        gm.nextTurn();

        assertFalse(playerInAtlanta.getIsDiscarding());
        assertEquals(playerInChicago, gm.getCurrentPlayer());
    }

    @Test
    public void testNextTurnKeepsDiscardingWhenHandIsOverLimit() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta, playerInChicago)), false);
        for (int i = 0; i < 8; i++) {
            playerInAtlanta.addCard(new Card("Card " + i, CardType.CITY, atlanta));
        }
        playerInAtlanta.setIsDiscarding(true);

        gm.nextTurn();

        assertTrue(playerInAtlanta.getIsDiscarding());
        assertEquals(playerInAtlanta, gm.getCurrentPlayer());
    }

    @Test
    public void testNextTurnSetsDiscardingWhenHandExceedsLimitAfterDraw() throws Exception {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta, playerInChicago)), false);
        Deck deck = gm.getState().getCardDeck();
        for (int i = 0; i < 6; i++) {
            playerInAtlanta.addCard(new Card("Existing " + i, CardType.CITY, atlanta));
        }
        replacePlayerDeck(deck, new ArrayList<>(List.of(
                new Card("Draw 1", CardType.CITY, gm.getState().getMap().getCity("Atlanta")),
                new Card("Draw 2", CardType.CITY, gm.getState().getMap().getCity("Chicago")))));

        gm.nextTurn();

        assertTrue(playerInAtlanta.getIsDiscarding());
        assertEquals(0, gm.getState().getActionsRemaining());
        assertEquals(playerInAtlanta, gm.getCurrentPlayer());
    }

    @Test
    public void testDiscardCurrentPlayerCardRemovesCardAndAdvancesAtLimit() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta, playerInChicago)), false);
        for (int i = 0; i < 8; i++) {
            playerInAtlanta.addCard(new Card("Card " + i, CardType.CITY, atlanta));
        }
        playerInAtlanta.setIsDiscarding(true);

        gm.discardCurrentPlayerCard(playerInAtlanta, 0);

        assertEquals(7, playerInAtlanta.getHand().size());
        assertFalse(playerInAtlanta.getIsDiscarding());
        assertEquals(playerInChicago, gm.getCurrentPlayer());
    }

    @Test
    public void testDiscardCurrentPlayerCardIgnoresInvalidIndex() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        playerInAtlanta.addCard(new Card("Atlanta", CardType.CITY, atlanta));
        playerInAtlanta.setIsDiscarding(true);

        gm.discardCurrentPlayerCard(playerInAtlanta, 5);

        assertEquals(1, playerInAtlanta.getHand().size());
        assertTrue(playerInAtlanta.getIsDiscarding());
    }

    @Test
    public void testGetCurrentPlayerResetsOutOfRangeIndex() {
        GameManager gm = new GameManager(new ArrayList<>(List.of(playerInAtlanta)), false);
        gm.getState().setCurrentPlayerIndex(99);

        assertEquals(playerInAtlanta, gm.getCurrentPlayer());
        assertEquals(0, gm.getState().getCurrentPlayerIndex());
    }

    @Test
    public void testGetCurrentPlayerReturnsNullWithNoPlayers() {
        GameManager gm = new GameManager(new ArrayList<>(), false);

        assertNull(gm.getCurrentPlayer());
    }

    @Test
    public void testGetInfectionRateTrackReturnsUnderlyingTrack() {
        GameManager gm = singlePlayerGame(playerInAtlanta);

        assertArrayEquals(new int[]{2, 2, 2, 3, 3, 4, 4}, gm.getInfectionRateTrack());
    }

    // =========================================================================
    // DriveFerryAction
    // =========================================================================

    @Test
    public void testDriveFerryValidWhenConnected() {
        assertTrue(new DriveFerryAction(chicago).isValid(null, playerInAtlanta));
    }

    @Test
    public void testDriveFerryInvalidWhenNotConnected() {
        assertFalse(new DriveFerryAction(miami).isValid(null, playerInAtlanta));
    }

    @Test
    public void testDriveFerryInvalidForCurrentCity() {
        assertFalse(new DriveFerryAction(atlanta).isValid(null, playerInAtlanta));
    }

    @Test
    public void testDriveFerryInvalidForIsolatedCity() {
        assertFalse(new DriveFerryAction(london).isValid(null, playerInAtlanta));
    }

    @Test
    public void testDriveFerryExecuteMovesPlayer() {
        new DriveFerryAction(chicago).execute(null, playerInAtlanta);
        assertEquals(chicago, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testDriveFerryExecuteDoesNotMoveIfInvalid() {
        new DriveFerryAction(miami).execute(null, playerInAtlanta);
        assertEquals(atlanta, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testDriveFerryOnRealMapAtlantaToChicago() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode atl = map.getCity("Atlanta");
        CityNode chi = map.getCity("Chicago");
        PlayerState ps = new PlayerState("Tester");
        ps.setCurrentCity(atl);

        DriveFerryAction action = new DriveFerryAction(chi);
        assertTrue(action.isValid(null, ps));
        action.execute(null, ps);
        assertEquals(chi, ps.getPlayerCurrentCity());
    }

    @Test
    public void testDriveFerryOnRealMapAtlantaToTokyoInvalid() {
        PandemicMapGraph map = new PandemicMapGraph();
        PlayerState ps = new PlayerState("Tester");
        ps.setCurrentCity(map.getCity("Atlanta"));
        assertFalse(new DriveFerryAction(map.getCity("Tokyo")).isValid(null, ps));
    }

    // =========================================================================
    // DirectFlightAction
    // =========================================================================

    @Test
    public void testDirectFlightValidWhenCardInHand() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        playerInAtlanta.addCard(chicagoCard);
        assertTrue(new DirectFlightAction(chicago, chicagoCard).isValid(null, playerInAtlanta));
    }

    @Test
    public void testDirectFlightInvalidWhenCardNotInHand() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        assertFalse(new DirectFlightAction(chicago, chicagoCard).isValid(null, playerInAtlanta));
    }

    @Test
    public void testDirectFlightInvalidWithWrongCityCard() {
        Card miamiCard = new Card("Miami", CardType.CITY, miami);
        playerInAtlanta.addCard(miamiCard);
        assertFalse(new DirectFlightAction(chicago, miamiCard).isValid(null, playerInAtlanta));
    }

    @Test
    public void testDirectFlightInvalidWithNonCityCard() {
        Card epidemic = new Card("System Breach", CardType.EPIDEMIC, null);
        playerInAtlanta.addCard(epidemic);
        assertFalse(new DirectFlightAction(chicago, epidemic).isValid(null, playerInAtlanta));
    }

    @Test
    public void testDirectFlightInvalidWhenSelectedCardIsNotDestinationCard() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        Card miamiCard = new Card("Miami", CardType.CITY, miami);

        playerInAtlanta.addCard(chicagoCard);
        playerInAtlanta.addCard(miamiCard);

        DirectFlightAction action = new DirectFlightAction(chicago, miamiCard);

        assertFalse(action.isValid(null, playerInAtlanta),
                "Direct flight must reject a selected card that does not match the destination city");
    }

    @Test
    public void testDirectFlightDoesNotDiscardWrongSelectedCard() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        Card eventCard = new Card("Firewall Lockdown", CardType.EVENT, null);

        playerInAtlanta.addCard(chicagoCard);
        playerInAtlanta.addCard(eventCard);

        DirectFlightAction action = new DirectFlightAction(chicago, eventCard);
        action.execute(null, playerInAtlanta);

        assertEquals(atlanta, playerInAtlanta.getPlayerCurrentCity(),
                "Player should not move if the selected discard card is invalid");
        assertTrue(playerInAtlanta.getHand().contains(chicagoCard),
                "Required destination card should remain in hand");
        assertTrue(playerInAtlanta.getHand().contains(eventCard),
                "Wrong selected card should not be discarded");
    }

    @Test
    public void testDirectFlightExecuteMovesPlayerAndDiscardsCard() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        playerInAtlanta.addCard(chicagoCard);
        new DirectFlightAction(chicago, chicagoCard).execute(null, playerInAtlanta);
        assertEquals(chicago, playerInAtlanta.getPlayerCurrentCity());
        assertFalse(playerInAtlanta.getHand().contains(chicagoCard));
    }

    @Test
    public void testDirectFlightExecuteDoesNotMoveIfInvalid() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        new DirectFlightAction(chicago, chicagoCard).execute(null, playerInAtlanta);
        assertEquals(atlanta, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testDirectFlightHandSizeDecreasesAfterExecute() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        playerInAtlanta.addCard(chicagoCard);
        playerInAtlanta.addCard(new Card("Miami", CardType.CITY, miami));
        new DirectFlightAction(chicago, chicagoCard).execute(null, playerInAtlanta);
        assertEquals(1, playerInAtlanta.getHand().size());
    }

    // =========================================================================
    // CharterFlightAction
    // =========================================================================

    @Test
    public void testCharterFlightValidWhenPlayerHasCurrentCityCard() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        playerInAtlanta.addCard(atlantaCard);
        assertTrue(new CharterFlightAction(london, atlantaCard).isValid(null, playerInAtlanta));
    }

    @Test
    public void testCharterFlightInvalidWhenSelectedCardIsNotCurrentCityCard() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);

        playerInAtlanta.addCard(atlantaCard);
        playerInAtlanta.addCard(chicagoCard);

        CharterFlightAction action = new CharterFlightAction(london, chicagoCard);

        assertFalse(action.isValid(null, playerInAtlanta),
                "Charter flight must reject a selected card that does not match the current city");
    }

    @Test
    public void testCharterFlightDoesNotDiscardWrongSelectedCard() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        Card eventCard = new Card("Firewall Lockdown", CardType.EVENT, null);

        playerInAtlanta.addCard(atlantaCard);
        playerInAtlanta.addCard(eventCard);

        CharterFlightAction action = new CharterFlightAction(london, eventCard);
        action.execute(null, playerInAtlanta);

        assertEquals(atlanta, playerInAtlanta.getPlayerCurrentCity(),
                "Player should not move if the selected discard card is invalid");
        assertTrue(playerInAtlanta.getHand().contains(atlantaCard),
                "Required current-city card should remain in hand");
        assertTrue(playerInAtlanta.getHand().contains(eventCard),
                "Wrong selected card should not be discarded");
    }

    @Test
    public void testCharterFlightInvalidWhenCardMissing() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        assertFalse(new CharterFlightAction(london, atlantaCard).isValid(null, playerInAtlanta));
    }

    @Test
    public void testCharterFlightInvalidWithWrongCityCard() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        playerInAtlanta.addCard(chicagoCard);
        assertFalse(new CharterFlightAction(london, chicagoCard).isValid(null, playerInAtlanta));
    }

    @Test
    public void testCharterFlightInvalidWithNonCityCard() {
        Card eventCard = new Card("Firewall Lockdown", CardType.EVENT, null);
        playerInAtlanta.addCard(eventCard);
        assertFalse(new CharterFlightAction(london, eventCard).isValid(null, playerInAtlanta));
    }

    @Test
    public void testCharterFlightExecuteMovesPlayerAndDiscardsCard() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        playerInAtlanta.addCard(atlantaCard);
        new CharterFlightAction(london, atlantaCard).execute(null, playerInAtlanta);
        assertEquals(london, playerInAtlanta.getPlayerCurrentCity());
        assertFalse(playerInAtlanta.getHand().contains(atlantaCard));
    }

    @Test
    public void testCharterFlightExecuteDoesNotMoveIfInvalid() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        new CharterFlightAction(london, atlantaCard).execute(null, playerInAtlanta);
        assertEquals(atlanta, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testCharterFlightCanFlyToUnconnectedCity() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        playerInAtlanta.addCard(atlantaCard);
        new CharterFlightAction(london, atlantaCard).execute(null, playerInAtlanta);
        assertEquals(london, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testCharterFlightHandSizeDecreasesAfterExecute() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        playerInAtlanta.addCard(atlantaCard);
        playerInAtlanta.addCard(new Card("Miami", CardType.CITY, miami));
        new CharterFlightAction(london, atlantaCard).execute(null, playerInAtlanta);
        assertEquals(1, playerInAtlanta.getHand().size());
    }

    // =========================================================================
    // ShuttleFlightAction
    // =========================================================================

    @Test
    public void testShuttleFlightValidWhenBothCitiesHaveResearchStations() {
        atlanta.addResearchStation();
        chicago.addResearchStation();
        assertTrue(new ShuttleFlightAction(chicago).isValid(null, playerInAtlanta));
    }

    @Test
    public void testShuttleFlightInvalidWhenOriginLacksStation() {
        chicago.addResearchStation();
        assertFalse(new ShuttleFlightAction(chicago).isValid(null, playerInAtlanta));
    }

    @Test
    public void testShuttleFlightInvalidWhenDestinationLacksStation() {
        atlanta.addResearchStation();
        assertFalse(new ShuttleFlightAction(chicago).isValid(null, playerInAtlanta));
    }

    @Test
    public void testShuttleFlightInvalidWhenNeitherHasStation() {
        assertFalse(new ShuttleFlightAction(chicago).isValid(null, playerInAtlanta));
    }

    @Test
    public void testShuttleFlightExecuteMovesPlayer() {
        atlanta.addResearchStation();
        chicago.addResearchStation();
        new ShuttleFlightAction(chicago).execute(null, playerInAtlanta);
        assertEquals(chicago, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testShuttleFlightExecuteDoesNotMoveIfInvalid() {
        new ShuttleFlightAction(chicago).execute(null, playerInAtlanta);
        assertEquals(atlanta, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testShuttleFlightDoesNotDiscardCards() {
        atlanta.addResearchStation();
        chicago.addResearchStation();
        Card card = new Card("Atlanta", CardType.CITY, atlanta);
        playerInAtlanta.addCard(card);
        new ShuttleFlightAction(chicago).execute(null, playerInAtlanta);
        assertEquals(1, playerInAtlanta.getHand().size());
    }

    @Test
    public void testShuttleFlightCanReachUnconnectedCity() {
        atlanta.addResearchStation();
        london.addResearchStation();
        new ShuttleFlightAction(london).execute(null, playerInAtlanta);
        assertEquals(london, playerInAtlanta.getPlayerCurrentCity());
    }

    @Test
    public void testShuttleFlightOnRealMapWithBothStations() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode atl = map.getCity("Atlanta");
        CityNode lon = map.getCity("London");
        atl.addResearchStation();
        lon.addResearchStation();

        PlayerState ps = new PlayerState("Tester");
        ps.setCurrentCity(atl);
        ShuttleFlightAction action = new ShuttleFlightAction(lon);
        assertTrue(action.isValid(null, ps));
        action.execute(null, ps);
        assertEquals(lon, ps.getPlayerCurrentCity());
    }
}
