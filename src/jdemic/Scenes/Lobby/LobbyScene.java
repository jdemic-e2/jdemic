package jdemic.Scenes.Lobby;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class LobbyScene {
    private final StackPane root;
    private final Stage stage;

    public LobbyScene(Stage stage) {
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
        // Sadece arka planın üzerinde üstte konumlanan "LOBBY" başlığı gösterilir.
        Label title = TextUtil.createText("LOBBY", "hkmodular", 0.05, "#cfc900", root);
        title.setTextAlignment(TextAlignment.CENTER);
        StackPane.setAlignment(title, javafx.geometry.Pos.TOP_CENTER);
        title.translateYProperty().bind(root.heightProperty().multiply(0.05));
        GlowUtil.applyGlow(title, "#cfc900", 7);

        TextField nicknameField = new TextField("Newbie");
        nicknameField.setMaxWidth(300);
        nicknameField.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: #cfc900; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'hkmodular'; -fx-font-size: 18;");

        Label nameLabel = TextUtil.createText("NICKNAME:", "hkmodular", 0.026, "#00b5d4", root);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        GlowUtil.applyGlow(nameLabel, "#00b5d4", 6);

        Label errorLabel = TextUtil.createText("ENTER NICKNAME", "hkmodular", 0.02, "#ff2d2d", root);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setVisible(false);

        ButtonsUtil hostBtn = new ButtonsUtil("HOST GAME", "#ff0000", "black", "#ff0000", "#ff0000", 2, 15, 15, 0.40, 0.10, 0.03, root);
        hostBtn.setOnMouseClicked(e -> {
            String nickname = nicknameField.getText().trim();
            if (nickname.isEmpty()) {
                errorLabel.setVisible(true);
            } else {
                errorLabel.setVisible(false);
                stage.getScene().setRoot(new HostGameScene(stage, nickname).getRoot());
            }
        });

        ButtonsUtil joinBtn = new ButtonsUtil("JOIN BY CODE", "#00d1ff", "black", "#00d4ff", "#00d4ff", 2, 15, 15, 0.40, 0.10, 0.03, root);
        joinBtn.setOnMouseClicked(e -> SceneManager.switchScene("JOIN_CODE"));

        VBox centerBox = new VBox(18, nameLabel, nicknameField, errorLabel, hostBtn, joinBtn);
        centerBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        StackPane.setAlignment(centerBox, javafx.geometry.Pos.TOP_CENTER);
        centerBox.translateYProperty().bind(root.heightProperty().multiply(0.25));

        ButtonsUtil backBtn = new ButtonsUtil("BACK", "#ff0000", "black", "#ff0000", "#ff0000", 2, 12, 12, 0.15, 0.06, 0.02, root);
        StackPane.setAlignment(backBtn, javafx.geometry.Pos.BOTTOM_CENTER);
        backBtn.translateYProperty().bind(root.heightProperty().multiply(-0.08));
        backBtn.setOnMouseClicked(e -> SceneManager.switchScene("MAIN_MENU"));

        root.getChildren().addAll(title, centerBox, backBtn);
    }

    public StackPane getRoot() {
        return root;
    }
}