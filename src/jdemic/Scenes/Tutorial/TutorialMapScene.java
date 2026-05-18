package jdemic.Scenes.Tutorial;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;

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

        java.net.URL mapUrl = getClass().getResource("/backgroundMap.png");
        if (mapUrl == null) {
            System.err.println("[TutorialMapScene] Missing resource: /backgroundMap.png");
            return;
        }
        ImageView map = new ImageView(new Image(mapUrl.toExternalForm()));
        map.setPreserveRatio(true);
        map.fitWidthProperty().bind(root.widthProperty().multiply(0.75));
        map.translateXProperty().bind(root.widthProperty().multiply(0.1));
        StackPane.setAlignment(map, Pos.CENTER);

        StackPane leftPanel = TutorialUtil.createInstructionPanel(root, "FAMILIARIZE\nYOURSELF\nWITH THE MAP", 0.013);
        StackPane.setAlignment(leftPanel, Pos.CENTER_LEFT);
        leftPanel.translateXProperty().bind(root.widthProperty().multiply(0.06));
        leftPanel.translateYProperty().bind(root.heightProperty().multiply(-0.05));

        root.getChildren().addAll(map, leftPanel);

        TutorialUtil.addBottomButtons(root, root, stage, () -> SceneManager.switchScene("MAIN_MENU"), () -> SceneManager.switchScene("TUT_CITIES"));
    }
    public StackPane getRoot() {
        return root;
    }
}
