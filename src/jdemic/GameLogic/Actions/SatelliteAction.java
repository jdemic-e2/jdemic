package jdemic.GameLogic.Actions;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;

public class SatelliteAction extends GameAction {
    private String targetPawn;
    private CityNode destination;
    private Card cardToDiscard;

    public SatelliteAction(String targetPawn, CityNode destination, Card cardToDiscard) {
        this.targetPawn = targetPawn;
        this.destination = destination;
        this.cardToDiscard = cardToDiscard;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        if (playerState == null || cardToDiscard == null) return false;
        return playerState.getHand().contains(cardToDiscard);
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (isValid(state, playerState)) {
            playerState.getHand().remove(cardToDiscard);
            state.getCardDeck().discard(cardToDiscard);

            for (PlayerState p : state.getPlayers()) {
                if (p.getPlayerName().equals(targetPawn)) {
                    p.setCurrentCity(destination);
                    break;
                }
            }
        }
    }
}