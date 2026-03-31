package jdemic.GameLogic.ServerRelatedClasses;
import jdemic.GameLogic.PlayerRoles;
import jdemic.GameLogic.CityNode;
import java.util.ArrayList;
import java.util.List;
import jdemic.GameLogic.Card;

public class PlayerState{

    private final String playerName;
    private PlayerRoles Role;
    private CityNode currentCity;
    private List<Card> hand = new ArrayList<>();

    public PlayerState(String name, CityNode currentCity){
        this.playerName = name;
        this.currentCity = currentCity;
    }

    public PlayerRoles getPlayerRole(){
        return this.Role;
    }

    public CityNode getPlayerCurrentCity() {
        return this.currentCity;
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
}