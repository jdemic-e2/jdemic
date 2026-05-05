package jdemic.GameLogic;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import jdemic.GameLogic.Actions.CharterFlightAction;
import jdemic.GameLogic.Actions.DirectFlightAction;
import jdemic.GameLogic.Actions.DriveFerryAction;
import jdemic.GameLogic.Actions.ShuttleFlightAction;
import jdemic.GameLogic.Card.EventType;
import jdemic.GameLogic.CityNode.DiseaseColor;
import jdemic.GameLogic.ServerRelatedClasses.GameState;
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

    @Before
    public void setUp() {
        atlanta = new CityNode("Atlanta", DiseaseColor.BLUE,   0.25f, 0.39f);
        chicago = new CityNode("Chicago", DiseaseColor.BLUE,   0.24f, 0.30f);
        miami   = new CityNode("Miami",   DiseaseColor.YELLOW, 0.26f, 0.45f);
        london  = new CityNode("London",  DiseaseColor.BLUE,   0.48f, 0.25f);

        // Atlanta <-> Chicago <-> Miami  (London intentionally isolated)
        atlanta.addConnection(chicago);
        chicago.addConnection(miami);

        playerInAtlanta = new PlayerState("Alice", atlanta);
        playerInChicago = new PlayerState("Bob",   chicago);
    }

    @After
    public void tearDown() {
        atlanta = chicago = miami = london = null;
        playerInAtlanta = playerInChicago = null;
    }

    // =========================================================================
    // Helper — build a single-player GameManager with a fresh PlayerState
    // =========================================================================

    private GameManager singlePlayerGame(PlayerState ps) {
        Player player = new Player(ps);
        List<Player> players = new ArrayList<>();
        players.add(player);
        return new GameManager(players);
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
            assertEquals("Initial cubes for " + color + " should be 0",
                    0, atlanta.getCubeCount(color));
        }
    }

    @Test
    public void testConnectedCitiesInitiallyEmpty() {
        // london has no connections set up in setUp()
        assertTrue(london.getConnectedCities().isEmpty());
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
        // atlanta->chicago set in setUp(); verify both directions
        assertTrue(atlanta.getConnectedCities().contains(chicago));
        assertTrue(chicago.getConnectedCities().contains(atlanta));
    }

    @Test
    public void testAddConnectionNoDuplicates() {
        atlanta.addConnection(chicago); // already connected in setUp()
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
            assertEquals(name + " should be BLUE",
                    DiseaseColor.BLUE, map.getCity(name).getNativeColor());
        }
    }

    @Test
    public void testAllRedCitiesAreRed() {
        PandemicMapGraph map = new PandemicMapGraph();
        for (String name : new String[]{"Beijing", "Seoul", "Tokyo", "Shanghai",
                "Hong Kong", "Taipei", "Osaka", "Bangkok",
                "Ho Chi Minh City", "Manila", "Jakarta", "Sydney"}) {
            assertEquals(name + " should be RED",
                    DiseaseColor.RED, map.getCity(name).getNativeColor());
        }
    }

    @Test
    public void testNoCityHasResearchStationBeforeSetup() {
        // Fresh graph — no GameManager has touched it yet
        long count = new PandemicMapGraph().getCityList().stream()
                .filter(CityNode::hasResearchStation).count();
        assertEquals(0, count);
    }

    // =========================================================================
    // DiseaseManager
    // =========================================================================

    @Test
    public void testDiseaseManagerInitialOutbreakScore() {
        assertEquals(0, new DiseaseManager().getOutbreakScore());
    }

    @Test
    public void testDiseaseManagerInitialCubesLeft() {
        assertEquals(0, new DiseaseManager().getInfectionCubesLeft());
    }

    @Test
    public void testDiseaseManagerNotCuredInitially() {
        assertFalse(new DiseaseManager().isCured());
    }

    @Test
    public void testDiseaseManagerDiscoverCureNoException() {
        new DiseaseManager().discoverCure();
    }

    @Test
    public void testDiseaseManagerAddRemoveCubesNoException() {
        DiseaseManager dm = new DiseaseManager();
        dm.addInfectionCubes(10);
        dm.removeInfectionCubes(5);
    }

    @Test
    public void testDiseaseManagerIncreaseOutbreakScoreNoException() {
        new DiseaseManager().increaseOutbreakScore();
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
        assertTrue("Bob's hand should still be empty",
                playerInChicago.getHand().isEmpty());
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
        assertTrue(state.getPlayerStates().isEmpty());
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
        assertEquals(1, state.getPlayerStates().size());
        assertEquals(playerInAtlanta, state.getPlayerStates().get(0));
    }

    @Test
    public void testGameStateAddMultiplePlayerStates() {
        GameState state = new GameState();
        state.addPlayer(playerInAtlanta);
        state.addPlayer(playerInChicago);
        assertEquals(2, state.getPlayerStates().size());
    }

    @Test
    public void testGameStateRemovePlayerState() {
        GameState state = new GameState();
        state.addPlayer(playerInAtlanta);
        state.addPlayer(playerInChicago);
        state.removePlayer(playerInAtlanta);
        assertEquals(1, state.getPlayerStates().size());
        assertFalse(state.getPlayerStates().contains(playerInAtlanta));
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
        GameState state = new GameState();
        DiseaseManager dm = new DiseaseManager();
        state.setDiseaseManager(dm);
        assertEquals(dm, state.getDiseaseManager());
    }

    @Test
    public void testGameStateSetPlayers() {
        GameState state = new GameState();
        Player p = new Player(playerInAtlanta);
        state.setPlayers(List.of(p));
        assertEquals(1, state.getPlayers().size());
        assertEquals(p, state.getPlayers().get(0));
    }

    // =========================================================================
    // Player
    // =========================================================================

    @Test
    public void testPlayerInitialization() {
        Player player = new Player(playerInAtlanta);
        assertEquals(playerInAtlanta, player.getState());
    }

    @Test
    public void testPlayerSyncStateFromServer() {
        Player player = new Player(playerInAtlanta);
        player.syncStateFromServer(playerInChicago);
        assertEquals(playerInChicago, player.getState());
    }

    @Test
    public void testPlayerDiscardCardReducesHandSize() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.getState().getCardDeck().drawHand(playerInAtlanta);

        if (!playerInAtlanta.getHand().isEmpty()) {
            int before = playerInAtlanta.getHand().size();
            gm.getCurrentPlayer().discardCard(0);
            assertEquals(before - 1, playerInAtlanta.getHand().size());
        }
    }

    @Test
    public void testPlayerDrawCardsClearsDiscardFlag() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.getCurrentPlayer().drawCards(gm.getState().getCardDeck());
        assertFalse(playerInAtlanta.getIsDiscarding());
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
    public void testAtlantaHasResearchStationAfterSetup() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertTrue(gm.getState().getMap().getCity("Atlanta").hasResearchStation());
    }

    @Test
    public void testGameManagerGetCurrentPlayer() {
        Player player = new Player(playerInAtlanta);
        List<Player> players = new ArrayList<>();
        players.add(player);
        GameManager gm = new GameManager(players);
        assertEquals(player, gm.getCurrentPlayer());
    }

    @Test
    public void testGameManagerInitialInfectionRateIsTwo() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        assertEquals(2, gm.getInfectionRate());
    }

    @Test
    public void testGameManagerInfectionRateIncreasesToThree() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        // Track: {2,2,2,3,3,4,4} — index 3 gives 3
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
    public void testCheckLoseConditionDoesNotTriggerInitially() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.checkLoseCondition();
        assertFalse(gm.isGameOver());
    }

    @Test
    public void testNextTurnAdvancesPlayerIndex() {
        PlayerState ps2 = new PlayerState("Bob", chicago);
        Player p1 = new Player(playerInAtlanta);
        Player p2 = new Player(ps2);
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        GameManager gm = new GameManager(players);

        assertEquals(p1, gm.getCurrentPlayer());
        gm.nextTurn();
        assertEquals(p2, gm.getCurrentPlayer());
    }

    @Test
    public void testNextTurnWrapsAroundToFirstPlayer() {
        PlayerState ps2 = new PlayerState("Bob", chicago);
        Player p1 = new Player(playerInAtlanta);
        Player p2 = new Player(ps2);
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        GameManager gm = new GameManager(players);

        gm.nextTurn();
        gm.nextTurn();
        assertEquals(p1, gm.getCurrentPlayer());
    }

    @Test
    public void testPerformActionIsNoOpWhenGameOver() {
        GameManager gm = singlePlayerGame(playerInAtlanta);
        gm.getState().setGameOver(true);
        gm.performAction(gm.getCurrentPlayer(), null); // must not throw
    }

    // =========================================================================
    // DriveFerryAction
    // =========================================================================

    @Test
    public void testDriveFerryValidWhenConnected() {
        assertTrue(new DriveFerryAction(chicago).isValid(playerInAtlanta));
    }

    @Test
    public void testDriveFerryInvalidWhenNotConnected() {
        // Atlanta -> Miami requires two hops
        assertFalse(new DriveFerryAction(miami).isValid(playerInAtlanta));
    }

    @Test
    public void testDriveFerryInvalidForCurrentCity() {
        assertFalse(new DriveFerryAction(atlanta).isValid(playerInAtlanta));
    }

    @Test
    public void testDriveFerryInvalidForIsolatedCity() {
        assertFalse(new DriveFerryAction(london).isValid(playerInAtlanta));
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
    public void testDriveFerryGameStateOverloadReturnsFalse() {
        assertFalse(new DriveFerryAction(chicago).isValid(new GameState()));
    }

    @Test
    public void testDriveFerryOnRealMapAtlantaToChicago() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode atl = map.getCity("Atlanta");
        CityNode chi = map.getCity("Chicago");
        PlayerState ps = new PlayerState("Tester", atl);

        DriveFerryAction action = new DriveFerryAction(chi);
        assertTrue(action.isValid(ps));
        action.execute(null, ps);
        assertEquals(chi, ps.getPlayerCurrentCity());
    }

    @Test
    public void testDriveFerryOnRealMapAtlantaToTokyoInvalid() {
        PandemicMapGraph map = new PandemicMapGraph();
        PlayerState ps = new PlayerState("Tester", map.getCity("Atlanta"));
        assertFalse(new DriveFerryAction(map.getCity("Tokyo")).isValid(ps));
    }

    // =========================================================================
    // DirectFlightAction
    // =========================================================================

    @Test
    public void testDirectFlightValidWhenCardInHand() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        playerInAtlanta.addCard(chicagoCard);
        assertTrue(new DirectFlightAction(chicago, chicagoCard).isValid(playerInAtlanta));
    }

    @Test
    public void testDirectFlightInvalidWhenCardNotInHand() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        assertFalse(new DirectFlightAction(chicago, chicagoCard).isValid(playerInAtlanta));
    }

    @Test
    public void testDirectFlightInvalidWithWrongCityCard() {
        Card miamiCard = new Card("Miami", CardType.CITY, miami);
        playerInAtlanta.addCard(miamiCard);
        assertFalse(new DirectFlightAction(chicago, miamiCard).isValid(playerInAtlanta));
    }

    @Test
    public void testDirectFlightInvalidWithNonCityCard() {
        Card epidemic = new Card("System Breach", CardType.EPIDEMIC, null);
        playerInAtlanta.addCard(epidemic);
        assertFalse(new DirectFlightAction(chicago, epidemic).isValid(playerInAtlanta));
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

    @Test
    public void testDirectFlightGameStateOverloadReturnsFalse() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        assertFalse(new DirectFlightAction(chicago, chicagoCard).isValid(new GameState()));
    }

    // =========================================================================
    // CharterFlightAction
    // =========================================================================

    @Test
    public void testCharterFlightValidWhenPlayerHasCurrentCityCard() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        playerInAtlanta.addCard(atlantaCard);
        assertTrue(new CharterFlightAction(london, atlantaCard).isValid(playerInAtlanta));
    }

    @Test
    public void testCharterFlightInvalidWhenCardMissing() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        assertFalse(new CharterFlightAction(london, atlantaCard).isValid(playerInAtlanta));
    }

    @Test
    public void testCharterFlightInvalidWithWrongCityCard() {
        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        playerInAtlanta.addCard(chicagoCard);
        assertFalse(new CharterFlightAction(london, chicagoCard).isValid(playerInAtlanta));
    }

    @Test
    public void testCharterFlightInvalidWithNonCityCard() {
        Card eventCard = new Card("Firewall Lockdown", CardType.EVENT, null);
        playerInAtlanta.addCard(eventCard);
        assertFalse(new CharterFlightAction(london, eventCard).isValid(playerInAtlanta));
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

    @Test
    public void testCharterFlightGameStateOverloadReturnsFalse() {
        Card atlantaCard = new Card("Atlanta", CardType.CITY, atlanta);
        assertFalse(new CharterFlightAction(london, atlantaCard).isValid(new GameState()));
    }

    // =========================================================================
    // ShuttleFlightAction
    // =========================================================================

    @Test
    public void testShuttleFlightValidWhenBothCitiesHaveResearchStations() {
        atlanta.addResearchStation();
        chicago.addResearchStation();
        assertTrue(new ShuttleFlightAction(chicago).isValid(playerInAtlanta));
    }

    @Test
    public void testShuttleFlightInvalidWhenOriginLacksStation() {
        chicago.addResearchStation();
        assertFalse(new ShuttleFlightAction(chicago).isValid(playerInAtlanta));
    }

    @Test
    public void testShuttleFlightInvalidWhenDestinationLacksStation() {
        atlanta.addResearchStation();
        assertFalse(new ShuttleFlightAction(chicago).isValid(playerInAtlanta));
    }

    @Test
    public void testShuttleFlightInvalidWhenNeitherHasStation() {
        assertFalse(new ShuttleFlightAction(chicago).isValid(playerInAtlanta));
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
    public void testShuttleFlightGameStateOverloadReturnsFalse() {
        assertFalse(new ShuttleFlightAction(chicago).isValid(new GameState()));
    }

    @Test
    public void testShuttleFlightOnRealMapWithBothStations() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode atl = map.getCity("Atlanta");
        CityNode lon = map.getCity("London");
        atl.addResearchStation();
        lon.addResearchStation();

        PlayerState ps = new PlayerState("Tester", atl);
        ShuttleFlightAction action = new ShuttleFlightAction(lon);
        assertTrue(action.isValid(ps));
        action.execute(null, ps);
        assertEquals(lon, ps.getPlayerCurrentCity());
    }
}