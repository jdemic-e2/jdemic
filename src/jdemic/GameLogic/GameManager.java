package jdemic.GameLogic;
import java.util.List;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Actions.GameAction;

public class GameManager {
    GameState state;

    private static final int ACTIONS_PER_TURN = 4;
    private static final int HAND_LIMIT = 7;
    private static final int[] INFECTION_RATE_TRACK = {2, 2, 2, 3, 3, 4, 4};
    private static final int MAX_OUTBREAKS = 8;
    private boolean initialHandsDealt;

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
        for(PlayerState player : players)
        {
            state.addPlayer(player);
        }
        setupGame();
        if (dealInitialHands) {
            dealInitialHands();
        }
    }

    private void setupGame()
    {

        CityNode atlanta = state.getMap().getCity("Atlanta");
        atlanta.addResearchStation();
        for(PlayerState player : state.getPlayers()){
            player.setCurrentCity(atlanta);
        }
    }

    public void startGame()
    {
        if(state.isGameStarted()) return;

        setupGame();
        for(PlayerState playerState : state.getPlayers())
        {
            playerState.setReady(false);
            playerState.setPlayer(new Player(playerState, null));
        }
        state.setCurrentPlayerIndex(0);
        state.setActionsRemaining(ACTIONS_PER_TURN);
        state.setLobbyCountdownStartedAt(0);
        dealInitialHands();
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
        if(playerCount <= 2) return 4;
        if(playerCount == 3) return 3;
        return 2;
    }

    public void performAction(Player player, GameAction action)
    {
        if(state.isGameOver()) return;
        if(state.getActionsRemaining() <= 0) return;
        if(state.getCurrentPlayer() != null && state.getCurrentPlayer().getIsDiscarding()) return;
        
        // Only allow the current player to perform actions
        if(!state.isPlayerTurn(player.getState())) return;

        if(action.isValid(state, player.getState()))
        {
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

    public void nextTurn()
    {
        if(state.isGameOver()) return;

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

        if(state.isGameOver()) return;

        if(currentPlayer.getHand().size() > HAND_LIMIT)
        {
            currentPlayer.setIsDiscarding(true);
            state.setActionsRemaining(0);
            return;
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

    private void advanceToNextPlayer()
    {
        state.setCurrentPlayerIndex((state.getCurrentPlayerIndex() + 1) % state.getPlayers().size());
        state.setActionsRemaining(ACTIONS_PER_TURN);
    }

    public void checkWinCondition()
    {
        if(state.getDiseaseManager().areAllCured())
        {
            state.setGameOver(true);
            state.setGameWon(true);
        }
    }

    public void checkLoseCondition(){

        if(state.getDiseaseManager().getOutbreakScore() >= MAX_OUTBREAKS)
        {
            state.setGameOver(true);
            state.setGameWon(false);
        }

        // Player deck loss is handled when a draw is attempted and cannot be completed.
    }

    public int getInfectionRate()
    {
        if(state.getInfectionRate() >= INFECTION_RATE_TRACK.length) return INFECTION_RATE_TRACK[INFECTION_RATE_TRACK.length - 1];
        return INFECTION_RATE_TRACK[state.getInfectionRate()];
    }

    public int[] getInfectionRateTrack()
    {
        return INFECTION_RATE_TRACK;
    }

    public void increaseInfectionRate()
    {
        state.setInfectionRate(state.getInfectionRate() + 1);
    }

    public PlayerState getCurrentPlayer()
    {
        return state.getPlayers().get(state.getCurrentPlayerIndex());
    }

    public GameState getState(){
        return this.state;
    }

    public boolean isGameOver()
    {
        return state.isGameOver();
    }

    public boolean isGameWon()
    {
        return state.isGameWon();
    }
}
