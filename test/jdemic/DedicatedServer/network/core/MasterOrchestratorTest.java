package jdemic.DedicatedServer.network.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterOrchestratorTest {

    private static Thread orchestratorThread;
    private static final int PORT = 8090;

    @BeforeAll
    static void setUp() throws InterruptedException {
        // start the orchestrator in a separate (Daemon) thread to avoid blocking the test execution
        orchestratorThread = new Thread(() -> {
            MasterOrchestrator.main(new String[]{});
        });
        orchestratorThread.setDaemon(true);
        orchestratorThread.start();

        // wait 1 second to ensure the ServerSocket is up
        Thread.sleep(1000);
    }

    @AfterAll
    static void tearDown() {
        // stop the thread at the end
        if (orchestratorThread != null) {
            orchestratorThread.interrupt();
        }
    }

    /**
     * A helper method that opens a Socket, sends a string, and reads the response. 
     */
    private String sendCommand(String command) throws Exception {
        try (Socket socket = new Socket("localhost", PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(command);
            
            // read the full response (it may have multiple lines, like an HTTP response)
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        }
    }

    @Test
    void testUnknownCommandIsRejected() throws Exception {
        String response = sendCommand("BOGUS_COMMAND");

        // The orchestrator must reject an unknown command
        assertTrue(response.contains("FAIL:UNKNOWN_COMMAND"), 
                "Serverul trebuie sa returneze FAIL:UNKNOWN_COMMAND pentru o cerere gresita.");
    }

    @Test
    void testHealthEndpointReturnsJson() throws Exception {
        // Simulate a simple HTTP request (only the first line, because the orchestrator reads with in.readLine() and processes immediately)
        String response = sendCommand("GET /health HTTP/1.1");

        // check the header
        assertTrue(response.contains("HTTP/1.1 200 OK"), "Must return HTTP 200 status code");
        assertTrue(response.contains("Content-Type: application/json"), "Must be JSON");
        
        // check the JSON body
        assertTrue(response.contains("\"status\":\"ok\""), "Response must indicate status ok");
        assertTrue(response.contains("\"service\":\"jdemic-master-orchestrator\""), "Service must be correctly identified");
    }

    @Test
    void testHostCommandSpawnsServerAndReturnsPort() throws Exception {
        String response = sendCommand("HOST");
        
        // The HOST command should launch a new Java process (game server)
        // and respond with "SUCCESS:<port>" (the first available port is usually 9001)
        assertTrue(response.contains("SUCCESS:9001") || response.contains("SUCCESS:9002"),
                "Orchestrator must allocate a valid port (e.g., 9001) and start with SUCCESS:");
    }
}
