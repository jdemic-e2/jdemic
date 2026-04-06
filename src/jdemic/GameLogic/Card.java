package jdemic.GameLogic;

public class Card {
    
    private String cardName;
    private CardType type;
    private CityNode targetCity;

    public Card(String cardName, CardType type, CityNode targetCity) {
        this.cardName = cardName;
        this.type = type;
        this.targetCity = targetCity;
    }

    public String getEffectDescription() {
        return null; 
    }

    public String getCardName() {
        return this.cardName;
    }

    public CardType getType() {
        return this.type;
    }

    public CityNode getTargetCity() {
        return this.targetCity;
    }
}