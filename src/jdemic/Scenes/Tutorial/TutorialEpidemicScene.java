package jdemic.Scenes.Tutorial;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
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

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

        ImageView card = new ImageView(new Image(getClass().getResource("/epidemicCard/SystemBreach.png").toExternalForm()));

        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(root.widthProperty().multiply(0.3));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        clip.widthProperty().bind(card.layoutBoundsProperty().map(b -> b.getWidth()));
        clip.heightProperty().bind(card.layoutBoundsProperty().map(b -> b.getHeight()));
        card.setClip(clip);

        StackPane cardWrapper = new StackPane(card);
        cardWrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cardWrapper.prefWidthProperty().bind(card.fitWidthProperty());
        cardWrapper.prefHeightProperty().bind(card.fitHeightProperty());
        cardWrapper.setStyle("-fx-background-color: black;" +
                        "-fx-border-color: #00b5d4;" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;"
        );

        GlowUtil.applyGlow(cardWrapper, "#00b5d4", 20);

        Button close = new Button("X");
        close.setStyle( "-fx-background-color: #ff2d2d;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50;" +
                        "-fx-min-width: 30;" +
                        "-fx-min-height: 30;" +
                        "-fx-max-width: 30;" +
                        "-fx-max-height: 30;"
        );

        close.setOnAction(e -> {
            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), cardWrapper);
            scaleOut.setToX(0.7);
            scaleOut.setToY(0.7);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), cardWrapper);
            fadeOut.setToValue(0);

            ParallelTransition out = new ParallelTransition(scaleOut, fadeOut);
            out.setOnFinished(ev -> root.getChildren().remove(overlay));
            out.play();
        });

        cardWrapper.getChildren().add(close);
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        close.setTranslateX(8);
        close.setTranslateY(-8);

        overlay.getChildren().add(cardWrapper);

        cardWrapper.setScaleX(0.7);
        cardWrapper.setScaleY(0.7);
        cardWrapper.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(250), cardWrapper);
        scale.setToX(1);
        scale.setToY(1);

        FadeTransition fade = new FadeTransition(Duration.millis(250), cardWrapper);
        fade.setToValue(1);

        new ParallelTransition(scale, fade).play();

        root.getChildren().add(overlay);
    }
    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02,"#00b5d4",Pos.TOP_LEFT,0.03,0.03,null,0);
        TutorialUtil.createTutorialTitle(root,"5. SYSTEM BREACH",0.05,"#cfc900",Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        HBox mainLayout = new HBox();
        mainLayout.prefWidthProperty().bind(root.widthProperty());
        mainLayout.prefHeightProperty().bind(root.heightProperty());
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.spacingProperty().bind(root.widthProperty().multiply(0.03));

        ImageView card = new ImageView(new Image(getClass().getResource("/epidemicCard/SystemBreach.png").toExternalForm()));
        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(root.widthProperty().multiply(0.18));
        GlowUtil.applyGlow(card, "#00b5d4", 20);

        card.setOnMouseEntered(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1);
            card.setScaleY(1);
        });
        card.setOnMouseClicked(e -> openSystemBreachCard());

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

        TutorialUtil.addBottomButtons(root,tutorialPanel,stage,() -> stage.getScene().setRoot(new TutorialVirusCardsScene(stage).getRoot()), () -> stage.getScene().setRoot(new TutorialPlayerTurnScene(stage).getRoot()));
    }

    public StackPane getRoot() {
        return root;
    }
}