package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Card;

public class SystemControlAction extends GameAction {

    private Card cardToDiscard;
    private Card infectionCardToRemove;

    public SystemControlAction(Card cardToDiscard, Card infectionCardToRemove) {
        this.cardToDiscard = cardToDiscard;
        this.infectionCardToRemove = infectionCardToRemove;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        if (playerState == null || cardToDiscard == null || infectionCardToRemove == null) {
            return false;
        }
        return playerState.getHand().contains(cardToDiscard);
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