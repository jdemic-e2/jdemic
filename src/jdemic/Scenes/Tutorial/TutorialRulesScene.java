package jdemic.Scenes.Tutorial;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jdemic.ui.GlowUtil;
import jdemic.ui.PanelUtil;
import jdemic.ui.TextUtil;

public class TutorialRulesScene {

        private StackPane root;
        private Stage stage;

    public TutorialRulesScene(Stage stage) {
            this.stage = stage;

            root = new StackPane();
        setupBackground();
        setupUI();

        }

    private void setupUI() {
        //TUTORIAL TITLE: use this on each screen for tutorials
        Label title = TextUtil.createText("TUTORIAL", "hkmodular", 0.05, "#cfc900", root);
        title.setTextAlignment(TextAlignment.LEFT);
        GlowUtil.applyGlow(title, "#000000", 10);
        title.translateYProperty().bind(root.heightProperty().multiply(0.06));
        StackPane.setAlignment(title, Pos.TOP_LEFT);
        StackPane.setMargin(title, new Insets(0, 0, 0, 80));
        root.getChildren().add(title);

        //TUTORIAL PANEL: use this on tutorials screens where its needed
        StackPane tutorialPanel = PanelUtil.createPanel(0.85, 0.60, "#00b5d4", 2, 15, 0, root);
        StackPane.setAlignment(tutorialPanel, Pos.CENTER);
        tutorialPanel.setStyle("-fx-background-color: black;" + "-fx-border-color: #ffffff;" + "-fx-border-width: 2 2 2 2;");
        GlowUtil.applyGlow(tutorialPanel, "#00b5d4", 25);
        tutorialPanel.setMaxWidth(Region.USE_PREF_SIZE);
        tutorialPanel.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(tutorialPanel, Pos.CENTER);
        root.getChildren().add(tutorialPanel);
        //good until now



    }

    private void setupBackground() {
        ImageView background = new ImageView(new Image(getClass().getResource("/background.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    public StackPane getRoot() {
            return root;
        }
}
