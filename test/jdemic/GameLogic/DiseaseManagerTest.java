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
        assertEquals(92, diseaseManager.getInfectionCubesLeft());
        assertEquals(1, diseaseManager.getOutbreakScore());

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

    private GameManager newManager() {
        CityNode atlanta = new CityNode("Atlanta", DiseaseColor.BLUE, 0.25f, 0.39f);
        Player player = new Player(new PlayerState("Ruben", atlanta));
        return new GameManager(List.of(player));
    }
}
