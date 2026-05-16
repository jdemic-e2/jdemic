package jdemic.DedicatedServer.network.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerStatusUiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void healthEndpointShouldReturnJsonStatus() throws Exception {
        DedicatedServerConfig config = new DedicatedServerConfig(9100, true, "localhost", 0, false);
        ServerStatusUi statusUi = new ServerStatusUi(config);

        try {
            statusUi.start();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + statusUi.getPort() + "/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"status\":\"ok\""));
            assertTrue(response.body().contains("\"gameServerPort\":9100"));
        } finally {
            statusUi.stop();
        }
    }

    @Test
    void healthJsonShouldSerializeStableStatusFields() throws Exception {
        DedicatedServerConfig config = new DedicatedServerConfig(9001, true, "localhost", 8081, false);
        ServerStatusUi statusUi = new ServerStatusUi(config);

        JsonNode json = objectMapper.readTree(statusUi.buildHealthJson());

        assertEquals("ok", json.get("status").asText());
        assertEquals("jdemic-dedicated-server", json.get("service").asText());
        assertEquals(9001, json.get("gameServerPort").asInt());
        assertEquals(3, json.size());
    }

    @Test
    void healthEndpointShouldRejectNonGetRequests() throws Exception {
        DedicatedServerConfig config = new DedicatedServerConfig(9100, true, "localhost", 0, false);
        ServerStatusUi statusUi = new ServerStatusUi(config);

        try {
            statusUi.start();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + statusUi.getPort() + "/health"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(405, response.statusCode());
        } finally {
            statusUi.stop();
        }
    }

    @Test
    void disabledStatusUiShouldNotBindHttpServer() throws Exception {
        DedicatedServerConfig config = new DedicatedServerConfig(9100, false, "localhost", 0, false);
        ServerStatusUi statusUi = new ServerStatusUi(config);

        statusUi.start();

        assertEquals(0, statusUi.getPort());
        statusUi.stop();
    }
}
