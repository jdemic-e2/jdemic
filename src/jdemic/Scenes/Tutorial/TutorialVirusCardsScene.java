package jdemic.Scenes.Tutorial;

import javafx.geometry.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;

public class TutorialVirusCardsScene {

    private StackPane root;
    private Stage stage;

    public TutorialVirusCardsScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }
    private ImageView createCard(String eventCard) {
        return TutorialUtil.createClickableCard(
                root,
                "/virusCards/VirusCard" + eventCard + ".png",
                "TutorialVirusCardsScene",
                0.12,
                1.1,
                () -> openEventCard(eventCard)
        );
    }
    private void openEventCard(String eventCardName) {
        String cleanName = eventCardName.replace(" ", "");
        TutorialUtil.openCardOverlay(root, "/virusCards/VirusCard" + cleanName + ".png", eventCardName, "TutorialVirusCardsScene");
    }

    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02, "#00b5d4", Pos.TOP_LEFT, 0.03, 0.03, null, 0);
        TutorialUtil.createTutorialTitle(root,"5. VIRUS CARDS",0.05,"#cfc900", Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        StackPane leftPanel = TutorialUtil.createInstructionPanel(root, "CLICK ON AN\nVIRUS CARD TO SEE\nTHEIR POWERS", 0.010);

        StackPane leftArea = new StackPane(leftPanel);
        leftArea.setAlignment(Pos.CENTER_LEFT);
        leftPanel.translateXProperty().bind(root.widthProperty().multiply(0.05));

        String[] virusCards = { "Conficker", "Stuxnet", "WannaCry", "Zeus"};

        HBox cardContainer = new HBox(40);
        cardContainer.setAlignment(Pos.CENTER_LEFT);

        for (String virusCard : virusCards) {cardContainer.getChildren().add(createCard(virusCard));}

        StackPane rightArea = new StackPane();
        rightArea.prefWidthProperty().bind(root.widthProperty().multiply(0.7));
        rightArea.prefHeightProperty().bind(root.heightProperty());
        rightArea.setAlignment(Pos.CENTER_LEFT);

        StackPane viewport = new StackPane(cardContainer);
        viewport.setAlignment(Pos.CENTER_LEFT);
        viewport.prefWidthProperty().bind(rightArea.widthProperty());
        viewport.prefHeightProperty().bind(rightArea.heightProperty());

        TutorialUtil.clipToBounds(viewport);
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

        TutorialUtil.addBottomButtons(root, root, stage, () -> SceneManager.switchScene("TUT_EVENTS"), () -> SceneManager.switchScene("TUT_EPIDEMIC"));
    }

    public StackPane getRoot() {
        return root;
    }
}
