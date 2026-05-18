package jdemic.DedicatedServer.network.security;

import jdemic.DedicatedServer.network.security.strategies.GlobalSecurityStrategy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class GlobalSecurityStrategyTest {

    @Test
    public void testStateMaskingHidesNestedInfectionDeckOrder() {
        JSONObject rawGameState = new JSONObject();
        JSONObject cardDeck = new JSONObject();
        cardDeck.put("infectionCards", new JSONArray().put("Tokyo").put("Paris"));
        rawGameState.put("cardDeck", cardDeck);

        new GlobalSecurityStrategy().apply(rawGameState, "Alice");

        JSONObject maskedDeck = rawGameState.getJSONObject("cardDeck");
        assertFalse(maskedDeck.has("infectionCards"));
        assertEquals(2, maskedDeck.getInt("infectionCardCount"));
    }
}
