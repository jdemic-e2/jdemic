package jdemic.DedicatedServer.network.core;

import jdemic.DedicatedServer.network.transport.ClientHandler;
import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import jdemic.DedicatedServer.network.ui.ServerStatusUi;
import jdemic.GameLogic.GameManager;
import jdemic.DedicatedServer.network.transport.Packet;

import java.io.IOException;
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

public class JdemicNetworkServer {

    private static final AtomicReference<JdemicNetworkServer> ACTIVE_SERVER = new AtomicReference<>();

    private final DedicatedServerConfig config;
    private final GameManager gameManager;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<Socket> clientSockets = ConcurrentHashMap.newKeySet();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private final AtomicReference<Packet> latestPacket = new AtomicReference<>();
    private ServerSocket serverSocket;
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
        System.out.println("Pornire Jdemic Network Server...");
        if (!bindAndStartServices()) {
            return;
        }

        acceptClients();
    }

    public boolean startAsync() {
        System.out.println("Pornire Jdemic Network Server...");
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
            System.out.println("Serverul asculta pe portul " + config.serverPort());
            try {
                statusUi.start();
            } catch (IOException e) {
                System.err.println("[Status] Could not start status UI: " + e.getMessage());
            }
            return true;
        } catch (Exception e) {
            running.set(false);
            System.err.println("Eroare fatala: Nu s-a putut porni serverul pe portul " + config.serverPort());
            System.err.println(e.getMessage());
            closeServerSocket();
            statusUi.stop();
            return false;
        }
    }

    private void acceptClients() {
        try {
            while (running.get()) {
                try {
                    Socket rawSocket = serverSocket.accept();
                    handleClient(rawSocket);
                } catch (SocketException e) {
                    if (running.get()) {
                        System.err.println("Eroare la acceptarea conexiunii: " + e.getMessage());
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Eroare la acceptarea conexiunii: " + e.getMessage());
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

        closeServerSocket();
        closeClientSockets();
        clientExecutor.shutdownNow();
        statusUi.stop();
        System.out.println("[SERVER] Shutdown complete.");
    }

    private void handleClient(Socket rawSocket) throws IOException {
        System.out.println("\n[SERVER] Client conectat: " + rawSocket.getInetAddress().getHostAddress());
        System.out.println("[SERVER] Initializare handshake RSA/AES...");

        SecureSocket secureSocket = SecureConnectionManager.wrapSocket(rawSocket);

        if (secureSocket == null) {
            System.err.println("[SERVER] Handshake esuat! Respingem clientul.");
            rawSocket.close();
            return;
        }

        System.out.println("[SERVER] Handshake reusit. Delegare catre ClientHandler.");
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
            System.err.println("[SERVER] Error while closing server socket: " + e.getMessage());
        }
    }

    private void closeClientSockets() {
        for (Socket clientSocket : clientSockets) {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("[SERVER] Error while closing client socket: " + e.getMessage());
            }
        }
        clientSockets.clear();
    }
}
