package jdemic.DedicatedServer.network.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.GameLogic.GameManager;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ServerStatusUi {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Supplier<GameManager> gameManagerSupplier;
    private final IntSupplier connectedPlayerCountSupplier;
    private final Supplier<Packet> latestPacketSupplier;
    private final Runnable shutdownAction;
    private HttpServer httpServer;

    public ServerStatusUi(
            Supplier<GameManager> gameManagerSupplier,
            IntSupplier connectedPlayerCountSupplier,
            Supplier<Packet> latestPacketSupplier,
            Runnable shutdownAction
    ) {
        this.gameManagerSupplier = gameManagerSupplier;
        this.connectedPlayerCountSupplier = connectedPlayerCountSupplier;
        this.latestPacketSupplier = latestPacketSupplier;
        this.shutdownAction = shutdownAction;
    }

    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            httpServer.createContext("/", this::handleIndex);
            httpServer.createContext("/state", this::handleState);
            httpServer.createContext("/shutdown", this::handleShutdown);
            httpServer.start();

            String url = "http://localhost:" + httpServer.getAddress().getPort() + "/";
            System.out.println("[ServerStatusUi] UI available at " + url);
            openBrowser(url);
        } catch (IOException e) {
            System.err.println("[ServerStatusUi] Could not start server UI: " + e.getMessage());
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        sendResponse(exchange, "text/html; charset=UTF-8", getHtml());
    }

    private void handleState(HttpExchange exchange) throws IOException {
        ObjectNode response = OBJECT_MAPPER.createObjectNode();
        response.put("connectedPlayers", connectedPlayerCountSupplier.getAsInt());

        GameManager gameManager = gameManagerSupplier.get();
        if (gameManager != null) {
            response.set("gameState", OBJECT_MAPPER.valueToTree(gameManager.getState()));
        } else {
            response.putNull("gameState");
        }

        Packet latestPacket = latestPacketSupplier.get();
        if (latestPacket != null) {
            response.set("latestPacket", OBJECT_MAPPER.valueToTree(latestPacket));
        } else {
            response.putNull("latestPacket");
        }

        sendResponse(exchange, "application/json; charset=UTF-8", OBJECT_MAPPER.writeValueAsString(response));
    }

    private void handleShutdown(HttpExchange exchange) throws IOException {
        sendResponse(exchange, "text/plain; charset=UTF-8", "Server closing");
        new Thread(() -> {
            shutdownAction.run();
            if (httpServer != null) {
                httpServer.stop(1);
            }
        }, "server-ui-shutdown").start();
    }

    private void sendResponse(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            System.err.println("[ServerStatusUi] Could not open browser automatically: " + e.getMessage());
        }
    }

    private String getHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Jdemic Network Server</title>
                    <style>
                        body {
                            margin: 24px;
                            font-family: Arial, sans-serif;
                            color: #e5edf6;
                            background: #0f1720;
                        }
                        h1 {
                            margin: 0;
                            font-size: 24px;
                        }
                        .topbar {
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            gap: 12px;
                            margin-bottom: 16px;
                        }
                        button {
                            padding: 10px 14px;
                            border: 1px solid #ef4444;
                            border-radius: 6px;
                            color: #fee2e2;
                            background: #7f1d1d;
                            font-weight: 700;
                            cursor: pointer;
                        }
                        button:hover {
                            background: #991b1b;
                        }
                        .summary {
                            display: flex;
                            gap: 12px;
                            margin-bottom: 16px;
                            flex-wrap: wrap;
                        }
                        .box {
                            padding: 12px;
                            border: 1px solid #263445;
                            border-radius: 6px;
                            background: #162231;
                        }
                        .field-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                            gap: 10px;
                            margin-bottom: 16px;
                        }
                        .field {
                            min-height: 58px;
                            padding: 10px;
                            border: 1px solid #263445;
                            border-radius: 6px;
                            background: #111c2a;
                            box-sizing: border-box;
                        }
                        .label {
                            font-size: 12px;
                            color: #94a3b8;
                            margin-bottom: 4px;
                        }
                        .value {
                            font-size: 20px;
                            font-weight: 700;
                            overflow-wrap: anywhere;
                        }
                        .field .value {
                            font-size: 14px;
                            font-weight: 600;
                        }
                        .json-value {
                            white-space: pre-wrap;
                            font-family: Consolas, monospace;
                            font-size: 12px;
                            line-height: 1.4;
                        }
                        .empty {
                            color: #94a3b8;
                            font-size: 14px;
                        }
                        pre {
                            margin: 0;
                            padding: 12px;
                            overflow: auto;
                            border: 1px solid #263445;
                            border-radius: 6px;
                            background: #111c2a;
                            font-size: 13px;
                        }
                        .players {
                            display: grid;
                            gap: 10px;
                            margin-bottom: 16px;
                        }
                        h2 {
                            margin: 20px 0 8px;
                            font-size: 18px;
                        }
                    </style>
                </head>
                <body>
                    <div class="topbar">
                        <h1>Jdemic Network Server</h1>
                        <button id="shutdownButton" type="button">Close Server</button>
                    </div>
                    <div class="summary">
                        <div class="box">
                            <div class="label">Players connected</div>
                            <div id="connectedPlayers" class="value">0</div>
                        </div>
                        <div class="box">
                            <div class="label">Game started</div>
                            <div id="gameStarted" class="value">false</div>
                        </div>
                        <div class="box">
                            <div class="label">Game over</div>
                            <div id="gameOver" class="value">false</div>
                        </div>
                    </div>

                    <h2>Latest Packet</h2>
                    <div id="latestPacket" class="field-grid"></div>

                    <h2>GameState</h2>
                    <div id="gameState" class="field-grid"></div>

                    <h2>Players</h2>
                    <div id="players" class="players"></div>

                    <script>
                        const hiddenGameStateFields = new Set(['players', 'lobbyChatMessages', 'diseaseManager', 'cardDeck', 'map']);
                        const hiddenPlayerFields = new Set(['player']);

                        function formatValue(value) {
                            if (value === null || value === undefined) {
                                return 'null';
                            }
                            if (typeof value === 'object') {
                                return JSON.stringify(value, null, 2);
                            }
                            return String(value);
                        }

                        function addField(parent, name, value) {
                            const field = document.createElement('div');
                            field.className = 'field';

                            const label = document.createElement('div');
                            label.className = 'label';
                            label.textContent = name;

                            const valueElement = document.createElement('div');
                            valueElement.className = typeof value === 'object' && value !== null
                                ? 'value json-value'
                                : 'value';
                            valueElement.textContent = formatValue(value);

                            field.appendChild(label);
                            field.appendChild(valueElement);
                            parent.appendChild(field);
                        }

                        function renderObject(parent, object, emptyMessage, hiddenFields = new Set()) {
                            parent.innerHTML = '';
                            if (!object || Object.keys(object).length === 0) {
                                const empty = document.createElement('div');
                                empty.className = 'box empty';
                                empty.textContent = emptyMessage;
                                parent.appendChild(empty);
                                return;
                            }

                            Object.entries(object).forEach(([name, value]) => {
                                if (!hiddenFields.has(name)) {
                                    addField(parent, name, value);
                                }
                            });
                        }

                        async function refresh() {
                            try {
                                const response = await fetch('/state', { cache: 'no-store' });
                                const data = await response.json();
                                const gameState = data.gameState || {};
                                const players = gameState.players || [];
                                const latestPacket = data.latestPacket;

                                document.getElementById('connectedPlayers').textContent = data.connectedPlayers ?? 0;
                                document.getElementById('gameStarted').textContent = Boolean(gameState.gameStarted);
                                document.getElementById('gameOver').textContent = Boolean(gameState.gameOver);

                                renderObject(
                                    document.getElementById('latestPacket'),
                                    latestPacket,
                                    'No packets received yet.'
                                );
                                renderObject(
                                    document.getElementById('gameState'),
                                    gameState,
                                    'No game state available.',
                                    hiddenGameStateFields
                                );

                                const playersElement = document.getElementById('players');
                                playersElement.innerHTML = '';
                                if (players.length === 0) {
                                    const empty = document.createElement('div');
                                    empty.className = 'box empty';
                                    empty.textContent = 'No players registered yet.';
                                    playersElement.appendChild(empty);
                                } else {
                                    players.forEach((player, index) => {
                                        const playerBox = document.createElement('div');
                                        playerBox.className = 'box';

                                        const title = document.createElement('div');
                                        title.className = 'label';
                                        title.textContent = 'Player ' + (index + 1);
                                        playerBox.appendChild(title);

                                        const fields = document.createElement('div');
                                        fields.className = 'field-grid';
                                        renderObject(fields, player, 'No player state available.', hiddenPlayerFields);
                                        playerBox.appendChild(fields);
                                        playersElement.appendChild(playerBox);
                                    });
                                }
                            } catch (error) {
                                renderObject(
                                    document.getElementById('gameState'),
                                    { error: 'Unable to load server state: ' + error },
                                    'Unable to load server state.'
                                );
                            }
                        }

                        refresh();
                        setInterval(refresh, 500);

                        document.getElementById('shutdownButton').addEventListener('click', async () => {
                            const button = document.getElementById('shutdownButton');
                            button.disabled = true;
                            button.textContent = 'Closing...';
                            try {
                                await fetch('/shutdown', { method: 'POST' });
                                button.textContent = 'Server Closed';
                            } catch (error) {
                                button.textContent = 'Close Failed';
                            }
                        });
                    </script>
                </body>
                </html>
                """;
    }
}
