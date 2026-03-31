package jdemic.GameLogic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.EnumMap;

public class CityNode {

    // Enums
    public enum DiseaseColor 
    {
        BLUE, YELLOW, BLACK, RED
    }

    // Static Board Data
    private final String name;
    private final DiseaseColor nativeColor;
    private final Set<CityNode> connectedCities; // Represents graph edges
    
    // Rendering coordinates for Vulkan
    private float renderX; 
    private float renderY;

    // Dynamic Game State
    private Map<DiseaseColor, Integer> diseaseCubes;
    private boolean hasResearchStation;

    public CityNode(String name, DiseaseColor nativeColor, float renderX, float renderY) 
    {
        this.name = name;
        this.nativeColor = nativeColor;
        this.renderX = renderX;
        this.renderY = renderY;
        
        this.connectedCities = new HashSet<>();
        this.hasResearchStation = false;
        
        // Initialize disease cubes to 0 for all colors
        this.diseaseCubes = new EnumMap<>(DiseaseColor.class);
        for (DiseaseColor color : DiseaseColor.values()) 
        {
            diseaseCubes.put(color, 0);
        }
    }

    // Graph Building Methods
    public void addConnectionMultiple(List<CityNode> neighbors){
        neighbors
            .forEach(e -> this.addConnection(e));
    }

    public void addResearchStation(){
        this.hasResearchStation = true;
    }

    public void removeResearchStation(){
        this.hasResearchStation = false;
    }

    public void addConnection(CityNode neighbor) 
    {
        this.connectedCities.add(neighbor);
        if (!neighbor.getConnectedCities().contains(this)) 
        {
            neighbor.addConnection(this);
        }
    }

    // Gameplay Mechanics Methods

    public boolean addDiseaseCube(DiseaseColor color, int amount) 
    {
        int currentCubes = diseaseCubes.get(color);
        
        if (currentCubes + amount > 3) 
        {
            diseaseCubes.put(color, 3);
            return false;
        } 
        else 
        {
            diseaseCubes.put(color, currentCubes + amount);
            return true;
        }
    }

    public void removeDiseaseCubes(DiseaseColor color, int amount) 
    {
        int currentCubes = diseaseCubes.get(color);
        diseaseCubes.put(color, Math.max(0, currentCubes - amount));
    }

    public void clickEvent(){
        if(!connectedCities.isEmpty()){
            System.out.println("--- " + this.name + " ---");
            connectedCities
                .forEach(e -> System.out.println(e.getName()));
        }
        else{
            System.out.println(this.getName() + " has no neighbours.");
        }
            
    }

    // Getters and Setters
    
    public String getName() { return name; }
    public DiseaseColor getNativeColor() { return nativeColor; }
    public Set<CityNode> getConnectedCities() { return connectedCities; }
    public float getRenderX() { return renderX; }
    public float getRenderY() { return renderY; }
    public boolean hasResearchStation() { return hasResearchStation; }
    
    public int getCubeCount(DiseaseColor color) 
    {
        return diseaseCubes.get(color);
    }
}