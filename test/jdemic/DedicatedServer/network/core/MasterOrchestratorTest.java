package jdemic.DedicatedServer.network.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

        assertTrue(response.startsWith("SUCCESS:"),
                "Orchestrator must return SUCCESS:<port> for HOST.");

        String trimmed = response.trim();
        String portText = trimmed.substring("SUCCESS:".length());
        int allocatedPort = Integer.parseInt(portText);

        assertTrue(allocatedPort >= 9001 && allocatedPort <= 9010,
                "Allocated port must be inside the configured game-server range.");
    }

    @Test
    void testConcurrentHostCommandsAllocateDifferentPorts() throws Exception {
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<String> responses = new CopyOnWriteArrayList<>();
        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Thread worker = new Thread(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    responses.add(sendCommand("HOST").trim());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            workers.add(worker);
            worker.start();
        }

        readyLatch.await();
        startLatch.countDown();

        for (Thread worker : workers) {
            worker.join();
        }

        assertEquals(2, responses.size(), "Both concurrent HOST requests must return a response.");

        String first = responses.get(0);
        String second = responses.get(1);

        assertTrue(first.startsWith("SUCCESS:"), "First HOST response must be SUCCESS:<port>.");
        assertTrue(second.startsWith("SUCCESS:"), "Second HOST response must be SUCCESS:<port>.");

        int firstPort = Integer.parseInt(first.substring("SUCCESS:".length()));
        int secondPort = Integer.parseInt(second.substring("SUCCESS:".length()));

        assertNotEquals(firstPort, secondPort,
                "Concurrent HOST requests must allocate different ports.");
    }
}