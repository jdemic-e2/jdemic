package jdemic.Scenes;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.PanelUtil;
import jdemic.ui.TextUtil;
import jdemic.GameLogic.Player;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.GameManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;

public class PlayScene {

    private final StackPane root;
    private final Stage stage;

    private String nickname = "";
    private final String hostCode;
    private Label hostStatusLabel;

    public PlayScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        this.hostCode = generateHostCode();

        setupBackground();
        showEntryScreen();
    }

    public StackPane getRoot() {
        return root;
    }

    private void resetScreen() {
        root.getChildren().clear();
        setupBackground();
    }

    private void setupBackground() {
        ImageView background = new ImageView(new Image(getClass().getResource("/background.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
        background.toBack();
    }

    private Canvas createFrameLayer(double widthRatio, double heightRatio, double yOffsetRatio) {
        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());
        canvas.setMouseTransparent(true);

        Runnable draw = () -> {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            double w = canvas.getWidth();
            double h = canvas.getHeight();
            gc.clearRect(0, 0, w, h);
            double frameW = w * widthRatio;
            double frameH = h * heightRatio;
            double x = (w - frameW) / 2.0;
            double y = ((h - frameH) / 2.0) + (h * yOffsetRatio);
            double lineW = Math.max(2.0, w * 0.0017);
            gc.setStroke(Color.web("#00b5d4", 0.30));
            gc.setLineWidth(lineW * 3.0);
            gc.strokeRoundRect(x, y, frameW, frameH, 22, 22);
            gc.setStroke(Color.web("#00b5d4"));
            gc.setLineWidth(lineW);
            gc.strokeRoundRect(x, y, frameW, frameH, 22, 22);
        };

        ChangeListener<Number> listener = (obs, oldV, newV) -> draw.run();
        canvas.widthProperty().addListener(listener);
        canvas.heightProperty().addListener(listener);
        Platform.runLater(draw);
        return canvas;
    }

    private TextField createCyberInput() {
        TextField field = new TextField();
        field.setStyle("-fx-background-color: black; -fx-text-fill: #00d9ff; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10; -fx-font-family: 'hkmodular';");
        return field;
    }
    private void lockToPref(Region region) {
        region.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        region.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    private void showEntryScreen() {
        resetScreen();
        Label title = TextUtil.createText("LOBBY", "hkmodular", 0.06, "#d1d412", root);
        GlowUtil.applyGlow(title, "#d1d412", 15);
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.07));
        StackPane panel = PanelUtil.createPanel(0.78, 0.58, "#00b5d4", 2, 15, 10, root);
        panel.setStyle("-fx-background-color: rgba(0,0,0,0.92); -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        Label nickLabel = TextUtil.createText("NICKNAME:", "hkmodular", 0.028, "#d1d412", root);
        TextField nickField = createCyberInput();
        nickField.setText(nickname);
        nickField.prefWidthProperty().bind(root.widthProperty().multiply(0.27));
        nickField.prefHeightProperty().bind(root.heightProperty().multiply(0.065));
        UnaryOperator<TextFormatter.Change> filter = change -> change.getControlNewText().length() <= 16 ? change : null;
        nickField.setTextFormatter(new TextFormatter<>(filter));
        Label errorLabel = TextUtil.createText("ENTER NICKNAME", "hkmodular", 0.017, "#ff2d2d", root);
        errorLabel.setVisible(false);
        HBox nickRow = new HBox(nickLabel, nickField);
        nickRow.setAlignment(Pos.CENTER);
        nickRow.spacingProperty().bind(root.widthProperty().multiply(0.02));
        ButtonsUtil hostBtn = new ButtonsUtil("HOST GAME", "#ff2d2d", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.33, 0.085, 0.024, root);
        ButtonsUtil joinBtn = new ButtonsUtil("JOIN BY CODE", "#00d9ff", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.33, 0.085, 0.024, root);
        ButtonsUtil backBtn = new ButtonsUtil("BACK", "#ff2d2d", "black", "#ff2d2d", "#ff2d2d", 2, 12, 12, 0.16, 0.07, 0.020, root);
        hostBtn.setOnMouseClicked(e -> {
            String name = nickField.getText().trim();
            if (name.isEmpty()) { errorLabel.setVisible(true); return; }
            errorLabel.setVisible(false); nickname = name; showHostCodeScreen();
        });
        backBtn.setOnMouseClicked(e -> stage.getScene().setRoot(new MainMenuScene(stage).getRoot()));
        VBox content = new VBox(nickRow, errorLabel, hostBtn, joinBtn, backBtn);
        content.setAlignment(Pos.TOP_CENTER);
        content.spacingProperty().bind(root.heightProperty().multiply(0.038));
        content.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.10, 0, 0, 0), root.heightProperty()));
        panel.getChildren().add(content);
        root.getChildren().addAll(createFrameLayer(0.78, 0.58, 0), title, panel);
    }

    private void showHostCodeScreen() {
        resetScreen();
        Label title = TextUtil.createText("LOBBY", "hkmodular", 0.06, "#d1d412", root);
        GlowUtil.applyGlow(title, "#d1d412", 15);
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.07));
        Label subtitle = TextUtil.createText("HOST GAME", "hkmodular", 0.034, "#ff2d2d", root);
        GlowUtil.applyGlow(subtitle, "#ff2d2d", 10);
        StackPane.setAlignment(subtitle, Pos.TOP_CENTER);
        subtitle.translateYProperty().bind(root.heightProperty().multiply(0.15));
        StackPane panel = PanelUtil.createPanel(0.78, 0.58, "#00b5d4", 2, 15, 10, root);
        panel.setStyle("-fx-background-color: rgba(0,0,0,0.92); -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        lockToPref(panel);
        Label codeText = TextUtil.createText("CODE:", "hkmodular", 0.038, "#00d9ff", root);
        StackPane codeBox = new StackPane();
        codeBox.prefWidthProperty().bind(root.widthProperty().multiply(0.34));
        codeBox.prefHeightProperty().bind(root.heightProperty().multiply(0.10));
        codeBox.setStyle("-fx-background-color: black; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        GlowUtil.applyGlow(codeBox, "#00b5d4", 12);
        lockToPref(codeBox);
        Label codeValue = TextUtil.createText(hostCode, "hkmodular", 0.042, "#ffffff", root);
        GlowUtil.applyGlow(codeValue, "#ffffff", 8);
        codeBox.getChildren().add(codeValue);
        ButtonsUtil copyBtn = new ButtonsUtil("COPY", "#00d9ff", "black", "#00d9ff", "#00d9ff", 2, 12, 12, 0.13, 0.07, 0.020, root);
        ButtonsUtil backBtn = new ButtonsUtil("BACK", "#ff2d2d", "black", "#ff2d2d", "#ff2d2d", 2, 12, 12, 0.13, 0.07, 0.020, root);
        ButtonsUtil hostBtn = new ButtonsUtil("HOST", "#00d9ff", "black", "#00d9ff", "#00d9ff", 2, 12, 12, 0.13, 0.07, 0.020, root);
        copyBtn.setOnMouseClicked(e -> { ClipboardContent cc = new ClipboardContent(); cc.putString(hostCode); Clipboard.getSystemClipboard().setContent(cc); });
        backBtn.setOnMouseClicked(e -> showEntryScreen());
        hostBtn.setOnMouseClicked(e -> showHostLobbyScreen());
        HBox bottomRow = new HBox(backBtn, hostBtn);
        bottomRow.setAlignment(Pos.CENTER);
        bottomRow.spacingProperty().bind(root.widthProperty().multiply(0.16));
        VBox content = new VBox(codeText, codeBox, copyBtn, bottomRow);
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(false);
        content.spacingProperty().bind(root.heightProperty().multiply(0.04));
        content.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.08, 0, root.getHeight() * 0.04, 0), root.heightProperty()));
        panel.getChildren().add(content);
        root.getChildren().addAll(createFrameLayer(0.78, 0.58, 0), title, subtitle, panel);
    }

    private void showHostLobbyScreen() {
        resetScreen();
        Label title = TextUtil.createText("LOBBY", "hkmodular", 0.06, "#d1d412", root);
        GlowUtil.applyGlow(title, "#d1d412", 15);
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.07));
        StackPane panel = PanelUtil.createPanel(0.70, 0.58, "#00b5d4", 2, 15, 10, root);
        panel.setStyle("-fx-background-color: rgba(0,0,0,0.92); -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        lockToPref(panel);
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
        chatPanel.setStyle("-fx-background-color: rgba(0,0,0,0.70); -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        GlowUtil.applyGlow(chatPanel, "#00b5d4", 10);
        lockToPref(chatPanel);
        chatPanel.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.018, root.getWidth() * 0.012, root.getHeight() * 0.018, root.getWidth() * 0.012), root.widthProperty(), root.heightProperty()));
        Label chatTitle = TextUtil.createText("PLAYER CHAT:", "hkmodular", 0.028, "#d1d412", root);
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false); chatArea.setWrapText(true); chatArea.setFocusTraversable(false);
        chatArea.prefHeightProperty().bind(root.heightProperty().multiply(0.145));
        chatArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: #00d9ff; -fx-border-color: transparent; -fx-font-family: 'hkmodular';");
        TextField chatInput = createCyberInput();
        chatInput.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        chatInput.prefHeightProperty().bind(root.heightProperty().multiply(0.055));
        lockToPref(chatInput);
        ButtonsUtil sendBtn = new ButtonsUtil("SEND", "#d1d412", "black", "#00b5d4", "#00b5d4", 2, 10, 10, 0.07, 0.055, 0.019, root);
        sendBtn.setOnMouseClicked(e -> {
            String msg = chatInput.getText().trim(); if (msg.isEmpty()) return;
            if (!chatArea.getText().isEmpty()) chatArea.appendText("\n");
            chatArea.appendText(nickname + ": " + msg); chatInput.clear();
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
        readyBtn.setOnMouseClicked(e -> {
            isReady[0] = !isReady[0];
            if (isReady[0]) {
                hostStatusLabel.setText("READY");
                readyBtn.setText("READY");
            } else {
                hostStatusLabel.setText("WAITING...");
                readyBtn.setText("UNREADY");
            }
        });
        cancelBtn.setOnMouseClicked(e -> showHostCodeScreen());
        HBox bottom = new HBox(cancelBtn, readyBtn);
        bottom.setAlignment(Pos.CENTER);
        bottom.spacingProperty().bind(root.widthProperty().multiply(0.08));
        VBox content = new VBox(upper, bottom);
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(false);
        content.spacingProperty().bind(root.heightProperty().multiply(0.04));
        content.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.04, root.getWidth() * 0.018, root.getHeight() * 0.03, root.getWidth() * 0.018), root.widthProperty(), root.heightProperty()));
        panel.getChildren().add(content);
        root.getChildren().addAll(createFrameLayer(0.70, 0.58, 0), title, panel);
    }

    private StackPane createPlayerRow(String playerName, Label statusLabel) {
        StackPane row = new StackPane();
        row.prefWidthProperty().bind(root.widthProperty().multiply(0.31));
        row.prefHeightProperty().bind(root.heightProperty().multiply(0.072));
        row.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        GlowUtil.applyGlow(row, "#00b5d4", 8);
        lockToPref(row);
        Label nameLabel = TextUtil.createText(playerName, "hkmodular", 0.024, "#00d9ff", root);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox inner = new HBox(nameLabel, spacer, statusLabel);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(0, root.getWidth() * 0.010, 0, root.getWidth() * 0.010), root.widthProperty()));
        row.getChildren().add(inner);
        return row;
    }

    private String generateHostCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) { sb.append(chars.charAt(rnd.nextInt(chars.length()))); }
        return sb.toString();
    }

    // --- PLAYER ICON TASK INNOVATIONS (Simplified to Icons Only) ---

    public void showGameplayScreen(GameManager gameManager) {
        resetScreen();
        VBox playerIconsContainer = new VBox(15);
        playerIconsContainer.setPadding(new Insets(20, 0, 0, 20));
        StackPane.setAlignment(playerIconsContainer, Pos.TOP_LEFT);
        playerIconsContainer.setPickOnBounds(false);

        for (Player p : gameManager.getPlayers()) {
            playerIconsContainer.getChildren().add(createGameplayPlayerRow(p));
        }
        root.getChildren().add(playerIconsContainer);
    }

    private HBox createGameplayPlayerRow(Player player) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(javafx.scene.Cursor.HAND);

        String roleFileName = "player_placeholder.png";
        if (player.getState().getPlayerRole() != null) {
            roleFileName = player.getState().getPlayerRole().toString().toLowerCase() + ".png";
        }

        Image img = null;
        try {
            var stream = getClass().getResourceAsStream("/elements/" + roleFileName);
            if (stream == null) throw new Exception("Asıl ikon bulunamadı");
            img = new Image(stream);
        } catch (Exception e) {
            try {
                var placeholderStream = getClass().getResourceAsStream("/elements/player_placeholder.png");
                if (placeholderStream == null) throw new Exception("Placeholder da bulunamadı");
                img = new Image(placeholderStream);
            } catch (Exception ex) {
                img = new Image(getClass().getResourceAsStream("/elements/redDot.png"));
            }
        }

        ImageView iconView = new ImageView(img);
        iconView.setFitWidth(45);
        iconView.setFitHeight(45);
        iconView.setPreserveRatio(true);

        GlowUtil.applyGlow(iconView, "#00d9ff", 10);

        row.getChildren().add(iconView);
        row.setOnMouseClicked(e -> showPlayerCardsOverlay(player));

        return row;
    }

    private void showPlayerCardsOverlay(Player player) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");

        VBox cardPanel = new VBox(20);
        cardPanel.setAlignment(Pos.CENTER);
        cardPanel.setMaxSize(700, 450);
        cardPanel.setStyle("-fx-background-color: rgba(13, 17, 23, 0.95); -fx-border-color: #00d9ff; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");
        GlowUtil.applyGlow(cardPanel, "#00d9ff", 15);

        Label title = TextUtil.createText(player.getState().getPlayerName() + "'S HAND", "hkmodular", 0.035, "#d1d412", root);
        HBox cardList = new HBox(15);
        cardList.setAlignment(Pos.CENTER);

        if (player.getState().getHand() == null || player.getState().getHand().isEmpty()) {
            cardList.getChildren().add(TextUtil.createText("NO CARDS IN HAND", "hkmodular", 0.020, "#ff2d2d", root));
        } else {
            for (Card card : player.getState().getHand()) {
                VBox cardUI = new VBox(5);
                cardUI.setAlignment(Pos.CENTER);
                cardUI.setStyle("-fx-border-color: #00b5d4; -fx-padding: 10; -fx-border-radius: 5;");
                Label cardName = TextUtil.createText(card.getCardName(), "hkmodular", 0.016, "#ffffff", root);
                cardUI.getChildren().add(cardName);
                cardList.getChildren().add(cardUI);
            }
        }

        ButtonsUtil closeBtn = new ButtonsUtil("CLOSE", "#ff2d2d", "black", "#ff2d2d", "#ff2d2d", 2, 12, 12, 0.12, 0.06, 0.018, root);
        closeBtn.setOnMouseClicked(e -> root.getChildren().remove(overlay));

        cardPanel.getChildren().addAll(title, cardList, closeBtn);
        overlay.getChildren().add(cardPanel);
        root.getChildren().add(overlay);
    }
}