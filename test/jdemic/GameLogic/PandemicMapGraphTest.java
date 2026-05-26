package jdemic.GameLogic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PandemicMapGraphTest {

    @Test
    void shouldCreateFullPandemicCityGraphWithKnownConnections() {
        PandemicMapGraph graph = new PandemicMapGraph();

        CityNode atlanta = graph.getCity("Atlanta");
        CityNode chicago = graph.getCity("Chicago");
        CityNode washington = graph.getCity("Washington");

        assertEquals(48, graph.getCityList().size());
        assertNotNull(atlanta);
        assertNotNull(chicago);
        assertNotNull(washington);
        assertEquals(DiseaseColor.BLUE, atlanta.getNativeColor());
        assertTrue(atlanta.getConnectedCities().contains(chicago));
        assertTrue(chicago.getConnectedCities().contains(atlanta));
        assertTrue(atlanta.getConnectedCities().contains(washington));
    }

    @Test
    void cityDiseaseCubesShouldCapAtThreeAndNeverGoBelowZero() {
        CityNode atlanta = new CityNode("Atlanta", DiseaseColor.BLUE, 0.25f, 0.39f);

        assertTrue(atlanta.addDiseaseCube(DiseaseColor.BLUE, 2));
        assertEquals(2, atlanta.getCubeCount(DiseaseColor.BLUE));

        boolean addedWithoutOutbreak = atlanta.addDiseaseCube(DiseaseColor.BLUE, 2);

        assertFalse(addedWithoutOutbreak);
        assertEquals(3, atlanta.getCubeCount(DiseaseColor.BLUE));

        atlanta.removeDiseaseCubes(DiseaseColor.BLUE, 10);

        assertEquals(0, atlanta.getCubeCount(DiseaseColor.BLUE));
    }
}
