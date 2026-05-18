package jdemic.DedicatedServer.network.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class MasterOrchestrator {
    private static final int MASTER_PORT = 8080;
    private static final String HOST_COMMAND = "HOST";
    private static final int BASE_PORT = 9001;
    private static final int MAX_PORT = 9999;
    private static final Set<Integer> usedPorts = new HashSet<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(MASTER_PORT)) {
            System.out.println("[MASTER] Orchestrator ascultă pe portul " + MASTER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[MASTER] Eroare la pornirea orchestratorului: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {
            String line = in.readLine();
            if (HOST_COMMAND.equals(line)) {
                int freePort = findFreePort();
                if (freePort == -1) {
                    out.println("FAIL:NO_PORT_AVAILABLE");
                    return;
                }
                // Launch new server process
                ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", getClasspath(), "jdemic.DedicatedServer.network.core.JdemicNetworkServer", String.valueOf(freePort)
                );
                pb.inheritIO();
                try {
                    pb.start();
                    usedPorts.add(freePort);
                    out.println("SUCCESS:" + freePort);
                    System.out.println("[MASTER] Server lansat pe portul " + freePort);
                } catch (IOException ex) {
                    out.println("FAIL:LAUNCH_ERROR");
                    System.err.println("[MASTER] Eroare la lansarea serverului: " + ex.getMessage());
                }
            } else {
                out.println("FAIL:UNKNOWN_COMMAND");
            }
        } catch (IOException e) {
            System.err.println("[MASTER] Eroare la comunicarea cu clientul: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    private static int findFreePort() {
        for (int port = BASE_PORT; port <= MAX_PORT; port++) {
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

    private static String getClasspath() {
        return System.getProperty("java.class.path");
    }
}
