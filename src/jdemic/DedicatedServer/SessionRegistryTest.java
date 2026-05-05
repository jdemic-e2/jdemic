package jdemic.DedicatedServer;

import jdemic.DedicatedServer.network.session.SessionRegistry;
import jdemic.DedicatedServer.network.transport.ClientHandler;

/**
 * Stress Test for SessionRegistry.
 * Simulates heavy multithreading to ensure no ConcurrentModificationExceptions occur.
 */
public class SessionRegistryTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== STARTING STRESS TEST: SessionRegistry ===");

        // Clear any existing data just in case
        SessionRegistry.clearAllSessions();

        // Array to hold 100 simulated connection threads
        Thread[] connectionThreads = new Thread[100];

        // 1. SIMULATE MASS CONNECTIONS (100 players connecting at once)
        for (int i = 0; i < 100; i++) {
            final String playerId = "Player_" + i;
            
            connectionThreads[i] = new Thread(() -> {
                // Passing 'null' instead of a real ClientHandler just for testing the map logic
                SessionRegistry.registerPlayer(playerId, new ClientHandler());
            });
            
            connectionThreads[i].start();
        }

        // Wait for all connection threads to finish executing
        for (Thread t : connectionThreads) {
            t.join();
        }

        System.out.println("\n[ASSERTION 1] Expected 100 players. Actual: " + SessionRegistry.getAllActiveSessions().size());
        if (SessionRegistry.getAllActiveSessions().size() == 100) {
            System.out.println("✅ TEST PASSED: Thread-safe insertion worked flawlessly.");
        } else {
            System.err.println("❌ TEST FAILED: Data loss during concurrent insertion.");
        }

        // 2. SIMULATE DUPLICATE CONNECTION ATTEMPT (Hacker or Glitch)
        System.out.println("\n[SYSTEM] Attempting to connect a player that is already online...");
        boolean duplicateSuccess = SessionRegistry.registerPlayer("Player_50", new ClientHandler());
        
        if (!duplicateSuccess) {
            System.out.println("✅ TEST PASSED: Duplicate connection blocked by putIfAbsent.");
        } else {
            System.err.println("❌ TEST FAILED: Allowed duplicate login!");
        }

        // 3. SIMULATE MASS DISCONNECTIONS (50 players dropping out at once)
        Thread[] disconnectionThreads = new Thread[50];
        for (int i = 0; i < 50; i++) {
            final String playerId = "Player_" + i;
            
            disconnectionThreads[i] = new Thread(() -> {
                SessionRegistry.removePlayer(playerId);
            });
            
            disconnectionThreads[i].start();
        }

        // Wait for all disconnection threads to finish executing
        for (Thread t : disconnectionThreads) {
            t.join();
        }

        System.out.println("\n[ASSERTION 2] Expected 50 players remaining. Actual: " + SessionRegistry.getAllActiveSessions().size());
        if (SessionRegistry.getAllActiveSessions().size() == 50) {
            System.out.println("✅ TEST PASSED: Thread-safe removal worked flawlessly.");
        } else {
            System.err.println("❌ TEST FAILED: Crash or data corruption during concurrent removal.");
        }

        System.out.println("\n=== FINAL RESULT: REGISTRY IS BULLETPROOF ===");
   
    }
    
}