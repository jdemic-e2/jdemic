package jdemic.DedicatedServer.network.core;

import jdemic.DedicatedServer.network.transport.ClientHandler;
import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class JdemicNetworkServer {

    private final DedicatedServerConfig config;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<Socket> clientSockets = ConcurrentHashMap.newKeySet();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private ServerStatusUi statusUi;

    public JdemicNetworkServer() {
        this(DedicatedServerConfig.fromEnvironment());
    }

    public JdemicNetworkServer(DedicatedServerConfig config) {
        this.config = config;
        this.statusUi = new ServerStatusUi(config);
    }

    public static void main(String[] args) {
        JdemicNetworkServer server = new JdemicNetworkServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "jdemic-network-server-shutdown"));
        server.start();
    }

    public void start() {
        System.out.println("Pornire Jdemic Network Server...");
        running.set(true);

        try (ServerSocket socket = new ServerSocket(config.serverPort())) {
            this.serverSocket = socket;
            System.out.println("Serverul asculta pe portul " + config.serverPort());
            try {
                statusUi.start();
            } catch (IOException e) {
                System.err.println("[Status] Could not start status UI: " + e.getMessage());
            }

            while (running.get()) {
                try {
                    Socket rawSocket = socket.accept();
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

        } catch (Exception e) {
            if (running.get()) {
                System.err.println("Eroare fatala: Nu s-a putut porni serverul pe portul " + config.serverPort());
                e.printStackTrace();
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
        clientExecutor.submit(() -> {
            try {
                new ClientHandler(secureSocket).run();
            } finally {
                clientSockets.remove(rawSocket);
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
