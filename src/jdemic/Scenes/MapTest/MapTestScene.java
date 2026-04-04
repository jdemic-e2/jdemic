package jdemic.Scenes.MapTest;

import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MapTestScene {
    private Stage stage;
    private StackPane root;

    public MapTestScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        setupContent();
    }

    private void setupContent() {
        root.setStyle("-fx-background-color: #050a14;");
    }

    public StackPane getRoot() {
        return root;
    }
}