package jdemic.ui.GameplayUI;

import javafx.beans.binding.DoubleExpression;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.beans.property.ReadOnlyDoubleProperty;

public class PawnUI {
    private final Circle pawnNode;
    private final String playerName;

    public PawnUI(String playerName, ReadOnlyDoubleProperty mapHeightProperty, Color pawnColor) {
        this.playerName = playerName;
        this.pawnNode = new Circle();

        // Scale pawn size based on the map height
        this.pawnNode.radiusProperty().bind(mapHeightProperty.multiply(0.008));

        // Visual Style
        this.pawnNode.setFill(pawnColor);
        this.pawnNode.setStroke(Color.BLACK);
        this.pawnNode.setStrokeWidth(1.5);

        // Shadow for better visibility on the dark map
        DropShadow shadow = new DropShadow();
        shadow.setRadius(3);
        shadow.setOffsetY(2);
        this.pawnNode.setEffect(shadow);
    }

    public Circle getNode() {
        return pawnNode;
    }

    public void unbindPosition() {
        pawnNode.centerXProperty().unbind();
        pawnNode.centerYProperty().unbind();
    }

    public void bindToCenter(DoubleExpression cityX, DoubleExpression cityY) {
        pawnNode.centerXProperty().bind(cityX);
        pawnNode.centerYProperty().bind(cityY);
    }

    public void bindWithOffset(DoubleExpression cityX, DoubleExpression cityY, double angle, double radiusOffset) {
        pawnNode.centerXProperty().bind(cityX.add(Math.cos(angle) * radiusOffset));
        pawnNode.centerYProperty().bind(cityY.add(Math.sin(angle) * radiusOffset));
    }
}