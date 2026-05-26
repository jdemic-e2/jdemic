/**
 * This test verifies that the Packet class correctly stores and exposes
 * the internal data of a validated network packet, including its type,
 * timestamp, and JSON payload.
 */

package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldStoreConstructorValuesCorrectly() throws Exception {
        // This test verifies that the packet constructor stores type, timestamp, and payload correctly.
        JsonNode payload = objectMapper.readTree("{\"action\":\"MOVE\"}");
        Packet packet = new Packet(PacketType.GAME_DATA, 123456L, payload);

        assertEquals(PacketType.GAME_DATA, packet.getType());
        assertEquals(123456L, packet.getTimestamp());
        assertEquals(payload, packet.getPayload());
    }

    @Test
    void shouldKeepPayloadAsJsonNode() throws Exception {
        // This test verifies that the payload is preserved as a JSON object after packet creation.
        JsonNode payload = objectMapper.readTree("{\"target\":\"Paris\"}");
        Packet packet = new Packet(PacketType.CONNECT, 999L, payload);

        assertNotNull(packet.getPayload());
        assertEquals("Paris", packet.getPayload().get("target").asText());
    }

    @Test
    void toStringShouldContainUsefulPacketInformation() throws Exception {
        // This test verifies that the string representation contains the most important packet fields.
        JsonNode payload = objectMapper.readTree("{\"key\":\"value\"}");
        Packet packet = new Packet(PacketType.PING, 50L, payload);

        String result = packet.toString();

        assertNotNull(result);
        assertTrue(result.contains("PING"));
        assertTrue(result.contains("50"));
        assertTrue(result.contains("payload"));
    }

    @Test
    void shouldDefaultNullPayloadToEmptyObject() {
        Packet packet = new Packet(PacketType.PING, 50L, null);

        assertNotNull(packet.getPayload());
        assertTrue(packet.getPayload().isObject());
        assertEquals(0, packet.getPayload().size());
        assertTrue(packet.isValid());
    }

    @Test
    void shouldReportInvalidPackets() throws Exception {
        assertFalse(new Packet(null, 50L, objectMapper.readTree("{}")).isValid());
        assertFalse(new Packet(PacketType.PING, 0L, objectMapper.readTree("{}")).isValid());
        assertFalse(new Packet(PacketType.PING, -1L, objectMapper.readTree("{}")).isValid());
        assertFalse(new Packet(PacketType.PING, 50L, objectMapper.readTree("[]")).isValid());
    }

    @Test
    void toJsonShouldSerializePacketFields() throws Exception {
        Packet packet = new Packet(PacketType.GAME_DATA, 123L, objectMapper.readTree("{\"action\":\"MOVE\"}"));

        JsonNode serialized = objectMapper.readTree(packet.toJson());

        assertEquals("GAME_DATA", serialized.get("type").asText());
        assertEquals(123L, serialized.get("timestamp").asLong());
        assertEquals("MOVE", serialized.get("payload").get("action").asText());
    }

    @Test
    void toJsonShouldFailForPacketWithoutType() {
        Packet packet = new Packet(null, 123L, objectMapper.createObjectNode());

        RuntimeException exception = assertThrows(RuntimeException.class, packet::toJson);

        assertTrue(exception.getMessage().contains("Error converting packet to JSON"));
    }
}
