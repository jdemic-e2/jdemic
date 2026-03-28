package com.jdemic.network.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;

public class SecureConnectionManager {

    private static final String RSA_ALGO = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;

    private static PrivateKey serverPrivateKey;
    private static PublicKey serverPublicKey;

    // Bloc static: Se executa o singura data cand serverul porneste
    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            serverPrivateKey = pair.getPrivate();
            serverPublicKey = pair.getPublic();
            System.out.println("Cheile RSA au fost generate");
        } catch (Exception e) {
            throw new RuntimeException("Eroare la generarea cheilor RSA", e);
        }
    }

    public static SecureSocket wrapSocket(Socket rawSocket) {
        try {
            DataOutputStream out = new DataOutputStream(rawSocket.getOutputStream());
            DataInputStream in = new DataInputStream(rawSocket.getInputStream());

            byte[] publicKeyBytes = serverPublicKey.getEncoded();
            out.writeInt(publicKeyBytes.length);
            out.write(publicKeyBytes);
            out.flush();

            int encryptedKeyLength = in.readInt();
            byte[] encryptedAesKey = new byte[encryptedKeyLength];
            in.readFully(encryptedAesKey);

            Cipher rsaCipher = Cipher.getInstance(RSA_ALGO);
            rsaCipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            byte[] decryptedAesBytes = rsaCipher.doFinal(encryptedAesKey);
            SecretKey sessionKey = new SecretKeySpec(decryptedAesBytes, "AES");

            System.out.println("Conexiune securizata pentru " + rawSocket.getInetAddress());

            return new SecureSocket(rawSocket, sessionKey);

        } catch (Exception e) {
            System.out.println("Handshake esuat: " + e.getMessage());
            return null;
        }
    }

    public static SecureSocket wrapClientSocket(Socket rawSocket) {
        try {
            DataInputStream in = new DataInputStream(rawSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(rawSocket.getOutputStream());

            int pubKeyLen = in.readInt();
            byte[] pubKeyBytes = new byte[pubKeyLen];
            in.readFully(pubKeyBytes);
            PublicKey serverPubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubKeyBytes));

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey aesKey = keyGen.generateKey();

            Cipher rsaCipher = Cipher.getInstance(RSA_ALGO);
            rsaCipher.init(Cipher.ENCRYPT_MODE, serverPubKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

            out.writeInt(encryptedAesKey.length);
            out.write(encryptedAesKey);
            out.flush();

            return new SecureSocket(rawSocket, aesKey);

        } catch (Exception e) {
            System.err.println("Handshake client esuat: " + e.getMessage());
            return null;
        }
    }

    public static class SecureSocket {
        private final Socket rawSocket;
        private final SecretKey aesKey;

        public SecureSocket(Socket rawSocket, SecretKey aesKey) {
            this.rawSocket = rawSocket;
            this.aesKey = aesKey;
        }

        public String encrypt(String data) throws Exception {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

            byte[] cipherText = cipher.doFinal(data.getBytes());

            byte[] combined = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();

            return Base64.getEncoder().encodeToString(combined);
        }

        public String decrypt(String encryptedBase64) throws Exception {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            ByteBuffer decombined = ByteBuffer.wrap(combined);

            byte[] iv = new byte[12];
            decombined.get(iv);

            byte[] cipherText = new byte[decombined.remaining()];
            decombined.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

            return new String(cipher.doFinal(cipherText));
        }

        public Socket getRawSocket() { return rawSocket; }
    }
}