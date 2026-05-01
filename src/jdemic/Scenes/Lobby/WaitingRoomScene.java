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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class WaitingRoomScene {
    private final StackPane root;
    private final Stage stage;
    private final String nickname;
    private final String roomCode;
    private Label hostStatusLabel;

    public WaitingRoomScene(Stage stage, String nickname, String roomCode) {
        this.stage = stage;
        this.root = new StackPane();
        this.nickname = nickname == null || nickname.isBlank() ? "PLAYER" : nickname.toUpperCase();
        this.roomCode = roomCode == null ? "----" : roomCode;

        setupBackground();
        setupUI();
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

        hostStatusLabel = TextUtil.createText("READY", "hkmodular", 0.024, "#d1d412", root);

        VBox playerList = new VBox(
                createPlayerRow(nickname.toUpperCase(), hostStatusLabel),
                createPlayerRow("TREXHERO", TextUtil.createText("WAITING...", "hkmodular", 0.024, "#d1d412", root)),
                createPlayerRow("TREXHERO", TextUtil.createText("WAITING...", "hkmodular", 0.024, "#d1d412", root)),
                createPlayerRow("TREXHERO", TextUtil.createText("WAITING...", "hkmodular", 0.024, "#d1d412", root))
        );
        playerList.spacingProperty().bind(root.heightProperty().multiply(0.012));
        playerList.setAlignment(Pos.TOP_LEFT);

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

        TextArea chatArea = new TextArea();
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
        sendBtn.setOnMouseClicked(e -> {
            String msg = chatInput.getText().trim();
            if (msg.isEmpty()) return;
            if (!chatArea.getText().isEmpty()) chatArea.appendText("\n");
            chatArea.appendText(nickname + ": " + msg);
            chatInput.clear();
        });

        HBox inputRow = new HBox(chatInput, sendBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.spacingProperty().bind(root.widthProperty().multiply(0.010));

        chatPanel.getChildren().addAll(chatTitle, chatArea, inputRow);

        HBox upper = new HBox(playerList, chatPanel);
        upper.setAlignment(Pos.TOP_CENTER);
        upper.spacingProperty().bind(root.widthProperty().multiply(0.015));

        ButtonsUtil cancelBtn = new ButtonsUtil("CANCEL", "#ff2d2d", "black", "#ff2d2d", "#ff2d2d", 2, 12, 12, 0.14, 0.07, 0.020, root);
        ButtonsUtil readyBtn = new ButtonsUtil("READY", "#00d9ff", "black", "#00d9ff", "#00d9ff", 2, 12, 12, 0.14, 0.07, 0.020, root);

        final boolean[] isReady = {true};
        readyBtn.setText("UNREADY");
        readyBtn.setOnMouseClicked(e -> {
            isReady[0] = !isReady[0];
            if (isReady[0]) {
                hostStatusLabel.setText("READY");
                readyBtn.setText("UNREADY");
            } else {
                hostStatusLabel.setText("WAITING...");
                readyBtn.setText("READY");
            }
        });

        cancelBtn.setOnMouseClicked(e -> SceneManager.switchScene("LOBBY"));

        HBox bottom = new HBox(cancelBtn, readyBtn);
        bottom.setAlignment(Pos.CENTER);
        bottom.spacingProperty().bind(root.widthProperty().multiply(0.08));

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
