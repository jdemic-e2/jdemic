package jdemic.DedicatedServer.network.security;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StateMaskerTest - Anti-Cheat & Masking Test Suite
 *
 * Verifica ca serverul nu trimite date secrete catre clienti.
 * Testeaza intregul pipeline: StateMasker -> GlobalSecurityStrategy -> PrivateSecurityStrategy.
 */
public class StateMaskerTest {

    private String rawGameStateJson;

    /**
     * Construieste un JSON complet de Game State inainte de fiecare test.
     * Contine: 3 jucatori (cu carti si IP-uri), infectionDeck, playerDeck.
     *
     * PrivateSecurityStrategy foloseste campul "id" pentru a identifica jucatorii
     * si "cardsInHand" pentru cartile din mana (nu "playerName" / "hand").
     */
    @BeforeEach
    public void buildFullGameState() {
        JSONObject gameState = new JSONObject();

        // --- Jucatori ---
        JSONArray players = new JSONArray();

        // Player_1 = jucatorul care primeste pachetul (target)
        JSONObject player1 = new JSONObject();
        player1.put("id", "Player_1");
        player1.put("playerName", "Player_1");
        player1.put("ipAddress", "192.168.1.10");
        player1.put("cardsInHand", new JSONArray().put("Atlanta").put("Madrid").put("Tokyo"));
        player1.put("role", "Medic");
        players.put(player1);

        // Player_2 = alt jucator (datele lui trebuie ascunse de la Player_1)
        JSONObject player2 = new JSONObject();
        player2.put("id", "Player_2");
        player2.put("playerName", "Player_2");
        player2.put("ipAddress", "192.168.1.20");
        player2.put("cardsInHand", new JSONArray().put("Paris").put("London"));
        player2.put("role", "Scientist");
        players.put(player2);

        // Player_3 = al treilea jucator
        JSONObject player3 = new JSONObject();
        player3.put("id", "Player_3");
        player3.put("playerName", "Player_3");
        player3.put("ipAddress", "10.0.0.5");
        player3.put("cardsInHand", new JSONArray().put("Cairo").put("Beijing").put("Mumbai").put("Sydney"));
        player3.put("role", "Researcher");
        players.put(player3);

        gameState.put("players", players);

        // --- Infection Deck (pachetul de virusuri) - trebuie STERS complet ---
        JSONArray infectionDeck = new JSONArray();
        infectionDeck.put("Lagos");
        infectionDeck.put("Kinshasa");
        infectionDeck.put("Johannesburg");
        gameState.put("infectionDeck", infectionDeck);

        // --- Player Deck (vizibil, nu e secret) ---
        gameState.put("playerDeckSize", 42);

        // --- Alte campuri de stare ---
        gameState.put("currentPlayer", "Player_1");
        gameState.put("actionsRemaining", 4);
        gameState.put("outbreakLevel", 0);
        gameState.put("gameStarted", true);

        rawGameStateJson = gameState.toString();
    }

    // =========================================================
    // SECTIUNEA 1: GlobalSecurityStrategy - infectionDeck
    // =========================================================

    @Test
    public void testInfectionDeckIsRemovedForTargetPlayer() {
        // infectionDeck trebuie sters - Player_1 nu trebuie sa stie ce virusuri urmeaza
        String masked = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_1");
        JSONObject result = new JSONObject(masked);

        assertFalse(result.has("infectionDeck"),
                "infectionDeck trebuie eliminat complet din starea trimisa catre Player_1.");
    }

    @Test
    public void testInfectionDeckIsRemovedForAllPlayers() {
        // infectionDeck trebuie sters pentru ORICE jucator, nu doar pentru Player_1
        String maskedP2 = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_2");
        String maskedP3 = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_3");

        assertFalse(new JSONObject(maskedP2).has("infectionDeck"),
                "infectionDeck trebuie eliminat si pentru Player_2.");
        assertFalse(new JSONObject(maskedP3).has("infectionDeck"),
                "infectionDeck trebuie eliminat si pentru Player_3.");
    }

    @Test
    public void testOtherFieldsAreNotRemovedByGlobalStrategy() {
        // GlobalSecurityStrategy nu trebuie sa stearga alte campuri (playerDeckSize, gameStarted etc.)
        String masked = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_1");
        JSONObject result = new JSONObject(masked);

        assertTrue(result.has("playerDeckSize"),
                "playerDeckSize trebuie sa ramana vizibil.");
        assertTrue(result.has("currentPlayer"),
                "currentPlayer trebuie sa ramana vizibil.");
        assertTrue(result.has("actionsRemaining"),
                "actionsRemaining trebuie sa ramana vizibil.");
        assertTrue(result.has("outbreakLevel"),
                "outbreakLevel trebuie sa ramana vizibil.");
        assertTrue(result.has("gameStarted"),
                "gameStarted trebuie sa ramana vizibil.");
    }

    // =========================================================
    // SECTIUNEA 2: PrivateSecurityStrategy - IP-uri
    // =========================================================

    @Test
    public void testOtherPlayersIpAddressIsHiddenFromPlayer1() {
        String masked = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_1");
        JSONArray players = new JSONObject(masked).getJSONArray("players");

        // Cautam Player_2 si Player_3 in array
        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);
            String id = player.getString("id");

            if (!id.equals("Player_1")) {
                assertFalse(player.has("ipAddress"),
                        "IP-ul lui " + id + " nu trebuie sa fie vizibil pentru Player_1.");
            }
        }
    }

    @Test
    public void testTargetPlayerKeepsOwnIpAddress() {
        // Player_1 isi vede propriul IP (nu e ascuns de la sine insusi)
        String masked = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_1");
        JSONArray players = new JSONObject(masked).getJSONArray("players");

        JSONObject player1 = findPlayer(players, "Player_1");
        assertNotNull(player1, "Player_1 trebuie sa existe in rezultat.");
        assertTrue(player1.has("ipAddress"),
                "Player_1 trebuie sa isi vada propriul IP.");
        assertEquals("192.168.1.10", player1.getString("ipAddress"),
                "IP-ul lui Player_1 trebuie sa fie cel corect.");
    }

    // =========================================================
    // SECTIUNEA 3: PrivateSecurityStrategy - Carti
    // =========================================================

    @Test
    public void testTargetPlayerSeesOwnCards() {
        // Player_1 trebuie sa isi vada propriile carti
        String masked = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_1");
        JSONArray players = new JSONObject(masked).getJSONArray("players");

        JSONObject player1 = findPlayer(players, "Player_1");
        assertNotNull(player1);
        assertTrue(player1.has("cardsInHand"),
                "Player_1 trebuie sa isi vada propriile carti.");
        assertEquals(3, player1.getJSONArray("cardsInHand").length(),
                "Player_1 trebuie sa aiba 3 carti vizibile.");
    }

    @Test
    public void testOtherPlayersCardsAreHiddenFromPlayer1() {
        // Player_1 NU trebuie sa vada cartile lui Player_2 sau Player_3
        String masked = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_1");
        JSONArray players = new JSONObject(masked).getJSONArray("players");

        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);
            String id = player.getString("id");

            if (!id.equals("Player_1")) {
                assertFalse(player.has("cardsInHand"),
                        "Cartile lui " + id + " trebuie ascunse de la Player_1.");
            }
        }
    }

    @Test
    public void testHiddenCardCountIsCorrectForOtherPlayers() {
        // In locul cartilor, trebuie sa existe hiddenCardCount cu numarul corect
        String masked = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_1");
        JSONArray players = new JSONObject(masked).getJSONArray("players");

        JSONObject player2 = findPlayer(players, "Player_2");
        JSONObject player3 = findPlayer(players, "Player_3");

        assertNotNull(player2);
        assertNotNull(player3);

        assertTrue(player2.has("hiddenCardCount"),
                "Player_2 trebuie sa aiba hiddenCardCount in loc de cardsInHand.");
        assertEquals(2, player2.getInt("hiddenCardCount"),
                "Player_2 are 2 carti, hiddenCardCount trebuie sa fie 2.");

        assertTrue(player3.has("hiddenCardCount"),
                "Player_3 trebuie sa aiba hiddenCardCount in loc de cardsInHand.");
        assertEquals(4, player3.getInt("hiddenCardCount"),
                "Player_3 are 4 carti, hiddenCardCount trebuie sa fie 4.");
    }

    // =========================================================
    // SECTIUNEA 4: Perspective diferite (alt jucator ca target)
    // =========================================================

    @Test
    public void testMaskingWorksCorrectlyForPlayer2Perspective() {
        // Cand Player_2 e target: el isi vede cartile, Player_1 si Player_3 nu
        String masked = StateMasker.maskStateForPlayer(rawGameStateJson, "Player_2");
        JSONArray players = new JSONObject(masked).getJSONArray("players");

        JSONObject player1 = findPlayer(players, "Player_1");
        JSONObject player2 = findPlayer(players, "Player_2");
        JSONObject player3 = findPlayer(players, "Player_3");

        assertNotNull(player1);
        assertNotNull(player2);
        assertNotNull(player3);

        // Player_2 isi vede cartile
        assertTrue(player2.has("cardsInHand"), "Player_2 trebuie sa isi vada cartile.");
        assertEquals(2, player2.getJSONArray("cardsInHand").length());

        // Player_1 si Player_3 nu sunt vizibili pentru Player_2
        assertFalse(player1.has("cardsInHand"), "Cartile lui Player_1 trebuie ascunse de la Player_2.");
        assertFalse(player3.has("cardsInHand"), "Cartile lui Player_3 trebuie ascunse de la Player_2.");

        // IP-urile celorlalti sunt ascunse
        assertFalse(player1.has("ipAddress"), "IP-ul lui Player_1 nu trebuie vizibil pentru Player_2.");
        assertFalse(player3.has("ipAddress"), "IP-ul lui Player_3 nu trebuie vizibil pentru Player_2.");
    }

    // =========================================================
    // SECTIUNEA 5: Edge cases (cazuri limita)
    // =========================================================

    @Test
    public void testMaskingWithNoInfectionDeckDoesNotCrash() {
        // Daca JSON-ul nu contine infectionDeck, masking-ul nu trebuie sa crape
        JSONObject stateWithoutDeck = new JSONObject();
        stateWithoutDeck.put("players", new JSONArray());
        stateWithoutDeck.put("gameStarted", false);

        String result = StateMasker.maskStateForPlayer(stateWithoutDeck.toString(), "Player_1");
        assertNotNull(result, "Rezultatul nu trebuie sa fie null chiar si fara infectionDeck.");
        assertFalse(result.isEmpty(), "Rezultatul nu trebuie sa fie gol.");
    }

    @Test
    public void testMaskingWithEmptyPlayersArrayDoesNotCrash() {
        // Lista de jucatori goala nu trebuie sa cauzeze exceptii
        JSONObject emptyState = new JSONObject();
        emptyState.put("players", new JSONArray());
        emptyState.put("infectionDeck", new JSONArray().put("Lagos"));

        String result = StateMasker.maskStateForPlayer(emptyState.toString(), "Player_1");
        JSONObject parsed = new JSONObject(result);

        assertFalse(parsed.has("infectionDeck"),
                "infectionDeck trebuie eliminat chiar si cu lista de jucatori goala.");
    }

    @Test
    public void testMaskingWithInvalidJsonReturnsFallback() {
        // JSON invalid nu trebuie sa arunce exceptie - fallback-ul returneaza input-ul original
        String invalidJson = "not a valid json {{{}";
        String result = StateMasker.maskStateForPlayer(invalidJson, "Player_1");
        assertNotNull(result, "Rezultatul trebuie sa fie non-null chiar si pentru JSON invalid.");
    }

    @Test
    public void testPlayerWithNoCardsIsNotAffected() {
        // Un jucator fara campul 'cardsInHand' nu trebuie sa cauzeze erori
        JSONObject state = new JSONObject();
        JSONArray players = new JSONArray();

        JSONObject playerNoCards = new JSONObject();
        playerNoCards.put("id", "Player_2");
        playerNoCards.put("playerName", "Player_2");
        playerNoCards.put("ipAddress", "10.0.0.1");
        // Nu punem 'cardsInHand' intentionat

        players.put(playerNoCards);
        state.put("players", players);

        String result = StateMasker.maskStateForPlayer(state.toString(), "Player_1");
        JSONObject parsed = new JSONObject(result);
        JSONObject p2 = parsed.getJSONArray("players").getJSONObject(0);

        assertFalse(p2.has("cardsInHand"),
                "Player fara cardsInHand nu trebuie sa aiba cardsInHand dupa masking.");
        assertFalse(p2.has("hiddenCardCount"),
                "hiddenCardCount nu trebuie adaugat daca nu exista cardsInHand.");
    }

    // =========================================================
    // Helper
    // =========================================================

    /**
     * Cauta un jucator dupa id in JSONArray.
     * Returneaza null daca nu il gaseste.
     */
    private JSONObject findPlayer(JSONArray players, String playerId) {
        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);
            if (playerId.equals(player.optString("id"))) {
                return player;
            }
        }
        return null;
    }
}
