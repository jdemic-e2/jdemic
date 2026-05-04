package jdemic.ui.GameplayUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import jdemic.GameLogic.CityNode;
import jdemic.ui.ButtonsUtil;
import jdemic.GameLogic.GameManager;

import java.util.List;
import java.util.function.Consumer;

import static javafx.beans.binding.Bindings.createObjectBinding;

public class ActionMenuManager {
    private final StackPane root;
    private final NotificationManager notificationManager;
    private final GameManager gameManager;
    private final String playerName;
    private final Runnable onTurnChangeCallback;
    private VBox actionSubMenu;
    private final List<MenuAction> mainActions;
    private final List<MenuAction> moveActions;
    private final Consumer<String> actionSender;
    private final Consumer<String> validNodesHighlighter;
    private MenuMode menuMode = MenuMode.MAIN;
    private String selectedMovementAction = null;

    private enum MenuMode {
        MAIN,
        MOVE
    }

    private record MenuAction(String label, String packetAction) {}

    public ActionMenuManager(StackPane root, NotificationManager notificationManager, GameManager gameManager) {
        this(root, notificationManager, gameManager, null, null, null, null);
    }

    public ActionMenuManager(StackPane root, NotificationManager notificationManager, GameManager gameManager, Consumer<String> actionSender) {
        this(root, notificationManager, gameManager, actionSender, null, null, null);
    }

    public ActionMenuManager(StackPane root, NotificationManager notificationManager, GameManager gameManager, Consumer<String> actionSender, Consumer<String> validNodesHighlighter) {
        this(root, notificationManager, gameManager, actionSender, validNodesHighlighter, null, null);
    }

    public ActionMenuManager(StackPane root, NotificationManager notificationManager, GameManager gameManager, Consumer<String> actionSender, Consumer<String> validNodesHighlighter, String playerName) {
        this(root, notificationManager, gameManager, actionSender, validNodesHighlighter, playerName, null);
    }

    public ActionMenuManager(StackPane root, NotificationManager notificationManager, GameManager gameManager, Consumer<String> actionSender, Consumer<String> validNodesHighlighter, String playerName, Runnable onTurnChangeCallback) {
        this.root = root;
        this.notificationManager = notificationManager;
        this.gameManager = gameManager;
        this.actionSender = actionSender;
        this.validNodesHighlighter = validNodesHighlighter;
        this.playerName = playerName;
        this.onTurnChangeCallback = onTurnChangeCallback;
        this.mainActions = List.of(
                new MenuAction("DISCOVER", "DISCOVER"),
                new MenuAction("MOVE", "MOVE"),
                new MenuAction("TREAT", "TREAT"),
                new MenuAction("SHARE", "SHARE"),
                new MenuAction("BUILD", "BUILD")
        );
        this.moveActions = List.of(
                new MenuAction("DRIVE/FERRY", "DRIVE_FERRY"),
                new MenuAction("DIRECT FLIGHT", "DIRECT_FLIGHT"),
                new MenuAction("CHARTER FLIGHT", "CHARTER_FLIGHT"),
                new MenuAction("SHUTTLE FLIGHT", "SHUTTLE_FLIGHT"),
                new MenuAction("BACK", null)
        );

        setupSubMenu();
        updateMenuState();
    }

    private void setupSubMenu() {
        actionSubMenu = new VBox(10);
        actionSubMenu.setAlignment(Pos.BOTTOM_LEFT);
        actionSubMenu.setPickOnBounds(false);

        actionSubMenu.paddingProperty().bind(createObjectBinding(() -> {
            double bottomOffset = root.getHeight() * 0.23;
            return new Insets(0, 0, bottomOffset, root.getWidth() * 0.02);
        }, root.widthProperty(), root.heightProperty()));

        root.getChildren().add(actionSubMenu);
    }

    private ButtonsUtil getButtonsUtil(MenuAction action) {
        ButtonsUtil actionBtn = new ButtonsUtil(
                action.label(), "#00ffea", "black", "#00b5d4", "#00ffea",
                2, 5, 5, 0.15, 0.04, 0.009, root
        );

        for (Node node : actionBtn.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setMinWidth(Label.USE_PREF_SIZE);
            }
        }

        actionBtn.setOnMouseClicked(e -> {
            // Check if it's the player's turn before allowing any actions
            if (playerName != null && !isCurrentPlayerTurn()) {
                notificationManager.showNotification("Not your turn! Waiting for " + gameManager.getState().getCurrentPlayer().getPlayerName());
                return;
            }

            if ("MOVE".equals(action.packetAction())) {
                menuMode = MenuMode.MOVE;
                selectedMovementAction = null;
                updateMenuState();
                notificationManager.showNotification("Choose a movement action");
                return;
            }

            if (action.packetAction() == null) {
                menuMode = MenuMode.MAIN;
                selectedMovementAction = null;
                if (validNodesHighlighter != null) {
                    validNodesHighlighter.accept(""); // Clear highlights when going back
                }
                updateMenuState();
                return;
            }

            // Handle movement action toggle
            if (menuMode == MenuMode.MOVE) {
                String actionType = action.packetAction();
                if (selectedMovementAction != null && selectedMovementAction.equals(actionType)) {
                    // Toggle off - deselect this movement action
                    selectedMovementAction = null;
                    if (validNodesHighlighter != null) {
                        validNodesHighlighter.accept("");
                    }
                    notificationManager.showNotification("Movement action cancelled");
                    updateMenuState();
                    return;
                } else if (selectedMovementAction == null) {
                    // Toggle on - select this movement action
                    selectedMovementAction = actionType;
                    if (validNodesHighlighter != null) {
                        validNodesHighlighter.accept(actionType);
                    }
                    notificationManager.showNotification("Selected: " + action.label());
                    updateMenuState();
                    return;
                } else {
                    // Different movement action already selected, switch to this one
                    selectedMovementAction = actionType;
                    if (validNodesHighlighter != null) {
                        validNodesHighlighter.accept(actionType);
                    }
                    notificationManager.showNotification("Switched to: " + action.label());
                    updateMenuState();
                    return;
                }
            }

            // Handle main menu actions
            int left = gameManager.getState().getActionsRemaining();
            if (left > 0) {
                if (actionSender != null) {
                    actionSender.accept(action.packetAction());
                    notificationManager.showNotification("Action " + action.label() + " sent to server");
                } else {
                    gameManager.getState().setActionsRemaining(left - 1);
                    int actionsAfter = gameManager.getState().getActionsRemaining();
                    notificationManager.showNotification("Action " + action.label() + " executed. Moves left: " + actionsAfter);
                    
                    // If actions reached 0, advance to next player
                    if (actionsAfter <= 0) {
                        gameManager.nextTurn();
                        onTurnChanged();
                    } else {
                        updateMenuState();
                    }
                }
            }
        });
        return actionBtn;
    }

    public void updateMenuState() {
        actionSubMenu.getChildren().setAll(createButtonsForCurrentMode());
    }

    public void clearSelectedMovementAction() {
        selectedMovementAction = null;
        updateMenuState();
    }

    /**
     * Called when the turn changes to update the UI and notify the player
     */
    public void onTurnChanged() {
        if (isCurrentPlayerTurn()) {
            notificationManager.showNotification("It's your turn!");
        } else {
            notificationManager.showNotification("Waiting for " + gameManager.getState().getCurrentPlayer().getPlayerName() + "'s turn...");
        }
        updateMenuState();
        
        // Notify external listeners
        if (onTurnChangeCallback != null) {
            onTurnChangeCallback.run();
        }
    }

    private List<ButtonsUtil> createButtonsForCurrentMode() {
        List<MenuAction> visibleActions = menuMode == MenuMode.MOVE
                ? moveActions
                : mainActions.stream()
                    .filter(action -> !"DISCOVER".equals(action.packetAction()) || currentPlayerCanDiscover())
                    .toList();

        return visibleActions.stream()
                .map(action -> {
                    ButtonsUtil button = getButtonsUtil(action);
                    styleActionButton(button, shouldDisable(action));
                    return button;
                })
                .toList();
    }

    private boolean currentPlayerCanDiscover() {
        if (gameManager == null || gameManager.getState().getPlayers().isEmpty()) {
            return false;
        }

        CityNode currentCity = gameManager.getCurrentPlayer().getPlayerCurrentCity();
        return currentCity != null && currentCity.hasResearchStation();
    }

    private boolean shouldDisable(MenuAction action) {
        // Check if it's not the current player's turn
        if (playerName != null && !isCurrentPlayerTurn()) {
            return true; // Disable all buttons for non-current players
        }

        // If in MOVE mode and a movement action is selected
        if (menuMode == MenuMode.MOVE && selectedMovementAction != null) {
            // Only enable the selected movement action
            if (action.packetAction() != null && action.packetAction().equals(selectedMovementAction)) {
                return false; // Enable the selected action
            }
            // Disable all other movement actions and BACK
            if (action.packetAction() != null || action.packetAction() == null) {
                return true; // Disable all others
            }
        }

        // Normal disable logic for other cases
        return action.packetAction() != null
                && gameManager.getState().getActionsRemaining() <= 0;
    }

    /**
     * Checks if the local player is the current player whose turn it is
     */
    private boolean isCurrentPlayerTurn() {
        if (gameManager == null || playerName == null) {
            return true; // If no player name specified, allow actions (local game)
        }
        
        var currentPlayer = gameManager.getState().getCurrentPlayer();
        if (currentPlayer == null) {
            return false;
        }
        
        return playerName.equals(currentPlayer.getPlayerName());
    }

    private void styleActionButton(ButtonsUtil button, boolean disabled) {
        button.setActionDisabled(disabled);
        button.setStyle(disabled
                ? "-fx-background-color: #222222; -fx-background-radius: 15; -fx-opacity: 0.8;"
                : "-fx-background-color: black; -fx-background-radius: 15; -fx-cursor: hand; -fx-opacity: 1.0;");

        for (Node node : button.getChildren()) {
            if (node instanceof Rectangle border) {
                if (disabled) {
                    border.setStroke(Color.GRAY);
                    border.setEffect(null);
                } else {
                    border.setStroke(Color.web("#00b5d4"));
                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                    glow.setColor(Color.web("#00b5d4"));
                    glow.setRadius(15 * 0.8);
                    border.setEffect(glow);
                }
            } else if (node instanceof Label label) {
                label.setTextFill(disabled ? Color.GRAY : Color.web("#00b5d4"));
            }
        }
    }
}
