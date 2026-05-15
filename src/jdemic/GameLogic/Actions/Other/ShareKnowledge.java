package jdemic.GameLogic.Actions.Other;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.PlayerRoles;

public class ShareKnowledge extends GameAction {

    private final Card card;
    private final PlayerState giver;
    private final PlayerState receiver;

    public ShareKnowledge(Card card, PlayerState giver, PlayerState receiver) {
        this.card = card;
        this.giver = giver;
        this.receiver = receiver;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        if (card == null || giver == null || receiver == null) {
            return false;
        }

        CityNode giverCity    = giver.getPlayerCurrentCity();
        CityNode receiverCity = receiver.getPlayerCurrentCity();
        if (giverCity == null || receiverCity == null) {
            return false;
        }
        if (!giverCity.getName().equals(receiverCity.getName())) {
            return false;
        }

        if (!giver.getHand().contains(card)) {
            return false;
        }

        if (card.getType() != CardType.CITY) {
            return false;
        }

        if (giver.getPlayerRole() == PlayerRoles.RESEARCHER) {
            return true;
        }

        return card.getCardName().equals(giverCity.getName());
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (!isValid(state, playerState)) {
            return;
        }

        giver.getHand().remove(card);
        receiver.addCard(card);
    }
}