package jdemic.DedicatedServer.network.core;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerStatusUiTest {

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
}
