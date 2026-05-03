package jdemic.DedicatedServer.network.core;

import jdemic.DedicatedServer.network.transport.ClientHandler;
import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JdemicNetworkServer {

    private static final int PORT = 9000;
    private static GameManager gameManager;
    private static List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("Pornire Jdemic Network Server...");

        // Start with empty player list - players register when they connect
        List<PlayerState> players = new ArrayList<>();
        gameManager = new GameManager(players);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serverul asculta pe portul " + PORT);

            while (true) {
                try {

                    Socket rawSocket = serverSocket.accept();
                    System.out.println("\n[SERVER] Client conectat: " + rawSocket.getInetAddress().getHostAddress());
                    System.out.println("[SERVER] Initializare handshake RSA/AES...");

                    SecureSocket secureSocket = SecureConnectionManager.wrapSocket(rawSocket);

                    if (secureSocket != null) {
                        System.out.println("[SERVER] Handshake reusit. Delegare catre ClientHandler.");

                        ClientHandler clientHandler = new ClientHandler(secureSocket, gameManager, connectedClients);
                        Thread clientThread = new Thread(clientHandler);
                        clientThread.start();
                        connectedClients.add(clientHandler);
                    } else {
                        System.err.println("[SERVER] Handshake esuat! Respingem clientul.");
                        rawSocket.close();
                    }

                } catch (IOException e) {
                    System.err.println("Eroare la acceptarea conexiunii: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Eroare fatala: Nu s-a putut porni serverul pe portul " + PORT);
            e.printStackTrace();
        }
    }

    public static GameManager getGameManager() {
        return gameManager;
    }

    public static void removeClient(ClientHandler client) {
        connectedClients.remove(client);
    }
}