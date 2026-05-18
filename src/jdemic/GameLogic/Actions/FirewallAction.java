package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;

public class FirewallAction extends GameAction {

    private Card cardToDiscard;

    public FirewallAction(Card cardToDiscard) {
        this.cardToDiscard = cardToDiscard;
    }

    public Card getCardToDiscard() {
        return this.cardToDiscard;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        if (playerState == null || cardToDiscard == null) {
            return false;
        }
        return playerState.getHand().contains(cardToDiscard)
                && cardToDiscard.getType() == CardType.EVENT
                && cardToDiscard.getEventType() == Card.EventType.FIREWALL;
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (isValid(state, playerState)) {
            playerState.getHand().remove(cardToDiscard);
            state.getCardDeck().discard(cardToDiscard);

            state.setSkipInfection(true);
        }
    }
}
