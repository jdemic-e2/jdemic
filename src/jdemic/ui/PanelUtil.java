package jdemic.ui;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class PanelUtil {
    public static StackPane createPanel(double widthRatio, double heightRatio, String colorHex, double borderWidth, double glowRadius, double cornerRadius, Region parent) {
        StackPane panel = new StackPane();
        panel.prefWidthProperty().bind(parent.widthProperty().multiply(widthRatio));
        panel.prefHeightProperty().bind(parent.heightProperty().multiply(heightRatio));
        panel.setStyle("-fx-background-color: rgba(0,0,0,0.85);" + "-fx-border-color: " + colorHex + ";" + "-fx-border-width: " + borderWidth + ";" + "-fx-border-radius: " + cornerRadius + ";" + "-fx-background-radius: " + cornerRadius + ";");
        GlowUtil.applyGlow(panel, colorHex, glowRadius);
        return panel;
    }
}