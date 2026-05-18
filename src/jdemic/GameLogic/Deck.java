package jdemic.GameLogic;
import jdemic.GameLogic.Card.EventType;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import java.util.logging.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Deck {

    private static final Logger LOGGER = Logger.getLogger(Deck.class.getName());

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

        // Initial shuffle of both piles
        initShuffle();
    }

    public void drawHand(PlayerState player) {
        drawOneCard(player);
        drawOneCard(player);
    }

    public void drawInitialHand(PlayerState player, int cardCount) {
        for (int i = 0; i < cardCount; i++) {
            drawOneInitialCard(player);
            if (manager.isGameOver()) {
                return;
            }
        }
    }

    private void drawOneInitialCard(PlayerState player) {
        for (int i = 0; i < playerCards.size(); i++) {
            Card drawn = playerCards.get(i);
            if (drawn.getType() != CardType.EPIDEMIC) {
                playerCards.remove(i);
                player.addCard(drawn);
                return;
            }
        }

        manager.getState().setGameOver(true);
        manager.getState().setGameWon(false);
    }

    private void drawOneCard(PlayerState player) {
        if (playerCards.isEmpty()) {
            manager.getState().setGameOver(true);
            manager.getState().setGameWon(false);
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
        LOGGER.info("[Deck] EPIDEMIC triggered! Resolving...");

        // INCREASE
        manager.increaseInfectionRate();
        manager.getState().setEpidemicCount(manager.getState().getEpidemicCount() + 1);
        LOGGER.info("[Deck] Infection rate increased to: " + manager.getInfectionRate());

        // INFECT
        if (!infectionCards.isEmpty()) {
            Card bottomCard = infectionCards.remove(infectionCards.size() - 1);
            CityNode targetCity = bottomCard.getTargetCity();

            LOGGER.info("[Deck] Epidemic infecting city: " + targetCity.getName() + " with 3 cubes.");
            manager.getState().getDiseaseManager().addInfectionCubes(targetCity, 3);

            infectionDiscardPile.add(bottomCard);
        }

        Collections.shuffle(infectionDiscardPile);
        infectionDiscardPile.addAll(infectionCards); // existing deck goes underneath
        infectionCards = new ArrayList<>(infectionDiscardPile);
        infectionDiscardPile.clear();
        LOGGER.info("[Deck] Infection deck intensified. New size: " + infectionCards.size());

        playerDiscardPile.add(epidemicCard);

        manager.checkLoseCondition();
    }
/////

    //extrage prima carte din pachetul de infectii , pentru infection din game manager
    public Card drawInfectionCard() {
        if (!infectionCards.isEmpty()) {
            Card topCard = infectionCards.get(0);
            infectionCards.remove(0);
            return topCard;
        }
        return null;
    }



    public void discard(Card card) {
        if (card.getType() == CardType.INFECTION) {
            this.infectionDiscardPile.add(card);
        } else {
            this.playerDiscardPile.add(card);
        }
    }

    public void removeInfectionCardFromDiscard(Card card) {
        if (this.infectionDiscardPile.contains(card)) {
            this.infectionDiscardPile.remove(card);
        }
    }

    public void reorderTopInfectionCards(List<Card> rearrangedCards) {
        if (rearrangedCards == null) {
            return;
        }
        for (int i = 0; i < rearrangedCards.size(); i++) {
            if (!this.infectionCards.isEmpty()) {
                this.infectionCards.remove(0);
            }
        }
        this.infectionCards.addAll(0, rearrangedCards);
    }

    public List<Card> getTopInfectionCards(int count) {
        int safeCount = Math.max(0, Math.min(count, infectionCards.size()));
        return new ArrayList<>(infectionCards.subList(0, safeCount));
    }

    // Initial shuffle for both piles at the start of the game
    public void initShuffle() {
        Collections.shuffle(playerCards);
        Collections.shuffle(infectionCards);
    }

    public void addEpidemicCards(int count) {
        for (int i = 0; i < count; i++) {
            playerCards.add(new Card("System Breach", CardType.EPIDEMIC, null));
        }
        Collections.shuffle(playerCards);
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
