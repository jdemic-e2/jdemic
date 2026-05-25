package jdemic.ui.GameplayUI;

import javafx.animation.*;
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

public class ResearchStationUI {

    private final StackPane root;

    private final ImageView stationImage;

    public ResearchStationUI(ReadOnlyDoubleProperty mapHeight) {
        root = new StackPane();
        Image image = SafeResourceLoader.loadImage("/ResearchStation.png");
        stationImage = new ImageView(image);
        stationImage.setPreserveRatio(true);
        stationImage.fitHeightProperty().bind(mapHeight.multiply(0.045));

        DropShadow glow =new DropShadow();
        glow.setColor(Color.web("#000000"));
        glow.setRadius(25);
        stationImage.setEffect(glow);
        root.getChildren().add(stationImage);
        root.setMouseTransparent(true);
    }

    public StackPane getNode() {return root; }
    public void bindToCity(DoubleExpression cityX, DoubleExpression cityY) {
        root.layoutXProperty().bind(cityX.add(stationImage.fitWidthProperty().multiply(0.35)));
        root.layoutYProperty().bind(cityY.subtract(stationImage.fitHeightProperty().multiply(0.55)));
    }

    public void playBuildAnimation() {
        root.toFront();
        root.setScaleX(0);
        root.setScaleY(0);
        root.setOpacity(0);
        root.setRotate(-25);
        FadeTransition fade =new FadeTransition(Duration.millis(300), root);
        fade.setToValue(1);
        ScaleTransition scale =new ScaleTransition(Duration.millis(500), root);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        RotateTransition rotate = new RotateTransition(Duration.millis(500), root);
        rotate.setToAngle(0);
        ParallelTransition intro =new ParallelTransition(fade, scale, rotate);
        AnimationSpeedUtil.play(intro);
    }
}
