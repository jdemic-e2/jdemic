/**
 * This test verifies that PacketProcessor can safely receive validated packets
 * and route each supported packet type without throwing runtime errors.
 */
package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketProcessorTest {

    private static final Logger PACKET_PROCESSOR_LOGGER = Logger.getLogger(PacketProcessor.class.getName());

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

    @Test
    void shouldRoutePingPongAndGameDataPackets() throws Exception {
        String output = capturePacketProcessorLog(() -> {
            packetProcessor.process(new Packet(PacketType.PING, 1L, objectMapper.createObjectNode()));
            packetProcessor.process(new Packet(PacketType.PONG, 2L, objectMapper.createObjectNode()));
            packetProcessor.process(new Packet(PacketType.GAME_DATA, 3L, objectMapper.createObjectNode().put("action", "MOVE")));
        });

        assertTrue(output.contains("Received PING packet"));
        assertTrue(output.contains("Received PONG packet"));
        assertTrue(output.contains("Received GAME_DATA packet"));
        assertTrue(output.contains("\"action\":\"MOVE\""));
    }

    @Test
    void shouldRouteConnectionLifecyclePackets() throws Exception {
        String output = capturePacketProcessorLog(() -> {
            packetProcessor.process(new Packet(PacketType.CONNECT, 4L, objectMapper.createObjectNode().put("player", "P2")));
            packetProcessor.process(new Packet(PacketType.DISCONNECT, 5L, objectMapper.createObjectNode()));
        });

        assertTrue(output.contains("Received CONNECT packet"));
        assertTrue(output.contains("\"player\":\"P2\""));
        assertTrue(output.contains("Received DISCONNECT packet"));
    }

    @Test
    void shouldRouteErrorPacketsToErrorOutput() throws Exception {
        Packet packet = new Packet(PacketType.ERROR, 6L, objectMapper.createObjectNode().put("message", "failure"));

        String output = capturePacketProcessorLog(() -> packetProcessor.process(packet));

        assertTrue(output.contains("Received ERROR packet"));
        assertTrue(output.contains("\"message\":\"failure\""));
    }

    @Test
    void shouldReportUnsupportedNullPacketType() throws Exception {
        Packet packet = new Packet(null, 7L, objectMapper.createObjectNode());

        String output = capturePacketProcessorLog(() -> packetProcessor.process(packet));

        assertTrue(output.contains("Unsupported packet type: null"));
    }

    private String capturePacketProcessorLog(Runnable action) {
        StringBuilder captured = new StringBuilder();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.append(record.getMessage()).append(System.lineSeparator());
            }

            @Override
            public void flush() {
                // StringBuilder writes do not need flushing.
            }

            @Override
            public void close() {
                // Nothing to close.
            }
        };

        Level originalLevel = PACKET_PROCESSOR_LOGGER.getLevel();
        boolean originalUseParentHandlers = PACKET_PROCESSOR_LOGGER.getUseParentHandlers();
        handler.setLevel(Level.ALL);
        PACKET_PROCESSOR_LOGGER.setLevel(Level.ALL);
        PACKET_PROCESSOR_LOGGER.setUseParentHandlers(false);
        PACKET_PROCESSOR_LOGGER.addHandler(handler);

        try {
            action.run();
            handler.flush();
            return captured.toString();
        } finally {
            PACKET_PROCESSOR_LOGGER.removeHandler(handler);
            PACKET_PROCESSOR_LOGGER.setLevel(originalLevel);
            PACKET_PROCESSOR_LOGGER.setUseParentHandlers(originalUseParentHandlers);
        }
    }

}
