package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Packet represents the internal model of a valid network message in the server.
 * It stores the packet type, the timestamp, and the JSON payload after the raw inputs
 * has already been checked and accepted. In the project flow, 'DataSanitizer' converts incoming
 * raw JSON into a 'Packet', and then this object can be safely passed to the next processing step. T
 * This makes communication cleaner and safer, because th rest of the system works with structured paxket
 * objects instead of raw strings.
 */
public class Packet {
    private final PacketType type;
    private final long timestamp;
    private final JsonNode payload;

    public Packet(PacketType type, long timestamp, JsonNode payload) {
        this.type = type;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public PacketType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public JsonNode getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "type=" + type +
                ", timestamp=" + timestamp +
                ", payload=" + payload +
                '}';
    }
}