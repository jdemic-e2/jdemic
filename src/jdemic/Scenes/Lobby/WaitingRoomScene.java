package jdemic.Scenes.Lobby;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import jdemic.GameLogic.GameClient;
import jdemic.Scenes.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class WaitingRoomScene {
    private final StackPane root;
    private final Stage stage;
    private final String nickname;
    private final String roomCode;
    private final GameClient gameClient;
    private Label hostStatusLabel;
    private VBox playerList;
    private TextArea chatArea;
    private ButtonsUtil readyBtn;
    private Label countdownLabel;
    private boolean currentReady;
    private long countdownStartedAt;
    private Timeline countdownTimeline;
    private static final int COUNTDOWN_SECONDS = 10;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public WaitingRoomScene(Stage stage, String nickname, String roomCode) {
        this(stage, nickname, roomCode, null);
    }

    public WaitingRoomScene(Stage stage, String nickname, String roomCode, GameClient gameClient) {
        this.stage = stage;
        this.root = new StackPane();
        this.nickname = nickname == null || nickname.isBlank() ? "PLAYER" : nickname.toUpperCase();
        this.roomCode = roomCode == null ? "----" : roomCode;
        this.gameClient = gameClient;

        setupBackground();
        setupUI();

        if (this.gameClient != null) {
            this.gameClient.addPlayerUpdateListener(gameState -> Platform.runLater(() -> updateLobbyState(gameState)));
        }
    }

    public StackPane getRoot() {
        return root;
    }

    private void setupBackground() {
        ImageView background = new ImageView(new Image(getClass().getResource("/background.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    private void setupUI() {
        Label title = TextUtil.createText("LOBBY", "hkmodular", 0.06, "#cfc900", root);
        GlowUtil.applyGlow(title, "#cfc900", 15);

        Label subtitle = TextUtil.createText("WAITING ROOM", "hkmodular", 0.034, "#ff2d2d", root);
        GlowUtil.applyGlow(subtitle, "#ff2d2d", 10);

        VBox headerBox = new VBox(title, subtitle);
        headerBox.setAlignment(Pos.TOP_CENTER);
        headerBox.spacingProperty().bind(root.heightProperty().multiply(0.018));
        StackPane.setAlignment(headerBox, Pos.TOP_CENTER);
        headerBox.translateYProperty().bind(root.heightProperty().multiply(0.05));

        hostStatusLabel = TextUtil.createText("NOT READY", "hkmodular", 0.024, "#d1d412", root);

        playerList = new VBox();
        playerList.spacingProperty().bind(root.heightProperty().multiply(0.012));
        playerList.setAlignment(Pos.TOP_LEFT);
        playerList.getChildren().add(createPlayerRow(nickname.toUpperCase(), hostStatusLabel));

        VBox chatPanel = new VBox();
        chatPanel.setAlignment(Pos.TOP_LEFT);
        chatPanel.spacingProperty().bind(root.heightProperty().multiply(0.016));
        chatPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.30));
        chatPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.31));
        chatPanel.setStyle(
                "-fx-background-color: rgba(0,0,0,0.70);" +
                "-fx-border-color: #00b5d4;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );
        GlowUtil.applyGlow(chatPanel, "#00b5d4", 10);

        chatPanel.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(root.getHeight() * 0.018, root.getWidth() * 0.012, root.getHeight() * 0.018, root.getWidth() * 0.012),
                root.widthProperty(), root.heightProperty()
        ));

        Label chatTitle = TextUtil.createText("PLAYER CHAT:", "hkmodular", 0.028, "#d1d412", root);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setFocusTraversable(false);
        chatArea.prefHeightProperty().bind(root.heightProperty().multiply(0.145));
        chatArea.setStyle(
                "-fx-control-inner-background: black;" +
                "-fx-text-fill: #00d9ff;" +
                "-fx-border-color: transparent;" +
                "-fx-font-family: 'hkmodular';"
        );

        TextField chatInput = new TextField();
        chatInput.setStyle("-fx-background-color: black; -fx-text-fill: #00d9ff; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-font-family: 'hkmodular';");
        chatInput.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        chatInput.prefHeightProperty().bind(root.heightProperty().multiply(0.055));

        ButtonsUtil sendBtn = new ButtonsUtil("SEND", "#d1d412", "black", "#00b5d4", "#00b5d4", 2, 10, 10, 0.07, 0.055, 0.019, root);
        Runnable sendChat = () -> {
            String msg = chatInput.getText().trim();
            if (msg.isEmpty()) return;
            sendLobbyChat(msg);
            chatInput.clear();
        };
        sendBtn.setOnMouseClicked(e -> sendChat.run());
        chatInput.setOnAction(e -> sendChat.run());

        HBox inputRow = new HBox(chatInput, sendBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.spacingProperty().bind(root.widthProperty().multiply(0.010));

        chatPanel.getChildren().addAll(chatTitle, chatArea, inputRow);

        HBox upper = new HBox(playerList, chatPanel);
        upper.setAlignment(Pos.TOP_CENTER);
        upper.spacingProperty().bind(root.widthProperty().multiply(0.015));

        ButtonsUtil cancelBtn = new ButtonsUtil("CANCEL", "#ff2d2d", "black", "#ff2d2d", "#ff2d2d", 2, 12, 12, 0.14, 0.07, 0.020, root);
        readyBtn = new ButtonsUtil("READY", "#00d9ff", "black", "#00d9ff", "#00d9ff", 2, 12, 12, 0.14, 0.07, 0.020, root);
        readyBtn.setOnMouseClicked(e -> sendReadyState(!currentReady));

        cancelBtn.setOnMouseClicked(e -> SceneManager.switchScene("LOBBY"));

        countdownLabel = TextUtil.createText("", "hkmodular", 0.024, "#ff2d2d", root);
        countdownLabel.setVisible(false);

        HBox bottom = new HBox(cancelBtn, readyBtn, countdownLabel);
        bottom.setAlignment(Pos.CENTER);
        bottom.spacingProperty().bind(root.widthProperty().multiply(0.05));

        VBox content = new VBox(upper, bottom);
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(false);
        content.spacingProperty().bind(root.heightProperty().multiply(0.04));
        content.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(root.getHeight() * 0.04, root.getWidth() * 0.018, root.getHeight() * 0.03, root.getWidth() * 0.018),
                root.widthProperty(), root.heightProperty()
        ));

        StackPane.setAlignment(content, Pos.TOP_CENTER);
        content.translateYProperty().bind(root.heightProperty().multiply(0.24));

        root.getChildren().addAll(
                headerBox,
                content
        );
    }

    private void updateLobbyState(JsonNode gameState) {
        if (gameState == null) {
            System.out.println("[WaitingRoomScene] Received null game state");
            return;
        }

        JsonNode playersArray = null;
        if (gameState.has("players")) {
            playersArray = gameState.get("players");
        } else if (gameState.has("playerArray")) {
            playersArray = gameState.get("playerArray");
        }

        if (playersArray == null || !playersArray.isArray()) {
            System.out.println("[WaitingRoomScene] No valid players array in game state");
            return;
        }

        playerList.getChildren().clear();
        boolean foundCurrentPlayer = false;
        for (JsonNode playerNode : playersArray) {
            String playerName = playerNode.has("playerName") ? playerNode.get("playerName").asText() : "UNKNOWN";
            boolean ready = playerNode.has("ready") && playerNode.get("ready").asBoolean();
            Label statusLabel = TextUtil.createText(ready ? "READY" : "NOT READY", "hkmodular", 0.024, "#d1d412", root);
            playerList.getChildren().add(createPlayerRow(playerName, statusLabel));

            if (playerName.equalsIgnoreCase(nickname)) {
                currentReady = ready;
                foundCurrentPlayer = true;
            }
        }

        if (!foundCurrentPlayer) {
            currentReady = false;
        }
        readyBtn.setText(currentReady ? "UNREADY" : "READY");
        updateChat(gameState);
        updateCountdown(gameState);

        System.out.println("[WaitingRoomScene] Updated player list with " + playersArray.size() + " players");
    }

    private void updateChat(JsonNode gameState) {
        JsonNode messages = gameState.get("lobbyChatMessages");
        if (messages == null || !messages.isArray()) {
            chatArea.clear();
            return;
        }

        StringBuilder chatText = new StringBuilder();
        for (JsonNode messageNode : messages) {
            String playerName = messageNode.has("playerName") ? messageNode.get("playerName").asText() : "PLAYER";
            String message = messageNode.has("message") ? messageNode.get("message").asText() : "";
            if (message.isBlank()) {
                continue;
            }
            if (!chatText.isEmpty()) {
                chatText.append("\n");
            }
            chatText.append(playerName).append(": ").append(message);
        }
        chatArea.setText(chatText.toString());
        chatArea.positionCaret(chatArea.getText().length());
    }

    private void updateCountdown(JsonNode gameState) {
        long newCountdownStartedAt = gameState.has("lobbyCountdownStartedAt")
                ? gameState.get("lobbyCountdownStartedAt").asLong()
                : 0;

        if (newCountdownStartedAt <= 0) {
            stopCountdown();
            return;
        }

        countdownStartedAt = newCountdownStartedAt;
        if (countdownTimeline == null) {
            countdownTimeline = new Timeline(new KeyFrame(Duration.millis(250), e -> refreshCountdownLabel()));
            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();
        }
        refreshCountdownLabel();
    }

    private void refreshCountdownLabel() {
        long elapsedMillis = System.currentTimeMillis() - countdownStartedAt;
        int remaining = COUNTDOWN_SECONDS - (int) Math.floor(elapsedMillis / 1000.0);
        if (remaining <= 0) {
            countdownLabel.setText("STARTING...");
            countdownLabel.setVisible(true);
            return;
        }
        countdownLabel.setText("STARTING IN " + remaining);
        countdownLabel.setVisible(true);
    }

    private void stopCountdown() {
        countdownStartedAt = 0;
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        countdownLabel.setText("");
        countdownLabel.setVisible(false);
    }

    private void sendLobbyChat(String message) {
        if (gameClient == null) {
            return;
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("playerName", nickname);
        payload.put("message", message);
        gameClient.sendPacket(new Packet(PacketType.LOBBY_CHAT, payload));
    }

    private void sendReadyState(boolean ready) {
        if (gameClient == null) {
            return;
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("ready", ready);
        gameClient.sendPacket(new Packet(PacketType.LOBBY_READY, payload));
    }

    private TextField createCyberInput() {
        TextField field = new TextField();
        field.setStyle(
                "-fx-background-color: black;" +
                "-fx-text-fill: #00d9ff;" +
                "-fx-border-color: #00b5d4;" +
                "-fx-border-width: 2;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-font-family: 'hkmodular';"
        );
        return field;
    }

    private void lockToPref(Region region) {
        region.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        region.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    private StackPane createPlayerRow(String name, Label statusLabel) {
        StackPane row = new StackPane();
        row.setMaxWidth(460);
        row.setStyle("-fx-background-color: rgba(0,0,0,0.65); -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label nameLabel = TextUtil.createText(name, "hkmodular", 0.024, "#00d9ff", root);

        HBox inner = new HBox(16, nameLabel, new Region(), statusLabel);
        HBox.setHgrow(inner.getChildren().get(1), Priority.ALWAYS);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(8));

        row.getChildren().add(inner);
        return row;
    }
}
