package jdemic.DedicatedServer.network.security;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that when 70 packets are sent within a single time window,
 * packets 1-60 are allowed and packets 61-70 are dropped.
 */
@DisplayName("RateLimiter Tests")
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // Fresh instance before each test — default: 60 packets/sec, 3 violations threshold
        rateLimiter = new RateLimiter();
    }

    @Test
    @DisplayName("Packets 1-60 should be allowed, packets 61-70 should be blocked (within 1 second)")
    void testRateLimitBlocks_packetsAfter60() {
        int totalPackets = 70;
        int limit = rateLimiter.getMaxPacketsPerSecond(); // 60

        boolean[] results = new boolean[totalPackets];

        // Send all 70 packets as fast as possible (well within 1-second window)
        for (int i = 0; i < totalPackets; i++) {
            results[i] = rateLimiter.allowPacket();
        }

        // --- Assert packets 1 to 60 were ALLOWED ---
        for (int i = 0; i < limit; i++) {
            assertTrue(results[i],
                    "Packet #" + (i + 1) + " should have been ALLOWED but was BLOCKED");
        }

        // --- Assert packets 61 to 70 were BLOCKED ---
        for (int i = limit; i < totalPackets; i++) {
            assertFalse(results[i],
                    "Packet #" + (i + 1) + " should have been BLOCKED but was ALLOWED");
        }
    }

    @Test
    @DisplayName("Exactly 60 packets should all be allowed (boundary check)")
    void testRateLimitAllows_exactlyAtLimit() {
        int limit = rateLimiter.getMaxPacketsPerSecond(); // 60

        for (int i = 0; i < limit; i++) {
            assertTrue(rateLimiter.allowPacket(),
                    "Packet #" + (i + 1) + " should be ALLOWED — still at or under the limit");
        }
    }

    @Test
    @DisplayName("Packet count should reflect all received packets, including blocked ones")
    void testPacketCount_reflectsAllPackets_includingBlocked() {
        int totalPackets = 70;

        for (int i = 0; i < totalPackets; i++) {
            rateLimiter.allowPacket();
        }

        // allowPacket() always increments before checking, so count == 70
        assertEquals(totalPackets, rateLimiter.getPacketCount(),
                "Internal packet counter should equal total packets sent, including blocked ones");
    }

    @Test
    @DisplayName("reset() should clear all state — all 60 packets allowed again after reset")
    void testReset_clearsStateCompletely() {
        // Exhaust the limit
        for (int i = 0; i < 70; i++) {
            rateLimiter.allowPacket();
        }

        // Reset — simulates re-initializing the connection
        rateLimiter.reset();

        assertEquals(0, rateLimiter.getPacketCount(),
                "Packet count should be 0 after reset");
        assertEquals(0, rateLimiter.getConsecutiveViolations(),
                "Consecutive violations should be 0 after reset");

        // Confirm packets flow again after reset
        assertTrue(rateLimiter.allowPacket(),
                "First packet after reset should be ALLOWED");
    }

    @Test
    @DisplayName("Window rollover should record a violation when the previous window exceeded the limit")
    void testWindowRollover_recordsViolationForFloodedWindow() throws Exception {
        RateLimiter limiter = new RateLimiter(3, 2);

        assertTrue(limiter.allowPacket());
        assertTrue(limiter.allowPacket());
        assertTrue(limiter.allowPacket());
        assertFalse(limiter.allowPacket());

        backdateWindow(limiter);

        assertTrue(limiter.allowPacket(), "First packet in the new window should be allowed");
        assertEquals(1, limiter.getConsecutiveViolations(),
                "The flooded previous window should count as one violation");
    }

    @Test
    @DisplayName("A clean window should reset consecutive violations")
    void testCleanWindow_resetsConsecutiveViolations() throws Exception {
        RateLimiter limiter = new RateLimiter(3, 2);

        for (int i = 0; i < 4; i++) {
            limiter.allowPacket();
        }
        backdateWindow(limiter);
        limiter.allowPacket();
        assertEquals(1, limiter.getConsecutiveViolations());

        backdateWindow(limiter);
        limiter.allowPacket();

        assertEquals(0, limiter.getConsecutiveViolations(),
                "A non-flooded window should reset the violation streak");
    }

    @Test
    @DisplayName("Persistent flooding should trigger disconnect after the configured threshold")
    void testShouldDisconnect_afterConsecutiveViolationsReachThreshold() throws Exception {
        RateLimiter limiter = new RateLimiter(3, 2);

        floodCurrentWindow(limiter, 4);
        backdateWindow(limiter);
        limiter.allowPacket();

        floodCurrentWindow(limiter, 3);
        backdateWindow(limiter);
        limiter.allowPacket();

        assertTrue(limiter.shouldDisconnect(),
                "Two consecutive flooded windows should reach the disconnect threshold");
    }

    @Test
    @DisplayName("Custom max packets and violation threshold should be exposed")
    void testCustomLimits_areStored() {
        RateLimiter limiter = new RateLimiter(100, 10);

        assertEquals(100, limiter.getMaxPacketsPerSecond());
        assertEquals(10, limiter.getViolationThreshold());
    }

    private void floodCurrentWindow(RateLimiter limiter, int packetCount) {
        for (int i = 0; i < packetCount; i++) {
            limiter.allowPacket();
        }
    }

    private void backdateWindow(RateLimiter limiter) throws Exception {
        Field windowStartField = RateLimiter.class.getDeclaredField("windowStart");
        windowStartField.setAccessible(true);
        AtomicLong windowStart = (AtomicLong) windowStartField.get(limiter);
        windowStart.set(System.currentTimeMillis() - 1_001);
    }
}
