package jdemic.Scenes.Tutorial;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.PandemicMapGraph;
import jdemic.ui.*;

public class TutorialCitiesScene {

    private StackPane root;
    private Stage stage;

    public TutorialCitiesScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }

    private void openCityCard(CityNode city) {

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");
        ImageView card;
        try {
            String colorPrefix = switch (city.getNativeColor()) {
                case BLUE -> "Blue";
                case YELLOW -> "Yellow";
                case BLACK -> "Black";
                case RED -> "Red";
            };

            String cityName = city.getName().replace(" ", "").replace(".", "");
            String path = "/cards/" + colorPrefix + cityName + ".png";
            card = new ImageView(new Image(getClass().getResource(path).toExternalForm()));
        } catch (Exception e) {
            Label fallback = new Label(city.getName());
            fallback.setStyle("-fx-text-fill: white; -fx-font-size: 24;");
            overlay.getChildren().add(fallback);
            root.getChildren().add(overlay);
            return;
        }
        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(root.widthProperty().multiply(0.3));

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(30);
        clip.arcHeightProperty().set(30);
        clip.widthProperty().bind(card.layoutBoundsProperty().map(Bounds::getWidth));
        clip.heightProperty().bind(card.layoutBoundsProperty().map(Bounds::getHeight));
        card.setClip(clip);

        StackPane cardWrapper = new StackPane(card);
        cardWrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cardWrapper.prefWidthProperty().bind(card.fitWidthProperty());
        cardWrapper.prefHeightProperty().bind(card.fitHeightProperty());
        cardWrapper.setStyle("-fx-background-color: black;" + "-fx-border-color: #00b5d4;" + "-fx-border-width: 2;" + "-fx-background-radius: 20;" +"-fx-border-radius: 20;");
        GlowUtil.applyGlow(cardWrapper, "#00b5d4", 20);

        Button close = new Button("X");
        close.setStyle("-fx-background-color: #ff2d2d;" +
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

        ParallelTransition animation = new ParallelTransition(scale, fade);
        animation.play();

        root.getChildren().add(overlay);
    }

    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02, "#00b5d4", Pos.TOP_LEFT, 0.03, 0.03, null, 0);
        TutorialUtil.createTutorialTitle(root,"2. CITY CARDS",0.05,"#cfc900", Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        ImageView map = new ImageView(new Image(getClass().getResource("/backgroundMap.png").toExternalForm()));
        map.setPreserveRatio(true);
        map.fitWidthProperty().bind(root.widthProperty().multiply(0.75));
        StackPane.setAlignment(map, Pos.CENTER);

        StackPane leftPanel = new StackPane();
        leftPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        leftPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.18));
        leftPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        leftPanel.setStyle("-fx-background-color: black;" +"-fx-border-color: #00b5d4;" +"-fx-border-width: 2;" +"-fx-background-radius: 6;" + "-fx-border-radius: 6;");
        GlowUtil.applyGlow(leftPanel, "#00b5d4", 15);
        StackPane.setAlignment(leftPanel, Pos.CENTER_LEFT);
        leftPanel.translateXProperty().bind(root.widthProperty().multiply(0.06));
        leftPanel.translateYProperty().bind(root.heightProperty().multiply(0.05));

        Label panelText = TextUtil.createText("CLICK ON A\nCITY TO SEE\nTHE CITY\nCARD","hkmodular",0.013,"#00b5d4",root);
        panelText.setWrapText(true);
        panelText.setAlignment(Pos.CENTER);
        panelText.setTextAlignment(TextAlignment.CENTER);
        panelText.maxWidthProperty().bind(leftPanel.widthProperty().multiply(0.75));

        StackPane panelContent = new StackPane(panelText);
        panelContent.paddingProperty().bind(Bindings.createObjectBinding(() ->
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
        root.getChildren().addAll(map, leftPanel);

        PandemicMapGraph graph = new PandemicMapGraph();
        for (CityNode city : graph.getCityList()) {
            Circle pin = new Circle(6);
            String color = switch (city.getNativeColor()) {
                case BLUE -> "#4da6ff";
                case YELLOW -> "#ffd84d";
                case BLACK -> "#444444";
                case RED -> "#ff4d4d";
            };
            pin.setStyle("-fx-fill: " + color + "; -fx-stroke: black;");
            GlowUtil.applyGlow(pin, color, 10);
            pin.translateXProperty().bind(map.fitWidthProperty().multiply(city.getRenderX()).subtract(map.fitWidthProperty().multiply(0.5)));
            pin.translateYProperty().bind(Bindings.createDoubleBinding(() -> map.getBoundsInParent().getHeight() * city.getRenderY() - map.getBoundsInParent().getHeight() * 0.5, map.boundsInParentProperty()));

            Label cityLabel = TextUtil.createText(city.getName(),"hkmodular",0.01,"#000000",root);
            cityLabel.setVisible(false);
            cityLabel.setOpacity(0);
            cityLabel.setMouseTransparent(true);
            GlowUtil.applyGlow(cityLabel, "#00b5d4", 2);

            cityLabel.translateXProperty().bind(pin.translateXProperty().add(15));
            cityLabel.translateYProperty().bind(pin.translateYProperty().subtract(15));
            root.getChildren().add(cityLabel);
            pin.setOnMouseEntered(e -> {
                pin.setScaleX(1.5);
                pin.setScaleY(1.5);
                cityLabel.toFront();
                cityLabel.setVisible(true);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), cityLabel);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            pin.setOnMouseExited(e -> {
                pin.setScaleX(1);
                pin.setScaleY(1);

                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), cityLabel);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> cityLabel.setVisible(false));
                fadeOut.play();
            });
            pin.setOnMouseClicked(e -> openCityCard(city));
            root.getChildren().add(pin);
        }
        TutorialUtil.addBottomButtons(root,root,stage,() -> stage.getScene().setRoot(new TutorialMapScene(stage).getRoot()),() -> stage.getScene().setRoot(new TutorialCardRolesScene(stage).getRoot()));
    }

    public StackPane getRoot() {
        return root;
    }
}
