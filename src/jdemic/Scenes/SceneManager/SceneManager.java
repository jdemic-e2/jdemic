package jdemic.Scenes.SceneManager;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.JoinCodeScene;
import jdemic.Scenes.MainMenuScene;
import jdemic.Scenes.Settings.AudioManager;
import jdemic.Scenes.Settings.SettingsScene;
import jdemic.Scenes.Tutorial.*;

public class SceneManager {
    public static final String LIFECYCLE_PROPERTY = SceneManager.class.getName() + ".lifecycle";
    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchScene(String sceneName) {
        if (stage == null) {
            throw new IllegalStateException("SceneManager must be initialized before switching scenes.");
        }

        Parent root = createSceneRoot(sceneName);
        if (root == null) {
            throw new IllegalArgumentException("Unknown scene: " + sceneName);
        }

        setRoot(root);
        if ("MAIN_MENU".equals(sceneName)) {
            AudioManager.getInstance().playMusic("MENU");
        }
    }

    public static void setRoot(Parent root) {
        if (stage == null) {
            throw new IllegalStateException("SceneManager must be initialized before switching scenes.");
        }
        if (stage.getScene() == null) {
            stage.setScene(new Scene(root));
            return;
        }

        cleanupRoot(stage.getScene().getRoot());
        stage.getScene().setRoot(root);
    }

    public static void registerLifecycle(Parent root, SceneLifecycle lifecycle) {
        if (root != null && lifecycle != null) {
            root.getProperties().put(LIFECYCLE_PROPERTY, lifecycle);
        }
    }

    public static void shutdownCurrentScene() {
        if (stage != null && stage.getScene() != null) {
            cleanupRoot(stage.getScene().getRoot());
        }
    }

    private static void cleanupRoot(Parent root) {
        if (root == null) {
            return;
        }
        Object lifecycle = root.getProperties().remove(LIFECYCLE_PROPERTY);
        if (lifecycle instanceof SceneLifecycle sceneLifecycle) {
            sceneLifecycle.onSceneRemoved();
        }
    }

    private static Parent createSceneRoot(String sceneName) {
        switch (sceneName) {
            //Standard buttons
            case "MAIN_MENU": return new MainMenuScene(stage).getRoot();
            case "SETTINGS": return new SettingsScene(stage).getRoot();

            //Lobby related buttons
            case "HOST_SCREEN": return new jdemic.Scenes.Lobby.LobbyScene(stage).getRoot();
            case "LOBBY": return new jdemic.Scenes.Lobby.LobbyScene(stage).getRoot();
            case "JOIN_CODE": return new JoinCodeScene(stage).getRoot();
            case "MAP_TEST": return new jdemic.Scenes.MapTest.MapTestScene(stage).getRoot();

            //The holy set of tutorial scenes
            case "TUTORIAL": return new jdemic.Scenes.Tutorial.TutorialRulesScene(stage).getRoot();
            case "TUT_RULES": return new TutorialRulesScene(stage).getRoot();
            case "TUT_MAP": return new TutorialMapScene(stage).getRoot();
            case "TUT_CITIES": return new TutorialCitiesScene(stage).getRoot();
            case "TUT_ROLES": return new TutorialCardRolesScene(stage).getRoot();
            case "TUT_EVENTS": return new TutorialEventCardsScene(stage).getRoot();
            case "TUT_VIRUS": return new TutorialVirusCardsScene(stage).getRoot();
            case "TUT_EPIDEMIC": return new TutorialEpidemicScene(stage).getRoot();
            case "TUT_TURN": return new TutorialPlayerTurnScene(stage).getRoot();
            case "TUT_WINLOSE": return new TutorialWinLoseScene(stage).getRoot();
            default: return null;
        }
    }

    public static void clearCache() {
        // Kept for compatibility with tests or callers from older branches.
    }
}
