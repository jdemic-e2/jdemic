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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void handshakeShouldCreateSecureSocketsOnBothSides() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            int port = listener.getLocalPort();

            AtomicReference<SecureSocket> serverSecureSocketRef = new AtomicReference<>();
            AtomicReference<Exception> serverExceptionRef = new AtomicReference<>();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> serverFuture = executor.submit(() -> {
                    try {
                        Socket serverSocket = listener.accept();
                        SecureSocket serverSecureSocket = SecureConnectionManager.wrapSocket(serverSocket);
                        serverSecureSocketRef.set(serverSecureSocket);
                    } catch (Exception e) {
                        serverExceptionRef.set(e);
                    }
                });

                Socket clientSocket = new Socket("localhost", port);
                SecureSocket clientSecureSocket = SecureConnectionManager.wrapClientSocket(clientSocket);

                serverFuture.get(5, TimeUnit.SECONDS);

                assertNull(serverExceptionRef.get());
                assertNotNull(clientSecureSocket);
                assertNotNull(serverSecureSocketRef.get());

                clientSocket.close();
                serverSecureSocketRef.get().getRawSocket().close();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void clientEncryptedMessageShouldBeDecryptedByServer() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            int port = listener.getLocalPort();

            AtomicReference<SecureSocket> serverSecureSocketRef = new AtomicReference<>();
            AtomicReference<Exception> serverExceptionRef = new AtomicReference<>();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> serverFuture = executor.submit(() -> {
                    try {
                        Socket serverSocket = listener.accept();
                        SecureSocket serverSecureSocket = SecureConnectionManager.wrapSocket(serverSocket);
                        serverSecureSocketRef.set(serverSecureSocket);
                    } catch (Exception e) {
                        serverExceptionRef.set(e);
                    }
                });

                Socket clientSocket = new Socket("localhost", port);
                SecureSocket clientSecureSocket = SecureConnectionManager.wrapClientSocket(clientSocket);

                serverFuture.get(5, TimeUnit.SECONDS);

                assertNull(serverExceptionRef.get());
                assertNotNull(clientSecureSocket);
                assertNotNull(serverSecureSocketRef.get());

                String originalMessage = "CARTE_JOC:AS_INIMA_ROSIE:ID_77";
                String encryptedMessage = clientSecureSocket.encrypt(originalMessage);
                String decryptedMessage = serverSecureSocketRef.get().decrypt(encryptedMessage);

                assertNotEquals(originalMessage, encryptedMessage);
                assertEquals(originalMessage, decryptedMessage);

                clientSocket.close();
                serverSecureSocketRef.get().getRawSocket().close();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void serverEncryptedMessageShouldBeDecryptedByClient() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            int port = listener.getLocalPort();

            AtomicReference<SecureSocket> serverSecureSocketRef = new AtomicReference<>();
            AtomicReference<Exception> serverExceptionRef = new AtomicReference<>();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> serverFuture = executor.submit(() -> {
                    try {
                        Socket serverSocket = listener.accept();
                        SecureSocket serverSecureSocket = SecureConnectionManager.wrapSocket(serverSocket);
                        serverSecureSocketRef.set(serverSecureSocket);
                    } catch (Exception e) {
                        serverExceptionRef.set(e);
                    }
                });

                Socket clientSocket = new Socket("localhost", port);
                SecureSocket clientSecureSocket = SecureConnectionManager.wrapClientSocket(clientSocket);

                serverFuture.get(5, TimeUnit.SECONDS);

                assertNull(serverExceptionRef.get());
                assertNotNull(clientSecureSocket);
                assertNotNull(serverSecureSocketRef.get());

                String originalMessage = "SERVER_RESPONSE_OK";
                String encryptedMessage = serverSecureSocketRef.get().encrypt(originalMessage);
                String decryptedMessage = clientSecureSocket.decrypt(encryptedMessage);

                assertNotEquals(originalMessage, encryptedMessage);
                assertEquals(originalMessage, decryptedMessage);

                clientSocket.close();
                serverSecureSocketRef.get().getRawSocket().close();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void tamperedCiphertextShouldFailDecryption() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            int port = listener.getLocalPort();

            AtomicReference<SecureSocket> serverSecureSocketRef = new AtomicReference<>();
            AtomicReference<Exception> serverExceptionRef = new AtomicReference<>();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> serverFuture = executor.submit(() -> {
                    try {
                        Socket serverSocket = listener.accept();
                        SecureSocket serverSecureSocket = SecureConnectionManager.wrapSocket(serverSocket);
                        serverSecureSocketRef.set(serverSecureSocket);
                    } catch (Exception e) {
                        serverExceptionRef.set(e);
                    }
                });

                Socket clientSocket = new Socket("localhost", port);
                SecureSocket clientSecureSocket = SecureConnectionManager.wrapClientSocket(clientSocket);

                serverFuture.get(5, TimeUnit.SECONDS);

                assertNull(serverExceptionRef.get());
                assertNotNull(clientSecureSocket);
                assertNotNull(serverSecureSocketRef.get());

                String encryptedMessage = clientSecureSocket.encrypt("IMPORTANT_GAME_MOVE");

                char lastChar = encryptedMessage.charAt(encryptedMessage.length() - 1);
                char replacement = (lastChar == 'A') ? 'B' : 'A';
                String tamperedMessage = encryptedMessage.substring(0, encryptedMessage.length() - 1) + replacement;

                assertThrows(Exception.class, () -> serverSecureSocketRef.get().decrypt(tamperedMessage));

                clientSocket.close();
                serverSecureSocketRef.get().getRawSocket().close();
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