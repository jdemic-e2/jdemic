package jdemic.DedicatedServer.network.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Packet represents the internal model of a valid network message in the server.
 * It stores the packet type, the timestamp, and the JSON payload after the raw inputs
 * has already been checked and accepted. In the project flow, 'DataSanitizer' converts incoming
 * raw JSON into a 'Packet', and then this object can be safely passed to the next processing step. 
 * This makes communication cleaner and safer, because the rest of the system works with structured packet
 * objects instead of raw strings.
 */
public class Packet {

    private final PacketType type;
    private final long timestamp;
    private final JsonNode payload;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Packet(PacketType type, long timestamp, JsonNode payload) {
        this.type = type;
        this.timestamp = timestamp;
        this.payload = (payload != null) ? payload : objectMapper.createObjectNode();
    }

    public Packet(PacketType type, JsonNode payload)
    {
        this(type, System.currentTimeMillis(), payload);
    }

    public Packet(PacketType type)
    {
        this(type, System.currentTimeMillis(), objectMapper.createObjectNode());
    }

    public boolean isValid()
    {
        return type != null && payload != null && payload.isObject();
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

    public String toJson()
    {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", type.name());
            node.put("timestamp", timestamp);
            node.set("payload", payload);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException("[packet] Error converting packet to JSON: " + e.getMessage(), e);
        }
    }
}