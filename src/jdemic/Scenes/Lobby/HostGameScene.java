package jdemic.Scenes.Lobby;

import java.util.concurrent.ThreadLocalRandom;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class HostGameScene {
    private final StackPane root;
    private final Stage stage;
    private final String nickname;
    private final String hostCode;

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
        ImageView background = new ImageView(new Image(getClass().getResource("/background.png").toExternalForm()));
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

        ButtonsUtil copyBtn = new ButtonsUtil("COPY", "#00d9ff", "black", "#00d9ff", "#00d9ff", 2, 12, 12, 0.13, 0.07, 0.020, root);
        ButtonsUtil backBtn = new ButtonsUtil("BACK", "#ff2d2d", "black", "#ff2d2d", "#ff2d2d", 2, 12, 12, 0.13, 0.07, 0.020, root);
        ButtonsUtil hostBtn = new ButtonsUtil("HOST", "#00d9ff", "black", "#00d9ff", "#00d9ff", 2, 12, 12, 0.13, 0.07, 0.020, root);

        copyBtn.setOnMouseClicked(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(hostCode);
            Clipboard.getSystemClipboard().setContent(cc);
        });

        backBtn.setOnMouseClicked(e -> SceneManager.switchScene("LOBBY"));
        hostBtn.setOnMouseClicked(e -> stage.getScene().setRoot(new WaitingRoomScene(stage, nickname, hostCode).getRoot()));

        HBox bottomRow = new HBox(backBtn, hostBtn);
        bottomRow.setAlignment(Pos.CENTER);
        bottomRow.spacingProperty().bind(root.widthProperty().multiply(0.16));

        VBox content = new VBox(codeText, codeBox, copyBtn, bottomRow);
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
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
            if (i == 3 && i < 7) sb.append('-');
        }
        return sb.toString();
    }
}
