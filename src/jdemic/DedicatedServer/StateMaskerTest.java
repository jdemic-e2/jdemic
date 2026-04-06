package jdemic.DedicatedServer;

import org.json.JSONArray;
import org.json.JSONObject;

import jdemic.DedicatedServer.network.security.StateMasker;

/**
 * Unit Test for the Strategy-based StateMasker.
 * Simulates a complex game state and verifies that all security rules apply.
 */
public class StateMaskerTest {

    public static void main(String[] args) {
        System.out.println("=== STARTING INTEGRATION TEST: Strategy-Based StateMasker ===");

        // STEP 1: Setup the "God Mode" State (What the Backend generates)
        JSONObject mockGameState = createMockBackendState();
        String rawData = mockGameState.toString(2); // The '2' adds nice indentation for reading
        System.out.println("\n[DEBUG] Raw Backend State (Contains Secrets):\n" + rawData);

        // STEP 2: Execution
        // We are simulating Player 2 receiving a network update
        String targetPlayer = "player2";
        System.out.println("\n[SYSTEM] Executing StateMasker for target: " + targetPlayer + "...");
        String filteredData = StateMasker.maskStateForPlayer(mockGameState.toString(), targetPlayer);

        System.out.println("\n[DEBUG] Filtered State (Ready for Encryption):\n" + new JSONObject(filteredData).toString(2));

        // STEP 3: Automated Validation
        System.out.println("\n=== RUNNING SECURITY ASSERTIONS ===");
        runAssertions(filteredData, targetPlayer);
    }

    /**
     * Helper method to generate dummy data.
     */
    private static JSONObject createMockBackendState() {
        JSONObject state = new JSONObject();

        // Data that triggers the GlobalSecurityStrategy
        state.put("infectionDeck", new JSONArray().put("Tokyo").put("Paris"));

        // Data that triggers the PrivateSecurityStrategy
        JSONArray players = new JSONArray();

        // Opponent (Should be heavily censored)
        JSONObject p1 = new JSONObject();
        p1.put("id", "player1");
        p1.put("ipAddress", "192.168.1.100");
        p1.put("cardsInHand", new JSONArray().put("City_London").put("Event_Airlift"));
        
        // Target Player (Should keep their data)
        JSONObject p2 = new JSONObject();
        p2.put("id", "player2");
        p2.put("ipAddress", "10.0.0.5");
        p2.put("cardsInHand", new JSONArray().put("City_Berlin"));

        players.put(p1);
        players.put(p2);
        state.put("players", players);

        return state;
    }

    /**
     * Helper method to verify the rules worked.
     */
    private static void runAssertions(String jsonOutput, String targetPlayer) {
        JSONObject result = new JSONObject(jsonOutput);
        boolean allPassed = true;

        // 1. Check Global Strategy (Anti-Cheat)
        if (result.has("infectionDeck")) {
            System.err.println("❌ GLOBAL STRATEGY FAILED: infectionDeck is still visible!");
            allPassed = false;
        } else {
            System.out.println("✅ GLOBAL STRATEGY PASSED: infectionDeck successfully removed.");
        }

        // 2. Check Private Strategy (Zero-Knowledge)
        JSONArray players = result.getJSONArray("players");
        for (int i = 0; i < players.length(); i++) {
            JSONObject p = players.getJSONObject(i);
            
            // Check Opponent
            if (p.getString("id").equals("player1")) {
                if (p.has("ipAddress") || p.has("cardsInHand")) {
                    System.err.println("❌ PRIVATE STRATEGY FAILED: Opponent's private data leaked!");
                    allPassed = false;
                } else if (p.has("hiddenCardCount") && p.getInt("hiddenCardCount") == 2) {
                    System.out.println("✅ PRIVATE STRATEGY PASSED: Opponent's cards masked (Count: 2).");
                }
            }

            // Check Target Player (Themselves)
            if (p.getString("id").equals(targetPlayer)) {
                if (!p.has("cardsInHand")) {
                    System.err.println("❌ SELF-DATA FAILED: Target player lost their own cards!");
                    allPassed = false;
                } else {
                    System.out.println("✅ SELF-DATA PASSED: Target player retained their own data.");
                }
            }
        }

        if (allPassed) {
            System.out.println("\n🎉 ALL TESTS PASSED! The Strategy pipeline is rock solid.");
        }
    }
}