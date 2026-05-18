package jdemic.DedicatedServer.network.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdemic.DedicatedServer.network.core.DedicatedServerConfig;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ServerStatusUiTest {

    @Test
    void stateSummaryDoesNotExposeHandsOrDecks() {
        PlayerState alice = new PlayerState("Alice");
        alice.addCard(new Card("Hidden card", CardType.EVENT, null));
        GameManager gameManager = new GameManager(new ArrayList<>(List.of(alice)), false);

        ServerStatusUi statusUi = new ServerStatusUi(
                new DedicatedServerConfig(9000, false, "localhost", 8080, false),
                () -> gameManager,
                () -> 1,
                () -> null,
                () -> {}
        );

        ObjectNode summary = statusUi.buildGameStateSummary(gameManager);
        JsonNode player = summary.get("players").get(0);

        assertFalse(summary.has("cardDeck"));
        assertFalse(summary.has("map"));
        assertFalse(player.has("hand"));
        assertEquals(1, player.get("handCount").asInt());
    }
}
