package jdemic.ui;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class DotUtil {
    public static Circle createDot(double radiusRatio, String colorHex, double glowRadius, Region parent) {
        Circle dot = new Circle();
        dot.radiusProperty().bind(parent.heightProperty().multiply(radiusRatio));
        dot.setFill(Color.web(colorHex));
        GlowUtil.applyGlow(dot, colorHex, glowRadius);
        return dot;
    }
}