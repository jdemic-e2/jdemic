package jdemic.ui.GameplayUI;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import jdemic.ui.AnimationSpeedUtil;
import jdemic.ui.SafeResourceLoader;

public class PawnUI {
    private final StackPane root;
    private final ImageView pawnImage;
    private final String playerName;

    public PawnUI(String playerName, ReadOnlyDoubleProperty mapHeightProperty, String imagePath) {
        this.playerName = playerName;
        this.root = new StackPane();

        Image img = SafeResourceLoader.loadImage(imagePath);
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
        root.layoutXProperty().unbind();
        root.layoutYProperty().unbind();
    }

    public void bindToCenter(DoubleExpression cityX, DoubleExpression cityY) {
        root.layoutXProperty().bind(cityX.subtract(pawnImage.fitHeightProperty().divide(2)));
        root.layoutYProperty().bind(cityY.subtract(pawnImage.fitHeightProperty().multiply(0.85)));
    }

    public void bindWithOffset(DoubleExpression cityX, DoubleExpression cityY, double angle, double radiusOffset) {
        root.layoutXProperty().bind(cityX.add(Math.cos(angle) * radiusOffset).subtract(pawnImage.fitHeightProperty().divide(2)));
        root.layoutYProperty().bind(cityY.add(Math.sin(angle) * radiusOffset).subtract(pawnImage.fitHeightProperty().multiply(0.65)));
    }
    
    public void animateMoveTo(double targetX, double targetY, Runnable onFinished) {
        TranslateTransition move = new TranslateTransition(Duration.millis(850),pawnImage);
        move.setToX(targetX);
        move.setToY(targetY);
        move.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition pulse =new ScaleTransition(Duration.millis(420), pawnImage);
        pulse.setToX(1.18);
        pulse.setToY(1.18);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        ParallelTransition full = new ParallelTransition(move,pulse);
        full.setOnFinished(e -> {
            pawnImage.setTranslateX(0);
            pawnImage.setTranslateY(0);
            if (onFinished != null) { onFinished.run(); }
        });

        AnimationSpeedUtil.play(full);
    }

    public void animateMoveFrom(double startX, double startY, Runnable onFinished) {
        pawnImage.setTranslateX(startX);
        pawnImage.setTranslateY(startY);

        TranslateTransition move = new TranslateTransition(Duration.millis(850), pawnImage);
        move.setToX(0);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition pulse = new ScaleTransition(Duration.millis(420), pawnImage);
        pulse.setToX(1.18);
        pulse.setToY(1.18);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);

        ParallelTransition full = new ParallelTransition(move, pulse);
        full.setOnFinished(e -> {
            pawnImage.setTranslateX(0);
            pawnImage.setTranslateY(0);
            if (onFinished != null) { onFinished.run(); }
        });

        AnimationSpeedUtil.play(full);
    }

    public ImageView getImage() { return pawnImage; }
}
