package jdemic.DedicatedServer.network.security.strategies;

import org.json.JSONObject;

public class GlobalSecurityStrategy implements MaskingStrategy {
    @Override
    public void apply(JSONObject gameState, String targetPlayerId) {
        // Anti-Cheat: Prevent clients from seeing the upcoming infection cards
        if (gameState.has("infectionDeck")) {
            gameState.remove("infectionDeck");
        }

        if (gameState.has("cardDeck")) {
            JSONObject cardDeck = gameState.optJSONObject("cardDeck");
            if (cardDeck != null && cardDeck.has("infectionCards")) {
                int remainingInfectionCards = cardDeck.getJSONArray("infectionCards").length();
                cardDeck.remove("infectionCards");
                cardDeck.put("infectionCardCount", remainingInfectionCards);
            }
        }
        
        // You can add more global rules here later (e.g., hiding the game seed)
    }
}
