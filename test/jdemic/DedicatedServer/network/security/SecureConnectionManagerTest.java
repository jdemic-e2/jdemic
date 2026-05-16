package jdemic.DedicatedServer.network.security;

import jdemic.DedicatedServer.network.security.SecureConnectionManager.SecureSocket;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecureConnectionManagerTest {

    @Test
    void secureSocketShouldEncryptAndDecryptRoundTrip() throws Exception {
        SecretKey key = generateAesKey();

        try (ConnectedSockets sockets = openConnectedSockets()) {
            SecureSocket secureSocket = new SecureSocket(sockets.clientSide(), key);

            String encrypted = secureSocket.encrypt("protocol-message");

            assertNotEquals("protocol-message", encrypted);
            assertEquals("protocol-message", secureSocket.decrypt(encrypted));
            assertThrows(Exception.class, () -> secureSocket.decrypt("not-base64"));
            assertEquals(sockets.clientSide(), secureSocket.getRawSocket());
        }
    }

    @Test
    void wrapSocketShouldReturnNullWhenClientSendsInvalidEncryptedKey() throws Exception {
        try (ConnectedSockets sockets = openConnectedSockets()) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<SecureSocket> handshake = executor.submit(() -> SecureConnectionManager.wrapSocket(sockets.serverSide()));

                DataInputStream clientInput = new DataInputStream(sockets.clientSide().getInputStream());
                int publicKeyLength = clientInput.readInt();
                byte[] publicKeyBytes = clientInput.readNBytes(publicKeyLength);
                assertEquals(publicKeyLength, publicKeyBytes.length);

                DataOutputStream clientOutput = new DataOutputStream(sockets.clientSide().getOutputStream());
                clientOutput.writeInt(4);
                clientOutput.write(new byte[] {1, 2, 3, 4});
                clientOutput.flush();

                assertNull(handshake.get(5, TimeUnit.SECONDS));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void wrapClientSocketShouldReturnNullWhenServerPublicKeyIsInvalid() throws Exception {
        try (ConnectedSockets sockets = openConnectedSockets()) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<SecureSocket> handshake = executor.submit(() -> SecureConnectionManager.wrapClientSocket(sockets.clientSide()));

                DataOutputStream serverOutput = new DataOutputStream(sockets.serverSide().getOutputStream());
                serverOutput.writeInt(4);
                serverOutput.write(new byte[] {1, 2, 3, 4});
                serverOutput.flush();

                assertNull(handshake.get(5, TimeUnit.SECONDS));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }

    private ConnectedSockets openConnectedSockets() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<Socket> acceptedSocket = executor.submit(listener::accept);
                Socket clientSide = new Socket("localhost", listener.getLocalPort());
                Socket serverSide = acceptedSocket.get(2, TimeUnit.SECONDS);
                return new ConnectedSockets(clientSide, serverSide);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private record ConnectedSockets(Socket clientSide, Socket serverSide) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            clientSide.close();
            serverSide.close();
        }
    }
}
