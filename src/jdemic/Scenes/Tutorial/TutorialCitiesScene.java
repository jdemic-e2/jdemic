package jdemic.Scenes.Tutorial;

import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.PandemicMapGraph;
import jdemic.Scenes.SceneManager.SceneManager;
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
        String colorPrefix = switch (city.getNativeColor()) {
            case BLUE -> "Blue";
            case YELLOW -> "Yellow";
            case BLACK -> "Black";
            case RED -> "Red";
        };

        String cityName = city.getName().replace(" ", "").replace(".", "");
        TutorialUtil.openCardOverlay(root, "/cards/" + colorPrefix + cityName + ".png", city.getName(), "TutorialCitiesScene");
    }

    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02, "#00b5d4", Pos.TOP_LEFT, 0.03, 0.03, null, 0);
        TutorialUtil.createTutorialTitle(root,"2. CITY CARDS",0.05,"#cfc900", Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        java.net.URL mapUrl = getClass().getResource("/backgroundMap.png");
        if (mapUrl == null) {
            System.err.println("[TutorialCitiesScene] Missing resource: /backgroundMap.png");
            return;
        }
        ImageView map = new ImageView(new Image(mapUrl.toExternalForm()));
        map.setPreserveRatio(true);
        map.fitWidthProperty().bind(root.widthProperty().multiply(0.75));
        StackPane.setAlignment(map, Pos.CENTER);

        StackPane leftPanel = TutorialUtil.createInstructionPanel(root, "CLICK ON A\nCITY TO SEE\nTHE CITY\nCARD", 0.013);
        StackPane.setAlignment(leftPanel, Pos.CENTER_LEFT);
        leftPanel.translateXProperty().bind(root.widthProperty().multiply(0.06));
        leftPanel.translateYProperty().bind(root.heightProperty().multiply(0.05));

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
        TutorialUtil.addBottomButtons(root, root, stage, () -> SceneManager.switchScene("TUT_MAP"), () -> SceneManager.switchScene("TUT_ROLES"));
    }

    public StackPane getRoot() {
        return root;
    }
}
