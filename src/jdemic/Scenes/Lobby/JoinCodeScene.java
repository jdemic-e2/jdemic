package jdemic.Scenes.Lobby;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class JoinCodeScene {
    private final StackPane root;
    private final Stage stage;
    private Label errorLabel;

    public JoinCodeScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        setupBackground();
        setupUI();
    }

    private void setupBackground() {
        ImageView background = new ImageView(new Image(getClass().getResource("/background.png").toExternalForm()));
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

        Label accessLabel = TextUtil.createText("ENTER ACCESS CODE", "hkmodular", 0.03, "#ff0000", root);
        accessLabel.setTextAlignment(TextAlignment.CENTER);

        TextField codeField = new TextField();
        codeField.setMaxWidth(500);
        codeField.setPrefHeight(75);
        codeField.setPromptText("XXXX-YYYY");
        codeField.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: #cfc900; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'hkmodular'; -fx-font-size: 18;");

        ButtonsUtil cancelBtn = new ButtonsUtil("CANCEL", "#ff0000", "black", "#ff0000", "#ff0000", 2, 15, 15, 0.2, 0.1, 0.02, root);
        cancelBtn.setOnMouseClicked(e -> stage.getScene().setRoot(new LobbyScene(stage).getRoot()));

        ButtonsUtil joinBtn = new ButtonsUtil("JOIN", "#00d1ff", "black", "#00d4ff", "#00d4ff", 2, 15, 15, 0.2, 0.1, 0.02, root);
        joinBtn.setOnMouseClicked(e -> {
            String code = codeField.getText();
            if ("1234".equals(code)) {
                System.out.println("Joining with valid code: " + code);
            } else {
                errorLabel.setVisible(true);
            }
        });

        errorLabel = TextUtil.createText("ERROR: INVALID CODE", "hkmodular", 0.025, "#ff0000", root);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setVisible(false);

        VBox inputBox = new VBox(10, accessLabel, codeField);
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
}