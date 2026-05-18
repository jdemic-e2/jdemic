package jdemic.GameLogic;

import java.util.HashSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

// Not important for the first sprint, will improve later with more correct gameplay options.
public class DiseaseManager {
    private int outbreakScore;
    private int infectionCubesLeft;

    private boolean isBlueCured;
    private boolean isYellowCured;
    private boolean isBlackCured;
    private boolean isRedCured;

    private GameManager gameManager;

    // Start with all 96 cubes and all cures set to false.
    public DiseaseManager(GameManager manager) {
        this.gameManager = manager;
        this.outbreakScore = 0;
        this.infectionCubesLeft = 96;

        this.isBlueCured = false;
        this.isYellowCured = false;
        this.isBlackCured = false;
        this.isRedCured = false;
    }

    public int getOutbreakScore() {
        return this.outbreakScore;
    }

    public void increaseOutbreakScore() {
        this.outbreakScore++;
        if (this.outbreakScore > 7) {//8 is lose
            gameManager.checkLoseCondition();
        }
    }

    public int getInfectionCubesLeft() {
        return this.infectionCubesLeft;
    }

    // Main entry point for adding cubes
    public void addInfectionCubes(CityNode city, int amount) {
        if (city == null || amount <= 0) {
            return;
        }
        if (amount >= this.infectionCubesLeft) {
            this.infectionCubesLeft = 0;
            gameManager.checkLoseCondition();
            return;
        }
        DiseaseColor color = city.getNativeColor();
        Set<CityNode> alreadyOutbroken = new HashSet<>();
        infectCity(city, amount, color, alreadyOutbroken);
    }

    // Recursive method to handle chain reactions
    private void infectCity(CityNode city, int amount, DiseaseColor color, Set<CityNode> alreadyOutbroken) {
        if (this.infectionCubesLeft <= 0) {
            gameManager.checkLoseCondition();
            return;
        }

        int currentCubes = city.getCubeCount(color);
        int cubesToAdd = Math.min(amount, 3 - currentCubes);
        boolean wasAdded = cubesToAdd > 0 && city.addDiseaseCube(color, cubesToAdd);
        this.infectionCubesLeft -= cubesToAdd;
        if (this.infectionCubesLeft <= 0) {
            this.infectionCubesLeft = 0;
            gameManager.checkLoseCondition();
            return;
        }

        if (!wasAdded || currentCubes + amount > 3) {
            // If the city would exceed the 3-cube limit, it outbreaks instead.
            triggerOutbreak(city, color, alreadyOutbroken);
        }
    }

    private void triggerOutbreak(CityNode originCity, DiseaseColor color, Set<CityNode> alreadyOutbroken) {
        // Prevent infinite loops during chain reactions
        if (alreadyOutbroken.contains(originCity)) {
            return;
        }

        alreadyOutbroken.add(originCity);
        increaseOutbreakScore();

        for (CityNode connectedCity : originCity.getConnectedCities()) {
            if (alreadyOutbroken.contains(connectedCity)) {
                continue;
            }
            infectCity(connectedCity, 1, color, alreadyOutbroken);
            if (gameManager.isGameOver()) {
                return;
            }
        }
    }

    public void removeInfectionCubes(CityNode city, int amount) {
        DiseaseColor color = city.getNativeColor();
        city.removeDiseaseCubes(color, amount);

        this.infectionCubesLeft += amount;
        if (this.infectionCubesLeft > 96) {
            this.infectionCubesLeft = 96;
        }
    }

    public boolean isCured(DiseaseColor color) {
        switch (color) {
            case BLUE: return isBlueCured;
            case YELLOW: return isYellowCured;
            case BLACK: return isBlackCured;
            case RED: return isRedCured;
            default: return false;
        }
    }

    public void discoverCure(DiseaseColor color) {
        switch (color) {
            case BLUE: this.isBlueCured = true; break;
            case YELLOW: this.isYellowCured = true; break;
            case BLACK: this.isBlackCured = true; break;
            case RED: this.isRedCured = true; break;
        }
    }

    public boolean areAllCured() {
        return isBlueCured && isYellowCured && isBlackCured && isRedCured;
    }

    public Map<DiseaseColor, Boolean> getCuredDiseases() {
        Map<DiseaseColor, Boolean> curedDiseases = new EnumMap<>(DiseaseColor.class);
        for (DiseaseColor color : DiseaseColor.values()) {
            curedDiseases.put(color, isCured(color));
        }
        return curedDiseases;
    }
}
