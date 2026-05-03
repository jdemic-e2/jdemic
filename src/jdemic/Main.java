package jdemic;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import jdemic.Scenes.MainMenuScene;
import jdemic.Scenes.SceneManager;
import jdemic.Scenes.Settings.AudioManager;
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

        stage.show();
        AudioManager.getInstance().playMusic("MENU");
    }

    public static void main(String[] args) {
        launch();
    }
}