package jdemic;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import jdemic.DedicatedServer.network.core.JdemicNetworkServer;
import jdemic.GameLogic.GameClient;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.Scenes.Settings.SettingsManager;

public class Main extends Application {
    @Override
    public void start(Stage stage) {

        SettingsManager sm = SettingsManager.getInstance();
        SceneManager.init(stage);

        Scene scene = new Scene(new Pane());
        stage.setScene(scene);

        sm.applySettingsToStage(stage);

        stage.setTitle("Cyber Crisis");
        SceneManager.switchScene("MAIN_MENU");

        stage.setOnCloseRequest(event -> {
            exitApplication();
        });

        stage.show();
    }

    @Override
    public void stop() {
        shutdownNetworkResources();
    }

    private void shutdownNetworkResources() {
        SceneManager.shutdownCurrentScene();
        GameClient.disconnectAllFromLobby();
        JdemicNetworkServer.shutdown();
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::performShutdown, "jdemic-app-shutdown"));
        launch();
    }
    public static voide performShutdown() {
        AudioManager.getInstance().stopMusic();
        JdemicNetworkServer.shutdown();
    }

    public static void exitApplication() {
        performShutdown();
        Platform.exit();
        System.exit(0);
    }
}

