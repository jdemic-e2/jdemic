/**
 * This test verifies that PacketProcessor can safely receive validated packets
 * and route each supported packet type without throwing runtime errors.
 */
package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PacketProcessorTest {

    private final PacketProcessor packetProcessor = new PacketProcessor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldHandleNullPacketSafely() {
        // This test verifies that the processor safely handles null input without throwing exceptions.
        assertDoesNotThrow(() -> packetProcessor.process(null));
    }

    @Test
    void shouldProcessPingPacketWithoutThrowing() throws Exception {
        // This test verifies that a valid PING packet can be processed safely.
        Packet packet = new Packet(PacketType.PING, 1L, objectMapper.readTree("{}"));

        assertDoesNotThrow(() -> packetProcessor.process(packet));
    }

    @Test
    void shouldProcessPongPacketWithoutThrowing() throws Exception {
        // This test verifies that a valid PONG packet can be processed safely.
        Packet packet = new Packet(PacketType.PONG, 2L, objectMapper.readTree("{}"));

        assertDoesNotThrow(() -> packetProcessor.process(packet));
    }

    @Test
    void shouldProcessGameDataPacketWithoutThrowing() throws Exception {
        // This test verifies that a valid GAME_DATA packet can be processed safely.
        Packet packet = new Packet(PacketType.GAME_DATA, 3L, objectMapper.readTree("{\"action\":\"MOVE\"}"));

        assertDoesNotThrow(() -> packetProcessor.process(packet));
    }

    @Test
    void shouldProcessConnectPacketWithoutThrowing() throws Exception {
        // This test verifies that a valid CONNECT packet can be processed safely.
        Packet packet = new Packet(PacketType.CONNECT, 4L, objectMapper.readTree("{\"player\":\"P2\"}"));

        assertDoesNotThrow(() -> packetProcessor.process(packet));
    }

    @Test
    void shouldProcessDisconnectPacketWithoutThrowing() throws Exception {
        // This test verifies that a valid DISCONNECT packet can be processed safely.
        Packet packet = new Packet(PacketType.DISCONNECT, 5L, objectMapper.readTree("{}"));

        assertDoesNotThrow(() -> packetProcessor.process(packet));
    }

    @Test
    void shouldProcessErrorPacketWithoutThrowing() throws Exception {
        // This test verifies that a valid ERROR packet can be processed safely.
        Packet packet = new Packet(PacketType.ERROR, 6L, objectMapper.readTree("{\"message\":\"failure\"}"));

        assertDoesNotThrow(() -> packetProcessor.process(packet));
    }
}