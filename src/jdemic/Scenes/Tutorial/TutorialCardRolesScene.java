package jdemic.Scenes.Tutorial;

import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import jdemic.Scenes.SceneManager;
import jdemic.ui.*;

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
        ImageView card = new ImageView(new Image(getClass().getResource("/roleCards/Role" + role + ".png").toExternalForm()));
        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(root.widthProperty().multiply(0.12));
        GlowUtil.applyGlow(card, "#00b5d4", 10);

        card.setOnMouseEntered(e -> {
            card.setScaleX(1.1);
            card.setScaleY(1.1);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1);
            card.setScaleY(1);
        });
        card.setOnMouseClicked(e -> openRoleCard(role));
        return card;
    }
    private void openRoleCard(String roleName) {

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");
        ImageView card;

        try {
            String cleanName = roleName.replace(" ", "");
            String path = "/roleCards/Role" + cleanName + ".png";
            card = new ImageView(new Image(getClass().getResource(path).toExternalForm()));
        } catch (Exception e) {
            Label fallback = new Label(roleName);
            fallback.setStyle("-fx-text-fill: white; -fx-font-size: 24;");
            overlay.getChildren().add(fallback);
            root.getChildren().add(overlay);
            return;
        }

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
        cardWrapper.setStyle("-fx-background-color: black;" +"-fx-border-color: #00b5d4;" +"-fx-border-width: 2;" +"-fx-background-radius: 20;" + "-fx-border-radius: 20;");
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

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02, "#00b5d4",Pos.TOP_LEFT, 0.03, 0.03, null, 0);
        TutorialUtil.createTutorialTitle(root,"3. PLAYER ROLES",0.05,"#cfc900",Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        StackPane leftPanel = new StackPane();
        leftPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        leftPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.18));
        leftPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        leftPanel.setStyle("-fx-background-color: black;" + "-fx-border-color: #00b5d4;" +"-fx-border-width: 2;" + "-fx-background-radius: 6;" + "-fx-border-radius: 6;");
        GlowUtil.applyGlow(leftPanel, "#00b5d4", 15);

        Label panelText = TextUtil.createText( "CLICK ON A\nROLE CARD TO SEE\nTHEIR ABILITIES", "hkmodular",0.010,"#00b5d4",root);
        panelText.setWrapText(true);
        panelText.setAlignment(Pos.CENTER);
        panelText.setTextAlignment(TextAlignment.CENTER);
        panelText.maxWidthProperty().bind(leftPanel.widthProperty().multiply(0.75));

        StackPane panelContent = new StackPane(panelText);
        panelContent.paddingProperty().bind(
                Bindings.createObjectBinding(() ->
                                new Insets(
                                        leftPanel.getHeight() * 0.15,
                                        leftPanel.getWidth() * 0.1,
                                        leftPanel.getHeight() * 0.15,
                                        leftPanel.getWidth() * 0.1
                                ),
                        leftPanel.widthProperty(),
                        leftPanel.heightProperty()
                )
        );
        leftPanel.getChildren().add(panelContent);

        StackPane leftArea = new StackPane();
        leftArea.setAlignment(Pos.CENTER_LEFT);
        leftPanel.translateXProperty().bind(leftArea.widthProperty().multiply(0.1));
        leftArea.getChildren().add(leftPanel);

        String[] roles = {
                "ArtificialIntelligenceAnalyst",
                "EncryptionSpecialist",
                "FireWallSpecialist",
                "IncidentResponder",
                "NetworkController",
                "SystemEngineer",
                "ThreatStrategist"
        };

        StackPane rightArea = new StackPane();
        HBox cardContainer = new HBox(30);

        for (String role : roles) { cardContainer.getChildren().add(createCard(role));}

        Pane viewport = new Pane();
        viewport.prefWidthProperty().bind(rightArea.widthProperty());
        viewport.prefHeightProperty().bind(rightArea.heightProperty());
        viewport.getChildren().add(cardContainer);
        cardContainer.layoutYProperty().bind(viewport.heightProperty().subtract(cardContainer.heightProperty()).divide(2));

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);

        cardContainer.layoutXProperty().bind(viewport.widthProperty().multiply(0.02));

        DoubleProperty scrollX = new SimpleDoubleProperty(0);
        cardContainer.translateXProperty().bind(scrollX);

        final double[] lastX = {0};

        viewport.setOnMousePressed(e -> lastX[0] = e.getSceneX());
        viewport.setOnMouseDragged(e -> {
            double delta = e.getSceneX() - lastX[0];
            lastX[0] = e.getSceneX();
            double contentWidth = cardContainer.getBoundsInLocal().getWidth();
            double viewportWidth = viewport.getWidth();
            double minScroll = Math.min(0, viewportWidth - contentWidth);
            double maxScroll = 0;
            double target = scrollX.get() + delta;
            target = Math.max(minScroll, Math.min(maxScroll, target));
            scrollX.set(target);
        });
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