package com.jdemic;

import com.jdemic.network.security.SecureConnectionManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) {
        System.out.println("[CLIENT] Conectare la portul 9000...");

        try (Socket socket = new Socket("localhost", 9000)) {

            SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapClientSocket(socket);

            if (secureSocket == null) {
                System.out.println("[CLIENT] Eroare la stabilirea conexiunii securizate.");
                return;
            }
            System.out.println("[CLIENT] Handshake finalizat. Canal securizat stabilit.");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String mesajOriginal = "Salut Server! Aceasta este o mutare la joc.";
            String mesajCriptat = secureSocket.encrypt(mesajOriginal);

            out.println(mesajCriptat);
            System.out.println("[CLIENT] Mesaj trimis (criptat): " + mesajCriptat);

            String raspunsCriptat = in.readLine();

            if (raspunsCriptat != null) {
                String raspunsDecriptat = secureSocket.decrypt(raspunsCriptat);
                System.out.println("[CLIENT] Raspuns primit (criptat): " + raspunsCriptat);
                System.out.println("[CLIENT] Raspuns descifrat: " + raspunsDecriptat);
            }

        } catch (Exception e) {
            System.err.println("[CLIENT] Eroare critica: " + e.getMessage());
            e.printStackTrace();
        }
    }
}