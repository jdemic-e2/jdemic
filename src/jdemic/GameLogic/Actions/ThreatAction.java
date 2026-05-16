package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Card;
import java.util.List;

public class ThreatAction extends GameAction {

    private Card cardToDiscard;
    private List<Card> rearrangedCards;

    public ThreatAction(Card cardToDiscard, List<Card> rearrangedCards) {
        this.cardToDiscard = cardToDiscard;
        this.rearrangedCards = rearrangedCards;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        if (playerState == null || cardToDiscard == null || rearrangedCards == null) {
            return false;
        }
        if (rearrangedCards.size() > 6) {
            return false;
        }
        return playerState.getHand().contains(cardToDiscard);
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