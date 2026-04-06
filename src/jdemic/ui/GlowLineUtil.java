package jdemic.ui;

import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;

public class GlowLineUtil {

    public static Rectangle createGlowLine(double widthRatio, javafx.scene.layout.Region parent) {
        Rectangle line = new Rectangle();
        line.setHeight(2);
        line.widthProperty().bind(parent.widthProperty().multiply(widthRatio));
        line.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(0.2, Color.web("#00b5d4", 0.3)),
                new Stop(0.5, Color.web("#FFFFFF")),
                new Stop(0.8, Color.web("#00b5d4", 0.3)),
                new Stop(1.0, Color.TRANSPARENT)
        ));
        return line;
    }
}