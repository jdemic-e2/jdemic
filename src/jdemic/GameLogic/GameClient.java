package jdemic.GameLogic;

import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

public class GameClient {

    private SecureSocket secureSocket;
    private PrintWriter out;
    private BufferedReader in;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean isConnected = false;
    private Thread listeningThread;
    private final List<PlayerUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private volatile JsonNode latestGameState;

    public interface PlayerUpdateListener {
        void onPlayersUpdated(JsonNode gameState);
    }

    public void addPlayerUpdateListener(PlayerUpdateListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        JsonNode currentGameState = latestGameState;
        if (currentGameState != null) {
            listener.onPlayersUpdated(currentGameState);
        }
    }

    public void removePlayerUpdateListener(PlayerUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlayersUpdated(JsonNode gameState) {
        for (PlayerUpdateListener listener : listeners) {
            listener.onPlayersUpdated(gameState);
        }
    }

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

            // Start maintaining the connection
            isConnected = true;
            startListeningThread();

        } catch (Exception e) {
            System.err.println("[GameClient] Critical error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startListeningThread() {
        listeningThread = new Thread(() -> {
            System.out.println("[GameClient] Starting listening thread...");
            while (isConnected) {
                try {
                    String received = receivePacket();
                    if (received != null) {
                        handleIncomingData(received);
                    }
                } catch (Exception e) {
                    if (isConnected) {
                        System.err.println("[GameClient] Error in listening thread: " + e.getMessage());
                        isConnected = false; // Trigger disconnection
                    }
                }
            }
            System.out.println("[GameClient] Listening thread stopped.");
        });
        listeningThread.setDaemon(true); // Make it a daemon thread so it doesn't prevent JVM exit
        listeningThread.start();
    }

    private void handleIncomingPacket(Packet packet) {
        System.out.println("[GameClient] Received packet: " + packet.getType());
        switch (packet.getType()) {
            case GAME_DATA:
                // TODO: Update local game state from packet.getPayload()
                System.out.println("[GameClient] Handling GAME_DATA packet.");
                break;
            case PONG:
                // Handle pong response
                System.out.println("[GameClient] Received PONG.");
                break;
            case ERROR:
                System.err.println("[GameClient] Received ERROR packet: " + packet.getPayload());
                break;
            default:
                System.out.println("[GameClient] Unhandled packet type: " + packet.getType());
                break;
        }
    }

    private void handleIncomingData(String data) {
        System.out.println("[GameClient] Received data: " + data);
        try {
            JsonNode gameState = objectMapper.readTree(data);
            latestGameState = gameState;
            notifyPlayersUpdated(gameState);
        } catch (Exception e) {
            System.err.println("[GameClient] Error parsing incoming data: " + e.getMessage());
        }
        // TODO: Parse as game state JSON and update local state
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
     * Sends a Packet object to the server by converting it to JSON first.
     */
    public void sendPacket(Packet packet) {
        sendPacket(packet.toJson());
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
        isConnected = false;
        if (listeningThread != null) {
            listeningThread.interrupt();
            try {
                listeningThread.join(1000); // Wait up to 1 second for thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
        System.out.println("[TEST] Starting client for network testing...");

        GameClient client = new GameClient();

        client.connectToServer("localhost", 9000);

        // Send CONNECT packet
        ObjectNode connectPayload = objectMapper.createObjectNode();
        connectPayload.put("playerName", "regin_77");
        Packet connectPacket = new Packet(PacketType.CONNECT, connectPayload);
        client.sendPacket(connectPacket);

        // Send GAME_DATA packet
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("PlayerID", "regin_77");
        payload.put("GameAction", "DRIVE_FERRY");
        payload.put("destination", "Atlanta");
        Packet p = new Packet(PacketType.GAME_DATA, System.currentTimeMillis(), payload);
        client.sendPacket(p);

        System.out.println("[TEST] Packets sent. Client is now listening. Press Enter to disconnect...");

        // Wait for user input to disconnect
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        client.disconnect();
        System.out.println("[TEST] Client disconnected.");
    }
}
