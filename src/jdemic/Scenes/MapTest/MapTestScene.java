package jdemic.Scenes.MapTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.GlowUtil;
import jdemic.GameLogic.*;
import javafx.scene.layout.VBox;
import jdemic.ui.GameplayUI.*;
import javafx.scene.Node;

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
    private Set<CityNode> highlightedValidNodes = new HashSet<>();

    //Variable for game manager (temp)
    private GameManager gameManager;

    //Variables for InfectionManager
    private InfectionRateManager infectionRateManager;

    //Variables for OutbreakManager
    private OutbreakManager outbreakManager;

    //Variables for CureManager
    private CureManager cureManager;

    //Variable for gameplay chat
    private ChatManager chatManager;

    //Variable for decks
    private DeckManager deckManager;

    //Variable player hand
    private HandManager handManager;

    // Add to MapTestScene class fields
    private PlayerListUI playerListUI;

    //Variables for connected gameplay
    private GameClient gameClient;
    
    //Variable for current player display
    private Label currentPlayerLabel;
    private String playerName = "Tester";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, PawnUI> playerPawns = new HashMap<>();
    private Map<CityNode, List<String>> cityOccupants = new HashMap<>();

    public MapTestScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        this.mapGraph = new PandemicMapGraph();
        addDefaultResearchStationToSceneMap();

        this.gameManager = createLocalGameManager();
        initializeScene();
    }

    public MapTestScene(Stage stage, String playerName, GameClient gameClient, JsonNode gameState) {
        this.stage = stage;
        this.root = new StackPane();
        this.mapGraph = new PandemicMapGraph();
        addDefaultResearchStationToSceneMap();
        this.gameClient = gameClient;
        this.playerName = playerName == null || playerName.isBlank() ? "PLAYER" : playerName.toUpperCase();

        this.gameManager = createNetworkGameManager(gameState);
        initializeScene();

        if (this.gameClient != null) {
            this.gameClient.addPlayerUpdateListener(updatedGameState ->
                    Platform.runLater(() -> applyGameStateSnapshot(updatedGameState))
            );
        }
    }

    private void initializeScene() {
        this.root.setStyle("-fx-background-color: #050a14;");
        setupBackground();
        setupContent();
        setupUI();
        setupNotifications();
        setupActionMenu();
        setupGlobalHUD();
        setupChat();
        deckManager = new DeckManager(root);
        handManager = new HandManager(root, deckManager);
        updateHandUI();

        Platform.runLater(() -> {
            updatePawnPositions();
        });

        Color[] playerColors = {Color.CYAN, Color.MAGENTA, Color.LIME, Color.ORANGE};
        this.playerListUI = new PlayerListUI(gameManager.getState().getPlayers(), playerColors);
        root.getChildren().add(playerListUI.getContainer()); // Add to top level StackPane

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Use Filter instead of Handler to catch TAB specifically
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.TAB) {
                        playerListUI.setVisible(true);
                        event.consume(); // Prevents focus from jumping to buttons
                    }
                });

                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.TAB) {
                        playerListUI.setVisible(false);
                        event.consume();
                    }
                });

                // Keep your other debug keys as standard handlers
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.I) {
                        if (gameManager.getState().getInfectionRate() < gameManager.getInfectionRateTrack().length - 1) {
                            gameManager.increaseInfectionRate();
                            infectionRateManager.updateTrack();
                            notificationManager.showNotification("DEBUG: Infection Rate Increased!");
                        } else {
                            notificationManager.showNotification("DEBUG: Infection Rate is maxed");
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

    // Inside setupContent() or initializeScene()
    private void setupPawns(Pane mapPane) {
        Color[] colors = {Color.CYAN, Color.MAGENTA, Color.LIME, Color.ORANGE};
        int i = 0;

        for (PlayerState player : gameManager.getState().getPlayers()) {
            PawnUI pawn = new PawnUI(player.getPlayerName(), mapPane.heightProperty(), colors[i % colors.length]);
            playerPawns.put(player.getPlayerName(), pawn);
            mapPane.getChildren().add(pawn.getNode());
            i++;
        }
        updatePawnPositions();
    }

    private void updatePawnPositions() {
        cityOccupants.clear();

        // 1. Group player names by their current CityNode[cite: 6]
        for (PlayerState player : gameManager.getState().getPlayers()) {
            CityNode city = player.getPlayerCurrentCity(); // Using the correct method from source 6
            if (city != null) {
                cityOccupants.computeIfAbsent(city, k -> new ArrayList<>()).add(player.getPlayerName());
            }
        }

        // 2. Iterate through cities that have occupants
        for (Map.Entry<CityNode, List<String>> entry : cityOccupants.entrySet()) {
            CityNode city = entry.getKey();
            List<String> playersInCity = entry.getValue();
            int count = playersInCity.size();

            Circle cityVisual = nodeVisuals.get(city); // Get visual node created in setupContent
            if (cityVisual == null) continue;

            for (int i = 0; i < count; i++) {
                String playerName = playersInCity.get(i);
                PawnUI pawnUI = playerPawns.get(playerName);

                if (pawnUI == null) continue;

                pawnUI.unbindPosition();

                if (count == 1) {
                    // Centered position
                    pawnUI.bindToCenter(cityVisual.centerXProperty(), cityVisual.centerYProperty());
                } else {
                    // Radial offset to prevent overlapping
                    double angle = 2 * Math.PI * i / count;
                    double offset = 14.0;
                    pawnUI.bindWithOffset(cityVisual.centerXProperty(), cityVisual.centerYProperty(), angle, offset);
                }
            }
        }
    }

    private GameManager createLocalGameManager() {
        List<PlayerState> players = new ArrayList<>();
        CityNode startingCity = mapGraph.getCity("Atlanta");
        PlayerState testPlayer = new PlayerState(playerName);
        testPlayer.setCurrentCity(startingCity);
        players.add(testPlayer);
        return new GameManager(players);
    }

    private GameManager createNetworkGameManager(JsonNode gameState) {
        List<PlayerState> players = createPlayersFromSnapshot(gameState);
        if (players.isEmpty()) {
            players.add(new PlayerState(playerName));
        }

        GameManager manager = new GameManager(players, false);
        applyGameStateSnapshot(manager, gameState);
        return manager;
    }

    private List<PlayerState> createPlayersFromSnapshot(JsonNode gameState) {
        List<PlayerState> players = new ArrayList<>();
        JsonNode playersArray = getPlayersArray(gameState);
        if (playersArray == null || !playersArray.isArray()) {
            return players;
        }

        for (JsonNode playerNode : playersArray) {
            String name = playerNode.has("playerName") ? playerNode.get("playerName").asText() : "PLAYER";
            PlayerState playerState = new PlayerState(name);
            CityNode currentCity = getCityFromPlayerNode(playerNode, mapGraph);
            if (currentCity != null) {
                playerState.setCurrentCity(currentCity);
            }
            players.add(playerState);
        }
        return players;
    }

    private void applyGameStateSnapshot(JsonNode gameState) {
        if (gameState == null) {
            return;
        }

        applyGameStateSnapshot(gameManager, gameState);
        if (actionMenuManager != null) actionMenuManager.updateMenuState();
        updateCurrentPlayerLabel();
        updateHandUI();
        if (infectionRateManager != null) infectionRateManager.updateTrack();
        if (outbreakManager != null) outbreakManager.updateTrack();
        if (cureManager != null) cureManager.updateUI();
        if (chatManager != null) chatManager.updateMessages(gameState.get("lobbyChatMessages"));
        Platform.runLater(this::updatePawnPositions);
    }

    private void applyGameStateSnapshot(GameManager manager, JsonNode gameState) {
        if (manager == null || gameState == null) {
            return;
        }

        if (gameState.has("actionsRemaining")) {
            manager.getState().setActionsRemaining(gameState.get("actionsRemaining").asInt());
        }
        if (gameState.has("infectionRate")) {
            manager.getState().setInfectionRate(gameState.get("infectionRate").asInt());
        }
        if (gameState.has("epidemicCount")) {
            manager.getState().setEpidemicCount(gameState.get("epidemicCount").asInt());
        }
        if (gameState.has("gameOver")) {
            manager.getState().setGameOver(gameState.get("gameOver").asBoolean());
        }
        if (gameState.has("gameWon")) {
            manager.getState().setGameWon(gameState.get("gameWon").asBoolean());
        }
        if (gameState.has("gameStarted")) {
            manager.getState().setGameStarted(gameState.get("gameStarted").asBoolean());
        }

        JsonNode playersArray = getPlayersArray(gameState);
        if (playersArray != null && playersArray.isArray()) {
            int index = 0;
            for (JsonNode playerNode : playersArray) {
                if (index >= manager.getState().getPlayers().size()) {
                    break;
                }

                CityNode currentCity = getCityFromPlayerNode(playerNode, this.mapGraph);
                if (currentCity != null) {
                    manager.getState().getPlayers().get(index).setCurrentCity(currentCity);
                }

                if (playerNode.has("hand") && playerNode.get("hand").isArray()) {
                    List<Card> hand = manager.getState().getPlayers().get(index).getHand();
                    hand.clear();
                    for (JsonNode cardNode : playerNode.get("hand")) {
                        Card card = getCardFromNode(cardNode, this.mapGraph);
                        if (card != null) {
                            hand.add(card);
                        }
                    }
                }

                if (playerNode.has("isDiscarding")) {
                    manager.getState().getPlayers().get(index).setIsDiscarding(playerNode.get("isDiscarding").asBoolean());
                } else if (playerNode.has("discardingCards")) {
                    manager.getState().getPlayers().get(index).setIsDiscarding(playerNode.get("discardingCards").asBoolean());
                }
                index++;
            }
        }

        if (gameState.has("currentPlayerIndex") && !manager.getState().getPlayers().isEmpty()) {
            int currentPlayerIndex = gameState.get("currentPlayerIndex").asInt();
            int maxPlayerIndex = manager.getState().getPlayers().size() - 1;
            manager.getState().setCurrentPlayerIndex(Math.max(0, Math.min(currentPlayerIndex, maxPlayerIndex)));
        }
    }

    private JsonNode getPlayersArray(JsonNode gameState) {
        if (gameState == null) {
            return null;
        }
        if (gameState.has("players")) {
            return gameState.get("players");
        }
        if (gameState.has("playerArray")) {
            return gameState.get("playerArray");
        }
        return null;
    }

    private CityNode getCityFromPlayerNode(JsonNode playerNode, PandemicMapGraph graph) {
        if (playerNode == null || graph == null) {
            return null;
        }

        JsonNode cityNode = playerNode.get("playerCurrentCity");
        if (cityNode == null) {
            cityNode = playerNode.get("currentCity");
        }

        if (cityNode == null) {
            return null;
        }

        String cityName = null;
        if (cityNode.isTextual()) {
            cityName = cityNode.asText();
        } else if (cityNode.has("name")) {
            cityName = cityNode.get("name").asText();
        }

        CityNode city = cityName == null ? null : graph.getCity(cityName);
        if (city != null && cityNode.has("hasResearchStation") && cityNode.get("hasResearchStation").asBoolean()) {
            city.addResearchStation();
        }
        if (city != null && cityNode.has("researchStation") && cityNode.get("researchStation").asBoolean()) {
            city.addResearchStation();
        }
        return city;
    }

    private Card getCardFromNode(JsonNode cardNode, PandemicMapGraph graph) {
        if (cardNode == null || !cardNode.has("cardName") || !cardNode.has("type")) {
            return null;
        }

        String cardName = cardNode.get("cardName").asText();
        CardType type;
        try {
            type = CardType.valueOf(cardNode.get("type").asText());
        } catch (IllegalArgumentException ex) {
            return null;
        }

        CityNode targetCity = null;
        JsonNode targetCityNode = cardNode.get("targetCity");
        if (targetCityNode != null && graph != null) {
            String cityName = null;
            if (targetCityNode.isTextual()) {
                cityName = targetCityNode.asText();
            } else if (targetCityNode.has("name")) {
                cityName = targetCityNode.get("name").asText();
            }
            targetCity = cityName == null ? null : graph.getCity(cityName);
        }

        Card card = new Card(cardName, type, targetCity);
        if (cardNode.has("eventType") && !cardNode.get("eventType").isNull()) {
            try {
                card.setEventType(Card.EventType.valueOf(cardNode.get("eventType").asText()));
            } catch (IllegalArgumentException ignored) {
                // Event type is optional for non-event cards and older snapshots.
            }
        }
        return card;
    }

    private void addDefaultResearchStationToSceneMap() {
        CityNode atlanta = mapGraph.getCity("Atlanta");
        if (atlanta != null) {
            atlanta.addResearchStation();
        }
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

        this.outbreakManager = new OutbreakManager(root, gameManager);
        this.infectionRateManager = new InfectionRateManager(root, gameManager);
        this.cureManager = new CureManager(root, gameManager);

        HBox outbreakBox = new HBox(outbreakManager.getContainer());
        outbreakBox.setAlignment(Pos.CENTER_LEFT);
        outbreakBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(outbreakBox, Pos.TOP_LEFT);
        StackPane.setMargin(outbreakBox, new Insets(40, 0, 0, 40));

        HBox infectionBox = new HBox(infectionRateManager.getContainer());
        infectionBox.setAlignment(Pos.CENTER);
        infectionBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(infectionBox, Pos.TOP_CENTER);
        StackPane.setMargin(infectionBox, new Insets(40, 0, 0, 0));

        VBox curesBox = cureManager.getContainer();
        curesBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(curesBox, Pos.TOP_RIGHT);
        StackPane.setMargin(curesBox, new Insets(100, 40, 0, 0));

        // Create current player display label
        currentPlayerLabel = new Label();
        currentPlayerLabel.setFont(Font.font("hkmodular", FontWeight.BOLD, 20));
        currentPlayerLabel.setTextFill(Color.web("#d1d412"));
        currentPlayerLabel.setStyle("-fx-effect: dropshadow(gaussian, #d1d412, 10, 0.5, 0, 0);");
        updateCurrentPlayerLabel();
        
        StackPane.setAlignment(currentPlayerLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(currentPlayerLabel, new Insets(0, 0, 40, 0));

        root.getChildren().addAll(outbreakBox, infectionBox, curesBox, currentPlayerLabel);
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
        // For network games, pass playerName to enforce turn-based validation
        // For local games, pass null to allow any player to act during their turn
        String playerNameForMenu = (gameClient != null) ? playerName : null;
        
        this.actionMenuManager = gameClient == null
                ? new ActionMenuManager(root, notificationManager, gameManager, null, this::highlightValidNodes, playerNameForMenu, this::refreshTurnUI)
                : new ActionMenuManager(root, notificationManager, gameManager, this::sendMenuActionPacket, this::highlightValidNodes, playerNameForMenu, this::refreshTurnUI);
    }

    private void setupChat()
    {
        String playerName = gameManager.getCurrentPlayer().getPlayerName();
        this.chatManager = new ChatManager(root, playerName, notificationManager, gameClient);
    }

    private void updateHandUI() {
        if (handManager == null || gameManager == null) {
            return;
        }

        PlayerState displayedPlayer = getDisplayedHandPlayer();
        boolean discardMode = displayedPlayer != null
                && gameManager.getState().isPlayerTurn(displayedPlayer)
                && displayedPlayer.getIsDiscarding();

        handManager.updateHand(displayedPlayer, discardMode, this::handleDiscardCardRequest);
    }

    private void refreshTurnUI() {
        updateCurrentPlayerLabel();
        updateHandUI();
    }

    private PlayerState getDisplayedHandPlayer() {
        if (gameManager == null || gameManager.getState() == null) {
            return null;
        }

        if (gameClient != null) {
            for (PlayerState player : gameManager.getState().getPlayers()) {
                if (player.getPlayerName().equalsIgnoreCase(playerName)) {
                    return player;
                }
            }
        }

        return gameManager.getState().getCurrentPlayer();
    }

    private void handleDiscardCardRequest(int cardIndex) {
        PlayerState displayedPlayer = getDisplayedHandPlayer();
        if (displayedPlayer == null || !displayedPlayer.getIsDiscarding()) {
            return;
        }

        if (gameClient != null) {
            sendDiscardCardPacket(cardIndex);
            notificationManager.showNotification("Discard request sent");
        } else {
            gameManager.discardCurrentPlayerCard(displayedPlayer, cardIndex);
            notificationManager.showNotification("Card discarded");
            if (actionMenuManager != null) actionMenuManager.updateMenuState();
            updateCurrentPlayerLabel();
            updateHandUI();
        }
    }

    /**
     * Updates the current player label with the name of the player whose turn it is
     */
    private void updateCurrentPlayerLabel() {
        if (currentPlayerLabel != null && gameManager != null) {
            PlayerState current = gameManager.getState().getCurrentPlayer();
            if (current != null) {
                currentPlayerLabel.setText("CURRENT TURN: " + current.getPlayerName().toUpperCase());
            }
        }
    }

    private void sendMenuActionPacket(String action) {
        if (gameClient == null) {
            return;
        }

        ObjectNode payload = createBaseGameActionPayload();
        payload.put("GameAction", action);
        gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
    }

    private void sendDriveFerryPacket(CityNode destination) {
        if (gameClient == null || destination == null) {
            return;
        }

        ObjectNode payload = createBaseGameActionPayload();
        payload.put("GameAction", "DRIVE_FERRY");
        payload.put("destination", destination.getName());
        gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
    }

    private void sendDiscardCardPacket(int cardIndex) {
        if (gameClient == null) {
            return;
        }

        ObjectNode payload = createBaseGameActionPayload();
        payload.put("GameAction", "DISCARD_CARD");
        payload.put("cardIndex", cardIndex);
        gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
    }

    private void highlightValidNodes(String movementType) {
        // Clear previous highlights
        for (CityNode city : highlightedValidNodes) {
            Circle nodeCircle = nodeVisuals.get(city);
            if (nodeCircle != null) {
                Color nativeColor = getFxColor(city.getNativeColor());
                nodeCircle.setFill(nativeColor);
                nodeCircle.setEffect(null);
            }
        }
        highlightedValidNodes.clear();

        if (movementType == null || movementType.isEmpty()) {
            return;
        }

        // Get current player's city
        PlayerState currentPlayer = gameManager.getCurrentPlayer();
        if (currentPlayer == null) {
            return;
        }

        CityNode currentCity = currentPlayer.getPlayerCurrentCity();
        if (currentCity == null) {
            return;
        }

        // For DRIVE/FERRY, highlight all connected cities
        if ("DRIVE_FERRY".equals(movementType)) {
            for (CityNode neighbor : currentCity.getConnectedCities()) {
                highlightedValidNodes.add(neighbor);
                Circle nodeCircle = nodeVisuals.get(neighbor);
                if (nodeCircle != null) {
                    nodeCircle.setFill(Color.web("#00ff00"));
                    
                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                    glow.setColor(Color.web("#00ff00"));
                    glow.setRadius(20);
                    glow.setSpread(0.6);
                    nodeCircle.setEffect(glow);
                    
                    // Add click handler for movement
                    nodeCircle.setOnMouseClicked(ev -> {
                        // Consume an action point
                        int actionsLeft = gameManager.getState().getActionsRemaining();
                        if (actionsLeft > 0) {
                            gameManager.getState().setActionsRemaining(actionsLeft - 1);
                            if (gameClient == null && gameManager.getState().getActionsRemaining() <= 0) {
                                gameManager.nextTurn();
                            }
                            
                            sendDriveFerryPacket(neighbor);
                            notificationManager.showNotification(
                                    gameClient == null
                                            ? "Moved to " + neighbor.getName() + ". Actions left: " + gameManager.getState().getActionsRemaining()
                                            : "Move request sent: " + neighbor.getName()
                            );
                            
                            // Clear highlights and reset movement selection
                            highlightValidNodes(""); 
                            if (actionMenuManager != null) {
                                actionMenuManager.clearSelectedMovementAction();
                            }
                            refreshTurnUI();
                        } else {
                            notificationManager.showNotification("No actions remaining!");
                        }
                    });
                }
            }
        }
    }

    private ObjectNode createBaseGameActionPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("PlayerID", playerName);
        return payload;
    }

    private void setupContent() {
        StackPane mapContainer = new StackPane();
        mapContainer.setAlignment(Pos.CENTER);
        mapContainer.translateYProperty().bind(root.heightProperty().multiply(-0.10));
        mapContainer.prefWidthProperty().bind(root.widthProperty());
        mapContainer.prefHeightProperty().bind(root.heightProperty());
        mapContainer.setPickOnBounds(false);

        Pane mapPane = new Pane();
        mapPane.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.min(root.getWidth() * 0.70, root.getHeight() * 1.38),
                root.widthProperty(), root.heightProperty()
        ));
        mapPane.prefHeightProperty().bind(mapPane.prefWidthProperty().multiply(0.5));
        mapPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

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

            node.setOnMouseClicked(ev -> {
                city.clickEvent();
            });

            Text label = new Text(city.getName());
            label.setFill(Color.WHITE);
            label.setFont(Font.font("hkmodular", FontWeight.BOLD, 10));
            label.setMouseTransparent(true);

            label.layoutXProperty().bind(Bindings.createDoubleBinding(() -> node.getCenterX() - label.getLayoutBounds().getWidth() / 2, node.centerXProperty(),label.layoutBoundsProperty()));
            label.layoutYProperty().bind(node.centerYProperty().add(20));

            mapPane.getChildren().addAll(node, label);
        }
        setupPawns(mapPane);
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
}
