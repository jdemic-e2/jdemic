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

    /**
     * Main constructor with all specific details.
     * @param type The specific type of the network packet (e.g., PING, GAME_DATA).
     * @see PacketType 
     * @param timestamp The exact time the packet was created (in milliseconds).
     * @param payload The JSON data. If null, an empty JSON object is assigned safely.
     */
    public Packet(PacketType type, long timestamp, JsonNode payload) {
        this.type = type;
        this.timestamp = timestamp;
        this.payload = (payload != null) ? payload : objectMapper.createObjectNode();
    }


    /**
     * Constructor that defaults the timestamp to the current time in milliseconds.
     * @param type The specific type of the network packet (e.g., PING, GAME_DATA).
     * @param payload The JSON data. If null, an empty JSON object is assigned safely.
     */
    public Packet(PacketType type, JsonNode payload)
    {
        this(type, System.currentTimeMillis(), payload);
    }


    /**
     * Constructor that defaults the timestamp to the current time and the payload to an empty payload.
     * @param type The specific type of the network packet (e.g., PING, GAME_DATA).
     */
    public Packet(PacketType type)
    {
        this(type, System.currentTimeMillis(), objectMapper.createObjectNode());
    }


    /**
     * Validates the internal integrity of the packet.
     * @return true if the packet is valid, false otherwise.
     */
    public boolean isValid()
    {
        return type != null && timestamp > 0 && payload != null && payload.isObject();
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

    /**
     * Serializes the Packet object into a valid JSON string ready for network transmission.
     * @return The stringified JSON representation of the packet.
     * @throws RuntimeException if Jackson fails to convert the object to a JSON string.
     */
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

    public static Packet fromJson(String jsonString)
    {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            PacketType type = PacketType.valueOf(rootNode.get("type").asText());
            long timestamp = rootNode.get("timestamp").asLong();
            JsonNode payload = rootNode.get("payload");
            return new Packet(type, timestamp, payload);
        } catch (Exception e) {
            throw new RuntimeException("[packet] Error parsing JSON to Packet: " + e.getMessage(), e);
        }
    }
}