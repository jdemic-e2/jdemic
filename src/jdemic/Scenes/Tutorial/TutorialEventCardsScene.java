package jdemic.Scenes.Tutorial;

import javafx.geometry.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;

public class TutorialEventCardsScene {

    private StackPane root;
    private Stage stage;

    public TutorialEventCardsScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }
    private ImageView createCard(String eventCard) {
        return TutorialUtil.createClickableCard(
                root,
                "/eventCards/Event" + eventCard + ".png",
                "TutorialEventCardsScene",
                0.12,
                1.1,
                () -> openEventCard(eventCard)
        );
    }
    private void openEventCard(String eventCardName) {
        String cleanName = eventCardName.replace(" ", "");
        TutorialUtil.openCardOverlay(root, "/eventCards/Event" + cleanName + ".png", eventCardName, "TutorialEventCardsScene");
    }

    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02, "#00b5d4", Pos.TOP_LEFT, 0.03, 0.03, null, 0);
        TutorialUtil.createTutorialTitle(root,"4. EVENT CARDS",0.05,"#cfc900", Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        StackPane leftPanel = TutorialUtil.createInstructionPanel(root, "CLICK ON AN\nEVENT CARD TO SEE\nTHEIR POWERS", 0.010);

        StackPane leftArea = new StackPane(leftPanel);
        leftArea.setAlignment(Pos.CENTER_LEFT);

        leftPanel.translateXProperty().bind(root.widthProperty().multiply(0.05));

        String[] eventCards = {"FirewallLockdown", "SatelliteOverride", "ServerDeployment", "SystemControl", "ThreatScan"};

        HBox cardContainer = new HBox(40);
        cardContainer.setAlignment(Pos.CENTER_LEFT);
        for (String eventCard : eventCards) { cardContainer.getChildren().add(createCard(eventCard));}

        StackPane rightArea = new StackPane();
        rightArea.prefWidthProperty().bind(root.widthProperty().multiply(0.7));
        rightArea.prefHeightProperty().bind(root.heightProperty());
        rightArea.setAlignment(Pos.CENTER_LEFT);

        ScrollPane viewport = TutorialUtil.createHorizontalCardScrollPane(cardContainer);
        viewport.prefWidthProperty().bind(rightArea.widthProperty());
        viewport.prefHeightProperty().bind(rightArea.heightProperty().multiply(0.8));
        viewport.setPadding(new Insets(0, 24, 0, 24));

        viewport.translateXProperty().bind(rightArea.widthProperty().multiply(0.05));

        TutorialUtil.enableHorizontalDragScroll(viewport, cardContainer);

        rightArea.getChildren().add(viewport);

        HBox mainLayout = new HBox();
        mainLayout.prefWidthProperty().bind(root.widthProperty());
        mainLayout.prefHeightProperty().bind(root.heightProperty());

        leftArea.prefWidthProperty().bind(root.widthProperty().multiply(0.3));
        rightArea.prefWidthProperty().bind(root.widthProperty().multiply(0.7));
        mainLayout.getChildren().addAll(leftArea, rightArea);
        root.getChildren().add(mainLayout);

        TutorialUtil.addBottomButtons(root, root, stage, () -> SceneManager.switchScene("TUT_ROLES"), () -> SceneManager.switchScene("TUT_VIRUS"));
    }

    public StackPane getRoot() {
        return root;
    }
}
