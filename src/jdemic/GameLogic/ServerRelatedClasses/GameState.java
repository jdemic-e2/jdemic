package jdemic.GameLogic.ServerRelatedClasses;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jdemic.GameLogic.Deck;
import jdemic.GameLogic.DiseaseManager;
import jdemic.GameLogic.PandemicMapGraph;
import jdemic.GameLogic.Player;

public class GameState{

    // player info
    private List<PlayerState> playerArray = new ArrayList<>();

    private DiseaseManager diseaseManager;
    private Deck cardDeck;

    @JsonIgnore // pentru erori
    private transient PandemicMapGraph map; // adaugat transient pentru evitare serializare

    private int currentPlayerIndex;
    private int actionsRemaining;
    private int infectionRate;
    private int epidemicCount;

    // variables to account win/lose and finished/ongoing

    private boolean gameOver;
    private boolean gameWon;

    public GameState(){
        this.playerArray = new ArrayList<>();
    }

    public void addPlayer(PlayerState s){
        this.playerArray.add(s);
    }

    public void removePlayer(PlayerState s){
        this.playerArray.remove(s);
    }

    public List<PlayerState> getPlayers(){
        return this.playerArray;
    }

    public DiseaseManager getDiseaseManager(){
        return this.diseaseManager;
    }

    public void setDiseaseManager(DiseaseManager diseaseManager){
        this.diseaseManager = diseaseManager;
    }

    public Deck getCardDeck(){
        return this.cardDeck;
    }

    public void setCardDeck(Deck cardDeck){
        this.cardDeck = cardDeck;
    }

    public PandemicMapGraph getMap(){
        return this.map;
    }

    public void setMap(PandemicMapGraph map){
        this.map = map;
    }

    public int getCurrentPlayerIndex(){
        return this.currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex){
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public int getActionsRemaining(){
        return this.actionsRemaining;
    }

    public void setActionsRemaining(int actionsRemaining){
        this.actionsRemaining = actionsRemaining;
    }

    public int getInfectionRate(){
        return this.infectionRate;
    }

    public void setInfectionRate(int infectionRate){
        this.infectionRate = infectionRate;
    }

    public int getEpidemicCount(){
        return this.epidemicCount;
    }

    public void setEpidemicCount(int epidemicCount){
        this.epidemicCount = epidemicCount;
    }

    public boolean isGameOver(){
        return this.gameOver;
    }

    public void setGameOver(boolean gameOver){
        this.gameOver = gameOver;
    }

    public boolean isGameWon(){
        return this.gameWon;
    }

    public void setGameWon(boolean gameWon){
        this.gameWon = gameWon;
    }
}