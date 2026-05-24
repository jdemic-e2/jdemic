package jdemic.Scenes.Lobby;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.application.Platform;
import jdemic.DedicatedServer.network.core.DedicatedServerConfig;
import jdemic.DedicatedServer.network.core.JdemicNetworkServer;
import jdemic.GameLogic.GameClient;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.Scenes.Settings.SettingsManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.SafeResourceLoader;
import jdemic.ui.TextUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public class LobbyScene {
    private final StackPane root;
    private final Stage stage;
    private static final int DEFAULT_GAME_PORT = 9000;
    private static final int DEFAULT_ORCHESTRATOR_PORT = 8090;
    private static final String SCAN_HOST = "79.118.90.140";
    private static final int SCAN_PORT_MIN = 9001;
    private static final int SCAN_PORT_MAX = 9010;
    private TextField nicknameField;
    private Label errorLabel;
    private VBox serverListBox;
    private final AtomicReference<ExecutorService> scanExecutor = new AtomicReference<>();

    public LobbyScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        setupBackground();
        setupUI();
        // Kick off an initial scan so the list is populated when the scene opens.
        Platform.runLater(this::scanServers);
    }

    private void setupBackground() {
        java.net.URL bgUrl = getClass().getResource("/background.png");
        if (bgUrl == null) {
            System.err.println("[LobbyScene] Missing resource: /background.png");
            return;
        }
        ImageView background = new ImageView(SafeResourceLoader.loadImage(bgUrl));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    private void setupUI() {
        Label title = TextUtil.createText("LOBBY", "hkmodular", 0.05, "#cfc900", root);
        title.setTextAlignment(TextAlignment.CENTER);
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.04));
        GlowUtil.applyGlow(title, "#cfc900", 7);

        Label nameLabel = TextUtil.createText("NICKNAME:", "hkmodular", 0.022, "#00b5d4", root);
        GlowUtil.applyGlow(nameLabel, "#00b5d4", 6);

        nicknameField = new TextField(savedPlayerName());
        nicknameField.setTextFormatter(new TextFormatter<>(nicknameFilter()));
        nicknameField.setMaxWidth(280);
        nicknameField.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: #cfc900; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'hkmodular'; -fx-font-size: 16;");

        HBox nicknameRow = new HBox(12, nameLabel, nicknameField);
        nicknameRow.setAlignment(Pos.CENTER);

        Label listTitle = TextUtil.createText("SERVERS", "hkmodular", 0.03, "#cfc900", root);
        GlowUtil.applyGlow(listTitle, "#cfc900", 6);

        ButtonsUtil refreshBtn = new ButtonsUtil("REFRESH", "#00d1ff", "black", "#00d4ff", "#00d4ff", 2, 12, 12, 0.14, 0.06, 0.020, root);
        refreshBtn.setOnMouseClicked(e -> scanServers());

        HBox listHeader = new HBox(listTitle, spacer(), refreshBtn);
        listHeader.setAlignment(Pos.CENTER);
        listHeader.maxWidthProperty().bind(root.widthProperty().multiply(0.60));

        serverListBox = new VBox(8);
        serverListBox.setAlignment(Pos.TOP_CENTER);
        serverListBox.setPadding(new Insets(8));

        ScrollPane serverScroll = new ScrollPane(serverListBox);
        serverScroll.setFitToWidth(true);
        serverScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        serverScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        serverScroll.maxWidthProperty().bind(root.widthProperty().multiply(0.60));
        serverScroll.prefHeightProperty().bind(root.heightProperty().multiply(0.36));
        serverScroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: rgba(0,0,0,0.65);" +
                "-fx-border-color: #00b5d4;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );
        GlowUtil.applyGlow(serverScroll, "#00b5d4", 8);

        errorLabel = TextUtil.createText("", "hkmodular", 0.022, "#ff2d2d", root);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setVisible(false);

        ButtonsUtil hostBtn = new ButtonsUtil("CREATE SERVER", "#ff0000", "black", "#ff0000", "#ff0000", 2, 15, 15, 0.30, 0.08, 0.024, root);
        hostBtn.setOnMouseClicked(e -> {
            String nickname = requireNickname();
            if (nickname == null) {
                return;
            }
            hostBtn.setDisable(true);
            startLocalServerAndEnterLobby(nickname, hostBtn);
        });

        ButtonsUtil joinIpBtn = new ButtonsUtil("JOIN BY IP", "#00d1ff", "black", "#00d4ff", "#00d4ff", 2, 15, 15, 0.30, 0.08, 0.024, root);
        joinIpBtn.setOnMouseClicked(e -> {
            String nickname = requireNickname();
            if (nickname == null) {
                return;
            }
            SceneManager.setRoot(new JoinCodeScene(stage, nickname).getRoot());
        });

        HBox actionRow = new HBox(20, hostBtn, joinIpBtn);
        actionRow.setAlignment(Pos.CENTER);

        VBox centerBox = new VBox(14, nicknameRow, listHeader, serverScroll, errorLabel, actionRow);
        centerBox.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(centerBox, Pos.TOP_CENTER);
        centerBox.translateYProperty().bind(root.heightProperty().multiply(0.15));

        ButtonsUtil backBtn = new ButtonsUtil("BACK", "#ff0000", "black", "#ff0000", "#ff0000", 2, 12, 12, 0.15, 0.06, 0.02, root);
        StackPane.setAlignment(backBtn, Pos.BOTTOM_CENTER);
        backBtn.translateYProperty().bind(root.heightProperty().multiply(-0.04));
        backBtn.setOnMouseClicked(e -> {
            shutdownScanExecutor();
            SceneManager.switchScene("MAIN_MENU");
        });

        root.getChildren().addAll(title, centerBox, backBtn);
    }

    public StackPane getRoot() {
        return root;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private void savePlayerName(String nickname) {
        SettingsManager settingsManager = SettingsManager.getInstance();
        settingsManager.playerNameProperty().set(nickname);
        settingsManager.saveSettings();
    }

    private String savedPlayerName() {
        String savedName = SettingsManager.getInstance().playerNameProperty().get();
        return normalizeNickname(savedName);
    }

    private UnaryOperator<TextFormatter.Change> nicknameFilter() {
        return change -> {
            String text = change.getControlNewText();
            return text.matches("[a-zA-Z0-9]*") && text.length() <= 16 ? change : null;
        };
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) {
            return "Player";
        }
        String normalized = nickname.replaceAll("[^a-zA-Z0-9]", "");
        if (normalized.length() > 16) {
            normalized = normalized.substring(0, 16);
        }
        return normalized.isBlank() ? "Player" : normalized;
    }

    private String requireNickname() {
        String nickname = nicknameField.getText().trim();
        if (nickname.isEmpty()) {
            errorLabel.setText("ENTER NICKNAME");
            errorLabel.setVisible(true);
            return null;
        }
        errorLabel.setVisible(false);
        savePlayerName(nickname);
        return nickname;
    }

    private void scanServers() {
        shutdownScanExecutor();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        scanExecutor.set(executor);

        serverListBox.getChildren().clear();
        Label scanning = TextUtil.createText("SCANNING...", "hkmodular", 0.022, "#00b5d4", root);
        serverListBox.getChildren().add(scanning);

        for (int port = SCAN_PORT_MIN; port <= SCAN_PORT_MAX; port++) {
            final int p = port;
            executor.submit(() -> {
                boolean reachable = probeTcp(SCAN_HOST, p);
                if (reachable) {
                    int[] status = queryServerStatus(SCAN_HOST, p);
                    boolean gameStarted = status[0] == 1;
                    int players = status[1];
                    int maxPlayers = status[2];
                    if (!gameStarted && players > 0) {
                        Platform.runLater(() -> addSortedRow(createServerRow(p, players, maxPlayers)));
                    }
                }
            });
        }
        executor.submit(() -> Platform.runLater(this::showNoServersIfEmpty));
        executor.shutdown();
    }

    private boolean probeTcp(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private StackPane createServerRow(int port, int players, int maxPlayers) {
        StackPane row = new StackPane();
        row.setUserData(port);
        row.maxWidthProperty().bind(root.widthProperty().multiply(0.55));
        row.setStyle(
                "-fx-background-color: rgba(0,0,0,0.65);" +
                "-fx-border-color: #00d9ff;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;"
        );

        Label portLabel = TextUtil.createText("SERVER " + port, "hkmodular", 0.022, "#00d9ff", root);
        Label statusLabel = TextUtil.createText(players + "/" + maxPlayers, "hkmodular", 0.02, "#cfc900", root);

        ButtonsUtil joinBtn = new ButtonsUtil("JOIN", "#00d1ff", "black", "#00d4ff", "#00d4ff",
                2, 10, 10, 0.10, 0.05, 0.018, root);
        joinBtn.setOnMouseClicked(e -> {
            String nickname = requireNickname();
            if (nickname == null) return;
            joinBtn.setDisable(true);
            new Thread(() -> {
                boolean ok = connectHostAndOpenWaitingRoom(SCAN_HOST, port, nickname, null, false);
                if (!ok) Platform.runLater(() -> joinBtn.setDisable(false));
            }, "jdemic-join-server-" + port).start();
        });

        HBox inner = new HBox(12, portLabel, spacer(), statusLabel, joinBtn);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(8, 12, 8, 12));
        row.getChildren().add(inner);
        return row;
    }

    private void addSortedRow(StackPane row) {
        // Remove "SCANNING..." placeholder on first real result
        if (serverListBox.getChildren().size() == 1
                && serverListBox.getChildren().get(0) instanceof Label lbl
                && "SCANNING...".equals(lbl.getText())) {
            serverListBox.getChildren().clear();
        }
        int port = (int) row.getUserData();
        int insertAt = 0;
        for (javafx.scene.Node node : serverListBox.getChildren()) {
            if (node.getUserData() instanceof Integer p && p < port) insertAt++;
            else break;
        }
        serverListBox.getChildren().add(insertAt, row);
    }

    private void showNoServersIfEmpty() {
        if (serverListBox.getChildren().size() == 1
                && serverListBox.getChildren().get(0) instanceof Label lbl
                && "SCANNING...".equals(lbl.getText())) {
            serverListBox.getChildren().clear();
            serverListBox.getChildren().add(
                    TextUtil.createText("NO ACTIVE SERVERS", "hkmodular", 0.022, "#888888", root));
        }
    }

    // Returns int[]{gameStarted (0/1), playerCount, maxPlayers}
    // Falls back to {0, 1, 4} if STATUS socket unreachable (old server without STATUS support)
    private int[] queryServerStatus(String host, int gamePort) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, gamePort + 1000), 500);
            socket.setSoTimeout(500);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("STATUS");
            String response = in.readLine();
            if (response == null) return new int[]{0, 1, 4};
            // Expected: "gameStarted:false,players:2,maxPlayers:4"
            int gameStarted = response.contains("gameStarted:true") ? 1 : 0;
            int players = 1;
            int maxPlayers = 4;
            for (String part : response.split(",")) {
                if (part.startsWith("players:")) {
                    try { players = Integer.parseInt(part.substring(8)); } catch (NumberFormatException ignored) {}
                } else if (part.startsWith("maxPlayers:")) {
                    try { maxPlayers = Integer.parseInt(part.substring(11)); } catch (NumberFormatException ignored) {}
                }
            }
            return new int[]{gameStarted, players, maxPlayers};
        } catch (Exception ex) {
            return new int[]{0, 1, 4}; // assume joinable if STATUS port unreachable
        }
    }

    private void shutdownScanExecutor() {
        ExecutorService existing = scanExecutor.getAndSet(null);
        if (existing != null) {
            existing.shutdownNow();
        }
    }

    private void startLocalServerAndEnterLobby(String nickname, ButtonsUtil hostBtn) {
        new Thread(() -> {
            Integer orchestratedPort = requestServerFromOrchestrator();
            if (orchestratedPort != null) {
                connectHostAndOpenWaitingRoom(SCAN_HOST, orchestratedPort, nickname, hostBtn, false);
                return;
            }

            DedicatedServerConfig config = new DedicatedServerConfig(
                    DEFAULT_GAME_PORT,
                    true,
                    DedicatedServerConfig.DEFAULT_STATUS_HOST,
                    DedicatedServerConfig.DEFAULT_STATUS_PORT,
                    false
            );

            boolean serverStarted = JdemicNetworkServer.startServer(config);
            if (!serverStarted) {
                Platform.runLater(() -> {
                    errorLabel.setText("PORT 9000 IN USE");
                    errorLabel.setVisible(true);
                    hostBtn.setDisable(false);
                });
                return;
            }

            if (!connectHostAndOpenWaitingRoom(SCAN_HOST, DEFAULT_GAME_PORT, nickname, hostBtn, true)) {
                JdemicNetworkServer.shutdown();
            }
        }, "jdemic-create-server").start();
    }

    private Integer requestServerFromOrchestrator() {
        try (Socket orchestratorSocket = new Socket()) {
            orchestratorSocket.connect(new InetSocketAddress(SCAN_HOST, DEFAULT_ORCHESTRATOR_PORT), 500);
            orchestratorSocket.setSoTimeout(1000);

            PrintWriter out = new PrintWriter(orchestratorSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(orchestratorSocket.getInputStream()));

            out.println("HOST");
            String response = in.readLine();
            if (response == null || !response.startsWith("SUCCESS:")) {
                System.err.println("[LobbyScene] Orchestrator unavailable or invalid response: " + response);
                return null;
            }

            return Integer.parseInt(response.split(":")[1]);
        } catch (Exception ex) {
            System.err.println("[LobbyScene] Orchestrator unavailable: " + ex.getMessage());
            return null;
        }
    }

    private boolean connectHostAndOpenWaitingRoom(
            String host,
            int port,
            String nickname,
            ButtonsUtil triggeringBtn,
            boolean ownsServer
    ) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            showError("FAILED TO CONNECT TO SERVER", triggeringBtn);
            return false;
        }

        GameClient hostClient = connectAndRegister(host, port, nickname);
        if (hostClient == null) {
            showError("FAILED TO CONNECT TO SERVER", triggeringBtn);
            return false;
        }

        String roomCode = getLocalHostAddress() + ":" + port;
        Platform.runLater(() ->
                SceneManager.setRoot(new WaitingRoomScene(stage, nickname, roomCode, hostClient, ownsServer).getRoot())
        );
        return true;
    }

    private GameClient connectAndRegister(String host, int port, String nickname) {
        GameClient client = new GameClient();
        try {
            if (!LobbyRegistrationHelper.connectAndRegister(client, host, port, nickname)) {
                client.disconnect();
                return null;
            }
            return client;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            client.disconnect();
            return null;
        }
    }

    private String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private void showError(String message, ButtonsUtil triggeringBtn) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            if (triggeringBtn != null) {
                triggeringBtn.setDisable(false);
            }
        });
    }
}
