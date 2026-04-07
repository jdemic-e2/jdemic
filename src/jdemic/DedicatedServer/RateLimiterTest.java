package jdemic.DedicatedServer;

import jdemic.DedicatedServer.network.security.RateLimiter;

/**
 * RateLimiterTest - Tests for the Anti-Flood Shield
 *
 * This test file verifies the core functionality of the RateLimiter module.
 * Each test method validates a specific behavior described in the comments.
 * Run this class directly (it has a main method) to execute all tests.
 */
public class RateLimiterTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== RateLimiter Test Suite ===\n");

        testAllowPacketUnderLimit();
        testDropPacketOverLimit();
        testWindowResetsAfterOneSecond();
        testConsecutiveViolationsIncrement();
        testViolationsResetOnGoodBehavior();
        testShouldDisconnectAfterThreshold();
        testResetClearsAllState();
        testCustomLimits();

        System.out.println("\n=== Results: " + testsPassed + " passed, "
                + testsFailed + " failed out of "
                + (testsPassed + testsFailed) + " tests ===");
    }

    /**
     * TEST 1: Packets under the limit should all be allowed.
     * Sends exactly maxPacketsPerSecond packets and verifies
     * that every single one is accepted by the RateLimiter.
     */
    private static void testAllowPacketUnderLimit() {
        RateLimiter limiter = new RateLimiter(10, 3);
        boolean allAllowed = true;

        // Send exactly 10 packets (the limit is 10/sec)
        for (int i = 0; i < 10; i++) {
            if (!limiter.allowPacket()) {
                allAllowed = false;
                break;
            }
        }

        assertTest("Packets under limit are all allowed", allAllowed);
    }

    /**
     * TEST 2: Packets over the limit should be dropped.
     * Sends more packets than the allowed limit within one window
     * and verifies the extra ones are rejected (return false).
     */
    private static void testDropPacketOverLimit() {
        RateLimiter limiter = new RateLimiter(5, 3);

        // Send 5 packets (all should pass)
        for (int i = 0; i < 5; i++) {
            limiter.allowPacket();
        }

        // The 6th packet should be dropped
        boolean sixthPacketDropped = !limiter.allowPacket();

        assertTest("6th packet is dropped when limit is 5", sixthPacketDropped);
    }

    /**
     * TEST 3: The sliding window resets after 1 second.
     * After sleeping for over 1 second, the counter should reset
     * and new packets should be accepted again.
     */
    private static void testWindowResetsAfterOneSecond() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(5, 3);

        // Fill up the entire window
        for (int i = 0; i < 5; i++) {
            limiter.allowPacket();
        }

        // Wait for the window to expire
        Thread.sleep(1100);

        // The next packet should be allowed (new window)
        boolean allowedAfterReset = limiter.allowPacket();

        assertTest("Packets allowed again after 1-second window reset", allowedAfterReset);
    }

    /**
     * TEST 4: Consecutive violations increment correctly.
     * When a client exceeds the limit across multiple windows,
     * the violation counter should go up each time.
     */
    private static void testConsecutiveViolationsIncrement() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(3, 5);

        // First window: send 5 packets (limit is 3, so 2 are over)
        for (int i = 0; i < 5; i++) {
            limiter.allowPacket();
        }

        // Wait for window to expire, then trigger a new packet
        // which will cause the old window to be evaluated
        Thread.sleep(1100);
        limiter.allowPacket();

        // The violation from the first window should be recorded
        int violations = limiter.getConsecutiveViolations();

        assertTest("Consecutive violation count incremented to 1", violations == 1);
    }

    /**
     * TEST 5: Violations reset when the client behaves well.
     * If a window passes without exceeding the limit, the
     * consecutive violation counter should reset back to 0.
     */
    private static void testViolationsResetOnGoodBehavior() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(5, 3);

        // Window 1: exceed the limit (send 8 packets)
        for (int i = 0; i < 8; i++) {
            limiter.allowPacket();
        }

        // Wait for window to expire
        Thread.sleep(1100);

        // Window 2: stay under the limit (send only 3 packets)
        for (int i = 0; i < 3; i++) {
            limiter.allowPacket();
        }

        // Wait again and trigger evaluation of window 2
        Thread.sleep(1100);
        limiter.allowPacket();

        // Violations should be 0 because the last window was clean
        int violations = limiter.getConsecutiveViolations();

        assertTest("Violations reset to 0 after a clean window", violations == 0);
    }

    /**
     * TEST 6: shouldDisconnect returns true after reaching the threshold.
     * Simulates sustained flooding across multiple windows and checks
     * that the forced disconnect signal is triggered.
     */
    private static void testShouldDisconnectAfterThreshold() throws InterruptedException {
        // Threshold set to 2 for faster testing
        RateLimiter limiter = new RateLimiter(3, 2);

        // Two consecutive windows of flooding
        for (int window = 0; window < 2; window++) {
            for (int i = 0; i < 6; i++) {
                limiter.allowPacket();
            }
            Thread.sleep(1100);
            // Trigger evaluation of previous window
            limiter.allowPacket();
        }

        // After 2 consecutive violations, shouldDisconnect should be true
        boolean shouldDisconnect = limiter.shouldDisconnect();

        assertTest("shouldDisconnect() returns true after threshold reached", shouldDisconnect);
    }

    /**
     * TEST 7: reset() clears all internal state.
     * After calling reset, packetCount and violations should be 0.
     */
    private static void testResetClearsAllState() {
        RateLimiter limiter = new RateLimiter(5, 3);

        // Pollute the state
        for (int i = 0; i < 10; i++) {
            limiter.allowPacket();
        }

        limiter.reset();

        boolean countCleared = limiter.getPacketCount() == 0;
        boolean violationsCleared = limiter.getConsecutiveViolations() == 0;

        assertTest("reset() clears packet count and violations",
                countCleared && violationsCleared);
    }

    /**
     * TEST 8: Custom limits are respected.
     * Creates a RateLimiter with non-default values and verifies
     * that those values are correctly stored and used.
     */
    private static void testCustomLimits() {
        RateLimiter limiter = new RateLimiter(100, 10);

        boolean correctMax = limiter.getMaxPacketsPerSecond() == 100;
        boolean correctThreshold = limiter.getViolationThreshold() == 10;

        assertTest("Custom max (100) and threshold (10) are stored correctly",
                correctMax && correctThreshold);
    }

    // --- Utility method for assertion ---

    private static void assertTest(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            testsPassed++;
        } else {
            System.out.println("  [FAIL] " + testName);
            testsFailed++;
        }
    }
}
