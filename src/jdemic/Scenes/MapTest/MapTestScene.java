package jdemic.Scenes.MapTest;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;
import jdemic.GameLogic.*;
import javafx.scene.layout.VBox;
import jdemic.ui.GameplayUI.ActionMenuManager;
import jdemic.ui.GameplayUI.InfectionRateManager;
import jdemic.ui.GameplayUI.NotificationManager;
import javafx.scene.layout.Region;
import javafx.beans.binding.Bindings;
import java.util.*;
import javafx.scene.Node;

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
        setupPlayerHand();
        createChatContent();
        setupDecks();
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
        StackPane mapContainer = new StackPane();
        mapContainer.setAlignment(Pos.CENTER);
        mapContainer.translateYProperty().bind(root.heightProperty().multiply(-0.10));
        Pane mapPane = new Pane();
        mapPane.prefWidthProperty().bind(root.widthProperty().multiply(0.70));
        mapPane.prefHeightProperty().bind(mapPane.prefWidthProperty().multiply(0.5));
        mapPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        mapPane.setStyle("-fx-border-color: red;");

        ImageView mapBg = new ImageView( new Image(getClass().getResource("/backgroundMap.png").toExternalForm()));
        mapBg.fitWidthProperty().bind(mapPane.widthProperty());
        mapBg.fitHeightProperty().bind(mapPane.heightProperty());
        mapBg.setPreserveRatio(false);
        mapPane.getChildren().add(mapBg);

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
                    line.setMouseTransparent(true);
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
            node.radiusProperty().bind(mapPane.widthProperty().add(mapPane.heightProperty()).multiply(0.006));

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

            label.layoutXProperty().bind(Bindings.createDoubleBinding(() -> node.getCenterX() - label.getLayoutBounds().getWidth() / 2, node.centerXProperty(),label.layoutBoundsProperty()));
            label.layoutYProperty().bind(node.centerYProperty().add(20));

            mapPane.getChildren().addAll(node, label);
        }

        mapContainer.getChildren().add(mapPane);
        root.getChildren().add(mapContainer);
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
        ImageView background = new ImageView(new Image(getClass().getResource("/bgGame.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }
    
    private void generateRandomCards(HBox container) {
        PandemicMapGraph graph = new PandemicMapGraph();
        List<CityNode> cities = new ArrayList<>(graph.getCityList());
        System.out.println("Total cities: " + cities.size()); //debu
        Collections.shuffle(cities);
        int count = Math.min(7, cities.size());
        System.out.println("Generating: " + count + " cards"); //debug

        for (int i = 0; i < count; i++) {
            CityNode city = cities.get(i);
            System.out.println("Card " + i + ": " + city.getName()); //debug
            StackPane card = createCityCard(city);
            container.getChildren().add(card);
        }
        System.out.println("Children in container: " + container.getChildren().size()); //debug
    }
    
    private StackPane createGlowBox(Node content, String color, double glowRadius) {
        StackPane box = new StackPane(content);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setStyle("-fx-background-color: black;" + "-fx-border-color: transparent " + color + " transparent transparent;" + "-fx-border-width: 0 1 0 0;");
        GlowUtil.applyGlow(box, color, glowRadius);
        return box;
    }
    
    private void setupPlayerHand() {

        HBox handContainer = new HBox();
        handContainer.setAlignment(Pos.BOTTOM_LEFT);
        handContainer.setPickOnBounds(false);

        handContainer.spacingProperty().bind(
            Bindings.createDoubleBinding(() -> {

                int cardCount = handContainer.getChildren().size();
                if (cardCount == 0) return -20.0;

                double rootWidth = root.getWidth();
                double cardWidth = Math.max(70, rootWidth * 0.06);
                double totalCardsWidth = cardWidth * cardCount;
                double chatWidth = Math.min(rootWidth * 0.22, 320);
                double availableWidth = rootWidth - chatWidth - 100;
                double overlap = (availableWidth - totalCardsWidth) / (cardCount - 1);

                return Math.min(-10, overlap);

            }, root.widthProperty(), Bindings.size(handContainer.getChildren()))
        );

        handContainer.paddingProperty().bind(
            Bindings.createObjectBinding(() -> {
                double vertical = root.getHeight() * 0.015;
                double horizontal = root.getWidth() * 0.02;
                return new Insets(vertical, horizontal, vertical, horizontal);
            }, root.heightProperty(), root.widthProperty())
        );

        VBox chatContent = createChatContent();

        HBox combined = new HBox();
        combined.setAlignment(Pos.BOTTOM_LEFT);

        HBox.setHgrow(handContainer, Priority.ALWAYS);
        HBox.setHgrow(chatContent, Priority.NEVER);

        combined.spacingProperty().bind(root.widthProperty().multiply(0.02));
        combined.getChildren().addAll(handContainer, chatContent);

        StackPane wrapper = createGlowBox(combined, "#00b5d4", 15);
        wrapper.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane.setAlignment(wrapper, Pos.BOTTOM_LEFT);

        root.getChildren().add(wrapper);

        generateRandomCards(handContainer);
    }
    
    private StackPane createCityCard(CityNode city) {
        ImageView card;
        try {
            String colorPrefix = switch (city.getNativeColor()) {
                case BLUE -> "Blue";
                case YELLOW -> "Yellow";
                case BLACK -> "Green"; // MISMATCH!
                case RED -> "Red";
            };

            String cityName = city.getName().replace(" ", "").replace(".", "");
            String path = "/cityCards/" + colorPrefix + cityName + ".png";

            var resource = getClass().getResource(path);
            if (resource == null) { return new StackPane(new Label(city.getName())); }
            card = new ImageView(new Image(resource.toExternalForm()));
        } catch (Exception e) { return new StackPane(new Label(city.getName())); }

        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(70, root.getWidth() * 0.06), root.widthProperty()));
        StackPane wrapper = new StackPane(card);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        GlowUtil.applyGlow(wrapper, "#00b5d4", 5);

        wrapper.setOnMouseEntered(e -> {
            wrapper.setTranslateY(-10);
            wrapper.setScaleX(1.1);
            wrapper.setScaleY(1.1);
        });

        wrapper.setOnMouseExited(e -> {
            wrapper.setTranslateY(0);
            wrapper.setScaleX(1);
            wrapper.setScaleY(1);
        });
        return wrapper;
    }
    
    private VBox createChatContent() {

        VBox chatContent = new VBox();
        chatContent.setAlignment(Pos.TOP_LEFT);
        chatContent.spacingProperty().bind(root.heightProperty().multiply(0.012));
        chatContent.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.min(root.getWidth() * 0.28, 320),root.widthProperty()));
        chatContent.prefHeightProperty().bind(root.heightProperty().multiply(0.22));
        chatContent.paddingProperty().bind(
            Bindings.createObjectBinding(() -> {

                double vertical = root.getHeight() * 0.012;
                double horizontal = root.getWidth() * 0.015;
                return new Insets(vertical, horizontal, vertical, horizontal);

            }, root.widthProperty(), root.heightProperty())
        );

        Label chatTitle = TextUtil.createText("PLAYER CHAT", "hkmodular", 0.010, "#00d9ff", root);

        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setFocusTraversable(false);
        chatArea.prefHeightProperty().bind(root.heightProperty().multiply(0.12));
        chatArea.prefWidthProperty().bind(chatContent.widthProperty().multiply(0.95));

        String chatAreaStyle = "-fx-control-inner-background: black;" + "-fx-text-fill: #00d9ff;" + "-fx-border-color: transparent;" + "-fx-font-family: 'hkmodular';";

        chatArea.setStyle(chatAreaStyle);

        TextField chatInput = new TextField();

        String inputStyle = "-fx-background-color: black;" + "-fx-text-fill: #00d9ff;" + "-fx-border-color: #00b5d4;" + "-fx-border-width: 1;" + "-fx-font-family: 'hkmodular';";

        chatInput.setStyle(inputStyle);
        chatInput.prefHeightProperty().bind(root.heightProperty().multiply(0.045));

        ButtonsUtil sendBtn = new ButtonsUtil("SEND", "#00d9ff", "black", "#00b5d4", "#00b5d4", 1, 8, 8, 0.06, 0.045, 0.012, root );

        sendBtn.setOnMouseClicked(e -> {
            String msg = chatInput.getText().trim();
            if (msg.isEmpty()) return;
            if (!chatArea.getText().isEmpty()) { chatArea.appendText("\n"); }
            chatArea.appendText("YOU: " + msg);
            chatInput.clear();
        });

        HBox inputRow = new HBox(chatInput, sendBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        inputRow.spacingProperty().bind(root.widthProperty().multiply(0.008));
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        chatContent.getChildren().addAll(chatTitle, chatArea, inputRow);

        return chatContent;
    }

    private StackPane createCardStack(Image topImage, int stackSize, boolean isVerso) {

        StackPane stack = new StackPane();
        stack.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        for (int i = 0; i < stackSize; i++) {
            ImageView card = new ImageView(topImage);
            card.setPreserveRatio(true);
            card.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(60, root.getWidth() * 0.06), root.widthProperty()));
            card.setTranslateX(i * 1);
            card.setTranslateY(-i * 1);
            card.setRotate((Math.random() - 0.5) * 2);
            stack.getChildren().add(card);
        }
        return stack;
    }

    private HBox createCityDeck() {

        Image verso = new Image(getClass().getResource("/cityCards/cityCardsVerso.png").toExternalForm());
        StackPane drawPile = createCardStack(verso, 6, true);
        Image topCard;

        try {
            String path = "/cityCards/BlueAtlanta.png"; // example
            var res = getClass().getResource(path);

            if (res != null) { topCard = new Image(res.toExternalForm()); } 
            else {topCard = verso; }
        } catch (Exception e) { topCard = verso; }

        StackPane discardPile = createCardStack(topCard, 4, false);
        HBox container = new HBox(drawPile, discardPile);
        container.setSpacing(20);
        GlowUtil.applyGlow(drawPile, "#00d9ff", 8);
        GlowUtil.applyGlow(discardPile, "#00d9ff", 8);
        return container;
    }

    private HBox createEpidemicDeck() {

        Image verso = new Image(getClass().getResource("/epidemicCards/epidemicCardsVerso.png").toExternalForm());
        StackPane drawPile = createCardStack(verso, 5, true);
        Image topCard;
        try {
            String path = "/epidemicCards/BlueAtlanta.png"; // example
            var res = getClass().getResource(path);
            topCard = (res != null) ? new Image(res.toExternalForm()) : verso;
        } catch (Exception e) { topCard = verso; }

        StackPane discardPile = createCardStack(topCard, 3, false);
        HBox container = new HBox(drawPile, discardPile);
        container.setSpacing(20);
        GlowUtil.applyGlow(drawPile, "#ff2d2d", 8);
        GlowUtil.applyGlow(discardPile, "#ff2d2d", 8);
        return container;
    }
    
    private void setupDecks() {

        HBox decks = new HBox(createCityDeck(), createEpidemicDeck());

        decks.setSpacing(30);

        StackPane wrapper = new StackPane(decks);
        wrapper.setPickOnBounds(false);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.getChildren().add(wrapper);

        StackPane.setAlignment(wrapper, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(wrapper, new Insets(0, 60, 10, 0));
    }
}