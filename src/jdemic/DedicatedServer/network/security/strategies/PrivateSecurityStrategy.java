package jdemic.DedicatedServer.network.security.strategies;

import org.json.JSONArray;
import org.json.JSONObject;

public class PrivateSecurityStrategy implements MaskingStrategy {
    @Override
    public void apply(JSONObject gameState, String targetPlayerId) {
        if (!gameState.has("players")) return;

        JSONArray players = gameState.getJSONArray("players");

        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);

            if (player.has("id")) {
                String id = player.getString("id");

                // If this is NOT the target player, censor their private info
                if (!id.equals(targetPlayerId)) {
                    // Privacy: Hide IP Address
                    player.remove("ipAddress");

                    // Strategy: Hide actual cards, but keep the count for UI rendering
                    if (player.has("cardsInHand")) {
                        int cardCount = player.getJSONArray("cardsInHand").length();
                        player.remove("cardsInHand");
                        player.put("hiddenCardCount", cardCount);
                    }
                }
            }
        }
    }
}