package jdemic.GameLogic;

import java.util.List;

public final class PandemicMapGraph
{
    private PandemicMapGraph()
    {
    }

    public static List<CityNode> createNodes()
    {
        return List.of(
            // Blue cities
            new CityNode("San Francisco", CityNode.DiseaseColor.BLUE, 0.08f, 0.33f),
            new CityNode("Chicago", CityNode.DiseaseColor.BLUE, 0.20f, 0.30f),
            new CityNode("Montreal", CityNode.DiseaseColor.BLUE, 0.28f, 0.27f),
            new CityNode("New York", CityNode.DiseaseColor.BLUE, 0.34f, 0.29f),
            new CityNode("Washington", CityNode.DiseaseColor.BLUE, 0.32f, 0.35f),
            new CityNode("Atlanta", CityNode.DiseaseColor.BLUE, 0.25f, 0.39f),
            new CityNode("London", CityNode.DiseaseColor.BLUE, 0.44f, 0.25f),
            new CityNode("Madrid", CityNode.DiseaseColor.BLUE, 0.43f, 0.33f),
            new CityNode("Paris", CityNode.DiseaseColor.BLUE, 0.49f, 0.28f),
            new CityNode("Essen", CityNode.DiseaseColor.BLUE, 0.50f, 0.22f),
            new CityNode("Milan", CityNode.DiseaseColor.BLUE, 0.53f, 0.28f),
            new CityNode("St. Petersburg", CityNode.DiseaseColor.BLUE, 0.57f, 0.18f),

            // Yellow cities
            new CityNode("Los Angeles", CityNode.DiseaseColor.YELLOW, 0.10f, 0.43f),
            new CityNode("Mexico City", CityNode.DiseaseColor.YELLOW, 0.16f, 0.49f),
            new CityNode("Miami", CityNode.DiseaseColor.YELLOW, 0.26f, 0.47f),
            new CityNode("Bogota", CityNode.DiseaseColor.YELLOW, 0.29f, 0.58f),
            new CityNode("Lima", CityNode.DiseaseColor.YELLOW, 0.24f, 0.71f),
            new CityNode("Santiago", CityNode.DiseaseColor.YELLOW, 0.24f, 0.88f),
            new CityNode("Sao Paulo", CityNode.DiseaseColor.YELLOW, 0.35f, 0.75f),
            new CityNode("Buenos Aires", CityNode.DiseaseColor.YELLOW, 0.33f, 0.88f),
            new CityNode("Lagos", CityNode.DiseaseColor.YELLOW, 0.47f, 0.57f),
            new CityNode("Khartoum", CityNode.DiseaseColor.YELLOW, 0.54f, 0.58f),
            new CityNode("Kinshasa", CityNode.DiseaseColor.YELLOW, 0.51f, 0.67f),
            new CityNode("Johannesburg", CityNode.DiseaseColor.YELLOW, 0.54f, 0.86f),

            // Black cities
            new CityNode("Algiers", CityNode.DiseaseColor.BLACK, 0.50f, 0.40f),
            new CityNode("Cairo", CityNode.DiseaseColor.BLACK, 0.56f, 0.42f),
            new CityNode("Istanbul", CityNode.DiseaseColor.BLACK, 0.57f, 0.34f),
            new CityNode("Moscow", CityNode.DiseaseColor.BLACK, 0.62f, 0.25f),
            new CityNode("Baghdad", CityNode.DiseaseColor.BLACK, 0.61f, 0.41f),
            new CityNode("Riyadh", CityNode.DiseaseColor.BLACK, 0.61f, 0.50f),
            new CityNode("Tehran", CityNode.DiseaseColor.BLACK, 0.66f, 0.35f),
            new CityNode("Karachi", CityNode.DiseaseColor.BLACK, 0.67f, 0.45f),
            new CityNode("Mumbai", CityNode.DiseaseColor.BLACK, 0.71f, 0.52f),
            new CityNode("Delhi", CityNode.DiseaseColor.BLACK, 0.72f, 0.42f),
            new CityNode("Chennai", CityNode.DiseaseColor.BLACK, 0.75f, 0.60f),
            new CityNode("Kolkata", CityNode.DiseaseColor.BLACK, 0.77f, 0.49f),

            // Red cities
            new CityNode("Beijing", CityNode.DiseaseColor.RED, 0.81f, 0.31f),
            new CityNode("Seoul", CityNode.DiseaseColor.RED, 0.87f, 0.33f),
            new CityNode("Tokyo", CityNode.DiseaseColor.RED, 0.92f, 0.38f),
            new CityNode("Shanghai", CityNode.DiseaseColor.RED, 0.83f, 0.40f),
            new CityNode("Hong Kong", CityNode.DiseaseColor.RED, 0.82f, 0.50f),
            new CityNode("Taipei", CityNode.DiseaseColor.RED, 0.87f, 0.47f),
            new CityNode("Osaka", CityNode.DiseaseColor.RED, 0.90f, 0.43f),
            new CityNode("Bangkok", CityNode.DiseaseColor.RED, 0.79f, 0.59f),
            new CityNode("Ho Chi Minh City", CityNode.DiseaseColor.RED, 0.82f, 0.63f),
            new CityNode("Manila", CityNode.DiseaseColor.RED, 0.89f, 0.58f),
            new CityNode("Jakarta", CityNode.DiseaseColor.RED, 0.80f, 0.73f),
            new CityNode("Sydney", CityNode.DiseaseColor.RED, 0.90f, 0.86f)
        );
    }
}
