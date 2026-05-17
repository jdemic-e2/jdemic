package jdemic.DedicatedServer.network.security.strategies;

import org.json.JSONObject;

/**
 * Defines the contract for any data masking algorithm.
 */
public interface MaskingStrategy {
    /**
     * Applies the specific masking rule to the game state.
     * @param gameState The JSON object representing the current state.
     * @param targetPlayerId The player who is requesting the data.
     */
    void apply(JSONObject gameState, String targetPlayerId);
}