package integration;

import jdemic.GameLogic.PandemicMapGraph;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExampleIT {
    @Test
    void mapInitializesExpectedPandemicBoard() {
        // Este test comprueba que el tablero del juego se genera con las 48 ciudades correctas
        PandemicMapGraph graph = new PandemicMapGraph();

        assertEquals(48, graph.getCityList().size());
        assertTrue(graph.getCityList().stream()
                .anyMatch(city -> "Atlanta".equals(city.getName())));
        assertTrue(graph.getCityList().stream()
                .allMatch(city -> city.getNativeColor() != null));
    }
}