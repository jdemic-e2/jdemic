package jdemic.GameLogic;
import java.util.List;

import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class GameManager {
    GameState state;

    private static final int ACTIONS_PER_TURN = 4;
    private static final int[] INFECTION_RATE_TRACK = {2, 2, 2, 3, 3, 4, 4};
    private static final int MAX_OUTBREAKS = 8;

    public GameManager(List<PlayerState> players) {
        this.state = new GameState();
        state.setMap(new PandemicMapGraph());
        state.setDiseaseManager(new DiseaseManager(this));
        state.setCardDeck(new Deck(this));
        state.setCurrentPlayerIndex(0);
        state.setActionsRemaining(ACTIONS_PER_TURN);
        state.setInfectionRate(0);
        state.setEpidemicCount(0);
        state.setGameOver(false);
        state.setGameWon(false);
        for (PlayerState player : players) {
            state.addPlayer(player);
        }
        setupGame();
    }

    private void setupGame() {
        CityNode atlanta = state.getMap().getCity("Atlanta");
        atlanta.addResearchStation();
        for (PlayerState player : state.getPlayers()) {
            player.setCurrentCity(atlanta);
        }
    }

    public void startGame() {
        if (state.isGameStarted()) return;

        setupGame();
        for (PlayerState playerState : state.getPlayers()) {
            playerState.setReady(false);
            playerState.setPlayer(new Player(playerState, null));
        }
        state.setCurrentPlayerIndex(0);
        state.setActionsRemaining(ACTIONS_PER_TURN);
        state.setLobbyCountdownStartedAt(0);
        state.setGameStarted(true);
    }

    public void performAction(Player player, GameAction action) {
        if (state.isGameOver()) return;
        if (state.getActionsRemaining() <= 0) return;

        if (action.isValid(state, player.getState())) {
            action.execute(state, player.getState());
            state.setActionsRemaining(state.getActionsRemaining() - 1);
        }
    }

    /**
     * Completes the current player's turn in the correct Pandemic sequence:
     *
     *  1. Draw 2 player cards (epidemic resolves inside Deck.drawHand if triggered).
     *  2. Infect cities according to the current infection rate.
     *  3. Check win/lose conditions.
     *  4. Advance to the next player.
     *
     * PacketProcessor calls this after the player has used all 4 actions
     * (or explicitly sends END_TURN).
     */
    public void nextTurn() {
        if (state.isGameOver()) return;

        // ── Phase 2: Draw player cards ────────────────────────────────────────
        // drawHand internally handles epidemic cards (Increase → Infect → Intensify)
        PlayerState currentPlayer = getCurrentPlayer();
        state.getCardDeck().drawHand(currentPlayer);

        // Drawing may have emptied the deck → lose
        if (state.getCardDeck().getRemainingCardsCount() <= 0) {
            state.setGameOver(true);
            state.setGameWon(false);
            return;
        }

        // ── Phase 3: Infect cities ────────────────────────────────────────────
        infectCities();

        // ── Check conditions after infection ──────────────────────────────────
        checkWinCondition();
        checkLoseCondition();
        if (state.isGameOver()) return;

        // ── Advance turn ──────────────────────────────────────────────────────
        state.setCurrentPlayerIndex(
                (state.getCurrentPlayerIndex() + 1) % state.getPlayers().size());
        state.setActionsRemaining(ACTIONS_PER_TURN);
    }

    /**
     * Draws N infection cards from the top of the infection deck (N = current
     * infection rate) and adds 1 disease cube of the city's native color to
     * each drawn city. Each drawn card goes to the infection discard pile.
     *
     * Outbreaks (city already at 3 cubes) are handled inside
     * DiseaseManager.addInfectionCubes().
     */
    public void infectCities() {
        if (state.isGameOver()) return;

        Deck deck = state.getCardDeck();
        DiseaseManager dm = state.getDiseaseManager();
        List<Card> infectionDeck = deck.getInfectionCards();
        int rate = getInfectionRate();

        for (int i = 0; i < rate; i++) {
            if (infectionDeck.isEmpty()) {
                // No infection cards left is not a standard lose condition,
                // but check anyway to be safe.
                checkLoseCondition();
                break;
            }

            // Draw from the top of the infection deck
            Card drawn = infectionDeck.remove(0);
            CityNode city = drawn.getTargetCity();

            System.out.println("[GameManager] Infecting city: " + city.getName()
                    + " (" + city.getNativeColor() + ")");

            dm.addInfectionCubes(city, 1);
            deck.getInfectionDiscardPile().add(drawn);

            // An outbreak may have just pushed us over the limit
            if (state.isGameOver()) return;
        }
    }

    public void checkWinCondition() {
        if (state.getDiseaseManager().areAllCured()) {
            state.setGameOver(true);
            state.setGameWon(true);
        }
    }

    public void checkLoseCondition() {
        if (state.getDiseaseManager().getOutbreakScore() >= MAX_OUTBREAKS) {
            state.setGameOver(true);
            state.setGameWon(false);
        }

        if (state.getCardDeck().getRemainingCardsCount() <= 0) {
            state.setGameOver(true);
            state.setGameWon(false);
        }
    }

    public int getInfectionRate() {
        if (state.getInfectionRate() >= INFECTION_RATE_TRACK.length)
            return INFECTION_RATE_TRACK[INFECTION_RATE_TRACK.length - 1];
        return INFECTION_RATE_TRACK[state.getInfectionRate()];
    }

    public int[] getInfectionRateTrack() {
        return INFECTION_RATE_TRACK;
    }

    public void increaseInfectionRate() {
        state.setInfectionRate(state.getInfectionRate() + 1);
    }

    public PlayerState getCurrentPlayer() {
        return state.getPlayers().get(state.getCurrentPlayerIndex());
    }

    public GameState getState() {
        return this.state;
    }

    public boolean isGameOver() {
        return state.isGameOver();
    }

    public boolean isGameWon() {
        return state.isGameWon();
    }
}