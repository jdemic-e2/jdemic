package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import java.util.List;

public class ThreatAction extends GameAction {

    private Card cardToDiscard;
    private List<Card> rearrangedCards;

    public ThreatAction(Card cardToDiscard, List<Card> rearrangedCards) {
        this.cardToDiscard = cardToDiscard;
        this.rearrangedCards = rearrangedCards;
    }

    public Card getCardToDiscard() {
        return cardToDiscard;
    }

    public List<Card> getRearrangedCards() {
        return rearrangedCards;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        if (playerState == null || cardToDiscard == null || rearrangedCards == null) {
            return false;
        }
        if (rearrangedCards.size() > 6) {
            return false;
        }
        List<Card> topCards = state.getCardDeck().getTopInfectionCards(rearrangedCards.size());
        return playerState.getHand().contains(cardToDiscard)
                && cardToDiscard.getType() == CardType.EVENT
                && cardToDiscard.getEventType() == Card.EventType.THREAT
                && rearrangedCards.size() == topCards.size()
                && topCards.containsAll(rearrangedCards);
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (isValid(state, playerState)) {
            playerState.getHand().remove(cardToDiscard);
            state.getCardDeck().discard(cardToDiscard);

            state.getCardDeck().reorderTopInfectionCards(rearrangedCards);
        }
    }
}
