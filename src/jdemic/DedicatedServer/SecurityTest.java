package jdemic.DedicatedServer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import jdemic.DedicatedServer.network.security.SecureConnectionManager;

public class SecurityTest {
    private static final int PORT = 8888;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- DEMO COMPLET SECURITATE (Pachete AES-GCM cu IV lipit) ---");

       
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                Socket rawSocket = serverSocket.accept();

                
                SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapSocket(rawSocket);

                if (secureSocket != null) {
                    DataInputStream in = new DataInputStream(secureSocket.getRawSocket().getInputStream());

                  
                    String encryptedMsg = in.readUTF();

                
                    String decrypted = secureSocket.decrypt(encryptedMsg);
                    System.out.println("[SERVER] Mesaj primit, descifrat si validat (Hash OK): " + decrypted);
                }
            } catch (Exception e) {
                System.err.println("[SERVER] Eroare: " + e.getMessage());
            }
        });

     
        Thread clientThread = new Thread(() -> {
            try {
                Thread.sleep(1000); 
                Socket socket = new Socket("127.0.0.1", PORT);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                SecureConnectionManager.SecureSocket secureSocket = SecureConnectionManager.wrapClientSocket(socket);

                String rawMove = "CARTE_JOC:AS_INIMA_ROSIE:ID_77";

               
                String encryptedMove = secureSocket.encrypt(rawMove);

                
                out.writeUTF(encryptedMove);
                out.flush();

                System.out.println("[CLIENT] Pachet expediat (Base64): " + encryptedMove);

            } catch (Exception e) {
                System.err.println("[CLIENT] Eroare: " + e.getMessage());
            }
        });

        serverThread.start();
        clientThread.start();
        serverThread.join();
        clientThread.join();
        System.out.println("--- TEST FINALIZAT CU SUCCES ---");
    }
}
