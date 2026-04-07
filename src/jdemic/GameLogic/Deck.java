package jdemic.GameLogic;
import jdemic.GameLogic.Card.EventType;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Deck {

    public enum DeckTypes{
        PLAYER,
        INFECTION,
        DISCARD
    }

    // We use different piles to draw from.
    private List<Card> playerCards;
    private List<Card> infectionCards;
    private List<Card> playerDiscardPile;
    private List<Card> infectionDiscardPile;
    private GameManager manager;

    // constructor
    public Deck(GameManager manager) {
        this.playerCards = new ArrayList<>();
        this.infectionCards = new ArrayList<>();
        this.playerDiscardPile = new ArrayList<>();
        this.infectionDiscardPile = new ArrayList<>();
        this.manager = manager;

        createDecks();
    }
    // create the cards needed for game time.

    private void createDecks(){
        // City Cards
        manager.map.getCityList()
            .stream()
            .forEach(c->playerCards.add(new Card(c.getName(), CardType.CITY, c )));

        // Infection Cards
        manager.map.getCityList()
            .stream()
            .forEach(c->infectionCards.add(new Card(c.getName(), CardType.INFECTION, c )));
        
        // Event Cards
        Card satellite = new Card("Satellite Override", CardType.EVENT, null);
        satellite.setEventType(EventType.SATELLITE);
        playerCards.add(satellite);

        Card threat = new Card("Threat Scan", CardType.EVENT, null);
        threat.setEventType(EventType.THREAT);
        playerCards.add(threat);

        Card serverDep = new Card("Server Deployment", CardType.EVENT, null);
        serverDep.setEventType(EventType.SERVER);
        playerCards.add(serverDep);

        Card controlCard = new Card("System Control", CardType.EVENT, null);
        controlCard.setEventType(EventType.CONTROL);
        playerCards.add(controlCard);

        Card firewall = new Card("Firewall Lockdown", CardType.EVENT, null);
        firewall.setEventType(EventType.FIREWALL);
        playerCards.add(firewall);

        // Epidemic cards
        for(int i = 1 ; i<=4; i++){
            playerCards.add(new Card("System Breach", CardType.EPIDEMIC, null));
        }

        //initial shuffle of the cards
        initShuffle();
    }

    public void drawHand(PlayerState player) {
        manager.checkLoseCondition();
        // send the 2 top cards from deck to player hand.
        if(!playerCards.isEmpty()){
            player.addCard(playerCards.get(0));
            playerCards.remove(0);
        }
        else{
            manager.checkLoseCondition();
        }

        if(!playerCards.isEmpty()){
            player.addCard(playerCards.get(0));
            playerCards.remove(0);
        }
        else{
            manager.checkLoseCondition();
        }

        //TODO Epidemic Card Case, not relevant for first sprint.

    }

    // Discard cards
    public void discard(Card card) {
        // if the card is of type INFECTION, put it in the infection discard pile, which can be reshuffled.
        if(card.getType() == CardType.INFECTION){
            this.infectionDiscardPile.add(card);
        }
        else {
            this.playerDiscardPile.add(card);
        }
    }

    //Initial shuffle for both piles at the start of the game
    public void initShuffle() {
        Collections.shuffle(playerCards);
        Collections.shuffle(infectionCards);
    }

    public void shuffle(List<Card> list){
        Collections.shuffle(list);
    }


    public boolean isEmpty() {
        return false;
    }

    public int getRemainingCardsCount() {
        return playerCards.size();
    }
}