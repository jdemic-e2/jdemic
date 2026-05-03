package jdemic.ui.GameplayUI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import jdemic.GameLogic.GameClient;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class ChatManager {
    private final StackPane root;
    private final String playerName;
    private final NotificationManager notificationManager;
    private final GameClient gameClient;
    private TextArea chatArea;
    private TextField chatInput;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ChatManager(StackPane root, String playerName, NotificationManager notificationManager) {
        this(root, playerName, notificationManager, null);
    }

    public ChatManager(StackPane root, String playerName, NotificationManager notificationManager, GameClient gameClient) {
        this.root = root;
        this.playerName = playerName == null || playerName.isBlank() ? "PLAYER" : playerName.toUpperCase();
        this.notificationManager = notificationManager;
        this.gameClient = gameClient;
        setupUI();
    }

    private void setupUI() {
        VBox chatPanel = new VBox();
        chatPanel.setAlignment(Pos.TOP_LEFT);
        chatPanel.spacingProperty().bind(root.heightProperty().multiply(0.012));
        chatPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.28));
        chatPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.27));
        chatPanel.maxWidthProperty().bind(chatPanel.prefWidthProperty());
        chatPanel.maxHeightProperty().bind(chatPanel.prefHeightProperty());
        chatPanel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.72);" +
                "-fx-border-color: #00b5d4;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );
        GlowUtil.applyGlow(chatPanel, "#00b5d4", 10);

        chatPanel.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(root.getHeight() * 0.014, root.getWidth() * 0.010,
                        root.getHeight() * 0.014, root.getWidth() * 0.010),
                root.widthProperty(), root.heightProperty()
        ));

        Label title = TextUtil.createText("PLAYER CHAT", "hkmodular", 0.018, "#d1d412", root);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setFocusTraversable(false);
        chatArea.prefHeightProperty().bind(root.heightProperty().multiply(0.14));
        chatArea.setStyle(
                "-fx-control-inner-background: black;" +
                "-fx-text-fill: #00d9ff;" +
                "-fx-border-color: transparent;" +
                "-fx-font-family: 'hkmodular';" +
                "-fx-font-size: 13px;"
        );

        chatInput = new TextField();
        chatInput.setPromptText("TYPE MESSAGE");
        chatInput.setStyle(
                "-fx-background-color: black;" +
                "-fx-text-fill: #00d9ff;" +
                "-fx-prompt-text-fill: #356f78;" +
                "-fx-border-color: #00b5d4;" +
                "-fx-border-width: 2;" +
                "-fx-font-family: 'hkmodular';"
        );
        chatInput.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        chatInput.prefHeightProperty().bind(root.heightProperty().multiply(0.045));
        chatInput.setOnAction(e -> sendMessage());

        ButtonsUtil sendBtn = new ButtonsUtil(
                "SEND", "#d1d412", "black", "#00b5d4", "#00b5d4",
                2, 8, 8, 0.07, 0.045, 0.012, root
        );
        sendBtn.setOnMouseClicked(e -> sendMessage());

        HBox inputRow = new HBox(chatInput, sendBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.spacingProperty().bind(root.widthProperty().multiply(0.008));

        chatPanel.getChildren().addAll(title, chatArea, inputRow);

        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        chatPanel.translateXProperty().bind(root.widthProperty().multiply(-0.025));
        chatPanel.translateYProperty().bind(root.heightProperty().multiply(-0.045));
        root.getChildren().add(chatPanel);
    }

    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;

        if (gameClient != null) {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("playerName", playerName);
            payload.put("message", message);
            gameClient.sendPacket(new Packet(PacketType.LOBBY_CHAT, payload));
        } else {
            appendMessage(playerName, message);
        }

        chatInput.clear();

        if (notificationManager != null) {
            notificationManager.showNotification("Chat message sent");
        }
    }

    public void updateMessages(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            chatArea.clear();
            return;
        }

        StringBuilder chatText = new StringBuilder();
        for (JsonNode messageNode : messages) {
            String senderName = messageNode.has("playerName") ? messageNode.get("playerName").asText() : "PLAYER";
            String message = messageNode.has("message") ? messageNode.get("message").asText() : "";
            if (message.isBlank()) {
                continue;
            }
            if (!chatText.isEmpty()) {
                chatText.append("\n");
            }
            chatText.append(senderName).append(": ").append(message);
        }

        chatArea.setText(chatText.toString());
        chatArea.positionCaret(chatArea.getText().length());
    }

    private void appendMessage(String senderName, String message) {
        if (!chatArea.getText().isEmpty()) {
            chatArea.appendText("\n");
        }
        chatArea.appendText(senderName + ": " + message);
    }
}
