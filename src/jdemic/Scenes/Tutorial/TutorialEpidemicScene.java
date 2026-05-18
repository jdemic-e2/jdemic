package jdemic.Scenes.Tutorial;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.*;

public class TutorialEpidemicScene {

    private StackPane root;
    private Stage stage;

    public TutorialEpidemicScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }
    private void openSystemBreachCard() {
        TutorialUtil.openCardOverlay(root, "/epidemicCard/SystemBreach.png", "System Breach", "TutorialEpidemicScene");
    }
    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02,"#00b5d4",Pos.TOP_LEFT,0.03,0.03,null,0);
        TutorialUtil.createTutorialTitle(root,"6. SYSTEM BREACH",0.05,"#cfc900",Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        HBox mainLayout = new HBox();
        mainLayout.prefWidthProperty().bind(root.widthProperty());
        mainLayout.prefHeightProperty().bind(root.heightProperty());
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.spacingProperty().bind(root.widthProperty().multiply(0.03));

        ImageView card = TutorialUtil.createClickableCard(
                root,
                "/epidemicCard/SystemBreach.png",
                "TutorialEpidemicScene",
                0.18,
                1.05,
                this::openSystemBreachCard
        );

        StackPane leftArea = new StackPane(card);
        leftArea.setAlignment(Pos.CENTER);
        leftArea.prefWidthProperty().bind(root.widthProperty().multiply(0.4));

        StackPane tutorialPanel = TutorialUtil.createTutorialPanel(root);

        StackPane rightArea = new StackPane(tutorialPanel);
        rightArea.prefWidthProperty().bind(root.widthProperty().multiply(0.6));
        rightArea.paddingProperty().bind(Bindings.createObjectBinding(() ->new Insets(0,root.getWidth() * 0.04,0,root.getWidth() * 0.02),root.widthProperty()));

        StackPane.setMargin(tutorialPanel,new Insets(0,root.getWidth() * 0.04,0,root.getWidth() * 0.02));

        VBox content = new VBox();
        content.setAlignment(Pos.TOP_LEFT);
        content.spacingProperty().bind(root.heightProperty().multiply(0.02));

        String TITLE_COLOR = "#00b5d4";
        String TEXT_COLOR = "#ffffff";
        double TITLE_SIZE = 0.022;
        double TEXT_SIZE = 0.016;

        Label title = TextUtil.createText("THIS IS THE SYSTEM BREACH CARD.","neotechprobold",TITLE_SIZE,TITLE_COLOR,root);
        Label description = TextUtil.createText(
                "When a player draws this card:\n\n" +
                        "• Increase: Infection rate goes up\n" +
                        "• Infect: A city gets heavily infected with 3 virus cubes\n" +
                        "• Intensify: Discard pile is shuffled and placed on top of the deck",
                "neotechprobold",
                TEXT_SIZE,
                TEXT_COLOR,
                root
        );
        description.setLineSpacing(10);
        description.setWrapText(true);
        description.maxWidthProperty().bind(tutorialPanel.widthProperty().multiply(0.75));
        content.getChildren().addAll(title, description);

        StackPane contentWrapper = new StackPane(content);
        contentWrapper.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(
                                root.getHeight() * 0.03,
                                root.getWidth() * 0.02,
                                root.getHeight() * 0.03,
                                root.getWidth() * 0.02
                        ),
                        root.widthProperty(),
                        root.heightProperty()
                )
        );
        contentWrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        tutorialPanel.getChildren().add(contentWrapper);

        mainLayout.getChildren().addAll(leftArea, rightArea);
        root.getChildren().add(mainLayout);

        TutorialUtil.addBottomButtons(root, tutorialPanel, stage, () -> SceneManager.switchScene("TUT_VIRUS"), () -> SceneManager.switchScene("TUT_TURN"));
    }

    public StackPane getRoot() {
        return root;
    }
}
