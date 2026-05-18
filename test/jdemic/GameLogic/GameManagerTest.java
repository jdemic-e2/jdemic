package jdemic.GameLogic;

import jdemic.GameLogic.Actions.Movement.DirectFlightAction;
import jdemic.GameLogic.Actions.Movement.DriveFerryAction;
import jdemic.GameLogic.Actions.Movement.ShuttleFlightAction;
import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameManagerTest {

    @Test
    void shouldInitializeGameStateForPlayers() {
        GameManager manager = newManager("Ruben", "Alvaro");
        GameState state = manager.getState();

        assertEquals(2, state.getPlayers().size());
        assertEquals(4, state.getActionsRemaining());
        assertEquals(0, state.getCurrentPlayerIndex());
        assertEquals(2, manager.getInfectionRate());
        assertTrue(state.getMap().getCity("Atlanta").hasResearchStation());
        assertFalse(state.isGameOver());
        assertFalse(state.isGameWon());
        assertSame(state.getPlayers().get(0), manager.getCurrentPlayer());
    }

    @Test
    void validDriveFerryShouldMovePlayerAndConsumeOneAction() {
        GameManager manager = newManager("Ruben");
        PlayerState playerState = manager.getCurrentPlayer();
        Player player = new Player(playerState, null);
        CityNode atlanta = manager.getState().getMap().getCity("Atlanta");
        CityNode chicago = manager.getState().getMap().getCity("Chicago");
        playerState.setCurrentCity(atlanta);

        manager.performAction(player, new DriveFerryAction(chicago));

        assertSame(chicago, playerState.getPlayerCurrentCity());
        assertEquals(3, manager.getState().getActionsRemaining());
    }

    @Test
    void invalidDriveFerryShouldNotMoveOrConsumeAction() {
        GameManager manager = newManager("Ruben");
        PlayerState playerState = manager.getCurrentPlayer();
        Player player = new Player(playerState, null);
        CityNode atlanta = manager.getState().getMap().getCity("Atlanta");
        CityNode london = manager.getState().getMap().getCity("London");
        playerState.setCurrentCity(atlanta);

        manager.performAction(player, new DriveFerryAction(london));

        assertSame(atlanta, playerState.getPlayerCurrentCity());
        assertEquals(4, manager.getState().getActionsRemaining());
    }

    @Test
    void nextTurnShouldDrawCardsAdvancePlayerAndResetActions() {
        GameManager manager = newManager("Ruben", "Alvaro");
        GameState state = manager.getState();
        state.setActionsRemaining(0);
        int startingCards = state.getCardDeck().getRemainingCardsCount();

        manager.nextTurn();

        assertEquals(1, state.getCurrentPlayerIndex());
        assertEquals(4, state.getActionsRemaining());
        assertEquals(2, state.getPlayers().get(0).getHand().size());
        assertEquals(startingCards - 2, state.getCardDeck().getRemainingCardsCount());
    }

    @Test
    void directAndShuttleFlightsShouldValidateRequiredCardsAndResearchStations() {
        GameManager manager = newManager("Ruben");
        PlayerState playerState = manager.getCurrentPlayer();
        Player player = new Player(playerState, null);
        CityNode atlanta = manager.getState().getMap().getCity("Atlanta");
        CityNode chicago = manager.getState().getMap().getCity("Chicago");
        playerState.setCurrentCity(atlanta);

        Card chicagoCard = new Card("Chicago", CardType.CITY, chicago);
        playerState.addCard(chicagoCard);

        manager.performAction(player, new DirectFlightAction(chicago, chicagoCard));

        assertSame(chicago, playerState.getPlayerCurrentCity());
        assertEquals(3, manager.getState().getActionsRemaining());

        chicago.addResearchStation();
        manager.performAction(player, new ShuttleFlightAction(atlanta));

        assertSame(atlanta, playerState.getPlayerCurrentCity());
        assertEquals(2, manager.getState().getActionsRemaining());
    }

    private GameManager newManager(String... playerNames) {
        List<PlayerState> players = java.util.Arrays.stream(playerNames)
                .map(PlayerState::new)
                .toList();
        return new GameManager(players);
    }
}
