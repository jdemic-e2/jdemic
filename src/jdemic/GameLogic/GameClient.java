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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

public class GameClient {

    private static final Logger LOGGER = Logger.getLogger(GameClient.class.getName());
    private SecureSocket secureSocket;
    private PrintWriter out;
    private BufferedReader in;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean isConnected = false;
    private Thread listeningThread;
    private final List<PlayerUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicReference<JsonNode> latestGameState = new AtomicReference<>();

    public interface PlayerUpdateListener {
        void onPlayersUpdated(JsonNode gameState);
    }

    public void addPlayerUpdateListener(PlayerUpdateListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        JsonNode currentGameState = latestGameState.get();
        if (currentGameState != null) {
            listener.onPlayersUpdated(currentGameState);
        }
    }

    public void removePlayerUpdateListener(PlayerUpdateListener listener) {
        listeners.remove(listener);
    }

    public void clearPlayerUpdateListeners() {
        listeners.clear();
    }
    private void notifyPlayersUpdated(JsonNode gameState) {
        for (PlayerUpdateListener listener : listeners) {
            listener.onPlayersUpdated(gameState);
        }
    }

    //Connecting happens only once
    public boolean connectToServer(String host, int port) {
        LOGGER.info("[GameClient] Connecting to " + host + ":" + port + "...");

        try {
            Socket rawSocket = new Socket(host, port);

            // Securing the socket
            this.secureSocket = SecureConnectionManager.wrapClientSocket(rawSocket);

            if (this.secureSocket == null) {
                LOGGER.info("[GameClient] Failed to connect securely.");

                return false;
            }

            LOGGER.info("[GameClient] Handshake finalized. Secure channel created.");

            // I/O initialisation on the socket
            this.in = new BufferedReader(new InputStreamReader(rawSocket.getInputStream()));
            this.out = new PrintWriter(rawSocket.getOutputStream(), true);

            // Start maintaining the connection
            isConnected = true;
            startListeningThread();
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Critical error connecting to server.", e);
            return false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void startListeningThread() {
        listeningThread = new Thread(() -> {
            LOGGER.info("[GameClient] Starting listening thread...");
            while (isConnected) {
                try {
                    String received = receivePacket();
                    if (received != null) {
                        handleIncomingData(received);
                    }
                } catch (Exception e) {
                    if (isConnected) {
                        LOGGER.severe("[GameClient] Error in listening thread: " + e.getMessage());
                        isConnected = false; // Trigger disconnection
                    }
                }
            }
            LOGGER.info("[GameClient] Listening thread stopped.");
        });
        listeningThread.setDaemon(true); // Make it a daemon thread so it doesn't prevent JVM exit
        listeningThread.start();
    }

    private void handleIncomingPacket(Packet packet) {
        LOGGER.info("[GameClient] Received packet: " + packet.getType());
        switch (packet.getType()) {
            case GAME_DATA:
                LOGGER.info("[GameClient] Handling GAME_DATA packet.");
                latestGameState.set(packet.getPayload());
                notifyPlayersUpdated(packet.getPayload());
                break;
            case PONG:
                // Handle pong response
                LOGGER.info("[GameClient] Received PONG.");
                break;
            case ERROR:
                LOGGER.severe("[GameClient] Received ERROR packet: " + packet.getPayload());
                break;
            default:
                LOGGER.info("[GameClient] Unhandled packet type: " + packet.getType());
                break;
        }
    }

    private void handleIncomingData(String data) {
        LOGGER.info("[GameClient] Received data: " + data);
        try {
            JsonNode root = objectMapper.readTree(data);
            if (root.has("type") && root.has("payload")) {
                Packet packet = Packet.fromJson(data);
                if (packet != null) {
                    handleIncomingPacket(packet);
                }
                return;
            }

            JsonNode gameState = root;
            latestGameState.set(gameState);
            notifyPlayersUpdated(gameState);
        } catch (Exception e) {
            LOGGER.severe("[GameClient] Error parsing incoming data: " + e.getMessage());
        }
    }

    /**
     * Sends a message (already JSON formated) to the server, applying AES encryption
     */
    public void sendPacket(String jsonPayload) {
        if (secureSocket == null || out == null) {
            LOGGER.severe("[GameClient] Not connected to server!");
            return;
        }

        try {
            // Encrypt JSON package
            String mesajCriptat = secureSocket.encrypt(jsonPayload);
            out.println(mesajCriptat);
            LOGGER.info("[GameClient] Sent encrypted packet to server.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error encrypting or sending packet.", e);
        }
    }

    /**
     * Sends a Packet object to the server by converting it to JSON first.
     */
    public void sendPacket(Packet packet) {
        sendPacket(packet.toJson());
    }

    public void disconnectFromLobby() {
        if (isConnected && secureSocket != null && out != null) {
            sendPacket(new Packet(PacketType.DISCONNECT));
        }
        disconnect();
    }

    /**
     * This method will be used to receive the updated Gamestate
     */
    public String receivePacket() {
        if (in == null || secureSocket == null) return null;
        
        try {
            String mesajCriptat = in.readLine();
            if (mesajCriptat == null) {
                isConnected = false;
                return null;
            }
            if (mesajCriptat != null) {
                return secureSocket.decrypt(mesajCriptat);
            }
        } catch (Exception e) {
            LOGGER.severe("[GameClient] Error receiving packet: " + e.getMessage());
            isConnected = false;
        }
        return null;
    }

    public void disconnect() {
        isConnected = false;
        listeners.clear();
        latestGameState.set(null);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (secureSocket != null && secureSocket.getRawSocket() != null && !secureSocket.getRawSocket().isClosed()) {
                secureSocket.getRawSocket().close();
            }
            if (listeningThread != null) {
                listeningThread.interrupt();
                try {
                    listeningThread.join(1000); // Wait up to 1 second for thread to finish
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            LOGGER.info("[GameClient] Disconnected safely. Resources freed.");
        } catch (Exception e) {
            LOGGER.severe("[GameClient] Error while closing connection: " + e.getMessage());
        }
    }
    // TERMINAL TESTING, seems to be working
    public static void main(String[] args) {
        LOGGER.info("[TEST] Starting client for network testing...");

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

        LOGGER.info("[TEST] Packets sent. Client is now listening. Press Enter to disconnect...");

        // Wait for user input to disconnect
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        scanner.close();
        client.disconnect();
        LOGGER.info("[TEST] Client disconnected.");
    }
}
