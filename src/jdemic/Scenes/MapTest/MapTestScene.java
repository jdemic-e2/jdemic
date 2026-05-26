package jdemic.Scenes.MapTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.DedicatedServer.network.transport.Packet;
import jdemic.DedicatedServer.network.transport.PacketType;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.Scenes.Settings.AudioManager;
import jdemic.GameLogic.Actions.GameAction;
import jdemic.GameLogic.Actions.FirewallAction;
import jdemic.GameLogic.Actions.Other.BuildResearchStation;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.DeckAnimationManager;
import jdemic.ui.PauseMenuOverlay;
import jdemic.ui.SafeResourceLoader;
import jdemic.ui.SceneBackgroundUtil;
import jdemic.ui.TurnAnimationManager;
import jdemic.ui.CardResourceUtil;
import jdemic.GameLogic.*;
import jdemic.GameLogic.RoleManager;
import jdemic.GameLogic.PlayerRoles;
import javafx.scene.layout.VBox;
import jdemic.ui.GameplayUI.*;
import jdemic.ui.MapZoomPanHandler;

import java.util.*;
import java.util.function.Consumer;

public class MapTestScene {
    private static final String FONT_HKMODULAR = "hkmodular";
    private static final String DEFAULT_LOCAL_PLAYER_NAME = "Tester";
    private static final String DEFAULT_PLAYER_NAME = "PLAYER";
    private static final String CYAN_COLOR = "#00b5d4";
    private static final String BRIGHT_CYAN_COLOR = "#00d4ff";
    private static final String HIGHLIGHT_GREEN_COLOR = "#00ff00";
    private static final String YELLOW_COLOR = "#d1d412";
    private static final String RED_COLOR = "#ff2d2d";
    private static final String BLACK_COLOR = "#333333";
    private static final String MAP_BACKGROUND_RESOURCE = "/backgroundMap.png";
    private static final String GAME_BACKGROUND_RESOURCE = "/bgGame.png";
    private static final String SCENE_MAIN_MENU = "MAIN_MENU";
    private static final String JSON_ACTIONS_REMAINING = "actionsRemaining";
    private static final String JSON_INFECTION_RATE = "infectionRate";
    private static final String JSON_EPIDEMIC_COUNT = "epidemicCount";
    private static final String JSON_GAME_OVER = "gameOver";
    private static final String JSON_GAME_WON = "gameWon";
    private static final String JSON_GAME_STARTED = "gameStarted";
    private static final String JSON_HAND = "hand";
    private static final String JSON_IS_DISCARDING = "isDiscarding";
    private static final String JSON_DISCARDING_CARDS = "discardingCards";
    private static final String JSON_CURRENT_PLAYER_INDEX = "currentPlayerIndex";
    private static final String JSON_MAP = "map";
    private static final String JSON_CITY_LIST = "cityList";
    private static final String JSON_NAME = "name";
    private static final String JSON_RESEARCH_STATION = "researchStation";
    private static final String JSON_HAS_RESEARCH_STATION = "hasResearchStation";
    private static final String JSON_DISEASE_CUBES = "diseaseCubes";
    private static final String JSON_PLAYERS = "players";
    private static final String JSON_PLAYER_ARRAY = "playerArray";
    private static final String JSON_PLAYER_NAME = "playerName";
    private static final String JSON_PLAYER_CURRENT_CITY = "playerCurrentCity";
    private static final String JSON_CURRENT_CITY = "currentCity";
    private static final String JSON_CARD_NAME = "cardName";
    private static final String JSON_TYPE = "type";
    private static final String JSON_TARGET_CITY = "targetCity";
    private static final String JSON_EVENT_TYPE = "eventType";
    private static final String JSON_LOBBY_CHAT_MESSAGES = "lobbyChatMessages";
    private static final String PAYLOAD_GAME_ACTION = "GameAction";
    private static final String PAYLOAD_DESTINATION = "destination";
    private static final String PAYLOAD_CARD_INDEX = "cardIndex";
    private static final String PAYLOAD_COLOR = "color";
    private static final String PAYLOAD_CARD_INDICES = "cardIndices";
    private static final String PAYLOAD_TARGET_PLAYER = "targetPlayer";
    private static final String PAYLOAD_DIRECTION = "direction";
    private static final String PAYLOAD_PLAYER_ID = "PlayerID";
    private static final String DIRECTION_GIVE = "give";
    private static final String ACTION_DRIVE_FERRY = "DRIVE_FERRY";
    private static final String ACTION_SHUTTLE_FLIGHT = "SHUTTLE_FLIGHT";
    private static final String ACTION_DIRECT_FLIGHT = "DIRECT_FLIGHT";
    private static final String ACTION_CHARTER_FLIGHT = "CHARTER_FLIGHT";
    private static final String ACTION_TREAT_DISEASE = "TREAT_DISEASE";
    private static final String ACTION_DISCOVER_CURE = "DISCOVER_CURE";
    private static final String ACTION_SHARE_KNOWLEDGE = "SHARE_KNOWLEDGE";
    private static final String ACTION_BUILD_RESEARCH_STATION = "BUILD_RESEARCH_STATION";
    private static final String ACTION_DISCARD_CARD = "DISCARD_CARD";

    private Stage stage;
    private StackPane root;
    private PandemicMapGraph mapGraph;
    private Map<CityNode, Circle> nodeVisuals = new HashMap<>();
    private Map<String, Line> edgeVisuals = new HashMap<>();

    // UI helpers for virus visuals per city
    private Map<CityNode, jdemic.ui.GameplayUI.Viruses.CityVirusGroupUI> cityVirusUIs = new HashMap<>();
    private Map<CityNode, ResearchStationUI> researchStationUIs = new HashMap<>();

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
    private DeckAnimationManager deckAnimationManager;

    // Add to MapTestScene class fields
    private PlayerListUI playerListUI;

    //Variables for connected gameplay
    private GameClient gameClient;
    
    private String playerName = "Tester";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, PawnUI> playerPawns = new HashMap<>();
    private Map<CityNode, List<String>> cityOccupants = new HashMap<>();

    private GameClient.PlayerUpdateListener playerUpdateListener;
    private Pane mapPane;

    private PauseMenuOverlay pauseMenuOverlay;
    private HBox pauseHeader;
    private ButtonsUtil pauseBtn;
    private TurnAnimationManager animationManager;
    private Consumer<CityNode> outbreakAnimationListener;
    private ChangeListener<Scene> sceneListener;
    private EventHandler<KeyEvent> keyPressedFilter;
    private EventHandler<KeyEvent> keyReleasedFilter;
    private EventHandler<KeyEvent> debugKeyHandler;
    private boolean returningToMainMenu;

    public MapTestScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        SceneManager.registerLifecycle(root, this::cleanupScene);
        this.mapGraph = new PandemicMapGraph();

        this.gameManager = createLocalGameManager();
        initializeScene();
    }

    public MapTestScene(Stage stage, String playerName, GameClient gameClient, JsonNode gameState) {
        this.stage = stage;
        this.root = new StackPane();
        SceneManager.registerLifecycle(root, this::cleanupScene);
        this.mapGraph = new PandemicMapGraph();
        addDefaultResearchStationToSceneMap();
        this.gameClient = gameClient;
        this.playerName = playerName == null || playerName.isBlank() ? "PLAYER" : playerName.toUpperCase();

        this.gameManager = createNetworkGameManager(gameState);
        initializeScene();

        if (this.gameClient != null) {
            playerUpdateListener = updatedGameState ->
                    Platform.runLater(() -> applyGameStateSnapshot(updatedGameState));
            this.gameClient.addPlayerUpdateListener(playerUpdateListener);
        }
    }

    private void initializeScene() {
        AudioManager.getInstance().playGameMusic();
        this.root.setStyle("-fx-background-color: #050a14;");
        setupBackground();
        setupContent();
        animationManager = new TurnAnimationManager(root);
        setupPauseMenu();
        setupNotifications();
        setupActionMenu();
        setupGlobalHUD();
        setupChat();
        deckManager = new DeckManager(root);
        handManager = new HandManager(root, deckManager);
        deckAnimationManager = new DeckAnimationManager(root, deckManager, handManager);
        updateHandUI();
        playStartupShuffleAnimation();

        Platform.runLater(() -> {
            updatePawnPositions();
        });

        Color[] playerColors = {Color.CYAN, Color.MAGENTA, Color.LIME, Color.ORANGE};
        this.playerListUI = new PlayerListUI(gameManager.getState().getPlayers(), playerColors);
        root.getChildren().add(playerListUI.getContainer()); // Add to top level StackPane

        setupUI();
        bringPauseControlToFront();

        keyPressedFilter = event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (pauseMenuOverlay != null) {
                    pauseMenuOverlay.toggle();
                    event.consume();
                }
            } else if (event.getCode() == javafx.scene.input.KeyCode.TAB) {
                playerListUI.setVisible(true);
                event.consume(); // Prevents focus from jumping to buttons
            }
        };
        keyReleasedFilter = event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.TAB) {
                playerListUI.setVisible(false);
                event.consume();
            }
        };
        debugKeyHandler = event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.I) {
                if (gameManager.getState().getInfectionRate() < gameManager.getInfectionRateTrack().length - 1) {
                    gameManager.increaseInfectionRate();
                    notificationManager.showNotification("DEBUG: Infection Rate Increased!");
                } else {
                    notificationManager.showNotification("DEBUG: Infection Rate is maxed");
                }
            }
            if (event.getCode() == javafx.scene.input.KeyCode.O) {
                gameManager.getState().getDiseaseManager().increaseOutbreakScore();
                outbreakManager.updateTrack();
                PlayerState currentPlayer = gameManager.getCurrentPlayer();
                outbreakManager.queueOutbreak(currentPlayer == null ? null : currentPlayer.getPlayerCurrentCity());
                notificationManager.showNotification("DEBUG: Outbreak Occurred!");
            }
        };

        sceneListener = (obs, oldScene, newScene) -> {
            detachSceneHandlers(oldScene);
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedFilter);
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleasedFilter);
                newScene.setOnKeyPressed(debugKeyHandler);
            }
        };
        root.sceneProperty().addListener(sceneListener);
    }

    private void detachSceneHandlers(Scene scene) {
        if (scene == null) {
            return;
        }
        if (keyPressedFilter != null) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedFilter);
        }
        if (keyReleasedFilter != null) {
            scene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleasedFilter);
        }
        if (scene.getOnKeyPressed() == debugKeyHandler) {
            scene.setOnKeyPressed(null);
        }
    }

    // Inside setupContent() or initializeScene()
    private void setupPawns(Pane mapPane) {
        this.mapPane = mapPane;
        String[] pawnImages = {
                "/playerPins/PinRoleArtificialIntelligenceAnalyst.png",
                "/playerPins/PinRoleEncryptionSpecialist.png",
                "/playerPins/PinRoleFirewallSpecialist.png",
                "/playerPins/PinRoleIncidentResponder.png",
                "/playerPins/PinRoleNetworkController.png",
                "/playerPins/PinRoleSystemEngineer.png",
                "/playerPins/PinRoleThreatStrategist.png"
        };
        int i = 0;

        for (PlayerState player : gameManager.getState().getPlayers()) {
            PawnUI pawn = new PawnUI(player.getPlayerName(), mapPane.heightProperty(), pawnImages[i % pawnImages.length], player.getPlayerRole());
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

                // Ensure the pawn displays the latest role from the game state
                PlayerState ps = getPlayerStateByName(playerName);
                if (ps != null) pawnUI.setRole(ps.getPlayerRole());

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
        PlayerState testPlayer = new PlayerState(playerName);
        players.add(testPlayer);
        // Ensure local test players get assigned roles so the UI can display them
        RoleManager.assignRandomRoles(players);
        GameManager manager = new GameManager(players, mapGraph);
        // Ensure initial infections and setup occur for local single-player scenes
        manager.startGame();
        return manager;
    }

    private GameManager createNetworkGameManager(JsonNode gameState) {
        List<PlayerState> players = createPlayersFromSnapshot(gameState);
        if (players.isEmpty()) {
            players.add(new PlayerState(playerName));
        }

        GameManager manager = new GameManager(players, mapGraph, false);
        applyGameStateSnapshot(manager, gameState);
        return manager;
    }

    private PlayerState getPlayerStateByName(String name) {
        if (name == null || gameManager == null || gameManager.getState() == null) return null;
        for (PlayerState p : gameManager.getState().getPlayers()) {
            if (name.equals(p.getPlayerName())) return p;
        }
        return null;
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
            // If snapshot contains a role, apply it to the PlayerState
            if (playerNode.has("playerRole") && !playerNode.get("playerRole").isNull()) {
                try {
                    PlayerRoles role = PlayerRoles.valueOf(playerNode.get("playerRole").asText());
                    playerState.setPlayerRole(role);
                } catch (IllegalArgumentException ignored) {
                }
            }
            players.add(playerState);
        }
        return players;
    }

    private void applyGameStateSnapshot(JsonNode gameState) {
        if (gameState == null) {
            return;
        }

        AnimationSnapshot before = createAnimationSnapshot();
        applyGameStateSnapshot(gameManager, gameState);
        if (actionMenuManager != null) actionMenuManager.updateMenuState();
        updateHandUI();
        if (infectionRateManager != null) infectionRateManager.updateTrack();
        if (outbreakManager != null) outbreakManager.updateTrack();
        if (cureManager != null) cureManager.updateUI();
        if (chatManager != null) chatManager.updateMessages(gameState.get("lobbyChatMessages"));
        Platform.runLater(() -> {
            syncResearchStationVisuals();
            if (cureManager != null) cureManager.updateUI();
            updatePawnPositions();
            playSnapshotAnimations(before);
        });
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
        applyMapSnapshot(gameState);
        applyDiseaseManagerSnapshot(manager, gameState);

        // Refresh virus visuals to reflect any changes applied to city cube counts
        updateVirusVisuals();

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
                // Update player role if snapshot includes it
                if (playerNode.has("playerRole") && !playerNode.get("playerRole").isNull()) {
                    try {
                        PlayerRoles role = PlayerRoles.valueOf(playerNode.get("playerRole").asText());
                        manager.getState().getPlayers().get(index).setPlayerRole(role);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                index++;
            }
        }

        if (gameState.has("currentPlayerIndex") && !manager.getState().getPlayers().isEmpty()) {
            int currentPlayerIndex = gameState.get("currentPlayerIndex").asInt();
            int maxPlayerIndex = manager.getState().getPlayers().size() - 1;
            manager.getState().setCurrentPlayerIndex(Math.max(0, Math.min(currentPlayerIndex, maxPlayerIndex)));
        }

        syncResearchStationVisuals();
    }

    private void applyMapSnapshot(JsonNode gameState) {
        if (gameState == null || !gameState.has("map")) {
            return;
        }

        JsonNode mapNode = gameState.get("map");
        JsonNode cityList = mapNode.get("cityList");
        if (cityList == null || !cityList.isArray()) {
            return;
        }

        for (JsonNode cityNode : cityList) {
            if (!cityNode.has("name")) {
                continue;
            }

            CityNode localCity = mapGraph.getCity(cityNode.get("name").asText());
            if (localCity == null) {
                continue;
            }

            JsonNode researchStation = cityNode.has("researchStation")
                    ? cityNode.get("researchStation")
                    : cityNode.get("hasResearchStation");
            if (researchStation != null && researchStation.asBoolean()) {
                localCity.addResearchStation();
            } else if (researchStation != null) {
                localCity.removeResearchStation();
            }
            updateResearchStationVisual(localCity);

            JsonNode diseaseCubes = cityNode.get("diseaseCubes");
            if (diseaseCubes != null && diseaseCubes.isObject()) {
                for (DiseaseColor color : DiseaseColor.values()) {
                    JsonNode cubeCount = diseaseCubes.get(color.name());
                    if (cubeCount != null && cubeCount.isNumber()) {
                        localCity.setDiseaseCubeCount(color, cubeCount.asInt());
                    }
                }
            }
        }
    }

    private void applyDiseaseManagerSnapshot(GameManager manager, JsonNode gameState) {
        if (manager == null || manager.getState().getDiseaseManager() == null || gameState == null) {
            return;
        }

        JsonNode diseaseManagerNode = gameState.get("diseaseManager");
        if (diseaseManagerNode == null || !diseaseManagerNode.isObject()) {
            return;
        }

        DiseaseManager diseaseManager = manager.getState().getDiseaseManager();
        for (DiseaseColor color : DiseaseColor.values()) {
            Boolean cured = getCuredStateFromSnapshot(diseaseManagerNode, color);
            if (cured != null) {
                diseaseManager.setCured(color, cured);
            }
        }

        JsonNode outbreakScore = diseaseManagerNode.get("outbreakScore");
        if (outbreakScore != null && outbreakScore.isNumber()) {
            manager.getState().getDiseaseManager().setOutbreakScore(outbreakScore.asInt());
        }

        // Recompute cube supply and eradication state from the restored map snapshot.
        manager.getState().getDiseaseManager().recomputeCubeSupplyFromMap();
    }

    private Boolean getCuredStateFromSnapshot(JsonNode diseaseManagerNode, DiseaseColor color) {
        JsonNode curedDiseases = diseaseManagerNode.get("curedDiseases");
        if (curedDiseases != null && curedDiseases.isObject()) {
            JsonNode cured = curedDiseases.get(color.name());
            if (cured == null) {
                cured = curedDiseases.get(color.name().toLowerCase(Locale.ROOT));
            }
            if (cured != null && cured.isBoolean()) {
                return cured.asBoolean();
            }
        }

        String lower = color.name().substring(0, 1).toUpperCase(Locale.ROOT)
                + color.name().substring(1).toLowerCase(Locale.ROOT);
        String[] fieldNames = {
                "is" + lower + "Cured",
                lower.substring(0, 1).toLowerCase(Locale.ROOT) + lower.substring(1) + "Cured"
        };

        for (String fieldName : fieldNames) {
            JsonNode cured = diseaseManagerNode.get(fieldName);
            if (cured != null && cured.isBoolean()) {
                return cured.asBoolean();
            }
        }
        return null;
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
        pauseHeader = new HBox();
        pauseHeader.setPadding(new Insets(20));
        pauseHeader.setAlignment(Pos.TOP_LEFT);
        pauseHeader.setPickOnBounds(false);
        pauseHeader.setMouseTransparent(false);

        pauseBtn = new ButtonsUtil(
                "PAUSE",
                CYAN_COLOR, "black", CYAN_COLOR, CYAN_COLOR,
                2, 0, 0,
                0.05, 0.05, 0.005,
                root
        );
        pauseBtn.setStyle("-fx-padding: 0; " +
                "-fx-background-radius: 0; " +
                "-fx-font-size: 18px;");

        pauseBtn.setPickOnBounds(true);

        pauseBtn.prefHeightProperty().bind(pauseBtn.prefWidthProperty());
        pauseBtn.minHeightProperty().bind(pauseBtn.prefWidthProperty());
        pauseBtn.maxHeightProperty().bind(pauseBtn.prefWidthProperty());
        pauseBtn.setMouseTransparent(false);

        pauseBtn.setOnMouseClicked(e -> {
            if (pauseMenuOverlay != null) {
                pauseMenuOverlay.show();
            }
        });

        pauseHeader.getChildren().add(pauseBtn);
        root.getChildren().add(pauseHeader);
    }

    private void bringPauseControlToFront() {
        if (pauseHeader != null) {
            pauseHeader.toFront();
        }
        if (pauseBtn != null) {
            pauseBtn.toFront();
        }
    }

    private static void configurePassiveHudRegion(Region region) {
        region.setPickOnBounds(false);
    }

    private void setupPauseMenu() {
        pauseMenuOverlay = new PauseMenuOverlay(root, this::disconnectFromGame, this::bringPauseControlToFront);
    }

    private void disconnectFromGame() {
        if (gameClient != null) {
            gameClient.disconnectFromLobby();
        }
        cleanupScene();
        SceneManager.switchScene(SCENE_MAIN_MENU);
    }

    private void setupGlobalHUD() {

        this.outbreakManager = new OutbreakManager(root, gameManager);
        this.infectionRateManager = new InfectionRateManager(root, gameManager);
        this.cureManager = new CureManager(root, gameManager);

        outbreakAnimationListener = city -> Platform.runLater(() -> {
            if (outbreakManager != null) {
                outbreakManager.queueOutbreak(city);
            }
        });
        gameManager.addOutbreakListener(outbreakAnimationListener);

        // Register a listener so UI widgets update automatically when model changes
        try {
            gameManager.addStateChangeListener(() -> Platform.runLater(() -> {
                if (outbreakManager != null) outbreakManager.updateTrack();
                if (infectionRateManager != null) infectionRateManager.updateTrack();
                if (cureManager != null) cureManager.updateUI();
                updateVirusVisuals();
                playGameEndAnimationAndReturnToMenu();
            }));
        } catch (Exception ignored) {}

        HBox statusBox = new HBox(outbreakManager.getContainer(), infectionRateManager.getContainer());
        statusBox.setAlignment(Pos.TOP_RIGHT);
        statusBox.spacingProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(8, root.getWidth() * 0.008),
                root.widthProperty()
        ));
        statusBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        configurePassiveHudRegion(statusBox);

        StackPane.setAlignment(statusBox, Pos.TOP_RIGHT);
        StackPane.setMargin(statusBox, Insets.EMPTY);

        VBox curesBox = cureManager.getContainer();
        curesBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        configurePassiveHudRegion(curesBox);

        StackPane.setAlignment(curesBox, Pos.CENTER_RIGHT);
        StackPane.setMargin(curesBox, Insets.EMPTY);

        root.getChildren().addAll(statusBox, curesBox);
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
        configurePassiveHudRegion(notificationContainer);

        notificationManager = new NotificationManager(notificationContainer);
        root.getChildren().add(notificationContainer);
    }

    private void setupActionMenu()
    {
        // For network games, pass playerName to enforce turn-based validation
        // For local games, pass null to allow any player to act during their turn
        String playerNameForMenu = (gameClient != null) ? playerName : null;
        
        this.actionMenuManager = gameClient == null
                ? new ActionMenuManager(root, notificationManager, gameManager, this::handleMenuAction, this::highlightValidNodes, playerNameForMenu, this::refreshTurnUI)
                : new ActionMenuManager(root, notificationManager, gameManager, this::sendMenuActionPacket, this::highlightValidNodes, playerNameForMenu, this::refreshTurnUI);
    }

    private void setupChat()
    {
        String chatPlayerName = gameClient != null
                ? this.playerName
                : gameManager.getCurrentPlayer().getPlayerName();
        this.chatManager = new ChatManager(root, chatPlayerName, notificationManager, gameClient);
    }

    private void updateHandUI() {
        if (handManager == null || gameManager == null) {
            return;
        }

        PlayerState displayedPlayer = getDisplayedHandPlayer();
        boolean discardMode = displayedPlayer != null
                && gameManager.getState().isPlayerTurn(displayedPlayer)
                && displayedPlayer.getIsDiscarding();

        handManager.updateHand(displayedPlayer, discardMode, this::handleDiscardCardRequest, this::handlePlayEventCard);
    }

    private void refreshTurnUI() {
        updateHandUI();
        updateVirusVisuals();
        playGameEndAnimationAndReturnToMenu();
    }

    private void playStartupShuffleAnimation() {
        if (deckAnimationManager == null) {
            return;
        }

        Platform.runLater(() -> deckAnimationManager.playStartupShuffleAnimation(this::updateHandUI));
    }

    private AnimationSnapshot createAnimationSnapshot() {
        if (mapGraph == null || gameManager == null || gameManager.getState() == null) {
            return null;
        }

        AnimationSnapshot snapshot = new AnimationSnapshot();
        snapshot.gameWon = gameManager.getState().isGameWon();
        snapshot.gameOver = gameManager.getState().isGameOver();
        snapshot.actionsRemaining = gameManager.getState().getActionsRemaining();
        if (gameManager.getState().getDiseaseManager() != null) {
            snapshot.outbreakScore = gameManager.getState().getDiseaseManager().getOutbreakScore();
        }
        PlayerState currentPlayer = gameManager.getState().getCurrentPlayer();
        if (currentPlayer != null) {
            snapshot.currentPlayerName = currentPlayer.getPlayerName();
        }

        for (CityNode city : mapGraph.getCityList()) {
            CitySnapshot citySnapshot = new CitySnapshot();
            citySnapshot.hasResearchStation = city.hasResearchStation();
            for (DiseaseColor color : DiseaseColor.values()) {
                citySnapshot.cubes.put(color, city.getCubeCount(color));
            }
            snapshot.cities.put(city.getName(), citySnapshot);
        }

        for (PlayerState player : gameManager.getState().getPlayers()) {
            CityNode city = player.getPlayerCurrentCity();
            if (city != null) {
                snapshot.playerCities.put(player.getPlayerName(), city.getName());
            }
        }

        if (gameManager.getState().getDiseaseManager() != null) {
            for (DiseaseColor color : DiseaseColor.values()) {
                snapshot.cures.put(color, gameManager.getState().getDiseaseManager().isCured(color));
            }
        }

        return snapshot;
    }

    private void playSnapshotAnimations(AnimationSnapshot before) {
        if (before == null || mapPane == null || animationManager == null) {
            playGameEndAnimationAndReturnToMenu();
            return;
        }
        
        Runnable playerPhase = () -> {
            for (PlayerState player : gameManager.getState().getPlayers()) {
                CityNode newCity = player.getPlayerCurrentCity();
                String oldCityName = before.playerCities.get(player.getPlayerName());
                if (newCity != null && oldCityName != null && !newCity.getName().equals(oldCityName)) {
                    animatePawnMove(player.getPlayerName(), oldCityName, newCity.getName());
                }
            }

            PlayerState currentPlayer = gameManager.getState().getCurrentPlayer();
            String currentPlayerName = currentPlayer == null ? null : currentPlayer.getPlayerName();
            if (currentPlayerName != null
                    && currentPlayerName.equals(before.currentPlayerName)
                    && gameManager.getState().getActionsRemaining() < before.actionsRemaining) {
                animationManager.playActionConsumed(null);
            } else if (currentPlayerName != null
                    && before.currentPlayerName != null
                    && !currentPlayerName.equals(before.currentPlayerName)) {
                animationManager.playTurnStart(currentPlayerName, null);
            }

            for (CityNode city : mapGraph.getCityList()) {
                CitySnapshot oldCity = before.cities.get(city.getName());
                if (oldCity == null) continue;

                Circle cityVisual = nodeVisuals.get(city);
                if (!oldCity.hasResearchStation && city.hasResearchStation()) {
                    ResearchStationUI stationUI = researchStationUIs.get(city);
                    if (stationUI != null) stationUI.playBuildAnimation();
                    if (cityVisual != null) animationManager.playResearchStationBuilt(cityVisual, null);
                }

                for (DiseaseColor color : DiseaseColor.values()) {
                    int oldCount = oldCity.cubes.getOrDefault(color, 0);
                    int newCount = city.getCubeCount(color);
                    if (newCount < oldCount && cityVisual != null) {
                        animationManager.playDiseaseTreated(cityVisual, null);
                    }
                }
            }

            if (gameManager.getState().getDiseaseManager() != null) {
                for (DiseaseColor color : DiseaseColor.values()) {
                    boolean wasCured = before.cures.getOrDefault(color, false);
                    boolean isCured = gameManager.getState().getDiseaseManager().isCured(color);
                    if (!wasCured && isCured) {
                        animationManager.playCureDiscovered(color, null);
                    }
                }
            }
        };

        Runnable infectionPhase = () -> {
            boolean hasInfectionActivity = false;

            int currentOutbreakScore = gameManager.getState().getDiseaseManager() == null
                    ? before.outbreakScore
                    : gameManager.getState().getDiseaseManager().getOutbreakScore();

            boolean hasOutbreak = currentOutbreakScore > before.outbreakScore;
            String outbreakCity = hasOutbreak ? findLikelyOutbreakCity(before) : null;
            int newOutbreaks = currentOutbreakScore - before.outbreakScore;

            for (CityNode city : mapGraph.getCityList()) {
                CitySnapshot oldCity = before.cities.get(city.getName());
                if (oldCity == null) continue;

                Circle cityVisual = nodeVisuals.get(city);
                for (DiseaseColor color : DiseaseColor.values()) {
                    int oldCount = oldCity.cubes.getOrDefault(color, 0);
                    int newCount = city.getCubeCount(color);

                    if (newCount > oldCount) {
                        animateVirusIncrease(city, color, newCount - oldCount);
                        if (cityVisual != null) {
                            animationManager.playCityInfection(cityVisual, null);
                        }
                        hasInfectionActivity = true;
                    }
                }
            }

            if (hasOutbreak && outbreakManager != null) {
                hasInfectionActivity = true; // Ensure endgame delay logic is triggered
                javafx.animation.PauseTransition outbreakDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
                outbreakDelay.setOnFinished(e -> {
                    for (int i = 0; i < newOutbreaks; i++) {
                        outbreakManager.queueOutbreak(outbreakCity);
                    }
                });
                outbreakDelay.play();
            }

            if (hasInfectionActivity) {
                double delayMillis = hasOutbreak ? 4000.0 : 800.0;
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(delayMillis));
                pause.setOnFinished(e -> resolveEndGame(before));
                pause.play();
            } else {
                resolveEndGame(before);
            }
        };

        playerPhase.run();

        javafx.animation.PauseTransition phaseTransition = new javafx.animation.PauseTransition(javafx.util.Duration.millis(800));
        phaseTransition.setOnFinished(e -> infectionPhase.run());
        phaseTransition.play();
    }

    private void resolveEndGame(AnimationSnapshot before) {
        if (!before.gameWon && gameManager.getState().isGameWon()) {
            playGameEndAnimationAndReturnToMenu();
        } else if (!before.gameOver && gameManager.getState().isGameOver()) {
            playGameEndAnimationAndReturnToMenu();
        } else if (gameManager.getState().isGameOver()) {
            playGameEndAnimationAndReturnToMenu();
        }
    }

    private String findLikelyOutbreakCity(AnimationSnapshot before) {
        if (before == null || mapGraph == null) {
            return "UNKNOWN CITY";
        }

        for (CityNode city : mapGraph.getCityList()) {
            CitySnapshot oldCity = before.cities.get(city.getName());
            if (oldCity == null) {
                continue;
            }

            for (DiseaseColor color : DiseaseColor.values()) {
                int oldCount = oldCity.cubes.getOrDefault(color, 0);
                int newCount = city.getCubeCount(color);
                if (oldCount < 3 || newCount != oldCount) {
                    continue;
                }

                for (CityNode neighbor : city.getConnectedCities()) {
                    CitySnapshot oldNeighbor = before.cities.get(neighbor.getName());
                    if (oldNeighbor != null
                            && neighbor.getCubeCount(color) > oldNeighbor.cubes.getOrDefault(color, 0)) {
                        return city.getName();
                    }
                }
            }
        }

        return "UNKNOWN CITY";
    }

    private void playGameEndAnimationAndReturnToMenu() {
        if (returningToMainMenu || gameManager == null || gameManager.getState() == null || !gameManager.getState().isGameOver()) {
            return;
        }

        returningToMainMenu = true;
        Runnable returnToMenu = this::returnToMainMenuAfterGameEnd;

        if (animationManager == null) {
            returnToMenu.run();
            return;
        }

        if (gameManager.getState().isGameWon()) {
            animationManager.playVictory(returnToMenu);
        } else {
            animationManager.playDefeat(returnToMenu);
        }
    }

    private void returnToMainMenuAfterGameEnd() {
        Platform.runLater(() -> {
            if (gameClient != null) {
                gameClient.disconnectFromLobby();
            }
            SceneManager.switchScene(SCENE_MAIN_MENU);
        });
    }

    private void animatePawnMove(String playerName, String oldCityName, String newCityName) {
        PawnUI pawnUI = playerPawns.get(playerName);
        CityNode oldCity = mapGraph.getCity(oldCityName);
        CityNode newCity = mapGraph.getCity(newCityName);
        Circle oldVisual = oldCity == null ? null : nodeVisuals.get(oldCity);
        Circle newVisual = newCity == null ? null : nodeVisuals.get(newCity);

        if (pawnUI == null || oldVisual == null || newVisual == null) {
            return;
        }

        double startX = oldVisual.getCenterX() - newVisual.getCenterX();
        double startY = oldVisual.getCenterY() - newVisual.getCenterY();
        pawnUI.animateMoveFrom(startX, startY, null);
    }

    private void animateVirusIncrease(CityNode city, DiseaseColor color, int amount) {
        jdemic.ui.GameplayUI.Viruses.CityVirusGroupUI virusUI = cityVirusUIs.get(city);
        if (virusUI == null) {
            return;
        }

        for (int i = 0; i < amount; i++) {
            virusUI.animateNewVirus(color);
        }
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
            updateHandUI();
        }
    }

    private void handlePlayEventCard(int cardIndex) {
        PlayerState displayedPlayer = getDisplayedHandPlayer();
        if (displayedPlayer == null) return;

        // If connected to a server, send a GAME_DATA packet to request the action
        if (gameClient != null) {
            List<Card> hand = displayedPlayer.getHand();
            if (hand == null || cardIndex < 0 || cardIndex >= hand.size()) return;
            Card card = hand.get(cardIndex);
            if (card == null || card.getType() != CardType.EVENT) return;

            String actionName;
            switch (card.getEventType()) {
                case FIREWALL: actionName = "FIREWALL"; break;
                case SATELLITE: actionName = "SATELLITE"; break;
                case SERVER: actionName = "SERVER"; break;
                case CONTROL: actionName = "CONTROL"; break;
                case THREAT: actionName = "THREAT"; break;
                default: actionName = null;
            }

            if (actionName == null) {
                notificationManager.showNotification("Unknown event type");
                return;
            }

            // If the event requires additional input, start an interactive selection flow
            if (!"FIREWALL".equals(actionName)) {
                startPendingEvent(actionName, cardIndex);
                return;
            }

            ObjectNode payload = createBaseGameActionPayload();
            payload.put("GameAction", actionName);
            payload.put("cardIndex", cardIndex);
            gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
            notificationManager.showNotification("Event action request sent");
            return;
        }

        // Local game mode: try to perform simple events directly (others require extra input)
        PlayerState current = gameManager.getState().getCurrentPlayer();
        if (current == null || !current.getPlayerName().equals(displayedPlayer.getPlayerName())) {
            notificationManager.showNotification("Not your turn");
            return;
        }

        List<Card> hand = displayedPlayer.getHand();
        if (hand == null || cardIndex < 0 || cardIndex >= hand.size()) return;
        Card card = hand.get(cardIndex);
        if (card == null || card.getType() != CardType.EVENT) {
            notificationManager.showNotification("Not an event card");
            return;
        }

        switch (card.getEventType()) {
            case FIREWALL: {
                FirewallAction action = new FirewallAction(card);
                int actionsBefore = gameManager.getState().getActionsRemaining();
                gameManager.performAction(displayedPlayer.getPlayer(), action);
                if (gameManager.getState().getActionsRemaining() == actionsBefore) {
                    notificationManager.showNotification("Cannot play this event right now");
                } else {
                    notificationManager.showNotification("Firewall event executed");
                    if (actionMenuManager != null) actionMenuManager.updateMenuState();
                    updateHandUI();
                }
                break;
            }
            default:
                startPendingEvent(card.getEventType().name(), cardIndex);
                break;
        }
    }

    // --- Pending event selection state and helpers ---
    private String pendingEventAction = null;
    private int pendingEventCardIndex = -1;
    private String pendingSatelliteTarget = null;
    private java.util.List<Integer> pendingThreatSelectedIndices = new java.util.ArrayList<>();
    private javafx.scene.layout.StackPane pendingSelectionOverlay = null;
    private final java.util.Map<javafx.scene.Node, javafx.animation.Animation> activeGlows = new java.util.HashMap<>();

    private void startPendingEvent(String actionName, int cardIndex) {
        // clear any previous pending state/overlay before starting a new one
        clearPendingEvent();

        pendingEventAction = actionName;
        pendingEventCardIndex = cardIndex;
        pendingThreatSelectedIndices.clear();

        if (actionMenuManager != null) actionMenuManager.setLocked(true);

        switch (actionName) {
            case "SERVER":
                notificationManager.showNotification("Select a city to place the research station.");
                highlightPendingEventCitiesForServer();
                break;
            case "SATELLITE":
                notificationManager.showNotification("Select a player to move.");
                highlightPlayersForSatellite();
                break;
            case "CONTROL":
                notificationManager.showNotification("Select an infection discard card to remove.");
                showInfectionDiscardOverlay();
                break;
            case "THREAT":
                notificationManager.showNotification("Reorder the top infection cards by clicking them in desired order.");
                showTopInfectionReorderOverlay();
                break;
            default:
                notificationManager.showNotification("This event requires additional input.");
                clearPendingEvent();
                break;
        }
    }

    private void clearPendingEvent() {
        pendingEventAction = null;
        pendingEventCardIndex = -1;
        pendingSatelliteTarget = null;
        pendingThreatSelectedIndices.clear();

        if (pendingSelectionOverlay != null) {
            root.getChildren().remove(pendingSelectionOverlay);
            pendingSelectionOverlay = null;
        }

        for (javafx.animation.Animation a : new java.util.ArrayList<>(activeGlows.values())) {
            try { a.stop(); } catch (Exception ignored) {}
        }
        activeGlows.clear();

        for (java.util.Map.Entry<String, jdemic.ui.GameplayUI.PawnUI> e : playerPawns.entrySet()) {
            e.getValue().getNode().setMouseTransparent(true);
            e.getValue().getNode().setOnMouseClicked(null);
        }

        if (actionMenuManager != null) actionMenuManager.setLocked(false);

        highlightValidNodes("");
    }

    private void highlightPendingEventCitiesForServer() {
        for (CityNode city : mapGraph.getCityList()) {
            Circle nodeCircle = nodeVisuals.get(city);
            if (nodeCircle == null) continue;

            if (!city.hasResearchStation()) {
                highlightedValidNodes.add(city);
                nodeCircle.setFill(Color.web("#00ff00"));
                javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                glow.setColor(Color.web("#00ff00"));
                glow.setRadius(20);
                glow.setSpread(0.6);
                nodeCircle.setEffect(glow);
                nodeCircle.setOnMouseClicked(ev -> handlePendingEventCitySelected(city));
            } else {
                nodeCircle.setOnMouseClicked(ev -> city.clickEvent());
            }
        }
    }

    private void handlePendingEventCitySelected(CityNode destination) {
        if (pendingEventAction == null) return;

        if ("SERVER".equals(pendingEventAction)) {
            if (gameClient == null) {
                PlayerState displayedPlayer = getDisplayedHandPlayer();
                if (displayedPlayer != null) {
                    Card card = displayedPlayer.getHand().get(pendingEventCardIndex);
                    jdemic.GameLogic.Actions.ServerAction action = new jdemic.GameLogic.Actions.ServerAction(destination, card);
                    int actionsBefore = gameManager.getState().getActionsRemaining();
                    gameManager.performAction(displayedPlayer.getPlayer(), action);
                    if (gameManager.getState().getActionsRemaining() == actionsBefore) {
                        notificationManager.showNotification("Cannot play Server here");
                    } else {
                        notificationManager.showNotification("Server deployed at " + destination.getName());
                        updateHandUI();
                        if (actionMenuManager != null) actionMenuManager.updateMenuState();
                    }
                }
            } else {
                ObjectNode payload = createBaseGameActionPayload();
                payload.put("GameAction", "SERVER");
                payload.put("cardIndex", pendingEventCardIndex);
                payload.put("destination", destination.getName());
                gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
                notificationManager.showNotification("Server action sent");
            }
        } else if ("SATELLITE".equals(pendingEventAction) && pendingSatelliteTarget != null) {
            if (gameClient == null) {
                PlayerState displayedPlayer = getDisplayedHandPlayer();
                if (displayedPlayer != null) {
                    Card card = displayedPlayer.getHand().get(pendingEventCardIndex);
                    jdemic.GameLogic.Actions.SatelliteAction action = new jdemic.GameLogic.Actions.SatelliteAction(pendingSatelliteTarget, destination, card);
                    int actionsBefore = gameManager.getState().getActionsRemaining();
                    gameManager.performAction(displayedPlayer.getPlayer(), action);
                    if (gameManager.getState().getActionsRemaining() == actionsBefore) {
                        notificationManager.showNotification("Cannot perform Satellite now");
                    } else {
                        notificationManager.showNotification("Satellite used");
                        updateHandUI();
                        if (actionMenuManager != null) actionMenuManager.updateMenuState();
                    }
                }
            } else {
                ObjectNode payload = createBaseGameActionPayload();
                payload.put("GameAction", "SATELLITE");
                payload.put("cardIndex", pendingEventCardIndex);
                payload.put("targetPlayer", pendingSatelliteTarget);
                payload.put("destination", destination.getName());
                gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
                notificationManager.showNotification("Satellite action sent");
            }
        }

        clearPendingEvent();
    }

    private void highlightPlayersForSatellite() {
        for (var entry : playerPawns.entrySet()) {
            String playerName = entry.getKey();
            jdemic.ui.GameplayUI.PawnUI pawn = entry.getValue();
            javafx.scene.Node node = pawn.getNode();
            node.setMouseTransparent(false);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(700), node);
            ft.setFromValue(1.0);
            ft.setToValue(0.6);
            ft.setCycleCount(javafx.animation.Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();
            activeGlows.put(node, ft);
            node.setOnMouseClicked(ev -> handleSatellitePlayerClicked(playerName));
        }
    }

    private void handleSatellitePlayerClicked(String playerName) {
        pendingSatelliteTarget = playerName;
        notificationManager.showNotification("Now select destination city for satellite.");
        for (CityNode city : mapGraph.getCityList()) {
            Circle nodeCircle = nodeVisuals.get(city);
            if (nodeCircle == null) continue;
            highlightedValidNodes.add(city);
            nodeCircle.setFill(Color.web("#00ff00"));
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(Color.web("#00ff00"));
            glow.setRadius(20);
            glow.setSpread(0.6);
            nodeCircle.setEffect(glow);
            nodeCircle.setOnMouseClicked(ev -> handlePendingEventCitySelected(city));
        }
    }

    private void showInfectionDiscardOverlay() {
        var deck = gameManager.getState().getCardDeck();
        java.util.List<Card> discard = deck.getInfectionDiscardPile();
        if (discard == null || discard.isEmpty()) {
            notificationManager.showNotification("No cards in infection discard pile.");
            clearPendingEvent();
            return;
        }
        // build a fullscreen dark overlay with the discard cards centered near the hand
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(root.widthProperty());
        bg.heightProperty().bind(root.heightProperty());
        bg.setFill(Color.color(0, 0, 0, 0.6));
        bg.setOnMouseClicked(e -> clearPendingEvent());

        HBox content = new HBox(8);
        content.setAlignment(Pos.BOTTOM_CENTER);
        content.setPickOnBounds(false);
        content.translateYProperty().bind(root.heightProperty().multiply(-0.18));

        int idx = 0;
        for (Card c : discard) {
            CityNode target = c.getTargetCity();
            String path = CardResourceUtil.epidemicCardPath(target);
            javafx.scene.image.ImageView iv = jdemic.ui.UIImageUtil.loadResponsive(root, path, 56, 96, 0.05);
            if (iv == null || iv.getImage() == null) continue;
            iv.setPreserveRatio(true);
            StackPane wrapper = new StackPane(iv);
            wrapper.getStyleClass().add("threat-card-wrapper");
            wrapper.setOnMouseClicked(e -> handleControlInfectionSelected(c));

            // pulsing to indicate selectable
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(800), iv);
            ft.setFromValue(1.0);
            ft.setToValue(0.6);
            ft.setCycleCount(javafx.animation.Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();
            activeGlows.put(wrapper, ft);

            // hover highlight
            wrapper.setOnMouseEntered(ev -> {
                javafx.scene.effect.DropShadow ds = new javafx.scene.effect.DropShadow();
                ds.setColor(Color.web("#00ff00"));
                ds.setRadius(18);
                wrapper.setEffect(ds);
                wrapper.setScaleX(1.08);
                wrapper.setScaleY(1.08);
            });
            wrapper.setOnMouseExited(ev -> {
                wrapper.setEffect(null);
                wrapper.setScaleX(1.0);
                wrapper.setScaleY(1.0);
            });

            content.getChildren().add(wrapper);
            idx++;
            if (idx > 20) break;
        }

        StackPane overlay = new StackPane(bg, content);
        StackPane.setAlignment(content, Pos.BOTTOM_CENTER);
        pendingSelectionOverlay = overlay;
        root.getChildren().add(pendingSelectionOverlay);
    }

    private void handleControlInfectionSelected(Card infectionCard) {
        if (infectionCard == null) return;
        if (gameClient == null) {
            PlayerState displayedPlayer = getDisplayedHandPlayer();
            if (displayedPlayer != null) {
                Card card = displayedPlayer.getHand().get(pendingEventCardIndex);
                jdemic.GameLogic.Actions.SystemControlAction action = new jdemic.GameLogic.Actions.SystemControlAction(card, infectionCard);
                int actionsBefore = gameManager.getState().getActionsRemaining();
                gameManager.performAction(displayedPlayer.getPlayer(), action);
                if (gameManager.getState().getActionsRemaining() == actionsBefore) {
                    notificationManager.showNotification("Cannot play Control now");
                } else {
                    // Ensure event card is discarded (fallback in case action did not remove it)
                    if (displayedPlayer.getHand().contains(card)) {
                        displayedPlayer.getHand().remove(card);
                        gameManager.getState().getCardDeck().discard(card);
                    }
                    notificationManager.showNotification("Control executed: infection card removed");
                    updateHandUI();
                    if (actionMenuManager != null) actionMenuManager.updateMenuState();
                }
            }
        } else {
            ObjectNode payload = createBaseGameActionPayload();
            payload.put("GameAction", "CONTROL");
            payload.put("cardIndex", pendingEventCardIndex);
            payload.put("infectionCardName", infectionCard.getCardName());
            gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
            notificationManager.showNotification("Control action sent");
        }

        clearPendingEvent();
    }

    private void showTopInfectionReorderOverlay() {
        var deck = gameManager.getState().getCardDeck();
        java.util.List<Card> top = deck.getTopInfectionCards(6);
        if (top == null || top.isEmpty()) {
            notificationManager.showNotification("Not enough cards in infection deck.");
            clearPendingEvent();
            return;
        }

        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(root.widthProperty());
        bg.heightProperty().bind(root.heightProperty());
        bg.setFill(Color.color(0, 0, 0, 0.6));
        bg.setOnMouseClicked(e -> clearPendingEvent());

        HBox content = new HBox(8);
        content.setAlignment(Pos.BOTTOM_CENTER);
        content.setPickOnBounds(false);
        content.translateYProperty().bind(root.heightProperty().multiply(-0.18));

        for (int i = 0; i < top.size(); i++) {
            Card c = top.get(i);
            CityNode target = c.getTargetCity();
            String path = CardResourceUtil.epidemicCardPath(target);
            javafx.scene.image.ImageView iv = jdemic.ui.UIImageUtil.loadResponsive(root, path, 56, 96, 0.05);
            if (iv == null || iv.getImage() == null) continue;
            iv.setPreserveRatio(true);
            int topIndex = i;
            StackPane wrapper = new StackPane(iv);
            wrapper.getStyleClass().add("threat-card-wrapper");
            wrapper.setOnMouseClicked(e -> handleThreatTopCardClicked(topIndex, wrapper));

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(700), iv);
            ft.setFromValue(1.0);
            ft.setToValue(0.6);
            ft.setCycleCount(javafx.animation.Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();
            activeGlows.put(wrapper, ft);

            wrapper.setOnMouseEntered(ev -> {
                javafx.scene.effect.DropShadow ds = new javafx.scene.effect.DropShadow();
                ds.setColor(Color.web("#00ff00"));
                ds.setRadius(18);
                wrapper.setEffect(ds);
                wrapper.setScaleX(1.08);
                wrapper.setScaleY(1.08);
            });
            wrapper.setOnMouseExited(ev -> {
                wrapper.setEffect(null);
                wrapper.setScaleX(1.0);
                wrapper.setScaleY(1.0);
            });

            content.getChildren().add(wrapper);
        }

        StackPane overlay = new StackPane(bg, content);
        StackPane.setAlignment(content, Pos.BOTTOM_CENTER);
        pendingSelectionOverlay = overlay;
        root.getChildren().add(pendingSelectionOverlay);
    }

    private void handleThreatTopCardClicked(int topIndex, StackPane wrapper) {
        if (pendingThreatSelectedIndices.contains(topIndex)) return;
        pendingThreatSelectedIndices.add(topIndex);

        Label badge = new Label(String.valueOf(pendingThreatSelectedIndices.size()));
        badge.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 4; -fx-background-radius: 8;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        wrapper.getChildren().add(badge);

        var deck = gameManager.getState().getCardDeck();
        int total = deck.getTopInfectionCards(6).size();
        if (pendingThreatSelectedIndices.size() >= total) {
            if (gameClient == null) {
                PlayerState displayedPlayer = getDisplayedHandPlayer();
                if (displayedPlayer != null) {
                    Card card = displayedPlayer.getHand().get(pendingEventCardIndex);
                    java.util.List<Card> top = deck.getTopInfectionCards(total);
                    java.util.List<Card> reordered = new java.util.ArrayList<>();
                    for (int idx : pendingThreatSelectedIndices) reordered.add(top.get(idx));
                    jdemic.GameLogic.Actions.ThreatAction action = new jdemic.GameLogic.Actions.ThreatAction(card, reordered);
                    int actionsBefore = gameManager.getState().getActionsRemaining();
                    gameManager.performAction(displayedPlayer.getPlayer(), action);
                    if (gameManager.getState().getActionsRemaining() == actionsBefore) {
                        notificationManager.showNotification("Cannot play Threat now");
                    } else {
                        notificationManager.showNotification("Threat executed");
                        updateHandUI();
                        if (actionMenuManager != null) actionMenuManager.updateMenuState();
                    }
                }
            } else {
                ObjectNode payload = createBaseGameActionPayload();
                payload.put("GameAction", "THREAT");
                payload.put("cardIndex", pendingEventCardIndex);
                ArrayNode arr = payload.putArray("infectionCardIndices");
                for (int idx : pendingThreatSelectedIndices) arr.add(idx);
                gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
                notificationManager.showNotification("Threat action sent");
            }

            clearPendingEvent();
        }
    }

    // --- End pending event helpers ---

    private void sendMenuActionPacket(String action) {
        if (gameClient == null) {
            return;
        }

        ObjectNode payload = createBaseGameActionPayload();
        payload.put("GameAction", action);

        if (!populateActionPayload(payload, action, null)) {
            notificationManager.showNotification("Action needs a valid target first");
            return;
        }

        gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
    }

    private void handleMenuAction(String action) {
        if (gameClient != null) {
            sendMenuActionPacket(action);
            return;
        }

        GameAction localAction = createLocalMenuAction(action);
        if (localAction == null) {
            notificationManager.showNotification("Action not available locally");
            return;
        }

        PlayerState playerState = gameManager.getState().getCurrentPlayer();
        if (playerState == null || playerState.getPlayer() == null) {
            notificationManager.showNotification("No active player");
            return;
        }

        int actionsBefore = gameManager.getState().getActionsRemaining();
        gameManager.performAction(playerState.getPlayer(), localAction);

        if (gameManager.getState().getActionsRemaining() == actionsBefore) {
            notificationManager.showNotification("Cannot build here");
            return;
        }

        refreshTurnUI();
        actionMenuManager.updateMenuState();
        notificationManager.showNotification("Build complete");
    }

    private GameAction createLocalMenuAction(String action) {
        if (ACTION_BUILD_RESEARCH_STATION.equals(action)) {
            return new BuildResearchStation();
        }
        return null;
    }

    private void sendMovementActionPacket(String action, CityNode destination) {
        if (gameClient == null || destination == null) {
            return;
        }

        ObjectNode payload = createBaseGameActionPayload();
        payload.put("GameAction", action);

        if (!populateActionPayload(payload, action, destination)) {
            notificationManager.showNotification("Cannot use " + action.replace('_', ' ') + " for " + destination.getName());
            return;
        }

        gameClient.sendPacket(new Packet(PacketType.GAME_DATA, payload));
    }

    private boolean populateActionPayload(ObjectNode payload, String action, CityNode destination) {
        PlayerState player = getDisplayedHandPlayer();
        if (player == null) {
            return false;
        }

        CityNode currentCity = player.getPlayerCurrentCity();
        switch (action) {
            case "DRIVE_FERRY":
            case "SHUTTLE_FLIGHT":
                if (destination == null) return false;
                payload.put("destination", destination.getName());
                return true;

            case "DIRECT_FLIGHT": {
                if (destination == null) return false;
                int cardIndex = findCardIndexForCity(player, destination);
                if (cardIndex < 0) return false;
                payload.put("destination", destination.getName());
                payload.put("cardIndex", cardIndex);
                return true;
            }

            case "CHARTER_FLIGHT": {
                if (destination == null || currentCity == null) return false;
                int cardIndex = findCardIndexForCity(player, currentCity);
                if (cardIndex < 0) return false;
                payload.put("destination", destination.getName());
                payload.put("cardIndex", cardIndex);
                return true;
            }

            case "TREAT_DISEASE": {
                DiseaseColor color = firstTreatableColor(currentCity);
                if (color == null) return false;
                payload.put("color", color.name());
                return true;
            }

            case "DISCOVER_CURE":
                return populateDiscoverCurePayload(payload, player);

            case "SHARE_KNOWLEDGE":
                return populateShareKnowledgePayload(payload, player);

            case "BUILD_RESEARCH_STATION":
                return true;

            default:
                return true;
        }
    }

    private int findCardIndexForCity(PlayerState player, CityNode city) {
        if (player == null || city == null) {
            return -1;
        }

        List<Card> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.getType() == CardType.CITY
                    && card.getTargetCity() != null
                    && city.getName().equals(card.getTargetCity().getName())) {
                return i;
            }
        }
        return -1;
    }

    private DiseaseColor firstTreatableColor(CityNode city) {
        if (city == null) {
            return null;
        }

        for (DiseaseColor color : DiseaseColor.values()) {
            if (city.getCubeCount(color) > 0) {
                return color;
            }
        }
        return null;
    }

    private boolean populateDiscoverCurePayload(ObjectNode payload, PlayerState player) {
        int requiredCards = player.getPlayerRole() == PlayerRoles.SCIENTIST ? 4 : 5;

        for (DiseaseColor color : DiseaseColor.values()) {
            List<Integer> matchingIndices = new ArrayList<>();
            List<Card> hand = player.getHand();
            for (int i = 0; i < hand.size(); i++) {
                Card card = hand.get(i);
                if (card.getType() == CardType.CITY
                        && card.getTargetCity() != null
                        && card.getTargetCity().getNativeColor() == color) {
                    matchingIndices.add(i);
                }
            }

            if (matchingIndices.size() >= requiredCards) {
                payload.put("color", color.name());
                ArrayNode cardIndices = payload.putArray("cardIndices");
                for (int i = 0; i < requiredCards; i++) {
                    cardIndices.add(matchingIndices.get(i));
                }
                return true;
            }
        }
        return false;
    }

    private boolean populateShareKnowledgePayload(ObjectNode payload, PlayerState player) {
        CityNode currentCity = player.getPlayerCurrentCity();
        if (currentCity == null) {
            return false;
        }

        int cardIndex = findCardIndexForCity(player, currentCity);
        if (cardIndex < 0 && player.getPlayerRole() == PlayerRoles.RESEARCHER && !player.getHand().isEmpty()) {
            cardIndex = 0;
        }
        if (cardIndex < 0) {
            return false;
        }

        for (PlayerState target : gameManager.getState().getPlayers()) {
            CityNode targetCity = target.getPlayerCurrentCity();
            if (!target.getPlayerName().equalsIgnoreCase(player.getPlayerName())
                    && targetCity != null
                    && targetCity.getName().equals(currentCity.getName())) {
                payload.put("cardIndex", cardIndex);
                payload.put("targetPlayer", target.getPlayerName());
                payload.put("direction", "give");
                return true;
            }
        }
        return false;
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
                nodeCircle.setOnMouseClicked(ev -> city.clickEvent());
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

        Set<CityNode> validDestinations = getValidDestinations(movementType, currentPlayer, currentCity);
        for (CityNode destination : validDestinations) {
            highlightedValidNodes.add(destination);
            Circle nodeCircle = nodeVisuals.get(destination);
            if (nodeCircle != null) {
                nodeCircle.setFill(Color.web("#00ff00"));

                javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                glow.setColor(Color.web("#00ff00"));
                glow.setRadius(20);
                glow.setSpread(0.6);
                nodeCircle.setEffect(glow);

                nodeCircle.setOnMouseClicked(ev -> handleHighlightedMovementClick(movementType, destination));
            }
        }
    }

    private Set<CityNode> getValidDestinations(String movementType, PlayerState currentPlayer, CityNode currentCity) {
        Set<CityNode> destinations = new HashSet<>();

        if ("DRIVE_FERRY".equals(movementType)) {
            destinations.addAll(currentCity.getConnectedCities());
            return destinations;
        }

        if ("DIRECT_FLIGHT".equals(movementType)) {
            for (Card card : currentPlayer.getHand()) {
                if (card.getType() == CardType.CITY && card.getTargetCity() != null) {
                    CityNode city = mapGraph.getCity(card.getTargetCity().getName());
                    if (city != null && !city.getName().equals(currentCity.getName())) {
                        destinations.add(city);
                    }
                }
            }
            return destinations;
        }

        if ("CHARTER_FLIGHT".equals(movementType)) {
            if (findCardIndexForCity(currentPlayer, currentCity) >= 0) {
                for (CityNode city : mapGraph.getCityList()) {
                    if (!city.getName().equals(currentCity.getName())) {
                        destinations.add(city);
                    }
                }
            }
            return destinations;
        }

        if ("SHUTTLE_FLIGHT".equals(movementType) && currentCity.hasResearchStation()) {
            for (CityNode city : mapGraph.getCityList()) {
                if (city.hasResearchStation() && !city.getName().equals(currentCity.getName())) {
                    destinations.add(city);
                }
            }
        }

        return destinations;
    }

    private void handleHighlightedMovementClick(String movementType, CityNode destination) {
        int actionsLeft = gameManager.getState().getActionsRemaining();
        if (actionsLeft <= 0) {
            notificationManager.showNotification("No actions remaining!");
            return;
        }

        if (gameClient == null) {
            PlayerState currentPlayer = gameManager.getCurrentPlayer();
            if (currentPlayer != null) {
                currentPlayer.setCurrentCity(destination);
            }
            gameManager.getState().setActionsRemaining(actionsLeft - 1);
            if (gameManager.getState().getActionsRemaining() <= 0) {
                gameManager.nextTurn();
            }
        }

        sendMovementActionPacket(movementType, destination);
        notificationManager.showNotification(
                gameClient == null
                        ? "Moved to " + destination.getName() + ". Actions left: " + gameManager.getState().getActionsRemaining()
                        : "Move request sent: " + destination.getName()
        );

        highlightValidNodes("");
        if (actionMenuManager != null) {
            actionMenuManager.clearSelectedMovementAction();
        }
        refreshTurnUI();
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
        mapPane.setMouseTransparent(false);
        mapPane.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.min(root.getWidth() * 0.70, root.getHeight() * 1.38),
                root.widthProperty(), root.heightProperty()
        ));
        mapPane.prefHeightProperty().bind(mapPane.prefWidthProperty().multiply(0.5));
        mapPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        java.net.URL mapBgUrl = getClass().getResource("/backgroundMap.png");
        if (mapBgUrl == null) {
            System.err.println("[MapTestScene] Missing resource: /backgroundMap.png");
            return;
        }
        ImageView mapBg = new ImageView(SafeResourceLoader.loadImage(mapBgUrl));
        mapBg.fitWidthProperty().bind(mapPane.widthProperty());
        mapBg.fitHeightProperty().bind(mapPane.heightProperty());
        mapBg.setPreserveRatio(false);
        mapPane.getChildren().add(mapBg);

        Color defaultLineColor = Color.color(0, 0.7, 1, 0.3);
        Color activeLineColor = Color.web("#00ffea");

        DropShadow glowEffect = new DropShadow();
        glowEffect.setColor(Color.web(BRIGHT_CYAN_COLOR));
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
                // preserve its highlight color instead of reverting to original one
                if (highlightedValidNodes.contains(city)) {
                    node.setFill(Color.web("#00ff00"));
                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                    glow.setColor(Color.web("#00ff00"));
                    glow.setRadius(20);
                    glow.setSpread(0.6);
                    node.setEffect(glow);
                } else {
                    node.setFill(nativeColor);
                    node.setEffect(null);
                }

                node.setScaleX(1);
                node.setScaleY(1);

                for (CityNode neighbor : city.getConnectedCities()) {
                    Circle neighborCircle = nodeVisuals.get(neighbor);
                    if (neighborCircle != null) {
                        neighborCircle.setStroke(Color.WHITE);
                        neighborCircle.setStrokeWidth(1);
                        if (highlightedValidNodes.contains(neighbor)) {
                            // keep highlighted neighbor appearance
                            neighborCircle.setFill(Color.web("#00ff00"));
                            javafx.scene.effect.DropShadow neighborGlow = new javafx.scene.effect.DropShadow();
                            neighborGlow.setColor(Color.web("#00ff00"));
                            neighborGlow.setRadius(20);
                            neighborGlow.setSpread(0.6);
                            neighborCircle.setEffect(neighborGlow);
                        } else {
                            neighborCircle.setEffect(null);
                            Color neighborNative = getFxColor(neighbor.getNativeColor());
                            neighborCircle.setFill(neighborNative);
                        }
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
            label.setFont(Font.font(FONT_HKMODULAR, FontWeight.BOLD, 10));
            label.setMouseTransparent(true);

            DropShadow textShadow = new DropShadow();
            textShadow.setRadius(6);
            textShadow.setSpread(0.8);
            textShadow.setColor(Color.rgb(0,0,0,0.9));
            textShadow.setOffsetX(0);
            textShadow.setOffsetY(0);
            label.setEffect(textShadow);

            label.layoutXProperty().bind(Bindings.createDoubleBinding(() -> node.getCenterX() - label.getLayoutBounds().getWidth() / 2, node.centerXProperty(),label.layoutBoundsProperty()));
            label.layoutYProperty().bind(node.centerYProperty().add(20));

            mapPane.getChildren().addAll(node, label);
        }
        for (CityNode city : mapGraph.getCityList()) {
            updateResearchStationVisual(city);
        }
        syncResearchStationVisuals();
        setupPawns(mapPane);
        // Create virus UI groups so disease cubes are rendered on the map
        for (CityNode city : mapGraph.getCityList()) {
            jdemic.ui.GameplayUI.Viruses.CityVirusGroupUI group = new jdemic.ui.GameplayUI.Viruses.CityVirusGroupUI(city, mapPane, mapPane.heightProperty());
            cityVirusUIs.put(city, group);
        }
        syncResearchStationVisuals();
        MapZoomPanHandler.attach(mapPane);
        mapContainer.getChildren().add(mapPane);
        root.getChildren().add(mapContainer);
    }

    private void updateVirusVisuals() {
        if (cityVirusUIs == null || cityVirusUIs.isEmpty()) return;
        Platform.runLater(() -> {
            for (jdemic.ui.GameplayUI.Viruses.CityVirusGroupUI group : cityVirusUIs.values()) {
                try {
                    group.updateVisuals();
                } catch (Exception ignored) {
                    // Defensive: avoid crashing UI if a single city fails to render
                }
            }
        });
    }

    private void updateResearchStationVisual(CityNode city) {
        if (city == null || mapPane == null) {
            return;
        }

        ResearchStationUI existing = researchStationUIs.get(city);
        if (!city.hasResearchStation()) {
            if (existing != null) {
                mapPane.getChildren().remove(existing.getNode());
                researchStationUIs.remove(city);
            }
            return;
        }

        if (existing != null) {
            existing.getNode().toFront();
            return;
        }

        Circle cityVisual = nodeVisuals.get(city);
        if (cityVisual == null) {
            return;
        }

        ResearchStationUI stationUI = new ResearchStationUI(mapPane.heightProperty());
        stationUI.bindToCity(cityVisual.centerXProperty(), cityVisual.centerYProperty());
        researchStationUIs.put(city, stationUI);
        mapPane.getChildren().add(stationUI.getNode());
        stationUI.getNode().toFront();
    }

    private void syncResearchStationVisuals() {
        if (mapGraph == null || mapPane == null) {
            return;
        }

        for (CityNode city : mapGraph.getCityList()) {
            updateResearchStationVisual(city);
        }
    }

    private static class AnimationSnapshot {
        private final Map<String, CitySnapshot> cities = new HashMap<>();
        private final Map<String, String> playerCities = new HashMap<>();
        private final Map<DiseaseColor, Boolean> cures = new EnumMap<>(DiseaseColor.class);
        private String currentPlayerName;
        private int actionsRemaining;
        private int outbreakScore;
        private boolean gameWon;
        private boolean gameOver;
    }

    private static class CitySnapshot {
        private final Map<DiseaseColor, Integer> cubes = new EnumMap<>(DiseaseColor.class);
        private boolean hasResearchStation;
    }

    private Color getFxColor(DiseaseColor color) {
        return switch (color) {
            case BLUE -> Color.web(CYAN_COLOR);
            case YELLOW -> Color.web("#cfc900");
            case BLACK -> Color.web(BLACK_COLOR);
            case RED -> Color.web(RED_COLOR);
        };
    }

    public StackPane getRoot() {
        return root;
    }

    private void setupBackground() {
        SceneBackgroundUtil.addCoverBackground(root, SceneBackgroundUtil.GAME_BACKGROUND);
    }

    private void cleanupScene() {
        if (sceneListener != null) {
            detachSceneHandlers(root.getScene());
            root.sceneProperty().removeListener(sceneListener);
            sceneListener = null;
        }
        if (gameClient != null && playerUpdateListener != null) {
            gameClient.removePlayerUpdateListener(playerUpdateListener);
            playerUpdateListener = null;
        }
        if (gameManager != null && outbreakAnimationListener != null) {
            gameManager.removeOutbreakListener(outbreakAnimationListener);
            outbreakAnimationListener = null;
        }
    }
}
