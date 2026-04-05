package jdemic.Scenes.Tutorial;

import javafx.beans.binding.Bindings;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.ui.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TutorialPlayerTurnScene {

    private StackPane root;
    private Stage stage;

    public TutorialPlayerTurnScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }
    private void setActiveButton(ButtonsUtil btn) {
        btn.setStyle("-fx-background-color: black;" + "-fx-text-fill: #00b5d4;" + "-fx-border-color: #00b5d4;" + "-fx-border-width: 2;" + "-fx-background-radius: 15;" + "-fx-border-radius: 15;");
    }

    private void setInactiveButton(ButtonsUtil btn) {
        btn.setStyle("-fx-background-color: black;" +"-fx-text-fill: #888888;" +"-fx-border-color: #444444;" +"-fx-border-width: 2;" +"-fx-background-radius: 15;" +"-fx-border-radius: 15;");
    }
    private static class ActionInfo {
        String title;
        String description;

        ActionInfo(String t, String d) {
            title = t;
            description = d;
        }
    }

    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02,"#00b5d4", Pos.TOP_LEFT,0.03,0.03,null,0);
        TutorialUtil.createTutorialTitle(root,"6. PLAYER TURNS",0.05,"#cfc900",Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);


        Map<String, ActionInfo> actions = new LinkedHashMap<>();
        actions.put("MOVE", new ActionInfo(
                "MOVE",
                "• Move to a connected city\n" +
                        "• Use cards for special travel\n" +
                        "• Some roles have special movement abilities"
        ));

        actions.put("BUILD", new ActionInfo(
                "BUILD",
                "• Build a research station\n" +
                        "• Requires matching city card\n" +
                        "• Some roles can build for free"
        ));

        actions.put("SHARE", new ActionInfo(
                "SHARE",
                "• Exchange cards with another player\n" +
                        "• Must be in the same city\n" +
                        "• Helps discover cures"
        ));

        actions.put("DISCOVER", new ActionInfo(
                "DISCOVER",
                "• Use 5 cards of the same color\n" +
                        "• Must be at a research station\n" +
                        "• Unlocks the cure"
        ));

        HBox mainLayout = new HBox();
        mainLayout.prefWidthProperty().bind(root.widthProperty());
        mainLayout.prefHeightProperty().bind(root.heightProperty());
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.spacingProperty().bind(root.widthProperty().multiply(0.03));

        VBox actionsBox = new VBox();
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.translateXProperty().bind(root.widthProperty().multiply(0.06));
        actionsBox.spacingProperty().bind(root.heightProperty().multiply(0.02));

        for (String key : actions.keySet()) {
            ButtonsUtil btn = new ButtonsUtil(
                    key, "#00b5d4", "black", "#00b5d4", "#00b5d4",
                    2, 15, 15,
                    0.15, 0.07, 0.02,
                    root
            );
            setInactiveButton(btn);

            actionsBox.getChildren().add(btn);
        }

        StackPane leftArea = new StackPane(actionsBox);
        leftArea.setAlignment(Pos.CENTER_LEFT);
        leftArea.prefWidthProperty().bind(root.widthProperty().multiply(0.3));

        StackPane tutorialPanel = TutorialUtil.createTutorialPanel(root);

        StackPane rightArea = new StackPane(tutorialPanel);
        rightArea.prefWidthProperty().bind(root.widthProperty().multiply(0.7));

        rightArea.paddingProperty().bind(Bindings.createObjectBinding(() ->new Insets(0,root.getWidth() * 0.04,0,root.getWidth() * 0.02),root.widthProperty()));

        Label subtitle = TextUtil.createText("EACH PLAYER HAS 4 ACTIONS PER TURN","neotechprobold",0.018,"#00b5d4",root);
        Label actionTitle = TextUtil.createText("SELECT AN ACTION","neotechprobold",0.03,"#00b5d4",root);
        Label actionDescription = TextUtil.createText("Choose one of the actions on the left to see what you can do.","neotechprobold",0.016,"#ffffff", root);
        actionDescription.setWrapText(true);
        actionDescription.setLineSpacing(10);
        actionDescription.maxWidthProperty().bind(tutorialPanel.widthProperty().multiply(0.75));

        VBox content = new VBox(subtitle, actionTitle, actionDescription);
        content.setAlignment(Pos.CENTER);
        content.spacingProperty().bind(root.heightProperty().multiply(0.015));

        StackPane contentWrapper = new StackPane(content);
        contentWrapper.setAlignment(Pos.CENTER);

        contentWrapper.paddingProperty().bind(Bindings.createObjectBinding(() ->new Insets(
                                        root.getHeight() * 0.03,
                                        root.getWidth() * 0.02,
                                        root.getHeight() * 0.03,
                                        root.getWidth() * 0.02
                                ),
                        root.widthProperty(),
                        root.heightProperty()
                )
        );

        tutorialPanel.getChildren().add(contentWrapper);
        Consumer<ActionInfo> updatePanel = info -> {
            actionTitle.setText(info.title);
            actionDescription.setText(info.description);
        };

        int index = 0;
        for (String key : actions.keySet()) {
            ButtonsUtil btn = (ButtonsUtil) actionsBox.getChildren().get(index);
            btn.setOnMouseClicked(e -> {
                updatePanel.accept(actions.get(key));
                actionsBox.getChildren().forEach(n -> {
                    if (n instanceof ButtonsUtil b) {setInactiveButton(b);}
                });
                setActiveButton(btn);
            });
            index++;
        }
        mainLayout.getChildren().addAll(leftArea, rightArea);
        root.getChildren().add(mainLayout);

        TutorialUtil.addBottomButtons(root,tutorialPanel,stage, () -> stage.getScene().setRoot(new TutorialEpidemicScene(stage).getRoot()), () -> stage.getScene().setRoot(new TutorialWinLoseScene(stage).getRoot()));
    }
    public StackPane getRoot() {
        return root;
    }
}