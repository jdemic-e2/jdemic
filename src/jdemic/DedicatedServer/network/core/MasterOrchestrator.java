package jdemic.DedicatedServer.network.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;


public class MasterOrchestrator {
    private static final int DEFAULT_MASTER_PORT = 8090;// 1st change-> 8090 instead of 8080 in master and config
    private static final String MASTER_PORT_ENV = "JDEMIC_ORCHESTRATOR_PORT";
    private static final String SERVER_MIN_PORT_ENV = "JDEMIC_SERVER_PORT_MIN";
    private static final String SERVER_MAX_PORT_ENV = "JDEMIC_SERVER_PORT_MAX";
    private static final String HOST_COMMAND = "HOST";
    private static final String LIST_COMMAND = "LIST";
    private static final int DEFAULT_BASE_PORT = 9001;
    private static final int DEFAULT_MAX_PORT = 9010;//2nd change-> reduced the number of servers from 1000 to 10
    private static final Object PORT_ALLOCATION_LOCK = new Object();
    private static final long SERVER_HEALTH_CHECK_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private static final long SERVER_HEALTH_CHECK_INTERVAL_MILLIS = 200L;

    private static final int masterPort = parsePort(System.getenv(MASTER_PORT_ENV), DEFAULT_MASTER_PORT);
    private static final int basePort = parsePort(System.getenv(SERVER_MIN_PORT_ENV), DEFAULT_BASE_PORT);
    private static final int maxPort = parsePort(System.getenv(SERVER_MAX_PORT_ENV), DEFAULT_MAX_PORT);
    private static final Set<Integer> usedPorts = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, Process> serverProcesses = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(MasterOrchestrator::stopSpawnedServers, "jdemic-master-shutdown"));

        try (ServerSocket serverSocket = new ServerSocket(masterPort)) {
            System.out.println("[MASTER] Orchestrator listening on port " + masterPort);
            System.out.println("[MASTER] Game server port range: " + basePort + "-" + maxPort);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(() -> handleClient(clientSocket), "jdemic-master-client");
                clientThread.setDaemon(true);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("[MASTER] Failed to start orchestrator: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {
            String line = in.readLine();
            if (line == null) {
                return;
            }

            if (line.startsWith("GET ")) {
                handleHttpRequest(clientSocket.getOutputStream(), line);
                return;
            }

            if (HOST_COMMAND.equals(line.trim())) {
                handleHostRequest(out);
                return;
            }

            if (LIST_COMMAND.equals(line.trim())) {
                handleListRequest(out);
                return;
            }

            out.println("FAIL:UNKNOWN_COMMAND");
        } catch (IOException e) {
            System.err.println("[MASTER] Client communication error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
                // Nothing else to do while closing a short-lived control connection.
            }
        }
    }

    private static void handleListRequest(PrintWriter out) {
        cleanupFinishedServers();
        String ports = serverProcesses.keySet().stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        out.println("PORTS:" + ports);
    }

    private static void handleHostRequest(PrintWriter out) {
        synchronized (PORT_ALLOCATION_LOCK) {
            cleanupFinishedServers();

            int freePort = reserveFreePort();
            if (freePort == -1) {
                out.println("FAIL:NO_PORT_AVAILABLE");
                return;
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "-cp",
                    getClasspath(),
                    "jdemic.DedicatedServer.network.core.JdemicNetworkServer"
            );
            processBuilder.inheritIO();

            Map<String, String> environment = processBuilder.environment();
            environment.put("JDEMIC_SERVER_PORT", String.valueOf(freePort));
            environment.put("JDEMIC_STATUS_ENABLED", "true");
            environment.put("JDEMIC_STATUS_PORT", String.valueOf(freePort + 1000));
            environment.put("JDEMIC_STATUS_HOST", "0.0.0.0");
            environment.put("JDEMIC_OPEN_BROWSER", "false");

            try {
                Process process = processBuilder.start();
                serverProcesses.put(freePort, process);

                boolean healthy = waitForServerHealth(freePort + 1000, process);
                if (!healthy) {
                    serverProcesses.remove(freePort);
                    usedPorts.remove(freePort);

                    if (process.isAlive()) {
                        process.destroy();
                    }

                    out.println("FAIL:SERVER_START_TIMEOUT");
                    System.err.println("[MASTER] Spawned server on port " + freePort
                            + " did not become healthy in time.");
                    return;
                }

                out.println("SUCCESS:" + freePort);
                System.out.println("[MASTER] Game server launched on port " + freePort);
            } catch (IOException ex) {
                usedPorts.remove(freePort);
                serverProcesses.remove(freePort);

                out.println("FAIL:LAUNCH_ERROR");
                System.err.println("[MASTER] Failed to launch game server: " + ex.getMessage());
            }
        }
    }

    private static void handleHttpRequest(OutputStream outputStream, String requestLine) throws IOException {
        if (requestLine.startsWith("GET /health ")) {
            String body = "{\"status\":\"ok\",\"service\":\"jdemic-master-orchestrator\",\"activeServers\":"
                    + countActiveServers()
                    + ",\"serverPortMin\":" + basePort
                    + ",\"serverPortMax\":" + maxPort
                    + "}";
            sendHttpResponse(outputStream, 200, "OK", "application/json; charset=UTF-8", body);
            return;
        }

        String body = "Jdemic master orchestrator";
        sendHttpResponse(outputStream, 200, "OK", "text/plain; charset=UTF-8", body);
    }

    private static void sendHttpResponse(
            OutputStream outputStream,
            int statusCode,
            String statusText,
            String contentType,
            String body
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        outputStream.write(headers.getBytes(StandardCharsets.UTF_8));
        outputStream.write(bytes);
        outputStream.flush();
    }

    private static int reserveFreePort() {
        for (int port = basePort; port <= maxPort; port++) {
            if (!usedPorts.contains(port) && isPortAvailable(port)) {
                usedPorts.add(port);
                return port;
            }
        }
        return -1;
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    private static boolean waitForServerHealth(int statusPort, Process process) {
        long deadline = System.currentTimeMillis() + SERVER_HEALTH_CHECK_TIMEOUT_MILLIS;

        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                return false;
            }

            try {
                URL url = new URL("http://localhost:" + statusPort + "/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(500);
                connection.setReadTimeout(500);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    return true;
                }
            } catch (IOException ignored) {
                // Server may still be starting up.
            }

            try {
                Thread.sleep(SERVER_HEALTH_CHECK_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    private static int countActiveServers() {
        cleanupFinishedServers();
        return serverProcesses.size();
    }

    private static void cleanupFinishedServers() {
        for (Map.Entry<Integer, Process> entry : serverProcesses.entrySet()) {
            if (!entry.getValue().isAlive()) {
                serverProcesses.remove(entry.getKey());
                usedPorts.remove(entry.getKey());
            }
        }
    }

    private static void stopSpawnedServers() {
        for (Process process : serverProcesses.values()) {
            if (process.isAlive()) {
                process.destroy();
            }
        }
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

    private static String getClasspath() {
        return System.getProperty("java.class.path");
    }
}
