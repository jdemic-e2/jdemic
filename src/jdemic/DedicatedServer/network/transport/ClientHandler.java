package jdemic.DedicatedServer.network.transport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

import jdemic.DedicatedServer.network.core.JdemicNetworkServer;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.security.StateMasker;
import jdemic.GameLogic.GameManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

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
            System.err.println("Eroare la initializarea fluxurilor I/O pentru client.");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (in == null || out == null) {
            System.err.println("[ClientHandler] Fluxurile I/O sunt nule. Opresc procesarea.");
            inchideConexiunea();
            return;
        }

        try {
            String encryptedMessage;
            System.out.println("[ClientHandler] Astept mesaje criptate de la client...");

            while ((encryptedMessage = in.readLine()) != null) {
                String decryptedMessage = secureSocket.decrypt(encryptedMessage);
                System.out.println("[ClientHandler] Am primit (decriptat): " + decryptedMessage);
                Packet packetMessage = Packet.fromJson(decryptedMessage);
                packetReceivedListener.accept(packetMessage);
                packetProcessor.process(packetMessage);

                // Send updated GameState as JSON
                String gameStateJson;
                try {
                    gameStateJson = objectMapper.writeValueAsString(gameManager.getState());
                } catch (JsonProcessingException e) {
                    System.err.println("[ClientHandler] Error serializing GameState: " + e.getMessage());
                    gameStateJson = "{}"; // Fallback
                }

                // Apply masking and send to this client
                String targetPlayerId = connectedPlayerName != null ? connectedPlayerName : "UNKNOWN";
                String maskedResponse = StateMasker.maskStateForPlayer(gameStateJson, targetPlayerId);
                String encryptedResponse = secureSocket.encrypt(maskedResponse);
                out.println(encryptedResponse);
            }

        } catch (Exception e) {
            System.out.println("[ClientHandler] Conexiune întrerupta cu clientul sau eroare de decriptare.");
        } finally {
            inchideConexiunea();
        }
    }

    private void inchideConexiunea() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            packetProcessor.disconnectCurrentPlayer();
            if (in != null) in.close();
            if (out != null) out.close();
            if (rawSocket != null && !rawSocket.isClosed()) {
                rawSocket.close();
            }
            connectedClients.remove(this);
            JdemicNetworkServer.removeClient(this);
            System.out.println("[ClientHandler] Resurse eliberate si conexiune inchisa.");
        } catch (Exception e) {
            System.err.println("[ClientHandler] Eroare la închiderea resurselor.");
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
        try {
            String gameStateJson = objectMapper.writeValueAsString(gameManager.getState());
            // Send to all connected clients
            for (ClientHandler client : connectedClients) {
                if (client.connectedPlayerName != null) {
                    try {
                        String maskedResponse = StateMasker.maskStateForPlayer(gameStateJson, client.connectedPlayerName);
                        String encryptedResponse = client.secureSocket.encrypt(maskedResponse);
                        client.out.println(encryptedResponse);
                    } catch (Exception e) {
                        System.err.println("[ClientHandler] Error sending to client: " + e.getMessage());
                    }
                }
            }
            System.out.println("[ClientHandler] Broadcast game state to " + connectedClients.size() + " clients");
        } catch (Exception e) {
            System.err.println("[ClientHandler] Error broadcasting game state: " + e.getMessage());
        }
    }
}
