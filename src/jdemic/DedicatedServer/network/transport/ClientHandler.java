package jdemic.DedicatedServer.network.transport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.session.SessionRegistry;

public class ClientHandler implements Runnable {

    
    private final SecureSocket secureSocket;
    private final Socket rawSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String playerId;

    public ClientHandler(SecureSocket secureSocket) {
        this.secureSocket = secureSocket;
        
        this.rawSocket = secureSocket.getRawSocket();

        try {
            this.in = new BufferedReader(new InputStreamReader(rawSocket.getInputStream()));
            this.out = new PrintWriter(rawSocket.getOutputStream(), true);
            // NEW: Generate a temporary ID for Sprint 1
            this.playerId = "Player_" + UUID.randomUUID().toString().substring(0, 5);

        } catch (Exception e) {
            System.err.println("Eroare la initializarea fluxurilor I/O pentru client.");
            e.printStackTrace();
        }
    }

    // NEW: Constructor fake folosit STRICT pentru Unit Testing
    public ClientHandler() {
        this.secureSocket = null;
        this.rawSocket = null;
        this.playerId = "TestPlayer";
    }

    // NEW: A getter method so other classes can ask this thread who it belongs to
    public String getPlayerId() {
        return playerId;
    }

    @Override
    public void run() {
        if (in == null || out == null) {
            System.err.println("[ClientHandler] Fluxurile I/O sunt nule. Opresc procesarea.");
            inchideConexiunea();
            return;
        }

        // NEW: Tell the server that this player is officially online
        SessionRegistry.registerPlayer(this.playerId, this);

        try {
            String mesajCriptat;
            System.out.println("[ClientHandler] Astept mesaje criptate de la client...");

            
            while ((mesajCriptat = in.readLine()) != null) {

               
                String mesajDecriptat = secureSocket.decrypt(mesajCriptat);
                System.out.println("[ClientHandler] Am primit (decriptat): " + mesajDecriptat);

                
                String raspunsServer = mesajDecriptat;

               
                String raspunsCriptat = secureSocket.encrypt(raspunsServer);
                out.println(raspunsCriptat);
            }

        } catch (Exception e) {
            System.out.println("[ClientHandler] Conexiune întrerupta cu clientul sau eroare de decriptare.");
        } finally {
            inchideConexiunea();
        }
    }

    private void inchideConexiunea() {
        
        // NEW: Unregister the player immediately when the connection drops
        SessionRegistry.removePlayer(this.playerId);

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (rawSocket != null && !rawSocket.isClosed()) {
                rawSocket.close();
            }
            System.out.println("[ClientHandler] Resurse eliberate și conexiune închisă.");
        } catch (Exception e) {
            System.err.println("[ClientHandler] Eroare la închiderea resurselor.");
        }
    }
}