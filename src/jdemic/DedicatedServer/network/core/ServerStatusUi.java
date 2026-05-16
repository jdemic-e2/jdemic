package jdemic.DedicatedServer.network.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerStatusUi {

    private final DedicatedServerConfig config;
    private HttpServer httpServer;
    private ExecutorService executor;

    public ServerStatusUi(DedicatedServerConfig config) {
        this.config = config;
    }

    public synchronized void start() throws IOException {
        if (!config.statusEnabled()) {
            System.out.println("[Status] Server status UI disabled.");
            return;
        }

        httpServer = HttpServer.create(new InetSocketAddress(config.statusHost(), config.statusPort()), 0);
        httpServer.createContext("/health", this::handleHealth);
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jdemic-status-ui");
            thread.setDaemon(true);
            return thread;
        });
        httpServer.setExecutor(executor);
        httpServer.start();

        System.out.println("[Status] Listening on http://" + config.statusHost() + ":" + getPort() + "/health");
        openBrowserIfRequested();
    }

    public synchronized void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    int getPort() {
        if (httpServer == null) {
            return config.statusPort();
        }

        return httpServer.getAddress().getPort();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        byte[] responseBody = buildHealthJson().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, responseBody.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(responseBody);
        }
    }

    String buildHealthJson() {
        return "{\"status\":\"ok\",\"service\":\"jdemic-dedicated-server\",\"gameServerPort\":"
                + config.serverPort()
                + "}";
    }

    private void openBrowserIfRequested() {
        if (!config.openBrowser()) {
            return;
        }

        if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()) {
            System.out.println("[Status] Browser launch skipped because this environment is headless.");
            return;
        }

        try {
            String browserHost = "0.0.0.0".equals(config.statusHost()) ? "localhost" : config.statusHost();
            Desktop.getDesktop().browse(URI.create("http://" + browserHost + ":" + getPort() + "/health"));
        } catch (Exception e) {
            System.err.println("[Status] Browser launch failed: " + e.getMessage());
        }
    }
}
