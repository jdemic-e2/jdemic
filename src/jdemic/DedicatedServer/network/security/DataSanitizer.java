package jdemic.DedicatedServer.network.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;

import java.util.Optional;

/**
 * DataSanitizer is responsible for validating raw packet data received from the network
 * before it reaches the rest of the server logic. It safely parses incoming JSON, check that all
 * required fields are present, validates their types, and ensures that only supported packet types
 * are accepted. Invalid packets are rejected without throwing errors further into the system, helping
 * keep the server stable against corrupted or malicious input
 */
public class DataSanitizer {
    private static final int MAX_PACKET_SIZE = 4096;

    private final ObjectMapper objectMapper;

    public DataSanitizer() {
        this.objectMapper = new ObjectMapper();
    }

    public Optional<Packet> sanitize(String rawInput) {
        if (!isRawInputValid(rawInput)) {
            logReject("Raw input is null, blank, or too large.");
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(rawInput);

            if (!hasRequiredFields(root)) {
                logReject("Missing required fields.");
                return Optional.empty();
            }

            if (!hasValidFieldTypes(root)) {
                logReject("Invalid field types.");
                return Optional.empty();
            }

            String typeText = root.get("type").asText();

            Optional<PacketType> packetTypeOpt = parsePacketType(typeText);
            if (packetTypeOpt.isEmpty()) {
                logReject("Invalid packet type: " + typeText);
                return Optional.empty();
            }

            long timestamp = root.get("timestamp").asLong();
            if (timestamp < 0) {
                logReject("Invalid timestamp: negative value.");
                return Optional.empty();
            }

            JsonNode payload = root.get("payload");

            Packet packet = new Packet(packetTypeOpt.get(), timestamp, payload);
            return Optional.of(packet);

        } catch (Exception e) {
            logReject("Exception while parsing packet: " + e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isRawInputValid(String rawInput) {
        if (rawInput == null) {
            return false;
        }

        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        return trimmed.length() <= MAX_PACKET_SIZE;
    }

    private boolean hasRequiredFields(JsonNode root) {
        return root.has("type")
                && root.has("timestamp")
                && root.has("payload");
    }

    private boolean hasValidFieldTypes(JsonNode root) {
        JsonNode typeNode = root.get("type");
        JsonNode timestampNode = root.get("timestamp");
        JsonNode payloadNode = root.get("payload");

        return typeNode != null && typeNode.isTextual()
                && timestampNode != null && timestampNode.isNumber()
                && payloadNode != null && payloadNode.isObject();
    }

    private Optional<PacketType> parsePacketType(String typeText) {
        try {
            return Optional.of(PacketType.valueOf(typeText));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private void logReject(String reason) {
        System.err.println("[DataSanitizer] Rejected packet: " + reason);
    }
}