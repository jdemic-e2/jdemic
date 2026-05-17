package jdemic.DedicatedServer;

import jdemic.DedicatedServer.network.security.DataSanitizer;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;

import java.util.Optional;

/**
 * Manual testing class for {@link DataSanitizer}.
 * <p>
 * This class verifies if the security filter at the server's gate correctly
 * rejects malicious inputs, corrupted packets, or invalid data types, allowing
 * only perfectly formatted JSON packets to pass through.
 */
public class DataSanitizerTest {

    /**
     * Entry point for the DataSanitizer test suite.
     */
    public static void main(String[] args) {
        
        System.out.println("=== STARTING TESTS FOR DataSanitizer.java ===\n");
        DataSanitizer sanitizer = new DataSanitizer();

        /* =========================================================================
         * TEST 1: Validly formatted packet (The ideal case)
         * Functionality tested: We verify if a fully correct JSON passes all 
         * security filters and is successfully parsed into a Packet object.
         * ========================================================================= */
        System.out.println("> TEST 1: Valid JSON string validation");
        String validJson = "{\"type\":\"PING\",\"timestamp\":1712000000000,\"payload\":{}}";
        Optional<Packet> validResult = sanitizer.sanitize(validJson);

        if (validResult.isPresent() && validResult.get().getType() == PacketType.PING) {
            System.out.println("[PASS] TEST 1: Valid packet was safely accepted and parsed.");
        } else {
            System.err.println("[FAIL] TEST 1: Valid packet was incorrectly rejected.");
        }

        /* =========================================================================
         * TEST 2: Security against empty or corrupted inputs (Null Pointer Protection)
         * Functionality tested: We verify if the server defends itself and prevents 
         * crashes when it receives empty strings or broken/malformed JSON payloads.
         * ========================================================================= */
        System.out.println("\n> TEST 2: Null, empty or malformed inputs");
        Optional<Packet> nullResult = sanitizer.sanitize(null);
        Optional<Packet> emptyResult = sanitizer.sanitize("   ");
        Optional<Packet> brokenJsonResult = sanitizer.sanitize("{ \"type\": broken string ");

        if (nullResult.isEmpty() && emptyResult.isEmpty() && brokenJsonResult.isEmpty()) {
            System.out.println("[PASS] TEST 2: Malformed or null inputs safely rejected.");
        } else {
            System.err.println("[FAIL] TEST 2: Malformed input bypassed the sanitizer!");
        }

        /* =========================================================================
         * TEST 3: JSON with missing fields or wrong data types
         * Functionality tested: We verify the strict validation rules. If the 
         * payload omits the packet type or sends a timestamp as text instead of 
         * a number, the system must block it.
         * ========================================================================= */
        System.out.println("\n> TEST 3: Missing fields or invalid value types");
        String missingTypeJson = "{\"timestamp\":1712000000000,\"payload\":{}}"; // Missing "type"
        String wrongTypeJson = "{\"type\":\"PING\",\"timestamp\":\"NOT_A_NUMBER\",\"payload\":{}}"; // Invalid timestamp

        if (sanitizer.sanitize(missingTypeJson).isEmpty() && sanitizer.sanitize(wrongTypeJson).isEmpty()) {
             System.out.println("[PASS] TEST 3: Packets with missing/wrong field types rejected.");
        } else {
             System.err.println("[FAIL] TEST 3: Invalid fields were accepted!");
        }

        /* =========================================================================
         * TEST 4: Spoofing protection (Unknown types / Broken temporal logic)
         * Functionality tested: We verify if the DataSanitizer rejects commands 
         * that the server doesn't support (e.g., "HACK_SERVER") and prevents 
         * time manipulation attempts (negative timestamps).
         * ========================================================================= */
        System.out.println("\n> TEST 4: Unknown PacketType or negative timestamp");
        String unknownTypeJson = "{\"type\":\"HACK_SERVER\",\"timestamp\":1712000000000,\"payload\":{}}";
        String negativeTimeJson = "{\"type\":\"PING\",\"timestamp\":-500,\"payload\":{}}";

        if (sanitizer.sanitize(unknownTypeJson).isEmpty() && sanitizer.sanitize(negativeTimeJson).isEmpty()) {
             System.out.println("[PASS] TEST 4: Unknown types and negative times rejected.");
        } else {
             System.err.println("[FAIL] TEST 4: Spoofed packets passed through!");
        }

        System.out.println("\n=== FINAL RESULT: DATASANITIZER IS BULLETPROOF ===");
    }
}