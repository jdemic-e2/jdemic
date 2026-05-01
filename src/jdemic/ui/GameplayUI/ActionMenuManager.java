package jdemic.ui.GameplayUI;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import jdemic.ui.ButtonsUtil;
import jdemic.GameLogic.GameManager;
import static javafx.beans.binding.Bindings.createObjectBinding;

public class ActionMenuManager {
    private final StackPane root;
    private final NotificationManager notificationManager;
    private final GameManager gameManager;
    private VBox actionSubMenu;
    private ButtonsUtil mainActionBtn;
    private boolean isMenuOpen = false;
    private final String[] actions;

    public ActionMenuManager(StackPane root, NotificationManager notificationManager, GameManager gameManager) {
        this.root = root;
        this.notificationManager = notificationManager;
        this.gameManager = gameManager;
        this.actions = new String[]{"MOVE", "BUILD", "SHARE", "FLY"};

        setupSubMenu();
        setupMainButton();
        updateMenuState();
    }

    private void setupSubMenu() {
        actionSubMenu = new VBox(10);
        actionSubMenu.setAlignment(Pos.BOTTOM_LEFT);
        actionSubMenu.setOpacity(0);
        actionSubMenu.setMouseTransparent(true);

        actionSubMenu.paddingProperty().bind(createObjectBinding(() -> {
            double mainButtonHeight = root.getHeight() * 0.07;
            double bottomOffset = mainButtonHeight + 40;
            return new Insets(0, 0, bottomOffset, (root.getWidth()+20)/24);
        }, root.heightProperty()));

        for (String action : actions) {
            actionSubMenu.getChildren().add(getButtonsUtil(action));
        }
        root.getChildren().add(actionSubMenu);
    }

    private void setupMainButton() {
        VBox mainButtonBox = new VBox();
        mainButtonBox.setAlignment(Pos.BOTTOM_LEFT);
        mainButtonBox.setPadding(new Insets(20));
        mainButtonBox.setPickOnBounds(false);

        mainActionBtn = new ButtonsUtil(
                "ACTION MENU", "#00b5d4", "black", "#00b5d4", "#00b5d4",
                3, 15, 15, 0.15, 0.07, 0.012, root
        );
        for (Node node : mainActionBtn.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setMinWidth(Label.USE_PREF_SIZE);
            }
        }

        mainActionBtn.setOnMouseClicked(e -> toggleMenu());
        mainButtonBox.getChildren().add(mainActionBtn);
        root.getChildren().add(mainButtonBox);
    }

    private ButtonsUtil getButtonsUtil(String action) {
        ButtonsUtil actionBtn = new ButtonsUtil(
                action, "#00ffea", "black", "#00b5d4", "#00ffea",
                2, 5, 5, 0.10, 0.04, 0.01, root
        );

        for (Node node : actionBtn.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setMinWidth(Label.USE_PREF_SIZE);
            }
        }

        actionBtn.setOnMouseClicked(e -> {
            int left = gameManager.getState().getActionsRemaining();
            if (left > 0) {
                gameManager.getState().setActionsRemaining(left - 1);
                notificationManager.showNotification("Action " + action + " executed. Moves left: " + (left - 1));
                updateMenuState();
                toggleMenu();
            }
        });
        return actionBtn;
    }

    public void updateMenuState() {
        if (gameManager.getState().getActionsRemaining() <= 0) {
            mainActionBtn.setDisable(true);
            mainActionBtn.setStyle("-fx-background-color: #222222; -fx-background-radius: 15; -fx-opacity: 0.8;");

            for (Node node : mainActionBtn.getChildren()) {
                if (node instanceof Rectangle border) {
                    border.setStroke(Color.GRAY);
                    border.setEffect(null);
                } else if (node instanceof Label label) {
                    label.setTextFill(Color.GRAY);
                }
            }
        } else {
            mainActionBtn.setDisable(false);
            mainActionBtn.setStyle("-fx-background-color: black; -fx-background-radius: 15; -fx-cursor: hand; -fx-opacity: 1.0;");

            for (Node node : mainActionBtn.getChildren()) {
                if (node instanceof Rectangle border) {
                    border.setStroke(Color.web("#00b5d4"));
                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                    glow.setColor(Color.web("#00b5d4"));
                    glow.setRadius(15 * 0.8);
                    border.setEffect(glow);
                } else if (node instanceof Label label) {
                    label.setTextFill(Color.web("#00b5d4"));
                }
            }
        }
    }

    public void toggleMenu() {
        if (gameManager.getState().getActionsRemaining() <= 0 && !isMenuOpen) return;

        isMenuOpen = !isMenuOpen;
        FadeTransition ft = new FadeTransition(Duration.millis(300), actionSubMenu);

        if (isMenuOpen) {
            actionSubMenu.setMouseTransparent(false);
            ft.setFromValue(0);
            ft.setToValue(1);
        } else {
            actionSubMenu.setMouseTransparent(true);
            ft.setFromValue(1);
            ft.setToValue(0);
        }
        ft.play();
    }
}