package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;

public class SystemControlAction extends GameAction {

    private Card cardToDiscard;
    private Card infectionCardToRemove;

    public SystemControlAction(Card cardToDiscard, Card infectionCardToRemove) {
        this.cardToDiscard = cardToDiscard;
        this.infectionCardToRemove = infectionCardToRemove;
    }

    public Card getCardToDiscard() {
        return cardToDiscard;
    }

    public Card getInfectionCardToRemove() {
        return infectionCardToRemove;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        if (playerState == null || cardToDiscard == null || infectionCardToRemove == null) {
            return false;
        }
        return playerState.getHand().contains(cardToDiscard)
                && state.getCardDeck().getInfectionDiscardPile().contains(infectionCardToRemove)
                && cardToDiscard.getType() == CardType.EVENT
                && cardToDiscard.getEventType() == Card.EventType.CONTROL;
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (isValid(state, playerState)) {
            playerState.getHand().remove(cardToDiscard);
            state.getCardDeck().discard(cardToDiscard);

            state.getCardDeck().removeInfectionCardFromDiscard(infectionCardToRemove);

        }
    }
}
