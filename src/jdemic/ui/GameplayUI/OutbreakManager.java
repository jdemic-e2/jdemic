package jdemic.ui.GameplayUI;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import jdemic.GameLogic.GameManager;

public class OutbreakManager {
    private final StackPane root;
    private final GameManager gameManager;
    private HBox trackBox;
    private Circle[] nodes;
    private final int MAX_OUTBREAKS = 8;

    public OutbreakManager(StackPane root, GameManager gameManager) {
        this.root = root;
        this.gameManager = gameManager;
        setupTrack();
        updateTrack();
    }

    private void setupTrack() {
        trackBox = new HBox(8);
        trackBox.setAlignment(Pos.CENTER);
        trackBox.setPickOnBounds(false);
        trackBox.setMouseTransparent(true);

        nodes = new Circle[MAX_OUTBREAKS + 1];
        for (int i = 0; i < nodes.length; i++) {
            Circle node = new Circle();
            node.radiusProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.min(root.getWidth(), root.getHeight()) * 0.015,
                    root.widthProperty(), root.heightProperty()
            ));
            node.setStroke(Color.web("#444444"));
            node.setStrokeWidth(2);
            node.setFill(Color.rgb(30, 30, 30));

            nodes[i] = node;
            trackBox.getChildren().add(node);
        }
    }

    public void updateTrack() {
        if (gameManager == null || gameManager.getState() == null) return;
        int currentOutbreaks = gameManager.getState().getDiseaseManager().getOutbreakScore();

        for (int i = 0; i < nodes.length; i++) {

            if (i <= currentOutbreaks) {
                nodes[i].setFill(Color.web("#ff0000"));
                nodes[i].setStroke(Color.WHITE);

                DropShadow glow = new DropShadow(15, Color.RED);
                nodes[i].setEffect(glow);
            } else {
                nodes[i].setFill(Color.rgb(30, 30, 30));
                nodes[i].setStroke(Color.web("#444444"));
                nodes[i].setEffect(null);
            }
        }
    }

    public HBox getContainer() { return trackBox; }
}