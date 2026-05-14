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
        chatPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.27));
        chatPanel.maxHeightProperty().bind(chatPanel.prefHeightProperty());
        StackPane wrapper = new StackPane(chatPanel);
        chatPanel.prefWidthProperty().bind(wrapper.prefWidthProperty());
        wrapper.setMaxSize(StackPane.USE_PREF_SIZE, StackPane.USE_PREF_SIZE);
        wrapper.setStyle("-fx-background-color: black;" + "-fx-border-color: transparent #00b5d4 transparent transparent;" + "-fx-border-width: 0 1 0 0;");
        wrapper.prefWidthProperty().bind(root.widthProperty().multiply(0.24));
        GlowUtil.applyGlow(wrapper, "#00b5d4", 15);

        chatPanel.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(root.getHeight() * 0.014, root.getWidth() * 0.010,
                        root.getHeight() * 0.014, root.getWidth() * 0.010),
                root.widthProperty(), root.heightProperty()
        ));
        Label title = TextUtil.createText("PLAYER CHAT", "hkmodular", 0.010, "#00d9ff", root);
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setFocusTraversable(false);
        chatArea.prefHeightProperty().bind(root.heightProperty().multiply(0.09));
        chatArea.prefWidthProperty().bind(chatPanel.widthProperty().multiply(0.97));
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
                "-fx-border-color: #00b5d4;" +
                "-fx-border-width: 1;" +
                "-fx-font-family: 'hkmodular';"
        );
        chatInput.prefWidthProperty().bind(chatPanel.widthProperty().multiply(0.75));
        chatInput.prefHeightProperty().bind(root.heightProperty().multiply(0.045));
        chatInput.setOnAction(e -> sendMessage());

        ButtonsUtil sendBtn = new ButtonsUtil(
            "SEND","#00d9ff", "black", "#00b5d4", "#00b5d4",
            1, 8, 8, 0.06, 0.045, 0.012, root
        );
        sendBtn.setOnMouseClicked(e -> sendMessage());
        sendBtn.setPrefWidth(70);
        HBox inputRow = new HBox(chatInput, sendBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.spacingProperty().bind(root.widthProperty().multiply(0.008));

        chatPanel.getChildren().addAll(title, chatArea, inputRow);

        StackPane.setAlignment(chatPanel, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(wrapper, Pos.BOTTOM_LEFT);
        StackPane.setMargin(wrapper, new Insets(0, 0, root.getHeight() * 0.02, root.getWidth() * 0.02));
        wrapper.translateXProperty().bind(root.widthProperty().multiply(0.42));
        wrapper.translateYProperty().bind(root.heightProperty().multiply(0.078));
        
        root.getChildren().add(wrapper);
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
