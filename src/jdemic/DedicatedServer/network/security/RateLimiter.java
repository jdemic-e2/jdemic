package jdemic.DedicatedServer.network.security;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RateLimiter - Anti-Flood Shield
 *
 * Protects the server's hardware resources by limiting the number of
 * packets a single client can send within a one-second sliding window.
 *
 * Each ClientHandler has its own RateLimiter instance attached.
 * If the incoming packet rate exceeds the hard limit (default: 60 packets/second),
 * subsequent packets are silently dropped before consuming CPU for parsing.
 * If the flood persists beyond a configurable violation threshold,
 * the module signals for a forced disconnection.
 */
public class RateLimiter {

    // Maximum number of packets allowed per second (hard limit)
    private static final int DEFAULT_MAX_PACKETS_PER_SECOND = 60;

    // Number of consecutive violations before triggering a forced disconnect
    private static final int DEFAULT_VIOLATION_THRESHOLD = 3;

    private final int maxPacketsPerSecond;
    private final int violationThreshold;

    // Thread-safe counter for packets in the current time window
    private final AtomicInteger packetCount;

    // Timestamp (ms) marking the start of the current one-second window
    private final AtomicLong windowStart;

    // Tracks how many consecutive seconds the client has exceeded the limit
    private final AtomicInteger consecutiveViolations;

    /**
     * Creates a RateLimiter with default settings:
     * 60 packets/second, 3 consecutive violations before disconnect.
     */
    public RateLimiter() {
        this(DEFAULT_MAX_PACKETS_PER_SECOND, DEFAULT_VIOLATION_THRESHOLD);
    }

    /**
     * Creates a RateLimiter with custom settings.
     *
     * @param maxPacketsPerSecond the maximum allowed packets per second
     * @param violationThreshold consecutive violation count before forced disconnect
     */
    public RateLimiter(int maxPacketsPerSecond, int violationThreshold) {
        this.maxPacketsPerSecond = maxPacketsPerSecond;
        this.violationThreshold = violationThreshold;
        this.packetCount = new AtomicInteger(0);
        this.windowStart = new AtomicLong(System.currentTimeMillis());
        this.consecutiveViolations = new AtomicInteger(0);
    }

    /**
     * Checks whether an incoming packet should be allowed or dropped.
     *
     * This method is called by the ClientHandler for every raw packet
     * received from the network, BEFORE any parsing or deserialization.
     *
     * @return true if the packet is allowed, false if it should be dropped
     */
    public synchronized boolean allowPacket() {
        long now = System.currentTimeMillis();
        long elapsed = now - windowStart.get();

        // If more than 1 second has passed, reset the window
        if (elapsed >= 1000) {
            // Check if the previous window was a violation
            if (packetCount.get() > maxPacketsPerSecond) {
                consecutiveViolations.incrementAndGet();
                System.out.println("[RateLimiter] WARNING: Client exceeded limit ("
                        + packetCount.get() + "/" + maxPacketsPerSecond
                        + " packets). Violation #" + consecutiveViolations.get());
            } else {
                // Good behavior resets the violation counter
                consecutiveViolations.set(0);
            }

            // Start a new window
            windowStart.set(now);
            packetCount.set(0);
        }

        // Increment and check against the limit
        int currentCount = packetCount.incrementAndGet();

        if (currentCount > maxPacketsPerSecond) {
            // Packet is over the limit -> drop it silently
            return false;
        }

        return true;
    }

    /**
     * Determines if the client should be forcefully disconnected
     * due to persistent flooding behavior.
     *
     * Should be checked periodically by the ClientHandler after
     * allowPacket() returns false.
     *
     * @return true if consecutive violations exceed the threshold
     */
    public boolean shouldDisconnect() {
        return consecutiveViolations.get() >= violationThreshold;
    }

    /**
     * Resets the rate limiter state completely.
     * Useful for testing or when re-initializing a connection.
     */
    public void reset() {
        packetCount.set(0);
        windowStart.set(System.currentTimeMillis());
        consecutiveViolations.set(0);
    }

    // --- Getters for testing and monitoring ---

    public int getPacketCount() {
        return packetCount.get();
    }

    public int getConsecutiveViolations() {
        return consecutiveViolations.get();
    }

    public int getMaxPacketsPerSecond() {
        return maxPacketsPerSecond;
    }

    public int getViolationThreshold() {
        return violationThreshold;
    }
}
