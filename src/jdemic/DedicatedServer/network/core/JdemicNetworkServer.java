package jdemic.DedicatedServer.network.core;

import jdemic.DedicatedServer.network.transport.ClientHandler;
import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.ui.ServerStatusUi;
import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.DedicatedServer.network.transport.Packet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class JdemicNetworkServer {

    private static final int PORT = 9000;
    private static GameManager gameManager;
    private static List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private static AtomicReference<Packet> latestPacket = new AtomicReference<>();
    private static volatile boolean running;
    private static ServerSocket serverSocket;
    private static final ScheduledExecutorService emptyServerScheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> emptyServerShutdownTask;

   
    public static void main(String[] args) {
        startServer();
    }
    public static boolean startServer() {
        if (running) {
            System.out.println("[SERVER] Serverul rulează deja!");
            return false; 
        }

        try {
            // Reserving the port,if it is occupied then it just throws the exception
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Pornire Jdemic Network Server... Ascultă pe portul " + PORT);

            // Initializing game logic
            List<PlayerState> players = new ArrayList<>();
            gameManager = new GameManager(players);
            
            // Start the server monitoring interface
            new ServerStatusUi(
                    () -> gameManager,
                    () -> connectedClients.size(),
                    latestPacket::get,
                    JdemicNetworkServer::shutdown
            ).start();

            // Start the connection acceptance thread (so as not to block the Host UI)
            Thread acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket rawSocket = serverSocket.accept();
                        System.out.println("\n[SERVER] Client conectat: " + rawSocket.getInetAddress().getHostAddress());
                        
                        // RSA/AES secure handshake
                        SecureSocket secureSocket = SecureConnectionManager.wrapSocket(rawSocket);

                        if (secureSocket != null) {
                            System.out.println("[SERVER] Handshake reușit. Delegare către ClientHandler.");

                            ClientHandler clientHandler = new ClientHandler(
                                    secureSocket,
                                    gameManager,
                                    connectedClients,
                                    latestPacket::set
                            );
                            Thread clientThread = new Thread(clientHandler);
                            clientThread.start();
                            connectedClients.add(clientHandler);
                            cancelEmptyServerShutdown();
                        } else {
                            System.err.println("[SERVER] Handshake eșuat! Respingem clientul.");
                            rawSocket.close();
                        }
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("Eroare la acceptarea conexiunii: " + e.getMessage());
                        }
                    }
                }
            });
            acceptThread.setDaemon(true); 
            acceptThread.start();

            return true; //The server was started successfully

        } catch (IOException e) {
            System.err.println("Eroare fatală: Portul " + PORT + " este deja utilizat!");
            return false; 
        }
    }

    public static void shutdown() {
        running = false;
        cancelEmptyServerShutdown();
        
        for (ClientHandler client : connectedClients) {
            client.closeConnection();
        }
        connectedClients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("[SERVER] Server închis corect.");
        } catch (IOException e) {
            System.err.println("[SERVER] Eroare la închiderea socket-ului: " + e.getMessage());
        }
    }

    public static GameManager getGameManager() {
        return gameManager;
    }

    public static void removeClient(ClientHandler client) {
        connectedClients.remove(client);
        scheduleEmptyServerShutdownIfNeeded();
    }

    private static void scheduleEmptyServerShutdownIfNeeded() {
        synchronized (JdemicNetworkServer.class) {
            if (!running || !connectedClients.isEmpty()) {
                return;
            }

            cancelEmptyServerShutdown();
            emptyServerShutdownTask = emptyServerScheduler.schedule(() -> {
                synchronized (JdemicNetworkServer.class) {
                    if (running && connectedClients.isEmpty()) {
                        System.out.println("[SERVER] Niciun jucător conectat. Închidere automată.");
                        shutdown();
                    }
                }
            }, 2, TimeUnit.SECONDS);
        }
    }

    private static void cancelEmptyServerShutdown() {
        synchronized (JdemicNetworkServer.class) {
            if (emptyServerShutdownTask != null && !emptyServerShutdownTask.isDone()) {
                emptyServerShutdownTask.cancel(false);
            }
            emptyServerShutdownTask = null;
        }
    }
}