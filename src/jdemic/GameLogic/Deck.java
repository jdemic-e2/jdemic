package jdemic.GameLogic;
import jdemic.GameLogic.Card.EventType;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

import java.util.List;
import java.util.ArrayList;

public class Deck {

    public enum DeckTypes{
        PLAYER,
        INFECTION,
        DISCARD
    }

    private List<Card> playerCards;
    private List<Card> infectionCards;
    private List<Card> discardPile;
    private PandemicMapGraph map;

    public Deck(PandemicMapGraph mapGraph) {
        this.playerCards = new ArrayList<>();
        this.infectionCards = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        this.map = mapGraph;

        createDecks();
    }

    private void createDecks(){
        // City Cards
        map.getCityList()
            .stream()
            .forEach(c->playerCards.add(new Card(c.getName(), CardType.CITY, c )));

        // Infection Cards
        map.getCityList()
            .stream()
            .forEach(c->infectionCards.add(new Card(c.getName(), CardType.INFECTION, c )));
        
        // Event Cards
        Card airlift = new Card("Airlift", CardType.EVENT, null);
        airlift.setEventType(EventType.AIRLIFT);
        playerCards.add(airlift);

        Card forecast = new Card("Forecast", CardType.EVENT, null);
        forecast.setEventType(EventType.FORECAST);
        playerCards.add(forecast);

        Card govGrant = new Card("Gouvernment Grant", CardType.EVENT, null);
        govGrant.setEventType(EventType.GOUVERNMENT_GRANT);
        playerCards.add(govGrant);

        Card resPop = new Card("Resilient Population", CardType.EVENT, null);
        resPop.setEventType(EventType.RESILIENT_POPULATION);
        playerCards.add(resPop);

        Card OQN = new Card("One Quiet Night", CardType.EVENT, null);
        OQN.setEventType(EventType.ONE_QUIET_NIGHT);
        playerCards.add(OQN);
    }

    public void drawHand(PlayerState player) {
    }

    public void discard(Card card) {
        this.discardPile.add(card);
    }

    public boolean checkIfCardCanBeAddedToPile(DeckTypes type, Card card){
        switch(type){
            case PLAYER:
                
            


            case INFECTION:

                
            

            case DISCARD:

                
            


            default:
                return false;
        }   
        
    }

    public void shuffle() {
    }

    public void reshuffle() {
    }

    public boolean isEmpty() {
        return false;
    }

    public int getRemainingCardsCount() {
        return 0;
    }
}