package jdemic.DedicatedServer;

import jdemic.DedicatedServer.network.security.SecureConnectionManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {

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

            String mesajOriginal = "Hello server! This is a game move!";
            String mesajCriptat = secureSocket.encrypt(mesajOriginal);

            out.println(mesajCriptat);
            System.out.println("[CLIENT] Sent message (crypted): " + mesajCriptat);

            String raspunsCriptat = in.readLine();

            if (raspunsCriptat != null) {
                String raspunsDecriptat = secureSocket.decrypt(raspunsCriptat);
                System.out.println("[CLIENT] Response received (crypted): " + raspunsCriptat);
                System.out.println("[CLIENT] Decrypted response: " + raspunsDecriptat);
            }

        } catch (Exception e) {
            System.err.println("[CLIENT] Critical error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}