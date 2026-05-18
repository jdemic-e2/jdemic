package jdemic.Scenes.Lobby;

import jdemic.GameLogic.GameClient;
import jdemic.DedicatedServer.network.core.DedicatedServerConfig;
import jdemic.DedicatedServer.network.core.JdemicNetworkServer;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class HostGameScene {
    private final StackPane root;
    private final Stage stage;
    private final String nickname;
    private final String hostCode;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public HostGameScene(Stage stage, String nickname) {
        this.stage = stage;
        this.root = new StackPane();
        this.nickname = nickname == null || nickname.isBlank() ? "PLAYER" : nickname.toUpperCase();
        this.hostCode = generateHostCode();
        setupBackground();
        setupUI();
    }

    public StackPane getRoot() {
        return root;
    }

    private void setupBackground() {
        java.net.URL bgUrl = getClass().getResource("/background.png");
        if (bgUrl == null) {
            System.err.println("[HostGameScene] Missing resource: /background.png");
            return;
        }
        ImageView background = new ImageView(new Image(bgUrl.toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    private void setupUI() {
        Label title = TextUtil.createText("LOBBY", "hkmodular", 0.06, "#d1d412", root);
        GlowUtil.applyGlow(title, "#d1d412", 15);

        Label subtitle = TextUtil.createText("HOST GAME", "hkmodular", 0.034, "#ff2d2d", root);
        GlowUtil.applyGlow(subtitle, "#ff2d2d", 10);

        VBox headerBox = new VBox(title, subtitle);
        headerBox.setAlignment(Pos.TOP_CENTER);
        headerBox.spacingProperty().bind(root.heightProperty().multiply(0.018));
        StackPane.setAlignment(headerBox, Pos.TOP_CENTER);
        headerBox.translateYProperty().bind(root.heightProperty().multiply(0.05));

        Label codeText = TextUtil.createText("CODE:", "hkmodular", 0.038, "#00d9ff", root);

        StackPane codeBox = new StackPane();
        codeBox.prefWidthProperty().bind(root.widthProperty().multiply(0.34));
        codeBox.prefHeightProperty().bind(root.heightProperty().multiply(0.10));
        codeBox.setStyle(
            "-fx-background-color: black;" +
            "-fx-border-color: #00b5d4;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;"
        );
        GlowUtil.applyGlow(codeBox, "#00b5d4", 12);

        Label codeValue = TextUtil.createText(hostCode, "hkmodular", 0.042, "#ffffff", root);
        GlowUtil.applyGlow(codeValue, "#ffffff", 8);
        codeBox.getChildren().add(codeValue);

        //Error message
        Label errorLabel = TextUtil.createText("", "hkmodular", 0.025, "#ff2d2d", root);
        errorLabel.setVisible(false);

        ButtonsUtil copyBtn = new ButtonsUtil("COPY", "#00d9ff", "black", "#00d9ff", "#00d9ff", 2, 12, 12, 0.13, 0.07, 0.020, root);
        ButtonsUtil backBtn = new ButtonsUtil("BACK", "#ff2d2d", "black", "#ff2d2d", "#ff2d2d", 2, 12, 12, 0.13, 0.07, 0.020, root);
        ButtonsUtil hostBtn = new ButtonsUtil("HOST", "#00d9ff", "black", "#00d9ff", "#00d9ff", 2, 12, 12, 0.13, 0.07, 0.020, root);

        copyBtn.setOnMouseClicked(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(hostCode);
            Clipboard.getSystemClipboard().setContent(cc);
        });

        backBtn.setOnMouseClicked(e -> SceneManager.switchScene("LOBBY"));
        
        hostBtn.setOnMouseClicked(e -> {
            hostBtn.setDisable(true); // stop multiple clicks
            errorLabel.setVisible(false);

            new Thread(() -> {
                try {
                    Integer orchestratedPort = requestServerFromOrchestrator();
                    if (orchestratedPort != null) {
                        connectHostAndOpenWaitingRoom(orchestratedPort);
                        return;
                    }

                    int fallbackPort = 9000;
                    DedicatedServerConfig embeddedConfig = new DedicatedServerConfig(
                            fallbackPort,
                            true,
                            DedicatedServerConfig.DEFAULT_STATUS_HOST,
                            DedicatedServerConfig.DEFAULT_STATUS_PORT,
                            false
                    );
                    boolean serverStarted = JdemicNetworkServer.startServer(embeddedConfig);
                    if (!serverStarted) {
                        showHostError("PORT 9000 IN USE! START FAILED.", hostBtn, errorLabel);
                        return;
                    }

                    connectHostAndOpenWaitingRoom(fallbackPort);
                } catch (Exception ex) {
                    System.err.println("[HostGameScene] Failed to host game: " + ex.getMessage());
                    showHostError("CRITICAL ERROR: ALL SYSTEMS OFFLINE!", hostBtn, errorLabel);
                }
            }, "jdemic-host-game").start();
        });
        //zi mi te rog ca e ok si ca merge))))
        HBox bottomRow = new HBox(backBtn, hostBtn);
        bottomRow.setAlignment(Pos.CENTER);
        bottomRow.spacingProperty().bind(root.widthProperty().multiply(0.16));

        VBox content = new VBox(codeText, codeBox, copyBtn, errorLabel, bottomRow);
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(false);
        content.spacingProperty().bind(root.heightProperty().multiply(0.04));
        content.paddingProperty().bind(Bindings.createObjectBinding(
            () -> new Insets(root.getHeight() * 0.08, 0, root.getHeight() * 0.04, 0),
            root.heightProperty()
        ));

        StackPane.setAlignment(content, Pos.TOP_CENTER);
        content.translateYProperty().bind(root.heightProperty().multiply(0.24));

        root.getChildren().addAll(
            headerBox,
            content
        );
    }

    private String generateHostCode() {
        try {
            //Only generate the base IP here. The port is added dynamically after clicking HOST.
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1"; //Fallback IP
        }
    }

    private Integer requestServerFromOrchestrator() {
        try (Socket orchestratorSocket = new Socket()) {
            orchestratorSocket.connect(new InetSocketAddress("localhost", 8080), 500);
            orchestratorSocket.setSoTimeout(1000);

            java.io.PrintWriter out = new java.io.PrintWriter(orchestratorSocket.getOutputStream(), true);
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(orchestratorSocket.getInputStream()));

            out.println("HOST");
            String response = in.readLine();
            if (response == null || !response.startsWith("SUCCESS:")) {
                System.err.println("[HostGameScene] Orchestrator unavailable or invalid response: " + response);
                return null;
            }

            return Integer.parseInt(response.split(":")[1]);
        } catch (Exception ex) {
            System.err.println("[HostGameScene] Orchestrator unavailable: " + ex.getMessage());
            return null;
        }
    }

    private void connectHostAndOpenWaitingRoom(int port) throws InterruptedException {
        Thread.sleep(250);

        GameClient hostClient = new GameClient();
        hostClient.connectToServer("localhost", port);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("playerName", nickname);
        hostClient.sendPacket(new Packet(PacketType.CONNECT, payload));

        Platform.runLater(() ->
                stage.getScene().setRoot(new WaitingRoomScene(stage, nickname, hostCode + ":" + port, hostClient).getRoot())
        );
    }

    private void showHostError(String message, ButtonsUtil hostBtn, Label errorLabel) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            hostBtn.setDisable(false);
        });
    }
}
