package jdemic.GameLogic;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

import java.util.List;
import java.util.ArrayList;

public class Deck {
    private List<Card> playerCards;
    private List<Card> infectionCards;
    private List<Card> discardPile;

    public Deck() {
        this.playerCards = new ArrayList<>();
        this.infectionCards = new ArrayList<>();
        this.discardPile = new ArrayList<>();
    }

    public void drawHand(PlayerState player) {
    }

    public void discard(Card card) {
        this.discardPile.add(card);
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