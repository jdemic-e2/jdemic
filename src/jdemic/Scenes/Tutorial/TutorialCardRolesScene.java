package jdemic.Scenes.Tutorial;

import javafx.geometry.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;

public class TutorialCardRolesScene {

    private StackPane root;
    private Stage stage;

    public TutorialCardRolesScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }
    private ImageView createCard(String role) {
        return TutorialUtil.createClickableCard(
                root,
                "/roleCards/Role" + role + ".png",
                "TutorialCardRolesScene",
                0.12,
                1.1,
                () -> openRoleCard(role)
        );
    }
    private void openRoleCard(String roleName) {
        String cleanName = roleName.replace(" ", "");
        TutorialUtil.openCardOverlay(root, "/roleCards/Role" + cleanName + ".png", roleName, "TutorialCardRolesScene");
    }

    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02, "#00b5d4",Pos.TOP_LEFT, 0.03, 0.03, null, 0);
        TutorialUtil.createTutorialTitle(root,"3. PLAYER ROLES",0.05,"#cfc900",Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        StackPane leftPanel = TutorialUtil.createInstructionPanel(root, "CLICK ON A\nROLE CARD TO SEE\nTHEIR ABILITIES", 0.010);

        StackPane leftArea = new StackPane();
        leftArea.setAlignment(Pos.CENTER_LEFT);
        leftPanel.translateXProperty().bind(leftArea.widthProperty().multiply(0.1));
        leftArea.getChildren().add(leftPanel);

        String[] roles = {
                "ArtificialIntelligenceAnalyst",
                "EncryptionSpecialist",
                "FirewallSpecialist",
                "IncidentResponder",
                "NetworkController",
                "SystemEngineer",
                "ThreatStrategist"
        };

        HBox cardContainer = new HBox(30);
        cardContainer.setAlignment(Pos.CENTER_LEFT);

        for (String role : roles) { cardContainer.getChildren().add(createCard(role));}

        StackPane rightArea = new StackPane();
        rightArea.setAlignment(Pos.CENTER_LEFT);
        ScrollPane viewport = TutorialUtil.createHorizontalCardScrollPane(cardContainer);
        viewport.prefWidthProperty().bind(rightArea.widthProperty());
        viewport.prefHeightProperty().bind(rightArea.heightProperty().multiply(0.8));
        viewport.setPadding(new Insets(0, 24, 0, 24));

        TutorialUtil.enableHorizontalDragScroll(viewport, cardContainer);
        rightArea.getChildren().add(viewport);

        HBox mainLayout = new HBox();
        mainLayout.prefWidthProperty().bind(root.widthProperty());
        mainLayout.prefHeightProperty().bind(root.heightProperty());

        leftArea.prefWidthProperty().bind(root.widthProperty().multiply(0.3));
        rightArea.prefWidthProperty().bind(root.widthProperty().multiply(0.7));

        mainLayout.getChildren().addAll(leftArea, rightArea);

        root.getChildren().add(mainLayout);

        TutorialUtil.addBottomButtons(root, root, stage, () -> SceneManager.switchScene("TUT_CITIES"), () -> SceneManager.switchScene("TUT_EVENTS"));
    }

    public StackPane getRoot() {
        return root;
    }
}
