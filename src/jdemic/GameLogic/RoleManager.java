package jdemic.GameLogic;

import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RoleManager {

    public static void assignRandomRoles(List<PlayerState> players) {
        List<PlayerRoles> availableRoles = Arrays.asList(PlayerRoles.values());
        Collections.shuffle(availableRoles);

        for (int i = 0; i < players.size(); i++) {
            if (i < availableRoles.size()) {
                PlayerRoles assignedRole = availableRoles.get(i);
                players.get(i).setPlayerRole(assignedRole);
                System.out.println("[RoleManager] " + players.get(i).getPlayerName() + " received role: " + assignedRole);
            }
        }
    }
}