package jdemic;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jdemic.Scenes.MainMenuScene;
import jdemic.Scenes.Settings.AudioManager;
import jdemic.Scenes.Settings.SettingsManager;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        SettingsManager sm = SettingsManager.getInstance();

        MainMenuScene mainMenu = new MainMenuScene(stage);
        Scene scene = new Scene(mainMenu.getRoot());
        sm.applySettingsToStage(stage);

        stage.setTitle("Cyber Crisis");
        stage.setScene(scene);
        stage.show();

        //AudioManager.getInstance().playMusic("MENU");
    }

    public static void main(String[] args) {
        launch();
    }
}