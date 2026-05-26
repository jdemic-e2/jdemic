package jdemic.GameLogic.Actions.Other;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.PlayerRoles;

import java.util.Iterator;
import java.util.List;

public class BuildResearchStation extends GameAction {

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        CityNode currentCity = playerState.getPlayerCurrentCity();

        if (currentCity == null) {
            return false;
        }

        if (currentCity.hasResearchStation()) {
            return false;
        }

        if (playerState.getPlayerRole() == PlayerRoles.OPERATIONS_EXPERT) {
            return true;
        }

        List<Card> hand = playerState.getHand();
        return hand.stream()
                .anyMatch(card -> card.getCardName().equals(currentCity.getName()));
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (!isValid(state, playerState)) {
            return;
        }
        
        CityNode currentCity = playerState.getPlayerCurrentCity();
        currentCity.addResearchStation();

        if (playerState.getPlayerRole() == PlayerRoles.OPERATIONS_EXPERT) {
            return;
        }

        Iterator<Card> iterator = playerState.getHand().iterator();
        while (iterator.hasNext()) {
            Card card = iterator.next();
            if (card.getCardName().equals(currentCity.getName())) {
                iterator.remove();
                state.getCardDeck().discard(card);
                return;
            }
        }
    }
}
