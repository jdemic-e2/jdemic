package jdemic.GameLogic;

public class Player {
    
    private final String playerName;
    private PlayerRoles Role;
    private CityNode currentCity;

    public Player(String name, CityNode currentCity){
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
}
