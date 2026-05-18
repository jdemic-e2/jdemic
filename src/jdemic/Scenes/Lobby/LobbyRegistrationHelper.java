package jdemic.Scenes.Lobby;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import jdemic.GameLogic.GameClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class LobbyRegistrationHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int REGISTRATION_TIMEOUT_SECONDS = 3;

    private LobbyRegistrationHelper() {
    }

    static boolean connectAndRegister(GameClient client, String host, int port, String playerName)
            throws InterruptedException {
        if (client == null || !client.connectToServer(host, port)) {
            return false;
        }

        CountDownLatch registered = new CountDownLatch(1);
        GameClient.PlayerUpdateListener registrationListener = gameState -> {
            if (containsPlayer(gameState, playerName)) {
                registered.countDown();
            }
        };
        client.addPlayerUpdateListener(registrationListener);

        try {
            ObjectNode payload = OBJECT_MAPPER.createObjectNode();
            payload.put("playerName", playerName);
            client.sendPacket(new Packet(PacketType.CONNECT, payload));

            return registered.await(REGISTRATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            client.removePlayerUpdateListener(registrationListener);
        }
    }

    static boolean containsPlayer(JsonNode gameState, String playerName) {
        if (gameState == null || playerName == null) {
            return false;
        }

        JsonNode players = gameState.has("players")
                ? gameState.get("players")
                : gameState.get("playerArray");
        if (players == null || !players.isArray()) {
            return false;
        }

        for (JsonNode player : players) {
            if (player.has("playerName") && playerName.equalsIgnoreCase(player.get("playerName").asText())) {
                return true;
            }
        }
        return false;
    }
}
