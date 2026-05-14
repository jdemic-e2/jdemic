package jdemic.ui.GameplayUI.Viruses;

import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.layout.Pane;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.DiseaseColor;

import java.util.ArrayList;
import java.util.List;

public class CityVirusGroupUI {
    private final CityNode city;
    private final Pane parentPane;
    private final List<VirusUI> activeViruses = new ArrayList<>();
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

                parentPane.getChildren().add(virus.getNode());
                activeViruses.add(virus);
                currentIndex++;
            }
        }
    }
}