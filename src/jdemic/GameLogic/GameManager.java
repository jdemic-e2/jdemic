package jdemic.GameLogic;
import java.util.List;

import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

public class GameManager {
    GameState state;
    private final Object stateLock = new Object();
    private static final int ACTIONS_PER_TURN = 4;
    private static final int HAND_LIMIT = 7;
    private static final int[] INFECTION_RATE_TRACK = {2, 2, 2, 3, 3, 4, 4};
    private static final int MAX_OUTBREAKS = 8;
    private boolean initialHandsDealt;
    private boolean epidemicsAdded;

    public GameManager(List<PlayerState> players) {
        this(players, true);
    }

    public GameManager(List<PlayerState> players, boolean dealInitialHands) {
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
        if (dealInitialHands) {
            dealInitialHands();
            addEpidemicsAfterInitialDeal();
        }
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
        RoleManager.assignRandomRoles(state.getPlayers());
        for(PlayerState playerState : state.getPlayers())
        {
            playerState.setReady(false);
            playerState.setPlayer(new Player(playerState, null));
        }
        state.setCurrentPlayerIndex(0);
        state.setActionsRemaining(ACTIONS_PER_TURN);
        state.setLobbyCountdownStartedAt(0);
        dealInitialHands();
        addEpidemicsAfterInitialDeal();
        state.setGameStarted(true);
    }

    private void dealInitialHands()
    {
        if(initialHandsDealt || state.getPlayers().isEmpty()) return;

        int cardsPerPlayer = getInitialHandSize(state.getPlayers().size());
        for(PlayerState playerState : state.getPlayers())
        {
            state.getCardDeck().drawInitialHand(playerState, cardsPerPlayer);
            if(state.isGameOver()) return;
        }

        initialHandsDealt = true;
    }

    private int getInitialHandSize(int playerCount)
    {
        if(playerCount < 2) return 0;
        if(playerCount == 2) return 4;
        if(playerCount == 3) return 3;
        return 2;
    }

    private void addEpidemicsAfterInitialDeal() {
        if(!epidemicsAdded && state.getCardDeck() != null) {
            state.getCardDeck().addEpidemicCards(4);
            epidemicsAdded = true;
        }
    }

    public void performAction(Player player, GameAction action)
    {
        if(state.isGameOver()) return;
        if(state.getActionsRemaining() <= 0) return;
        if(state.getCurrentPlayer() != null && state.getCurrentPlayer().getIsDiscarding()) return;
        
        // Only allow the current player to perform actions
        if(!state.isPlayerTurn(player.getState())) return;

        if (action.isValid(state, player.getState())) {
            action.execute(state, player.getState());
            state.setActionsRemaining(state.getActionsRemaining() - 1);
            
            // Automatically advance to next turn when actions reach 0
            if(state.getActionsRemaining() <= 0){
                nextTurn();
            }
        }
    }

    public void consumeAction(PlayerState playerState)
    {
        if(state.isGameOver()) return;
        if(state.getActionsRemaining() <= 0) return;
        if(state.getCurrentPlayer() != null && state.getCurrentPlayer().getIsDiscarding()) return;
        if(!state.isPlayerTurn(playerState)) return;

        state.setActionsRemaining(state.getActionsRemaining() - 1);

        if(state.getActionsRemaining() <= 0){
            nextTurn();
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

        PlayerState currentPlayer = state.getCurrentPlayer();
        if(currentPlayer == null) return;

        if(currentPlayer.getIsDiscarding())
        {
            if(currentPlayer.getHand().size() <= HAND_LIMIT) {
                currentPlayer.setIsDiscarding(false);
                advanceToNextPlayer();
            }
            return;
        }

        state.getCardDeck().drawHand(currentPlayer);
        if(state.isGameOver()) return;

        checkWinCondition();
        checkLoseCondition();
        if (state.isGameOver()) return;

        if(state.isGameOver()) return;

        if(currentPlayer.getHand().size() > HAND_LIMIT)
        {
            currentPlayer.setIsDiscarding(true);
            state.setActionsRemaining(0);
            return;
        }

        if(state.isSkipInfection()) {
            state.setSkipInfection(false);
        } else {
            infectCities();
            if(state.isGameOver()) return;
        }

        advanceToNextPlayer();
    }

    public void discardCurrentPlayerCard(PlayerState playerState, int cardIndex)
    {
        if(state.isGameOver()) return;
        if(!state.isPlayerTurn(playerState)) return;

        PlayerState currentPlayer = state.getCurrentPlayer();
        if(currentPlayer == null || !currentPlayer.getIsDiscarding()) return;
        if(cardIndex < 0 || cardIndex >= currentPlayer.getHand().size()) return;

        Card discardedCard = currentPlayer.getHand().remove(cardIndex);
        state.getCardDeck().discard(discardedCard);

        if(currentPlayer.getHand().size() <= HAND_LIMIT)
        {
            currentPlayer.setIsDiscarding(false);
            advanceToNextPlayer();
        }
    }

    public void removePlayer(PlayerState playerToRemove) {
        List<PlayerState> players = state.getPlayers();
        int removedIndex = players.indexOf(playerToRemove);

        if (removedIndex == -1) return; // Player not found

        players.remove(removedIndex);

        if (players.isEmpty()) {
            state.setCurrentPlayerIndex(0);
            state.setGameOver(true);
        } else {
            if (removedIndex < state.getCurrentPlayerIndex()) {
                state.setCurrentPlayerIndex(state.getCurrentPlayerIndex() - 1);
            }

            if (state.getCurrentPlayerIndex() >= players.size()) {
                state.setCurrentPlayerIndex(0);
            }
        }

        if (!state.isGameStarted()) {
            state.setLobbyCountdownStartedAt(0);
        }

        if (state.isGameStarted()) {
            checkLoseCondition();
        }
    }

    private void advanceToNextPlayer()
    {
        state.setCurrentPlayerIndex((state.getCurrentPlayerIndex() + 1) % state.getPlayers().size());
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

        // Player deck loss is handled when a draw is attempted and cannot be completed.
    }

    public int getInfectionRate() {
        if (state.getInfectionRate() >= INFECTION_RATE_TRACK.length)
            return INFECTION_RATE_TRACK[INFECTION_RATE_TRACK.length - 1];
        return INFECTION_RATE_TRACK[state.getInfectionRate()];
    }

    public int[] getInfectionRateTrack() {
        return INFECTION_RATE_TRACK;
    }

/////

//after each player turn
    public void executeInfectionPhase(int currentInfectionRate, Deck deck, DiseaseManager diseaseManager) {
        
        for (int i = 0; i < currentInfectionRate; i++) {
            
            //metoda nou adaugata, check deck.java
            Card drawnCard = deck.drawInfectionCard(); 
            
            if (drawnCard != null) {//iau numele cu getter si il adaug in disease manager
                CityNode targetCity = drawnCard.getTargetCity(); 
                diseaseManager.addInfectionCubes(targetCity, 1);
                
                deck.discard(drawnCard);
            }
        }
    }



    public void increaseInfectionRate() {
        state.setInfectionRate(state.getInfectionRate() + 1);
    }

    public PlayerState getCurrentPlayer() {
        synchronized (stateLock) {
            List<PlayerState> players = state.getPlayers();
            if (players.isEmpty()) return null;

            if (state.getCurrentPlayerIndex() >= players.size()) {
                state.setCurrentPlayerIndex(0);
            }

            return players.get(state.getCurrentPlayerIndex());
        }
    }

    public Object getStateLock() {
        return stateLock;
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
