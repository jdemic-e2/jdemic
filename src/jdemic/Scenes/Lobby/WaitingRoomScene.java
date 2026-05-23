package jdemic.Scenes.Lobby;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import jdemic.DedicatedServer.network.core.JdemicNetworkServer;
import jdemic.GameLogic.GameClient;
import jdemic.Scenes.MapTest.MapTestScene;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.SafeResourceLoader;
import jdemic.ui.TextUtil;

public class WaitingRoomScene {
    private static final String FONT_HKMODULAR = "hkmodular";
    private static final String DEFAULT_PLAYER_NAME = "PLAYER";
    private static final String DEFAULT_ROOM_CODE = "----";
    private static final String READY_TEXT = "READY";
    private static final String NOT_READY_TEXT = "NOT READY";
    private static final String UNREADY_TEXT = "UNREADY";
    private static final String CYAN = "#00b5d4";
    private static final String BRIGHT_CYAN = "#00d9ff";
    private static final String RED = "#ff2d2d";
    private static final String YELLOW = "#d1d412";
    private static final String BLACK = "black";
    private static final String BACKGROUND_RESOURCE = "/background.png";
    private static final String SCENE_LOBBY = "LOBBY";
    private static final String JSON_PLAYERS = "players";
    private static final String JSON_PLAYER_ARRAY = "playerArray";
    private static final String JSON_GAME_STARTED = "gameStarted";
    private static final String JSON_PLAYER_NAME = "playerName";
    private static final String JSON_READY = "ready";
    private static final String JSON_LOBBY_CHAT_MESSAGES = "lobbyChatMessages";
    private static final String JSON_MESSAGE = "message";
    private static final String JSON_LOBBY_COUNTDOWN_STARTED_AT = "lobbyCountdownStartedAt";

    private final StackPane root;
    private final Stage stage;
    private final String nickname;
    private final String roomCode;
    private final GameClient gameClient;
    private final boolean ownsServer;
    private Label hostStatusLabel;
    private VBox playerList;
    private TextArea chatArea;
    private ButtonsUtil readyBtn;
    private Label countdownLabel;
    private boolean currentReady;
    private boolean transitionedToGame;
    private long countdownStartedAt;
    private Timeline countdownTimeline;
    private static final int COUNTDOWN_SECONDS = 10;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private GameClient.PlayerUpdateListener playerUpdateListener;

    public WaitingRoomScene(Stage stage, String nickname, String roomCode) {
        this(stage, nickname, roomCode, null);
    }

    public WaitingRoomScene(Stage stage, String nickname, String roomCode, GameClient gameClient) {
        this(stage, nickname, roomCode, gameClient, false);
    }

    public WaitingRoomScene(Stage stage, String nickname, String roomCode, GameClient gameClient, boolean ownsServer) {
        this.stage = stage;
        this.root = new StackPane();
        SceneManager.registerLifecycle(root, this::cleanupScene);
        this.nickname = nickname == null || nickname.isBlank() ? DEFAULT_PLAYER_NAME : nickname.toUpperCase();
        this.roomCode = roomCode == null ? DEFAULT_ROOM_CODE : roomCode;
        this.gameClient = gameClient;
        this.ownsServer = ownsServer;

        setupBackground();
        setupUI();

        if (this.gameClient != null) {
            playerUpdateListener = gameState -> Platform.runLater(() -> updateLobbyState(gameState));
            this.gameClient.addPlayerUpdateListener(playerUpdateListener);
        }
    }

    public StackPane getRoot() {
        return root;
    }

    private void setupBackground() {
        java.net.URL bgUrl = getClass().getResource(BACKGROUND_RESOURCE);
        if (bgUrl == null) {
            System.err.println("[WaitingRoomScene] Missing resource: " + BACKGROUND_RESOURCE);
            return;
        }
        ImageView background = new ImageView(SafeResourceLoader.loadImage(bgUrl));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    private void setupUI() {
        Label title = TextUtil.createText(SCENE_LOBBY, FONT_HKMODULAR, 0.06, "#cfc900", root);
        GlowUtil.applyGlow(title, "#cfc900", 15);

        Label subtitle = TextUtil.createText("WAITING ROOM", FONT_HKMODULAR, 0.034, RED, root);
        GlowUtil.applyGlow(subtitle, RED, 10);

        VBox headerBox = new VBox(title, subtitle);
        headerBox.setAlignment(Pos.TOP_CENTER);
        headerBox.spacingProperty().bind(root.heightProperty().multiply(0.018));
        StackPane.setAlignment(headerBox, Pos.TOP_CENTER);
        headerBox.translateYProperty().bind(root.heightProperty().multiply(0.05));

        hostStatusLabel = TextUtil.createText(NOT_READY_TEXT, FONT_HKMODULAR, 0.024, YELLOW, root);

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
                "-fx-border-color: " + CYAN + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );
        GlowUtil.applyGlow(chatPanel, CYAN, 10);

        chatPanel.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(root.getHeight() * 0.018, root.getWidth() * 0.012, root.getHeight() * 0.018, root.getWidth() * 0.012),
                root.widthProperty(), root.heightProperty()
        ));

        Label chatTitle = TextUtil.createText("PLAYER CHAT:", FONT_HKMODULAR, 0.028, YELLOW, root);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setFocusTraversable(false);
        chatArea.prefHeightProperty().bind(root.heightProperty().multiply(0.145));
        chatArea.setStyle(
                "-fx-control-inner-background: black;" +
                "-fx-text-fill: " + BRIGHT_CYAN + ";" +
                "-fx-border-color: transparent;" +
                "-fx-font-family: '" + FONT_HKMODULAR + "';"
        );

        TextField chatInput = new TextField();
        chatInput.setStyle("-fx-background-color: " + BLACK + "; -fx-text-fill: " + BRIGHT_CYAN + "; -fx-border-color: " + CYAN + "; -fx-border-width: 2; -fx-font-family: '" + FONT_HKMODULAR + "';");
        chatInput.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        chatInput.prefHeightProperty().bind(root.heightProperty().multiply(0.055));

        ButtonsUtil sendBtn = new ButtonsUtil("SEND", YELLOW, BLACK, CYAN, CYAN, 2, 10, 10, 0.07, 0.055, 0.019, root);
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

        ButtonsUtil cancelBtn = new ButtonsUtil("CANCEL", RED, BLACK, RED, RED, 2, 12, 12, 0.14, 0.07, 0.020, root);
        readyBtn = new ButtonsUtil(READY_TEXT, BRIGHT_CYAN, BLACK, BRIGHT_CYAN, BRIGHT_CYAN, 2, 12, 12, 0.14, 0.07, 0.020, root);
        readyBtn.setOnMouseClicked(e -> sendReadyState(!currentReady));

        cancelBtn.setOnMouseClicked(e -> leaveWaitingRoom());

        countdownLabel = TextUtil.createText("", FONT_HKMODULAR, 0.024, RED, root);
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
        if (gameState.has(JSON_PLAYERS)) {
            playersArray = gameState.get(JSON_PLAYERS);
        } else if (gameState.has(JSON_PLAYER_ARRAY)) {
            playersArray = gameState.get(JSON_PLAYER_ARRAY);
        }

        if (playersArray == null || !playersArray.isArray()) {
            System.out.println("[WaitingRoomScene] No valid players array in game state");
            return;
        }

        if (gameState.has(JSON_GAME_STARTED) && gameState.get(JSON_GAME_STARTED).asBoolean()) {
            transitionToGame(gameState);
            return;
        }

        playerList.getChildren().clear();
        boolean foundCurrentPlayer = false;
        for (JsonNode playerNode : playersArray) {
            String playerName = playerNode.has(JSON_PLAYER_NAME) ? playerNode.get(JSON_PLAYER_NAME).asText() : "UNKNOWN";
            boolean ready = playerNode.has(JSON_READY) && playerNode.get(JSON_READY).asBoolean();
            Label statusLabel = TextUtil.createText(ready ? READY_TEXT : NOT_READY_TEXT, FONT_HKMODULAR, 0.024, YELLOW, root);
            playerList.getChildren().add(createPlayerRow(playerName, statusLabel));

            if (playerName.equalsIgnoreCase(nickname)) {
                currentReady = ready;
                foundCurrentPlayer = true;
            }
        }

        if (!foundCurrentPlayer) {
            currentReady = false;
        }
        readyBtn.setText(currentReady ? UNREADY_TEXT : READY_TEXT);
        updateChat(gameState);
        updateCountdown(gameState);

        System.out.println("[WaitingRoomScene] Updated player list with " + playersArray.size() + " players");
    }

    private void updateChat(JsonNode gameState) {
        JsonNode messages = gameState.get(JSON_LOBBY_CHAT_MESSAGES);
        if (messages == null || !messages.isArray()) {
            chatArea.clear();
            return;
        }

        StringBuilder chatText = new StringBuilder();
        for (JsonNode messageNode : messages) {
            String playerName = messageNode.has(JSON_PLAYER_NAME) ? messageNode.get(JSON_PLAYER_NAME).asText() : DEFAULT_PLAYER_NAME;
            String message = messageNode.has(JSON_MESSAGE) ? messageNode.get(JSON_MESSAGE).asText() : "";
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
        long newCountdownStartedAt = gameState.has(JSON_LOBBY_COUNTDOWN_STARTED_AT)
                ? gameState.get(JSON_LOBBY_COUNTDOWN_STARTED_AT).asLong()
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
        payload.put(JSON_PLAYER_NAME, nickname);
        payload.put(JSON_MESSAGE, message);
        gameClient.sendPacket(new Packet(PacketType.LOBBY_CHAT, payload));
    }

    private void sendReadyState(boolean ready) {
        if (gameClient == null) {
            return;
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put(JSON_READY, ready);
        gameClient.sendPacket(new Packet(PacketType.LOBBY_READY, payload));
    }

    private void leaveWaitingRoom() {
        cleanupScene();
        if (gameClient != null) {
            gameClient.disconnectFromLobby();
        }
        if (ownsServer) {
            JdemicNetworkServer.shutdown();
        }
        SceneManager.switchScene(SCENE_LOBBY);
    }

    private void transitionToGame(JsonNode gameState) {
        if (transitionedToGame) {
            return;
        }
        transitionedToGame = true;
        cleanupScene();
        SceneManager.setRoot(new MapTestScene(stage, nickname, gameClient, gameState).getRoot());
    }

    @SuppressWarnings("unused")
    private TextField createCyberInput() {
        TextField field = new TextField();
        field.setStyle(
                "-fx-background-color: " + BLACK + ";" +
                "-fx-text-fill: " + BRIGHT_CYAN + ";" +
                "-fx-border-color: " + CYAN + ";" +
                "-fx-border-width: 2;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-font-family: '" + FONT_HKMODULAR + "';"
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
        row.setStyle("-fx-background-color: rgba(0,0,0,0.65); -fx-border-color: " + CYAN + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label nameLabel = TextUtil.createText(name, FONT_HKMODULAR, 0.024, BRIGHT_CYAN, root);

        HBox inner = new HBox(16, nameLabel, new Region(), statusLabel);
        HBox.setHgrow(inner.getChildren().get(1), Priority.ALWAYS);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(8));

        row.getChildren().add(inner);
        return row;
    }
    private void cleanupScene() {
        stopCountdown();
        if (gameClient != null && playerUpdateListener != null) {
            gameClient.removePlayerUpdateListener(playerUpdateListener);
            playerUpdateListener = null;
        }
    }
}
