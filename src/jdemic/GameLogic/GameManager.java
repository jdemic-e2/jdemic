package jdemic.GameLogic;
import java.util.List;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.Actions.GameAction;

public class GameManager {
    GameState state;

    private static final int ACTIONS_PER_TURN = 4;
    private static final int[] INFECTION_RATE_TRACK = {2, 2, 2, 3, 3, 4, 4};
    private static final int MAX_OUTBREAKS = 8;

    public GameManager(List<Player> players) {
        this.state = new GameState(); 
        state.setMap(new PandemicMapGraph());
        state.setDiseaseManager(new DiseaseManager(this));
        state.setPlayers(players);
        state.setCardDeck(new Deck(this));
        state.setCurrentPlayerIndex(0);
        state.setActionsRemaining(ACTIONS_PER_TURN);
        state.setInfectionRate(0);
        state.setEpidemicCount(0);
        state.setGameOver(false);
        state.setGameWon(false);

        setupGame();
    }

    private void setupGame()
    {

        CityNode atlanta = state.getMap().getCity("Atlanta");
        atlanta.addResearchStation();

        for(Player player : state.getPlayers())
        {
            state.addPlayer(player.getState());
            player.deckReference = state.getCardDeck();
        }
    }

    public void performAction(Player player, GameAction action)
    {
        if(state.isGameOver()) return;
        if(state.getActionsRemaining() <= 0) return;

        if(action.isValid(state, player.getState()))
        {
            action.execute(state, player.getState());
            state.setActionsRemaining(state.getActionsRemaining() - 1);
        }
    }

    public void nextTurn()
    {
        if(state.isGameOver()) return;

        Player current = state.getPlayers().get(state.getCurrentPlayerIndex());

        current.drawCards(state.getCardDeck());

        if(state.getCardDeck().getRemainingCardsCount() <= 0)
        {
            state.setGameOver(true);
            state.setGameWon(false);
            return;
        }

        checkWinCondition();
        checkLoseCondition();

        if(state.isGameOver()) return;

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

        if(state.getCardDeck().getRemainingCardsCount() <= 0)
        {
            state.setGameOver(true);
            state.setGameWon(false);
        }
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

    public Player getCurrentPlayer()
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

    public void syncState()
    {
        // partea de networking
    }
}
