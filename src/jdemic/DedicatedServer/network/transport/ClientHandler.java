package com.jdemic.network.transport;

import com.jdemic.network.security.SecureConnectionManager.SecureSocket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    
    private final SecureSocket secureSocket;
    private final Socket rawSocket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(SecureSocket secureSocket) {
        this.secureSocket = secureSocket;
        
        this.rawSocket = secureSocket.getRawSocket();

        try {
            this.in = new BufferedReader(new InputStreamReader(rawSocket.getInputStream()));
            this.out = new PrintWriter(rawSocket.getOutputStream(), true);
        } catch (Exception e) {
            System.err.println("Eroare la initializarea fluxurilor I/O pentru client.");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (in == null || out == null) {
            System.err.println("[ClientHandler] Fluxurile I/O sunt nule. Opresc procesarea.");
            inchideConexiunea();
            return;
        }

        try {
            String mesajCriptat;
            System.out.println("[ClientHandler] Astept mesaje criptate de la client...");

            
            while ((mesajCriptat = in.readLine()) != null) {

               
                String mesajDecriptat = secureSocket.decrypt(mesajCriptat);
                System.out.println("[ClientHandler] Am primit (decriptat): " + mesajDecriptat);

                
                String raspunsServer = mesajDecriptat;

               
                String raspunsCriptat = secureSocket.encrypt(raspunsServer);
                out.println(raspunsCriptat);
            }

        } catch (Exception e) {
            System.out.println("[ClientHandler] Conexiune întrerupta cu clientul sau eroare de decriptare.");
        } finally {
            inchideConexiunea();
        }
    }

    private void inchideConexiunea() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (rawSocket != null && !rawSocket.isClosed()) {
                rawSocket.close();
            }
            System.out.println("[ClientHandler] Resurse eliberate și conexiune închisă.");
        } catch (Exception e) {
            System.err.println("[ClientHandler] Eroare la închiderea resurselor.");
        }
    }
}