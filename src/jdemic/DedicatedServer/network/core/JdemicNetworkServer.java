package jdemic.DedicatedServer.network.core;

import jdemic.DedicatedServer.network.transport.ClientHandler;
import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class JdemicNetworkServer {

    private static final int PORT = 9000;

    public static void main(String[] args) {
        System.out.println("Pornire Jdemic Network Server...");

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

                        ClientHandler clientHandler = new ClientHandler(secureSocket);
                        Thread clientThread = new Thread(clientHandler);
                        clientThread.start();
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
}