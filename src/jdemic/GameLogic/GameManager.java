package jdemic.GameLogic;
import java.util.List;

import jdemic.GameLogic.ServerRelatedClasses.GameState;

public class GameManager {
    GameState state;
    DiseaseManager diseaseManager;
    Deck cardDeck;
    List<Player> players;
    PandemicMapGraph map;
    private int activePlayerIndex = 0; // Tracks whose turn it is

    public GameManager(List<Player> players) {
        this.state = new GameState(); 
        this.map = new PandemicMapGraph();
        this.diseaseManager = new DiseaseManager();
        this.players = players;
        this.cardDeck = new Deck();

        setupGame();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        if (players == null || players.isEmpty()) return null;
        return players.get(activePlayerIndex);
    }

    private void setupGame(){
        // TODO
    }

    public void nextTurn(){
        //TODO
    }

    public void checkWinCondition(){
        //TODO
    }

    public void syncState(){
        //TODO
    }
}
