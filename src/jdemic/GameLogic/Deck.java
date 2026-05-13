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

    private void createDecks(){
        // City Cards
        manager.getState().getMap().getCityList()
            .stream()
            .forEach(c -> playerCards.add(new Card(c.getName(), CardType.CITY, c)));

        // Infection Cards
        manager.getState().getMap().getCityList()
            .stream()
            .forEach(c -> infectionCards.add(new Card(c.getName(), CardType.INFECTION, c)));

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
        for(int i = 1; i <= 4; i++){
            playerCards.add(new Card("System Breach", CardType.EPIDEMIC, null));
        }

        // Initial shuffle of both piles
        initShuffle();
    }

    public void drawHand(PlayerState player) {
        drawOneCard(player);
        drawOneCard(player);
    }

    private void drawOneCard(PlayerState player) {
        if (playerCards.isEmpty()) {
            manager.checkLoseCondition();
            return;
        }

        Card drawn = playerCards.remove(0);

        if (drawn.getType() == CardType.EPIDEMIC) {
            resolveEpidemic(drawn);
        } else {
            player.addCard(drawn);
        }
    }

    private void resolveEpidemic(Card epidemicCard) {
        System.out.println("[Deck] EPIDEMIC triggered! Resolving...");

        // INCREASE
        manager.increaseInfectionRate();
        manager.getState().setEpidemicCount(manager.getState().getEpidemicCount() + 1);
        System.out.println("[Deck] Infection rate increased to: " + manager.getInfectionRate());

        // INFECT
        if (!infectionCards.isEmpty()) {
            Card bottomCard = infectionCards.remove(infectionCards.size() - 1);
            CityNode targetCity = bottomCard.getTargetCity();

            System.out.println("[Deck] Epidemic infecting city: " + targetCity.getName() + " with 3 cubes.");
            manager.getState().getDiseaseManager().addInfectionCubes(targetCity, 3);

            infectionDiscardPile.add(bottomCard);
        }

        Collections.shuffle(infectionDiscardPile);
        infectionDiscardPile.addAll(infectionCards); // existing deck goes underneath
        infectionCards = new ArrayList<>(infectionDiscardPile);
        infectionDiscardPile.clear();
        System.out.println("[Deck] Infection deck intensified. New size: " + infectionCards.size());

        playerDiscardPile.add(epidemicCard);

        manager.checkLoseCondition();
    }

    public void discard(Card card) {
        if (card.getType() == CardType.INFECTION) {
            this.infectionDiscardPile.add(card);
        } else {
            this.playerDiscardPile.add(card);
        }
    }

    // Initial shuffle for both piles at the start of the game
    public void initShuffle() {
        Collections.shuffle(playerCards);
        Collections.shuffle(infectionCards);
    }

    public void shuffle(List<Card> list) {
        Collections.shuffle(list);
    }

    public boolean isEmpty() {
        return playerCards.isEmpty();
    }

    public int getRemainingCardsCount() {
        return playerCards.size();
    }

    public List<Card> getInfectionCards() {
        return infectionCards;
    }

    public List<Card> getInfectionDiscardPile() {
        return infectionDiscardPile;
    }

    public List<Card> getPlayerDiscardPile() {
        return playerDiscardPile;
    }
}