package jdemic.DedicatedServer.network.security;

import jdemic.DedicatedServer.network.security.strategies.PrivateSecurityStrategy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PrivateSecurityStrategyTest {

    @Test
    public void testStateMaskingHidesOtherPlayersCards() {
        // Simulare starea jocului trimisa de GameManager (raw JSON)
        JSONObject rawGameState = new JSONObject();
        JSONArray playersArray = new JSONArray();

        // Jucatorul target (cel care primeste pachetul)
        JSONObject targetPlayer = new JSONObject();
        targetPlayer.put("playerName", "Alice");
        targetPlayer.put("hand", new JSONArray().put("Atlanta").put("Madrid"));
        targetPlayer.put("ipAddress", "192.168.1.10");

        // Un alt jucator in joc 
        JSONObject otherPlayer = new JSONObject();
        otherPlayer.put("playerName", "Bob");
        otherPlayer.put("hand", new JSONArray().put("Paris").put("London").put("Tokyo"));
        otherPlayer.put("ipAddress", "192.168.1.20");

        playersArray.put(targetPlayer);
        playersArray.put(otherPlayer);
        rawGameState.put("players", playersArray);

        //  Aplic strategia pentru Alice
        PrivateSecurityStrategy strategy = new PrivateSecurityStrategy();
        strategy.apply(rawGameState, "Alice");

        // 3. Verific rezultatul pentru Alice
        JSONArray maskedPlayers = rawGameState.getJSONArray("players");
        JSONObject maskedAlice = maskedPlayers.getJSONObject(0);
        JSONObject maskedBob = maskedPlayers.getJSONObject(1);

        // Alice isi vede cartile
        assertTrue(maskedAlice.has("hand"), "Alice ar trebui sa isi vada propriile carti.");
        assertEquals(2, maskedAlice.getJSONArray("hand").length());

        // Alice NU vede cartile lui Bob, ci doar numarul lor
        assertFalse(maskedBob.has("hand"), "Cartile lui Bob trebuie ascunse de la Alice.");
        assertTrue(maskedBob.has("hiddenCardCount"), "Trebuie sa existe un counter pentru cartile ascunse.");
        assertEquals(3, maskedBob.getInt("hiddenCardCount"), "Numarul de carti ascunse trebuie sa fie corect.");
        
        // Verific si privacy (IP)
        assertFalse(maskedBob.has("ipAddress"), "IP-ul lui Bob nu trebuie sa fie vizibil.");
    }
}