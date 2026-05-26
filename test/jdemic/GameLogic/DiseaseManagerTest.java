package jdemic.GameLogic;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiseaseManagerTest {

    @Test
    void shouldTrackInfectionCubesAndOutbreaks() {
        GameManager manager = newManager();
        DiseaseManager diseaseManager = manager.getState().getDiseaseManager();
        CityNode atlanta = manager.getState().getMap().getCity("Atlanta");

        diseaseManager.addInfectionCubes(atlanta, 2);

        assertEquals(2, atlanta.getCubeCount(DiseaseColor.BLUE));
        assertEquals(94, diseaseManager.getInfectionCubesLeft());
        assertEquals(0, diseaseManager.getOutbreakScore());

        diseaseManager.addInfectionCubes(atlanta, 2);

        assertEquals(3, atlanta.getCubeCount(DiseaseColor.BLUE));
        assertEquals(96 - 2 - 1 - atlanta.getConnectedCities().size(), diseaseManager.getInfectionCubesLeft());
        assertEquals(1, diseaseManager.getOutbreakScore());
        for (CityNode connectedCity : atlanta.getConnectedCities()) {
            assertEquals(1, connectedCity.getCubeCount(DiseaseColor.BLUE));
        }

        diseaseManager.removeInfectionCubes(atlanta, 10);

        assertEquals(0, atlanta.getCubeCount(DiseaseColor.BLUE));
        assertEquals(96, diseaseManager.getInfectionCubesLeft());
    }

    @Test
    void shouldTrackCuresByDiseaseColor() {
        DiseaseManager diseaseManager = newManager().getState().getDiseaseManager();

        assertFalse(diseaseManager.areAllCured());

        for (DiseaseColor color : DiseaseColor.values()) {
            assertFalse(diseaseManager.isCured(color));
            diseaseManager.discoverCure(color);
            assertTrue(diseaseManager.isCured(color));
        }

        assertTrue(diseaseManager.areAllCured());
    }

    @Test
    void shouldExposeCuresAsSerializableColorMap() {
        DiseaseManager diseaseManager = newManager().getState().getDiseaseManager();

        diseaseManager.discoverCure(DiseaseColor.BLUE);

        assertTrue(diseaseManager.getCuredDiseases().get(DiseaseColor.BLUE));
        assertFalse(diseaseManager.getCuredDiseases().get(DiseaseColor.RED));
    }

    @Test
    void shouldMarkEradicatedWhenCureDiscoveredAndNoCubesRemain() {
        DiseaseManager diseaseManager = newManager().getState().getDiseaseManager();

        diseaseManager.discoverCure(DiseaseColor.RED);

        assertTrue(diseaseManager.isCured(DiseaseColor.RED));
        assertTrue(diseaseManager.isEradicated(DiseaseColor.RED));
    }

    @Test
    void shouldRecomputeCubeSupplyFromMap() {
        GameManager manager = newManager();
        DiseaseManager diseaseManager = manager.getState().getDiseaseManager();
        CityNode atlanta = manager.getState().getMap().getCity("Atlanta");

        diseaseManager.addInfectionCubes(atlanta, 2);
        assertEquals(22, diseaseManager.getCubesLeftForColor(DiseaseColor.BLUE));

        diseaseManager.recomputeCubeSupplyFromMap();
        assertEquals(22, diseaseManager.getCubesLeftForColor(DiseaseColor.BLUE));
    }

    private GameManager newManager() {
        return new GameManager(List.of(new PlayerState("Ruben")));
    }
}
