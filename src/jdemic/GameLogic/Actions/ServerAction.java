package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;

public class ServerAction extends GameAction {

    private CityNode destinationCity;
    private Card cardToDiscard;

    public ServerAction(CityNode destinationCity, Card cardToDiscard) {
        this.destinationCity = destinationCity;
        this.cardToDiscard = cardToDiscard;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        if (playerState == null || cardToDiscard == null || destinationCity == null) {
            return false;
        }

        return playerState.getHand().contains(cardToDiscard);
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (isValid(state, playerState)) {
            playerState.getHand().remove(cardToDiscard);
            state.getCardDeck().discard(cardToDiscard);

            destinationCity.addResearchStation();
        }
    }
}