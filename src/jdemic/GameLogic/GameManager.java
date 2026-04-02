package jdemic.GameLogic;
import java.util.List;

import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.ServerRelatedClasses.GameState;

public class GameManager {
    GameState state;
    DiseaseManager diseaseManager;
    Deck cardDeck;
    List<Player> players;
    PandemicMapGraph map;

    int currentPlayerIndex;
    int actionsRemaining;
    int infectionRate;
    int epidemicCount;
    boolean gameOver;
    boolean gameWon;

    private static final int ACTIONS_PER_TURN = 4;
    private static final int[] INFECTION_RATE_TRACK = {2, 2, 2, 3, 3, 4, 4};
    private static final int MAX_OUTBREAKS = 8;

    public GameManager(List<Player> players) 
    {
        this.state = new GameState(); 
        this.map = new PandemicMapGraph();
        this.diseaseManager = new DiseaseManager();
        this.players = players;
        this.cardDeck = new Deck(this);
        this.currentPlayerIndex = 0;
        this.actionsRemaining = ACTIONS_PER_TURN;
        this.infectionRate = 0;
        this.epidemicCount = 0;
        this.gameOver = false;
        this.gameWon = false;

        setupGame();
    }

    private void setupGame()
    {

        CityNode atlanta = map.getCity("Atlanta");
        atlanta.addResearchStation();

        for(Player player : players)
        {
            state.addPlayer(player.getState());
            player.deckReference = cardDeck;
        }
    }

    public void performAction(Player player, GameAction action)
    {
        if(gameOver) return;
        if(actionsRemaining <= 0) return;

        if(action.isValid(state))
        {
            action.execute(state, player.getState());
            actionsRemaining--;
        }
    }

    public void nextTurn()
    {
        if(gameOver) return;

        Player current = players.get(currentPlayerIndex);

        current.drawCards(cardDeck);

        if(cardDeck.getRemainingCardsCount() <= 0)
        {
            gameOver = true;
            gameWon = false;
            return;
        }

        checkWinCondition();
        checkLoseCondition();

        if(gameOver) return;

        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        actionsRemaining = ACTIONS_PER_TURN;
    }

    public void checkWinCondition()
    {
        if(diseaseManager.isCured())
        {
            gameOver = true;
            gameWon = true;
        }
    }

    public void checkLoseCondition(){

        if(diseaseManager.getOutbreakScore() >= MAX_OUTBREAKS)
        {
            gameOver = true;
            gameWon = false;
        }

        if(cardDeck.getRemainingCardsCount() <= 0)
        {
            gameOver = true;
            gameWon = false;
        }
    }

    public int getInfectionRate()
    {
        if(infectionRate >= INFECTION_RATE_TRACK.length) return INFECTION_RATE_TRACK[INFECTION_RATE_TRACK.length - 1];
        return INFECTION_RATE_TRACK[infectionRate];
    }

    public void increaseInfectionRate()
    {
        infectionRate++;
    }

    public Player getCurrentPlayer()
    {
        return players.get(currentPlayerIndex);
    }

    public boolean isGameOver()
    {
        return gameOver;
    }

    public boolean isGameWon()
    {
        return gameWon;
    }

    public void syncState()
    {
        // partea de networking
    }
}
