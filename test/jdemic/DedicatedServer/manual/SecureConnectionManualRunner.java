package jdemic.DedicatedServer.manual;

import jdemic.DedicatedServer.network.security.SecureConnectionManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Manual smoke runner for the secure socket handshake.
 *
 * <p>This is intentionally kept under test sources so it never ships in the
 * production artifact. Run directly from an IDE when debugging the handshake.</p>
 */
public final class SecureConnectionManualRunner {

    private static final int PORT = 8888;

    private SecureConnectionManualRunner() {
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Secure connection manual runner ---");

        Thread serverThread = new Thread(SecureConnectionManualRunner::runServer, "secure-runner-server");
        Thread clientThread = new Thread(SecureConnectionManualRunner::runClient, "secure-runner-client");

        serverThread.start();
        clientThread.start();
        serverThread.join();
        clientThread.join();

        System.out.println("--- Manual runner finished ---");
    }

    private static void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT);
             Socket rawSocket = serverSocket.accept()) {
            SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapSocket(rawSocket);

            if (secureSocket == null) {
                System.err.println("[SERVER] Secure handshake failed.");
                return;
            }

            DataInputStream in = new DataInputStream(secureSocket.getRawSocket().getInputStream());
            String encryptedMessage = in.readUTF();
            String decryptedMessage = secureSocket.decrypt(encryptedMessage);
            System.out.println("[SERVER] Decrypted message: " + decryptedMessage);
        } catch (Exception e) {
            System.err.println("[SERVER] Error: " + e.getMessage());
        }
    }

    private static void runClient() {
        try {
            Thread.sleep(1_000);

            try (Socket socket = new Socket("127.0.0.1", PORT)) {
                SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapClientSocket(socket);

                if (secureSocket == null) {
                    System.err.println("[CLIENT] Secure handshake failed.");
                    return;
                }

                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                String encryptedMessage = secureSocket.encrypt("CARTE_JOC:AS_INIMA_ROSIE:ID_77");
                out.writeUTF(encryptedMessage);
                out.flush();
                System.out.println("[CLIENT] Sent encrypted message.");
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Error: " + e.getMessage());
        }
    }
}
