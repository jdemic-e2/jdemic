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
import java.util.Set;
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

        executor.submit(() -> {
            Set<Integer> activePorts = queryActivePorts();
            if (activePorts.isEmpty()) {
                Platform.runLater(() -> {
                    serverListBox.getChildren().clear();
                    Label none = TextUtil.createText("NO ACTIVE SERVERS", "hkmodular", 0.022, "#888888", root);
                    serverListBox.getChildren().add(none);
                });
                return;
            }
            // Check game-started status for each active port in parallel
            ExecutorService statusExec = Executors.newFixedThreadPool(activePorts.size());
            for (int port : activePorts) {
                final int p = port;
                statusExec.submit(() -> {
                    boolean gameStarted = queryGameStarted(SCAN_HOST, p);
                    if (!gameStarted) {
                        Platform.runLater(() -> {
                            serverListBox.getChildren().clear(); // remove "SCANNING..."
                            serverListBox.getChildren().add(createServerRow(p, true));
                            // re-sort by port
                            serverListBox.getChildren().sort((a, b) -> {
                                Integer pa = (a.getUserData() instanceof Integer) ? (Integer) a.getUserData() : 0;
                                Integer pb = (b.getUserData() instanceof Integer) ? (Integer) b.getUserData() : 0;
                                return pa.compareTo(pb);
                            });
                        });
                    }
                });
            }
            statusExec.shutdown();
        });
        executor.shutdown();
    }

    private Set<Integer> queryActivePorts() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(SCAN_HOST, DEFAULT_ORCHESTRATOR_PORT), 500);
            socket.setSoTimeout(1000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("LIST");
            String response = in.readLine();
            if (response == null || !response.startsWith("PORTS:")) {
                return java.util.Collections.emptySet();
            }
            String portsPart = response.substring("PORTS:".length()).trim();
            if (portsPart.isEmpty()) {
                return java.util.Collections.emptySet();
            }
            Set<Integer> result = new java.util.HashSet<>();
            for (String p : portsPart.split(",")) {
                try { result.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
            }
            return result;
        } catch (Exception ex) {
            System.err.println("[LobbyScene] Orchestrator unreachable during scan: " + ex.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    private void placeServerRow(int port, boolean reachable) {
        // Drop the "SCANNING..." placeholder once the first real row arrives.
        if (serverListBox.getChildren().size() == 1
                && serverListBox.getChildren().get(0) instanceof Label first
                && "SCANNING...".equals(first.getText())) {
            serverListBox.getChildren().clear();
        }

        StackPane row = createServerRow(port, reachable);
        // Keep rows ordered by port number even though probes finish out of order.
        int insertAt = 0;
        for (javafx.scene.Node node : serverListBox.getChildren()) {
            Integer rowPort = (node.getUserData() instanceof Integer) ? (Integer) node.getUserData() : null;
            if (rowPort == null || rowPort < port) {
                insertAt++;
            } else {
                break;
            }
        }
        serverListBox.getChildren().add(insertAt, row);
    }

    private StackPane createServerRow(int port, boolean reachable) {
        StackPane row = new StackPane();
        row.setUserData(port);
        row.maxWidthProperty().bind(root.widthProperty().multiply(0.55));
        row.setStyle(
                "-fx-background-color: rgba(0,0,0,0.65);" +
                "-fx-border-color: " + (reachable ? "#00d9ff" : "#444444") + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;"
        );

        Label portLabel = TextUtil.createText("SERVER " + port, "hkmodular", 0.022, "#00d9ff", root);
        Label statusLabel = TextUtil.createText(reachable ? "JOINABLE" : "OFFLINE", "hkmodular", 0.02,
                reachable ? "#cfc900" : "#888888", root);

        HBox inner = new HBox(12, portLabel, spacer(), statusLabel);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(8, 12, 8, 12));

        if (reachable) {
            ButtonsUtil joinBtn = new ButtonsUtil("JOIN", "#00d1ff", "black", "#00d4ff", "#00d4ff",
                    2, 10, 10, 0.10, 0.05, 0.018, root);
            joinBtn.setOnMouseClicked(e -> {
                String nickname = requireNickname();
                if (nickname == null) {
                    return;
                }
                joinBtn.setDisable(true);
                new Thread(() -> {
                    boolean ok = connectHostAndOpenWaitingRoom(SCAN_HOST, port, nickname, null, false);
                    if (!ok) {
                        Platform.runLater(() -> joinBtn.setDisable(false));
                    }
                }, "jdemic-join-server-" + port).start();
            });
            inner.getChildren().add(joinBtn);
        }

        row.getChildren().add(inner);
        return row;
    }

    private boolean queryGameStarted(String host, int gamePort) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, gamePort + 500), 500);
            socket.setSoTimeout(500);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("STATUS");
            String response = in.readLine();
            return response != null && response.equals("gameStarted:true");
        } catch (Exception ex) {
            return false;
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
