package jdemic.DedicatedServer.network.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateMaskerProductionPayloadTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void masksOtherPlayerHandsFromSerializedGameStatePayload() throws Exception {
        GameState state = new GameState();
        state.addPlayer(playerState("Player_1", "Atlanta", "Madrid"));
        state.addPlayer(playerState("Player_2", "Paris", "London", "Tokyo"));

        String rawGameState = OBJECT_MAPPER.writeValueAsString(state);
        JSONObject raw = new JSONObject(rawGameState);
        JSONArray rawPlayers = raw.getJSONArray("playerStates");

        assertTrue(findPlayer(rawPlayers, "Player_1").has("hand"));
        assertTrue(findPlayer(rawPlayers, "Player_2").has("hand"));

        String maskedGameState = StateMasker.maskStateForPlayer(rawGameState, "Player_1");
        JSONObject masked = new JSONObject(maskedGameState);
        JSONArray maskedPlayers = masked.getJSONArray("playerStates");
        JSONObject targetPlayer = findPlayer(maskedPlayers, "Player_1");
        JSONObject otherPlayer = findPlayer(maskedPlayers, "Player_2");

        assertTrue(targetPlayer.has("hand"), "The target player must still receive their own hand.");
        assertEquals(2, targetPlayer.getJSONArray("hand").length());

        assertFalse(otherPlayer.has("hand"), "Other players' real card objects must not be sent.");
        assertEquals(3, otherPlayer.getInt("hiddenCardCount"));
        assertFalse(maskedGameState.contains("Paris"));
        assertFalse(maskedGameState.contains("London"));
        assertFalse(maskedGameState.contains("Tokyo"));
    }

    @Test
    void serializedGameStateWithNullPlayersFieldStillMasksPlayerStates() throws Exception {
        GameState state = new GameState();
        state.addPlayer(playerState("Player_1", "Atlanta"));
        state.addPlayer(playerState("Player_2", "Paris"));

        String rawGameState = OBJECT_MAPPER.writeValueAsString(state);
        JSONObject raw = new JSONObject(rawGameState);

        assertTrue(raw.has("players"), "The production GameState serializer emits a players field.");
        assertTrue(raw.isNull("players"), "The production players field can be null before action players are built.");

        String maskedGameState = StateMasker.maskStateForPlayer(rawGameState, "Player_1");
        JSONObject masked = new JSONObject(maskedGameState);
        JSONObject otherPlayer = findPlayer(masked.getJSONArray("playerStates"), "Player_2");

        assertNotNull(otherPlayer);
        assertFalse(otherPlayer.has("hand"));
        assertEquals(1, otherPlayer.getInt("hiddenCardCount"));
    }

    private static PlayerState playerState(String playerName, String... cardNames) {
        PlayerState playerState = new PlayerState(playerName, null);
        for (String cardName : cardNames) {
            playerState.addCard(new Card(cardName, CardType.CITY, null));
        }
        return playerState;
    }

    private static JSONObject findPlayer(JSONArray players, String playerName) {
        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);
            if (playerName.equals(player.optString("playerName"))) {
                return player;
            }
        }
        return null;
    }
}
