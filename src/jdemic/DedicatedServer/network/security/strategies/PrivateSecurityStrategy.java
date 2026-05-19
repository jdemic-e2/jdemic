package jdemic.DedicatedServer.network.security.strategies;

import org.json.JSONArray;
import org.json.JSONObject;

public class PrivateSecurityStrategy implements MaskingStrategy {
    @Override
    public void apply(JSONObject gameState, String targetPlayerId) {
        maskPlayers(gameState.optJSONArray("players"), targetPlayerId);
        maskPlayers(gameState.optJSONArray("playerStates"), targetPlayerId);
    }

    private void maskPlayers(JSONArray players, String targetPlayerId) {
        if (players == null) {
            return;
        }

        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.optJSONObject(i);
            if (player == null) {
                continue;
            }

            if (!isTargetPlayer(player, targetPlayerId)) {
                player.remove("ipAddress");
                maskCards(player, "cardsInHand");
                maskCards(player, "hand");
            }
        }
    }

    private boolean isTargetPlayer(JSONObject player, String targetPlayerId) {
        if (targetPlayerId == null || targetPlayerId.isBlank()) {
            return false;
        }

        return targetPlayerId.equalsIgnoreCase(player.optString("id"))
                || targetPlayerId.equalsIgnoreCase(player.optString("playerName"));
    }

    private void maskCards(JSONObject player, String cardsField) {
        JSONArray cards = player.optJSONArray(cardsField);
        if (cards == null) {
            return;
        }

        player.remove(cardsField);
        player.put("hiddenCardCount", cards.length());
    }
}
