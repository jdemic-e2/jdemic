package jdemic.DedicatedServer.manual;

import jdemic.DedicatedServer.network.security.SecureConnectionManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Manual client for debugging a running dedicated server on localhost:9000.
 *
 * <p>This is intentionally kept under test sources so it never ships in the
 * production artifact. It is not part of the automated Maven test suite.</p>
 */
public final class DedicatedServerSecureClientManualRunner {

    private DedicatedServerSecureClientManualRunner() {
    }

    public static void main(String[] args) {
        System.out.println("[CLIENT] Connecting to port 9000...");

        try (Socket socket = new Socket("localhost", 9000)) {
            SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapClientSocket(socket);

            if (secureSocket == null) {
                System.out.println("[CLIENT] Failed to connect securely.");
                return;
            }

            System.out.println("[CLIENT] Handshake finalized. Secure channel created.");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String encryptedMessage = secureSocket.encrypt("Hello server! This is a game move!");
            out.println(encryptedMessage);
            System.out.println("[CLIENT] Sent encrypted message: " + encryptedMessage);

            String encryptedResponse = in.readLine();
            if (encryptedResponse != null) {
                System.out.println("[CLIENT] Encrypted response: " + encryptedResponse);
                System.out.println("[CLIENT] Decrypted response: " + secureSocket.decrypt(encryptedResponse));
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Critical error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
