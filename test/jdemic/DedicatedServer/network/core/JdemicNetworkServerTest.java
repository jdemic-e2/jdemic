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
        try (ServerSocket blockerSocket = new ServerSocket(0)) {
            int blockedPort = blockerSocket.getLocalPort();
            DedicatedServerConfig blockedConfig = new DedicatedServerConfig(blockedPort, false, "localhost", 0, false);

            // 2. Încercăm să pornim serverul Jdemic (acesta ar trebui să eșueze elegant)
            boolean result = JdemicNetworkServer.startServer(blockedConfig);

            // 3. Verificăm că a returnat FALSE
            assertFalse(result, "Serverul ar trebui să returneze FALSE dacă portul este deja ocupat.");

        } catch (IOException e) {
            fail("Testul a eșuat la configurarea portului blocant: " + e.getMessage());
        }

        // 5. Acum că portul este liber, încercăm din nou
        boolean secondResult = JdemicNetworkServer.startServer(
                new DedicatedServerConfig(0, false, "localhost", 0, false)
        );

        // 6. Verificăm că de data asta a reușit (TRUE)
        assertTrue(secondResult, "Serverul ar trebui să pornească cu succes după ce portul a fost eliberat.");
    }
}
