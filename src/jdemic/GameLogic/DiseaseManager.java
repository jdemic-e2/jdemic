package jdemic.GameLogic;


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
    }

    public int getInfectionCubesLeft() {
        return this.infectionCubesLeft;
    }

    // Adds X amount of infection cubes to a certain city.
    public void addInfectionCubes(CityNode city, int amount) {
        DiseaseColor color = city.getNativeColor();

        boolean wasAdded = city.addDiseaseCube(color, amount);

        if (!wasAdded) {
            increaseOutbreakScore();
        }

        this.infectionCubesLeft -= amount;

        if (this.infectionCubesLeft <= 0) {
            this.infectionCubesLeft = 0;
            gameManager.checkLoseCondition();
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
}