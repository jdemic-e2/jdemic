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
            new CityNode("New York", CityNode.DiseaseColor.BLUE, 0.30f, 0.32f), //
            new CityNode("Washington", CityNode.DiseaseColor.BLUE, 0.28f, 0.35f),  //
            new CityNode("Atlanta", CityNode.DiseaseColor.BLUE, 0.25f, 0.39f), //
            new CityNode("London", CityNode.DiseaseColor.BLUE, 0.48f, 0.25f), //
            new CityNode("Madrid", CityNode.DiseaseColor.BLUE, 0.475f, 0.335f), //
            new CityNode("Paris", CityNode.DiseaseColor.BLUE, 0.49f, 0.28f), //
            new CityNode("Essen", CityNode.DiseaseColor.BLUE, 0.50f, 0.22f), //
            new CityNode("Milan", CityNode.DiseaseColor.BLUE, 0.52f, 0.32f), //
            new CityNode("St. Petersburg", CityNode.DiseaseColor.BLUE, 0.57f, 0.21f), //

            // Yellow cities
            new CityNode("Los Angeles", CityNode.DiseaseColor.YELLOW, 0.16f, 0.40f), // 
            new CityNode("Mexico City", CityNode.DiseaseColor.YELLOW, 0.21f, 0.49f), //
            new CityNode("Miami", CityNode.DiseaseColor.YELLOW, 0.26f, 0.45f), //
            new CityNode("Bogota", CityNode.DiseaseColor.YELLOW, 0.27f, 0.58f), //
            new CityNode("Lima", CityNode.DiseaseColor.YELLOW, 0.27f, 0.71f), //
            new CityNode("Santiago", CityNode.DiseaseColor.YELLOW, 0.28f, 0.88f), //
            new CityNode("Sao Paulo", CityNode.DiseaseColor.YELLOW, 0.35f, 0.78f), //
            new CityNode("Buenos Aires", CityNode.DiseaseColor.YELLOW, 0.32f, 0.88f), //
            new CityNode("Lagos", CityNode.DiseaseColor.YELLOW, 0.50f, 0.57f), //
            new CityNode("Khartoum", CityNode.DiseaseColor.YELLOW, 0.57f, 0.52f), //
            new CityNode("Kinshasa", CityNode.DiseaseColor.YELLOW, 0.53f, 0.64f), //
            new CityNode("Johannesburg", CityNode.DiseaseColor.YELLOW, 0.56f, 0.80f), //

            // Black cities
            new CityNode("Algiers", CityNode.DiseaseColor.BLACK, 0.50f, 0.38f), //
            new CityNode("Cairo", CityNode.DiseaseColor.BLACK, 0.57f, 0.42f), // 
            new CityNode("Istanbul", CityNode.DiseaseColor.BLACK, 0.57f, 0.34f), //
            new CityNode("Moscow", CityNode.DiseaseColor.BLACK, 0.60f, 0.25f), //
            new CityNode("Baghdad", CityNode.DiseaseColor.BLACK, 0.62f, 0.40f), //
            new CityNode("Riyadh", CityNode.DiseaseColor.BLACK, 0.63f, 0.47f), //
            new CityNode("Tehran", CityNode.DiseaseColor.BLACK, 0.64f, 0.36f), //
            new CityNode("Karachi", CityNode.DiseaseColor.BLACK, 0.68f, 0.45f), //
            new CityNode("Mumbai", CityNode.DiseaseColor.BLACK, 0.69f, 0.50f), //
            new CityNode("Delhi", CityNode.DiseaseColor.BLACK, 0.72f, 0.42f), //
            new CityNode("Chennai", CityNode.DiseaseColor.BLACK, 0.70f, 0.56f), //
            new CityNode("Kolkata", CityNode.DiseaseColor.BLACK, 0.74f, 0.47f), //

            // Red cities
            new CityNode("Beijing", CityNode.DiseaseColor.RED, 0.81f, 0.33f), //
            new CityNode("Seoul", CityNode.DiseaseColor.RED, 0.84f, 0.36f), //
            new CityNode("Tokyo", CityNode.DiseaseColor.RED, 0.88f, 0.38f), //
            new CityNode("Shanghai", CityNode.DiseaseColor.RED, 0.83f, 0.41f), //
            new CityNode("Hong Kong", CityNode.DiseaseColor.RED, 0.795f, 0.48f), //
            new CityNode("Taipei", CityNode.DiseaseColor.RED, 0.83f, 0.47f), //
            new CityNode("Osaka", CityNode.DiseaseColor.RED, 0.86f, 0.39f), //
            new CityNode("Bangkok", CityNode.DiseaseColor.RED, 0.765f, 0.53f), //
            new CityNode("Ho Chi Minh City", CityNode.DiseaseColor.RED, 0.79f, 0.55f), //
            new CityNode("Manila", CityNode.DiseaseColor.RED, 0.83f, 0.51f), //
            new CityNode("Jakarta", CityNode.DiseaseColor.RED, 0.78f, 0.65f), //
            new CityNode("Sydney", CityNode.DiseaseColor.RED, 0.90f, 0.86f) //
        );
    }

    private void createNeighbours(){
        // Blue Cities
        getCity("San Francisco").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Chicago"), getCity("Tokyo"), getCity("Manila"), getCity("Los Angeles"))));
        getCity("Chicago").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("San Francisco"), getCity("Los Angeles"), getCity("Atlanta"), getCity("Mexico City"), getCity("Montreal"))));
        getCity("Montreal").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Washington"), getCity("New York"), getCity("Chicago"))));
        getCity("New York").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Montreal"), getCity("Washington"), getCity("Madrid"), getCity("London"))));
        getCity("Washington").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("New York"), getCity("Montreal"), getCity("Atlanta"), getCity("Miami"))));
        getCity("Atlanta").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Chicago"), getCity("Miami"), getCity("Washington"))));
        getCity("London").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("New York"), getCity("Madrid"), getCity("Essen"), getCity("Paris"))));
        getCity("Madrid").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("London"), getCity("New York"), getCity("Paris"), getCity("Sao Paulo"), getCity("Algiers"))));
        getCity("Paris").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("London"), getCity("Madrid"), getCity("Essen"), getCity("Milan"), getCity("Algiers"))));
        getCity("Essen").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("St. Petersburg"), getCity("Milan"), getCity("London"), getCity("Paris"))));
        getCity("Milan").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Istanbul"), getCity("Essen"), getCity("Paris"))));
        getCity("St. Petersburg").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Istanbul"), getCity("Essen"), getCity("Moscow"))));

        // Yellow Cities
        getCity("Los Angeles").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Sydney"), getCity("Mexico City"), getCity("San Francisco"), getCity("Chicago"))));
        getCity("Mexico City").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Chicago"), getCity("Los Angeles"), getCity("Miami"), getCity("Bogota"), getCity("Lima"))));
        getCity("Miami").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Atlanta"), getCity("Washington"), getCity("Mexico City"), getCity("Bogota"))));
        getCity("Bogota").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Miami"), getCity("Mexico City"), getCity("Lima"), getCity("Buenos Aires"), getCity("Sao Paulo"))));
        getCity("Lima").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Mexico City"), getCity("Bogota"), getCity("Santiago"))));
        getCity("London").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Lima"))));
        getCity("Sao Paulo").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Buenos Aires"), getCity("Bogota"), getCity("Madrid"), getCity("Lagos"))));
        getCity("Buenos Aires").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Sao Paulo"), getCity("Bogota"))));
        getCity("Lagos").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Sao Paulo"), getCity("Kinshasa"), getCity("Khartoum"))));
        getCity("Khartoum").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Lagos"), getCity("Kinshasa"), getCity("Johannesburg"), getCity("Cairo"))));
        getCity("Kinshasa").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Lagos"), getCity("Khartoum"), getCity("Johannesburg"))));
        getCity("Johannesburg").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Kinshasa"), getCity("Khartoum"))));

        // Black Cities
        getCity("Algiers").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Cairo"), getCity("Madrid"), getCity("Istanbul"), getCity("Paris"))));
        getCity("Cairo").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Algiers"), getCity("Istanbul"), getCity("Baghdad"), getCity("Riyadh"), getCity("Khartoum"))));
        getCity("Istanbul").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Cairo"), getCity("Algiers"), getCity("Milan"), getCity("St. Petersburg"), getCity("Moscow"), getCity("Baghdad"))));
        getCity("Moscow").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("St. Petersburg"), getCity("Tehran"), getCity("Istanbul"))));
        getCity("Baghdad").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Cairo"), getCity("Riyadh"), getCity("Istanbul"), getCity("Tehran"), getCity("Karachi"))));
        getCity("Riyadh").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Cairo"), getCity("Baghdad"), getCity("Karachi"))));
        getCity("Tehran").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Moscow"), getCity("Delhi"), getCity("Karachi"), getCity("Baghdad"))));
        getCity("Karachi").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Tehran"), getCity("Baghdad"), getCity("Riyadh"), getCity("Mumbai"), getCity("Delhi"))));
        getCity("Mumbai").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Karachi"), getCity("Delhi"), getCity("Chennai"))));
        getCity("Delhi").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Tehran"), getCity("Karachi"), getCity("Mumbai"), getCity("Chennai"), getCity("Kolkata"))));
        getCity("Chennai").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Delhi"), getCity("Mumbai"), getCity("Kolkata"), getCity("Bangkok"), getCity("Jakarta"))));
        getCity("Kolkata").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Delhi"), getCity("Chennai"), getCity("Bangkok"), getCity("Hong Kong"))));

        // Red Cities
        getCity("Beijing").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Shanghai"), getCity("Seoul"))));
        getCity("Seoul").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Beijing"), getCity("Shanghai"), getCity("Tokyo"))));
        getCity("Tokyo").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Seoul"), getCity("Shanghai"), getCity("Osaka"), getCity("San Francisco"))));
        getCity("Shanghai").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Beijing"), getCity("Seoul"), getCity("Tokyo"), getCity("Taipei"), getCity("Hong Kong"))));
        getCity("Hong Kong").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Kolkata"), getCity("Bangkok"), getCity("Ho Chi Minh City"), getCity("Manila"), getCity("Taipei"), getCity("Shanghai"))));
        getCity("Taipei").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Osaka"), getCity("Shanghai"), getCity("Hong Kong"), getCity("Manila"))));
        getCity("Osaka").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Tokyo"), getCity("Taipei"))));
        getCity("Bangkok").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Kolkata"), getCity("Chennai"), getCity("Jakarta"), getCity("Ho Chi Minh City"), getCity("Hong Kong"))));
        getCity("Ho Chi Minh City").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Jakarta"), getCity("Manila"), getCity("Hong Kong"), getCity("Bangkok"))));
        getCity("Manila").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("San Francisco"), getCity("Taipei"), getCity("Hong Kong"), getCity("Ho Chi Minh City"), getCity("Sydney"))));
        getCity("Jakarta").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Chennai"), getCity("Bangkok"), getCity("Ho Chi Minh City"), getCity("Sydney"))));
        getCity("Seoul").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Beijing"), getCity("Shanghai"), getCity("Tokyo"))));
        getCity("Sydney").addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity("Los Angeles"), getCity("Manila"), getCity("Jakarta"))));

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
