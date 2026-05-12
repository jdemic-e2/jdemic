package jdemic.DedicatedServer.network.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.net.ServerSocket;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
//Asta e facut cu AI pt ca e doar un  test:))
public class JdemicNetworkServerTest {

    // Această metodă se rulează automat după fiecare test pentru a face curățenie
    @AfterEach
    public void tearDown() {
        JdemicNetworkServer.shutdown();
    }

    @Test
    public void testStartServerFailsGracefullyWhenPortIsInUse() {
        ServerSocket blockerSocket = null;
        try {
            // 1. Ocupăm portul 9000 manual (simulăm un alt server care a rămas deschis)
            blockerSocket = new ServerSocket(9000);

            // 2. Încercăm să pornim serverul Jdemic (acesta ar trebui să eșueze elegant)
            boolean result = JdemicNetworkServer.startServer();

            // 3. Verificăm că a returnat FALSE
            assertFalse(result, "Serverul ar trebui să returneze FALSE dacă portul 9000 este deja ocupat.");

        } catch (IOException e) {
            fail("Testul a eșuat la configurarea portului blocant: " + e.getMessage());
        } finally {
            // 4. Eliberăm portul
            if (blockerSocket != null) {
                try {
                    blockerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 5. Acum că portul este liber, încercăm din nou
        boolean secondResult = JdemicNetworkServer.startServer();

        // 6. Verificăm că de data asta a reușit (TRUE)
        assertTrue(secondResult, "Serverul ar trebui să pornească cu succes după ce portul a fost eliberat.");
    }
}