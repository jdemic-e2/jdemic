package jdemic.Scenes.MapTest;

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
import jdemic.ui.GameplayUI.ActionMenuManager;
import jdemic.ui.GameplayUI.InfectionRateManager;
import jdemic.ui.GameplayUI.NotificationManager;

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
        setupInfectionManager();

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.I) {
                        // Verificăm dacă mai avem unde să creștem (indexul pleacă de la 0 la 6)
                        if (gameManager.getState().getInfectionRate() < gameManager.getInfectionRateTrack().length - 1) {
                            gameManager.increaseInfectionRate();
                            infectionRateManager.updateTrack();
                            notificationManager.showNotification("DEBUG: Infection Rate Increased!");
                        } else {
                            notificationManager.showNotification("DEBUG: Infection Rate is maxed");
                        }
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
            //added this for debugging
            if (gameManager != null && gameManager.getState() != null)
            {
                gameManager.getState().setActionsRemaining(4);
            }
            if (actionMenuManager != null) actionMenuManager.updateMenuState();
            if (infectionRateManager != null) infectionRateManager.updateTrack();

            returnToMainMenu();
        });

        header.getChildren().add(backBtn);
        root.getChildren().add(header);
    }

    private void setupInfectionManager()
    {
        this.infectionRateManager = new InfectionRateManager(root,gameManager);
    }

    private void setupNotifications()
    {
        notificationContainer = new VBox(10);
        notificationContainer.prefWidthProperty().bind(root.widthProperty());
        notificationContainer.prefHeightProperty().bind(root.heightProperty());

        notificationContainer.setPadding(new Insets(20));
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