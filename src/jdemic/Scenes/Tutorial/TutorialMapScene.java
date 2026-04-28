package jdemic.Scenes.Tutorial;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jdemic.Scenes.MainMenuScene;
import jdemic.Scenes.SceneManager;
import jdemic.ui.*;

public class TutorialMapScene {

    private StackPane root;
    private Stage stage;

    public TutorialMapScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }

    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02, "#00b5d4", Pos.TOP_LEFT, 0.03, 0.03, null, 0);
        TutorialUtil.createTutorialTitle(root,"1. THE MAP",0.05,"#cfc900", Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        ImageView map = new ImageView(new Image(getClass().getResource("/backgroundMap.png").toExternalForm()));
        map.setPreserveRatio(true);
        map.fitWidthProperty().bind(root.widthProperty().multiply(0.75));
        map.translateXProperty().bind(root.widthProperty().multiply(0.1));
        StackPane.setAlignment(map, Pos.CENTER);

        StackPane leftPanel = new StackPane();
        leftPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        leftPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.18));
        leftPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        leftPanel.setStyle("-fx-background-color: black;" + "-fx-border-color: #00b5d4;" +"-fx-border-width: 2;" +"-fx-background-radius: 6;" +"-fx-border-radius: 6;");
        GlowUtil.applyGlow(leftPanel, "#00b5d4", 15);
        StackPane.setAlignment(leftPanel, Pos.CENTER_LEFT);
        leftPanel.translateXProperty().bind(root.widthProperty().multiply(0.06));
        leftPanel.translateYProperty().bind(root.heightProperty().multiply(-0.05));

        Label panelText = TextUtil.createText("FAMILIARIZE\nYOURSELF\nWITH THE MAP","hkmodular",0.013,"#00b5d4",root);
        panelText.setWrapText(true);
        panelText.setAlignment(Pos.CENTER);
        panelText.setTextAlignment(TextAlignment.CENTER);
        panelText.maxWidthProperty().bind(leftPanel.widthProperty().multiply(0.75));

        StackPane panelContent = new StackPane(panelText);
        panelContent.paddingProperty().bind(Bindings.createObjectBinding(() ->
                                new Insets(
                                        leftPanel.getHeight() * 0.15,
                                        leftPanel.getWidth() * 0.1,
                                        leftPanel.getHeight() * 0.15,
                                        leftPanel.getWidth() * 0.1
                                ),
                        leftPanel.widthProperty(),
                        leftPanel.heightProperty()
                )
        );
        leftPanel.getChildren().add(panelContent);
        root.getChildren().addAll(map, leftPanel);

        TutorialUtil.addBottomButtons(root, root, stage, () -> SceneManager.switchScene("MAIN_MENU"), () -> SceneManager.switchScene("TUT_CITIES"));
    }
    public StackPane getRoot() {
        return root;
    }
}