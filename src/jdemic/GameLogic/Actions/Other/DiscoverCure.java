package jdemic.GameLogic.Actions.Other;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.DiseaseColor;
import jdemic.GameLogic.PlayerRoles;

import java.util.List;

public class DiscoverCure extends GameAction {

    private final DiseaseColor targetColor;
    private final List<Card> cardsToDiscard;

    public DiscoverCure(DiseaseColor targetColor, List<Card> cardsToDiscard) {
        this.targetColor = targetColor;
        this.cardsToDiscard = cardsToDiscard;
    }

    private int requiredCardCount(PlayerState playerState) {
        return playerState.getPlayerRole() == PlayerRoles.SCIENTIST ? 4 : 5;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        CityNode currentCity = playerState.getPlayerCurrentCity();
        if (currentCity == null || !currentCity.hasResearchStation()) {
            return false;
        }

        if (state.getDiseaseManager().isCured(targetColor)) {
            return false;
        }

        if (cardsToDiscard == null || cardsToDiscard.size() < requiredCardCount(playerState)) {
            return false;
        }

        List<Card> hand = playerState.getHand();
        for (Card card : cardsToDiscard) {
            if (card.getType() != CardType.CITY) {
                return false;
            }
            if (card.getTargetCity() == null
                    || card.getTargetCity().getNativeColor() != targetColor) {
                return false;
            }
            if (!hand.contains(card)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (!isValid(state, playerState)) {
            return;
        }

        List<Card> hand = playerState.getHand();
        for (Card card : cardsToDiscard) {
            hand.remove(card);
        }

        state.getDiseaseManager().discoverCure(targetColor);
    }
}