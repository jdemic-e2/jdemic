package jdemic.ui.GameplayUI;

import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class PawnUI {
    private final StackPane root;
    private final ImageView pawnImage;
    private final String playerName;

    public PawnUI(String playerName, ReadOnlyDoubleProperty mapHeightProperty, String imagePath) {
        this.playerName = playerName;
        this.root = new StackPane();

        Image img = new Image(getClass().getResource(imagePath).toExternalForm());
        pawnImage = new ImageView(img);

        pawnImage.fitHeightProperty().bind(mapHeightProperty.multiply(0.08));
        pawnImage.setPreserveRatio(true);

        DropShadow glow = new DropShadow();
        glow.setRadius(10);
        glow.setColor(Color.CYAN);
        pawnImage.setEffect(glow);

        root.getChildren().add(pawnImage);
        this.root.setMouseTransparent(true);
    }

    public StackPane getNode() {
        return root;
    }

    public void unbindPosition() {
        root.translateXProperty().unbind();
        root.translateYProperty().unbind();
    }

    public void bindToCenter(DoubleExpression cityX, DoubleExpression cityY) {
        root.layoutXProperty().bind(cityX.subtract(pawnImage.fitHeightProperty().divide(2)));
        root.layoutYProperty().bind(cityY.subtract(pawnImage.fitHeightProperty().multiply(0.85)));
    }

    public void bindWithOffset(DoubleExpression cityX, DoubleExpression cityY, double angle, double radiusOffset) {
        root.layoutXProperty().bind(cityX.add(Math.cos(angle) * radiusOffset).subtract(pawnImage.fitHeightProperty().divide(2)));
        root.layoutYProperty().bind(cityY.add(Math.sin(angle) * radiusOffset).subtract(pawnImage.fitHeightProperty().multiply(0.65)));
    }
}