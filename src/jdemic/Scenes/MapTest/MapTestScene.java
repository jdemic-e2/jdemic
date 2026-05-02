package jdemic.Scenes.MapTest;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.GameLogic.*;
import javafx.scene.layout.VBox;
import jdemic.ui.GameplayUI.*;

import java.util.*;

public class MapTestScene {
    private Stage stage;
    private StackPane root;
    private PandemicMapGraph mapGraph;
    private Map<CityNode, Circle> nodeVisuals = new HashMap<>();
    private Map<String, Line> edgeVisuals = new HashMap<>();

    //Variables for notifications
    private NotificationManager notificationManager;
    private VBox notificationContainer;

    //Variable for ActionMenu
    private ActionMenuManager actionMenuManager;

    //Variable for game manager (temp)
    private GameManager gameManager;

    //Variables for InfectionManager
    private InfectionRateManager infectionRateManager;

    //Variables for OutbreakManager
    private OutbreakManager outbreakManager;

    //Variables for CureManager
    private CureManager cureManager;

    public MapTestScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        this.mapGraph = new PandemicMapGraph();

        //This is a test player because I needed to test certain features
        List<Player> players = new ArrayList<>();
        CityNode startingCity = mapGraph.getCity("Atlanta");
        players.add(new Player(new PlayerState("Tester", startingCity)));

        this.gameManager = new GameManager(players); //[cite: 16]

        this.root.setStyle("-fx-background-color: #050a14;");
        setupBackground();
        setupContent();
        setupUI();
        setupNotifications();
        setupActionMenu();
        setupGlobalHUD();

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.I) {
                        if (gameManager.getState().getInfectionRate() < gameManager.getInfectionRateTrack().length - 1) {
                            gameManager.increaseInfectionRate();
                            infectionRateManager.updateTrack(); // Actualizăm doar ce s-a schimbat
                            notificationManager.showNotification("DEBUG: Infection Rate Increased!");
                        }
                    }
                    if (event.getCode() == javafx.scene.input.KeyCode.O) {
                        gameManager.getState().getDiseaseManager().increaseOutbreakScore();
                        outbreakManager.updateTrack();
                        notificationManager.showNotification("DEBUG: Outbreak Occurred!");
                    }
                });
            }
        });
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
            if (gameManager != null && gameManager.getState() != null) {
                gameManager.getState().setActionsRemaining(4);
            }
            if (actionMenuManager != null) actionMenuManager.updateMenuState();
            if (infectionRateManager != null) infectionRateManager.updateTrack();
            if (outbreakManager != null) outbreakManager.updateTrack();
            if (cureManager != null) cureManager.updateUI();

            returnToMainMenu();
        });

        header.getChildren().add(backBtn);
        root.getChildren().add(header);
    }

    private void setupGlobalHUD() {
        HBox mainHUD = new HBox();
        mainHUD.setAlignment(Pos.TOP_CENTER);
        mainHUD.setPickOnBounds(false);

        mainHUD.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.8); " +
                        "-fx-background-radius: 15px; " +
                        "-fx-padding: 10px;"
        );

        mainHUD.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        mainHUD.maxWidthProperty().bind(root.widthProperty().multiply(0.6));
        StackPane.setAlignment(mainHUD, Pos.TOP_CENTER);
        mainHUD.paddingProperty().bind(Bindings.createObjectBinding(() ->
                        new Insets(root.getHeight() * 0.04, 10, 10, 10),
                root.heightProperty()
        ));

        mainHUD.translateXProperty().bind(root.widthProperty().multiply(0.05));
        mainHUD.spacingProperty().bind(root.widthProperty().multiply(0.03));

        this.outbreakManager = new OutbreakManager(root, gameManager);
        this.infectionRateManager = new InfectionRateManager(root, gameManager);
        this.cureManager = new CureManager(root, gameManager);

        mainHUD.getChildren().addAll(
                outbreakManager.getContainer(),
                infectionRateManager.getContainer(),
                cureManager.getContainer()
        );

        root.getChildren().add(mainHUD);
    }

    private void setupNotifications()
    {
        notificationContainer = new VBox(10);
        notificationContainer.prefWidthProperty().bind(root.widthProperty());
        notificationContainer.prefHeightProperty().bind(root.heightProperty());

        notificationContainer.paddingProperty().bind(Bindings.createObjectBinding(() ->
                        new Insets(root.getHeight() * 0.15, 20, 0, 0),
                root.heightProperty()
        ));
        notificationContainer.setAlignment(Pos.TOP_RIGHT);
        notificationContainer.setMouseTransparent(true);

        notificationManager = new NotificationManager(notificationContainer);
        root.getChildren().add(notificationContainer);
    }

    private void setupActionMenu()
    {
        this.actionMenuManager = new ActionMenuManager(root, notificationManager, gameManager);
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

            node.setOnMouseClicked(ev -> {city.clickEvent();
                notificationManager.showNotification("Selected City: " + city.getName());
            });

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

    private Color getFxColor(DiseaseColor color) {
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
        SceneManager.switchScene("MAIN_MENU");
    }

    private void setupBackground() {
        ImageView background = new ImageView(new Image(getClass().getResource("/backgroundMap.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }
}