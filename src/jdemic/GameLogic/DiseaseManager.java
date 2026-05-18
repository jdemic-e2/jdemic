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

    // Tracks whether a cured disease has been eradicated (no cubes left on the board)
    private boolean isBlueEradicated;
    private boolean isYellowEradicated;
    private boolean isBlackEradicated;
    private boolean isRedEradicated;

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

        this.isBlueEradicated = false;
        this.isYellowEradicated = false;
        this.isBlackEradicated = false;
        this.isRedEradicated = false;
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

        DiseaseColor color = city.getNativeColor();
        // If the disease has been eradicated, it no longer appears on the board
        if (isEradicated(color)) {
            return;
        }

        if (amount >= this.infectionCubesLeft) {
            this.infectionCubesLeft = 0;
            gameManager.checkLoseCondition();
            return;
        }

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

        // If the disease has a discovered cure and now there are no cubes of that
        // color left on the entire board, mark it as eradicated.
        if (isCured(color)) {
            int total = 0;
            for (CityNode c : gameManager.getState().getMap().getCityList()) {
                total += c.getCubeCount(color);
            }
            if (total == 0) {
                switch (color) {
                    case BLUE: this.isBlueEradicated = true; break;
                    case YELLOW: this.isYellowEradicated = true; break;
                    case BLACK: this.isBlackEradicated = true; break;
                    case RED: this.isRedEradicated = true; break;
                }
            }
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

    /**
     * Returns true if the disease color has been eradicated (cured and no cubes
     * remain on the board). When eradicated, no further cubes of that color
     * will be placed by infection effects.
     */
    public boolean isEradicated(DiseaseColor color) {
        switch (color) {
            case BLUE: return isBlueEradicated;
            case YELLOW: return isYellowEradicated;
            case BLACK: return isBlackEradicated;
            case RED: return isRedEradicated;
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
