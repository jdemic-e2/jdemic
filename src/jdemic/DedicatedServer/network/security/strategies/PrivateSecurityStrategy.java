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
            JSONObject player = players.getJSONObject(i);

            if (player.has("playerName")) {
                String id = player.getString("playerName");

                // If this is NOT the target player, censor their private info
                if (!id.equals(targetPlayerId)) {
                    // Privacy: Hide IP Address
                    player.remove("ipAddress");

                    // Strategy: Hide actual cards, but keep the count for UI rendering
                    if (player.has("hand")) {
                        int cardCount = player.getJSONArray("hand").length();
                        player.remove("hand");
                        player.put("hiddenCardCount", cardCount);
                    }
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
