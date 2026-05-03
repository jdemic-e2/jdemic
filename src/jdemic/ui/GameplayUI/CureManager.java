package jdemic.ui.GameplayUI;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import jdemic.GameLogic.DiseaseColor;
import jdemic.GameLogic.GameManager;

public class CureManager {
    private final StackPane root;
    private final GameManager gameManager;
    private HBox container;
    private Circle[] icons;

    public CureManager(StackPane root, GameManager gameManager)
    {
        this.root = root;
        this.gameManager = gameManager;
        setupUI();
        updateUI();
    }

    private void setupUI()
    {
        container = new HBox(15);
        container.setAlignment(Pos.CENTER);

        DiseaseColor[] colors = DiseaseColor.values();
        icons = new Circle[colors.length];

        for (int i = 0; i < colors.length; i++) {
            Circle icon = new Circle();
            icon.radiusProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.min(root.getWidth(), root.getHeight()) * 0.015,
                    root.widthProperty(), root.heightProperty()
            ));
            icon.setStroke(Color.web("#333333"));
            icon.setFill(Color.rgb(40, 40, 40, 0.5));

            icons[i] = icon;
            container.getChildren().add(icon);
        }
    }

    public void updateUI() {
        if (gameManager == null) return;
        DiseaseColor[] colors = DiseaseColor.values();

        for (int i = 0; i < colors.length; i++) {
            boolean cured = gameManager.getState().getDiseaseManager().isCured(colors[i]);
            if (cured) {
                Color fxColor = getFxColor(colors[i]);
                icons[i].setFill(fxColor);
                icons[i].setStroke(Color.WHITE);
                icons[i].setEffect(new DropShadow(15, fxColor));
            } else {
                icons[i].setFill(Color.rgb(40, 40, 40, 0.5));
                icons[i].setEffect(null);
            }
        }
    }

    private Color getFxColor(DiseaseColor color) {
        return switch (color) {
            case BLUE -> Color.web("#00b5d4");
            case YELLOW -> Color.web("#cfc900");
            case BLACK -> Color.web("#111111");
            case RED -> Color.web("#ff2d2d");
        };
    }

    public HBox getContainer() { return container; }
}
