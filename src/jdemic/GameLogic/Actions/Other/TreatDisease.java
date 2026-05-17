package jdemic.GameLogic.Actions.Other;

import jdemic.GameLogic.ServerRelatedClasses.GameState;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.DiseaseColor;
import jdemic.GameLogic.DiseaseManager;
import jdemic.GameLogic.PlayerRoles;

public class TreatDisease extends GameAction {

    private final DiseaseColor targetDisease;

    public TreatDisease(DiseaseColor targetDisease) {
        this.targetDisease = targetDisease;
    }

    @Override
    public boolean isValid(GameState state, PlayerState playerState) {
        CityNode currentCity = playerState.getPlayerCurrentCity();
        if (currentCity == null) {
            return false;
        }
        return currentCity.getCubeCount(targetDisease) > 0;
    }

    @Override
    public void execute(GameState state, PlayerState playerState) {
        if (!isValid(state, playerState)) {
            return;
        }

        CityNode currentCity = playerState.getPlayerCurrentCity();
        DiseaseManager diseaseManager = state.getDiseaseManager();
        boolean isCured = diseaseManager.isCured(targetDisease);
        boolean isMedic = playerState.getPlayerRole() == PlayerRoles.MEDIC;

        if (isMedic || isCured) {
            int cubesToRemove = currentCity.getCubeCount(targetDisease);
            diseaseManager.removeInfectionCubes(currentCity, cubesToRemove);
        } else {
            diseaseManager.removeInfectionCubes(currentCity, 1);
        }
    }
}