package jdemic.ui.GameplayUI.Viruses;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.DiseaseColor;
import jdemic.ui.AnimationSpeedUtil;

import java.util.ArrayList;
import java.util.List;

public class CityVirusGroupUI {
    private final CityNode city;
    private final Pane parentPane;
    private final List<VirusUI> activeViruses = new ArrayList<>();
    private final List<Node> renderedVirusNodes = new ArrayList<>();
    private final ReadOnlyDoubleProperty mapHeight;

    public CityVirusGroupUI(CityNode city, Pane parentPane, ReadOnlyDoubleProperty mapHeight) {
        this.city = city;
        this.parentPane = parentPane;
        this.mapHeight = mapHeight;
        updateVisuals();
    }

    /**
     * Redraws all viruses per city
     */
    public void updateVisuals() {
        activeViruses.forEach(v -> parentPane.getChildren().remove(v.getNode()));
        activeViruses.clear();
        parentPane.getChildren().removeAll(renderedVirusNodes);
        renderedVirusNodes.clear();

        int totalCubes = 0;
        for (DiseaseColor color : DiseaseColor.values()) {
            totalCubes += city.getCubeCount(color);
        }

        if (totalCubes == 0) return;

        DoubleExpression cityX = parentPane.widthProperty().multiply(city.getRenderX());
        DoubleExpression cityY = parentPane.heightProperty().multiply(city.getRenderY());

        int currentIndex = 0;
        for (DiseaseColor color : DiseaseColor.values()) {
            int count = city.getCubeCount(color);
            for (int i = 0; i < count; i++) {
                VirusUI virus = new VirusUI(color, mapHeight);

                virus.bindWithOffset(cityX, cityY, currentIndex, totalCubes, mapHeight);

                Node virusNode = virus.getNode();
                parentPane.getChildren().add(virusNode);
                renderedVirusNodes.add(virusNode);
                currentIndex++;
            }
        }
    }
    public void animateNewVirus(DiseaseColor color) {
        int totalCubes = 0;
        for (DiseaseColor c : DiseaseColor.values()) { totalCubes += city.getCubeCount(c);}
        DoubleExpression cityX = parentPane.widthProperty().multiply(city.getRenderX());
        DoubleExpression cityY =parentPane.heightProperty().multiply(city.getRenderY());
        VirusUI virus =new VirusUI(color, mapHeight);
        virus.bindWithOffset(cityX, cityY, totalCubes - 1, totalCubes, mapHeight);

        Node virusNode = virus.getNode();
        virusNode.setScaleX(0);
        virusNode.setScaleY(0);
        virusNode.setOpacity(0);
        virusNode.toFront();
        parentPane.getChildren().add(virusNode);
        virus.getNode().toFront();
        activeViruses.add(virus);

        ScaleTransition pop =new ScaleTransition(Duration.millis(320), virusNode);
        pop.setToX(1.3);
        pop.setToY(1.3);
        pop.setAutoReverse(true);
        pop.setCycleCount(2);

        FadeTransition fade = new FadeTransition(Duration.millis(180), virusNode);
        fade.setToValue(1);

        ParallelTransition full = new ParallelTransition(pop, fade);
        full.setOnFinished(e -> { updateVisuals(); });
        AnimationSpeedUtil.play(full);
    }
}
