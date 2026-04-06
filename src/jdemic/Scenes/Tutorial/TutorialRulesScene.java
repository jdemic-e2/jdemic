package jdemic.Scenes.Tutorial;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.MainMenuScene;
import jdemic.ui.*;

public class TutorialRulesScene {

    private StackPane root;
    private Stage stage;

    public TutorialRulesScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }

    private ImageView createThreatIcon(String path) {
        ImageView img = new ImageView(new Image(getClass().getResource(path).toExternalForm()));
        img.fitWidthProperty().bind(root.widthProperty().multiply(0.015));
        img.fitHeightProperty().bind(root.widthProperty().multiply(0.015));
        img.setPreserveRatio(true);
        return img;
    }

    private void setupUI() {
        TutorialUtil.createTutorialTitle( root, "TUTORIAL", 0.05, "#cfc900", Pos.TOP_LEFT, 0.05, 0.06, "#000000", 10);

        StackPane tutorialPanel = TutorialUtil.createTutorialPanel(root);
        root.getChildren().add(tutorialPanel);

        VBox content = new VBox();
        content.setAlignment(Pos.TOP_LEFT);
        content.spacingProperty().bind(root.heightProperty().multiply(0.015));
        content.paddingProperty().bind(
                Bindings.createObjectBinding(() -> {
                            double w = root.getWidth();
                            double h = root.getHeight();
                            return new Insets(h * 0.02, w * 0.03, h * 0.02, w * 0.03);
                        },
                        root.widthProperty(),
                        root.heightProperty()
                )
        );

        String LEFT = "#00b5d4";
        String RIGHT = "#ffffff";
        double SIZE = 0.012;

        content.getChildren().add(TutorialUtil.createRow(root, "Who are you?","You are a team of cybersecurity specialists working together to stop the spread of dangerous malware across the global network.", LEFT, RIGHT, SIZE));
        content.getChildren().add(TutorialUtil.createLine(tutorialPanel, "#000000", 6));

        content.getChildren().add(TutorialUtil.createRow(root, "What is the goal?","Develop countermeasures for all four cyber threats before the system collapses.", LEFT, RIGHT, SIZE));
        content.getChildren().add(TutorialUtil.createLine(tutorialPanel, "#000000", 6));

        content.getChildren().add(TutorialUtil.createRow(root, "What are the threats?", "",LEFT, RIGHT, SIZE));
        content.getChildren().add(TutorialUtil.createLine(tutorialPanel, "#000000", 6));

        content.getChildren().add(TutorialUtil.createRow(root, "What role can you play?","Incident Responder | Artificial Intelligence Analyst | Network Controller | Encryption Specialist | System Engineer | Threat Strategist | Firewall Specialist", LEFT, RIGHT, SIZE));
        content.getChildren().add(TutorialUtil.createLine(tutorialPanel, "#000000", 6));

        content.getChildren().add(TutorialUtil.createRow(root, "Game Phases:","Actions: Move, Treat, Share, Build, Discover\nDraw Cards: Take 2 city cards, risk drawing a System Breach card\nInfect Cities: Draw infection cards, virus are added to the city", LEFT, RIGHT, SIZE));
        content.getChildren().add(TutorialUtil.createLine(tutorialPanel, "#000000", 6));

        content.getChildren().add(TutorialUtil.createRow(root, "When do you win?","All 4 cures are discovered",LEFT, RIGHT, SIZE));
        content.getChildren().add(TutorialUtil.createLine(tutorialPanel, "#000000", 6));

        content.getChildren().add( TutorialUtil.createRow(root, "When do you lose?","Too many outbreaks occur, or you run out of malware cubes or the player deck runs out", LEFT, RIGHT, SIZE));
        content.getChildren().add(TutorialUtil.createLine(tutorialPanel, "#000000", 6));

        HBox threatsRow = new HBox();
        threatsRow.setAlignment(Pos.CENTER_LEFT);
        threatsRow.spacingProperty().bind(root.widthProperty().multiply(0.01));
        threatsRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(threatsRow, Priority.ALWAYS);

        ImageView conficker = createThreatIcon("/icons/IconVirusConficker.png");
        ImageView wannacry = createThreatIcon("/icons/IconVirusWannaCry.png");
        ImageView stuxnet = createThreatIcon("/icons/IconVirusStuxnet.png");
        ImageView zeus = createThreatIcon("/icons/IconVirusZeus.png");

        threatsRow.getChildren().addAll(
                conficker, TextUtil.createText("Conficker", "neotechprobold", SIZE, RIGHT, root),
                wannacry, TextUtil.createText("WannaCry", "neotechprobold", SIZE, RIGHT, root),
                stuxnet, TextUtil.createText("Stuxnet", "neotechprobold", SIZE, RIGHT, root),
                zeus, TextUtil.createText("Zeus", "neotechprobold", SIZE, RIGHT, root)
        );

        Label leftLabel = TextUtil.createText("What are the threats?", "neotechprobold", SIZE, LEFT, root);
        leftLabel.minWidthProperty().bind(root.widthProperty().multiply(0.18));

        HBox row = new HBox(leftLabel, threatsRow);
        row.spacingProperty().bind(root.widthProperty().multiply(0.05));
        row.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().set(4, new VBox(row));
        content.maxHeightProperty().bind(tutorialPanel.heightProperty().subtract(40));
        content.setFillWidth(true);

        StackPane contentWrapper = new StackPane(content);
        contentWrapper.paddingProperty().bind(
                Bindings.createObjectBinding(() ->
                                new Insets(
                                        root.getHeight() * 0.02,
                                        root.getWidth() * 0.02,
                                        root.getHeight() * 0.02,
                                        root.getWidth() * 0.02
                                ),
                        root.widthProperty(),
                        root.heightProperty()
                )
        );
        contentWrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        tutorialPanel.getChildren().add(contentWrapper);

        TutorialUtil.addBottomButtons(root,tutorialPanel,stage,() -> stage.getScene().setRoot(new MainMenuScene(stage).getRoot()), () -> stage.getScene().setRoot(new TutorialMapScene(stage).getRoot()));
    }
    public StackPane getRoot() {return root;}
}