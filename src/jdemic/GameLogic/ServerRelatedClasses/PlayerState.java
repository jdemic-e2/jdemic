package jdemic.GameLogic.ServerRelatedClasses;
import jdemic.GameLogic.PlayerRoles;
import jdemic.GameLogic.CityNode;
import java.util.ArrayList;
import java.util.List;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.Player;

public class PlayerState{

    private String playerName;
    private PlayerRoles Role;
    private CityNode currentCity;
    private List<Card> hand = new ArrayList<>();
    private boolean discardingCards;
    private Player playerReference;

    public PlayerState(String name){
        this.discardingCards = false;
        this.playerName = name;
    }

    // PlayerState must only have simple methods for changing variables, for easier serialization on the network.

    public Player getPlayer(){
        return this.playerReference;
    }

    public PlayerRoles getPlayerRole(){
        return this.Role;
    }

    public CityNode getPlayerCurrentCity() {
        return this.currentCity;
    }

    public void setCurrentCity(CityNode city){
        this.currentCity = city;
    }

    public String getPlayerName(){
        return this.playerName;
    }

    public List<Card> getHand(){
        return this.hand;
    }

    public Card getCard(int index){
        return this.hand.get(index);
    }

    public void addCard(Card o){
        this.hand.add(o);
    }

    public void removeCard(int index){
        this.hand.remove(index);
    }

    public boolean getIsDiscarding(){
        return this.discardingCards;
    }

    public void setIsDiscarding(boolean state){
        this.discardingCards = state;
    }
}