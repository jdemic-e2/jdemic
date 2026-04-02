package jdemic.GameLogic;
public class Card {

    public enum EventType {
        FIREWALL,
        SATELLITE,
        SERVER,
        CONTROL,
        THREAT
    }
    
    private String cardName;
    private CardType type;
    private CityNode targetCity;
    private EventType eventType;

    public Card(String cardName, CardType type, CityNode targetCity) {
        this.cardName = cardName;
        this.type = type;
        this.targetCity = targetCity;
    }

    public String getEffectDescription() {
        switch(type){
            case CITY:
                return("You can use this card for any city-related action in " + this.getCardName());
            case EVENT:
                switch(eventType){
                    case SATELLITE:
                        return("Move any 1 pawn to any city on the board.");
                    case SERVER:
                        return("Place 1 Research Station in any city (no City card required).");
                    case THREAT:
                        return("Draw, examine, and rearrange the top 6 cards of the Infection Deck.");
                    case FIREWALL:
                        return("Skip the \"Infect Cities\" step of the current turn.");
                    case CONTROL:
                        return("Remove 1 card from the Infection Discard Pile from the game entirely.");
                }
            case INFECTION:
                return("Use this card to infect " + this.getCardName());
            case EPIDEMIC:
                return("Start an epidemic in the next location.");
        }
        return "error";
    }

    public void setEventType(EventType type){
        this.eventType = type;
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