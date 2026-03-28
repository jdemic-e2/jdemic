package jdemic.GameLogic.ServerRelatedClasses;

import java.util.ArrayList;
import java.util.List;
public class GameState{

    private List<PlayerState> playerArray = new ArrayList<>();

    public GameState(){

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
}