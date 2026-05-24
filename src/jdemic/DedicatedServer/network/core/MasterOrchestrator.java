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

public class MasterOrchestrator {
    private static final int DEFAULT_MASTER_PORT = 8090;// 1st change-> 8090 instead of 8080 in master and config
    private static final String MASTER_PORT_ENV = "JDEMIC_ORCHESTRATOR_PORT";
    private static final String SERVER_MIN_PORT_ENV = "JDEMIC_SERVER_PORT_MIN";
    private static final String SERVER_MAX_PORT_ENV = "JDEMIC_SERVER_PORT_MAX";
    private static final String HOST_COMMAND = "HOST";
    private static final String LIST_COMMAND = "LIST";
    private static final int DEFAULT_BASE_PORT = 9001;
    private static final int DEFAULT_MAX_PORT = 9010;//2nd change-> reduced the number of servers from 1000 to 10

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
            while (true) {
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
        cleanupFinishedServers();
        int freePort = findFreePort();
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
        environment.put("JDEMIC_STATUS_PORT", String.valueOf(freePort + 1000));//now the status page should(actually will) work
        environment.put("JDEMIC_OPEN_BROWSER", "false");

        try {
            Process process = processBuilder.start();
            usedPorts.add(freePort);
            serverProcesses.put(freePort, process);
            out.println("SUCCESS:" + freePort);
            System.out.println("[MASTER] Game server launched on port " + freePort);
        } catch (IOException ex) {
            out.println("FAIL:LAUNCH_ERROR");
            System.err.println("[MASTER] Failed to launch game server: " + ex.getMessage());
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

    private static int findFreePort() {
        for (int port = basePort; port <= maxPort; port++) {
            if (!usedPorts.contains(port) && isPortAvailable(port)) {
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
