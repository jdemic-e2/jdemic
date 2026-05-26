package jdemic.DedicatedServer.network.core;

import java.util.Map;

public record DedicatedServerConfig(
        int serverPort,
        boolean statusEnabled,
        String statusHost,
        int statusPort,
        boolean openBrowser,
        long idleShutdownMillis,
        boolean exitProcessOnIdleShutdown
) {

    public static final int DEFAULT_SERVER_PORT = 9000;
    public static final boolean DEFAULT_STATUS_ENABLED = true;
    public static final String DEFAULT_STATUS_HOST = "localhost";
    public static final int DEFAULT_STATUS_PORT = 8090;
    public static final boolean DEFAULT_OPEN_BROWSER = false;
    public static final long DEFAULT_IDLE_SHUTDOWN_MILLIS = 0L;
    public static final boolean DEFAULT_EXIT_PROCESS_ON_IDLE_SHUTDOWN = false;

    static final String SERVER_PORT_ENV = "JDEMIC_SERVER_PORT";
    static final String STATUS_ENABLED_ENV = "JDEMIC_STATUS_ENABLED";
    static final String STATUS_HOST_ENV = "JDEMIC_STATUS_HOST";
    static final String STATUS_PORT_ENV = "JDEMIC_STATUS_PORT";
    static final String OPEN_BROWSER_ENV = "JDEMIC_OPEN_BROWSER";
    static final String IDLE_SHUTDOWN_MILLIS_ENV = "JDEMIC_IDLE_SHUTDOWN_MILLIS";
    static final String EXIT_PROCESS_ON_IDLE_SHUTDOWN_ENV = "JDEMIC_EXIT_PROCESS_ON_IDLE_SHUTDOWN";

    public static DedicatedServerConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static DedicatedServerConfig fromEnvironment(Map<String, String> env) {
        return new DedicatedServerConfig(
                parsePort(env.get(SERVER_PORT_ENV), DEFAULT_SERVER_PORT),
                parseBoolean(env.get(STATUS_ENABLED_ENV), DEFAULT_STATUS_ENABLED),
                parseString(env.get(STATUS_HOST_ENV), DEFAULT_STATUS_HOST),
                parsePort(env.get(STATUS_PORT_ENV), DEFAULT_STATUS_PORT),
                parseBoolean(env.get(OPEN_BROWSER_ENV), DEFAULT_OPEN_BROWSER),
                parseLong(env.get(IDLE_SHUTDOWN_MILLIS_ENV), DEFAULT_IDLE_SHUTDOWN_MILLIS),
                parseBoolean(env.get(EXIT_PROCESS_ON_IDLE_SHUTDOWN_ENV), DEFAULT_EXIT_PROCESS_ON_IDLE_SHUTDOWN)
        );
    }

    private static int parsePort(String rawValue, int defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(rawValue.trim());
            if (parsed >= 1 && parsed <= 65535) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to the default.
        }

        return defaultValue;
    }
    private static long parseLong(String rawValue, long defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        try {
            long parsed = Long.parseLong(rawValue.trim());
            if (parsed >= 0L) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to the default.
        }

        return defaultValue;
    }


    private static boolean parseBoolean(String rawValue, boolean defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        String normalized = rawValue.trim().toLowerCase();
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }

        return defaultValue;
    }

    private static String parseString(String rawValue, String defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        return rawValue.trim();
    }
}
