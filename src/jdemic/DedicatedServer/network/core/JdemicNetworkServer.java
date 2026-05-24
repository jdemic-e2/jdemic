package jdemic.DedicatedServer.network.core;

import jdemic.DedicatedServer.network.transport.ClientHandler;
import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.ui.ServerStatusUi;
import jdemic.GameLogic.GameManager;
import jdemic.DedicatedServer.network.transport.Packet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class JdemicNetworkServer {

    private static final AtomicReference<JdemicNetworkServer> ACTIVE_SERVER = new AtomicReference<>();
    private static final Logger LOGGER = Logger.getLogger(JdemicNetworkServer.class.getName());

    private static final int STATUS_PORT_OFFSET = 500;

    private final DedicatedServerConfig config;
    private final GameManager gameManager;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<Socket> clientSockets = ConcurrentHashMap.newKeySet();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private final AtomicReference<Packet> latestPacket = new AtomicReference<>();
    private ServerSocket serverSocket;
    private ServerSocket statusSocket;
    private ServerStatusUi statusUi;

    public JdemicNetworkServer() {
        this(DedicatedServerConfig.fromEnvironment());
    }

    public JdemicNetworkServer(DedicatedServerConfig config) {
        this.config = config;
        this.gameManager = new GameManager(new ArrayList<>(), false);
        this.statusUi = new ServerStatusUi(
                config,
                () -> gameManager,
                connectedClients::size,
                latestPacket::get,
                this::stop
        );
    }

    public static void main(String[] args) {
        JdemicNetworkServer server = new JdemicNetworkServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "jdemic-network-server-shutdown"));
        server.start();
    }

    public static boolean startServer() {
        return startServer(DedicatedServerConfig.fromEnvironment());
    }

    public static boolean startServer(DedicatedServerConfig config) {
        JdemicNetworkServer server = new JdemicNetworkServer(config);
        if (!server.startAsync()) {
            return false;
        }

        JdemicNetworkServer previous = ACTIVE_SERVER.getAndSet(server);
        if (previous != null && previous != server) {
            previous.stop();
        }
        return true;
    }

    public static void shutdown() {
        JdemicNetworkServer server = ACTIVE_SERVER.getAndSet(null);
        if (server != null) {
            server.stop();
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        JdemicNetworkServer server = ACTIVE_SERVER.get();
        if (server != null) {
            server.connectedClients.remove(clientHandler);
        }
    }

    public void start() {
        LOGGER.info("Starting Jdemic Network Server...");
        if (!bindAndStartServices()) {
            return;
        }

        acceptClients();
    }

    public boolean startAsync() {
        LOGGER.info("Starting Jdemic Network Server...");
        if (!bindAndStartServices()) {
            return false;
        }

        Thread serverThread = new Thread(this::acceptClients, "jdemic-network-server");
        serverThread.setDaemon(true);
        serverThread.start();
        return true;
    }

    private boolean bindAndStartServices() {
        if (!running.compareAndSet(false, true)) {
            return true;
        }

        try {
            serverSocket = new ServerSocket(config.serverPort());
            LOGGER.info("The server is listening on the port: " + config.serverPort());
            statusSocket = new ServerSocket(config.serverPort() + STATUS_PORT_OFFSET);
            startStatusListener();
            startStatusUi();
            startIdleTimeoutChecker();
            return true;
        } catch (Exception e) {
            running.set(false);
            LOGGER.severe("Fatal error: The server with the port: \"" + config.serverPort()+"\" could not be started!");
            LOGGER.severe(e.getMessage());
            closeServerSocket();
            statusUi.stop();
            return false;
        }
    }

    private void startStatusListener() {
        Thread t = new Thread(() -> {
            while (running.get()) {
                try {
                    Socket client = statusSocket.accept();
                    client.setSoTimeout(500);
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                         PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
                        String line = in.readLine();
                        if ("STATUS".equals(line)) {
                            boolean started = gameManager.getState().isGameStarted();
                            int players = gameManager.getState().getPlayers().size();
                            int maxPlayers = jdemic.GameLogic.GameManager.MAX_PLAYERS;
                            out.println("gameStarted:" + started + ",players:" + players + ",maxPlayers:" + maxPlayers);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        client.close();
                    }
                } catch (SocketException e) {
                    if (running.get()) LOGGER.severe("[STATUS] Accept error: " + e.getMessage());
                } catch (IOException e) {
                    if (running.get()) LOGGER.severe("[STATUS] IO error: " + e.getMessage());
                }
            }
        }, "jdemic-status-listener");
        t.setDaemon(true);
        t.start();
    }

    private void startStatusUi() {
        try {
            statusUi.start();
        } catch (IOException e) {
            LOGGER.severe("[Status] Could not start status UI: " + e.getMessage());
        }
    }
    //3rd change-> implemented a method that checks if the server has been idle for more than 60 seconds
    private void startIdleTimeoutChecker() {
        Thread timeoutThread = new Thread(() -> {
            long emptySince = System.currentTimeMillis();

            while (running.get()) {
                try {
                    Thread.sleep(5000); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (!connectedClients.isEmpty()) {
                    emptySince = System.currentTimeMillis();
                } else {
                    long idleDuration = System.currentTimeMillis() - emptySince;
                    
                    if (idleDuration >= 60000) { 
                        LOGGER.info("[SERVER] 1 minute inactivity detected (0 players).This server will close.");
                        stop(); 
                        System.exit(0); 
                        break;
                    }
                }
            }
        }, "jdemic-idle-timeout");
        
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    private void acceptClients() {
        try {
            while (running.get()) {
                try {
                    Socket rawSocket = serverSocket.accept();
                    handleClient(rawSocket);
                } catch (SocketException e) {
                    if (running.get()) {
                        LOGGER.severe("Error accepting connection: " + e.getMessage());
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        LOGGER.severe("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        LOGGER.info("[SERVER] Stopping server...");
        closeServerSocket();
        closeStatusSocket();
        closeClientSockets();
        clientExecutor.shutdownNow();
        statusUi.stop();
        LOGGER.info("[SERVER] Shutdown complete.");
    }

    private void handleClient(Socket rawSocket) throws IOException {
        LOGGER.info("\n[SERVER] Client Connected: " + rawSocket.getInetAddress().getHostAddress());
        LOGGER.info("[SERVER] Initializing handshake RSA/AES...");

        SecureSocket secureSocket = SecureConnectionManager.wrapSocket(rawSocket);

        if (secureSocket == null) {
            LOGGER.severe("[SERVER] Failed handshake! Rejecting the client.");
            rawSocket.close();
            return;
        }

        LOGGER.info("[SERVER] Successful Handshake. Delegating to ClientHandler.");
        clientSockets.add(rawSocket);
        ClientHandler clientHandler = new ClientHandler(secureSocket, gameManager, connectedClients, latestPacket::set);
        connectedClients.add(clientHandler);
        clientExecutor.submit(() -> {
            try {
                clientHandler.run();
            } finally {
                clientSockets.remove(rawSocket);
                connectedClients.remove(clientHandler);
            }
        });
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.severe("[SERVER] Error while closing server socket: " + e.getMessage());
        }
    }

    private void closeStatusSocket() {
        try {
            if (statusSocket != null && !statusSocket.isClosed()) {
                statusSocket.close();
            }
        } catch (IOException e) {
            LOGGER.severe("[SERVER] Error while closing status socket: " + e.getMessage());
        }
    }

    private void closeClientSockets() {
        for (Socket clientSocket : clientSockets) {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                LOGGER.severe("[SERVER] Error while closing client socket: " + e.getMessage());
            }
        }
        clientSockets.clear();
    }
}
