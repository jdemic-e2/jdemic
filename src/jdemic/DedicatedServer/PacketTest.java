package jdemic.DedicatedServer;

import com.fasterxml.jackson.databind.node.ObjectNode;

import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PacketTest {
    public static void main(String[] args) {

        ObjectMapper objectMapper = new ObjectMapper();
        Packet ping = new Packet(PacketType.PING);
        System.out.println("> TEST 1 : packet with type only\n");

        if(ping.getType() == PacketType.PING)
        {
            System.out.println("test 1.1 type PASSED");
        }
        else{
            System.err.println("test 1.1 FAILED: packet is not of type ping");
        }

        if(ping.getTimestamp() > 0)
        {
            System.out.println("test 1.2 timestamp PASSED");
            }
        else{
            System.err.println("test 1.2 FAILED: packet timestamp is not valid");
        }

        if(ping.getPayload() != null && ping.getPayload().isObject())
        {
            System.out.println("test 1.3 payload PASSED");
        }
        else   {
            System.err.println("test 1.3 FAILED: invalid payload");
        }

        System.out.println("\n> TEST 2 : packet with type and payload only\n");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("senderId", "Player_1");
        payload.put("cityId", 7);
        Packet data = new Packet(PacketType.GAME_DATA, payload);

        if(data.getType() == PacketType.GAME_DATA)
        {
            System.out.println("test 2.1 type PASSED");
        }
        else{
            System.err.println("test 2.1 FAILED: packet is not of type game data");
        }

        if(data.getTimestamp() > 0)
        {
            System.out.println("test 2.2 timestamp PASSED");
            }
        else{
            System.err.println("test 2.2 FAILED: packet timestamp is not valid");
        }

        if(data.getPayload().has("senderId"))
        {
            System.out.println("| checking payload senderId...");
            if(data.getPayload().get("senderId").asText().equals("Player_1"))
            {
                System.out.println("test 2.3.1 payload senderId PASSED");
            }
            else System.err.println("test 2.3.1 FAILED: senderId value is incorrect");
            
            System.out.println("| checking payload cityId...");
            if(data.getPayload().get("cityId").asInt() == 7)
            {
                System.out.println("test 2.3.2 payload cityId PASSED");
            }
            else System.err.println("test 2.3.2 FAILED: cityId value is incorrect");
        }
        else   {
            System.err.println("test 2.3 FAILED: invalid payload");
        }

        System.out.println("\n> TEST 3: serializing json\n");
        String json = ping.toJson();

        if(json.contains("\"type\":\"PING\""))
        {
            System.out.println("test 3.1 type serialization PASSED");
        }
        else{
            System.err.println("test 3.1 FAILED: type not serialized correctly");
        }

        if(json.contains("\"timestamp\":"))
        {
            System.out.println("test 3.2 timestamp serialization PASSED");
        }
        else{
            System.err.println("test 3.2 FAILED: timestamp not serialized correctly");
        }

        if(json.contains("\"payload\":{}"))
        {
            System.out.println("test 3.3 payload serialization PASSED");
        }
        else{
            System.err.println("test 3.3 FAILED: payload not serialized correctly");
        }

        System.out.println("\n> TEST 4: validating packets (isValid method)\n");

        if(ping.isValid())
        {
            System.out.println("ping packet is valid!");
        }
        else{
            System.err.println("ping packet is invalid!");
        }

        if(data.isValid())
        {
            System.out.println("data packet is valid!");
        }
        else{
            System.err.println("data packet is invalid!");
        }

        long timestamp = 171800000000L; 
        Packet complete = new Packet(PacketType.CONNECT, timestamp, objectMapper.createObjectNode().put("senderId", "Player_2"));
        if(complete.isValid())
        {
            System.out.println("complete packet is valid!");
        }
        else{
            System.err.println("complete packet is invalid!");
        }

        System.out.println("\n> end of tests, printing packet details:\n");

        System.out.println(ping.getType());
        System.out.println(ping.getPayload());
        System.out.println(data.getType());
        System.out.println(data.getPayload());
        System.out.println(complete.getType());
        System.out.println(complete.getPayload());
    }
}
