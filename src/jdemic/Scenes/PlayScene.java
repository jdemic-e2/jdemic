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
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.PanelUtil;
import jdemic.ui.SafeResourceLoader;
import jdemic.ui.TextUtil;
import java.security.SecureRandom;
import java.util.function.UnaryOperator;

public class PlayScene {
    private static final String FONT_HKMODULAR = "hkmodular";
    private static final String CYAN = "#00b5d4";
    private static final String BRIGHT_CYAN = "#00d9ff";
    private static final String RED = "#ff2d2d";
    private static final String YELLOW = "#d1d412";
    private static final String BLACK = "black";
    private static final String WHITE = "#ffffff";
    private static final String TITLE_LOBBY = "LOBBY";
    private static final String READY_TEXT = "READY";
    private static final String WAITING_TEXT = "WAITING...";
    private static final String UNREADY_TEXT = "UNREADY";
    private static final String PLAYER_PLACEHOLDER = "TREXHERO";
    private static final String BACKGROUND_RESOURCE = "/background.png";
    private static final String HOST_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String PANEL_STYLE = "-fx-background-color: rgba(0,0,0,0.92); -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;";
    private static final String CYBER_INPUT_STYLE = "-fx-background-color: black; -fx-text-fill: #00d9ff; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10; -fx-font-family: 'hkmodular';";

    private final StackPane root;
    private final Stage stage;

    private String nickname = "";
    private final String hostCode;
    private Label hostStatusLabel;

    SecureRandom rnd = new SecureRandom();

    public PlayScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        this.hostCode = generateHostCode();

        jdemic.Scenes.SceneUtil.setBackground(root);
        showEntryScreen();
    }

    public StackPane getRoot() {
        return root;
    }

    private void resetScreen() {
        root.getChildren().clear();
        jdemic.Scenes.SceneUtil.setBackground(root);
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
            gc.setStroke(Color.web(CYAN, 0.30));
            gc.setLineWidth(lineW * 3.0);
            gc.strokeRoundRect(x, y, frameW, frameH, 22, 22);
            gc.setStroke(Color.web(CYAN));
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
        field.setStyle(CYBER_INPUT_STYLE);
        return field;
    }
    private void lockToPref(Region region) {
        region.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        region.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    private void showEntryScreen() {
        resetScreen();
        Label title = TextUtil.createText(TITLE_LOBBY, FONT_HKMODULAR, 0.06, YELLOW, root);
        GlowUtil.applyGlow(title, YELLOW, 15);
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.07));
        StackPane panel = PanelUtil.createPanel(0.78, 0.58, CYAN, 2, 15, 10, root);
        panel.setStyle(PANEL_STYLE);
        Label nickLabel = TextUtil.createText("NICKNAME:", FONT_HKMODULAR, 0.028, YELLOW, root);
        TextField nickField = createCyberInput();
        nickField.setText(nickname);
        nickField.prefWidthProperty().bind(root.widthProperty().multiply(0.27));
        nickField.prefHeightProperty().bind(root.heightProperty().multiply(0.065));
        UnaryOperator<TextFormatter.Change> filter = change -> change.getControlNewText().length() <= 16 ? change : null;
        nickField.setTextFormatter(new TextFormatter<>(filter));
        Label errorLabel = TextUtil.createText("ENTER NICKNAME", FONT_HKMODULAR, 0.017, RED, root);
        errorLabel.setVisible(false);
        HBox nickRow = new HBox(nickLabel, nickField);
        nickRow.setAlignment(Pos.CENTER);
        nickRow.spacingProperty().bind(root.widthProperty().multiply(0.02));
        ButtonsUtil hostBtn = new ButtonsUtil("HOST GAME", RED, BLACK, CYAN, CYAN, 2, 15, 15, 0.33, 0.085, 0.024, root);
        ButtonsUtil joinBtn = new ButtonsUtil("JOIN BY CODE", BRIGHT_CYAN, BLACK, CYAN, CYAN, 2, 15, 15, 0.33, 0.085, 0.024, root);
        ButtonsUtil backBtn = new ButtonsUtil("BACK", RED, BLACK, RED, RED, 2, 12, 12, 0.16, 0.07, 0.020, root);
        hostBtn.setOnMouseClicked(e -> {
            String name = nickField.getText().trim();
            if (name.isEmpty()) { errorLabel.setVisible(true); return; }
            errorLabel.setVisible(false); nickname = name; showHostCodeScreen();
        });
        backBtn.setOnMouseClicked(e -> SceneManager.setRoot(new MainMenuScene(stage).getRoot()));
        VBox content = new VBox(nickRow, errorLabel, hostBtn, joinBtn, backBtn);
        content.setAlignment(Pos.TOP_CENTER);
        content.spacingProperty().bind(root.heightProperty().multiply(0.038));
        content.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.10, 0, 0, 0), root.heightProperty()));
        panel.getChildren().add(content);
        root.getChildren().addAll(createFrameLayer(0.78, 0.58, 0), title, panel);
    }

    private void showHostCodeScreen() {
        resetScreen();
        Label title = TextUtil.createText(TITLE_LOBBY, FONT_HKMODULAR, 0.06, YELLOW, root);
        GlowUtil.applyGlow(title, YELLOW, 15);
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.07));
        Label subtitle = TextUtil.createText("HOST GAME", FONT_HKMODULAR, 0.034, RED, root);
        GlowUtil.applyGlow(subtitle, RED, 10);
        StackPane.setAlignment(subtitle, Pos.TOP_CENTER);
        subtitle.translateYProperty().bind(root.heightProperty().multiply(0.15));
        StackPane panel = PanelUtil.createPanel(0.78, 0.58, CYAN, 2, 15, 10, root);
        panel.setStyle(PANEL_STYLE);
        lockToPref(panel);
        Label codeText = TextUtil.createText("CODE:", FONT_HKMODULAR, 0.038, BRIGHT_CYAN, root);
        StackPane codeBox = new StackPane();
        codeBox.prefWidthProperty().bind(root.widthProperty().multiply(0.34));
        codeBox.prefHeightProperty().bind(root.heightProperty().multiply(0.10));
        codeBox.setStyle("-fx-background-color: " + BLACK + "; -fx-border-color: " + CYAN + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        GlowUtil.applyGlow(codeBox, CYAN, 12);
        lockToPref(codeBox);
        Label codeValue = TextUtil.createText(hostCode, FONT_HKMODULAR, 0.042, WHITE, root);
        GlowUtil.applyGlow(codeValue, WHITE, 8);
        codeBox.getChildren().add(codeValue);
        ButtonsUtil copyBtn = new ButtonsUtil("COPY", BRIGHT_CYAN, BLACK, BRIGHT_CYAN, BRIGHT_CYAN, 2, 12, 12, 0.13, 0.07, 0.020, root);
        ButtonsUtil backBtn = new ButtonsUtil("BACK", RED, BLACK, RED, RED, 2, 12, 12, 0.13, 0.07, 0.020, root);
        ButtonsUtil hostBtn = new ButtonsUtil("HOST", BRIGHT_CYAN, BLACK, BRIGHT_CYAN, BRIGHT_CYAN, 2, 12, 12, 0.13, 0.07, 0.020, root);
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
        Label title = TextUtil.createText(TITLE_LOBBY, FONT_HKMODULAR, 0.06, YELLOW, root);
        GlowUtil.applyGlow(title, YELLOW, 15);
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.07));
        StackPane panel = PanelUtil.createPanel(0.70, 0.58, CYAN, 2, 15, 10, root);
        panel.setStyle(PANEL_STYLE);
        lockToPref(panel);
        hostStatusLabel = TextUtil.createText(READY_TEXT, FONT_HKMODULAR, 0.024, YELLOW, root);
        VBox playerList = new VBox(
                createPlayerRow(nickname.toUpperCase(), hostStatusLabel),
                createPlayerRow(PLAYER_PLACEHOLDER, TextUtil.createText(WAITING_TEXT, FONT_HKMODULAR, 0.024, YELLOW, root)),
                createPlayerRow(PLAYER_PLACEHOLDER, TextUtil.createText(WAITING_TEXT, FONT_HKMODULAR, 0.024, YELLOW, root)),
                createPlayerRow(PLAYER_PLACEHOLDER, TextUtil.createText(WAITING_TEXT, FONT_HKMODULAR, 0.024, YELLOW, root))
        );
        playerList.spacingProperty().bind(root.heightProperty().multiply(0.012));
        playerList.setAlignment(Pos.TOP_LEFT);
        VBox chatPanel = new VBox();
        chatPanel.setAlignment(Pos.TOP_LEFT);
        chatPanel.spacingProperty().bind(root.heightProperty().multiply(0.016));
        chatPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.30));
        chatPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.31));
        chatPanel.setStyle("-fx-background-color: rgba(0,0,0,0.70); -fx-border-color: " + CYAN + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        GlowUtil.applyGlow(chatPanel, CYAN, 10);
        lockToPref(chatPanel);
        chatPanel.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.018, root.getWidth() * 0.012, root.getHeight() * 0.018, root.getWidth() * 0.012), root.widthProperty(), root.heightProperty()));
        Label chatTitle = TextUtil.createText("PLAYER CHAT:", FONT_HKMODULAR, 0.028, YELLOW, root);
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false); chatArea.setWrapText(true); chatArea.setFocusTraversable(false);
        chatArea.prefHeightProperty().bind(root.heightProperty().multiply(0.145));
        chatArea.setStyle("-fx-control-inner-background: " + BLACK + "; -fx-text-fill: " + BRIGHT_CYAN + "; -fx-border-color: transparent; -fx-font-family: '" + FONT_HKMODULAR + "';");
        TextField chatInput = createCyberInput();
        chatInput.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        chatInput.prefHeightProperty().bind(root.heightProperty().multiply(0.055));
        lockToPref(chatInput);
        ButtonsUtil sendBtn = new ButtonsUtil("SEND", YELLOW, BLACK, CYAN, CYAN, 2, 10, 10, 0.07, 0.055, 0.019, root);
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
        ButtonsUtil cancelBtn = new ButtonsUtil("CANCEL", RED, BLACK, RED, RED, 2, 12, 12, 0.14, 0.07, 0.020, root);
        ButtonsUtil readyBtn = new ButtonsUtil(READY_TEXT, BRIGHT_CYAN, BLACK, BRIGHT_CYAN, BRIGHT_CYAN, 2, 12, 12, 0.14, 0.07, 0.020, root);
        final boolean[] isReady = {true};
        readyBtn.setOnMouseClicked(e -> {
            isReady[0] = !isReady[0];
            if (isReady[0]) {
                hostStatusLabel.setText(READY_TEXT);
                readyBtn.setText(READY_TEXT);
            } else {
                hostStatusLabel.setText(WAITING_TEXT);
                readyBtn.setText(UNREADY_TEXT);
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
        row.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-border-color: " + CYAN + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        GlowUtil.applyGlow(row, CYAN, 8);
        lockToPref(row);
        Label nameLabel = TextUtil.createText(playerName, FONT_HKMODULAR, 0.024, BRIGHT_CYAN, root);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox inner = new HBox(nameLabel, spacer, statusLabel);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(0, root.getWidth() * 0.010, 0, root.getWidth() * 0.010), root.widthProperty()));
        row.getChildren().add(inner);
        return row;
    }

    private String generateHostCode() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) { sb.append(HOST_CODE_CHARS.charAt(rnd.nextInt(HOST_CODE_CHARS.length()))); }
        return sb.toString();
    }
}
