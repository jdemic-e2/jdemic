package jdemic.DedicatedServer.network.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DedicatedServerConfigTest {

    @Test
    void shouldUseLocalDevelopmentDefaultsWhenEnvironmentIsEmpty() {
        DedicatedServerConfig config = DedicatedServerConfig.fromEnvironment(Map.of());

        assertEquals(9000, config.serverPort());
        assertTrue(config.statusEnabled());
        assertEquals("localhost", config.statusHost());
        assertEquals(8090, config.statusPort());
        assertFalse(config.openBrowser());
        assertEquals(0L, config.idleShutdownMillis());
        assertFalse(config.exitProcessOnIdleShutdown());
    }

    @Test
    void shouldReadValidEnvironmentOverrides() {
        DedicatedServerConfig config = DedicatedServerConfig.fromEnvironment(Map.of(
                "JDEMIC_SERVER_PORT", "9100",
                "JDEMIC_STATUS_ENABLED", "false",
                "JDEMIC_STATUS_HOST", "0.0.0.0",
                "JDEMIC_STATUS_PORT", "8181",
                "JDEMIC_OPEN_BROWSER", "true",
                "JDEMIC_IDLE_SHUTDOWN_MILLIS", "60000",
                "JDEMIC_EXIT_PROCESS_ON_IDLE_SHUTDOWN", "true"
        ));

        assertEquals(9100, config.serverPort());
        assertFalse(config.statusEnabled());
        assertEquals("0.0.0.0", config.statusHost());
        assertEquals(8181, config.statusPort());
        assertTrue(config.openBrowser());
        assertEquals(60000L, config.idleShutdownMillis());
        assertTrue(config.exitProcessOnIdleShutdown());
    }

    @Test
    void shouldFallBackToDefaultsForInvalidEnvironmentValues() {
        DedicatedServerConfig config = DedicatedServerConfig.fromEnvironment(Map.of(
                "JDEMIC_SERVER_PORT", "not-a-port",
                "JDEMIC_STATUS_ENABLED", "sometimes",
                "JDEMIC_STATUS_HOST", " ",
                "JDEMIC_STATUS_PORT", "70000",
                "JDEMIC_OPEN_BROWSER", "yes",
                "JDEMIC_IDLE_SHUTDOWN_MILLIS", "-5",
                "JDEMIC_EXIT_PROCESS_ON_IDLE_SHUTDOWN", "maybe"
        ));

        assertEquals(9000, config.serverPort());
        assertTrue(config.statusEnabled());
        assertEquals("localhost", config.statusHost());
        assertEquals(8090, config.statusPort());
        assertFalse(config.openBrowser());
        assertEquals(0L, config.idleShutdownMillis());
        assertFalse(config.exitProcessOnIdleShutdown());
    }
}
