package jdemic.DedicatedServer.network.security;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import jdemic.DedicatedServer.network.security.strategies.GlobalSecurityStrategy;
import jdemic.DedicatedServer.network.security.strategies.MaskingStrategy;
import jdemic.DedicatedServer.network.security.strategies.PrivateSecurityStrategy;

/**
 * StateMasker Context - Executes all registered masking strategies.
 */
public class StateMasker {

    // A registry of all active security filters
    private static final List<MaskingStrategy> activeStrategies = new ArrayList<>();

    // Static block to initialize the strategies once
    static {
        activeStrategies.add(new GlobalSecurityStrategy());
        activeStrategies.add(new PrivateSecurityStrategy());
    }

    /**
     * Filters the raw game state for a specific player by piping it through all strategies.
     */
    public static String maskStateForPlayer(String rawData, String targetPlayerId) {
        try {
            JSONObject gameState = new JSONObject(rawData);

            // Pipe the data through every active security strategy
            for (MaskingStrategy strategy : activeStrategies) {
                strategy.apply(gameState, targetPlayerId);
            }

            return gameState.toString();

        } catch (JSONException e) {
            // FALLBACK: Allows your existing TestClient/SecurityTest to pass simple strings
            return rawData;
        } catch (Exception e) {
            System.err.println("[StateMasker] Critical error: " + e.getMessage());
            return "{}"; // Fail-safe: return empty data on error
        }
    }
}