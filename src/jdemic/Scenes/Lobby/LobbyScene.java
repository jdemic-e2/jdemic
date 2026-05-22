package jdemic.Scenes.Lobby;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.application.Platform;
import jdemic.DedicatedServer.network.core.DedicatedServerConfig;
import jdemic.DedicatedServer.network.core.JdemicNetworkServer;
import jdemic.GameLogic.GameClient;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LobbyScene {
    private final StackPane root;
    private final Stage stage;
    private static final int DEFAULT_GAME_PORT = 9000;
    private static final int DEFAULT_ORCHESTRATOR_PORT = 8090;

    public LobbyScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        setupBackground();
        setupUI();
    }

    private void setupBackground() {
        java.net.URL bgUrl = getClass().getResource("/background.png");
        if (bgUrl == null) {
            System.err.println("[LobbyScene] Missing resource: /background.png");
            return;
        }
        ImageView background = new ImageView(new Image(bgUrl.toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    private void setupUI() {
        // Sadece arka planın üzerinde üstte konumlanan "LOBBY" başlığı gösterilir.
        Label title = TextUtil.createText("HOST", "hkmodular", 0.05, "#cfc900", root);
        title.setTextAlignment(TextAlignment.CENTER);
        StackPane.setAlignment(title, javafx.geometry.Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.05));
        GlowUtil.applyGlow(title, "#cfc900", 7);

        TextField nicknameField = new TextField("Newbie");
        nicknameField.setMaxWidth(300);
        nicknameField.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: #cfc900; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'hkmodular'; -fx-font-size: 18;");

        Label nameLabel = TextUtil.createText("NICKNAME:", "hkmodular", 0.026, "#00b5d4", root);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        GlowUtil.applyGlow(nameLabel, "#00b5d4", 6);

        Label errorLabel = TextUtil.createText("ENTER NICKNAME", "hkmodular", 0.02, "#ff2d2d", root);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setVisible(false);

        ButtonsUtil hostBtn = new ButtonsUtil("CREATE SERVER", "#ff0000", "black", "#ff0000", "#ff0000", 2, 15, 15, 0.40, 0.10, 0.03, root);
        hostBtn.setOnMouseClicked(e -> {
            String nickname = nicknameField.getText().trim();
            if (nickname.isEmpty()) {
                errorLabel.setText("ENTER NICKNAME");
                errorLabel.setVisible(true);
            } else {
                errorLabel.setVisible(false);
                hostBtn.setDisable(true);
                startLocalServerAndEnterLobby(nickname, hostBtn, errorLabel);
            }
        });

        ButtonsUtil joinBtn = new ButtonsUtil("JOIN BY IP", "#00d1ff", "black", "#00d4ff", "#00d4ff", 2, 15, 15, 0.40, 0.10, 0.03, root);
        joinBtn.setOnMouseClicked(e -> {
            String nickname = nicknameField.getText().trim();
            if (nickname.isEmpty()) {
                errorLabel.setText("ENTER NICKNAME");
                errorLabel.setVisible(true);
                return;
            }

            errorLabel.setVisible(false);
            stage.getScene().setRoot(new JoinCodeScene(stage, nickname).getRoot());
        });

        VBox centerBox = new VBox(18, nameLabel, nicknameField, errorLabel, hostBtn, joinBtn);
        centerBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        StackPane.setAlignment(centerBox, javafx.geometry.Pos.TOP_CENTER);
        centerBox.translateYProperty().bind(root.heightProperty().multiply(0.25));

        ButtonsUtil backBtn = new ButtonsUtil("BACK", "#ff0000", "black", "#ff0000", "#ff0000", 2, 12, 12, 0.15, 0.06, 0.02, root);
        StackPane.setAlignment(backBtn, javafx.geometry.Pos.BOTTOM_CENTER);
        backBtn.translateYProperty().bind(root.heightProperty().multiply(-0.08));
        backBtn.setOnMouseClicked(e -> SceneManager.switchScene("MAIN_MENU"));

        root.getChildren().addAll(title, centerBox, backBtn);
    }

    public StackPane getRoot() {
        return root;
    }

    private void startLocalServerAndEnterLobby(String nickname, ButtonsUtil hostBtn, Label errorLabel) {
        new Thread(() -> {
            Integer orchestratedPort = requestServerFromOrchestrator();
            if (orchestratedPort != null) {
                connectHostAndOpenWaitingRoom("79.118.90.140", orchestratedPort, nickname, hostBtn, errorLabel, false);
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

            if (!connectHostAndOpenWaitingRoom("79.118.90.140", DEFAULT_GAME_PORT, nickname, hostBtn, errorLabel, true)) {
                JdemicNetworkServer.shutdown();
            }
        }, "jdemic-create-server").start();
    }

    private Integer requestServerFromOrchestrator() {
        try (Socket orchestratorSocket = new Socket()) {
            orchestratorSocket.connect(new InetSocketAddress("79.118.90.140", DEFAULT_ORCHESTRATOR_PORT), 500);
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
            ButtonsUtil hostBtn,
            Label errorLabel,
            boolean ownsServer
    ) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            showHostError("FAILED TO CONNECT TO SERVER", hostBtn, errorLabel);
            return false;
        }

        GameClient hostClient = connectAndRegister(host, port, nickname);
        if (hostClient == null) {
            showHostError("FAILED TO CONNECT TO SERVER", hostBtn, errorLabel);
            return false;
        }

        String roomCode = getLocalHostAddress() + ":" + port;
        Platform.runLater(() ->
                stage.getScene().setRoot(new WaitingRoomScene(stage, nickname, roomCode, hostClient, ownsServer).getRoot())
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

    private void showHostError(String message, ButtonsUtil hostBtn, Label errorLabel) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            hostBtn.setDisable(false);
        });
    }
}
