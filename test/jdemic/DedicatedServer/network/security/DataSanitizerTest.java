/**
 * This test verifies that DataSanitizer safely accepts valid packets and
 * rejects malformed, incomplete, or unsupported input before it reaches
 * the rest of the server logic.
 */

package jdemic.DedicatedServer.network.security;

import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DataSanitizerTest {

    private final DataSanitizer dataSanitizer = new DataSanitizer();

    @Test
    void shouldAcceptValidPingPacket() {
        // This test verifies that a correctly formatted PING packet is accepted.
        String raw = """
                {
                  "type": "PING",
                  "timestamp": 1000,
                  "payload": {}
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isPresent());
        assertEquals(PacketType.PING, result.get().getType());
        assertEquals(1000L, result.get().getTimestamp());
        assertTrue(result.get().getPayload().isObject());
    }

    @Test
    void shouldAcceptValidGameDataPacket() {
        // This test verifies that a valid GAME_DATA packet with a JSON payload is accepted.
        String raw = """
                {
                  "type": "GAME_DATA",
                  "timestamp": 2000,
                  "payload": {
                    "action": "MOVE",
                    "target": "Paris"
                  }
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isPresent());
        assertEquals(PacketType.GAME_DATA, result.get().getType());
        assertEquals("MOVE", result.get().getPayload().get("action").asText());
        assertEquals("Paris", result.get().getPayload().get("target").asText());
    }

    @Test
    void shouldRejectNullInput() {
        // This test verifies that null input is safely rejected.
        Optional<Packet> result = dataSanitizer.sanitize(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectBlankInput() {
        // This test verifies that blank input is safely rejected.
        Optional<Packet> result = dataSanitizer.sanitize("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectInvalidJson() {
        // This test verifies that malformed JSON does not crash the sanitizer and is rejected safely.
        String raw = "{ invalid json";

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectPacketWithoutType() {
        // This test verifies that packets missing the required type field are rejected.
        String raw = """
                {
                  "timestamp": 1000,
                  "payload": {}
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectPacketWithoutTimestamp() {
        // This test verifies that packets missing the required timestamp field are rejected.
        String raw = """
                {
                  "type": "PING",
                  "payload": {}
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectPacketWithoutPayload() {
        // This test verifies that packets missing the required payload field are rejected.
        String raw = """
                {
                  "type": "PING",
                  "timestamp": 1000
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectPacketWithInvalidTypeField() {
        // This test verifies that the sanitizer rejects packets where type is not a string.
        String raw = """
                {
                  "type": 123,
                  "timestamp": 1000,
                  "payload": {}
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectPacketWithInvalidTimestampField() {
        // This test verifies that the sanitizer rejects packets where timestamp is not numeric.
        String raw = """
                {
                  "type": "PING",
                  "timestamp": "abc",
                  "payload": {}
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectPacketWithInvalidPayloadField() {
        // This test verifies that the sanitizer rejects packets where payload is not a JSON object.
        String raw = """
                {
                  "type": "PING",
                  "timestamp": 1000,
                  "payload": "not-an-object"
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectUnknownPacketType() {
        // This test verifies that unsupported packet types are rejected.
        String raw = """
                {
                  "type": "UNKNOWN_TYPE",
                  "timestamp": 1000,
                  "payload": {}
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectNegativeTimestamp() {
        // This test verifies that packets with negative timestamps are rejected.
        String raw = """
                {
                  "type": "PING",
                  "timestamp": -1,
                  "payload": {}
                }
                """;

        Optional<Packet> result = dataSanitizer.sanitize(raw);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectOversizedPacket() {
        // This test verifies that packets larger than the maximum allowed size are rejected.
        StringBuilder builder = new StringBuilder();
        builder.append("{\"type\":\"PING\",\"timestamp\":1,\"payload\":{\"data\":\"");
        builder.append("a".repeat(5000));
        builder.append("\"}}");

        Optional<Packet> result = dataSanitizer.sanitize(builder.toString());

        assertTrue(result.isEmpty());
    }
}