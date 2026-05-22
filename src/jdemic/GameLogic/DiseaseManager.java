package jdemic.GameLogic;

import java.util.HashSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

// Not important for the first sprint, will improve later with more correct gameplay options.
public class DiseaseManager {
    private int outbreakScore;

    // Remaining cubes per color. Pandemic uses 24 cubes per disease color.
    private final java.util.Map<DiseaseColor, Integer> cubesRemaining;
    private static final int CUBES_PER_COLOR = 24;

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

    // Start with CUBES_PER_COLOR per disease and all cures set to false.
    public DiseaseManager(GameManager manager) {
        this.gameManager = manager;
        this.outbreakScore = 0;

        // Initialize per-color supply
        this.cubesRemaining = new EnumMap<>(DiseaseColor.class);
        for (DiseaseColor c : DiseaseColor.values()) {
            this.cubesRemaining.put(c, CUBES_PER_COLOR);
        }

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
        int total = 0;
        for (Integer v : cubesRemaining.values()) {
            total += (v == null) ? 0 : v;
        }
        return total;
    }

    /** Returns how many cubes remain for a specific color */
    public int getCubesLeftForColor(DiseaseColor color) {
        Integer v = cubesRemaining.get(color);
        return v == null ? 0 : v;
    }

    // Main entry point for adding cubes
    public void addInfectionCubes(CityNode city, int amount) {
        if (city == null || amount <= 0) {
            return;
        }

        if (amount >= getInfectionCubesLeft()) {
            for (DiseaseColor c : DiseaseColor.values()) {
                cubesRemaining.put(c, 0);
            }
            gameManager.checkLoseCondition();
            return;
        }

        DiseaseColor color = city.getNativeColor();
        // If the disease has been eradicated, it no longer appears on the board
        if (isEradicated(color)) {
            return;
        }

        int supply = getCubesLeftForColor(color);
        if (supply <= 0) {
            // No cubes of this color remain -> immediate loss condition
            gameManager.checkLoseCondition();
            return;
        }

        // Use a single outbreak-tracker per external call so chain reactions are limited
        Set<CityNode> alreadyOutbroken = new HashSet<>();
        infectCity(city, amount, color, alreadyOutbroken);
    }

    // Recursive method to handle chain reactions
    private void infectCity(CityNode city, int amount, DiseaseColor color, Set<CityNode> alreadyOutbroken) {
        // Check supply for this color before attempting placement
        int available = getCubesLeftForColor(color);
        if (available <= 0) {
            gameManager.checkLoseCondition();
            return;
        }

        int currentCubes = city.getCubeCount(color);

        // If the city already has 3 cubes and an attempt is made to add more, trigger outbreak
        if (currentCubes >= 3 && amount > 0) {
            triggerOutbreak(city, color, alreadyOutbroken);
            return;
        }

        int cubesToAdd = Math.min(amount, 3 - currentCubes);
        if (cubesToAdd > 0) {
            // If supply is insufficient to place all requested cubes, place what's left then lose
            if (available < cubesToAdd) {
                int place = available;
                if (place > 0) {
                    city.addDiseaseCube(color, place);
                    cubesRemaining.put(color, 0);
                    // Notify UI
                    if (gameManager != null) {
                        try { gameManager.notifyStateChange(); } catch (Exception ignored) {}
                    }
                }
                // Supply exhausted while still needing to place cubes -> lose
                gameManager.checkLoseCondition();
                return;
            }

            boolean added = city.addDiseaseCube(color, cubesToAdd);
            cubesRemaining.put(color, available - cubesToAdd);

            // Notify UI of cube addition so visuals update
            if (gameManager != null) {
                try { gameManager.notifyStateChange(); } catch (Exception ignored) {}
            }

            // If we couldn't add the cubes (CityNode capped at 3), outbreak instead
            if (!added) {
                triggerOutbreak(city, color, alreadyOutbroken);
                return;
            }
        }

        // If there's still leftover amount (e.g., adding more than fits), outbreak
        if (currentCubes + amount > 3) {
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

        // Notify manager/UI that an outbreak occurred so widgets can refresh
        if (gameManager != null) {
            try { gameManager.notifyStateChange(); } catch (Exception ignored) {}
        }

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

        // Return removed cubes to the color's supply (cap at CUBES_PER_COLOR)
        int currentLeft = getCubesLeftForColor(color);
        int newLeft = Math.min(CUBES_PER_COLOR, currentLeft + amount);
        cubesRemaining.put(color, newLeft);

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

                // Notify UI that eradication happened
                if (gameManager != null) {
                    try { gameManager.notifyStateChange(); } catch (Exception ignored) {}
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
        // Notify UI that a cure was discovered so widgets can update
        if (gameManager != null) {
            try { gameManager.notifyStateChange(); } catch (Exception ignored) {}
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
