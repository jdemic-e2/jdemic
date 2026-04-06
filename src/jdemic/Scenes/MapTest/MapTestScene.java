package jdemic.Scenes.MapTest;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.PandemicMapGraph;
import jdemic.Scenes.MainMenuScene;
import jdemic.ui.ButtonsUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapTestScene {
    private Stage stage;
    private StackPane root;
    private PandemicMapGraph mapGraph;
    private Map<CityNode, Circle> nodeVisuals = new HashMap<>();
    private Map<String, Line> edgeVisuals = new HashMap<>();

    public MapTestScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        this.mapGraph = new PandemicMapGraph();
        this.root.setStyle("-fx-background-color: #050a14;");
        setupBackground();
        setupContent();
        setupUI();
    }

    private void setupUI() {
        HBox header = new HBox();
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.TOP_LEFT);
        header.setPickOnBounds(false);

        ButtonsUtil backBtn = new ButtonsUtil(
                "BACK",
                "#00b5d4", "black", "#00b5d4", "#00b5d4",
                2, 10, 10,
                0.12, 0.06, 0.015,
                root
        );

        backBtn.setOnMouseClicked(e -> {
            returnToMainMenu();
        });

        header.getChildren().add(backBtn);
        root.getChildren().add(header);
    }

    private void setupContent() {
        Pane mapPane = new Pane();
        mapPane.prefWidthProperty().bind(root.widthProperty());
        mapPane.prefHeightProperty().bind(root.heightProperty());

        Color defaultLineColor = Color.color(0, 0.7, 1, 0.3);
        Color activeLineColor = Color.web("#00ffea");

        DropShadow glowEffect = new DropShadow();
        glowEffect.setColor(Color.web("#00d4ff"));
        glowEffect.setRadius(20);
        glowEffect.setSpread(0.6);

        Set<String> processedEdges = new HashSet<>();

        for (CityNode city : mapGraph.getCityList()) {
            for (CityNode neighbor : city.getConnectedCities()) {
                String id1 = city.getName() + "-" + neighbor.getName();
                String id2 = neighbor.getName() + "-" + city.getName();

                if (!processedEdges.contains(id1) && !processedEdges.contains(id2)) {
                    Line line = new Line();
                    line.setStroke(defaultLineColor);
                    line.setStrokeWidth(2);

                    line.startXProperty().bind(mapPane.widthProperty().multiply(city.getRenderX()));
                    line.startYProperty().bind(mapPane.heightProperty().multiply(city.getRenderY()));
                    line.endXProperty().bind(mapPane.widthProperty().multiply(neighbor.getRenderX()));
                    line.endYProperty().bind(mapPane.heightProperty().multiply(neighbor.getRenderY()));

                    mapPane.getChildren().add(line);
                    edgeVisuals.put(id1, line);
                    processedEdges.add(id1);
                }
            }
        }

        for (CityNode city : mapGraph.getCityList()) {
            Circle node = new Circle();
            node.radiusProperty().bind(mapPane.heightProperty().multiply(0.012));

            Color nativeColor = getFxColor(city.getNativeColor());
            node.setFill(nativeColor);
            node.setStroke(Color.WHITE);
            node.setStrokeWidth(1);

            node.centerXProperty().bind(mapPane.widthProperty().multiply(city.getRenderX()));
            node.centerYProperty().bind(mapPane.heightProperty().multiply(city.getRenderY()));

            nodeVisuals.put(city, node);

            node.setOnMouseEntered(e -> {
                node.setFill(Color.WHITE);
                node.setEffect(glowEffect);
                node.setScaleX(1.5);
                node.setScaleY(1.5);

                for (CityNode neighbor : city.getConnectedCities()) {
                    Circle neighborCircle = nodeVisuals.get(neighbor);
                    if (neighborCircle != null) {
                        neighborCircle.setStroke(Color.WHITE);
                        neighborCircle.setStrokeWidth(3);
                        neighborCircle.setEffect(glowEffect);
                    }

                    Line connection = edgeVisuals.get(city.getName() + "-" + neighbor.getName());
                    if (connection == null) connection = edgeVisuals.get(neighbor.getName() + "-" + city.getName());

                    if (connection != null) {
                        connection.setStroke(activeLineColor);
                        connection.setStrokeWidth(4);
                        connection.setEffect(glowEffect);
                    }
                }
            });

            node.setOnMouseExited(e -> {
                node.setFill(nativeColor);
                node.setEffect(null);
                node.setScaleX(1);
                node.setScaleY(1);

                for (CityNode neighbor : city.getConnectedCities()) {
                    Circle neighborCircle = nodeVisuals.get(neighbor);
                    if (neighborCircle != null) {
                        neighborCircle.setStroke(Color.WHITE);
                        neighborCircle.setStrokeWidth(1);
                        neighborCircle.setEffect(null);
                    }

                    Line connection = edgeVisuals.get(city.getName() + "-" + neighbor.getName());
                    if (connection == null) connection = edgeVisuals.get(neighbor.getName() + "-" + city.getName());

                    if (connection != null) {
                        connection.setStroke(defaultLineColor);
                        connection.setStrokeWidth(2);
                        connection.setEffect(null);
                    }
                }
            });

            node.setOnMouseClicked(ev -> city.clickEvent());

            Text label = new Text(city.getName());
            label.setFill(Color.WHITE);
            label.setFont(Font.font("hkmodular", FontWeight.BOLD, 10));
            label.setMouseTransparent(true);

            label.layoutXProperty().bind(node.centerXProperty().subtract(label.getLayoutBounds().getWidth() / 2));
            label.layoutYProperty().bind(node.centerYProperty().add(20));

            mapPane.getChildren().addAll(node, label);
        }

        root.getChildren().add(mapPane);
    }

    private Color getFxColor(CityNode.DiseaseColor color) {
        return switch (color) {
            case BLUE -> Color.web("#00b5d4");
            case YELLOW -> Color.web("#cfc900");
            case BLACK -> Color.web("#333333");
            case RED -> Color.web("#ff2d2d");
        };
    }

    public StackPane getRoot() {
        return root;
    }

    private void returnToMainMenu() {
        stage.getScene().setRoot(new MainMenuScene(stage).getRoot());
    }

    private void setupBackground() {
        ImageView background = new ImageView(new Image(getClass().getResource("/backgroundMap.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }
}