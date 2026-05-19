package jdemic.ui.GameplayUI.Viruses;

import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import jdemic.GameLogic.DiseaseColor;

public class VirusUI {
    private final StackPane container;
    private final Rectangle shape;

    public VirusUI(DiseaseColor diseaseColor, ReadOnlyDoubleProperty mapHeightProperty) {
        this.container = new StackPane();
        this.shape = new Rectangle();

        DoubleExpression size = mapHeightProperty.multiply(0.008);
        this.shape.widthProperty().bind(size);
        this.shape.heightProperty().bind(size);

        this.shape.setRotate(45);
        this.shape.setFill(getFxColor(diseaseColor));
        this.shape.setStroke(Color.BLACK);
        this.shape.setStrokeWidth(1.0);

        DropShadow glow = new DropShadow(10, getFxColor(diseaseColor));
        this.shape.setEffect(glow);

        this.container.getChildren().add(shape);
        this.container.setMouseTransparent(true);
    }

    public Node getNode() {
        return container;
    }

    public void bindWithOffset(DoubleExpression cityX, DoubleExpression cityY, int index, int total, ReadOnlyDoubleProperty mapHeight) {
        DoubleExpression size = mapHeight.multiply(0.01);
        DoubleExpression spacingX = size.multiply(0.9); // Horizontal offset
        DoubleExpression spacingY = size.multiply(0.8); // Vertical offset
        DoubleExpression halfSize = size.divide(2);

        DoubleExpression globalOffsetX = mapHeight.multiply(0.015);
        DoubleExpression globalOffsetY = mapHeight.multiply(0.015);

        // Anchor point
        DoubleExpression anchorX = cityX.add(globalOffsetX);
        DoubleExpression anchorY = cityY.subtract(globalOffsetY);

        double targetX = 0;
        double targetY = 0;

        if (total == 1) {
            targetX = 0;
            targetY = 0;
        }
        else if (total == 2) {
            targetX = (index == 0) ? -0.6 : 0.6;
            targetY = 0;
        }
        else {
            if (index == 0) {
                targetX = 0;
                targetY = -0.5;
            } else if (index == 1) {
                targetX = -0.6;
                targetY = 0.5;
            } else if (index == 2) {
                targetX = 0.6;
                targetY = 0.5;
            } else {
                targetX = (index - 1) * 0.5;
                targetY = 1.0;
            }
        }

        container.translateXProperty().bind(anchorX.add(spacingX.multiply(targetX)).subtract(halfSize));
        container.translateYProperty().bind(anchorY.add(spacingY.multiply(targetY)).subtract(halfSize));
    }

    private Color getFxColor(DiseaseColor color) {
        return switch (color) {
            case BLUE -> Color.web("#00b5d4");
            case YELLOW -> Color.web("#cfc900");
            case BLACK -> Color.web("#333333");
            case RED -> Color.web("#ff2d2d");
        };
    }
}

    /*
    Test method for MapTestScene

    private void setupRandomViruses(Pane mapPane) {
        Random random = new Random();
        DiseaseColor[] colors = DiseaseColor.values();

        for (CityNode city : gameManager.getState().getMap().getCityList()) {
            DiseaseColor randomColor = colors[random.nextInt(colors.length)];
            int amount = random.nextInt(3) + 1;
            city.addDiseaseCube(randomColor, amount);
            new CityVirusGroupUI(city, mapPane, root.heightProperty());
        }
    }
     */