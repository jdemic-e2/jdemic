package jdemic.Scenes.Lobby;

import jdemic.GameLogic.GameClient;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.Scenes.Settings.SettingsManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.SafeResourceLoader;
import jdemic.ui.TextUtil;

import java.util.function.UnaryOperator;

public class JoinCodeScene {
    private final StackPane root;
    private final Stage stage;
    private final String presetNickname;
    private Label errorLabel;
    private GameClient gameClient;

    public JoinCodeScene(Stage stage) {
        this(stage, null);
    }

    public JoinCodeScene(Stage stage, String presetNickname) {
        this.stage = stage;
        this.presetNickname = presetNickname;
        root = new StackPane();
        setupBackground();
        setupUI();
    }

    private void setupBackground() {
        java.net.URL bgUrl = getClass().getResource("/background.png");
        if (bgUrl == null) {
            System.err.println("[JoinCodeScene] Missing resource: /background.png");
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
        title.translateYProperty().bind(root.heightProperty().multiply(0.05));
        GlowUtil.applyGlow(title, "#cfc900", 7);

        Label accessLabel = TextUtil.createText("ENTER IP ADDRESS", "hkmodular", 0.03, "#ff0000", root);
        accessLabel.setTextAlignment(TextAlignment.CENTER);

        TextField nicknameField = new TextField(presetNickname == null ? savedPlayerName() : normalizeNickname(presetNickname));
        nicknameField.setTextFormatter(new TextFormatter<>(nicknameFilter()));
        nicknameField.setMaxWidth(300);
        nicknameField.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: #cfc900; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'hkmodular'; -fx-font-size: 18;");
        nicknameField.setVisible(presetNickname == null);
        nicknameField.setManaged(presetNickname == null);

        Label nameLabel = TextUtil.createText("NICKNAME:", "hkmodular", 0.026, "#00b5d4", root);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        GlowUtil.applyGlow(nameLabel, "#00b5d4", 6);
        nameLabel.setVisible(presetNickname == null);
        nameLabel.setManaged(presetNickname == null);

        TextField codeField = new TextField();
        codeField.setMaxWidth(500);
        codeField.setPrefHeight(75);
        codeField.setPromptText("192.168.1.100");
        codeField.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: #cfc900; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'hkmodular'; -fx-font-size: 18;");

        ButtonsUtil cancelBtn = new ButtonsUtil("CANCEL", "#ff0000", "black", "#ff0000", "#ff0000", 2, 15, 15, 0.2, 0.1, 0.02, root);
        cancelBtn.setOnMouseClicked(e -> SceneManager.switchScene("HOST_SCREEN"));

        ButtonsUtil joinBtn = new ButtonsUtil("JOIN", "#00d1ff", "black", "#00d4ff", "#00d4ff", 2, 15, 15, 0.2, 0.1, 0.02, root);
        joinBtn.setOnMouseClicked(e -> {
            String code = codeField.getText().trim();
            String nickname = nicknameField.getText().trim();
            if (nickname.isEmpty()) {
                errorLabel.setText("ENTER NICKNAME");
                errorLabel.setVisible(true);
                return;
            }
            if (code.isEmpty()) {
                errorLabel.setText("ENTER IP ADDRESS");
                errorLabel.setVisible(true);
                return;
            }
            joinBtn.setDisable(true);
            errorLabel.setVisible(false);
            savePlayerName(nickname);
            connectToLobby(code, nickname, joinBtn);
        });

        errorLabel = TextUtil.createText("", "hkmodular", 0.025, "#ff0000", root);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setVisible(false);

        VBox inputBox = new VBox(10, nameLabel, nicknameField, accessLabel, codeField);
        inputBox.setAlignment(Pos.CENTER);

        HBox buttonBox = new HBox(20, joinBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);

        VBox box = new VBox(30, inputBox, errorLabel, buttonBox);
        box.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(box, Pos.TOP_CENTER);
        box.translateYProperty().bind(root.heightProperty().multiply(0.25));

        root.getChildren().addAll(title, box);
    }

    public StackPane getRoot() {
        return root;
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
            return "Newbie";
        }
        String normalized = nickname.replaceAll("[^a-zA-Z0-9]", "");
        if (normalized.length() > 16) {
            normalized = normalized.substring(0, 16);
        }
        return normalized.isBlank() ? "Newbie" : normalized;
    }

    private void connectToLobby(String code, String nickname, ButtonsUtil joinBtn) {
        new Thread(() -> {
            try {
                String targetIp = code;
                int targetPort = 9001;

                if (code.contains(":")) {
                    String[] parts = code.split(":");
                    targetIp = parts[0].trim();
                    targetPort = Integer.parseInt(parts[1].trim());
                }

                GameClient client = new GameClient();
                if (!connectAndRegister(client, targetIp, targetPort, nickname)) {
                    client.disconnect();
                    showJoinError(joinBtn);
                    return;
                }

                gameClient = client;

                String roomCode = targetIp + ":" + targetPort;
                Platform.runLater(() ->
                        SceneManager.setRoot(new WaitingRoomScene(stage, nickname, roomCode, gameClient).getRoot())
                );
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                showJoinError(joinBtn);
            } catch (Exception ex) {
                showJoinError(joinBtn);
            }
        }, "jdemic-join-server").start();
    }

    private boolean connectAndRegister(GameClient client, String host, int port, String playerName) throws InterruptedException {
        return LobbyRegistrationHelper.connectAndRegister(client, host, port, playerName);
    }

    private void showJoinError(ButtonsUtil joinBtn) {
        Platform.runLater(() -> {
            errorLabel.setText("FAILED TO CONNECT");
            errorLabel.setVisible(true);
            joinBtn.setDisable(false);
        });
    }
}
