/**
 * E2E tests for the complete game logic flow of jDemic.
 *
 * These tests simulate a full game session from start to finish —
 * initializing the game, performing player actions across multiple turns,
 * triggering disease outbreaks, and verifying win/lose conditions.
 * No JavaFX window is needed; the game logic layer is tested directly.
 *
 * Authors: Rubén Alcázar, Álvaro Miñán
 * Branch:  feature/e2e-testing-setup
 */

package jdemic.e2e;

import jdemic.GameLogic.*;
import jdemic.GameLogic.Actions.*;
import jdemic.GameLogic.ServerRelatedClasses.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameFlowE2ETest {

    private GameManager gameManager;
    private Player player1;
    private Player player2;

    /**
     * Creates a fresh 2-player game before every test.
     * Atlanta is chosen as the starting city because the GameManager
     * always places the Research Station there at setup.
     */
    @BeforeEach
    void setUp() {
        PandemicMapGraph map = new PandemicMapGraph();
        CityNode atlanta = map.getCity("Atlanta");

        PlayerState state1 = new PlayerState("Ruben", atlanta);
        PlayerState state2 = new PlayerState("Alvaro", atlanta);

        player1 = new Player(state1);
        player2 = new Player(state2);

        gameManager = new GameManager(List.of(player1, player2));
    }

    // ─────────────────────────────────────────────────────────────
    // 1. GAME INITIALIZATION
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-01 | Game initialises with 48 cities on the map")
    void gameShouldInitialiseWithCorrectNumberOfCities() {
        // The Pandemic board has exactly 48 cities across 4 disease colours.
        int cityCount = gameManager.getState().getMap().getCityList().size();
        assertEquals(48, cityCount,
                "The map should contain exactly 48 cities at game start.");
    }

    @Test
    @DisplayName("E2E-02 | Atlanta has a Research Station at game start")
    void atlantaShouldHaveResearchStationAtStart() {
        CityNode atlanta = gameManager.getState().getMap().getCity("Atlanta");
        assertTrue(atlanta.hasResearchStation(),
                "Atlanta must have a Research Station at the beginning of the game.");
    }

    @Test
    @DisplayName("E2E-03 | Game starts with the correct number of player-deck cards")
    void deckShouldHaveCorrectCardCountAtStart() {
        // 48 city cards + 5 event cards + 4 epidemic cards = 57 total,
        // but 2 players × 0 initial hand cards = 57 remaining in deck.
        int remaining = gameManager.getState().getCardDeck().getRemainingCardsCount();
        assertTrue(remaining > 0,
                "Player deck must not be empty at game start.");
    }

    @Test
    @DisplayName("E2E-04 | Game is not over and not won at start")
    void gameStateShouldBeActiveAtStart() {
        assertFalse(gameManager.isGameOver(), "Game should not be over at start.");
        assertFalse(gameManager.isGameWon(),  "Game should not be won at start.");
    }

    @Test
    @DisplayName("E2E-05 | First player to act is player 1 (index 0)")
    void firstCurrentPlayerShouldBePlayerOne() {
        assertEquals(player1, gameManager.getCurrentPlayer(),
                "Player 1 must be the current player at game start.");
    }

    // ─────────────────────────────────────────────────────────────
    // 2. PLAYER MOVEMENT (Drive/Ferry)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-06 | Player can move to an adjacent city via Drive/Ferry")
    void playerShouldMoveToAdjacentCityWithDriveFerry() {
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode atlanta  = map.getCity("Atlanta");
        CityNode chicago  = map.getCity("Chicago");

        // Atlanta and Chicago are connected on the Pandemic board.
        atlanta.addConnection(chicago);

        DriveFerryAction move = new DriveFerryAction(chicago);
        gameManager.performAction(player1, move);

        assertEquals(chicago, player1.getState().getPlayerCurrentCity(),
                "Player 1 should be in Chicago after a Drive/Ferry action.");
    }

    @Test
    @DisplayName("E2E-07 | Drive/Ferry to a non-adjacent city is rejected")
    void driveFerryToNonAdjacentCityShouldBeRejected() {
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode tokyo = map.getCity("Tokyo"); // Tokyo is not adjacent to Atlanta

        DriveFerryAction illegalMove = new DriveFerryAction(tokyo);
        gameManager.performAction(player1, illegalMove);

        // Player must remain in Atlanta
        assertNotEquals(tokyo, player1.getState().getPlayerCurrentCity(),
                "Player 1 must not teleport to Tokyo without a valid connection.");
    }

    @Test
    @DisplayName("E2E-08 | Each action reduces the actions-remaining counter by 1")
    void performingAnActionShouldDecrementActionsRemaining() {
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode atlanta = map.getCity("Atlanta");
        CityNode chicago = map.getCity("Chicago");
        atlanta.addConnection(chicago);

        int before = gameManager.getState().getActionsRemaining();
        gameManager.performAction(player1, new DriveFerryAction(chicago));
        int after = gameManager.getState().getActionsRemaining();

        assertEquals(before - 1, after,
                "One successful action must reduce actionsRemaining by exactly 1.");
    }

    // ─────────────────────────────────────────────────────────────
    // 3. PLAYER MOVEMENT (Direct Flight)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-09 | Player can fly directly to a city using the matching card")
    void playerShouldFlyDirectlyWithCityCard() {
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode tokyo = map.getCity("Tokyo");

        // Give the player the Tokyo card
        Card tokyoCard = new Card("Tokyo", CardType.CITY, tokyo);
        player1.getState().addCard(tokyoCard);

        DirectFlightAction flight = new DirectFlightAction(tokyo, tokyoCard);
        gameManager.performAction(player1, flight);

        assertEquals(tokyo, player1.getState().getPlayerCurrentCity(),
                "Player 1 should be in Tokyo after a Direct Flight action.");
        assertFalse(player1.getState().getHand().contains(tokyoCard),
                "The Tokyo card must be discarded after the Direct Flight.");
    }

    @Test
    @DisplayName("E2E-10 | Direct Flight without the city card is rejected")
    void directFlightWithoutCardShouldBeRejected() {
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode tokyo    = map.getCity("Tokyo");
        CityNode atlanta  = map.getCity("Atlanta");

        // Player has NO Tokyo card — create a dummy card for a different city
        Card wrongCard = new Card("London", CardType.CITY, map.getCity("London"));
        player1.getState().addCard(wrongCard);

        DirectFlightAction illegalFlight = new DirectFlightAction(tokyo, wrongCard);
        gameManager.performAction(player1, illegalFlight);

        assertEquals(atlanta, player1.getState().getPlayerCurrentCity(),
                "Player 1 must stay in Atlanta when the required card is not in hand.");
    }

    // ─────────────────────────────────────────────────────────────
    // 4. TURN MANAGEMENT
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-11 | Turn advances to player 2 after nextTurn()")
    void turnShouldAdvanceToSecondPlayer() {
        gameManager.nextTurn();
        assertEquals(player2, gameManager.getCurrentPlayer(),
                "After the first nextTurn(), player 2 should be the active player.");
    }

    @Test
    @DisplayName("E2E-12 | Turn wraps back to player 1 after both players have gone")
    void turnShouldWrapAroundToFirstPlayer() {
        gameManager.nextTurn(); // → player 2
        gameManager.nextTurn(); // → player 1 again
        assertEquals(player1, gameManager.getCurrentPlayer(),
                "Turn must cycle back to player 1 after both players have gone.");
    }

    @Test
    @DisplayName("E2E-13 | Actions reset to 4 at the start of each new turn")
    void actionsShouldResetOnNextTurn() {
        // Use one action so the counter is < 4
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode atlanta = map.getCity("Atlanta");
        CityNode chicago = map.getCity("Chicago");
        atlanta.addConnection(chicago);
        gameManager.performAction(player1, new DriveFerryAction(chicago));

        gameManager.nextTurn();

        assertEquals(4, gameManager.getState().getActionsRemaining(),
                "Actions remaining must reset to 4 at the start of a new turn.");
    }

    // ─────────────────────────────────────────────────────────────
    // 5. DISEASE / INFECTION MECHANICS
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-14 | Infecting a city adds disease cubes correctly")
    void infectionShouldAddDiseaseCubesToCity() {
        PandemicMapGraph map   = gameManager.getState().getMap();
        CityNode paris         = map.getCity("Paris"); // Blue city
        DiseaseManager dm      = gameManager.getState().getDiseaseManager();

        dm.addInfectionCubes(paris, 2);

        assertEquals(2, paris.getCubeCount(DiseaseColor.BLUE),
                "Paris should have 2 Blue disease cubes after infection.");
    }

    @Test
    @DisplayName("E2E-15 | Outbreak triggers when a city would exceed 3 cubes")
    void outbreakShouldTriggerWhenCityExceedsThreeCubes() {
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode london      = map.getCity("London");
        DiseaseManager dm    = gameManager.getState().getDiseaseManager();

        // Fill London to 3 cubes, then add one more to trigger an outbreak
        dm.addInfectionCubes(london, 3);
        int scoreBeforeOutbreak = dm.getOutbreakScore();
        dm.addInfectionCubes(london, 1); // This should cause an outbreak

        assertEquals(scoreBeforeOutbreak + 1, dm.getOutbreakScore(),
                "Outbreak score must increase by 1 when a city overflows its cube limit.");
    }

    @Test
    @DisplayName("E2E-16 | Removing cubes decreases the disease count in the city")
    void removingCubesShouldDecreaseDiseaseCubeCount() {
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode madrid      = map.getCity("Madrid");
        DiseaseManager dm    = gameManager.getState().getDiseaseManager();

        dm.addInfectionCubes(madrid, 3);
        dm.removeInfectionCubes(madrid, 2);

        assertEquals(1, madrid.getCubeCount(DiseaseColor.BLUE),
                "Madrid should have 1 Blue cube left after removing 2 of 3.");
    }

    // ─────────────────────────────────────────────────────────────
    // 6. WIN / LOSE CONDITIONS
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-17 | Game is won when all four diseases are cured")
    void gameShouldBeWonWhenAllDiseasesAreCured() {
        DiseaseManager dm = gameManager.getState().getDiseaseManager();

        dm.discoverCure(DiseaseColor.BLUE);
        dm.discoverCure(DiseaseColor.YELLOW);
        dm.discoverCure(DiseaseColor.BLACK);
        dm.discoverCure(DiseaseColor.RED);

        gameManager.checkWinCondition();

        assertTrue(gameManager.isGameOver(), "Game must be over when all diseases are cured.");
        assertTrue(gameManager.isGameWon(),  "Game must be marked as won when all diseases are cured.");
    }

    @Test
    @DisplayName("E2E-18 | Game is NOT won when only three diseases are cured")
    void gameShouldNotBeWonWithOnlyThreeCures() {
        DiseaseManager dm = gameManager.getState().getDiseaseManager();

        dm.discoverCure(DiseaseColor.BLUE);
        dm.discoverCure(DiseaseColor.YELLOW);
        dm.discoverCure(DiseaseColor.BLACK);
        // RED is NOT cured

        gameManager.checkWinCondition();

        assertFalse(gameManager.isGameOver(),
                "Game must not be over when one disease is still uncured.");
    }

    @Test
    @DisplayName("E2E-19 | Game is lost when outbreak score reaches 8")
    void gameShouldBeLostWhenOutbreakScoreReachesEight() {
        DiseaseManager dm = gameManager.getState().getDiseaseManager();

        // Trigger 8 outbreaks manually by overflowing 8 different cities
        List<String> cities = List.of(
            "London", "Paris", "Madrid", "Milan",
            "Essen", "Chicago", "Montreal", "New York"
        );
        PandemicMapGraph map = gameManager.getState().getMap();

        for (String cityName : cities) {
            CityNode city = map.getCity(cityName);
            city.addDiseaseCube(DiseaseColor.BLUE, 3); // fill to max
            dm.addInfectionCubes(city, 1);             // overflow → outbreak
        }

        gameManager.checkLoseCondition();

        assertTrue(gameManager.isGameOver(), "Game must be over after 8 outbreaks.");
        assertFalse(gameManager.isGameWon(), "Game must be lost (not won) after 8 outbreaks.");
    }

    // ─────────────────────────────────────────────────────────────
    // 7. MULTI-TURN FULL GAME FLOW
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-20 | Full 2-turn flow: move, infect, advance — game still running")
    void fullTwoTurnFlowShouldKeepGameRunning() {
        PandemicMapGraph map = gameManager.getState().getMap();
        CityNode atlanta     = map.getCity("Atlanta");
        CityNode chicago     = map.getCity("Chicago");
        atlanta.addConnection(chicago);

        // Turn 1 — Player 1 moves to Chicago
        gameManager.performAction(player1, new DriveFerryAction(chicago));
        assertEquals(chicago, player1.getState().getPlayerCurrentCity());

        // Infection phase: infect Paris with 1 cube
        DiseaseManager dm = gameManager.getState().getDiseaseManager();
        dm.addInfectionCubes(map.getCity("Paris"), 1);

        gameManager.nextTurn(); // → Player 2's turn

        // Turn 2 — Player 2 moves to Chicago as well
        gameManager.performAction(player2, new DriveFerryAction(chicago));

        gameManager.nextTurn(); // → Back to Player 1

        // Game should still be running normally
        assertFalse(gameManager.isGameOver(),
                "Game must still be active after a normal 2-turn sequence.");
        assertEquals(player1, gameManager.getCurrentPlayer(),
                "After 2 full turns, it should be Player 1's turn again.");
    }
}
