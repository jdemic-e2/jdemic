package jdemic.GameLogic;

import java.util.ArrayList;
import java.util.List;

public final class PandemicMapGraph
{
    public static final String CITY_SAN_FRANCISCO = "San Francisco";
    public static final String CITY_CHICAGO = "Chicago";
    public static final String CITY_MONTREAL = "Montreal";
    public static final String CITY_NEW_YORK = "New York";
    public static final String CITY_WASHINGTON = "Washington";
    public static final String CITY_ATLANTA = "Atlanta";
    public static final String CITY_LONDON = "London";
    public static final String CITY_MADRID = "Madrid";
    public static final String CITY_PARIS = "Paris";
    public static final String CITY_ESSEN = "Essen";
    public static final String CITY_MILAN = "Milan";
    public static final String CITY_ST_PETERSBURG = "St. Petersburg";
    public static final String CITY_LOS_ANGELES = "Los Angeles";
    public static final String CITY_MEXICO_CITY = "Mexico City";
    public static final String CITY_MIAMI = "Miami";
    public static final String CITY_BOGOTA = "Bogota";
    public static final String CITY_LIMA = "Lima";
    public static final String CITY_SANTIAGO = "Santiago";
    public static final String CITY_SAO_PAULO = "Sao Paulo";
    public static final String CITY_BUENOS_AIRES = "Buenos Aires";
    public static final String CITY_LAGOS = "Lagos";
    public static final String CITY_KHARTOUM = "Khartoum";
    public static final String CITY_KINSHASA = "Kinshasa";
    public static final String CITY_JOHANNESBURG = "Johannesburg";
    public static final String CITY_ALGIERS = "Algiers";
    public static final String CITY_CAIRO = "Cairo";
    public static final String CITY_ISTANBUL = "Istanbul";
    public static final String CITY_MOSCOW = "Moscow";
    public static final String CITY_BAGHDAD = "Baghdad";
    public static final String CITY_RIYADH = "Riyadh";
    public static final String CITY_TEHRAN = "Tehran";
    public static final String CITY_KARACHI = "Karachi";
    public static final String CITY_MUMBAI = "Mumbai";
    public static final String CITY_DELHI = "Delhi";
    public static final String CITY_CHENNAI = "Chennai";
    public static final String CITY_KOLKATA = "Kolkata";
    public static final String CITY_BEIJING = "Beijing";
    public static final String CITY_SEOUL = "Seoul";
    public static final String CITY_TOKYO = "Tokyo";
    public static final String CITY_SHANGHAI = "Shanghai";
    public static final String CITY_HONG_KONG = "Hong Kong";
    public static final String CITY_TAIPEI = "Taipei";
    public static final String CITY_OSAKA = "Osaka";
    public static final String CITY_BANGKOK = "Bangkok";
    public static final String CITY_HO_CHI_MINH_CITY = "Ho Chi Minh City";
    public static final String CITY_MANILA = "Manila";
    public static final String CITY_JAKARTA = "Jakarta";
    public static final String CITY_SYDNEY = "Sydney";

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
                new CityNode(CITY_SAN_FRANCISCO, DiseaseColor.BLUE, 0.14f, 0.34f), //
                new CityNode(CITY_CHICAGO, DiseaseColor.BLUE, 0.24f, 0.30f), //
                new CityNode(CITY_MONTREAL, DiseaseColor.BLUE, 0.30f, 0.26f), //
                new CityNode(CITY_NEW_YORK, DiseaseColor.BLUE, 0.30f, 0.32f), //
                new CityNode(CITY_WASHINGTON, DiseaseColor.BLUE, 0.28f, 0.35f),  //
                new CityNode(CITY_ATLANTA, DiseaseColor.BLUE, 0.25f, 0.39f), //
                new CityNode(CITY_LONDON, DiseaseColor.BLUE, 0.48f, 0.25f), //
                new CityNode(CITY_MADRID, DiseaseColor.BLUE, 0.475f, 0.335f), //
                new CityNode(CITY_PARIS, DiseaseColor.BLUE, 0.49f, 0.28f), //
                new CityNode(CITY_ESSEN, DiseaseColor.BLUE, 0.50f, 0.22f), //
                new CityNode(CITY_MILAN, DiseaseColor.BLUE, 0.52f, 0.32f), //
                new CityNode(CITY_ST_PETERSBURG, DiseaseColor.BLUE, 0.57f, 0.21f), //

                // Yellow cities
                new CityNode(CITY_LOS_ANGELES, DiseaseColor.YELLOW, 0.16f, 0.40f), //
                new CityNode(CITY_MEXICO_CITY, DiseaseColor.YELLOW, 0.21f, 0.49f), //
                new CityNode(CITY_MIAMI, DiseaseColor.YELLOW, 0.26f, 0.45f), //
                new CityNode(CITY_BOGOTA, DiseaseColor.YELLOW, 0.27f, 0.58f), //
                new CityNode(CITY_LIMA, DiseaseColor.YELLOW, 0.27f, 0.71f), //
                new CityNode(CITY_SANTIAGO, DiseaseColor.YELLOW, 0.28f, 0.88f), //
                new CityNode(CITY_SAO_PAULO, DiseaseColor.YELLOW, 0.35f, 0.78f), //
                new CityNode(CITY_BUENOS_AIRES, DiseaseColor.YELLOW, 0.32f, 0.88f), //
                new CityNode(CITY_LAGOS, DiseaseColor.YELLOW, 0.50f, 0.57f), //
                new CityNode(CITY_KHARTOUM, DiseaseColor.YELLOW, 0.57f, 0.52f), //
                new CityNode(CITY_KINSHASA, DiseaseColor.YELLOW, 0.53f, 0.64f), //
                new CityNode(CITY_JOHANNESBURG, DiseaseColor.YELLOW, 0.56f, 0.80f), //

                // Black cities
                new CityNode(CITY_ALGIERS, DiseaseColor.BLACK, 0.50f, 0.38f), //
                new CityNode(CITY_CAIRO, DiseaseColor.BLACK, 0.57f, 0.42f), //
                new CityNode(CITY_ISTANBUL, DiseaseColor.BLACK, 0.57f, 0.34f), //
                new CityNode(CITY_MOSCOW, DiseaseColor.BLACK, 0.60f, 0.25f), //
                new CityNode(CITY_BAGHDAD, DiseaseColor.BLACK, 0.62f, 0.40f), //
                new CityNode(CITY_RIYADH, DiseaseColor.BLACK, 0.63f, 0.47f), //
                new CityNode(CITY_TEHRAN, DiseaseColor.BLACK, 0.64f, 0.36f), //
                new CityNode(CITY_KARACHI, DiseaseColor.BLACK, 0.68f, 0.45f), //
                new CityNode(CITY_MUMBAI, DiseaseColor.BLACK, 0.69f, 0.50f), //
                new CityNode(CITY_DELHI, DiseaseColor.BLACK, 0.72f, 0.42f), //
                new CityNode(CITY_CHENNAI, DiseaseColor.BLACK, 0.70f, 0.56f), //
                new CityNode(CITY_KOLKATA, DiseaseColor.BLACK, 0.74f, 0.47f), //

                // Red cities
                new CityNode(CITY_BEIJING, DiseaseColor.RED, 0.81f, 0.33f), //
                new CityNode(CITY_SEOUL, DiseaseColor.RED, 0.84f, 0.36f), //
                new CityNode(CITY_TOKYO, DiseaseColor.RED, 0.88f, 0.38f), //
                new CityNode(CITY_SHANGHAI, DiseaseColor.RED, 0.83f, 0.41f), //
                new CityNode(CITY_HONG_KONG, DiseaseColor.RED, 0.795f, 0.48f), //
                new CityNode(CITY_TAIPEI, DiseaseColor.RED, 0.83f, 0.47f), //
                new CityNode(CITY_OSAKA, DiseaseColor.RED, 0.86f, 0.39f), //
                new CityNode(CITY_BANGKOK, DiseaseColor.RED, 0.765f, 0.53f), //
                new CityNode(CITY_HO_CHI_MINH_CITY, DiseaseColor.RED, 0.79f, 0.55f), //
                new CityNode(CITY_MANILA, DiseaseColor.RED, 0.83f, 0.51f), //
                new CityNode(CITY_JAKARTA, DiseaseColor.RED, 0.78f, 0.65f), //
                new CityNode(CITY_SYDNEY, DiseaseColor.RED, 0.90f, 0.86f) //
        );
    }
    
    private void createNeighbours(){
        // Blue Cities
        getCity(CITY_SAN_FRANCISCO).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_CHICAGO), getCity(CITY_TOKYO), getCity(CITY_MANILA), getCity(CITY_LOS_ANGELES))));
        getCity(CITY_CHICAGO).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_SAN_FRANCISCO), getCity(CITY_LOS_ANGELES), getCity(CITY_ATLANTA), getCity(CITY_MEXICO_CITY), getCity(CITY_MONTREAL))));
        getCity(CITY_MONTREAL).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_WASHINGTON), getCity(CITY_NEW_YORK), getCity(CITY_CHICAGO))));
        getCity(CITY_NEW_YORK).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_MONTREAL), getCity(CITY_WASHINGTON), getCity(CITY_MADRID), getCity(CITY_LONDON))));
        getCity(CITY_WASHINGTON).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_NEW_YORK), getCity(CITY_MONTREAL), getCity(CITY_ATLANTA), getCity(CITY_MIAMI))));
        getCity(CITY_ATLANTA).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_CHICAGO), getCity(CITY_MIAMI), getCity(CITY_WASHINGTON))));
        getCity(CITY_LONDON).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_NEW_YORK), getCity(CITY_MADRID), getCity(CITY_ESSEN), getCity(CITY_PARIS))));
        getCity(CITY_MADRID).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_LONDON), getCity(CITY_NEW_YORK), getCity(CITY_PARIS), getCity(CITY_SAO_PAULO), getCity(CITY_ALGIERS))));
        getCity(CITY_PARIS).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_LONDON), getCity(CITY_MADRID), getCity(CITY_ESSEN), getCity(CITY_MILAN), getCity(CITY_ALGIERS))));
        getCity(CITY_ESSEN).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_ST_PETERSBURG), getCity(CITY_MILAN), getCity(CITY_LONDON), getCity(CITY_PARIS))));
        getCity(CITY_MILAN).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_ISTANBUL), getCity(CITY_ESSEN), getCity(CITY_PARIS))));
        getCity(CITY_ST_PETERSBURG).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_ISTANBUL), getCity(CITY_ESSEN), getCity(CITY_MOSCOW))));

        // Yellow Cities
        getCity(CITY_LOS_ANGELES).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_SYDNEY), getCity(CITY_MEXICO_CITY), getCity(CITY_SAN_FRANCISCO), getCity(CITY_CHICAGO))));
        getCity(CITY_MEXICO_CITY).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_CHICAGO), getCity(CITY_LOS_ANGELES), getCity(CITY_MIAMI), getCity(CITY_BOGOTA), getCity(CITY_LIMA))));
        getCity(CITY_MIAMI).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_ATLANTA), getCity(CITY_WASHINGTON), getCity(CITY_MEXICO_CITY), getCity(CITY_BOGOTA))));
        getCity(CITY_BOGOTA).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_MIAMI), getCity(CITY_MEXICO_CITY), getCity(CITY_LIMA), getCity(CITY_BUENOS_AIRES), getCity(CITY_SAO_PAULO))));
        getCity(CITY_LIMA).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_MEXICO_CITY), getCity(CITY_BOGOTA), getCity(CITY_SANTIAGO))));
        getCity(CITY_LONDON).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_LIMA))));
        getCity(CITY_SAO_PAULO).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_BUENOS_AIRES), getCity(CITY_BOGOTA), getCity(CITY_MADRID), getCity(CITY_LAGOS))));
        getCity(CITY_BUENOS_AIRES).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_SAO_PAULO), getCity(CITY_BOGOTA))));
        getCity(CITY_LAGOS).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_SAO_PAULO), getCity(CITY_KINSHASA), getCity(CITY_KHARTOUM))));
        getCity(CITY_KHARTOUM).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_LAGOS), getCity(CITY_KINSHASA), getCity(CITY_JOHANNESBURG), getCity(CITY_CAIRO))));
        getCity(CITY_KINSHASA).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_LAGOS), getCity(CITY_KHARTOUM), getCity(CITY_JOHANNESBURG))));
        getCity(CITY_JOHANNESBURG).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_KINSHASA), getCity(CITY_KHARTOUM))));

        // Black Cities
        getCity(CITY_ALGIERS).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_CAIRO), getCity(CITY_MADRID), getCity(CITY_ISTANBUL), getCity(CITY_PARIS))));
        getCity(CITY_CAIRO).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_ALGIERS), getCity(CITY_ISTANBUL), getCity(CITY_BAGHDAD), getCity(CITY_RIYADH), getCity(CITY_KHARTOUM))));
        getCity(CITY_ISTANBUL).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_CAIRO), getCity(CITY_ALGIERS), getCity(CITY_MILAN), getCity(CITY_ST_PETERSBURG), getCity(CITY_MOSCOW), getCity(CITY_BAGHDAD))));
        getCity(CITY_MOSCOW).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_ST_PETERSBURG), getCity(CITY_TEHRAN), getCity(CITY_ISTANBUL))));
        getCity(CITY_BAGHDAD).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_CAIRO), getCity(CITY_RIYADH), getCity(CITY_ISTANBUL), getCity(CITY_TEHRAN), getCity(CITY_KARACHI))));
        getCity(CITY_RIYADH).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_CAIRO), getCity(CITY_BAGHDAD), getCity(CITY_KARACHI))));
        getCity(CITY_TEHRAN).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_MOSCOW), getCity(CITY_DELHI), getCity(CITY_KARACHI), getCity(CITY_BAGHDAD))));
        getCity(CITY_KARACHI).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_TEHRAN), getCity(CITY_BAGHDAD), getCity(CITY_RIYADH), getCity(CITY_MUMBAI), getCity(CITY_DELHI))));
        getCity(CITY_MUMBAI).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_KARACHI), getCity(CITY_DELHI), getCity(CITY_CHENNAI))));
        getCity(CITY_DELHI).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_TEHRAN), getCity(CITY_KARACHI), getCity(CITY_MUMBAI), getCity(CITY_CHENNAI), getCity(CITY_KOLKATA))));
        getCity(CITY_CHENNAI).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_DELHI), getCity(CITY_MUMBAI), getCity(CITY_KOLKATA), getCity(CITY_BANGKOK), getCity(CITY_JAKARTA))));
        getCity(CITY_KOLKATA).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_DELHI), getCity(CITY_CHENNAI), getCity(CITY_BANGKOK), getCity(CITY_HONG_KONG))));

        // Red Cities
        getCity(CITY_BEIJING).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_SHANGHAI), getCity(CITY_SEOUL))));
        getCity(CITY_SEOUL).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_BEIJING), getCity(CITY_SHANGHAI), getCity(CITY_TOKYO))));
        getCity(CITY_TOKYO).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_SEOUL), getCity(CITY_SHANGHAI), getCity(CITY_OSAKA), getCity(CITY_SAN_FRANCISCO))));
        getCity(CITY_SHANGHAI).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_BEIJING), getCity(CITY_SEOUL), getCity(CITY_TOKYO), getCity(CITY_TAIPEI), getCity(CITY_HONG_KONG))));
        getCity(CITY_HONG_KONG).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_KOLKATA), getCity(CITY_BANGKOK), getCity(CITY_HO_CHI_MINH_CITY), getCity(CITY_MANILA), getCity(CITY_TAIPEI), getCity(CITY_SHANGHAI))));
        getCity(CITY_TAIPEI).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_OSAKA), getCity(CITY_SHANGHAI), getCity(CITY_HONG_KONG), getCity(CITY_MANILA))));
        getCity(CITY_OSAKA).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_TOKYO), getCity(CITY_TAIPEI))));
        getCity(CITY_BANGKOK).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_KOLKATA), getCity(CITY_CHENNAI), getCity(CITY_JAKARTA), getCity(CITY_HO_CHI_MINH_CITY), getCity(CITY_HONG_KONG))));
        getCity(CITY_HO_CHI_MINH_CITY).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_JAKARTA), getCity(CITY_MANILA), getCity(CITY_HONG_KONG), getCity(CITY_BANGKOK))));
        getCity(CITY_MANILA).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_SAN_FRANCISCO), getCity(CITY_TAIPEI), getCity(CITY_HONG_KONG), getCity(CITY_HO_CHI_MINH_CITY), getCity(CITY_SYDNEY))));
        getCity(CITY_JAKARTA).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_CHENNAI), getCity(CITY_BANGKOK), getCity(CITY_HO_CHI_MINH_CITY), getCity(CITY_SYDNEY))));
        getCity(CITY_SEOUL).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_BEIJING), getCity(CITY_SHANGHAI), getCity(CITY_TOKYO))));
        getCity(CITY_SYDNEY).addConnectionMultiple(new ArrayList<CityNode>(List.of(getCity(CITY_LOS_ANGELES), getCity(CITY_MANILA), getCity(CITY_JAKARTA))));

    }

    public List<CityNode> getCityList(){
        return this.cityList;
    }

    //find a city and return its reference
    public CityNode getCity(String name){
        CityNode city = cityList
                .stream()
                .filter(e -> name.equals(e.getName()))
                .findAny()
                .orElse(null);
        return city;
    }
}
