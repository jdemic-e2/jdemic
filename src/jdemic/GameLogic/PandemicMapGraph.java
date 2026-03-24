package jdemic.GameLogic;

import java.util.ArrayList;
import java.util.List;

public final class PandemicMapGraph
{
    private List<CityNode> cityList;
    public PandemicMapGraph()
    {
        this.cityList = createNodes();
        createNeighbours();
    }

    public static List<CityNode> createNodes()
    {
        return List.of(
            // Blue cities
            new CityNode("San Francisco", CityNode.DiseaseColor.BLUE, 0.14f, 0.34f), //
            new CityNode("Chicago", CityNode.DiseaseColor.BLUE, 0.24f, 0.30f), // 
            new CityNode("Montreal", CityNode.DiseaseColor.BLUE, 0.30f, 0.26f), // 
            new CityNode("New York", CityNode.DiseaseColor.BLUE, 0.30f, 0.32f),
            new CityNode("Washington", CityNode.DiseaseColor.BLUE, 0.28f, 0.35f),
            new CityNode("Atlanta", CityNode.DiseaseColor.BLUE, 0.25f, 0.39f),
            new CityNode("London", CityNode.DiseaseColor.BLUE, 0.48f, 0.25f),
            new CityNode("Madrid", CityNode.DiseaseColor.BLUE, 0.475f, 0.335f),
            new CityNode("Paris", CityNode.DiseaseColor.BLUE, 0.49f, 0.28f),
            new CityNode("Essen", CityNode.DiseaseColor.BLUE, 0.50f, 0.22f),
            new CityNode("Milan", CityNode.DiseaseColor.BLUE, 0.52f, 0.32f),
            new CityNode("St. Petersburg", CityNode.DiseaseColor.BLUE, 0.57f, 0.21f),

            // Yellow cities
            new CityNode("Los Angeles", CityNode.DiseaseColor.YELLOW, 0.16f, 0.40f),
            new CityNode("Mexico City", CityNode.DiseaseColor.YELLOW, 0.21f, 0.49f),
            new CityNode("Miami", CityNode.DiseaseColor.YELLOW, 0.26f, 0.45f),
            new CityNode("Bogota", CityNode.DiseaseColor.YELLOW, 0.27f, 0.58f),
            new CityNode("Lima", CityNode.DiseaseColor.YELLOW, 0.27f, 0.71f),
            new CityNode("Santiago", CityNode.DiseaseColor.YELLOW, 0.28f, 0.88f),
            new CityNode("Sao Paulo", CityNode.DiseaseColor.YELLOW, 0.35f, 0.78f),
            new CityNode("Buenos Aires", CityNode.DiseaseColor.YELLOW, 0.32f, 0.88f),
            new CityNode("Lagos", CityNode.DiseaseColor.YELLOW, 0.50f, 0.57f),
            new CityNode("Khartoum", CityNode.DiseaseColor.YELLOW, 0.57f, 0.52f),
            new CityNode("Kinshasa", CityNode.DiseaseColor.YELLOW, 0.53f, 0.64f),
            new CityNode("Johannesburg", CityNode.DiseaseColor.YELLOW, 0.56f, 0.80f),

            // Black cities
            new CityNode("Algiers", CityNode.DiseaseColor.BLACK, 0.50f, 0.38f),
            new CityNode("Cairo", CityNode.DiseaseColor.BLACK, 0.57f, 0.42f),
            new CityNode("Istanbul", CityNode.DiseaseColor.BLACK, 0.57f, 0.34f),
            new CityNode("Moscow", CityNode.DiseaseColor.BLACK, 0.60f, 0.25f),
            new CityNode("Baghdad", CityNode.DiseaseColor.BLACK, 0.62f, 0.40f),
            new CityNode("Riyadh", CityNode.DiseaseColor.BLACK, 0.63f, 0.47f),
            new CityNode("Tehran", CityNode.DiseaseColor.BLACK, 0.64f, 0.36f),
            new CityNode("Karachi", CityNode.DiseaseColor.BLACK, 0.68f, 0.45f),
            new CityNode("Mumbai", CityNode.DiseaseColor.BLACK, 0.69f, 0.50f),
            new CityNode("Delhi", CityNode.DiseaseColor.BLACK, 0.72f, 0.42f),
            new CityNode("Chennai", CityNode.DiseaseColor.BLACK, 0.70f, 0.56f),
            new CityNode("Kolkata", CityNode.DiseaseColor.BLACK, 0.74f, 0.47f),

            // Red cities
            new CityNode("Beijing", CityNode.DiseaseColor.RED, 0.81f, 0.33f),
            new CityNode("Seoul", CityNode.DiseaseColor.RED, 0.84f, 0.36f),
            new CityNode("Tokyo", CityNode.DiseaseColor.RED, 0.88f, 0.38f),
            new CityNode("Shanghai", CityNode.DiseaseColor.RED, 0.83f, 0.41f),
            new CityNode("Hong Kong", CityNode.DiseaseColor.RED, 0.795f, 0.48f),
            new CityNode("Taipei", CityNode.DiseaseColor.RED, 0.83f, 0.47f),
            new CityNode("Osaka", CityNode.DiseaseColor.RED, 0.86f, 0.39f),
            new CityNode("Bangkok", CityNode.DiseaseColor.RED, 0.765f, 0.53f),
            new CityNode("Ho Chi Minh City", CityNode.DiseaseColor.RED, 0.79f, 0.55f),
            new CityNode("Manila", CityNode.DiseaseColor.RED, 0.83f, 0.51f),
            new CityNode("Jakarta", CityNode.DiseaseColor.RED, 0.78f, 0.65f),
            new CityNode("Sydney", CityNode.DiseaseColor.RED, 0.90f, 0.86f)
        );
    }

    private void createNeighbours(){
        getCity("San Francisco").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Chicago"), getCity("Tokyo"), getCity("Manila"), getCity("Los Angeles"))));
        getCity("Chicago").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("San Francisco"), getCity("Los Angeles"), getCity("Atlanta"), getCity("Mexico City"), getCity("Montreal"))));
        getCity("Montreal").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Washington"), getCity("New York"), getCity("Chicago"))));
        
    }   

    public List<CityNode> getCityList(){
        return this.cityList;
    }

    public CityNode getCity(String name){
        CityNode city = cityList
                            .stream()
                            .filter(e -> name.equals(e.getName()))
                            .findAny()
                            .orElse(null);
        return city;
    }
}
