package jdemic.DedicatedServer.network.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jdemic.DedicatedServer.network.core.DedicatedServerConfig;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.DiseaseColor;
import jdemic.GameLogic.DiseaseManager;
import jdemic.GameLogic.GameManager;
import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerStatusUi {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = Logger.getLogger(ServerStatusUi.class.getName());
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private final DedicatedServerConfig config;
    private final Supplier<GameManager> gameManagerSupplier;
    private final IntSupplier connectedPlayerCountSupplier;
    private final Supplier<Packet> latestPacketSupplier;
    private final Runnable shutdownAction;
    private HttpServer httpServer;
    private ExecutorService executor;

    public ServerStatusUi(
            DedicatedServerConfig config,
            Supplier<GameManager> gameManagerSupplier,
            IntSupplier connectedPlayerCountSupplier,
            Supplier<Packet> latestPacketSupplier,
            Runnable shutdownAction
    ) {
        this.config = config;
        this.gameManagerSupplier = gameManagerSupplier;
        this.connectedPlayerCountSupplier = connectedPlayerCountSupplier;
        this.latestPacketSupplier = latestPacketSupplier;
        this.shutdownAction = shutdownAction;
    }

    public synchronized void start() throws IOException {
        if (!config.statusEnabled()) {
            System.out.println("[ServerStatusUi] UI disabled.");
            return;
        }
        if (httpServer != null) {
            return;
        }

        httpServer = HttpServer.create(new InetSocketAddress(config.statusHost(), config.statusPort()), 0);
        httpServer.createContext("/", this::handleIndex);
        httpServer.createContext("/state", this::handleState);
        httpServer.createContext("/health", this::handleHealth);
        httpServer.createContext("/shutdown", this::handleShutdown);
        executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "jdemic-status-ui");
            thread.setDaemon(true);
            return thread;
        });
        httpServer.setExecutor(executor);
        httpServer.start();

        String url = getBrowserUrl();
        System.out.println("[ServerStatusUi] UI available at " + url);
        openBrowser(url);
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

    private String getBrowserUrl() {
        String browserHost = "0.0.0.0".equals(config.statusHost()) ? "localhost" : config.statusHost();
        return "http://" + browserHost + ":" + httpServer.getAddress().getPort() + "/";
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) return;

        ObjectNode response = OBJECT_MAPPER.createObjectNode();
        response.put("status", "ok");
        response.put("service", "jdemic-dedicated-server");
        response.put("gameServerPort", config.serverPort());
        response.put("connectedPlayers", connectedPlayerCountSupplier.getAsInt());
        sendResponse(exchange, JSON_CONTENT_TYPE, OBJECT_MAPPER.writeValueAsString(response));
    }

    private void openBrowser(String url) {
        if (!config.openBrowser()) return;

        try {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Standard browser launch failed. Open manually: " + url, e);
        }

        LOGGER.info("[ServerStatusUi] Browser auto-open unavailable. Open manually: " + url);
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) return;
        sendResponse(exchange, "text/html; charset=UTF-8", getHtml());
    }

    private void handleState(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) return;

        ObjectNode response = OBJECT_MAPPER.createObjectNode();
        response.put("connectedPlayers", connectedPlayerCountSupplier.getAsInt());

        GameManager gameManager = gameManagerSupplier.get();
        if (gameManager != null) {
            response.set("gameState", buildGameStateSummary(gameManager));
        } else {
            response.putNull("gameState");
        }

        Packet latestPacket = latestPacketSupplier.get();
        if (latestPacket != null) {
            response.set("latestPacket", OBJECT_MAPPER.valueToTree(latestPacket));
        } else {
            response.putNull("latestPacket");
        }

        sendResponse(exchange, JSON_CONTENT_TYPE, OBJECT_MAPPER.writeValueAsString(response));
    }

    private void handleShutdown(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) return;
        if (!isLocalRequest(exchange)) {
            sendResponse(exchange, 403, JSON_CONTENT_TYPE, "{\"error\":\"forbidden\"}");
            return;
        }
        sendResponse(exchange, "text/plain; charset=UTF-8", "Server closing");
        new Thread(shutdownAction, "server-ui-shutdown").start();
    }

    private void sendResponse(HttpExchange exchange, String contentType, String body) throws IOException {
        sendResponse(exchange, 200, contentType, body);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (method.equalsIgnoreCase(exchange.getRequestMethod())) return true;
        exchange.getResponseHeaders().set("Allow", method.toUpperCase());
        sendResponse(exchange, 405, JSON_CONTENT_TYPE, "{\"error\":\"method_not_allowed\"}");
        return false;
    }

    private boolean isLocalRequest(HttpExchange exchange) {
        if (exchange.getRemoteAddress() == null || exchange.getRemoteAddress().getAddress() == null) return false;
        return exchange.getRemoteAddress().getAddress().isLoopbackAddress()
                || exchange.getRemoteAddress().getAddress().isAnyLocalAddress();
    }

    // ─── State Building ───────────────────────────────────────────────────────

    ObjectNode buildGameStateSummary(GameManager gameManager) {
        ObjectNode summary = OBJECT_MAPPER.createObjectNode();

        synchronized (gameManager.getStateLock()) {
            GameState state = gameManager.getState();

            // ── Core flags
            summary.put("gameStarted", state.isGameStarted());
            summary.put("gameOver", state.isGameOver());
            summary.put("gameWon", state.isGameWon());
            summary.put("skipInfection", state.isSkipInfection());
            summary.put("currentPlayerIndex", state.getCurrentPlayerIndex());
            summary.put("actionsRemaining", state.getActionsRemaining());
            summary.put("infectionRate", state.getInfectionRate());
            summary.put("epidemicCount", state.getEpidemicCount());
            summary.put("lobbyCountdownStartedAt", state.getLobbyCountdownStartedAt());

            // ── Current player name (convenience)
            PlayerState currentPlayer = state.getCurrentPlayer();
            summary.put("currentPlayerName", currentPlayer != null ? currentPlayer.getPlayerName() : null);

            // ── Disease manager
            DiseaseManager dm = state.getDiseaseManager();
            if (dm != null) {
                summary.set("diseases", buildDiseaseNode(dm));
            } else {
                summary.putNull("diseases");
            }

            // ── Players
            ArrayNode players = summary.putArray("players");
            for (PlayerState player : state.getPlayers()) {
                players.add(buildPlayerNode(player, currentPlayer));
            }

            // ── Map / cities
            if (state.getMap() != null) {
                summary.set("cities", buildCitiesNode(state.getMap().getCityList()));
            } else {
                summary.putNull("cities");
            }
        }

        return summary;
    }

    private ObjectNode buildDiseaseNode(DiseaseManager dm) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("outbreakScore", dm.getOutbreakScore());
        node.put("infectionCubesLeft", dm.getInfectionCubesLeft());

        Map<DiseaseColor, Boolean> cured = dm.getCuredDiseases();
        ObjectNode curesNode = node.putObject("cures");
        for (Map.Entry<DiseaseColor, Boolean> entry : cured.entrySet()) {
            curesNode.put(entry.getKey().name(), entry.getValue());
        }

        return node;
    }

    private ObjectNode buildPlayerNode(PlayerState player, PlayerState currentPlayer) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("playerName", player.getPlayerName());
        node.put("ready", player.isReady());
        node.put("discardingCards", player.getIsDiscarding());
        node.put("isCurrentTurn", currentPlayer != null
                && player.getPlayerName().equals(currentPlayer.getPlayerName()));

        if (player.getPlayerRole() != null) {
            node.put("playerRole", player.getPlayerRole().name());
        } else {
            node.putNull("playerRole");
        }

        CityNode city = player.getPlayerCurrentCity();
        node.put("currentCity", city != null ? city.getName() : null);
        if (city != null) {
            node.put("currentCityColor", city.getNativeColor().name());
        } else {
            node.putNull("currentCityColor");
        }

        // Hand — count only (card details are secret)
        node.put("handCount", player.getHand() != null ? player.getHand().size() : 0);

        return node;
    }

    private ArrayNode buildCitiesNode(List<CityNode> cities) {
        ArrayNode arr = OBJECT_MAPPER.createArrayNode();
        for (CityNode city : cities) {
            ObjectNode node = OBJECT_MAPPER.createObjectNode();
            node.put("name", city.getName());
            node.put("color", city.getNativeColor().name());
            node.put("hasResearchStation", city.hasResearchStation());

            // Cube counts per color
            ObjectNode cubes = node.putObject("cubes");
            for (DiseaseColor color : DiseaseColor.values()) {
                int count = city.getCubeCount(color);
                if (count > 0) {
                    cubes.put(color.name(), count);
                }
            }

            // Connected cities
            ArrayNode connections = node.putArray("connectedCities");
            if (city.getConnectedCities() != null) {
                for (CityNode neighbor : city.getConnectedCities()) {
                    connections.add(neighbor.getName());
                }
            }

            arr.add(node);
        }
        return arr;
    }

    // ─── HTML ─────────────────────────────────────────────────────────────────

    private String getHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Jdemic Network Server</title>
                    <style>
                        *, *::before, *::after { box-sizing: border-box; }
                        :root {
                            --bg:        #0b1218;
                            --surface:   #111c2a;
                            --card:      #162231;
                            --border:    #1e3048;
                            --border-hi: #2a4a6a;
                            --text:      #dce8f5;
                            --muted:     #7a99b8;
                            --accent:    #3b82f6;
                            --green:     #22c55e;
                            --red:       #ef4444;
                            --yellow:    #eab308;
                            --orange:    #f97316;
                            --blue-d:    #1d4ed8;
                            --tag-h:     11px;
                        }
                        body {
                            margin: 0;
                            font-family: 'Segoe UI', Arial, sans-serif;
                            font-size: 13px;
                            color: var(--text);
                            background: var(--bg);
                            min-height: 100vh;
                        }

                        /* ── Layout */
                        .topbar {
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            padding: 14px 24px;
                            background: var(--surface);
                            border-bottom: 1px solid var(--border);
                            position: sticky;
                            top: 0;
                            z-index: 100;
                        }
                        .topbar h1 { margin: 0; font-size: 18px; letter-spacing: .5px; }
                        .topbar-right { display: flex; align-items: center; gap: 12px; }
                        .pulse { width: 8px; height: 8px; border-radius: 50%; background: var(--green); animation: pulse 1.8s infinite; }
                        @keyframes pulse { 0%,100%{opacity:1}50%{opacity:.3} }

                        .main { padding: 20px 24px; }

                        /* ── Tabs */
                        .tabs { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1px solid var(--border); }
                        .tab {
                            padding: 8px 18px;
                            border: none;
                            border-radius: 6px 6px 0 0;
                            background: transparent;
                            color: var(--muted);
                            font-size: 13px;
                            font-weight: 600;
                            cursor: pointer;
                            border-bottom: 2px solid transparent;
                            margin-bottom: -1px;
                        }
                        .tab:hover { color: var(--text); }
                        .tab.active { color: var(--accent); border-bottom-color: var(--accent); }
                        .tab-panel { display: none; }
                        .tab-panel.active { display: block; }

                        /* ── Stat pills */
                        .stats-row { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 20px; }
                        .stat {
                            padding: 10px 16px;
                            border: 1px solid var(--border);
                            border-radius: 8px;
                            background: var(--card);
                            min-width: 110px;
                        }
                        .stat .label { font-size: 10px; text-transform: uppercase; letter-spacing: .8px; color: var(--muted); margin-bottom: 4px; }
                        .stat .val { font-size: 22px; font-weight: 800; line-height: 1; }
                        .val.ok   { color: var(--green); }
                        .val.warn { color: var(--yellow); }
                        .val.bad  { color: var(--red); }

                        /* ── Section headers */
                        .section-title {
                            font-size: 11px;
                            font-weight: 700;
                            text-transform: uppercase;
                            letter-spacing: .9px;
                            color: var(--muted);
                            margin: 20px 0 10px;
                            padding-bottom: 6px;
                            border-bottom: 1px solid var(--border);
                        }

                        /* ── Disease panel */
                        .disease-grid { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
                        .disease-card {
                            padding: 12px 16px;
                            border-radius: 8px;
                            border: 1px solid var(--border);
                            min-width: 120px;
                            position: relative;
                        }
                        .disease-card.BLUE   { border-color: #3b82f6; background: #0c1f3a; }
                        .disease-card.YELLOW { border-color: #eab308; background: #1a1500; }
                        .disease-card.BLACK  { border-color: #94a3b8; background: #111318; }
                        .disease-card.RED    { border-color: #ef4444; background: #1f0c0c; }
                        .disease-name { font-weight: 700; font-size: 13px; margin-bottom: 6px; }
                        .disease-card.BLUE   .disease-name { color: #60a5fa; }
                        .disease-card.YELLOW .disease-name { color: #facc15; }
                        .disease-card.BLACK  .disease-name { color: #cbd5e1; }
                        .disease-card.RED    .disease-name { color: #f87171; }
                        .cured-badge {
                            display: inline-block;
                            font-size: 10px;
                            font-weight: 700;
                            padding: 2px 7px;
                            border-radius: 99px;
                            background: var(--green);
                            color: #000;
                        }
                        .not-cured { font-size: 11px; color: var(--muted); }

                        /* ── Player cards */
                        .players-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
                        .player-card {
                            border: 1px solid var(--border);
                            border-radius: 10px;
                            background: var(--card);
                            overflow: hidden;
                        }
                        .player-card.active-turn { border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
                        .player-header {
                            display: flex;
                            align-items: center;
                            gap: 10px;
                            padding: 10px 14px;
                            background: var(--surface);
                            border-bottom: 1px solid var(--border);
                        }
                        .player-avatar {
                            width: 32px; height: 32px;
                            border-radius: 50%;
                            background: var(--blue-d);
                            display: flex; align-items: center; justify-content: center;
                            font-weight: 800; font-size: 14px; flex-shrink: 0;
                        }
                        .player-name { font-weight: 700; font-size: 14px; }
                        .player-role { font-size: 11px; color: var(--muted); }
                        .turn-indicator {
                            margin-left: auto;
                            font-size: 10px;
                            font-weight: 700;
                            padding: 3px 8px;
                            border-radius: 99px;
                            background: var(--accent);
                            color: #fff;
                        }
                        .player-body { padding: 12px 14px; }
                        .player-meta { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
                        .badge {
                            font-size: 10px;
                            font-weight: 700;
                            padding: 2px 8px;
                            border-radius: 99px;
                            border: 1px solid var(--border);
                        }
                        .badge.ready    { background: #14532d; border-color: #22c55e; color: #86efac; }
                        .badge.waiting  { background: #1c1400; border-color: #eab308; color: #fde047; }
                        .badge.discard  { background: #450a0a; border-color: #ef4444; color: #fca5a5; }
                        .city-tag {
                            display: inline-flex; align-items: center; gap: 5px;
                            font-size: 11px; padding: 2px 8px;
                            border-radius: 6px; border: 1px solid var(--border);
                            background: var(--surface);
                        }
                        .dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
                        .dot.BLUE   { background: #3b82f6; }
                        .dot.YELLOW { background: #eab308; }
                        .dot.BLACK  { background: #94a3b8; }
                        .dot.RED    { background: #ef4444; }

                        .hand-count-badge {
                            display: inline-flex; align-items: center; gap: 5px;
                            font-size: 11px; padding: 2px 10px;
                            border-radius: 6px; border: 1px solid var(--border);
                            background: var(--surface); font-weight: 700;
                        }
                        .hand-count-badge .hc-num { font-size: 15px; font-weight: 800; color: var(--text); }
                        .hand-count-badge.hc-warn { border-color: #f97316; background: #1f1000; }
                        .hand-count-badge.hc-over { border-color: #ef4444; background: #1f0000; }

                        .empty-msg { color: var(--muted); font-size: 13px; font-style: italic; padding: 12px 0; }
                        .city-search {
                            width: 100%; max-width: 360px;
                            padding: 7px 12px;
                            border-radius: 7px;
                            border: 1px solid var(--border);
                            background: var(--surface);
                            color: var(--text);
                            font-size: 13px;
                            margin-bottom: 14px;
                            outline: none;
                        }
                        .city-search:focus { border-color: var(--accent); }
                        .city-filter-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
                        .filter-btn {
                            padding: 4px 12px; border-radius: 99px; font-size: 11px; font-weight: 700;
                            border: 1px solid var(--border); background: var(--card); color: var(--muted); cursor: pointer;
                        }
                        .filter-btn.active-filter { color: #fff; }
                        .filter-btn[data-color="ALL"].active-filter  { background: var(--accent); border-color: var(--accent); }
                        .filter-btn[data-color="BLUE"].active-filter  { background: #1d4ed8; border-color: #3b82f6; }
                        .filter-btn[data-color="YELLOW"].active-filter { background: #854d0e; border-color: #eab308; }
                        .filter-btn[data-color="BLACK"].active-filter  { background: #374151; border-color: #9ca3af; }
                        .filter-btn[data-color="RED"].active-filter   { background: #991b1b; border-color: #ef4444; }
                        .filter-btn[data-color="HOT"].active-filter   { background: #7c2d12; border-color: #f97316; }
                        .filter-btn[data-color="RS"].active-filter    { background: #1a3a1a; border-color: #22c55e; }
                        .cities-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
                        .city-card {
                            padding: 10px 12px;
                            border-radius: 8px;
                            background: var(--card);
                            border: 1px solid var(--border);
                            position: relative;
                        }
                        .city-card.has-rs { border-color: var(--green); }
                        .city-card-top { display: flex; align-items: center; gap: 7px; margin-bottom: 6px; }
                        .city-name { font-weight: 700; font-size: 12px; flex: 1; }
                        .rs-icon { font-size: 13px; }
                        .cubes-row { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 6px; }
                        .cube-block {
                            display: inline-flex; align-items: center; gap: 3px;
                            padding: 1px 6px; border-radius: 4px; font-size: 10px; font-weight: 800;
                        }
                        .cube-block.BLUE   { background: #1e3a6e; color: #93c5fd; }
                        .cube-block.YELLOW { background: #3a2d00; color: #fde68a; }
                        .cube-block.BLACK  { background: #1f2937; color: #d1d5db; }
                        .cube-block.RED    { background: #3b0a0a; color: #fca5a5; }
                        .connections { font-size: 10px; color: var(--muted); line-height: 1.5; }

                        /* ── Packet panel */
                        .packet-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
                        .field {
                            padding: 10px 12px;
                            border: 1px solid var(--border);
                            border-radius: 8px;
                            background: var(--surface);
                        }
                        .field .label { font-size: 10px; text-transform: uppercase; letter-spacing: .7px; color: var(--muted); margin-bottom: 4px; }
                        .field .value { font-size: 13px; font-weight: 600; overflow-wrap: anywhere; }
                        .field .value.mono { font-family: Consolas, monospace; font-size: 11px; white-space: pre-wrap; }

                        .empty-msg { color: var(--muted); font-size: 13px; font-style: italic; padding: 12px 0; }
                    </style>
                </head>
                <body>

                <div class="topbar">
                    <h1>⚡ Jdemic Network Server</h1>
                    <div class="topbar-right">
                        <div class="pulse" id="pulse" title="Polling active"></div>
                    </div>
                </div>

                <div class="main">
                    <!-- Summary stats always visible -->
                    <div class="stats-row" id="statsRow">
                        <div class="stat"><div class="label">Connected</div><div id="statConnected" class="val">0</div></div>
                        <div class="stat"><div class="label">Game Started</div><div id="statStarted" class="val">—</div></div>
                        <div class="stat"><div class="label">Game Over</div><div id="statOver" class="val">—</div></div>
                        <div class="stat"><div class="label">Current Turn</div><div id="statTurn" class="val" style="font-size:14px">—</div></div>
                        <div class="stat"><div class="label">Actions Left</div><div id="statActions" class="val">—</div></div>
                        <div class="stat"><div class="label">Infection Rate</div><div id="statInfRate" class="val">—</div></div>
                        <div class="stat"><div class="label">Epidemics</div><div id="statEpidemic" class="val">—</div></div>
                        <div class="stat"><div class="label">Cubes Left</div><div id="statCubes" class="val">—</div></div>
                        <div class="stat"><div class="label">Outbreaks</div><div id="statOutbreaks" class="val">—</div></div>
                    </div>

                    <div class="tabs">
                        <button class="tab active" data-panel="panelPlayers">Players</button>
                        <button class="tab" data-panel="panelDiseases">Diseases</button>
                        <button class="tab" data-panel="panelCities">Cities</button>
                        <button class="tab" data-panel="panelPacket">Latest Packet</button>
                    </div>

                    <!-- Players -->
                    <div class="tab-panel active" id="panelPlayers">
                        <div id="playersGrid" class="players-grid"></div>
                    </div>

                    <!-- Diseases -->
                    <div class="tab-panel" id="panelDiseases">
                        <div class="section-title">Disease Status</div>
                        <div id="diseaseGrid" class="disease-grid"></div>
                        <div class="section-title">Outbreak Score</div>
                        <div id="outbreakMeter"></div>
                    </div>

                    <!-- Cities -->
                    <div class="tab-panel" id="panelCities">
                        <input class="city-search" id="citySearch" type="text" placeholder="Search cities…">
                        <div class="city-filter-row">
                            <button class="filter-btn active-filter" data-color="ALL">All</button>
                            <button class="filter-btn" data-color="BLUE">🔵 Blue</button>
                            <button class="filter-btn" data-color="YELLOW">🟡 Yellow</button>
                            <button class="filter-btn" data-color="BLACK">⚪ Black</button>
                            <button class="filter-btn" data-color="RED">🔴 Red</button>
                            <button class="filter-btn" data-color="HOT">🔥 Infected</button>
                            <button class="filter-btn" data-color="RS">🏥 Research Station</button>
                        </div>
                        <div id="citiesGrid" class="cities-grid"></div>
                    </div>

                    <!-- Latest Packet -->
                    <div class="tab-panel" id="panelPacket">
                        <div class="section-title">Latest Packet</div>
                        <div id="packetGrid" class="packet-grid"></div>
                    </div>
                </div>

                <script>
                (function() {
                    // ── Tab switching
                    document.querySelectorAll('.tab').forEach(btn => {
                        btn.addEventListener('click', () => {
                            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
                            document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
                            btn.classList.add('active');
                            document.getElementById(btn.dataset.panel).classList.add('active');
                        });
                    });

                    // ── City filter state
                    let cityFilter = 'ALL';
                    let citySearch = '';
                    let allCities = [];

                    document.querySelectorAll('.filter-btn').forEach(btn => {
                        btn.addEventListener('click', () => {
                            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active-filter'));
                            btn.classList.add('active-filter');
                            cityFilter = btn.dataset.color;
                            renderCities(allCities);
                        });
                    });

                    document.getElementById('citySearch').addEventListener('input', e => {
                        citySearch = e.target.value.toLowerCase();
                        renderCities(allCities);
                    });

                    // ── Color helpers
                    const COLOR_DOT = { BLUE: '#3b82f6', YELLOW: '#eab308', BLACK: '#9ca3af', RED: '#ef4444' };

                    function colorDot(color) {
                        return `<span class="dot ${color}" style="background:${COLOR_DOT[color] || '#888'}"></span>`;
                    }

                    // ── Stats row
                    function updateStats(data) {
                        const gs = data.gameState || {};
                        const dm = gs.diseases || {};
                        const cures = dm.cures || {};
                        const allCured = Object.values(cures).length === 4 && Object.values(cures).every(Boolean);

                        setText('statConnected', data.connectedPlayers ?? 0);
                        setValClass('statStarted', gs.gameStarted ? 'true' : 'false', gs.gameStarted ? 'ok' : 'warn');
                        setValClass('statOver', gs.gameOver ? 'true' : 'false', gs.gameOver ? 'bad' : 'ok');
                        setText('statTurn', gs.currentPlayerName || '—');
                        setText('statActions', gs.actionsRemaining ?? '—');
                        setText('statInfRate', gs.infectionRate ?? '—');

                        const epi = gs.epidemicCount ?? null;
                        setValClass('statEpidemic', epi !== null ? epi : '—', epi >= 4 ? 'bad' : epi >= 2 ? 'warn' : 'ok');

                        const cubes = dm.infectionCubesLeft ?? null;
                        setValClass('statCubes', cubes !== null ? cubes : '—', cubes < 20 ? 'bad' : cubes < 40 ? 'warn' : 'ok');

                        const ob = dm.outbreakScore ?? null;
                        setValClass('statOutbreaks', ob !== null ? ob : '—', ob >= 6 ? 'bad' : ob >= 3 ? 'warn' : 'ok');
                    }

                    function setText(id, val) {
                        const el = document.getElementById(id);
                        if (el) el.textContent = val;
                    }

                    function setValClass(id, val, cls) {
                        const el = document.getElementById(id);
                        if (!el) return;
                        el.textContent = val;
                        el.className = 'val ' + (cls || '');
                    }

                    // ── Disease panel
                    function renderDiseases(diseases) {
                        const grid = document.getElementById('diseaseGrid');
                        if (!diseases) { grid.innerHTML = '<div class="empty-msg">No disease data yet.</div>'; return; }
                        const cures = diseases.cures || {};
                        const colors = ['BLUE','YELLOW','BLACK','RED'];
                        grid.innerHTML = colors.map(c => {
                            const cured = !!cures[c];
                            return `<div class="disease-card ${c}">
                                <div class="disease-name">${c}</div>
                                ${cured
                                    ? '<span class="cured-badge">✓ CURED</span>'
                                    : '<span class="not-cured">Not cured</span>'}
                            </div>`;
                        }).join('');

                        // Outbreak meter
                        const meter = document.getElementById('outbreakMeter');
                        const score = diseases.outbreakScore ?? 0;
                        const pips = Array.from({length:8}, (_,i) => {
                            const filled = i < score;
                            const danger = i >= 6;
                            const bg = filled ? (danger ? '#ef4444' : '#f97316') : '#1e3048';
                            return `<div style="width:28px;height:28px;border-radius:6px;background:${bg};border:1px solid #2a4060;display:inline-flex;align-items:center;justify-content:center;font-size:11px;font-weight:800;color:${filled?'#fff':'#3a5a7a'}">${i}</div>`;
                        }).join('');
                        meter.innerHTML = `<div style="display:flex;gap:6px;flex-wrap:wrap;align-items:center">${pips}<span style="margin-left:8px;font-size:12px;color:var(--muted)">${score}/7 (8 = game over)</span></div>`;
                    }

                    // ── Players panel
                    function renderPlayers(players) {
                        const grid = document.getElementById('playersGrid');
                        if (!players || players.length === 0) {
                            grid.innerHTML = '<div class="empty-msg">No players registered yet.</div>';
                            return;
                        }
                        grid.innerHTML = players.map((p, idx) => {
                            const initials = (p.playerName || '?').substring(0,2).toUpperCase();
                            const avatarColors = ['#1d4ed8','#7c3aed','#0f766e','#b45309'];
                            const avatarBg = avatarColors[idx % avatarColors.length];
                            const isActive = p.isCurrentTurn;

                            const readyBadge = p.ready
                                ? '<span class="badge ready">✓ Ready</span>'
                                : '<span class="badge waiting">Waiting</span>';
                            const discardBadge = p.discardingCards
                                ? '<span class="badge discard">Discarding</span>'
                                : '';

                            const cityHtml = p.currentCity
                                ? `<span class="city-tag">${colorDot(p.currentCityColor || '')} ${p.currentCity}</span>`
                                : '<span style="color:var(--muted);font-size:11px">No city</span>';

                            const handCount = p.handCount ?? 0;
                            const hcClass = handCount > 7 ? 'hc-over' : handCount >= 6 ? 'hc-warn' : '';
                            const handHtml = `<span class="hand-count-badge ${hcClass}">
                                🃏 <span class="hc-num">${handCount}</span> card${handCount !== 1 ? 's' : ''}
                                ${handCount > 7 ? '<span style="color:#f87171">▲ over limit</span>' : ''}
                            </span>`;

                            return `<div class="player-card${isActive ? ' active-turn' : ''}">
                                <div class="player-header">
                                    <div class="player-avatar" style="background:${avatarBg}">${initials}</div>
                                    <div>
                                        <div class="player-name">${p.playerName}</div>
                                        <div class="player-role">${p.playerRole || 'No role assigned'}</div>
                                    </div>
                                    ${isActive ? '<div class="turn-indicator">▶ TURN</div>' : ''}
                                </div>
                                <div class="player-body">
                                    <div class="player-meta">
                                        ${readyBadge}${discardBadge}
                                        ${cityHtml}
                                        ${handHtml}
                                    </div>
                                </div>
                            </div>`;
                        }).join('');
                    }

                    // ── Cities panel
                    function renderCities(cities) {
                        allCities = cities || [];
                        const grid = document.getElementById('citiesGrid');
                        if (allCities.length === 0) {
                            grid.innerHTML = '<div class="empty-msg">No map data yet.</div>';
                            return;
                        }

                        const filtered = allCities.filter(city => {
                            const nameMatch = !citySearch || city.name.toLowerCase().includes(citySearch);
                            if (!nameMatch) return false;
                            if (cityFilter === 'ALL') return true;
                            if (cityFilter === 'HOT') {
                                const cubes = city.cubes || {};
                                return Object.values(cubes).some(v => v >= 2);
                            }
                            if (cityFilter === 'RS') return city.hasResearchStation;
                            return city.color === cityFilter;
                        });

                        if (filtered.length === 0) {
                            grid.innerHTML = '<div class="empty-msg">No cities match the current filter.</div>';
                            return;
                        }

                        grid.innerHTML = filtered.map(city => {
                            const cubes = city.cubes || {};
                            const totalCubes = Object.values(cubes).reduce((a,b)=>a+b,0);
                            const cubeHtml = Object.entries(cubes).map(([color, count]) =>
                                `<span class="cube-block ${color}">${colorDot(color)} ${count}</span>`
                            ).join('');
                            const rsHtml = city.hasResearchStation ? '<span class="rs-icon" title="Research Station">🏥</span>' : '';
                            const conns = (city.connectedCities || []).join(', ');
                            const hotStyle = totalCubes >= 3 ? 'border-color:#ef4444;' : totalCubes >= 2 ? 'border-color:#f97316;' : '';

                            return `<div class="city-card${city.hasResearchStation ? ' has-rs' : ''}" style="${hotStyle}">
                                <div class="city-card-top">
                                    ${colorDot(city.color)}
                                    <span class="city-name">${city.name}</span>
                                    ${rsHtml}
                                </div>
                                ${cubeHtml ? `<div class="cubes-row">${cubeHtml}</div>` : ''}
                                <div class="connections">↔ ${conns || 'none'}</div>
                            </div>`;
                        }).join('');
                    }

                    // ── Packet panel
                    function renderPacket(packet) {
                        const grid = document.getElementById('packetGrid');
                        if (!packet) {
                            grid.innerHTML = '<div class="empty-msg">No packets received yet.</div>';
                            return;
                        }
                        grid.innerHTML = Object.entries(packet).map(([k,v]) => {
                            const isObj = typeof v === 'object' && v !== null;
                            return `<div class="field">
                                <div class="label">${k}</div>
                                <div class="value${isObj?' mono':''}">${isObj ? JSON.stringify(v,null,2) : String(v)}</div>
                            </div>`;
                        }).join('');
                    }

                    // ── Main refresh loop
                    async function refresh() {
                        try {
                            const res = await fetch('/state', { cache: 'no-store' });
                            const data = await res.json();
                            const gs = data.gameState || {};

                            updateStats(data);
                            renderDiseases(gs.diseases || null);
                            renderPlayers(gs.players || []);
                            renderCities(gs.cities || []);
                            renderPacket(data.latestPacket || null);

                            document.getElementById('pulse').style.background = 'var(--green)';
                        } catch (err) {
                            document.getElementById('pulse').style.background = 'var(--red)';
                            console.error('Refresh error:', err);
                        }
                    }

                    refresh();
                    setInterval(refresh, 500);

                })();
                </script>
                </body>
                </html>
                """;
    }
}