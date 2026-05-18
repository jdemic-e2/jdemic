/**
 * This test verifies that PacketProcessor can safely receive validated packets
 * and route each supported packet type without throwing runtime errors.
 */
package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shouldRoutePingPongAndGameDataPackets() throws Exception {
        String output = captureStdout(() -> {
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
        String output = captureStdout(() -> {
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

        String output = captureStderr(() -> packetProcessor.process(packet));

        assertTrue(output.contains("Received ERROR packet"));
        assertTrue(output.contains("\"message\":\"failure\""));
    }

    @Test
    void shouldReportUnsupportedNullPacketType() throws Exception {
        Packet packet = new Packet(null, 7L, objectMapper.createObjectNode());

        String output = captureStderr(() -> packetProcessor.process(packet));

        assertTrue(output.contains("Unsupported packet type: null"));
    }

    private String captureStdout(Runnable action) {
        return captureOutput(action, true);
    }

    private String captureStderr(Runnable action) {
        return captureOutput(action, false);
    }

    private String captureOutput(Runnable action, boolean stdout) {
        PrintStream original = stdout ? System.out : System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream replacement = new PrintStream(captured, true, StandardCharsets.UTF_8);

        if (stdout) {
            System.setOut(replacement);
        } else {
            System.setErr(replacement);
        }

        try {
            action.run();
            replacement.flush();
            return captured.toString(StandardCharsets.UTF_8);
        } finally {
            if (stdout) {
                System.setOut(original);
            } else {
                System.setErr(original);
            }
        }
    }
}
