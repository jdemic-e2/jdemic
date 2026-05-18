package jdemic.Scenes.SceneManager;

import javafx.scene.Parent;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.JoinCodeScene;
import jdemic.Scenes.MainMenuScene;
import jdemic.Scenes.Settings.SettingsScene;
import jdemic.Scenes.Tutorial.*;

import java.util.HashMap;
import java.util.Map;

public class SceneManager {
    private static Stage stage;
    private static final Map<String, Parent> scenes = new HashMap<>();

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchScene(String sceneName) {
        Parent root = isDynamicScene(sceneName) ? null : scenes.get(sceneName);

        if (root == null) {
            switch (sceneName) {
                //Standard buttons
                case "MAIN_MENU":root = new MainMenuScene(stage).getRoot();break;
                case "SETTINGS": root = new SettingsScene(stage).getRoot();break;

                //Lobby related buttons
                case "HOST_SCREEN": root = new jdemic.Scenes.Lobby.LobbyScene(stage).getRoot();break;
                case "LOBBY": root = new jdemic.Scenes.Lobby.LobbyScene(stage).getRoot();break;
                case "JOIN_CODE":root = new JoinCodeScene(stage).getRoot();break;
                case "MAP_TEST": root = new jdemic.Scenes.MapTest.MapTestScene(stage).getRoot();break;

                //The holy set of tutorial scenes
                case "TUTORIAL": root = new jdemic.Scenes.Tutorial.TutorialRulesScene(stage).getRoot();break;
                case "TUT_RULES": root = new TutorialRulesScene(stage).getRoot(); break;
                case "TUT_MAP": root = new TutorialMapScene(stage).getRoot(); break;
                case "TUT_CITIES": root = new TutorialCitiesScene(stage).getRoot(); break;
                case "TUT_ROLES": root = new TutorialCardRolesScene(stage).getRoot(); break;
                case "TUT_EVENTS": root = new TutorialEventCardsScene(stage).getRoot(); break;
                case "TUT_VIRUS": root = new TutorialVirusCardsScene(stage).getRoot(); break;
                case "TUT_EPIDEMIC": root = new TutorialEpidemicScene(stage).getRoot(); break;
                case "TUT_TURN": root = new TutorialPlayerTurnScene(stage).getRoot(); break;
                case "TUT_WINLOSE": root = new TutorialWinLoseScene(stage).getRoot(); break;
            }
            if (!isDynamicScene(sceneName)) {
                scenes.put(sceneName, root);
            }
        }

        stage.getScene().setRoot(root);
    }

    private static boolean isDynamicScene(String sceneName) {
        return "LOBBY".equals(sceneName)
                || "HOST_SCREEN".equals(sceneName)
                || "JOIN_CODE".equals(sceneName)
                || "MAP_TEST".equals(sceneName);
    }

    public static void clearCache()
    {
        scenes.clear();
    }
}
