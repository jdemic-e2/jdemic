package jdemic.GameLogic;

import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GameClient {

    private SecureSocket secureSocket;
    private PrintWriter out;
    private BufferedReader in;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    //Connecting happens only once
    public void connectToServer(String host, int port) {
        System.out.println("[GameClient] Connecting to " + host + ":" + port + "...");

        try {
            Socket rawSocket = new Socket(host, port);

            // Securing the socket
            this.secureSocket = SecureConnectionManager.wrapClientSocket(rawSocket);

            if (this.secureSocket == null) {
                System.out.println("[GameClient] Failed to connect securely.");
                return;
            }

            System.out.println("[GameClient] Handshake finalized. Secure channel created.");

            // I/O initialisation on the socket
            this.in = new BufferedReader(new InputStreamReader(rawSocket.getInputStream()));
            this.out = new PrintWriter(rawSocket.getOutputStream(), true);

        } catch (Exception e) {
            System.err.println("[GameClient] Critical error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a message (already JSON formated) to the server, applying AES encryption
     */
    public void sendPacket(String jsonPayload) {
        if (secureSocket == null || out == null) {
            System.err.println("[GameClient] Not connected to server!");
            return;
        }

        try {
            // Encrypt JSON package
            String mesajCriptat = secureSocket.encrypt(jsonPayload);
            out.println(mesajCriptat);
            System.out.println("[GameClient] Sent encrypted packet to server.");
        } catch (Exception e) {
            System.err.println("[GameClient] Error encrypting/sending packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method will be used to receive the updated Gamestate
     */
    public String receivePacket() {
        if (in == null || secureSocket == null) return null;
        
        try {
            String mesajCriptat = in.readLine();
            if (mesajCriptat != null) {
                return secureSocket.decrypt(mesajCriptat);
            }
        } catch (Exception e) {
            System.err.println("[GameClient] Error receiving packet: " + e.getMessage());
        }
        return null;
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (secureSocket != null && secureSocket.getRawSocket() != null && !secureSocket.getRawSocket().isClosed()) {
                secureSocket.getRawSocket().close();
            }
            System.out.println("[GameClient] Disconnected safely. Resources freed.");
        } catch (Exception e) {
            System.err.println("[GameClient] Error while closing connection: " + e.getMessage());
        }
    }
    // TERMINAL TESTING, seems to be working
    public static void main(String[] args) {
        System.out.println("[TEST] Pornim clientul pentru testarea retelei...");
        
        GameClient client = new GameClient();
        
        client.connectToServer("localhost", 9000);

        //create packet
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("PlayerID", "regin_77");
        payload.put("GameAction", "DRIVE_FERRY");
        payload.put("destination", "Atlanta");
        Packet p = new Packet(PacketType.GAME_DATA, System.currentTimeMillis(), payload);
        String json = p.toJson();
        
        client.sendPacket(json);
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) { }
        
        client.disconnect();
    }
}