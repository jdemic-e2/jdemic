
package jdemic.DedicatedServer.network.transport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import jdemic.DedicatedServer.network.core.JdemicNetworkServer;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.security.StateMasker;
import jdemic.DedicatedServer.network.security.DataSanitizer;
import jdemic.DedicatedServer.network.security.RateLimiter;
import jdemic.GameLogic.GameManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class ClientHandler implements Runnable {

    private final SecureSocket secureSocket;
    private final Socket rawSocket;
    private BufferedReader in;
    private PrintWriter out;
    private PacketProcessor packetProcessor;
    private GameManager gameManager;
    private List<ClientHandler> connectedClients;
    private Consumer<Packet> packetReceivedListener;
    private String connectedPlayerName = null;
    private volatile boolean closed;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final DataSanitizer dataSanitizer = new DataSanitizer();
    private final RateLimiter rateLimiter = new RateLimiter();

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(SecureSocket secureSocket, GameManager gameManager, List<ClientHandler> connectedClients) {
        this(secureSocket, gameManager, connectedClients, packet -> {});
    }

    public ClientHandler(
            SecureSocket secureSocket,
            GameManager gameManager,
            List<ClientHandler> connectedClients,
            Consumer<Packet> packetReceivedListener
    ) {
        this.secureSocket = secureSocket;
        this.rawSocket = secureSocket.getRawSocket();
        this.gameManager = gameManager;
        this.connectedClients = connectedClients;
        this.packetReceivedListener = packetReceivedListener;
        packetProcessor = new PacketProcessor(gameManager, this);

        try {
            this.in = new BufferedReader(new InputStreamReader(rawSocket.getInputStream()));
            this.out = new PrintWriter(rawSocket.getOutputStream(), true);
        } catch (Exception e) {
            LOGGER.severe("Eroare la initializarea fluxurilor I/O pentru client.");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (in == null || out == null) {
            LOGGER.severe("[ClientHandler] I/O streams are null. Stopping execution.");
            inchideConexiunea();
            return;
        }

        try {
            String encryptedMessage;
            LOGGER.info("[ClientHandler] Waiting for encrypted messages from client...");

            // Read incoming messages from the client continuously
            while ((encryptedMessage = in.readLine()) != null) {

                if(!rateLimiter.allowPacket()){
                    if(rateLimiter.shouldDisconnect())
                    {
                        LOGGER.severe("[ClientHandler] Client flooding. Disconnecting...");
                        break;
                    }
                    continue;
                }

                // Decrypt and parse the incoming message
                String decryptedMessage = secureSocket.decrypt(encryptedMessage);
                LOGGER.info("[ClientHandler] Received (decrypted): " + decryptedMessage);

                Optional<Packet> sanitized = dataSanitizer.sanitize(decryptedMessage);
                if(sanitized.isPresent()) {
                    Packet packetMessage = sanitized.get();
                    packetReceivedListener.accept(packetMessage);

                    // process packet (PacketProcessor already handles the state broadcast to all clients)
                    packetProcessor.process(packetMessage);
                }         
            }

        } catch (Exception e) {
            LOGGER.info("[ClientHandler] Connection with client interrupted or decryption failed.");
        } finally {
            // Ensure resources are freed when the loop ends or an error occurs
            inchideConexiunea();
        }
    }

    private void inchideConexiunea() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            try {
                packetProcessor.disconnectCurrentPlayer();
            } finally {
                packetProcessor.shutdown();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (rawSocket != null && !rawSocket.isClosed()) {
                rawSocket.close();
            }
            connectedClients.remove(this);
            JdemicNetworkServer.removeClient(this);
            LOGGER.info("[ClientHandler] Resurse eliberate si conexiune inchisa.");
        } catch (Exception e) {
            LOGGER.severe("[ClientHandler] Eroare la închiderea resurselor.");
        }
    }

    public void closeConnection() {
        inchideConexiunea();
    }

    public void setConnectedPlayerName(String playerName) {
        this.connectedPlayerName = playerName;
    }

    public void clearConnectedPlayerName() {
        this.connectedPlayerName = null;
    }

    public String getConnectedPlayerName() {
        return connectedPlayerName;
    }

    public void broadcastGameStateToAll() {
        String gameStateJson;

        synchronized (gameManager.getStateLock()) {
            try {
                gameStateJson = objectMapper.writeValueAsString(gameManager.getState());
            } catch (Exception e) {
                LOGGER.severe("[ClientHandler] Error broadcasting game state: " + e.getMessage());
                return;
            }
        }

        try {
            for (ClientHandler client : connectedClients) {
                if (client.connectedPlayerName != null) {
                    broadcastGameStateToClient(gameStateJson, client);
                }
            }
            LOGGER.info("[ClientHandler] Broadcast game state to " + connectedClients.size() + " clients");
        } catch (Exception e) {
            LOGGER.severe("[ClientHandler] Error broadcasting game state: " + e.getMessage());
        }
    }

    private void broadcastGameStateToClient(String gameStateJson, ClientHandler client) {
        try {
            String maskedResponse = StateMasker.maskStateForPlayer(gameStateJson, client.connectedPlayerName);
            JsonNode stateNode = objectMapper.readTree(maskedResponse);
            Packet statePacket = new Packet(PacketType.GAME_DATA, stateNode);
            String finalEncrypted = client.secureSocket.encrypt(statePacket.toJson());
            client.out.println(finalEncrypted);
        } catch (Exception e) {
            LOGGER.severe("[ClientHandler] Error sending to client: " + e.getMessage());
        }
    }

    public void sendPacketToClient(Packet packet) {
        if (packet == null || out == null) {
            return;
        }

        try {
            String finalEncrypted = secureSocket.encrypt(packet.toJson());
            out.println(finalEncrypted);
        } catch (Exception e) {
            LOGGER.severe("[ClientHandler] Error sending packet to client: " + e.getMessage());
        }
    }
}
