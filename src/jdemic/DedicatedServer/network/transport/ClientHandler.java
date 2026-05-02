package jdemic.DedicatedServer.network.transport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ClientHandler(SecureSocket secureSocket, GameManager gameManager) {
        this.secureSocket = secureSocket;
        this.rawSocket = secureSocket.getRawSocket();
        this.gameManager = gameManager;
        packetProcessor = new PacketProcessor(gameManager);

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
                packetProcessor.process(packetMessage);
                // --- 1. Here is where you get the response from the Backend Game Logic ---
                // Send updated GameState as JSON
                String gameStateJson;
                try {
                    gameStateJson = objectMapper.writeValueAsString(gameManager.getState());
                } catch (JsonProcessingException e) {
                    System.err.println("[ClientHandler] Error serializing GameState: " + e.getMessage());
                    gameStateJson = "{}"; // Fallback
                }

                // --- 2. APPLY ZERO-KNOWLEDGE MASKING BEFORE ENCRYPTION ---
                // (Assuming we somehow know this thread belongs to "player2". 
                // In the future, ClientHandler should store the connected player's ID)
                String targetPlayerId = "player2"; 
                String maskedResponse = StateMasker.maskStateForPlayer(gameStateJson, targetPlayerId);

                // --- 3. Encrypt the cleaned, masked data and send it ---
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
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (rawSocket != null && !rawSocket.isClosed()) {
                rawSocket.close();
            }
            System.out.println("[ClientHandler] Resurse eliberate si conexiune inchisa.");
        } catch (Exception e) {
            System.err.println("[ClientHandler] Eroare la închiderea resurselor.");
        }
    }
}